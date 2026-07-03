import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import AdminAiEngineView from '@/views/admin/AdminAiEngineView.vue'
import {
  fetchAdminAiModels,
  getCustomAiDailyLimit,
  getAdminAiEngines,
  getCustomAiUsageTrends,
  getCustomAiUsageStats,
  testAdminAiEngineConnectivity,
  updateCustomAiDailyLimit
} from '@/api/admin/aiEngines'
import {
  discoverAdminTtsOptions,
  getAdminTtsConfig,
  previewAdminTtsVoice,
  saveAdminTtsConfig,
  testAdminTtsConnectivity
} from '@/api/admin/ttsConfig'
import { showAdminSuccess } from '@/utils/adminFeedback'

let currentWrapper = null

vi.mock('vue-chartjs', () => ({
  Line: {
    name: 'Line',
    props: ['data', 'options', 'height'],
    template: '<div class="line-chart-stub">趋势图</div>'
  }
}))

vi.mock('chart.js', () => ({
  Chart: { register: vi.fn() },
  Title: {},
  Tooltip: {},
  Legend: {},
  CategoryScale: {},
  LinearScale: {},
  LineElement: {},
  PointElement: {},
  Filler: {}
}))

vi.mock('@/api/admin/aiEngines', () => ({
  createAdminAiEngine: vi.fn(() => Promise.resolve({ data: 1 })),
  deleteAiEngine: vi.fn(() => Promise.resolve()),
  deleteAiEngines: vi.fn(() => Promise.resolve()),
  fetchAdminAiModels: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      message: '模型列表获取成功',
      models: [
        { id: 'gpt-4o-mini', name: 'gpt-4o-mini' },
        { id: 'gpt-4.1-mini', name: 'gpt-4.1-mini' }
      ],
      latencyMs: 15
    }
  })),
  getAdminAiEngines: vi.fn(() => Promise.resolve({ data: [] })),
  getCustomAiDailyLimit: vi.fn(() => Promise.resolve({ data: { limit: 50 } })),
  getCustomAiUsageStats: vi.fn(() => Promise.resolve({
    data: {
      startDate: '2026-05-28',
      endDate: '2026-06-03',
      configuredUserCount: 3,
      activeUserCount: 2,
      totalCalls: 17,
      totalUsers: 1,
      page: 1,
      pageSize: 5,
      typeStats: [
        { usageType: 'resume_diagnosis', usageTypeDesc: '简历诊断', callCount: 7 },
        { usageType: 'interview_message', usageTypeDesc: '面试消息', callCount: 10 }
      ],
      userStats: [
        {
          userId: 10,
          username: 'alice',
          nickname: 'Alice',
          totalCalls: 17,
          typeStats: [
            { usageType: 'resume_diagnosis', usageTypeDesc: '简历诊断', callCount: 7 },
            { usageType: 'interview_message', usageTypeDesc: '面试消息', callCount: 10 }
          ]
        }
      ]
    }
  })),
  getCustomAiUsageTrends: vi.fn(() => Promise.resolve({
    data: {
      startDate: '2026-05-28',
      endDate: '2026-06-03',
      totalCalls: 21,
      activeUserCount: 5,
      days: [
        { date: '2026-05-28', totalCalls: 0, activeUserCount: 0, typeStats: [] },
        {
          date: '2026-05-29',
          totalCalls: 8,
          activeUserCount: 2,
          typeStats: [
            { usageType: 'resume_diagnosis', usageTypeDesc: '简历诊断', callCount: 8 }
          ]
        },
        { date: '2026-05-30', totalCalls: 0, activeUserCount: 0, typeStats: [] },
        { date: '2026-05-31', totalCalls: 0, activeUserCount: 0, typeStats: [] },
        { date: '2026-06-01', totalCalls: 4, activeUserCount: 1, typeStats: [] },
        { date: '2026-06-02', totalCalls: 3, activeUserCount: 1, typeStats: [] },
        { date: '2026-06-03', totalCalls: 6, activeUserCount: 2, typeStats: [] }
      ]
    }
  })),
  testAdminAiEngineConnectivity: vi.fn(),
  toggleAdminAiEngineActive: vi.fn(() => Promise.resolve()),
  toggleAiEnginesBatchActive: vi.fn(() => Promise.resolve()),
  updateAdminAiEngine: vi.fn(() => Promise.resolve()),
  updateCustomAiDailyLimit: vi.fn(() => Promise.resolve({ data: { limit: 80 } }))
}))

