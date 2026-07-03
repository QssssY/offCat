import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  fetchAdminAiModels,
  getCustomAiDailyLimit,
  getCustomAiUsageTrends,
  getCustomAiUsageStats,
  testAdminAiEngineConnectivity,
  updateCustomAiDailyLimit
} from '@/api/admin/aiEngines'

describe('admin aiEngines API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('testAdminAiEngineConnectivity sends current form config', async () => {
    const data = {
      id: 10,
      providerType: 'openai',
      modelName: 'gpt-test',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      thinkingMode: 'none',
      temperature: 0,
      maxTokens: 8,
      timeoutMs: 30000
    }

    await testAdminAiEngineConnectivity(data)

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/ai-engines/connectivity-test',
      method: 'post',
      data
    })
  })

  it('fetchAdminAiModels sends current form credential fields', async () => {
    const data = {
      id: 10,
      providerType: 'openai',
      baseUrl: 'https://api.example.com/v1',
      apiKey: 'sk-real',
      timeoutMs: 30000
    }

    await fetchAdminAiModels(data)

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/ai-engines/models',
      method: 'post',
      data
    })
  })

  it('custom AI daily limit endpoints use admin custom-ai route', async () => {
    await getCustomAiDailyLimit()
    await updateCustomAiDailyLimit(80)

    expect(adminRequest).toHaveBeenNthCalledWith(1, {
      url: '/api/admin/custom-ai/daily-limit',
      method: 'get'
    })
    expect(adminRequest).toHaveBeenNthCalledWith(2, {
      url: '/api/admin/custom-ai/daily-limit',
      method: 'put',
      data: { limit: 80 }
    })
  })

  it('custom AI usage stats endpoint keeps legacy date param compatibility', async () => {
    await getCustomAiUsageStats({ date: '2026-06-03', page: 2, pageSize: 10 })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/custom-ai/usage-stats',
      method: 'get',
      params: {
        date: '2026-06-03',
        page: 2,
        pageSize: 10
      }
    })
  })

  it('custom AI usage stats endpoint sends date range and pagination params', async () => {
    await getCustomAiUsageStats({ startDate: '2026-06-01', endDate: '2026-06-07', page: 2, pageSize: 5 })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/custom-ai/usage-stats',
      method: 'get',
      params: {
        startDate: '2026-06-01',
        endDate: '2026-06-07',
        page: 2,
        pageSize: 5
      }
    })
  })

  it('custom AI usage trends endpoint sends date range params', async () => {
    await getCustomAiUsageTrends({ startDate: '2026-06-01', endDate: '2026-06-07' })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/custom-ai/usage-trends',
      method: 'get',
      params: {
        startDate: '2026-06-01',
        endDate: '2026-06-07'
      }
    })
  })
})
