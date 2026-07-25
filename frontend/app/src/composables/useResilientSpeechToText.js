import { onUnmounted, ref, watch } from 'vue'
import { useSpeechToText } from '@/composables/useSpeechToText'
import { useCloudSpeechToText } from '@/composables/useCloudSpeechToText'

// 浏览器 Web Speech 出现下列错误码时，说明“在录音却拿不到文字/服务不可用/无法启动”，
// 此时若云端识别可用则静默切到云端兜底，不把错误抛给通话层（避免直接降级手动输入）。
// 覆盖 Chrome/Edge 语音后端抖动时的启动失败（start-failed）与未归类识别错误（recognition-error），
// 确保浏览器识别反复罢工时能真正切到管理端配置的云端兜底，而不是停留在浏览器识别或手动输入。
// 不含 no-speech：它在用户正常停顿时也会触发，纳入会导致一次停顿就永久切到云端上传，过于激进。
const BROWSER_TO_CLOUD_SWITCH_CODES = new Set([
  'network',
  'service-not-allowed',
  'start-timeout',
  'end-without-result',
  'no-transcript',
  'start-failed',
  'recognition-error',
])

/**
 * 弹性语音识别协调器。
 * 对外暴露与 useSpeechToText 完全一致的接口，内部优先使用浏览器 Web Speech，
 * 浏览器不可用时透明切换到云端语音识别兜底。通话编排层（useVoiceCall）无需感知差异。
 *
 * @param {Object} options
 * @param {import('vue').Ref<string>|string} options.sessionId - 面试会话 ID
 * @param {boolean} [options.cloudEnabled] - 云端兜底是否启用（由后端能力 + 用户设置门控）
 */
export function useResilientSpeechToText(options = {}) {
  const browser = useSpeechToText()
  const cloud = useCloudSpeechToText({ sessionId: options.sessionId, enabled: Boolean(options.cloudEnabled) })

  // 统一对外的响应式状态，始终镜像当前激活引擎。
  const isSupported = ref(browser.isSupported.value || cloud.isSupported.value)
  const isRecording = ref(false)
  const isVoiceActive = ref(false)
  const voiceActivityAt = ref(0)
  const finalTranscript = ref('')
  const interimTranscript = ref('')
  const error = ref('')
  const errorCode = ref('')
  const engineStatus = ref(browser.isSupported.value ? 'browser-service' : (cloud.isSupported.value ? 'cloud-service' : 'unsupported'))
  const startConfirmed = ref(false)
  const language = ref('zh-CN')

  // 'browser' | 'cloud'：当前激活引擎。一旦本会话切到云端就保持云端，避免来回抖动。
  const activeEngine = ref(browser.isSupported.value ? 'browser' : 'cloud')
  let switchedToCloud = !browser.isSupported.value

  const cloudAvailable = () => Boolean(cloud.isSupported.value)

  const getActive = () => (activeEngine.value === 'cloud' ? cloud : browser)

  const syncFromActive = () => {
    const active = getActive()
    isRecording.value = active.isRecording.value
    isVoiceActive.value = active.isVoiceActive.value
    voiceActivityAt.value = active.voiceActivityAt.value
    finalTranscript.value = active.finalTranscript.value
    interimTranscript.value = active.interimTranscript.value
    startConfirmed.value = active.startConfirmed.value
    engineStatus.value = active.engineStatus.value
  }

  // 镜像激活引擎的状态到统一 ref；非激活引擎的变化忽略，避免串状态。
  const mirror = (source, engineName) => {
    watch(source.isRecording, (v) => { if (activeEngine.value === engineName) isRecording.value = v })
    watch(source.isVoiceActive, (v) => { if (activeEngine.value === engineName) isVoiceActive.value = v })
    watch(source.voiceActivityAt, (v) => { if (activeEngine.value === engineName) voiceActivityAt.value = v })
    watch(source.finalTranscript, (v) => { if (activeEngine.value === engineName) finalTranscript.value = v })
    watch(source.interimTranscript, (v) => { if (activeEngine.value === engineName) interimTranscript.value = v })
    watch(source.startConfirmed, (v) => { if (activeEngine.value === engineName) startConfirmed.value = v })
    watch(source.engineStatus, (v) => { if (activeEngine.value === engineName) engineStatus.value = v })
  }
  mirror(browser, 'browser')
  mirror(cloud, 'cloud')

  // 云端可用性变化时更新对外 isSupported（后端能力探测异步返回后可能从不可用变可用）。
  watch(cloud.isSupported, () => {
    isSupported.value = browser.isSupported.value || cloud.isSupported.value
  })

  // 语言双向传递到两个引擎。
  watch(language, (lang) => {
    browser.language.value = lang
    cloud.language.value = lang
  }, { immediate: true })

  const switchToCloud = async () => {
    if (switchedToCloud && activeEngine.value === 'cloud') return
    switchedToCloud = true
    // 清理浏览器引擎残留，切换激活源。
    browser.cancel?.()
    activeEngine.value = 'cloud'
    error.value = ''
    errorCode.value = ''
    engineStatus.value = 'cloud-service'
    // 立即在云端接续收音，保持通话不中断。
    await cloud.start()
    syncFromActive()
  }

  // 浏览器引擎错误：可切云端则静默切换，否则把错误透传给通话层按原有规则降级。
  watch(browser.error, (nextError) => {
    if (activeEngine.value !== 'browser' || !nextError) return
    const code = browser.errorCode.value || ''
    if (cloudAvailable() && BROWSER_TO_CLOUD_SWITCH_CODES.has(code)) {
      void switchToCloud()
      return
    }
    error.value = nextError
    errorCode.value = code
  })

  // 云端引擎错误：直接透传（通话层会按 network 等码进入文本兜底）。
  watch(cloud.error, (nextError) => {
    if (activeEngine.value !== 'cloud' || !nextError) return
    error.value = nextError
    errorCode.value = cloud.errorCode.value || ''
  })

  const start = (startOptions = {}) => {
    error.value = ''
    errorCode.value = ''
    return getActive().start(startOptions)
  }

  const stop = () => getActive().stop?.()

  const cancel = () => {
    browser.cancel?.()
    cloud.cancel?.()
    isRecording.value = false
    startConfirmed.value = false
  }

  // 供协调外部（面试页）在拿到后端能力后开关云端兜底。
  const setCloudEnabled = (enabled) => {
    cloud.setEnabled?.(enabled)
    isSupported.value = browser.isSupported.value || cloud.isSupported.value
    // 浏览器本就不支持时，直接把云端作为主引擎。
    if (!browser.isSupported.value && cloud.isSupported.value) {
      switchedToCloud = true
      activeEngine.value = 'cloud'
      engineStatus.value = 'cloud-service'
    }
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
    activeEngine,
    start,
    stop,
    cancel,
    setCloudEnabled,
  }
}