vi.mock('@/api/admin/ttsConfig', () => ({
  discoverAdminTtsOptions: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      message: 'TTS 模型和音色获取成功',
      models: [{ id: 'tts-1', name: 'tts-1' }],
      voices: [{ id: 'alloy', name: 'alloy' }],
      ttsEndpointPath: '/audio/speech'
    }
  })),
  getAdminTtsConfig: vi.fn(() => Promise.resolve({
    data: {
      enabled: true,
      configured: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      apiKey: 'sys****1234',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    }
  })),
  previewAdminTtsVoice: vi.fn(() => Promise.resolve(new Blob(['mp3'], { type: 'audio/mpeg' }))),
  saveAdminTtsConfig: vi.fn(() => Promise.resolve({ data: { enabled: true, configured: true } })),
  testAdminTtsConnectivity: vi.fn(() => Promise.resolve({
    data: {
      success: true,
      message: 'TTS 连通测试成功',
      endpointPath: '/audio/speech',
      latencyMs: 18
    }
  }))
}))

vi.mock('@/utils/adminFeedback', () => ({
  confirmAdminRiskAction: vi.fn(() => Promise.resolve()),
  resolveAdminTableEmptyText: vi.fn(() => '暂无 AI 引擎配置'),
  showAdminError: vi.fn(),
  showAdminSuccess: vi.fn(),
  showAdminWarning: vi.fn()
}))

const mountView = async () => {
  currentWrapper = mount(AdminAiEngineView, {
    attachTo: document.body,
    global: {
      plugins: [ElementPlus],
      stubs: {
        transition: false
      }
    }
  })
  await flushPromises()
  return currentWrapper
}

const buildExpectedRecentRange = (days) => {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - (days - 1))
  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = `${date.getMonth() + 1}`.padStart(2, '0')
    const day = `${date.getDate()}`.padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  return [formatDate(start), formatDate(end)]
}

const switchToCustomAiUsageSection = async (wrapper) => {
  await wrapper.find('[data-admin-section="custom-ai-usage"]').trigger('click')
  await nextTick()
}

const switchToSystemTtsSection = async (wrapper) => {
  await wrapper.find('[data-admin-section="system-tts-config"]').trigger('click')
  await nextTick()
}

