import { onUnmounted, ref } from 'vue'
import {
  detectSpeechRecognitionCapability,
  getSpeechRecognitionConstructor,
  SPEECH_RECOGNITION_CAPABILITY_STATUS,
} from '@/utils/speechRecognitionCapability'

const VOICE_ACTIVITY_THRESHOLD = 0.018
const VOICE_ACTIVITY_INTERVAL_MS = 120
const NO_TRANSCRIPT_TIMEOUT_MS = 6000
const START_HEALTH_TIMEOUT_MS = 2000
const HEALTHY_START_OBSERVATION_MS = 1000
const FIRST_EFFECTIVE_EVENT_TIMEOUT_MS = 6000
const UNSUPPORTED_RECOGNITION_ERROR_MESSAGE = '当前浏览器不支持语音识别，已降级为手动输入'
const NETWORK_RECOGNITION_ERROR_MESSAGE = '当前浏览器语音识别服务不可用，已降级为手动输入'
const TEMPORARILY_UNAVAILABLE_ERROR_MESSAGE = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
const MICROPHONE_PERMISSION_ERROR_MESSAGE = '麦克风权限被拒绝，已降级为手动输入'
const AUDIO_CAPTURE_ERROR_MESSAGE = '未检测到可用麦克风，已降级为手动输入'
const START_RECOGNITION_ERROR_MESSAGE = '启动语音识别失败，已降级为手动输入'
const NO_SPEECH_ERROR_MESSAGE = '未识别到有效语音内容，已降级为手动输入。错误码：no-speech'
const NO_TRANSCRIPT_ERROR_MESSAGE = '检测到麦克风输入，但浏览器未返回识别文字，已降级为手动输入。错误码：no-transcript'
const RECOGNITION_ENDED_WITHOUT_RESULT_MESSAGE = '语音识别已结束但未返回文字，已降级为手动输入。错误码：end-without-result'

