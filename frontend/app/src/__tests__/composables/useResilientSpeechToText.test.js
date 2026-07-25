import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, nextTick, ref } from 'vue'

// 浏览器识别与云端识别都用可控假实现，专注验证协调器的切换与镜像逻辑。
const browserRefs = {}
const cloudRefs = {}
const browserApi = {}
const cloudApi = {}

const resetBrowser = () => {
  browserRefs.isSupported = ref(true)
  browserRefs.isRecording = ref(false)
  browserRefs.isVoiceActive = ref(false)
  browserRefs.voiceActivityAt = ref(0)
  browserRefs.finalTranscript = ref('')
  browserRefs.interimTranscript = ref('')
  browserRefs.error = ref('')
  browserRefs.errorCode = ref('')
  browserRefs.engineStatus = ref('browser-service')
  browserRefs.startConfirmed = ref(false)
  browserRefs.language = ref('zh-CN')
  browserApi.start = vi.fn(() => { browserRefs.isRecording.value = true })
  browserApi.stop = vi.fn(() => { browserRefs.isRecording.value = false })
  browserApi.cancel = vi.fn(() => { browserRefs.isRecording.value = false })
}

const resetCloud = () => {
  cloudRefs.isSupported = ref(false)
  cloudRefs.isRecording = ref(false)
  cloudRefs.isVoiceActive = ref(false)
  cloudRefs.voiceActivityAt = ref(0)
  cloudRefs.finalTranscript = ref('')
  cloudRefs.interimTranscript = ref('')
  cloudRefs.error = ref('')
  cloudRefs.errorCode = ref('')
  cloudRefs.engineStatus = ref('cloud-service')
  cloudRefs.startConfirmed = ref(false)
  cloudRefs.language = ref('zh-CN')
  cloudApi.start = vi.fn(() => { cloudRefs.isRecording.value = true; return Promise.resolve({ ok: true, code: '' }) })
  cloudApi.stop = vi.fn(() => { cloudRefs.isRecording.value = false; return Promise.resolve() })
  cloudApi.cancel = vi.fn(() => { cloudRefs.isRecording.value = false })
  cloudApi.setEnabled = vi.fn((enabled) => { cloudRefs.isSupported.value = Boolean(enabled) })
}

vi.mock('@/composables/useSpeechToText', () => ({
  useSpeechToText: () => ({ ...browserRefs, ...browserApi }),
}))

vi.mock('@/composables/useCloudSpeechToText', () => ({
  useCloudSpeechToText: () => ({ ...cloudRefs, ...cloudApi }),
}))

import { useResilientSpeechToText } from '@/composables/useResilientSpeechToText'

const runInScope = (fn) => {
  const scope = effectScope()
  let result
  scope.run(() => { result = fn() })
  return { result, scope }
}

describe('useResilientSpeechToText', () => {
  beforeEach(() => {
    resetBrowser()
    resetCloud()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('使用浏览器识别作为主引擎', () => {
    const { result, scope } = runInScope(() => useResilientSpeechToText({ sessionId: 's1' }))
    expect(result.engineStatus.value).toBe('browser-service')
    result.start()
    expect(browserApi.start).toHaveBeenCalledTimes(1)
    expect(cloudApi.start).not.toHaveBeenCalled()
    scope.stop()
  })

  it('浏览器出现可切换错误码且云端可用时静默切到云端', async () => {
    const { result, scope } = runInScope(() => useResilientSpeechToText({ sessionId: 's1', cloudEnabled: true }))
    cloudRefs.isSupported.value = true
    await nextTick()
    result.start()

    // 浏览器返回“录音了却无文字”类错误码
    browserRefs.errorCode.value = 'end-without-result'
    browserRefs.error.value = '语音识别已结束但未返回文字'
    await nextTick()
    await nextTick()

    expect(cloudApi.start).toHaveBeenCalled()
    expect(result.engineStatus.value).toBe('cloud-service')
    // 切换后不把浏览器错误抛给通话层
    expect(result.error.value).toBe('')
    scope.stop()
  })

  it('云端不可用时浏览器错误透传给通话层', async () => {
    const { result, scope } = runInScope(() => useResilientSpeechToText({ sessionId: 's1', cloudEnabled: false }))
    result.start()
    browserRefs.errorCode.value = 'network'
    browserRefs.error.value = '网络异常'
    await nextTick()

    expect(cloudApi.start).not.toHaveBeenCalled()
    expect(result.error.value).toBe('网络异常')
    expect(result.errorCode.value).toBe('network')
    scope.stop()
  })

  it('镜像激活引擎的识别文本', async () => {
    const { result, scope } = runInScope(() => useResilientSpeechToText({ sessionId: 's1' }))
    browserRefs.finalTranscript.value = '你好'
    await nextTick()
    expect(result.finalTranscript.value).toBe('你好')
    scope.stop()
  })

  it('setCloudEnabled 透传给云端引擎并更新可用性', async () => {
    const { result, scope } = runInScope(() => useResilientSpeechToText({ sessionId: 's1' }))
    result.setCloudEnabled(true)
    expect(cloudApi.setEnabled).toHaveBeenCalledWith(true)
    scope.stop()
  })

  it('浏览器不支持时直接以云端为主引擎', async () => {
    browserRefs.isSupported = ref(false)
    const { result, scope } = runInScope(() => useResilientSpeechToText({ sessionId: 's1', cloudEnabled: true }))
    result.setCloudEnabled(true)
    await nextTick()
    expect(result.engineStatus.value).toBe('cloud-service')
    result.start()
    expect(cloudApi.start).toHaveBeenCalled()
    scope.stop()
  })
})