describe('AdminAiEngineView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    testAdminAiEngineConnectivity.mockResolvedValue({
      data: {
        success: true,
        message: '连通测试成功',
        latencyMs: 12,
        responsePreview: 'ok'
      }
    })
  })

  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
  })

  it('should call connectivity test API with current form values and show result', async () => {
    const wrapper = await mountView()
    wrapper.vm.openCreateDialog()
    await nextTick()
    Object.assign(wrapper.vm.formData, {
      engineCode: 'test-engine',
      engineName: '测试引擎',
      providerType: 'openai',
      businessType: 'interview',
      modelName: 'gpt-test',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      thinkingMode: 'none',
      temperature: 0.2,
      maxTokens: 64,
      timeoutMs: 30000,
      isActive: 1,
      sort: 0
    })

    await wrapper.vm.handleConnectivityTest()
    await flushPromises()

    expect(getAdminAiEngines).toHaveBeenCalled()
    expect(testAdminAiEngineConnectivity).toHaveBeenCalledWith({
      id: null,
      providerType: 'openai',
      modelName: 'gpt-test',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      thinkingMode: 'none',
      temperature: 0.2,
      maxTokens: 64,
      timeoutMs: 30000
    })
    expect(wrapper.vm.connectivityTestResult).toMatchObject({
      success: true,
      message: '连通测试成功',
      latencyMs: 12,
      responsePreview: 'ok'
    })
    expect(showAdminSuccess).toHaveBeenCalledWith('连通测试成功')
  }, 10000)

  it('should fetch admin AI model options and select the first model when empty', async () => {
    const wrapper = await mountView()
    wrapper.vm.openCreateDialog()
    await nextTick()
    Object.assign(wrapper.vm.formData, {
      providerType: 'openai',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      modelName: '',
      timeoutMs: 30000
    })

    await wrapper.vm.handleModelFetch()
    await flushPromises()

    expect(fetchAdminAiModels).toHaveBeenCalledWith({
      id: undefined,
      providerType: 'openai',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      timeoutMs: 30000
    })
    expect(wrapper.vm.modelOptions).toEqual([
      { id: 'gpt-4o-mini', name: 'gpt-4o-mini' },
      { id: 'gpt-4.1-mini', name: 'gpt-4.1-mini' }
    ])
    expect(wrapper.vm.formData.modelName).toBe('gpt-4o-mini')
  })

  it('should keep manually entered admin model name when model discovery fails', async () => {
    fetchAdminAiModels.mockResolvedValueOnce({
      data: {
        success: false,
        message: '获取失败',
        models: []
      }
    })
    const wrapper = await mountView()
    wrapper.vm.openCreateDialog()
    await nextTick()
    Object.assign(wrapper.vm.formData, {
      providerType: 'openai',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      modelName: 'manual-model',
      timeoutMs: 30000
    })

    await wrapper.vm.handleModelFetch()
    await flushPromises()

    expect(wrapper.vm.formData.modelName).toBe('manual-model')
  })

  it('should display and update custom AI daily limit', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)

    expect(getCustomAiDailyLimit).toHaveBeenCalled()
    expect(wrapper.text()).toContain('用户自定义 AI 每日上限')
    expect(wrapper.text()).toContain('50')

    wrapper.vm.customAiDailyLimitForm.limit = 80
    await wrapper.vm.handleCustomAiDailyLimitSave()
    await flushPromises()

    expect(updateCustomAiDailyLimit).toHaveBeenCalledWith(80)
    expect(wrapper.vm.customAiDailyLimit).toBe(80)
  })

  it('should keep engine config as default section and hide custom AI usage layout', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('[data-admin-section="engine-config"]').attributes('aria-selected')).toBe('true')
    expect(wrapper.find('.filter-bar').exists()).toBe(true)
    expect(wrapper.find('.table-card').exists()).toBe(true)
    expect(wrapper.find('.custom-ai-usage-card').exists()).toBe(false)
  })

  it('should render system TTS tab and load current config', async () => {
    const wrapper = await mountView()
    await switchToSystemTtsSection(wrapper)

    expect(getAdminTtsConfig).toHaveBeenCalled()
    expect(wrapper.find('.system-tts-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('系统 TTS 配置')
    expect(wrapper.vm.systemTtsForm).toMatchObject({
      enabled: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    })
  })

  it('should save and test system TTS config with current form values', async () => {
    const wrapper = await mountView()
    await switchToSystemTtsSection(wrapper)
    Object.assign(wrapper.vm.systemTtsForm, {
      enabled: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      apiKey: 'sys-real-key',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    })

    await wrapper.vm.handleSystemTtsSave()
    await wrapper.vm.handleSystemTtsConnectivityTest()
    await flushPromises()

    expect(saveAdminTtsConfig).toHaveBeenCalledWith({
      enabled: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      apiKey: 'sys-real-key',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    })
    expect(testAdminTtsConnectivity).toHaveBeenCalled()
    expect(wrapper.vm.systemTtsConnectivityResult).toMatchObject({ success: true })
  })

  it('should discover and preview system TTS voice', async () => {
    const wrapper = await mountView()
    await switchToSystemTtsSection(wrapper)
    Object.assign(wrapper.vm.systemTtsForm, {
      enabled: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      apiKey: 'sys-real-key',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    })

    await wrapper.vm.handleSystemTtsDiscover()
    await wrapper.vm.handleSystemTtsPreview()
    await flushPromises()

    expect(discoverAdminTtsOptions).toHaveBeenCalled()
    expect(previewAdminTtsVoice).toHaveBeenCalled()
    expect(wrapper.vm.systemTtsDiscoveryResult.voices).toEqual([{ id: 'alloy', name: 'alloy' }])
  })

  it('should offer EdgeTTS as a no-key system TTS provider preset', async () => {
    const wrapper = await mountView()
    await switchToSystemTtsSection(wrapper)

    wrapper.vm.handleSystemTtsProviderChange('edge')
    await flushPromises()

    expect(wrapper.vm.systemTtsForm).toMatchObject({
      ttsProvider: 'edge',
      baseUrl: 'https://speech.platform.bing.com',
      model: 'edge-tts',
      voiceId: 'zh-CN-XiaoxiaoNeural',
      endpointPath: '/consumer/speech/synthesize/readaloud/edge/v1'
    })
    expect(wrapper.vm.systemTtsDiscoveryResult.voices).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: 'zh-CN-XiaoxiaoNeural' }),
      expect.objectContaining({ id: 'zh-CN-YunxiNeural' })
    ]))
  })

  it('should offer Gemini MiniMax Qwen and xAI system TTS provider presets', async () => {
    const wrapper = await mountView()
    await switchToSystemTtsSection(wrapper)
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
      wrapper.vm.systemTtsForm.ttsProvider = preset.provider
      wrapper.vm.handleSystemTtsProviderChange(preset.provider)
      await flushPromises()

      expect(wrapper.vm.systemTtsForm).toMatchObject({
        ttsProvider: preset.provider,
        baseUrl: preset.baseUrl,
        model: preset.model,
        voiceId: preset.voiceId,
        endpointPath: preset.endpointPath
      })
      expect(wrapper.vm.systemTtsDiscoveryResult.models).toEqual([{ id: preset.model, name: preset.model }])
      expect(wrapper.vm.systemTtsDiscoveryResult.voices).toEqual(expect.arrayContaining(
        preset.voiceIds.map(id => expect.objectContaining({ id }))
      ))
    }
  })

  it('should display custom AI usage stats and user details', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    const [startDate, endDate] = buildExpectedRecentRange(7)

    expect(getCustomAiUsageStats).toHaveBeenCalledWith({
      startDate,
      endDate,
      page: 1,
      pageSize: 5
    })
    expect(wrapper.text()).toContain('近 7 天自定义 AI 调用')
    expect(wrapper.text()).toContain('17')
    expect(wrapper.text()).toContain('简历诊断')
    expect(wrapper.text()).toContain('面试消息')
    expect(wrapper.text()).toContain('alice')
    expect(wrapper.find('.custom-ai-usage-footer').exists()).toBe(true)
  })

  it('should request paged custom AI user usage stats when changing detail page', async () => {
    const wrapper = await mountView()
    const [startDate, endDate] = buildExpectedRecentRange(7)
    getCustomAiUsageStats.mockClear()

    await wrapper.vm.handleCustomAiUsagePageChange(2)
    await flushPromises()

    expect(getCustomAiUsageStats).toHaveBeenCalledWith({
      startDate,
      endDate,
      page: 2,
      pageSize: 5
    })
  })

  it('should reload custom AI usage stats when switching usage range preset', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    const [startDate, endDate] = buildExpectedRecentRange(30)
    getCustomAiUsageStats.mockClear()

    await wrapper.vm.handleCustomAiUsageRangePresetChange('last30')
    await flushPromises()

    expect(getCustomAiUsageStats).toHaveBeenCalledWith({
      startDate,
      endDate,
      page: 1,
      pageSize: 5
    })
  })

  it('should reload custom AI usage stats when custom usage date range changes', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    getCustomAiUsageStats.mockClear()

    await wrapper.vm.handleCustomAiUsageRangeChange(['2026-06-01', '2026-06-03'])
    await flushPromises()

    expect(getCustomAiUsageStats).toHaveBeenCalledWith({
      startDate: '2026-06-01',
      endDate: '2026-06-03',
      page: 1,
      pageSize: 5
    })
  })

  it('should request default seven-day custom AI usage trends on mount', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    const [startDate, endDate] = buildExpectedRecentRange(7)

    expect(getCustomAiUsageTrends).toHaveBeenCalledWith({ startDate, endDate })
    expect(wrapper.text()).toContain('用户自定义 AI 按日趋势')
    expect(wrapper.find('.line-chart-stub').exists()).toBe(true)
    expect(wrapper.find('.custom-ai-trend-toggle').attributes('aria-expanded')).toBe('true')
  })

  it('should collapse custom AI trend chart after toggling trend section', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)

    await wrapper.find('.custom-ai-trend-toggle').trigger('click')
    await nextTick()

    expect(wrapper.find('.custom-ai-trend-toggle').attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('.line-chart-stub').exists()).toBe(false)
  })

  it('should reload custom AI trends when switching to last thirty days', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    const [startDate, endDate] = buildExpectedRecentRange(30)
    getCustomAiUsageTrends.mockClear()

    await wrapper.vm.handleCustomAiTrendPresetChange('last30')
    await flushPromises()

    expect(getCustomAiUsageTrends).toHaveBeenCalledWith({ startDate, endDate })
  })

  it('should reload custom AI trends when custom date range changes', async () => {
    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    getCustomAiUsageTrends.mockClear()

    await wrapper.vm.handleCustomAiTrendRangeChange(['2026-06-01', '2026-06-03'])
    await flushPromises()

    expect(getCustomAiUsageTrends).toHaveBeenCalledWith({
      startDate: '2026-06-01',
      endDate: '2026-06-03'
    })
  })

  it('should render trend empty state when every day has no calls', async () => {
    getCustomAiUsageTrends.mockResolvedValueOnce({
      data: {
        startDate: '2026-06-01',
        endDate: '2026-06-07',
        totalCalls: 0,
        activeUserCount: 0,
        days: [
          { date: '2026-06-01', totalCalls: 0, activeUserCount: 0, typeStats: [] },
          { date: '2026-06-02', totalCalls: 0, activeUserCount: 0, typeStats: [] }
        ]
      }
    })

    const wrapper = await mountView()
    await switchToCustomAiUsageSection(wrapper)
    await nextTick()

    expect(wrapper.text()).toContain('暂无趋势数据')
    expect(wrapper.find('.line-chart-stub').exists()).toBe(false)
  })
})
