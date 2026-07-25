import { computed, onUnmounted, ref, unref } from 'vue'
import { synthesizeInterviewTts } from '@/api/interview'

const SENTENCE_END_REGEXP = /[。！？.!?]/
const FEEDBACK_BLOCK_REGEXP = /<FEEDBACK>[\s\S]*?<\/FEEDBACK>/gi
// 云端逐段合成有网络往返（每段约 1~3s）。若按每个句末标点切段，一句“。”就是一次请求，
// 短句播完而下一段还没合成回来就会卡顿（UI 反复跳“正在合成”）。
// 因此把连续短句合并成较大段：首段用较小阈值抢“首句尽快出声”，后续段用较大阈值让
// 单段音频足够长以掩盖下一段的合成延迟，从而消除断流。
const FIRST_SEGMENT_MIN_CHARS = 14
const SEGMENT_TARGET_CHARS = 56

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
  // 本轮播报是否已切出过至少一段：首段用较小阈值抢首句出声，之后切回较大阈值。
  let hasEmittedSegment = false

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
  // 播报音色随每次合成透传给后端；仅 EdgeTTS 且命中白名单时后端才会覆盖，其它情况后端忽略。
  const resolveVoiceId = () => unref(options.voiceId) || ''

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
        voiceId: resolveVoiceId(),
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
    // speak 是整句直接播报，视为已切段，后续 flush 走大段阈值。
    hasEmittedSegment = true
    enqueue(text, speechOptions)
  }

  // 目标切段长度：首段用较小阈值抢“首句尽快出声”，之后用较大阈值把连续短句合并成一段，
  // 让单段音频足够长以掩盖下一段的合成往返延迟，从而消除断流。
  const currentSegmentTarget = () => (hasEmittedSegment ? SEGMENT_TARGET_CHARS : FIRST_SEGMENT_MIN_CHARS)

  // 从 buffer 头部切出一段：累积完整句子直到达到目标长度，返回切出的段（不足目标且无更多完整句时返回 null）。
  const takeSegmentFromBuffer = (force) => {
    const chars = Array.from(buffer)
    let lastSentenceEnd = -1
    for (let i = 0; i < chars.length; i += 1) {
      if (SENTENCE_END_REGEXP.test(chars[i])) {
        lastSentenceEnd = i
        // 已累积到目标长度且落在句末，切到此处，避免继续吞并整段回复。
        if (i + 1 >= currentSegmentTarget()) {
          const segment = chars.slice(0, i + 1).join('').trim()
          buffer = chars.slice(i + 1).join('')
          return segment
        }
      }
    }
    // 无完整句：非强制时不切，等待更多 chunk 合并成大段。
    if (lastSentenceEnd === -1) {
      return force ? takeForcedSegment() : null
    }
    // 有完整句但未达目标长度：非强制时继续等待合并；强制（done/flush）时把已有完整句整体切出。
    if (!force) return null
    const segment = chars.slice(0, lastSentenceEnd + 1).join('').trim()
    buffer = chars.slice(lastSentenceEnd + 1).join('')
    return segment
  }

  // 强制切段兜底：done/flush 时把 buffer 剩余全部作为一段切出。
  const takeForcedSegment = () => {
    const segment = buffer.trim()
    buffer = ''
    return segment || null
  }

  const drainSegments = (force, speechOptions) => {
    while (true) {
      const segment = takeSegmentFromBuffer(force)
      if (!segment) break
      hasEmittedSegment = true
      enqueue(segment, speechOptions)
    }
  }

  const speakStreaming = (chunk, speechOptions = {}) => {
    if (!chunk || !isSupported.value) return
    buffer += String(chunk).replace(FEEDBACK_BLOCK_REGEXP, '')
    if (!buffer.trim()) return
    drainSegments(false, speechOptions)
  }

  const flushRemaining = (speechOptions = {}) => {
    if (!isSupported.value) return
    // done/flush：把 buffer 剩余全部切出播报，无残留时才判定本轮结束。
    drainSegments(true, speechOptions)
    if (!buffer.trim() && !isActive.value && queue.length === 0) {
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
    // 下一轮播报重新从首段小阈值开始，保证首句尽快出声。
    hasEmittedSegment = false
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
