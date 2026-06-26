import { computed, onUnmounted, ref, unref } from 'vue'
import { synthesizeInterviewTts } from '@/api/interview'

const SENTENCE_END_REGEXP = /[。！？.!?]/
const FEEDBACK_BLOCK_REGEXP = /<FEEDBACK>[\s\S]*?<\/FEEDBACK>/gi

/**
 * 用户自定义云端 TTS 播放队列。
 * 只用于语音面试的 AI 面试官播报；单句合成失败后立即关闭云端队列，由调用方降级到浏览器 TTS。
 */
export function useCloudTextToSpeech(options = {}) {
  const isSupported = ref(Boolean(options.enabled))
  const isPreparing = ref(false)
  const isSpeaking = ref(false)
  const isActive = computed(() => isPreparing.value || isSpeaking.value)
  const engineStatus = computed(() => (isSupported.value ? 'cloud-tts' : 'unavailable'))

  let buffer = ''
  let queue = []
  let activeAudio = null
  let activeObjectUrl = ''
  let activeAbortController = null
  let activeSynthesisItem = null
  let pendingFallbackEvent = null
  let runId = 0
  let isPlaying = false
  let fallbackNotified = false

  const normalizeTextForSpeech = (text) => {
    if (!text) return ''
    return String(text)
      .replace(FEEDBACK_BLOCK_REGEXP, '')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/[#*_>`|]/g, '')
      .replace(/\[(.*?)\]\(.*?\)/g, '$1')
      .replace(/\s+/g, ' ')
      .trim()
  }

  const resolveSessionId = () => unref(options.sessionId)

  const setEnabled = (enabled) => {
    isSupported.value = Boolean(enabled)
    if (enabled) {
      fallbackNotified = false
      return
    }
    stop()
  }

  const updatePreparingState = () => {
    // 云端 TTS 等待期包含“上游合成中”和“Audio.play() 尚未真正开始”两段，
    // 这段时间需要暂停收音，但不能把 UI 展示为真实播报。
    isPreparing.value = Boolean(isSupported.value && (
      activeSynthesisItem
      || (isPlaying && !isSpeaking.value)
      || queue.some((item) => item.preparing || !item.objectUrl)
    ))
  }

  const releaseQueuedAudio = () => {
    queue.forEach((item) => {
      if (item.objectUrl) {
        URL.revokeObjectURL(item.objectUrl)
        item.objectUrl = ''
      }
    })
  }

  const abortActiveSynthesis = () => {
    if (activeAbortController) {
      activeAbortController.abort()
      activeAbortController = null
    }
    if (activeSynthesisItem) {
      activeSynthesisItem.preparing = false
      activeSynthesisItem = null
    }
  }

  const releaseActiveAudio = () => {
    if (activeAudio) {
      activeAudio.onended = null
      activeAudio.onerror = null
      activeAudio.pause?.()
      activeAudio = null
    }
    if (activeObjectUrl) {
      URL.revokeObjectURL(activeObjectUrl)
      activeObjectUrl = ''
    }
  }

  const emitFallback = (event) => {
    if (fallbackNotified) return
    fallbackNotified = true
    options.onFallback?.(event)
  }

  const buildRemainingText = (failedItem) => [
    failedItem?.text || '',
    ...queue.filter((item) => item !== failedItem).map((item) => item.text),
    buffer.trim(),
  ].filter(Boolean).join('')

  const disableAndFallback = (failedItem, reason) => {
    const fallbackEvent = {
      text: buildRemainingText(failedItem),
      reason,
      speechOptions: failedItem?.speechOptions || {},
    }
    releaseQueuedAudio()
    queue = []
    buffer = ''
    isPlaying = false
    isSpeaking.value = false
    isSupported.value = false
    pendingFallbackEvent = null
    abortActiveSynthesis()
    releaseActiveAudio()
    updatePreparingState()
    emitFallback(fallbackEvent)
  }

  const deferFallbackUntilCurrentAudioEnds = (failedItem, reason) => {
    pendingFallbackEvent = {
      text: buildRemainingText(failedItem),
      reason,
      speechOptions: failedItem?.speechOptions || {},
    }
    releaseQueuedAudio()
    queue = []
    buffer = ''
    isSupported.value = false
    abortActiveSynthesis()
    updatePreparingState()
    if (!isPlaying) {
      const fallbackEvent = pendingFallbackEvent
      pendingFallbackEvent = null
      emitFallback(fallbackEvent)
    }
  }

  const finishCurrentItem = (currentRunId, speechOptions, event = {}) => {
    if (currentRunId !== runId) return
    releaseActiveAudio()
    speechOptions?.onEnd?.({
      reason: event.reason || 'end',
      started: true,
      text: event.text || '',
    })
    isPlaying = false
    isSpeaking.value = false
    if (pendingFallbackEvent) {
      const fallbackEvent = pendingFallbackEvent
      pendingFallbackEvent = null
      updatePreparingState()
      emitFallback(fallbackEvent)
      return
    }
    if (queue.length > 0) {
      updatePreparingState()
      if (queue[0].objectUrl) {
        void playPreparedItem()
      } else {
        void prepareNext()
      }
      return
    }
    updatePreparingState()
    options.onEnd?.(event)
  }

  async function playPreparedItem() {
    if (isPlaying || !queue.length || !isSupported.value) return
    const item = queue[0]
    if (!item.objectUrl) {
      updatePreparingState()
      void prepareNext()
      return
    }
    queue.shift()
    const currentRunId = runId
    isPlaying = true
    isPreparing.value = true
    activeObjectUrl = item.objectUrl
    item.objectUrl = ''
    activeAudio = new Audio(activeObjectUrl)
    activeAudio.onended = () => finishCurrentItem(currentRunId, item.speechOptions, {
      reason: 'end',
      text: item.text,
    })
    activeAudio.onerror = () => disableAndFallback(item, new Error('Cloud TTS audio playback failed'))

    try {
      await activeAudio.play()
      if (currentRunId !== runId) return
      isSpeaking.value = true
      updatePreparingState()
      item.speechOptions?.onStart?.({
        reason: 'start',
        started: true,
        text: item.text,
      })
      void prepareNext()
    } catch (error) {
      if (currentRunId !== runId) return
      disableAndFallback(item, error)
    }
  }

  async function prepareNext() {
    if (activeSynthesisItem || !isSupported.value) {
      updatePreparingState()
      return
    }
    const item = queue.find((candidate) => !candidate.objectUrl && !candidate.preparing)
    if (!item) {
      updatePreparingState()
      if (!isPlaying && queue[0]?.objectUrl) {
        void playPreparedItem()
      }
      return
    }
    const sessionId = resolveSessionId()
    if (!sessionId) {
      disableAndFallback(item, new Error('TTS sessionId missing'))
      return
    }

    const currentRunId = runId
    const abortController = new AbortController()
    activeAbortController = abortController
    activeSynthesisItem = item
    item.preparing = true
    updatePreparingState()

    try {
      const audioBlob = await synthesizeInterviewTts(sessionId, item.text, {
        signal: abortController.signal,
      })
      if (currentRunId !== runId) return
      item.preparing = false
      activeSynthesisItem = null
      activeAbortController = null
      item.objectUrl = URL.createObjectURL(audioBlob)
      updatePreparingState()
      if (!isPlaying && queue[0] === item) {
        void playPreparedItem()
      }
      void prepareNext()
    } catch (error) {
      if (error?.name === 'AbortError') return
      if (currentRunId !== runId) return
      item.preparing = false
      activeSynthesisItem = null
      activeAbortController = null
      if (isPlaying) {
        deferFallbackUntilCurrentAudioEnds(item, error)
        return
      }
      disableAndFallback(item, error)
    }
  }

  const enqueue = (text, speechOptions = {}) => {
    const normalizedText = normalizeTextForSpeech(text)
    if (!normalizedText || !isSupported.value) return
    queue.push({
      text: normalizedText,
      speechOptions,
      objectUrl: '',
      preparing: false,
    })
    updatePreparingState()
    void prepareNext()
  }

  const speak = (text, speechOptions = {}) => {
    stop()
    if (!isSupported.value) return
    enqueue(text, speechOptions)
  }

  const speakStreaming = (chunk, speechOptions = {}) => {
    if (!chunk || !isSupported.value) return
    buffer += String(chunk).replace(FEEDBACK_BLOCK_REGEXP, '')
    if (!buffer.trim()) return
    while (true) {
      const endIndex = Array.from(buffer).findIndex((char) => SENTENCE_END_REGEXP.test(char))
      if (endIndex === -1) break
      const sentence = buffer.slice(0, endIndex + 1).trim()
      buffer = buffer.slice(endIndex + 1)
      enqueue(sentence, speechOptions)
    }
  }

  const flushRemaining = (speechOptions = {}) => {
    if (!isSupported.value) return
    const remaining = buffer.trim()
    buffer = ''
    if (remaining) {
      enqueue(remaining, speechOptions)
      return
    }
    if (!isActive.value) {
      options.onEnd?.()
    }
  }

  function stop() {
    runId += 1
    abortActiveSynthesis()
    releaseQueuedAudio()
    queue = []
    buffer = ''
    pendingFallbackEvent = null
    isPlaying = false
    isPreparing.value = false
    isSpeaking.value = false
    releaseActiveAudio()
  }

  const prepareForUserGesture = () => {}

  onUnmounted(() => {
    stop()
  })

  return {
    isSupported,
    isPreparing,
    isSpeaking,
    isActive,
    engineStatus,
    setEnabled,
    speak,
    speakStreaming,
    flushRemaining,
    stop,
    prepareForUserGesture,
    normalizeTextForSpeech,
  }
}
