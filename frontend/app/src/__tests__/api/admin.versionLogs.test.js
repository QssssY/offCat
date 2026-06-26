import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  getAdminVersionLogs,
  createAdminVersionLog,
  updateAdminVersionLog,
  publishAdminVersionLog,
  publishAdminVersionLogsBatch,
  deleteAdminVersionLog,
  deleteAdminVersionLogsBatch
} from '@/api/admin/versionLogs'

describe('admin versionLogs API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminVersionLogs calls correct endpoint', async () => {
    await getAdminVersionLogs()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/version-logs',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })

  it('getAdminVersionLogs merges pagination params', async () => {
    await getAdminVersionLogs({ page: 2, size: 50 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/version-logs',
      method: 'get',
      params: { page: 2, size: 50 }
    })
  })

  it('createAdminVersionLog sends data', async () => {
    const data = { version: '2.0.0', title: 'Release', type: 'major', content: 'Notes', status: 1 }
    await createAdminVersionLog(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/version-logs', method: 'post', data })
  })

  it('updateAdminVersionLog sends data', async () => {
    const data = { id: 1, title: 'Updated' }
    await updateAdminVersionLog(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/version-logs', method: 'put', data })
  })

  it('publishAdminVersionLog calls correct endpoint', async () => {
    await publishAdminVersionLog(1)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/version-logs/1/publish', method: 'put' })
  })

  it('publishAdminVersionLogsBatch sends ids', async () => {
    await publishAdminVersionLogsBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/version-logs/batch/publish',
      method: 'put',
      data: [1, 2]
    })
  })

  it('deleteAdminVersionLog calls correct endpoint', async () => {
    await deleteAdminVersionLog(1)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/version-logs/1', method: 'delete' })
  })

  it('deleteAdminVersionLogsBatch sends ids', async () => {
    await deleteAdminVersionLogsBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/version-logs/batch-delete',
      method: 'post',
      data: [1, 2]
    })
  })
})
