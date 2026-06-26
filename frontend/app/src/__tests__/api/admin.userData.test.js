import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import { getAdminUserInterviews, getAdminUserResumeTasks } from '@/api/admin/userData'

describe('admin userData API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminUserInterviews calls correct endpoint', async () => {
    await getAdminUserInterviews(10)
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users/10/interviews', method: 'get', params: { page: 1, size: 20 }
    })
  })

  it('getAdminUserResumeTasks calls correct endpoint', async () => {
    await getAdminUserResumeTasks(10, { page: 2 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/users/10/resume-tasks', method: 'get', params: { page: 2, size: 20 }
    })
  })
})
