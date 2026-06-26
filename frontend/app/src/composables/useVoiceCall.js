import { computed, nextTick, onUnmounted, ref, watch } from 'vue'

const DEFAULT_SILENCE_TIMEOUT_MS = 3000
const CHECK_INTERVAL_MS = 500
const MUTE_RESUME_MODE_AUTO = 'auto'
const TTS_RESUME_DELAY_MS = 800
const RECOVERABLE_SPEECH_RESTART_DELAY_MS = 600
const MAX_RECOVERABLE_SPEECH_RESTARTS = 2
const UNSUPPORTED_SPEECH_ERROR_MESSAGE = '当前浏览器不支持语音识别，已降级为手动输入'
const UNSUPPORTED_TTS_ERROR_MESSAGE = '当前浏览器不支持语音播报，已降级为手动输入'
const TEMPORARY_TEXT_FALLBACK_MESSAGE = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
const SHORT_RECOVERABLE_SPEECH_ERROR_CODES = new Set([
  'no-speech',
  'no-transcript',
])
const TEXT_FALLBACK_SPEECH_ERROR_CODES = new Set([
  'network',
  'service-not-allowed',
  'start-timeout',
  'end-without-result',
  'not-allowed',
  'audio-capture',
])
/**
 * 语音通话模式编排。
 * STT 可恢复中断会优先自动重启，不可恢复错误才退出语音模式；AI 播报期间暂停收音，播报结束后按当前规则恢复。
 */
