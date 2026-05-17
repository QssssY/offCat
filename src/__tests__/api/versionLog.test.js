import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import request from '@/utils/request'
import { getLatestVersionLogs } from '@/api/versionLog'
import { getPublicVersionLogsPage } from '@/api/publicVersionLog'

describe('public versionLog API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getLatestVersionLogs sends limit param', async () => {
    await getLatestVersionLogs(3)
    expect(request).toHaveBeenCalledWith({
      url: '/api/version-logs/latest', method: 'get', params: { limit: 3 }
    })
  })

  it('getLatestVersionLogs uses default limit', async () => {
    await getLatestVersionLogs()
    expect(request).toHaveBeenCalledWith({
      url: '/api/version-logs/latest', method: 'get', params: { limit: 5 }
    })
  })

  it('getPublicVersionLogsPage sends pagination params', async () => {
    await getPublicVersionLogsPage({ page: 2, size: 20 })
    expect(request).toHaveBeenCalledWith({
      url: '/api/version-logs',
      method: 'get',
      params: { page: 2, size: 20 }
    })
  })

  it('getPublicVersionLogsPage uses default pagination', async () => {
    await getPublicVersionLogsPage()
    expect(request).toHaveBeenCalledWith({
      url: '/api/version-logs',
      method: 'get',
      params: { page: 1, size: 10 }
    })
  })
})
