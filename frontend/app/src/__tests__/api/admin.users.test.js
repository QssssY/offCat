import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  banAdminUser,
  banAdminUsersBatch,
  getAdminUsers,
  getAdminUserStats,
  unbanAdminUser,
  unbanAdminUsersBatch,
  updateUsersBatchStatus
} from '@/api/admin/users'

describe('admin users API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('updateUsersBatchStatus sends string ids to avoid precision loss', async () => {
    await updateUsersBatchStatus(['9007199254740993', 123], 0)

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users/batch/status',
      method: 'put',
      data: {
        ids: ['9007199254740993', '123'],
        isActive: 0
      }
    })
  })

  it('getAdminUsers passes server-side pagination and vip state filters', async () => {
    await getAdminUsers({
      page: 2,
      size: 50,
      keyword: 'alice',
      role: 1,
      status: 1,
      vipState: 'active'
    })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users',
      method: 'get',
      params: {
        page: 2,
        size: 50,
        keyword: 'alice',
        role: 1,
        status: 1,
        vipState: 'active'
      }
    })
  })

  it('getAdminUserStats requests the admin user stats endpoint', async () => {
    await getAdminUserStats()

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users/stats',
      method: 'get'
    })
  })

  it('banAdminUser sends duration and reason to the new ban endpoint', async () => {
    await banAdminUser('9007199254740993', { duration: '7d', reason: '违规发布色情内容' })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users/9007199254740993/ban',
      method: 'put',
      data: { duration: '7d', reason: '违规发布色情内容' }
    })
  })

  it('unbanAdminUser sends optional reason to the new unban endpoint', async () => {
    await unbanAdminUser(123, { reason: '申诉通过' })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users/123/unban',
      method: 'put',
      data: { reason: '申诉通过' }
    })
  })

  it('batch ban and unban endpoints keep ids as strings', async () => {
    await banAdminUsersBatch({ ids: ['9007199254740993', 123], duration: '30d', reason: '批量风控' })
    await unbanAdminUsersBatch({ ids: ['9007199254740993', 123], reason: '批量恢复' })

    expect(adminRequest).toHaveBeenNthCalledWith(1, {
      url: '/api/admin/users/batch/ban',
      method: 'put',
      data: {
        ids: ['9007199254740993', '123'],
        duration: '30d',
        reason: '批量风控'
      }
    })
    expect(adminRequest).toHaveBeenNthCalledWith(2, {
      url: '/api/admin/users/batch/unban',
      method: 'put',
      data: {
        ids: ['9007199254740993', '123'],
        reason: '批量恢复'
      }
    })
  })
})
