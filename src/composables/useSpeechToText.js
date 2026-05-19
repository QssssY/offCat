import { onUnmounted, ref } from 'vue'

const VOICE_ACTIVITY_THRESHOLD = 0.018
const VOICE_ACTIVITY_INTERVAL_MS = 120
const NO_TRANSCRIPT_TIMEOUT_MS = 6000
const UNSUPPORTED_RECOGNITION_ERROR_MESSAGE = '当前浏览器不支持语音识别，已降级为手动输入'
const NETWORK_RECOGNITION_ERROR_MESSAGE = '当前浏览器语音识别服务不可用，已降级为手动输入；可切换 Edge 或检查网络后重试'
const MICROPHONE_PERMISSION_ERROR_MESSAGE = '麦克风权限被拒绝，已降级为手动输入'
const AUDIO_CAPTURE_ERROR_MESSAGE = '未检测到可用麦克风，已降级为手动输入'
const START_RECOGNITION_ERROR_MESSAGE = '启动语音识别失败，已降级为手动输入'
const NO_SPEECH_ERROR_MESSAGE = '未识别到有效语音内容，已降级为手动输入。错误码：no-speech'
const NO_TRANSCRIPT_ERROR_MESSAGE = '检测到麦克风输入，但浏览器未返回识别文字，已降级为手动输入。错误码：no-transcript'
const RECOGNITION_ENDED_WITHOUT_RESULT_MESSAGE = '语音识别已结束但未返回文字，已降级为手动输入。错误码：end-without-result'

export function useSpeechToText() {
  const isSupported = ref(false)
  const isRecording = ref(false)
  const isVoiceActive = ref(false)
  const voiceActivityAt = ref(0)
  const finalTranscript = ref('')
  const interimTranscript = ref('')
  const error = ref('')
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

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  isSupported.value = !!SpeechRecognition

  const clearState = () => {
    finalTranscript.value = ''
    interimTranscript.value = ''
    error.value = ''
  }

  const cleanupRecognition = () => {
    if (!recognition) return
    recognition.onresult = null
    recognition.onerror = null
    recognition.onend = null
    recognition = null
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

  const stopWithError = (message) => {
    error.value = message
    isRecording.value = false
    try {
      recognition?.abort?.()
    } catch {}
    cleanupRecognition()
    cleanupVoiceActivity()
  }

  const startVoiceActivityMonitor = async () => {
    if (!navigator.mediaDevices?.getUserMedia || voiceActivityTimer) return

    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext
    if (!AudioContextConstructor) return

    // 用麦克风原始音量补充 Web Speech 文本回调，文本结果延迟时仍能判断用户正在说话。
    // 注意：mobile Safari/Chrome 在 autoplay 策略下 new AudioContext() 可能抛错，
    // 这里必须包 try/catch 并在失败时清理已申请的 mediaStream，避免留下半初始化状态。
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    try {
      audioContext = new AudioContextConstructor()
      analyser = audioContext.createAnalyser()
      analyser.fftSize = 1024
      audioContext.createMediaStreamSource(mediaStream).connect(analyser)
    } catch (audioContextError) {
      cleanupVoiceActivity()
      throw audioContextError
    }

    const samples = new Uint8Array(analyser.fftSize)
    voiceActivityTimer = setInterval(() => {
      analyser.getByteTimeDomainData(samples)
      const rms = Math.sqrt(
        samples.reduce((sum, sample) => {
          const normalized = (sample - 128) / 128
          return sum + normalized * normalized
        }, 0) / samples.length
      )
      const active = rms >= VOICE_ACTIVITY_THRESHOLD
      isVoiceActive.value = active
      if (active) {
        voiceActivityAt.value = Date.now()
        if (!hasTranscriptResult) {
          voiceActivityStartedAt ||= voiceActivityAt.value
          if (voiceActivityAt.value - voiceActivityStartedAt >= NO_TRANSCRIPT_TIMEOUT_MS) {
            // 麦克风有声音但 Web Speech 长时间没有任何文本回调时，明确降级，避免用户以为还在识别。
            stopWithError(NO_TRANSCRIPT_ERROR_MESSAGE)
          }
        }
      }
    }, VOICE_ACTIVITY_INTERVAL_MS)
  }

  const start = async () => {
    if (!SpeechRecognition) {
      error.value = UNSUPPORTED_RECOGNITION_ERROR_MESSAGE
      return
    }
    if (isRecording.value || isStarting) return

    clearState()
    ignoreResults = false
    isStarting = true
    hasTranscriptResult = false
    voiceActivityStartedAt = 0

    try {
      await startVoiceActivityMonitor()
    } catch {
      error.value = MICROPHONE_PERMISSION_ERROR_MESSAGE
      cleanupVoiceActivity()
      isStarting = false
      return
    }

    recognition = new SpeechRecognition()
    recognition.lang = language.value
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onresult = (event) => {
      if (ignoreResults) return
      hasTranscriptResult = true

      let final = ''
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i]
        if (result.isFinal) {
          final += result[0].transcript
        } else {
          interim += result[0].transcript
        }
      }

      if (final) {
        finalTranscript.value += final
      }
      interimTranscript.value = interim
    }

    recognition.onerror = (event) => {
      if (ignoreResults) return
      if (event.error === 'aborted') {
        // aborted 通常来自浏览器主动中断或调用 abort，避免把取消动作提示成识别失败。
        isRecording.value = false
        cleanupRecognition()
        cleanupVoiceActivity()
        return
      }

      if (event.error === 'network') {
        // network 表示浏览器识别服务不可用，文字输入和语音通话都需要明确提示用户。
        error.value = NETWORK_RECOGNITION_ERROR_MESSAGE
      } else {
        if (event.error === 'no-speech') {
          error.value = NO_SPEECH_ERROR_MESSAGE
        } else if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
          error.value = MICROPHONE_PERMISSION_ERROR_MESSAGE
        } else if (event.error === 'audio-capture') {
          error.value = AUDIO_CAPTURE_ERROR_MESSAGE
        } else {
          // 未覆盖的 Web Speech 错误统一显式降级，避免用户误以为系统仍在持续识别。
          error.value = `语音识别不可用，已降级为手动输入: ${event.error}`
        }
      }
      isRecording.value = false
      cleanupRecognition()
      cleanupVoiceActivity()
    }

    recognition.onend = () => {
      if (!ignoreResults && !hasTranscriptResult && isRecording.value) {
        stopWithError(RECOGNITION_ENDED_WITHOUT_RESULT_MESSAGE)
        return
      }
      isRecording.value = false
      cleanupRecognition()
      cleanupVoiceActivity()
    }

    try {
      recognition.start()
      isRecording.value = true
    } catch {
      error.value = START_RECOGNITION_ERROR_MESSAGE
      cleanupRecognition()
      cleanupVoiceActivity()
    }
    isStarting = false
  }

  const stop = () => {
    if (recognition && isRecording.value) {
      try {
        recognition.stop()
      } finally {
        // 用户点击关闭麦克风时立即释放本地资源，避免浏览器 onend 延迟导致界面仍显示录音中。
        isRecording.value = false
        cleanupRecognition()
      }
    }
    cleanupVoiceActivity()
  }

  const cancel = () => {
    ignoreResults = true
    isStarting = false
    isRecording.value = false
    clearState()
    cleanupVoiceActivity()

    if (!recognition) return

    try {
      recognition.abort()
    } catch {}

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
    language,
    start,
    stop,
    cancel,
    toggle,
  }
}