export function useVoiceCall(options) {
  const isVoiceMode = ref(false)
  const isMuted = ref(false)
  const isListening = computed(() => Boolean(options.speech.isRecording.value))
  const isAiAudioActive = computed(() => Boolean(options.textToSpeech.isActive?.value ?? options.textToSpeech.isSpeaking.value))
  const isAiSpeaking = computed(() => Boolean(isAiAudioActive.value || options.isReplying?.value))
  const callDuration = ref(0)
  const pendingMessage = ref('')
  const error = ref('')
  const isManualResumePending = ref(false)
  const isTextFallbackMode = ref(false)
  const speechFallbackReason = ref('')

  let durationTimer = null
  let silenceTimer = null
  let lastSpeechAt = 0
  let lastFinal = ''
  let lastInterim = ''
  let pendingFinalText = ''
  let pendingInterimText = ''
  let isInitialListeningDeferred = false
  let isAutoSending = false
  // ttsWasActive: TTS 播报期间同步设置，确保 resumeListening 在播报结束后始终走延迟路径，
  // 避免 isSpeaking / isReplying 两个 watcher 竞态导致延迟失效。
  let ttsWasActive = false
  let ttsResumeTimer = null
  let recoverableSpeechRestartTimer = null
  let recoverableSpeechRestartCount = 0

  const silenceTimeoutMs = Number(options.silenceTimeoutMs ?? DEFAULT_SILENCE_TIMEOUT_MS)
  const muteResumeMode = options.muteResumeMode || MUTE_RESUME_MODE_AUTO

  const clearRecoverableSpeechRestartTimer = () => {
    if (recoverableSpeechRestartTimer) {
      clearTimeout(recoverableSpeechRestartTimer)
      recoverableSpeechRestartTimer = null
    }
  }

  const clearTimers = () => {
    if (durationTimer) {
      clearInterval(durationTimer)
      durationTimer = null
    }
    if (silenceTimer) {
      clearInterval(silenceTimer)
      silenceTimer = null
    }
    if (ttsResumeTimer) {
      clearTimeout(ttsResumeTimer)
      ttsResumeTimer = null
    }
    clearRecoverableSpeechRestartTimer()
  }

  const clearPendingTranscript = () => {
    pendingFinalText = ''
    pendingInterimText = ''
    pendingMessage.value = ''
    lastSpeechAt = 0
    lastFinal = options.speech.finalTranscript.value || ''
    lastInterim = options.speech.interimTranscript.value || ''
  }

  const resetSpeechState = () => {
    clearPendingTranscript()
    isManualResumePending.value = false
    isTextFallbackMode.value = false
    speechFallbackReason.value = ''
    isInitialListeningDeferred = false
    ttsWasActive = false
    isAutoSending = false
    recoverableSpeechRestartCount = 0
    clearRecoverableSpeechRestartTimer()
  }

  const pauseListeningForAi = () => {
    clearRecoverableSpeechRestartTimer()
    if (options.speech.isRecording.value) {
      void options.speech.stop?.()
    }
  }

  const flushListeningBeforeSend = () => {
    if (!options.speech.isRecording.value) return null
    const stopResult = options.speech.stop?.()
    if (!stopResult || typeof stopResult.then !== 'function') return null
    return stopResult.then(() => nextTick())
  }

  const resumeListening = (resumeOptions = {}) => {
    if (
      !isVoiceMode.value
      || isTextFallbackMode.value
      || isMuted.value
      || options.isReplying?.value
      || isAiAudioActive.value
    ) {
      return
    }
    // TTS 刚结束时必须延迟恢复收音，避免麦克风拾取扬声器的尾音 / 回声。
    // ttsWasActive 在 TTS 开始时同步设置（isSpeaking watcher speaking=true 分支），
    // 无论哪个 watcher 先触发 resumeListening，延迟保护始终生效。
    if (ttsWasActive) {
      if (!ttsResumeTimer) {
        ttsResumeTimer = setTimeout(() => {
          ttsResumeTimer = null
          ttsWasActive = false
          resumeListening()
        }, TTS_RESUME_DELAY_MS)
      }
      return
    }
    isInitialListeningDeferred = false
    if (!resumeOptions.preserveTranscript) {
      // 清理 TTS 播报期间可能残留的误识别文本，防止复读；STT 自动恢复时会保留候选文本，避免用户刚说出的内容丢失。
      pendingFinalText = ''
      pendingInterimText = ''
      pendingMessage.value = ''
      lastSpeechAt = 0
      lastFinal = options.speech.finalTranscript.value || ''
      lastInterim = options.speech.interimTranscript.value || ''
    }
    if (!options.speech.isSupported.value) {
      error.value = UNSUPPORTED_SPEECH_ERROR_MESSAGE
      return
    }
    if (!options.speech.isRecording.value) {
      options.speech.start?.()
    }
  }

  const enterTextFallbackMode = (reason) => {
    clearRecoverableSpeechRestartTimer()
    isManualResumePending.value = false
    isTextFallbackMode.value = true
    speechFallbackReason.value = reason || TEMPORARY_TEXT_FALLBACK_MESSAGE
    error.value = speechFallbackReason.value
    options.speech.stop?.()
  }

  const leaveTextFallbackMode = () => {
    isTextFallbackMode.value = false
    speechFallbackReason.value = ''
    error.value = ''
  }

  async function retrySpeechNow() {
    if (!isVoiceMode.value || !isTextFallbackMode.value) return false
    if (isMuted.value || options.isReplying?.value || isAiAudioActive.value) {
      return false
    }
    if (!options.speech.isSupported.value) {
      error.value = UNSUPPORTED_SPEECH_ERROR_MESSAGE
      return false
    }

    const startResult = options.speech.start?.({ waitForHealthyStart: true })
    if (startResult && typeof startResult.then === 'function') {
      const healthResult = await startResult
      if (healthResult?.ok) {
        leaveTextFallbackMode()
        return true
      }
      if (healthResult && healthResult.ok === false) {
        return false
      }
    }
    // 兼容旧测试桩或未来轻量实现：没有返回健康探测结果时，才回退到已确认启动状态。
    if (options.speech.isRecording.value && (!options.speech.startConfirmed || options.speech.startConfirmed.value)) {
      leaveTextFallbackMode()
      return true
    }

    return false
  }

  const scheduleRecoverableSpeechRestart = () => {
    clearRecoverableSpeechRestartTimer()
    isInitialListeningDeferred = false
    lastSpeechAt = Date.now()
    if (recoverableSpeechRestartCount >= MAX_RECOVERABLE_SPEECH_RESTARTS) {
      // 连续短恢复失败后切入正式文本兜底，让面试继续进行，并交给后台退避探测恢复语音。
      enterTextFallbackMode(error.value || TEMPORARY_TEXT_FALLBACK_MESSAGE)
      return
    }
    recoverableSpeechRestartCount += 1
    isManualResumePending.value = false
    recoverableSpeechRestartTimer = setTimeout(() => {
      recoverableSpeechRestartTimer = null
      if (!isVoiceMode.value || isMuted.value || options.isReplying?.value || isAiAudioActive.value) {
        return
      }
      error.value = ''
      resumeListening({ preserveTranscript: true })
    }, RECOVERABLE_SPEECH_RESTART_DELAY_MS)
  }

  const autoSendTranscript = async () => {
    if (isAutoSending) return false
    if (options.isReplying?.value) return false
    isAutoSending = true
    try {
      clearRecoverableSpeechRestartTimer()
      if (pendingMessage.value.trim() || lastSpeechAt) {
        const flushPromise = flushListeningBeforeSend()
        if (flushPromise) {
          await flushPromise
        }
      }
      const text = pendingMessage.value.trim()
      if (!text || options.isReplying?.value) return false

      pendingFinalText = ''
      pendingInterimText = ''
      pendingMessage.value = ''
      await options.onSend(text)
      return true
    } finally {
      isAutoSending = false
    }
  }

  const stopListeningAndSend = async () => {
    if (!isVoiceMode.value || options.isReplying?.value || isAiAudioActive.value) {
      return false
    }
    return autoSendTranscript()
  }

  const checkSilence = () => {
    if (!isVoiceMode.value || isTextFallbackMode.value || options.isReplying?.value || isAiAudioActive.value) return
    if (
      !options.speech.isRecording.value &&
      !isMuted.value &&
      !isManualResumePending.value &&
      !isInitialListeningDeferred &&
      !recoverableSpeechRestartTimer
    ) {
      resumeListening({ preserveTranscript: Boolean(pendingMessage.value.trim()) })
    }
    if (options.speech.isVoiceActive?.value) {
      lastSpeechAt = Date.now()
      return
    }
    if (!silenceTimeoutMs || !lastSpeechAt) return
    if (!pendingMessage.value.trim()) return
    if (Date.now() - lastSpeechAt >= silenceTimeoutMs) {
      autoSendTranscript()
    }
  }

  const startVoiceCall = (startOptions = {}) => {
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
    isInitialListeningDeferred = startOptions.startListening === false
    clearTimers()
    durationTimer = setInterval(() => {
      callDuration.value += 1
    }, 1000)
    silenceTimer = setInterval(checkSilence, CHECK_INTERVAL_MS)
    // 首轮需要先播报开场白时，允许页面进入通话态但暂不开麦，避免 STT 启动后立刻被 TTS 取消。
    if (!isInitialListeningDeferred) {
      resumeListening()
    }
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

  const toggleMute = () => {
    if (!isVoiceMode.value) return false
    if (!isMuted.value && isManualResumePending.value) {
      isManualResumePending.value = false
      recoverableSpeechRestartCount = 0
      resumeListening({ preserveTranscript: true })
      return false
    }
    isMuted.value = !isMuted.value
    if (isMuted.value) {
      clearRecoverableSpeechRestartTimer()
      void options.speech.stop?.()
      return true
    }
    if (muteResumeMode === MUTE_RESUME_MODE_AUTO) {
      resumeListening()
    } else {
      isManualResumePending.value = true
    }
    return false
  }

  watch(
    [options.speech.finalTranscript, options.speech.interimTranscript],
    ([nextFinal, nextInterim]) => {
      if (!isVoiceMode.value || options.isReplying?.value || isAiAudioActive.value) return
      const normalizedFinal = nextFinal || ''
      const normalizedInterim = nextInterim || ''
      const finalChanged = normalizedFinal !== lastFinal
      const interimChanged = normalizedInterim !== lastInterim
      if (!finalChanged && !interimChanged) return

      lastSpeechAt = Date.now()
      if (finalChanged) {
        const appendedText = normalizedFinal.startsWith(lastFinal)
          ? normalizedFinal.slice(lastFinal.length)
          : normalizedFinal
        if (appendedText.trim()) {
          pendingFinalText = `${pendingFinalText}${appendedText}`
        }
      }
      // 浏览器识别会持续返回 interim；自动静音提交临时展示/发送 interim，final 到达后再由最终文本接管。
      pendingInterimText = normalizedInterim
      pendingMessage.value = `${pendingFinalText}${pendingInterimText}`
      lastFinal = normalizedFinal
      lastInterim = normalizedInterim
      recoverableSpeechRestartCount = 0
      clearRecoverableSpeechRestartTimer()
      isManualResumePending.value = false
      error.value = ''
    }
  )

  watch(options.speech.voiceActivityAt || ref(0), (nextActivityAt) => {
    if (!isVoiceMode.value || isMuted.value || options.isReplying?.value || isAiAudioActive.value) return
    if (nextActivityAt) {
      lastSpeechAt = nextActivityAt
    }
  })

  watch(isAiAudioActive, (active) => {
    if (!isVoiceMode.value) return
    if (active) {
      // TTS 合成准备或真实播报期间都要同步设置 flag，确保后续所有 resumeListening 路径都走延迟。
      ttsWasActive = true
      pauseListeningForAi()
      return
    }
    if (isTextFallbackMode.value) {
      // 已降级到文本后不再后台探测恢复，避免 AI 回复结束后未经用户选择自动切回语音。
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
      if (isTextFallbackMode.value) {
        // 文本降级只允许用户点击“重试语音”恢复，AI 回复完成不能主动切回语音。
        return
      }
      resumeListening()
    }
  )

  watch(options.speech.error, (nextError) => {
    if (!nextError || !isVoiceMode.value) return
    error.value = nextError
    const speechErrorCode = options.speech.errorCode?.value || ''
    if (TEXT_FALLBACK_SPEECH_ERROR_CODES.has(speechErrorCode)) {
      enterTextFallbackMode(nextError)
      return
    }
    if (SHORT_RECOVERABLE_SPEECH_ERROR_CODES.has(speechErrorCode)) {
      // 浏览器 Web Speech 偶发无结果/短暂中断时先自动重启识别；连续失败再暴露手动继续收音。
      scheduleRecoverableSpeechRestart()
      return
    }
    enterTextFallbackMode(nextError)
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
    isManualResumePending,
    isTextFallbackMode,
    speechFallbackReason,
    startVoiceCall,
    endVoiceCall,
    toggleMute,
    resumeListening,
    retrySpeechNow,
    clearPendingTranscript,
    autoSendTranscript,
    stopListeningAndSend,
  }
}
