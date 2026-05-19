import { onUnmounted, ref } from 'vue'

export function useSpeechToText() {
  const isSupported = ref(false)
  const isRecording = ref(false)
  const finalTranscript = ref('')
  const interimTranscript = ref('')
  const error = ref('')
  const language = ref('zh-CN')
  let recognition = null
  let ignoreResults = false

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

  const start = () => {
    if (!SpeechRecognition || isRecording.value) return

    clearState()
    ignoreResults = false

    recognition = new SpeechRecognition()
    recognition.lang = language.value
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onresult = (event) => {
      if (ignoreResults) return

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
      if (event.error === 'no-speech') return

      error.value = event.error === 'not-allowed'
        ? '麦克风权限被拒绝'
        : `语音识别错误: ${event.error}`
      isRecording.value = false
      cleanupRecognition()
    }

    recognition.onend = () => {
      isRecording.value = false
      cleanupRecognition()
    }

    try {
      recognition.start()
      isRecording.value = true
    } catch {
      error.value = '启动语音识别失败'
      cleanupRecognition()
    }
  }

  const stop = () => {
    if (recognition && isRecording.value) {
      recognition.stop()
    }
  }

  const cancel = () => {
    ignoreResults = true
    isRecording.value = false
    clearState()

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
