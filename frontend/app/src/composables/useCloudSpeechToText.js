import { onUnmounted, ref } from 'vue'
import { transcribeInterviewSpeech } from '@/api/interview'

// 云端语音识别兜底：浏览器录音 -> 后端 -> 云端 ASR -> 文本。
// 只在浏览器 Web Speech 不可用时由协调器启用；本模块不感知面试其它逻辑。
const VOICE_ACTIVITY_THRESHOLD = 0.018
const VOICE_ACTIVITY_INTERVAL_MS = 120
// 一段话说完后的静音时长达到此值即切段上传，兼顾识别延迟与不打断正常停顿。
const SEGMENT_SILENCE_MS = 900
// 单段录音的兜底最大时长，避免用户长时间不停顿导致音频过大、上游超时。
const SEGMENT_MAX_DURATION_MS = 15000
const START_ERROR_MESSAGE = '云端语音识别启动失败，已降级为手动输入'
const PERMISSION_ERROR_MESSAGE = '麦克风权限被拒绝，已降级为手动输入'
const CAPTURE_ERROR_MESSAGE = '未检测到可用麦克风，已降级为手动输入'
const UPLOAD_ERROR_MESSAGE = '云端语音识别暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'

const getMediaRecorderConstructor = () => (typeof window !== 'undefined' ? window.MediaRecorder : null)

const resolveRecorderMimeType = () => {
  const MediaRecorderConstructor = getMediaRecorderConstructor()
  if (!MediaRecorderConstructor || typeof MediaRecorderConstructor.isTypeSupported !== 'function') {
    return { mimeType: '', extension: 'webm' }
  }
  const candidates = [
    { mimeType: 'audio/webm;codecs=opus', extension: 'webm' },
    { mimeType: 'audio/webm', extension: 'webm' },
    { mimeType: 'audio/ogg;codecs=opus', extension: 'ogg' },
    { mimeType: 'audio/mp4', extension: 'mp4' },
  ]
  const matched = candidates.find((candidate) => MediaRecorderConstructor.isTypeSupported(candidate.mimeType))
  return matched || { mimeType: '', extension: 'webm' }
}

/**
 * 云端语音识别 composable。
 * 暴露与 useSpeechToText 一致的接口，供协调器在浏览器识别不可用时透明替换。
 *
 * @param {Object} options
 * @param {import('vue').Ref<string>|string} options.sessionId - 面试会话 ID
 */
