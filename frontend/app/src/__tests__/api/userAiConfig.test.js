import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import request from '@/utils/request'
import {
  deleteUserAiConfig,
  fetchUserAiModels,
  getUserAiConfigs,
  getSystemTtsStatus,
  getUserAiUsage,
  saveUserAiConfig,
  testUserAiConnectivity,
  testUserTtsConnectivity,
  toggleUserAiConfig
} from '@/api/userAiConfig'

describe('userAiConfig API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should send user custom AI config requests to the documented endpoints', async () => {
    const payload = {
      configType: 'interview',
      providerName: 'DeepSeek',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-user',
      model: 'deepseek-chat',
      supportsMultimodal: true,
      ttsBaseUrl: 'https://tts.example.com/v1',
      ttsApiKey: 'tts-user',
      ttsModel: 'tts-1',
      ttsVoiceId: 'alloy'
    }

    await getUserAiConfigs()
    await saveUserAiConfig(payload)
    await toggleUserAiConfig('interview', false)
    await fetchUserAiModels(payload)
    await testUserAiConnectivity(payload)
    await testUserTtsConnectivity(payload)
    await getSystemTtsStatus()
    await getUserAiUsage()
    await deleteUserAiConfig('resume')

    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/api/user/ai-config',
      method: 'get'
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/api/user/ai-config',
      method: 'post',
      data: payload,
      skipDefaultErrorHandler: true
    })
    expect(request).toHaveBeenNthCalledWith(3, {
      url: '/api/user/ai-config/interview/toggle',
      method: 'put',
      data: { enabled: false }
    })
    expect(request).toHaveBeenNthCalledWith(4, {
      url: '/api/user/ai-config/models',
      method: 'post',
      data: {
        baseUrl: payload.baseUrl,
        apiKey: payload.apiKey
      },
      skipDefaultErrorHandler: true
    })
    expect(request).toHaveBeenNthCalledWith(5, {
      url: '/api/user/ai-config/test-connectivity',
      method: 'post',
      data: {
        baseUrl: payload.baseUrl,
        apiKey: payload.apiKey,
        model: payload.model,
        supportsMultimodal: payload.supportsMultimodal
      },
      skipDefaultErrorHandler: true
    })
    expect(request).toHaveBeenNthCalledWith(6, {
      url: '/api/user/ai-config/test-tts-connectivity',
      method: 'post',
      data: {
        baseUrl: payload.ttsBaseUrl,
        apiKey: payload.ttsApiKey,
        model: payload.ttsModel,
        voiceId: payload.ttsVoiceId
      },
      skipDefaultErrorHandler: true
    })
    expect(request).toHaveBeenNthCalledWith(7, {
      url: '/api/user/ai-config/system-tts-status',
      method: 'get'
    })
    expect(request).toHaveBeenNthCalledWith(8, {
      url: '/api/user/ai-config/usage',
      method: 'get'
    })
    expect(request).toHaveBeenNthCalledWith(9, {
      url: '/api/user/ai-config/resume',
      method: 'delete'
    })
  })
})
