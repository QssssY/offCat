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
  const isSpeaking = ref(false)
  const engineStatus = computed(() => (isSupported.value ? 'cloud-tts' : 'unavailable'))

  let buffer = ''
  let queue = []
  let activeAudio = null
  let activeObjectUrl = ''
  let activeAbortController = null
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
    if (activeAbortController) {
      activeAbortController.abort()
      activeAbortController = null
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
    if (queue.length > 0) {
      void playNext()
      return
    }
    isSpeaking.value = false
    options.onEnd?.(event)
  }

  const disableAndFallback = (failedItem, reason) => {
    const remainingText = [
      failedItem?.text || '',
      ...queue.map((item) => item.text),
      buffer.trim(),
    ].filter(Boolean).join('')
    queue = []
    buffer = ''
    isPlaying = false
    isSpeaking.value = false
    isSupported.value = false
    releaseActiveAudio()
    if (!fallbackNotified) {
      fallbackNotified = true
      options.onFallback?.({
        text: remainingText,
        reason,
        speechOptions: failedItem?.speechOptions || {},
      })
    }
  }

  async function playNext() {
    if (isPlaying || !queue.length || !isSupported.value) return
    const item = queue.shift()
    const sessionId = resolveSessionId()
    if (!sessionId) {
      disableAndFallback(item, new Error('TTS sessionId missing'))
      return
    }

    const currentRunId = runId
    isPlaying = true
    isSpeaking.value = true
    activeAbortController = new AbortController()

    try {
      const audioBlob = await synthesizeInterviewTts(sessionId, item.text, {
        signal: activeAbortController.signal,
      })
      if (currentRunId !== runId) return
      activeAbortController = null
      activeObjectUrl = URL.createObjectURL(audioBlob)
      activeAudio = new Audio(activeObjectUrl)
      activeAudio.onended = () => finishCurrentItem(currentRunId, item.speechOptions, {
        reason: 'end',
        text: item.text,
      })
      activeAudio.onerror = () => disableAndFallback(item, new Error('Cloud TTS audio playback failed'))
      item.speechOptions?.onStart?.({
        reason: 'start',
        started: true,
        text: item.text,
      })
      await activeAudio.play()
    } catch (error) {
      if (error?.name === 'AbortError') return
      if (currentRunId !== runId) return
      disableAndFallback(item, error)
    }
  }

  const enqueue = (text, speechOptions = {}) => {
    const normalizedText = normalizeTextForSpeech(text)
    if (!normalizedText || !isSupported.value) return
    queue.push({ text: normalizedText, speechOptions })
    isSpeaking.value = true
    void playNext()
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
    if (!isSpeaking.value) {
      options.onEnd?.()
    }
  }

  function stop() {
    runId += 1
    queue = []
    buffer = ''
    isPlaying = false
    isSpeaking.value = false
    releaseActiveAudio()
  }

  const prepareForUserGesture = () => {}

  onUnmounted(() => {
    stop()
  })

  return {
    isSupported,
    isSpeaking,
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