export function useCloudSpeechToText(options = {}) {
  const MediaRecorderConstructor = getMediaRecorderConstructor()
  const hasRecorderSupport = Boolean(
    MediaRecorderConstructor
    && typeof navigator !== 'undefined'
    && navigator.mediaDevices?.getUserMedia
  )

  // enabled 由协调器按后端能力 + 用户设置门控；未启用时 isSupported 为 false，协调器不会走云端。
  const enabled = ref(Boolean(options.enabled))
  const isSupported = ref(hasRecorderSupport && enabled.value)
  const isRecording = ref(false)
  const isVoiceActive = ref(false)
  const voiceActivityAt = ref(0)
  const finalTranscript = ref('')
  const interimTranscript = ref('')
  const error = ref('')
  const errorCode = ref('')
  const engineStatus = ref('cloud-service')
  const startConfirmed = ref(false)
  const language = ref('zh-CN')

  let mediaStream = null
  let audioContext = null
  let analyser = null
  let vadTimer = null
  let recorder = null
  let recorderChunks = []
  let segmentHasSpeech = false
  let silenceStartedAt = 0
  let segmentStartedAt = 0
  let recorderMime = resolveRecorderMimeType()
  let runId = 0
  let pendingUploads = []
  let segmentStopResolvers = []

  const resolveSessionId = () => {
    const raw = options.sessionId
    return raw && typeof raw === 'object' && 'value' in raw ? raw.value : raw
  }

  const setEnabled = (nextEnabled) => {
    enabled.value = Boolean(nextEnabled)
    isSupported.value = hasRecorderSupport && enabled.value
    if (!enabled.value) {
      cancel()
    }
  }

  const clearError = () => {
    error.value = ''
    errorCode.value = ''
  }

  const setErrorState = (message, code) => {
    error.value = message
    errorCode.value = code
  }

  const stopVadTimer = () => {
    if (vadTimer) {
      clearInterval(vadTimer)
      vadTimer = null
    }
  }

  const releaseAudioGraph = () => {
    stopVadTimer()
    if (mediaStream) {
      mediaStream.getTracks().forEach((track) => track.stop())
      mediaStream = null
    }
    if (audioContext) {
      audioContext.close?.()
      audioContext = null
    }
    analyser = null
    isVoiceActive.value = false
  }

  const flushSegmentStopResolvers = () => {
    const resolvers = segmentStopResolvers
    segmentStopResolvers = []
    resolvers.forEach((resolve) => resolve())
  }

  const uploadSegment = (blob, currentRunId) => {
    const sessionId = resolveSessionId()
    if (!sessionId || !blob || blob.size === 0) return
    // 逐段上传识别；单段失败不立即清空已识别文本，交给协调器/通话层按错误码决定是否降级。
    const uploadPromise = transcribeInterviewSpeech(sessionId, blob, {
      language: language.value,
      filename: `speech.${recorderMime.extension}`,
    })
      .then((text) => {
        if (currentRunId !== runId) return
        const normalized = (text || '').trim()
        if (normalized) {
          // 追加而非覆盖：与浏览器 finalTranscript 语义一致，通话层按增量构建 pendingMessage。
          finalTranscript.value = `${finalTranscript.value}${normalized}`
        }
      })
      .catch((uploadError) => {
        if (currentRunId !== runId) return
        console.warn('云端语音识别上传失败', uploadError)
        setErrorState(UPLOAD_ERROR_MESSAGE, 'network')
      })
      .finally(() => {
        pendingUploads = pendingUploads.filter((item) => item !== uploadPromise)
      })
    pendingUploads.push(uploadPromise)
  }

  const startSegmentRecorder = (currentRunId) => {
    if (currentRunId !== runId || !mediaStream) return
    recorderChunks = []
    segmentHasSpeech = false
    silenceStartedAt = 0
    segmentStartedAt = Date.now()
    try {
      recorder = recorderMime.mimeType
        ? new MediaRecorderConstructor(mediaStream, { mimeType: recorderMime.mimeType })
        : new MediaRecorderConstructor(mediaStream)
    } catch (recorderError) {
      console.warn('创建云端语音录制器失败', recorderError)
      setErrorState(START_ERROR_MESSAGE, 'start-failed')
      stopInternal(currentRunId)
      return
    }
    recorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) recorderChunks.push(event.data)
    }
    recorder.onstop = () => {
      const hadSpeech = segmentHasSpeech
      const chunks = recorderChunks
      recorderChunks = []
      if (hadSpeech && chunks.length > 0) {
        const blob = new Blob(chunks, recorderMime.mimeType ? { type: recorderMime.mimeType } : undefined)
        uploadSegment(blob, currentRunId)
      }
      // 仍在录音状态则开启下一段；stop()/cancel() 已置 isRecording=false，不再续录。
      if (currentRunId === runId && isRecording.value && mediaStream) {
        startSegmentRecorder(currentRunId)
      }
      flushSegmentStopResolvers()
    }
    try {
      recorder.start()
    } catch (startError) {
      console.warn('启动云端语音录制失败', startError)
      setErrorState(START_ERROR_MESSAGE, 'start-failed')
      stopInternal(currentRunId)
    }
  }

  // 主动切段：停止当前录制器触发 onstop 上传；返回 Promise 便于 stop() 等待尾段文本。
  const finalizeCurrentSegment = () => {
    if (!recorder || recorder.state === 'inactive') {
      return Promise.resolve()
    }
    const stopPromise = new Promise((resolve) => segmentStopResolvers.push(resolve))
    try {
      recorder.stop()
    } catch (stopError) {
      console.warn('停止云端语音录制失败', stopError)
      flushSegmentStopResolvers()
    }
    return stopPromise
  }

  const runVadTick = (samples, currentRunId) => {
    if (currentRunId !== runId || !analyser) return
    analyser.getByteTimeDomainData(samples)
    const rms = Math.sqrt(
      samples.reduce((sum, sample) => {
        const normalized = (sample - 128) / 128
        return sum + normalized * normalized
      }, 0) / samples.length
    )
    const active = rms >= VOICE_ACTIVITY_THRESHOLD
    isVoiceActive.value = active
    const now = Date.now()
    if (active) {
      voiceActivityAt.value = now
      segmentHasSpeech = true
      silenceStartedAt = 0
    } else if (segmentHasSpeech) {
      silenceStartedAt ||= now
    }
    const silenceElapsed = silenceStartedAt ? now - silenceStartedAt : 0
    const segmentElapsed = segmentStartedAt ? now - segmentStartedAt : 0
    const shouldCutBySilence = segmentHasSpeech && silenceElapsed >= SEGMENT_SILENCE_MS
    const shouldCutByDuration = segmentHasSpeech && segmentElapsed >= SEGMENT_MAX_DURATION_MS
    if ((shouldCutBySilence || shouldCutByDuration) && recorder && recorder.state === 'recording') {
      // 触发切段上传；onstop 会自动开启下一段录制。
      finalizeCurrentSegment()
    }
  }

  const startVadLoop = (currentRunId) => {
    const samples = new Uint8Array(analyser.fftSize)
    vadTimer = setInterval(() => runVadTick(samples, currentRunId), VOICE_ACTIVITY_INTERVAL_MS)
  }

  const stopInternal = (currentRunId) => {
    if (currentRunId !== runId) return
    isRecording.value = false
    startConfirmed.value = false
    releaseAudioGraph()
    recorder = null
  }

  const start = async (startOptions = {}) => {
    const waitForHealthyStart = Boolean(startOptions.waitForHealthyStart)
    if (!isSupported.value) {
      setErrorState(START_ERROR_MESSAGE, 'unsupported')
      return waitForHealthyStart ? { ok: false, code: 'unsupported' } : undefined
    }
    if (isRecording.value) {
      return waitForHealthyStart ? { ok: true, code: '' } : undefined
    }

    runId += 1
    const currentRunId = runId
    clearError()
    finalTranscript.value = ''
    interimTranscript.value = ''
    startConfirmed.value = false

    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext
    try {
      mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    } catch (mediaError) {
      const isPermission = mediaError?.name === 'NotAllowedError' || mediaError?.name === 'SecurityError'
      setErrorState(
        isPermission ? PERMISSION_ERROR_MESSAGE : CAPTURE_ERROR_MESSAGE,
        isPermission ? 'not-allowed' : 'audio-capture'
      )
      return waitForHealthyStart ? { ok: false, code: isPermission ? 'not-allowed' : 'audio-capture' } : undefined
    }

    if (currentRunId !== runId) {
      mediaStream?.getTracks?.().forEach((track) => track.stop())
      mediaStream = null
      return waitForHealthyStart ? { ok: false, code: 'cancelled' } : undefined
    }

    if (AudioContextConstructor) {
      try {
        audioContext = new AudioContextConstructor()
        analyser = audioContext.createAnalyser()
        analyser.fftSize = 1024
        audioContext.createMediaStreamSource(mediaStream).connect(analyser)
      } catch (audioGraphError) {
        console.warn('云端语音识别音量监测启动失败', audioGraphError)
        // 无音量监测时无法自动切段，直接判定启动失败并降级，避免录音永不上传。
        releaseAudioGraph()
        setErrorState(START_ERROR_MESSAGE, 'start-failed')
        return waitForHealthyStart ? { ok: false, code: 'start-failed' } : undefined
      }
    } else {
      releaseAudioGraph()
      setErrorState(START_ERROR_MESSAGE, 'start-failed')
      return waitForHealthyStart ? { ok: false, code: 'start-failed' } : undefined
    }

    recorderMime = resolveRecorderMimeType()
    isRecording.value = true
    startConfirmed.value = true
    startSegmentRecorder(currentRunId)
    startVadLoop(currentRunId)
    return waitForHealthyStart ? { ok: true, code: '' } : undefined
  }

  // 返回 Promise：等待尾段录音上传完成，确保通话层发送前拿到最后一句识别文本。
  const stop = () => {
    if (!isRecording.value) {
      return Promise.resolve()
    }
    isRecording.value = false
    startConfirmed.value = false
    stopVadTimer()
    const currentRunId = runId
    const finalizePromise = finalizeCurrentSegment()
    return finalizePromise
      .then(() => Promise.allSettled(pendingUploads))
      .finally(() => {
        if (currentRunId === runId) {
          releaseAudioGraph()
          recorder = null
        }
      })
  }

  const cancel = () => {
    runId += 1
    isRecording.value = false
    startConfirmed.value = false
    finalTranscript.value = ''
    interimTranscript.value = ''
    clearError()
    if (recorder && recorder.state !== 'inactive') {
      try {
        recorder.stop()
      } catch (stopError) {
        console.warn('取消云端语音录制失败', stopError)
      }
    }
    recorder = null
    recorderChunks = []
    pendingUploads = []
    flushSegmentStopResolvers()
    releaseAudioGraph()
  }

  onUnmounted(() => {
    cancel()
  })

  return {
    isSupported,
    isRecording,
    isVoiceActive,
    voiceActivityAt,
    finalTranscript,
    interimTranscript,
    error,
    errorCode,
    engineStatus,
    startConfirmed,
    language,
    setEnabled,
    start,
    stop,
    cancel,
  }
}