export function useSpeechToText() {
  const SpeechRecognition = getSpeechRecognitionConstructor()
  const hasBrowserRecognition = () => Boolean(SpeechRecognition)

  const isSupported = ref(hasBrowserRecognition())
  const isRecording = ref(false)
  const isVoiceActive = ref(false)
  const voiceActivityAt = ref(0)
  const finalTranscript = ref('')
  const interimTranscript = ref('')
  const error = ref('')
  const errorCode = ref('')
  const engineStatus = ref(isSupported.value ? 'browser-service' : 'unsupported')
  const startConfirmed = ref(false)
  const capabilityStatus = ref(
    isSupported.value
      ? SPEECH_RECOGNITION_CAPABILITY_STATUS.WEBSPEECH_READY
      : SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED
  )
  const language = ref('zh-CN')

  let recognition = null
  let ignoreResults = false
  let isStarting = false
  let mediaStream = null
  let audioContext = null
  let analyser = null
  let voiceActivityTimer = null
  let voiceActivityStartedAt = 0
  let hasTranscriptResult = false
  let startRunId = 0
  let startHealthTimer = null
  let firstEffectiveEventTimer = null
  let healthyStartProbe = null

  const clearState = () => {
    finalTranscript.value = ''
    interimTranscript.value = ''
    error.value = ''
    errorCode.value = ''
    if (isSupported.value && engineStatus.value === 'unavailable') {
      engineStatus.value = 'browser-service'
    }
    if (capabilityStatus.value === SPEECH_RECOGNITION_CAPABILITY_STATUS.TEMPORARILY_UNAVAILABLE) {
      capabilityStatus.value = SPEECH_RECOGNITION_CAPABILITY_STATUS.WEBSPEECH_READY
    }
  }

  const setErrorState = (
    message,
    code,
    status = SPEECH_RECOGNITION_CAPABILITY_STATUS.TEMPORARILY_UNAVAILABLE
  ) => {
    error.value = message
    errorCode.value = code
    engineStatus.value = code === 'unsupported' ? 'unsupported' : 'unavailable'
    capabilityStatus.value = status
  }

  const clearStartHealthTimer = () => {
    if (!startHealthTimer) return
    clearTimeout(startHealthTimer)
    startHealthTimer = null
  }

  const clearFirstEffectiveEventTimer = () => {
    if (!firstEffectiveEventTimer) return
    clearTimeout(firstEffectiveEventTimer)
    firstEffectiveEventTimer = null
  }

  const resolveHealthyStartProbe = (ok, code = '') => {
    if (!healthyStartProbe) return
    const currentProbe = healthyStartProbe
    healthyStartProbe = null
    if (currentProbe.observationTimer) {
      clearTimeout(currentProbe.observationTimer)
    }
    currentProbe.resolve({ ok, code })
  }

  const createHealthyStartProbe = () => {
    resolveHealthyStartProbe(false, 'cancelled')
    return new Promise((resolve) => {
      healthyStartProbe = {
        resolve,
        observationTimer: null,
        observing: false,
      }
    })
  }

  const beginHealthyStartObservation = () => {
    if (!healthyStartProbe || healthyStartProbe.observing) return
    healthyStartProbe.observing = true
    // 恢复探测不能只看 recognition.start() 是否被浏览器接受；必须等 onstart 后再观察短窗口，避免 UI 误报“语音已恢复”。
    healthyStartProbe.observationTimer = setTimeout(() => {
      resolveHealthyStartProbe(true, '')
    }, HEALTHY_START_OBSERVATION_MS)
  }

  const markFirstEffectiveRecognitionEvent = () => {
    clearFirstEffectiveEventTimer()
    resolveHealthyStartProbe(true, '')
  }

  const scheduleFirstEffectiveEventTimeout = (currentStartRunId) => {
    clearFirstEffectiveEventTimer()
    firstEffectiveEventTimer = setTimeout(() => {
      if (
        currentStartRunId !== startRunId
        || ignoreResults
        || !recognition
        || !isRecording.value
      ) {
        return
      }
      stopWithError(TEMPORARILY_UNAVAILABLE_ERROR_MESSAGE, 'start-timeout')
    }, FIRST_EFFECTIVE_EVENT_TIMEOUT_MS)
  }

  const scheduleStartHealthTimeout = (currentStartRunId) => {
    clearStartHealthTimer()
    startHealthTimer = setTimeout(() => {
      if (
        currentStartRunId !== startRunId
        || ignoreResults
        || !recognition
        || !isRecording.value
      ) {
        return
      }
      stopWithError(TEMPORARILY_UNAVAILABLE_ERROR_MESSAGE, 'start-timeout')
    }, START_HEALTH_TIMEOUT_MS)
  }

  const cleanupRecognition = () => {
    if (!recognition) return
    clearStartHealthTimer()
    clearFirstEffectiveEventTimer()
    startConfirmed.value = false
    recognition.onstart = null
    recognition.onaudiostart = null
    recognition.onsoundstart = null
    recognition.onspeechstart = null
    recognition.onresult = null
    recognition.onerror = null
    recognition.onend = null
    recognition = null
  }

  const cleanupBeforeStart = () => {
    const staleRecognition = recognition
    resolveHealthyStartProbe(false, 'cancelled')
    cleanupRecognition()
    try {
      staleRecognition?.abort?.()
    } catch (abortError) {
      console.warn('重启浏览器语音识别前清理旧实例失败', abortError)
    }
    cleanupVoiceActivity()
  }

  const cleanupVoiceActivity = () => {
    if (voiceActivityTimer) {
      clearInterval(voiceActivityTimer)
      voiceActivityTimer = null
    }
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
    voiceActivityStartedAt = 0
  }

  const releaseVoiceActivityResources = (stream, context, source) => {
    source?.disconnect?.()
    stream?.getTracks?.().forEach((track) => track.stop())
    context?.close?.()
  }

  const stopWithError = (message, code = 'recognition-error') => {
    const status = code === 'not-allowed'
      ? SPEECH_RECOGNITION_CAPABILITY_STATUS.PERMISSION_BLOCKED
      : SPEECH_RECOGNITION_CAPABILITY_STATUS.TEMPORARILY_UNAVAILABLE
    setErrorState(message, code, status)
    isStarting = false
    isRecording.value = false
    startConfirmed.value = false
    resolveHealthyStartProbe(false, code)
    try {
      recognition?.abort?.()
    } catch (abortError) {
      console.warn('停止浏览器语音识别失败', abortError)
    }
    cleanupRecognition()
    cleanupVoiceActivity()
  }

  const updateCapability = async () => {
    const capability = await detectSpeechRecognitionCapability({ lang: language.value })
    capabilityStatus.value = capability.status
    isSupported.value = capability.status !== SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED
    return capability
  }

  const startVoiceActivityMonitor = async (shouldKeepMonitor = () => true) => {
    if (!navigator.mediaDevices?.getUserMedia || voiceActivityTimer) return

    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext
    if (!AudioContextConstructor) return

    let nextMediaStream = null
    let nextAudioContext = null
    let nextAnalyser = null
    let nextAudioSource = null

    try {
      nextAudioContext = new AudioContextConstructor()
      nextMediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      nextAnalyser = nextAudioContext.createAnalyser()
      nextAnalyser.fftSize = 1024
      nextAudioSource = nextAudioContext.createMediaStreamSource(nextMediaStream)
      nextAudioSource.connect(nextAnalyser)
    } catch (audioContextError) {
      releaseVoiceActivityResources(nextMediaStream, nextAudioContext, nextAudioSource)
      throw audioContextError
    }

    if (!shouldKeepMonitor() || voiceActivityTimer) {
      releaseVoiceActivityResources(nextMediaStream, nextAudioContext, nextAudioSource)
      return
    }

    mediaStream = nextMediaStream
    audioContext = nextAudioContext
    analyser = nextAnalyser

    const samples = new Uint8Array(nextAnalyser.fftSize)
    voiceActivityTimer = setInterval(() => {
      nextAnalyser.getByteTimeDomainData(samples)
      const rms = Math.sqrt(
        samples.reduce((sum, sample) => {
          const normalized = (sample - 128) / 128
          return sum + normalized * normalized
        }, 0) / samples.length
      )
      const active = rms >= VOICE_ACTIVITY_THRESHOLD
      isVoiceActive.value = active
      if (!active) return

      // 已确认有麦克风输入时，不再按“启动后完全无有效事件”处理，交给 no-transcript 分支判断浏览器是否返回文字。
      clearFirstEffectiveEventTimer()
      voiceActivityAt.value = Date.now()
      if (!hasTranscriptResult) {
        voiceActivityStartedAt ||= voiceActivityAt.value
        if (voiceActivityAt.value - voiceActivityStartedAt >= NO_TRANSCRIPT_TIMEOUT_MS) {
          stopWithError(NO_TRANSCRIPT_ERROR_MESSAGE, 'no-transcript')
        }
      }
    }, VOICE_ACTIVITY_INTERVAL_MS)
  }

  const startOptionalVoiceActivityMonitor = (currentStartRunId) => {
    void startVoiceActivityMonitor(() => (
      currentStartRunId === startRunId && !ignoreResults && isRecording.value && Boolean(recognition)
    )).catch((monitorError) => {
      // 浏览器 Web Speech 自己负责授权和识别；音量监测失败只影响无文字保护，不阻断主识别链路。
      console.warn('浏览器语音活动监测启动失败', monitorError)
      cleanupVoiceActivity()
    })
  }

  const start = async (startOptions = {}) => {
    const waitForHealthyStart = Boolean(startOptions.waitForHealthyStart)
    if (!SpeechRecognition) {
      isSupported.value = false
      setErrorState(
        UNSUPPORTED_RECOGNITION_ERROR_MESSAGE,
        'unsupported',
        SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED
      )
      return waitForHealthyStart ? { ok: false, code: 'unsupported' } : undefined
    }
    if (isRecording.value || isStarting) {
      return waitForHealthyStart
        ? { ok: Boolean(isRecording.value && startConfirmed.value), code: '' }
        : undefined
    }

    // Web Speech 在 Chrome/Edge 中可能残留旧 recognition 或麦克风监测资源；每次重启前先清干净，避免内部状态卡住。
    cleanupBeforeStart()
    clearState()
    ignoreResults = false
    isStarting = true
    startConfirmed.value = false
    startRunId += 1
    const currentStartRunId = startRunId
    hasTranscriptResult = false
    voiceActivityStartedAt = 0

    const capability = await updateCapability()
    if (capability.status === SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED) {
      setErrorState(
        UNSUPPORTED_RECOGNITION_ERROR_MESSAGE,
        'unsupported',
        SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED
      )
      isStarting = false
      return waitForHealthyStart ? { ok: false, code: 'unsupported' } : undefined
    }
    if (capability.status === SPEECH_RECOGNITION_CAPABILITY_STATUS.PERMISSION_BLOCKED) {
      setErrorState(
        MICROPHONE_PERMISSION_ERROR_MESSAGE,
        'not-allowed',
        SPEECH_RECOGNITION_CAPABILITY_STATUS.PERMISSION_BLOCKED
      )
      isStarting = false
      return waitForHealthyStart ? { ok: false, code: 'not-allowed' } : undefined
    }
    if (currentStartRunId !== startRunId || !isStarting || ignoreResults) {
      cleanupVoiceActivity()
      isStarting = false
      return waitForHealthyStart ? { ok: false, code: 'cancelled' } : undefined
    }
    const healthyStartPromise = waitForHealthyStart ? createHealthyStartProbe() : null

    recognition = new SpeechRecognition()
    engineStatus.value = 'browser-service'
    recognition.lang = language.value
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onstart = () => {
      if (ignoreResults) return
      startConfirmed.value = true
      clearStartHealthTimer()
      scheduleFirstEffectiveEventTimeout(currentStartRunId)
      beginHealthyStartObservation()
    }

    recognition.onaudiostart = () => {
      if (ignoreResults) return
      markFirstEffectiveRecognitionEvent()
    }

    recognition.onsoundstart = () => {
      if (ignoreResults) return
      markFirstEffectiveRecognitionEvent()
    }

    recognition.onspeechstart = () => {
      if (ignoreResults) return
      startConfirmed.value = true
      markFirstEffectiveRecognitionEvent()
    }

    recognition.onresult = (event) => {
      if (ignoreResults) return
      startConfirmed.value = true
      markFirstEffectiveRecognitionEvent()
      hasTranscriptResult = true
      let final = ''
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i]
        if (result.isFinal) final += result[0].transcript
        else interim += result[0].transcript
      }
      if (final) finalTranscript.value += final
      interimTranscript.value = interim
    }

    recognition.onerror = (event) => {
      if (ignoreResults) return
      clearFirstEffectiveEventTimer()
      if (event.error === 'aborted') {
        isRecording.value = false
        startConfirmed.value = false
        resolveHealthyStartProbe(false, 'aborted')
        cleanupRecognition()
        cleanupVoiceActivity()
        return
      }
      if (event.error === 'network') {
        setErrorState(NETWORK_RECOGNITION_ERROR_MESSAGE, 'network')
      } else if (event.error === 'no-speech') {
        setErrorState(NO_SPEECH_ERROR_MESSAGE, 'no-speech')
      } else if (event.error === 'service-not-allowed') {
        setErrorState(TEMPORARILY_UNAVAILABLE_ERROR_MESSAGE, 'service-not-allowed')
      } else if (event.error === 'not-allowed') {
        setErrorState(
          MICROPHONE_PERMISSION_ERROR_MESSAGE,
          'not-allowed',
          SPEECH_RECOGNITION_CAPABILITY_STATUS.PERMISSION_BLOCKED
        )
      } else if (event.error === 'audio-capture') {
        setErrorState(AUDIO_CAPTURE_ERROR_MESSAGE, 'audio-capture')
      } else {
        setErrorState(`语音识别不可用，已降级为手动输入: ${event.error}`, event.error || 'recognition-error')
      }
      isRecording.value = false
      startConfirmed.value = false
      resolveHealthyStartProbe(false, event.error || 'recognition-error')
      cleanupRecognition()
      cleanupVoiceActivity()
    }

    recognition.onend = () => {
      clearFirstEffectiveEventTimer()
      if (!ignoreResults && !hasTranscriptResult && isRecording.value) {
        stopWithError(RECOGNITION_ENDED_WITHOUT_RESULT_MESSAGE, 'end-without-result')
        return
      }
      resolveHealthyStartProbe(Boolean(hasTranscriptResult), hasTranscriptResult ? '' : 'end-without-result')
      isRecording.value = false
      startConfirmed.value = false
      cleanupRecognition()
      cleanupVoiceActivity()
    }

    try {
      recognition.start()
      isRecording.value = true
      scheduleStartHealthTimeout(currentStartRunId)
      startOptionalVoiceActivityMonitor(currentStartRunId)
    } catch (startError) {
      console.warn('启动浏览器语音识别失败', startError)
      setErrorState(START_RECOGNITION_ERROR_MESSAGE, 'start-failed')
      startConfirmed.value = false
      resolveHealthyStartProbe(false, 'start-failed')
      cleanupRecognition()
      cleanupVoiceActivity()
      isStarting = false
      return waitForHealthyStart ? { ok: false, code: 'start-failed' } : undefined
    }
    isStarting = false
    if (healthyStartPromise) {
      return healthyStartPromise
    }
    return undefined
  }

  const stop = () => {
    if (isStarting) {
      startRunId += 1
      isStarting = false
      resolveHealthyStartProbe(false, 'stopped')
      cleanupRecognition()
    }
    if (recognition && isRecording.value) {
      try {
        recognition.stop()
      } finally {
        isRecording.value = false
        startConfirmed.value = false
        resolveHealthyStartProbe(false, 'stopped')
        cleanupRecognition()
      }
    }
    isRecording.value = false
    startConfirmed.value = false
    resolveHealthyStartProbe(false, 'stopped')
    cleanupVoiceActivity()
  }

  const cancel = () => {
    startRunId += 1
    ignoreResults = true
    isStarting = false
    isRecording.value = false
    startConfirmed.value = false
    resolveHealthyStartProbe(false, 'cancelled')
    clearState()
    cleanupVoiceActivity()
    if (!recognition) return
    try {
      recognition.abort()
    } catch (abortError) {
      console.warn('取消浏览器语音识别失败', abortError)
    }
    cleanupRecognition()
  }

  const toggle = () => {
    isRecording.value ? stop() : start()
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
    capabilityStatus,
    language,
    start,
    stop,
    cancel,
    toggle,
  }
}
