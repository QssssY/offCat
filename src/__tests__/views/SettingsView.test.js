import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus from 'element-plus'
import SettingsView from '@/views/settings/SettingsView.vue'
import { getGrowthOverview } from '@/api/growth'
import { clearInterviewHistory, getInterviewJobRoles } from '@/api/interview'
import { getMembershipPlans } from '@/api/membership'
import { clearResumeHistory } from '@/api/resume'
import { getUserSettings, saveUserSettings } from '@/api/userSettings'
import { fetchUserAiModels, getSystemTtsStatus, getUserAiConfigs, getUserAiUsage, previewTtsVoice, saveUserAiConfig, testUserAiConnectivity, testUserTtsConnectivity, toggleUserAiConfig } from '@/api/userAiConfig'
import { deleteAccount, getCurrentAccountSecurityQuestion } from '@/api/auth'
import { createUserFeedback } from '@/api/feedback'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { getSettingsPreferences, saveSettingsPreferences } from '@/utils/settingsPreferences'

const push = vi.fn()
const fetchUserInfo = vi.fn(() => Promise.resolve())
const clearUserInfo = vi.fn()
const setTheme = vi.fn()
const setFollowSystem = vi.fn()
let currentWrapper = null
const originalUserAgent = window.navigator.userAgent

vi.setConfig({ testTimeout: 15000 })

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

vi.mock('@/api/auth', () => ({
  deleteAccount: vi.fn(() => Promise.resolve()),
  getCurrentAccountSecurityQuestion: vi.fn(() => Promise.resolve({ data: { securityQuestion: '你的出生城市是哪里？' } })),
  updatePassword: vi.fn(() => Promise.resolve()),
  updateSecurityQuestion: vi.fn(() => Promise.resolve())
}))

vi.mock('@/api/membership', () => ({
  getMembershipPlans: vi.fn(() => Promise.resolve({
    data: [
      { planCode: 'vip_month', planName: 'Monthly VIP' }
    ]
  }))
}))

vi.mock('@/api/interview', () => ({
  clearInterviewHistory: vi.fn(() => Promise.resolve({ data: { deletedCount: 2 } })),
  getInterviewJobRoles: vi.fn(() => Promise.resolve({
    data: [
      { roleName: '前端工程师', roleCode: 'frontend', interviewTag: '热门' },
      { roleName: '后端工程师', roleCode: 'backend', interviewTag: '常规' }
    ]
  }))
}))

vi.mock('@/api/resume', () => ({
  clearResumeHistory: vi.fn(() => Promise.resolve({ data: { deletedCount: 3 } }))
}))

vi.mock('@/api/userSettings', () => ({
  getUserSettings: vi.fn(() => Promise.resolve({
    data: {
      interviewRetentionDays: 0,
      resumeRetentionDays: 0
    }
  })),
  saveUserSettings: vi.fn((data) => Promise.resolve({ data }))
}))

vi.mock('@/api/userAiConfig', () => ({
  deleteUserAiConfig: vi.fn(() => Promise.resolve()),
  discoverTtsModelsAndVoices: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      models: [{ id: 'tts-1', name: 'tts-1' }],
      voices: [{ id: 'alloy', name: 'alloy' }],
      voiceDiscoverySupported: true
    }
  })),
  fetchUserAiModels: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      message: '模型列表获取成功',
      models: [
        { id: 'gpt-4o-mini', name: 'gpt-4o-mini' },
        { id: 'deepseek-chat', name: 'deepseek-chat' }
      ],
      latencyMs: 18
    }
  })),
  getUserAiConfigs: vi.fn(() => Promise.resolve({
    data: [
      {
        configType: 'default',
        providerName: '默认 DeepSeek',
        baseUrl: 'https://api.deepseek.com/v1',
        apiKey: 'sk-****-abcd',
        model: 'deepseek-chat',
        enabled: true,
        supportsMultimodal: false,
        verificationStatus: 'verified',
        ttsBaseUrl: 'https://tts.example.com/v1',
        ttsApiKey: 'tts****abcd',
        ttsModel: 'tts-1',
        ttsVoiceId: 'alloy',
        ttsConfigured: true
      }
    ]
  })),
  getUserAiUsage: vi.fn(() => Promise.resolve({
    data: {
      used: 12,
      limit: 50,
      remaining: 38
    }
  })),
  getSystemTtsStatus: vi.fn(() => Promise.resolve({
    data: {
      systemTtsAvailable: true
    }
  })),
  previewTtsVoice: vi.fn(() => Promise.resolve(new Blob(['audio'], { type: 'audio/mpeg' }))),
  saveUserAiConfig: vi.fn(() => Promise.resolve({ data: { configType: 'resume' } })),
  testUserAiConnectivity: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      message: '连通测试成功',
      latencyMs: 23
    }
  })),
  testUserTtsConnectivity: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      message: 'TTS 连通测试成功',
      endpointPath: '/audio/speech',
      latencyMs: 31
    }
  })),
  toggleUserAiConfig: vi.fn(() => Promise.resolve())
}))

vi.mock('@/api/growth', () => ({
  getGrowthOverview: vi.fn(() => Promise.resolve({
    data: {
      summary: {
        resumeDiagnosisCount: 7,
        mockInterviewCount: 5,
        jobMatchCount: 3,
        polishCount: 2
      }
    }
  }))
}))

vi.mock('@/api/feedback', () => ({
  createUserFeedback: vi.fn(() => Promise.resolve({ data: 100 }))
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

vi.mock('@/stores/theme', () => ({
  useThemeStore: vi.fn()
}))

vi.mock('@/components/OnboardingGuide.vue', () => ({
  default: {
    name: 'OnboardingGuide',
    template: '<div class="onboarding-guide-stub"></div>'
  }
}))

const mountView = () => {
  currentWrapper = mount(SettingsView, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        transition: false
      }
    }
  })
  return currentWrapper
}

const waitForSecurityTransition = () => new Promise((resolve) => setTimeout(resolve, 320))

const switchSection = async (wrapper, section) => {
  wrapper.vm.activeSection = section
  await flushPromises()
  await waitForSecurityTransition()
}

const settingsViewSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/settings/SettingsView.vue'), 'utf8')

const setUserAgent = (userAgent) => {
  Object.defineProperty(window.navigator, 'userAgent', {
    value: userAgent,
    configurable: true
  })
}

