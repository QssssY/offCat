import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  getAdminNotifications,
  getAdminNotificationDetail,
  createAdminNotification,
  publishAdminNotification,
  publishAdminNotificationsBatch,
  deleteAdminNotification,
  deleteAdminNotificationsBatch
} from '@/api/admin/notifications'

describe('admin notifications API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminNotifications calls correct endpoint', async () => {
    await getAdminNotifications()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/notifications',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })

  it('getAdminNotifications merges pagination params', async () => {
    await getAdminNotifications({ page: 3, size: 10 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/notifications',
      method: 'get',
      params: { page: 3, size: 10 }
    })
  })

  it('getAdminNotifications forwards non-empty filter params only', async () => {
    await getAdminNotifications({ page: 2, size: 10, type: 'activity', status: 0, targetType: 'vip', keyword: '维护', empty: '' })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/notifications',
      method: 'get',
      params: {
        page: 2,
        size: 10,
        type: 'activity',
        status: 0,
        targetType: 'vip',
        keyword: '维护'
      }
    })
  })

  it('getAdminNotificationDetail calls correct endpoint', async () => {
    await getAdminNotificationDetail(123)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/notifications/123', method: 'get' })
  })

  it('createAdminNotification sends data', async () => {
    const data = { title: 'Test', content: 'Content', type: 'system', targetType: 'all', status: 1 }
    await createAdminNotification(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/notifications', method: 'post', data })
  })

  it('publishAdminNotification calls correct endpoint', async () => {
    await publishAdminNotification(456)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/notifications/456/publish', method: 'put' })
  })

  it('publishAdminNotificationsBatch sends ids', async () => {
    await publishAdminNotificationsBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/notifications/batch/publish',
      method: 'put',
      data: [1, 2]
    })
  })

  it('deleteAdminNotification calls correct endpoint', async () => {
    await deleteAdminNotification(789)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/notifications/789', method: 'delete' })
  })

  it('deleteAdminNotificationsBatch sends ids', async () => {
    await deleteAdminNotificationsBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/notifications/batch-delete',
      method: 'post',
      data: [1, 2]
    })
  })
})
