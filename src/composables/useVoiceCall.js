import { computed, onUnmounted, ref, watch } from 'vue'

const DEFAULT_SILENCE_TIMEOUT_MS = 3000
const CHECK_INTERVAL_MS = 500
const UNSUPPORTED_SPEECH_ERROR_MESSAGE = '当前浏览器不支持语音识别，已降级为手动输入'
const UNSUPPORTED_TTS_ERROR_MESSAGE = '当前浏览器不支持语音播报，已降级为手动输入'

/**
 * 语音通话模式编排。
 * STT 底层复用现有 useSpeechToText，TTS 由外部传入；静音时只暂停收音，不退出通话。
 */
export function useVoiceCall(options) {
  const isVoiceMode = ref(false)
  const isMuted = ref(false)
  const isListening = computed(() => Boolean(options.speech.isRecording.value))
  const isAiSpeaking = computed(() => Boolean(options.textToSpeech.isSpeaking.value || options.isReplying?.value))
  const callDuration = ref(0)
  const pendingMessage = ref('')
  const error = ref('')

  let durationTimer = null
  let silenceTimer = null
  let lastSpeechAt = 0
  let lastFinal = ''
  let lastInterim = ''

  const silenceTimeoutMs = options.silenceTimeoutMs || DEFAULT_SILENCE_TIMEOUT_MS

  const clearTimers = () => {
    if (durationTimer) {
      clearInterval(durationTimer)
      durationTimer = null
    }
    if (silenceTimer) {
      clearInterval(silenceTimer)
      silenceTimer = null
    }
  }

  const resetSpeechState = () => {
    pendingMessage.value = ''
    lastSpeechAt = 0
    lastFinal = options.speech.finalTranscript.value || ''
    lastInterim = options.speech.interimTranscript.value || ''
  }

  const pauseListeningForAi = () => {
    if (options.speech.isRecording.value) {
      options.speech.stop?.()
    }
  }

  const resumeListening = () => {
    if (!isVoiceMode.value || isMuted.value || options.isReplying?.value || options.textToSpeech.isSpeaking.value) {
      return
    }
    if (!options.speech.isSupported.value) {
      error.value = UNSUPPORTED_SPEECH_ERROR_MESSAGE
      return
    }
    if (!options.speech.isRecording.value) {
      options.speech.start?.()
    }
  }

  const autoSendTranscript = async () => {
    const text = pendingMessage.value.trim()
    if (!text || options.isReplying?.value) return false

    pendingMessage.value = ''
    pauseListeningForAi()
    await options.onSend(text)
    return true
  }

  const stopListeningAndSend = async () => {
    if (!isVoiceMode.value || options.isReplying?.value || options.textToSpeech.isSpeaking.value) {
      return false
    }
    // 手动停止只提交本轮已识别文本，不改变静音状态，避免把“提交回答”误处理成“暂停通话”。
    return autoSendTranscript()
  }

  const checkSilence = () => {
    if (!isVoiceMode.value || options.isReplying?.value || options.textToSpeech.isSpeaking.value) return
    if (!options.speech.isRecording.value && !isMuted.value) {
      resumeListening()
    }
    // 人声活动优先于文本结果刷新静音计时，避免识别文本尚未回调时误判用户已停止说话。
    if (options.speech.isVoiceActive?.value) {
      lastSpeechAt = Date.now()
      return
    }
    if (!pendingMessage.value.trim() || !lastSpeechAt) return
    if (Date.now() - lastSpeechAt >= silenceTimeoutMs) {
      autoSendTranscript()
    }
  }

  const startVoiceCall = () => {
    error.value = ''
    if (!options.speech.isSupported.value) {
      error.value = UNSUPPORTED_SPEECH_ERROR_MESSAGE
      return false
    }
    if (!options.textToSpeech.isSupported.value) {
      error.value = UNSUPPORTED_TTS_ERROR_MESSAGE
      return false
    }

    isVoiceMode.value = true
    isMuted.value = false
    callDuration.value = 0
    resetSpeechState()
    clearTimers()
    durationTimer = setInterval(() => {
      callDuration.value += 1
    }, 1000)
    silenceTimer = setInterval(checkSilence, CHECK_INTERVAL_MS)
    resumeListening()
    return true
  }

  const endVoiceCall = () => {
    isVoiceMode.value = false
    isMuted.value = false
    clearTimers()
    resetSpeechState()
    options.speech.cancel?.()
    options.textToSpeech.stop()
  }

  /**
   * 切换静音状态。
   * 返回值表示切换后是否处于静音 (true=已静音, false=未静音 / 未进入通话)，
   * 这并非"操作是否成功"。调用方据此渲染麦克风图标与"已静音/已取消静音"toast。
   * @returns {boolean} 切换后的 isMuted 状态
   */
  const toggleMute = () => {
    if (!isVoiceMode.value) {
      return false
    }
    isMuted.value = !isMuted.value
    if (isMuted.value) {
      options.speech.stop?.()
      return true
    }
    resumeListening()
    return false
  }

  watch(
    [options.speech.finalTranscript, options.speech.interimTranscript],
    ([nextFinal, nextInterim]) => {
      if (!isVoiceMode.value || options.isReplying?.value || options.textToSpeech.isSpeaking.value) {
        return
      }
      const finalChanged = nextFinal !== lastFinal
      const interimChanged = nextInterim !== lastInterim
      if (!finalChanged && !interimChanged) return

      lastSpeechAt = Date.now()
      if (finalChanged) {
        // Web Speech 的累计转写可能在英文单词边界包含空格，发送前再统一 trim。
        const appendedText = nextFinal.slice(lastFinal.length)
        if (appendedText.trim()) {
          pendingMessage.value = `${pendingMessage.value}${appendedText}`
        }
      }
      lastFinal = nextFinal || ''
      lastInterim = nextInterim || ''
    }
  )

  watch(options.speech.voiceActivityAt || ref(0), (nextActivityAt) => {
    if (!isVoiceMode.value || isMuted.value || options.isReplying?.value || options.textToSpeech.isSpeaking.value) {
      return
    }
    if (nextActivityAt) {
      lastSpeechAt = nextActivityAt
    }
  })

  watch(options.textToSpeech.isSpeaking, (speaking) => {
    if (!isVoiceMode.value) return
    if (speaking) {
      pauseListeningForAi()
      return
    }
    resumeListening()
  })

  watch(
    () => options.isReplying?.value,
    (replying) => {
      if (!isVoiceMode.value) return
      if (replying) {
        pauseListeningForAi()
        return
      }
      resumeListening()
    }
  )

  watch(options.speech.error, (nextError) => {
    if (!nextError || !isVoiceMode.value) return
    error.value = nextError
    endVoiceCall()
  })

  onUnmounted(() => {
    endVoiceCall()
  })

  return {
    isVoiceMode,
    isMuted,
    isListening,
    isAiSpeaking,
    callDuration,
    pendingMessage,
    error,
    startVoiceCall,
    endVoiceCall,
    toggleMute,
    resumeListening,
    autoSendTranscript,
    stopListeningAndSend,
  }
}