describe('SettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    window.speechSynthesis = {
      getVoices: vi.fn(() => []),
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      onvoiceschanged: null
    }
    window.SpeechSynthesisUtterance = vi.fn(function SpeechSynthesisUtterance(text) {
      this.text = text
    })
    Object.defineProperty(URL, 'createObjectURL', {
      value: vi.fn(() => 'blob:tts-preview'),
      configurable: true
    })
    Object.defineProperty(URL, 'revokeObjectURL', {
      value: vi.fn(),
      configurable: true
    })
    vi.stubGlobal('Audio', vi.fn(function Audio(src) {
      this.src = src
      this.pause = vi.fn()
      this.play = vi.fn(() => Promise.resolve())
    }))
    useUserStore.mockReturnValue({
      userInfo: {
        id: 1,
        username: 'tester',
        nickname: '测试用户',
        role: 0,
        status: 1,
        membershipPlanCode: 'vip_month',
        vipExpireTime: '2026-05-18T12:00:00',
        createTime: '2026-05-01T09:30:00',
        resumeQuota: 3,
        interviewQuota: 4,
        vipDailyResumeQuota: 5,
        vipDailyInterviewQuota: 6
      },
      isVip: () => true,
      fetchUserInfo,
      clearUserInfo
    })
    useThemeStore.mockReturnValue({
      followSystem: false,
      manualTheme: 'light',
      resolvedTheme: 'light',
      setTheme,
      setFollowSystem
    })
  })

  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
    setUserAgent(originalUserAgent)
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('renders all settings sections', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('设置中心')
    expect(wrapper.text()).toContain('账号资料')
    expect(wrapper.text()).toContain('面试偏好')
    expect(wrapper.text()).toContain('自定义 AI')
    expect(wrapper.text()).toContain('账号安全')
    expect(wrapper.text()).toContain('隐私与数据')
    expect(wrapper.text()).toContain('数据管理')
    expect(wrapper.text()).toContain('问题反馈')
    expect(wrapper.text()).toContain('外观偏好')
    expect(wrapper.text()).toContain('通知偏好')
    expect(wrapper.text()).toContain('新手引导')
    expect(wrapper.text()).toContain('会员与额度')

    await switchSection(wrapper, 'interview')
    expect(wrapper.text()).toContain('默认交互方式')
    expect(wrapper.find('.sub-nav-tabs').exists()).toBe(true)
    expect(wrapper.findAll('.sub-nav-tab').map((tab) => tab.text())).toEqual(['面试偏好', '语音通话'])

    await switchSection(wrapper, 'security')
    expect(wrapper.text()).toContain('账号安全')
    expect(wrapper.text()).toContain('注销账号')
    expect(wrapper.find('.settings-nav').text()).not.toContain('注销账号')

    await switchSection(wrapper, 'dataManagement')
    expect(wrapper.text()).toContain('隐私与数据')
    expect(wrapper.text()).toContain('数据管理')

    await switchSection(wrapper, 'feedback')
    expect(wrapper.text()).toContain('问题反馈')

    await switchSection(wrapper, 'appearance')
    expect(wrapper.text()).toContain('外观偏好')

    await switchSection(wrapper, 'notification')
    expect(wrapper.text()).toContain('通知偏好')

    await switchSection(wrapper, 'onboarding')
    expect(wrapper.text()).toContain('新手引导')

    await switchSection(wrapper, 'membership')
    expect(wrapper.text()).toContain('会员与额度')
  }, 15000)

  it('shows and saves user custom AI provider settings', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'customAi')

    expect(getUserAiConfigs).toHaveBeenCalled()
    expect(getUserAiUsage).toHaveBeenCalled()
    expect(wrapper.text()).toContain('自定义 AI 接入')
    expect(wrapper.text()).toContain('12/50')
    expect(wrapper.text()).toContain('默认 DeepSeek')

    Object.assign(wrapper.vm.userAiConfigForm, {
      configType: 'resume',
      providerName: '简历模型',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: 'gpt-4o-mini',
      supportsMultimodal: true
    })

    await wrapper.vm.handleUserAiConnectivityTest()
    await wrapper.vm.handleUserAiConfigSave()
    await wrapper.vm.handleUserAiConfigToggle('default', false)

    expect(testUserAiConnectivity).toHaveBeenCalledWith({
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: 'gpt-4o-mini',
      supportsMultimodal: true
    })
    expect(saveUserAiConfig).toHaveBeenCalledWith({
      configType: 'resume',
      providerName: '简历模型',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: 'gpt-4o-mini',
      supportsMultimodal: true
    })
    expect(toggleUserAiConfig).toHaveBeenCalledWith('default', false)
  })

  it('fetches user AI model options and selects the first model when empty', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'customAi')

    Object.assign(wrapper.vm.userAiConfigForm, {
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: ''
    })

    await wrapper.vm.handleUserAiModelsFetch()
    await flushPromises()

    expect(fetchUserAiModels).toHaveBeenCalledWith({
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real'
    })
    expect(wrapper.vm.userAiModelOptions).toEqual([
      { id: 'gpt-4o-mini', name: 'gpt-4o-mini' },
      { id: 'deepseek-chat', name: 'deepseek-chat' }
    ])
    expect(wrapper.vm.userAiConfigForm.model).toBe('gpt-4o-mini')
  })

  it('keeps manually entered user AI model when model discovery fails', async () => {
    fetchUserAiModels.mockResolvedValueOnce({
      data: {
        success: false,
        message: '获取失败',
        models: []
      }
    })
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'customAi')

    Object.assign(wrapper.vm.userAiConfigForm, {
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: 'manual-model'
    })

    await wrapper.vm.handleUserAiModelsFetch()
    await flushPromises()

    expect(wrapper.vm.userAiConfigForm.model).toBe('manual-model')
  })

  it('keeps TTS controls scoped to fallback and interview custom AI settings', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'customAi')
    wrapper.vm.handleUserAiConfigTypeChange('resume')
    await nextTick()

    expect(wrapper.text()).not.toContain('TTS 语音合成')

    wrapper.vm.handleUserAiConfigTypeChange('default')
    await nextTick()

    expect(wrapper.text()).toContain('TTS 语音合成')
    expect(wrapper.text()).not.toContain('TTS 地址')

    await wrapper.find('[data-testid="custom-ai-tts-toggle"]').trigger('click')
    expect(wrapper.text()).toContain('TTS 地址')

    wrapper.vm.handleUserAiConfigTypeChange('interview')
    await nextTick()

    expect(wrapper.text()).toContain('TTS 语音合成')
    await wrapper.find('[data-testid="custom-ai-tts-toggle"]').trigger('click')
    expect(wrapper.text()).toContain('TTS 地址')

    Object.assign(wrapper.vm.userAiConfigForm, {
      configType: 'interview',
      providerName: '面试模型',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: 'gpt-4o-mini',
      supportsMultimodal: false,
      ttsBaseUrl: 'https://tts.example.com/v1',
      ttsApiKey: 'tts-user-real',
      ttsEndpointPath: '',
      ttsModel: 'tts-1',
      ttsVoiceId: 'alloy',
      ttsProvider: ''
    })

    await wrapper.vm.handleUserTtsConnectivityTest()
    await wrapper.vm.handleUserAiConfigSave()

    expect(testUserTtsConnectivity).toHaveBeenCalledWith({
      ttsBaseUrl: 'https://tts.example.com/v1',
      ttsApiKey: 'tts-user-real',
      ttsEndpointPath: '',
      ttsModel: 'tts-1',
      ttsVoiceId: 'alloy',
      ttsProvider: ''
    })
    expect(saveUserAiConfig).toHaveBeenCalledWith({
      configType: 'interview',
      providerName: '面试模型',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user-real',
      model: 'gpt-4o-mini',
      supportsMultimodal: false,
      ttsBaseUrl: 'https://tts.example.com/v1',
      ttsApiKey: 'tts-user-real',
      ttsEndpointPath: '',
      ttsModel: 'tts-1',
      ttsVoiceId: 'alloy',
      ttsProvider: ''
    })
  })

  it('offers EdgeTTS provider with built-in free voices', async () => {
    const wrapper = mountView()
    wrapper.vm.handleTtsProviderChange('edge')
    await flushPromises()

    expect(wrapper.vm.userAiConfigForm.ttsProvider).toBe('edge')
    expect(wrapper.vm.userAiConfigForm.ttsBaseUrl).toBe('https://speech.platform.bing.com')
    expect(wrapper.vm.userAiConfigForm.ttsModel).toBe('edge-tts')
    expect(wrapper.vm.userAiConfigForm.ttsVoiceId).toBe('zh-CN-XiaoxiaoNeural')
    expect(wrapper.vm.userAiConfigForm.ttsEndpointPath).toBe('/consumer/speech/synthesize/readaloud/edge/v1')
    expect(wrapper.vm.ttsDiscoveryResult.voices).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: 'zh-CN-XiaoxiaoNeural' }),
      expect.objectContaining({ id: 'zh-CN-YunxiNeural' })
    ]))
  })

  it('offers Gemini MiniMax Qwen and xAI TTS provider presets', async () => {
    const wrapper = mountView()
    const providerPresets = [
      {
        provider: 'gemini',
        baseUrl: 'https://generativelanguage.googleapis.com',
        model: 'gemini-2.5-flash-preview-tts',
        voiceId: 'Kore',
        endpointPath: '/v1beta/models/{model}:generateContent',
        voiceIds: ['Kore', 'Puck']
      },
      {
        provider: 'minimax',
        baseUrl: 'https://api.minimax.chat',
        model: 'speech-02-turbo',
        voiceId: 'male-qn-qingse',
        endpointPath: '/v1/t2a_v2',
        voiceIds: ['male-qn-qingse', 'female-shaonv']
      },
      {
        provider: 'qwen',
        baseUrl: 'https://dashscope.aliyuncs.com',
        model: 'qwen3-tts-flash',
        voiceId: 'Cherry',
        endpointPath: '/api/v1/services/aigc/multimodal-generation/generation',
        voiceIds: ['Cherry', 'Serena']
      },
      {
        provider: 'xai',
        baseUrl: 'https://api.x.ai',
        model: 'grok-tts',
        voiceId: 'Fritz-PlayAI',
        endpointPath: '/v1/tts',
        voiceIds: ['Fritz-PlayAI', 'Luna-PlayAI']
      }
    ]

    for (const preset of providerPresets) {
      wrapper.vm.handleTtsProviderChange(preset.provider)
      await flushPromises()

      expect(wrapper.vm.userAiConfigForm.ttsProvider).toBe(preset.provider)
      expect(wrapper.vm.userAiConfigForm.ttsBaseUrl).toBe(preset.baseUrl)
      expect(wrapper.vm.userAiConfigForm.ttsModel).toBe(preset.model)
      expect(wrapper.vm.userAiConfigForm.ttsVoiceId).toBe(preset.voiceId)
      expect(wrapper.vm.userAiConfigForm.ttsEndpointPath).toBe(preset.endpointPath)
      expect(wrapper.vm.ttsDiscoveryResult.models).toEqual([{ id: preset.model, name: preset.model }])
      expect(wrapper.vm.ttsDiscoveryResult.voices).toEqual(expect.arrayContaining(
        preset.voiceIds.map(id => expect.objectContaining({ id }))
      ))
    }
  })

  it('shows system TTS fallback status when user has no custom TTS config', async () => {
    getUserAiConfigs.mockResolvedValueOnce({
      data: [
        {
          configType: 'default',
          providerName: '默认 DeepSeek',
          baseUrl: 'https://api.deepseek.com/v1',
          apiKey: 'sk-****-abcd',
          model: 'deepseek-chat',
          enabled: true,
          supportsMultimodal: false,
          verificationStatus: 'verified',
          ttsConfigured: false
        }
      ]
    })
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'customAi')

    expect(getSystemTtsStatus).toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前使用系统提供的云端语音服务')
  })

  it('shows custom TTS priority status over system TTS when user TTS is configured', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'customAi')

    expect(wrapper.text()).toContain('当前使用自定义语音服务（优先于系统配置）')
  })

  it('centers the TTS status capsule inside the collapsed heading', () => {
    const source = settingsViewSource()

    expect(source).toContain('class="cai-tts-status"')
    expect(source).toContain('.cai-tts-status')
    expect(source).toContain('display: inline-flex;')
    expect(source).toContain('align-items: center;')
    expect(source).toContain('justify-content: center;')
  })

  it('keeps custom AI select wrappers aligned with input control height styles', () => {
    const source = settingsViewSource()

    expect(source).toContain('.cai-form :deep(.el-input__wrapper),')
    expect(source).toContain('.cai-form :deep(.el-select__wrapper)')
    expect(source).toContain('.cai-form :deep(.el-select .el-input__wrapper),')
    expect(source).toContain('.cai-form :deep(.el-select .el-select__wrapper)')
    expect(source).toContain('.cai-tts-discover-btn')
    expect(source).toContain('height: var(--cai-form-control-height);')
    expect(source).toContain('.cai-form .el-select .el-input__wrapper,')
    expect(source).toContain('.cai-form .el-select .el-select__wrapper')
  })

  it('keeps EdgeTTS preset visible in user custom TTS provider list', () => {
    const source = settingsViewSource()

    expect(source).toContain("value: 'edge'")
    expect(source).toContain('EdgeTTS')
    expect(source).toContain('zh-CN-XiaoxiaoNeural')
    expect(source).toContain('EDGE_CLOUD_TTS_VOICES')
    expect(source).toContain('voices: EDGE_CLOUD_TTS_VOICES')
  })

  it('uses readable local feature icon sizes inside the personal settings center', () => {
    const source = settingsViewSource()

    expect(source).toContain('loading="eager"')
    expect(source).toContain('fetch-priority="auto"')
    expect(source).toContain('class="settings-nav-icon"')
    expect(source).toContain("{ key: 'customAi', label: '自定义 AI', icon: 'membership-center' }")
    expect(source).toContain('<FeatureIcon name="membership-center" size="lg" class="panel-heading-icon" />')
    expect(source).toContain('<FeatureIcon name="announcement" size="md" class="voice-preview-icon" />')
    expect(source).toContain('<FeatureIcon name="account-security" size="md" class="settings-alert-icon" />')
    expect(source).toContain('<FeatureIcon name="retry" size="md" class="settings-refresh-icon" />')
    expect(source).toContain('<FeatureIcon name="account-security" size="md" />')
    expect(source).toContain('.settings-nav-item:hover .settings-nav-icon')
    expect(source).toContain('width: 48px;')
  })

  it('uses a balanced workspace layout for short settings panels', async () => {
    const wrapper = mountView()
    await flushPromises()
    const source = settingsViewSource()

    expect(source).toContain('min-height: calc(100dvh - var(--header-height, 82px) - 96px)')
    expect(source).toContain('.settings-workspace')
    expect(source).toContain('.settings-panel-body')
    expect(source).toContain('.settings-fill-note')
    expect(source).toContain('.profile-overview-card')
    expect(source).toContain('.profile-support-grid')
    expect(source).toContain('.onboarding-intro-grid')
    expect(source).toContain('.settings-nav-item.active::after')
    expect(source).not.toMatch(/\.settings-nav-item::before[\s\S]*?width:\s*3px/)
    expect(source).not.toMatch(/\.info-item::before[\s\S]*?width:\s*3px/)
    expect(source).not.toContain('transition: all')

    expect(wrapper.find('.settings-workspace').exists()).toBe(true)
    expect(wrapper.find('.settings-panel-body').exists()).toBe(true)
    expect(wrapper.find('.profile-overview-card').exists()).toBe(true)
    expect(wrapper.find('.profile-support-grid').exists()).toBe(true)

    await switchSection(wrapper, 'notification')
    expect(wrapper.text()).toContain('通知偏好')
    expect(wrapper.find('.settings-fill-note').exists()).toBe(true)

    await switchSection(wrapper, 'membership')
    expect(wrapper.text()).toContain('会员与额度')
    expect(wrapper.find('.settings-fill-note').exists()).toBe(true)

    await switchSection(wrapper, 'onboarding')
    expect(wrapper.text()).toContain('新手引导')
    expect(wrapper.find('.settings-fill-note').exists()).toBe(true)
    expect(wrapper.find('.onboarding-intro-grid').exists()).toBe(true)
  }, 15000)

  it('uses settings sub navigation, Naive UI controls, and route-safe motion contracts', () => {
    const source = settingsViewSource()
    const layoutSource = readFileSync(resolve(process.cwd(), 'src/layouts/MainLayout.vue'), 'utf8')
    const appSource = readFileSync(resolve(process.cwd(), 'src/App.vue'), 'utf8')

    expect(source).toContain("const interviewSubTab = ref('basic')")
    expect(source).toContain('const interviewSubTabs = [')
    expect(source).not.toContain('voicePrefsExpanded')
    expect(source).not.toContain('offlineEnhanceExpanded')
    expect(source).toContain('<n-select')
    expect(source).toContain('<n-slider')
    expect(source).toContain('<n-switch')
    expect(source).toContain('<n-button')
    expect(source).toContain('.sub-nav-tabs')
    expect(source).toContain('prefers-reduced-motion: reduce')
    expect(source).not.toContain('transition: all')
    expect(layoutSource).toContain('<Transition name="page-fade" mode="out-in">')
    expect(layoutSource).toContain('class="page-fade-route"')
    expect(layoutSource).toContain('transform: translateY(12px)')
    expect(layoutSource).toContain('prefers-reduced-motion: reduce')
    expect(appSource).toContain('Switch: {')
    expect(appSource).toContain('Slider: {')
  })

  it('switches interview preference sub tabs without the old long collapsible layout', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')

    expect(wrapper.vm.interviewSubTab).toBe('basic')
    expect(wrapper.text()).toContain('默认面试岗位')
    expect(wrapper.text()).not.toContain('浏览器 voice 列表')

    await wrapper.findAll('.sub-nav-tab')[1].trigger('click')
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.vm.interviewSubTab).toBe('voice')
    expect(wrapper.text()).toContain('AI 播报声音')
    expect(wrapper.text()).toContain('重置语音偏好')

    expect(wrapper.text()).not.toContain('离线增强')
    expect(wrapper.text()).not.toContain('sherpa-onnx')
  }, 15000)

  it('removes offline speech package actions from settings center', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'interview')
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.text()).not.toContain('下载离线语音引擎')
    expect(wrapper.text()).not.toContain('删除资源包')
    expect(wrapper.vm).not.toHaveProperty('handleOfflineSttDownload')
    expect(wrapper.vm).not.toHaveProperty('handleOfflineSttClearConfirm')
  })

  it('does not show browser local speech pack actions in settings center', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.text()).not.toContain('浏览器本地语音包')
    expect(wrapper.text()).not.toContain('安装语音包')
    expect(wrapper.vm).not.toHaveProperty('handleInstallLocalSpeech')
    expect(wrapper.vm).not.toHaveProperty('handleManageLocalSpeechPack')
  }, 15000)


  it('shows subscription plan name without exposing internal identifiers', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getMembershipPlans).toHaveBeenCalled()
    expect(wrapper.text()).toContain('订阅套餐')
    expect(wrapper.text()).toContain('月度会员')
    expect(wrapper.text()).toContain('注册时间')
    expect(wrapper.text()).toContain('2026-05-01')
    expect(wrapper.text()).not.toContain('2026-05-18 12:00')
    expect(wrapper.text()).not.toContain('用户 ID')
    expect(wrapper.text()).not.toContain('vip_month')
  })

  it('keeps profile nickname read-only in settings center', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('测试用户')
    expect(wrapper.text()).not.toContain('保存昵称')
    expect(wrapper.find('input[autocomplete="nickname"]').exists()).toBe(false)
  })

  it('switches account security forms with one form visible at a time', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'security')

    expect(wrapper.vm.securityMode).toBe('password')
    expect(wrapper.find('input[autocomplete="new-password"]').exists()).toBe(true)
    expect(wrapper.find('section[aria-labelledby="security-title"] .settings-form .el-select').exists()).toBe(false)

    wrapper.vm.handleSecurityModeChange('securityQuestion')
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.find('input[autocomplete="new-password"]').exists()).toBe(false)
    expect(wrapper.find('section[aria-labelledby="security-title"] .settings-form .el-select').exists()).toBe(true)
  }, 15000)

  it('renders account deletion as an account security tab', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'security')
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()

    const tabs = wrapper.findAll('.security-mode-tab').map((tab) => tab.text())
    expect(tabs).toEqual(['修改密码', '修改安全问题', '注销账号'])
    expect(wrapper.find('.settings-nav').text()).not.toContain('注销账号')
    expect(wrapper.text()).toContain('注销后不可恢复')
    expect(wrapper.find('.account-delete-context').exists()).toBe(true)
    expect(wrapper.find('.account-delete-form').exists()).toBe(true)
  })

  it('does not repeat security section title inside the selected form', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.settings-form h3').exists()).toBe(false)
  }, 15000)

  it('updates theme via theme store handlers', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'appearance')

    expect(wrapper.text()).toContain('已保存到当前浏览器')
    wrapper.vm.handleThemeChange('dark')
    expect(wrapper.vm.themeChoice).toBe('dark')
    expect(setTheme).toHaveBeenCalledWith('dark')
  })

  it('saves local interview preferences from settings center', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getInterviewJobRoles).toHaveBeenCalled()
    wrapper.vm.interviewPreferenceForm.defaultInterviewDifficulty = 'advanced'
    wrapper.vm.interviewPreferenceForm.defaultInterviewMode = 'tech_leader'
    wrapper.vm.interviewPreferenceForm.defaultFeedbackMode = 'immediate'
    wrapper.vm.interviewPreferenceForm.defaultInterviewInteractionType = 1
    wrapper.vm.interviewPreferenceForm.voiceSpeakingRate = 1.1
    wrapper.vm.interviewPreferenceForm.voicePitch = 0.95
    wrapper.vm.interviewPreferenceForm.voiceVolume = 0.6
    wrapper.vm.interviewPreferenceForm.voiceMuteResumeMode = 'manual'
    wrapper.vm.interviewPreferenceForm.voiceAutoSubmitDelayMs = 5000
    wrapper.vm.interviewPreferenceForm.voiceRecognitionLanguage = 'en-US'
    wrapper.vm.interviewPreferenceForm.voiceRecognitionEngine = 'system_local'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'custom'
    wrapper.vm.interviewPreferenceForm.voiceName = 'Microsoft Xiaoxiao Natural'
    wrapper.vm.interviewPreferenceForm.voiceURI = 'xiaoxiao-uri'
    wrapper.vm.interviewPreferenceForm.voiceLang = 'zh-CN'
    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 90
    wrapper.vm.interviewPreferenceForm.resumeRetentionDays = 180
    await wrapper.vm.handleDefaultJobChange('frontend')
    await flushPromises()

    const preferences = getSettingsPreferences()
    expect(saveUserSettings).not.toHaveBeenCalled()
    expect(preferences).toMatchObject({
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate',
      defaultInterviewInteractionType: 1,
      voiceSpeakingRate: 1.1,
      voicePitch: 0.95,
      voiceVolume: 0.6,
      voiceMuteResumeMode: 'manual',
      voiceAutoSubmitDelayMs: 5000,
      voiceRecognitionLanguage: 'en-US',
      voiceRecognitionEngine: 'system_local',
      voicePreferredType: 'custom',
      voiceName: 'Microsoft Xiaoxiao Natural',
      voiceURI: 'xiaoxiao-uri',
      voiceLang: 'zh-CN',
      interviewRetentionDays: 90,
      resumeRetentionDays: 180
    })
  }, 15000)

  it('resets local voice preferences without changing other interview settings', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.defaultInterviewDifficulty = 'advanced'
    wrapper.vm.interviewPreferenceForm.voiceSpeakingRate = 1.1
    wrapper.vm.interviewPreferenceForm.voicePitch = 0.95
    wrapper.vm.interviewPreferenceForm.voiceVolume = 0.6
    wrapper.vm.interviewPreferenceForm.voiceMuteResumeMode = 'manual'
    wrapper.vm.interviewPreferenceForm.voiceAutoSubmitDelayMs = 5000
    wrapper.vm.interviewPreferenceForm.voiceRecognitionLanguage = 'en-US'
    wrapper.vm.interviewPreferenceForm.voiceRecognitionEngine = 'system_local'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'custom'
    wrapper.vm.interviewPreferenceForm.voiceName = 'Microsoft Xiaoxiao Natural'
    wrapper.vm.interviewPreferenceForm.voiceURI = 'xiaoxiao-uri'
    wrapper.vm.interviewPreferenceForm.voiceLang = 'zh-CN'
    wrapper.vm.handleInterviewPreferenceSave()
    await flushPromises()

    wrapper.vm.handleVoicePreferenceReset()
    await flushPromises()

    expect(getSettingsPreferences()).toMatchObject({
      defaultInterviewDifficulty: 'advanced',
      voiceSpeakingRate: 0.92,
      voicePitch: 1.06,
      voiceVolume: 1,
      voiceMuteResumeMode: 'auto',
      voiceAutoSubmitDelayMs: 3000,
      voiceRecognitionLanguage: 'auto',
      voiceRecognitionEngine: 'system_local',
      voicePreferredType: 'natural_zh',
      voiceName: '',
      voiceURI: '',
      voiceLang: ''
    })
  }, 15000)

  it('uses responsive class for custom browser voice selector', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'custom'
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.find('.browser-voice-select').exists()).toBe(true)
    const browserVoiceSelect = wrapper.findAllComponents({ name: 'Select' })
      .find((select) => select.classes().includes('browser-voice-select'))
    expect(browserVoiceSelect.props('filterable')).toBe(true)
    expect(browserVoiceSelect.props('disabled')).toBe(true)
  }, 15000)

  it('renders voice preview as an accessible icon button', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    const previewButton = wrapper.find('.voice-preview-button')
    expect(previewButton.exists()).toBe(true)
    expect(previewButton.attributes('aria-label')).toBe('试听当前 AI 播报声音')
    expect(previewButton.find('.voice-preview-icon img').exists()).toBe(true)
  })

  it('prepares browser TTS during voice preview clicks so Chrome can load preferred voices', async () => {
    let synthesisPrepared = false
    window.speechSynthesis.getVoices = vi.fn(() => (
      synthesisPrepared ? [{ lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural' }] : []
    ))
    window.speechSynthesis.resume = vi.fn(() => {
      synthesisPrepared = true
    })
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    await wrapper.find('.voice-preview-button').trigger('click')

    expect(window.speechSynthesis.resume).toHaveBeenCalled()
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].voice.name).toBe('Microsoft Xiaoxiao Natural')
  }, 15000)

  it('previews the default Chinese natural voice through browser TTS', async () => {
    saveSettingsPreferences({
      voicePreferredType: 'natural_zh'
    })
    window.speechSynthesis.getVoices = vi.fn(() => [
      {
        name: 'Microsoft Xiaoxiao Natural',
        lang: 'zh-CN',
        voiceURI: 'xiaoxiao-uri',
        localService: true
      }
    ])
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    await wrapper.vm.handleVoicePreview()
    await flushPromises()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].text).toBe('你好，我是你的 AI 面试官。')
    expect(window.speechSynthesis.speak.mock.calls[0][0].voice.name).toBe('Microsoft Xiaoxiao Natural')
  }, 15000)

  it('groups browser voice presets and marks unavailable specific presets', async () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      {
        name: 'Microsoft Xiaoxiao Natural',
        lang: 'zh-CN',
        voiceURI: 'xiaoxiao-uri',
        localService: true
      }
    ])
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    const optionGroups = wrapper.vm.voicePreferredTypeOptions
    const femaleGroup = optionGroups.find((group) => group.label === '女声系列')
    const maleGroup = optionGroups.find((group) => group.label === '男声系列')
    const cloudGroup = optionGroups.find((group) => group.children.some((option) => option.value === 'edge_cloud'))
    const livelyFemaleOption = femaleGroup.children.find((option) => option.value === 'lively_female')
    const edgeCloudOption = cloudGroup.children.find((option) => option.value === 'edge_cloud')

    expect(optionGroups.map((group) => group.label)).toEqual(expect.arrayContaining(['女声系列', '男声系列', '通用', '云端语音', '自定义']))
    expect(femaleGroup.children.map((option) => option.value)).toEqual(expect.arrayContaining([
      'gentle_female',
      'pro_female',
      'lively_female',
      'warm_female'
    ]))
    expect(maleGroup.children.map((option) => option.value)).toEqual(expect.arrayContaining([
      'magnetic_male',
      'pro_male',
      'calm_male',
      'energetic_male'
    ]))
    expect(cloudGroup.children.length).toBeGreaterThan(6)
    expect(cloudGroup.children.map((option) => option.value)).toEqual(expect.arrayContaining([
      'edge_cloud',
      'edge_cloud:zh-CN-XiaoxiaoNeural',
      'edge_cloud:zh-CN-YunxiNeural',
      'edge_cloud:zh-HK-HiuMaanNeural',
      'edge_cloud:zh-TW-HsiaoYuNeural'
    ]))
    expect(edgeCloudOption.disabled).toBe(false)
    expect(livelyFemaleOption.disabled).toBe(true)
    expect(livelyFemaleOption.label).toContain('当前系统不可用')
  }, 15000)

  it('selects EdgeTTS cloud voice as an unsaved TTS configuration shortcut', async () => {
    const wrapper = mountView()
    await flushPromises()

    const previousPreference = getSettingsPreferences().voicePreferredType
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'edge_cloud'
    wrapper.vm.handleVoicePreferredTypeChange()
    await flushPromises()

    expect(wrapper.vm.userAiConfigForm.ttsProvider).toBe('edge')
    expect(wrapper.vm.userAiConfigForm.ttsBaseUrl).toBe('https://speech.platform.bing.com')
    expect(wrapper.vm.userAiConfigForm.ttsApiKey).toBe('')
    expect(wrapper.vm.userAiConfigForm.ttsModel).toBe('edge-tts')
    expect(wrapper.vm.userAiConfigForm.ttsVoiceId).toBe('zh-CN-XiaoxiaoNeural')
    expect(wrapper.vm.userAiConfigForm.ttsEndpointPath).toBe('/consumer/speech/synthesize/readaloud/edge/v1')
    expect(wrapper.vm.userTtsConfigExpanded).toBe(true)
    expect(getSettingsPreferences().voicePreferredType).toBe(previousPreference)
    expect(wrapper.vm.browserTtsVoiceStatusText).toContain('EdgeTTS')

    await wrapper.vm.handleVoicePreview()
    await flushPromises()

    expect(previewTtsVoice).toHaveBeenCalledWith({
      ttsBaseUrl: 'https://speech.platform.bing.com',
      ttsApiKey: '',
      ttsModel: 'edge-tts',
      ttsVoiceId: 'zh-CN-XiaoxiaoNeural',
      ttsEndpointPath: '/consumer/speech/synthesize/readaloud/edge/v1',
      ttsProvider: 'edge'
    })
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  }, 15000)

  it('selects a specific EdgeTTS cloud voice from AI broadcast voice list', async () => {
    const wrapper = mountView()
    await flushPromises()

    const previousPreference = getSettingsPreferences().voicePreferredType
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'edge_cloud:zh-CN-YunxiNeural'
    wrapper.vm.handleVoicePreferredTypeChange()
    await flushPromises()

    expect(wrapper.vm.userAiConfigForm.ttsProvider).toBe('edge')
    expect(wrapper.vm.userAiConfigForm.ttsVoiceId).toBe('zh-CN-YunxiNeural')
    expect(wrapper.vm.userTtsConfigExpanded).toBe(true)
    expect(getSettingsPreferences().voicePreferredType).toBe(previousPreference)

    await wrapper.vm.handleVoicePreview()
    await flushPromises()

    expect(previewTtsVoice).toHaveBeenCalledWith(expect.objectContaining({
      ttsProvider: 'edge',
      ttsModel: 'edge-tts',
      ttsVoiceId: 'zh-CN-YunxiNeural'
    }))
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  }, 15000)

  it('persists EdgeTTS cloud voice preference after saving the custom TTS config', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.userAiConfigForm.configType = 'interview'
    wrapper.vm.userAiConfigForm.providerName = 'EdgeTTS Interview'
    wrapper.vm.userAiConfigForm.baseUrl = 'https://api.deepseek.com/v1'
    wrapper.vm.userAiConfigForm.apiKey = 'sk-user-real'
    wrapper.vm.userAiConfigForm.model = 'deepseek-chat'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'edge_cloud'
    wrapper.vm.handleVoicePreferredTypeChange()
    await flushPromises()

    await wrapper.vm.handleUserAiConfigSave()
    await flushPromises()

    expect(saveUserAiConfig).toHaveBeenCalledWith(expect.objectContaining({
      configType: 'interview',
      ttsProvider: 'edge',
      ttsBaseUrl: 'https://speech.platform.bing.com',
      ttsApiKey: '',
      ttsModel: 'edge-tts',
      ttsVoiceId: 'zh-CN-XiaoxiaoNeural',
      ttsEndpointPath: '/consumer/speech/synthesize/readaloud/edge/v1'
    }))
    expect(getSettingsPreferences().voicePreferredType).toBe('edge_cloud')
  }, 15000)

  it('keeps saved EdgeTTS cloud voice preference aligned after settings reload', async () => {
    saveSettingsPreferences({ voicePreferredType: 'edge_cloud' })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.vm.interviewPreferenceForm.voicePreferredType).toBe('edge_cloud')
    expect(wrapper.vm.userAiConfigForm.ttsProvider).toBe('edge')
    expect(wrapper.vm.userAiConfigForm.ttsBaseUrl).toBe('https://speech.platform.bing.com')
    expect(wrapper.vm.userAiConfigForm.ttsModel).toBe('edge-tts')
    expect(wrapper.vm.userAiConfigForm.ttsVoiceId).toBe('zh-CN-XiaoxiaoNeural')
    expect(wrapper.vm.userTtsConfigExpanded).toBe(true)
  }, 15000)

  it('applies preset rate and pitch when a bound browser voice preset is selected', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.voiceSpeakingRate = 1.2
    wrapper.vm.interviewPreferenceForm.voicePitch = 0.8
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'slow_clear'
    wrapper.vm.handleVoicePreferredTypeChange()
    await flushPromises()

    expect(wrapper.vm.interviewPreferenceForm.voiceSpeakingRate).toBe(0.75)
    expect(wrapper.vm.interviewPreferenceForm.voicePitch).toBe(1.02)
    expect(getSettingsPreferences()).toMatchObject({
      voicePreferredType: 'slow_clear',
      voiceSpeakingRate: 0.75,
      voicePitch: 1.02
    })
  }, 15000)

  it('shows the actual browser voice when male preference is degraded in Chrome', async () => {
    saveSettingsPreferences({ voicePreferredType: 'male' })
    window.speechSynthesis.getVoices = vi.fn(() => [
      {
        name: 'Microsoft Xiaoxiao Natural',
        lang: 'zh-CN',
        voiceURI: 'xiaoxiao-uri',
        localService: true
      },
      {
        name: 'Google 普通话（中国大陆）',
        lang: 'zh-CN',
        voiceURI: 'google-zh-cn',
        localService: false
      }
    ])
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    const status = wrapper.find('[data-testid="browser-tts-voice-status"]')
    expect(status.exists()).toBe(true)
    expect(status.text()).toContain('没有暴露中文男声')
    expect(status.text()).toContain('Google 普通话（中国大陆）')
  }, 15000)

  it('labels Chrome presets when only one Chinese browser voice is exposed', async () => {
    setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')
    window.speechSynthesis.getVoices = vi.fn(() => [
      {
        name: 'Google 普通话（中国大陆）',
        lang: 'zh-CN',
        voiceURI: 'google-zh-cn',
        localService: false
      }
    ])
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    const status = wrapper.find('[data-testid="browser-tts-voice-status"]')
    const generalGroup = wrapper.vm.voicePreferredTypeOptions.find((group) => group.label.includes('通用'))
    const newsAnchorOption = generalGroup.children.find((option) => option.value === 'news_anchor')
    const slowClearOption = generalGroup.children.find((option) => option.value === 'slow_clear')

    expect(status.text()).toContain('Chrome 当前只暴露 1 种中文浏览器 voice')
    expect(status.text()).toContain('多个预设会共用同一音色')
    expect(status.text()).toContain('Google 普通话（中国大陆）')
    expect(newsAnchorOption.disabled).toBe(false)
    expect(newsAnchorOption.label).toContain('Chrome 共用 1 种 voice')
    expect(slowClearOption.label).toContain('Chrome 共用 1 种 voice')
  }, 15000)

  it('keeps browser TTS status below the voice controls to avoid squeezing the row', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    const status = wrapper.find('[data-testid="browser-tts-voice-status"]')
    expect(wrapper.find('.voice-preference-main').exists()).toBe(true)
    expect(status.exists()).toBe(true)
    expect(status.element.parentElement.classList.contains('voice-preference-row')).toBe(true)
    expect(status.element.previousElementSibling.classList.contains('voice-preference-main')).toBe(true)
  }, 15000)

  it('does not show a gendered Chrome voice as the default Chinese natural voice fallback', async () => {
    saveSettingsPreferences({ voicePreferredType: 'natural_zh' })
    window.speechSynthesis.getVoices = vi.fn(() => [
      {
        name: 'Microsoft Kangkang - Chinese (Simplified, PRC)',
        lang: 'zh-CN',
        voiceURI: 'kangkang-uri',
        localService: true
      }
    ])
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'interview')
    wrapper.vm.interviewSubTab = 'voice'
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.vm.previewTextToSpeech.voice.value).toBeNull()
    expect(wrapper.vm.browserTtsVoiceStatusText).toContain('浏览器默认')
    expect(wrapper.vm.browserTtsVoiceStatusText).not.toContain('Microsoft Kangkang')
  }, 15000)

  it('loads server settings and renders resume retention preference', async () => {
    getUserSettings.mockResolvedValueOnce({
      data: {
        interviewRetentionDays: 30,
        resumeRetentionDays: 90
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'dataManagement')

    expect(getUserSettings).toHaveBeenCalled()
    expect(wrapper.text()).toContain('简历诊断保留天数')
    expect(getSettingsPreferences()).toMatchObject({
      interviewRetentionDays: 30,
      resumeRetentionDays: 90
    })
  }, 15000)

  it('does not change local preferences when server settings save fails', async () => {
    saveSettingsPreferences({ interviewRetentionDays: 30, resumeRetentionDays: 90 })
    getUserSettings.mockResolvedValueOnce({
      data: {
        interviewRetentionDays: 30,
        resumeRetentionDays: 90
      }
    })
    saveUserSettings.mockRejectedValueOnce(new Error('failed'))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 365
    await expect(wrapper.vm.handleDataManagementSettingsSave()).rejects.toThrow('failed')

    expect(getSettingsPreferences()).toMatchObject({
      interviewRetentionDays: 30,
      resumeRetentionDays: 90
    })
  })

  it('saves data management retention settings only after explicit save', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 90
    wrapper.vm.interviewPreferenceForm.resumeRetentionDays = 180

    expect(saveUserSettings).not.toHaveBeenCalled()
    await wrapper.vm.handleDataManagementSettingsSave()

    expect(saveUserSettings).toHaveBeenCalledWith({
      interviewRetentionDays: 90,
      resumeRetentionDays: 180
    })
    expect(getSettingsPreferences()).toMatchObject({
      interviewRetentionDays: 90,
      resumeRetentionDays: 180
    })
  })

  it('renders enabled destructive backend actions', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'security')
    expect(wrapper.text()).toContain('注销账号')

    await switchSection(wrapper, 'dataManagement')
    expect(wrapper.text()).toContain('面试记录清理')
    expect(wrapper.text()).toContain('简历诊断清理')
    expect(wrapper.findAll('button').some((button) => button.text().includes('待后端接入'))).toBe(false)
    const destructiveButtons = wrapper.findAll('button').filter((button) => ['清理记录'].includes(button.text()))
    expect(destructiveButtons).toHaveLength(2)
    expect(destructiveButtons.every((button) => button.attributes('disabled') === undefined)).toBe(true)
  })

  it('deletes account and clears login state after password and security confirmation', async () => {
    localStorage.setItem('ai_resume_token', 'user-token')
    localStorage.setItem('ai_resume_token_type', 'Bearer')
    const wrapper = mountView()
    await flushPromises()

    await wrapper.vm.handleAccountDelete({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })

    expect(deleteAccount).toHaveBeenCalledWith({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })
    expect(localStorage.getItem('ai_resume_token')).toBeNull()
    expect(clearUserInfo).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('clears interview and resume history then refreshes account overview', async () => {
    const wrapper = mountView()
    await flushPromises()
    getGrowthOverview.mockClear()

    await wrapper.vm.handleInterviewHistoryClear()
    await wrapper.vm.handleResumeHistoryClear()

    expect(clearInterviewHistory).toHaveBeenCalled()
    expect(clearResumeHistory).toHaveBeenCalled()
    expect(getGrowthOverview).toHaveBeenCalledTimes(2)
  })

  it('keeps login state when account deletion api fails', async () => {
    deleteAccount.mockRejectedValueOnce(new Error('failed'))
    localStorage.setItem('ai_resume_token', 'user-token')
    const wrapper = mountView()
    await flushPromises()

    await expect(wrapper.vm.handleAccountDelete({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })).rejects.toThrow('failed')

    expect(localStorage.getItem('ai_resume_token')).toBe('user-token')
    expect(clearUserInfo).not.toHaveBeenCalled()
  }, 15000)

  it('loads security question without blocking account delete form before confirmation dialog', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'security')
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()

    expect(getCurrentAccountSecurityQuestion).toHaveBeenCalled()
    expect(wrapper.text()).toContain('注销后不可恢复')
    expect(wrapper.text()).toContain('你的出生城市是哪里？')
    const submitButton = wrapper.find('.account-delete-form button.el-button--danger')
    expect(submitButton.attributes('disabled')).toBeUndefined()
    expect(submitButton.text()).toContain('确认注销')
  }, 15000)

  it('starts account deletion countdown only inside the confirmation dialog', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.useFakeTimers()

    expect(wrapper.vm.accountDeleteCountdown).toBe(0)

    wrapper.vm.onDialogOpen()

    expect(wrapper.vm.accountDeleteCountdown).toBe(15)
    expect(wrapper.vm.dialogConfirmButtonText).toBe('等待 15 秒')

    vi.advanceTimersByTime(3000)
    expect(wrapper.vm.accountDeleteCountdown).toBe(12)

    vi.advanceTimersByTime(12000)
    expect(wrapper.vm.accountDeleteCountdown).toBe(0)
    expect(wrapper.vm.dialogConfirmButtonText).toBe('确认注销')
  })

  it('submits account deletion from security tab after cooldown with existing payload', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'security')
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()
    wrapper.vm.accountDeleteForm = {
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    }
    await flushPromises()

    await wrapper.vm.handleAccountDeleteSubmit()
    expect(deleteAccount).not.toHaveBeenCalled()

    wrapper.vm.accountDeleteCountdown = 0
    wrapper.vm.accountDeleteConfirmText = wrapper.vm.accountDeleteExpectedText
    await wrapper.vm.handleDialogConfirm()

    expect(deleteAccount).toHaveBeenCalledWith({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })
  }, 15000)

  it('opens text confirmation before deleting account', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'security')
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()
    wrapper.vm.accountDeleteForm = {
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    }
    await flushPromises()

    await expect(wrapper.vm.handleAccountDeleteSubmit()).resolves.toBeUndefined()

    expect(wrapper.vm.accountDeleteConfirmDialogVisible).toBe(true)
    expect(deleteAccount).not.toHaveBeenCalled()
  }, 15000)

  it('collapses and expands long account deletion security questions', async () => {
    const wrapper = mountView()
    await flushPromises()

    await switchSection(wrapper, 'security')
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()
    await flushPromises()
    wrapper.vm.accountDeleteSecurityQuestion = '这是一条非常长的安全问题文本用于验证默认折叠展示，包含足够多的文字以避免撑破注销账号表单布局'
    await flushPromises()

    const card = wrapper.find('.security-question-card')
    const toggle = wrapper.find('.security-question-toggle')
    expect(toggle.exists()).toBe(true)
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(card.classes()).not.toContain('expanded')

    await toggle.trigger('click')

    expect(wrapper.find('.security-question-toggle').attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('.security-question-card').classes()).toContain('expanded')
  }, 15000)

  it('shows account data overview from existing growth overview api', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'privacy')

    expect(getGrowthOverview).toHaveBeenCalled()
    expect(wrapper.text()).toContain('简历诊断次数')
    expect(wrapper.text()).toContain('模拟面试次数')
    expect(wrapper.text()).toContain('JD 匹配次数')
    expect(wrapper.text()).toContain('AI 润色次数')
    expect(wrapper.text()).toContain('7')
    expect(wrapper.text()).toContain('5')
  })

  it('clears local settings cache without removing login tokens', async () => {
    localStorage.setItem('ai_resume_token', 'user-token')
    localStorage.setItem('ai_resume_admin_token', 'admin-token')
    localStorage.setItem('theme', 'dark')
    localStorage.setItem('followSystem', 'false')
    saveSettingsPreferences({ defaultInterviewMode: 'stress' })

    const wrapper = mountView()
    await flushPromises()
    wrapper.vm.handleClearLocalCache()

    expect(localStorage.getItem('theme')).toBeNull()
    expect(localStorage.getItem('followSystem')).toBeNull()
    expect(getSettingsPreferences().defaultInterviewMode).toBe('normal')
    expect(localStorage.getItem('ai_resume_token')).toBe('user-token')
    expect(localStorage.getItem('ai_resume_admin_token')).toBe('admin-token')
  })

  it('submits user feedback from settings center and resets form', async () => {
    const wrapper = mountView()
    await flushPromises()
    await switchSection(wrapper, 'feedback')

    wrapper.vm.feedbackForm = {
      type: 'suggestion',
      title: ' 增加导出提示 ',
      content: '这里是一段超过十个字符的功能建议内容',
      contact: 'user@example.com'
    }
    await wrapper.vm.handleFeedbackSubmit()

    expect(createUserFeedback).toHaveBeenCalledWith({
      type: 'suggestion',
      title: '增加导出提示',
      content: '这里是一段超过十个字符的功能建议内容',
      contact: 'user@example.com'
    })
    expect(wrapper.vm.feedbackForm).toMatchObject({
      type: 'bug',
      title: '',
      content: '',
      contact: ''
    })
  })
})
