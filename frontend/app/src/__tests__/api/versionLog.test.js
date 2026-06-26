import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import request from '@/utils/request'
import { clearApiCache } from '@/utils/apiCache'
import { getLatestVersionLogs } from '@/api/versionLog'
import { getPublicVersionLogsPage } from '@/api/publicVersionLog'

describe('public versionLog API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearApiCache()
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

  it('reuses latest version log requests with the same limit during the cache window', async () => {
    await getLatestVersionLogs(3)
    await getLatestVersionLogs(3)

    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith({
      url: '/api/version-logs/latest', method: 'get', params: { limit: 3 }
    })
  })

  it('keeps latest version log cache entries isolated by limit', async () => {
    await getLatestVersionLogs(3)
    await getLatestVersionLogs(5)
    await getLatestVersionLogs(3)

    expect(request).toHaveBeenCalledTimes(2)
    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/api/version-logs/latest', method: 'get', params: { limit: 3 }
    })
    expect(request).toHaveBeenNthCalledWith(2, {
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

  it('reuses public version log page requests with the same pagination during the cache window', async () => {
    await getPublicVersionLogsPage({ page: 2, size: 20 })
    await getPublicVersionLogsPage({ page: 2, size: 20 })

    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith({
      url: '/api/version-logs',
      method: 'get',
      params: { page: 2, size: 20 }
    })
  })

  it('keeps public version log page cache entries isolated by pagination', async () => {
    await getPublicVersionLogsPage({ page: 1, size: 10 })
    await getPublicVersionLogsPage({ page: 2, size: 10 })
    await getPublicVersionLogsPage({ page: 1, size: 10 })

    expect(request).toHaveBeenCalledTimes(2)
    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/api/version-logs',
      method: 'get',
      params: { page: 1, size: 10 }
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/api/version-logs',
      method: 'get',
      params: { page: 2, size: 10 }
    })
  })
})
