import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  deleteAdminFeedbackBatch,
  getAdminFeedbackDetail,
  getAdminFeedbackList,
  updateAdminFeedbackStatus
} from '@/api/admin/feedback'

describe('admin feedback API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminFeedbackList sends defaults', async () => {
    await getAdminFeedbackList()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/feedback',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })

  it('getAdminFeedbackList merges filters', async () => {
    await getAdminFeedbackList({ page: 2, type: 'bug', status: 0, userId: 10 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/feedback',
      method: 'get',
      params: { page: 2, size: 20, type: 'bug', status: 0, userId: 10 }
    })
  })

  it('getAdminFeedbackDetail calls detail endpoint', async () => {
    await getAdminFeedbackDetail(100)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/feedback/100', method: 'get' })
  })

  it('updateAdminFeedbackStatus sends status payload', async () => {
    const data = { status: 2, adminRemark: '已处理' }
    await updateAdminFeedbackStatus(100, data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/feedback/100/status', method: 'put', data })
  })

  it('deleteAdminFeedbackBatch sends ids', async () => {
    await deleteAdminFeedbackBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/feedback/batch-delete',
      method: 'post',
      data: [1, 2]
    })
  })
})
