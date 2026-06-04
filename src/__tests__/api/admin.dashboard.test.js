import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import { getAdminDashboardSummary } from '@/api/admin/dashboard'

describe('admin dashboard API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('dashboard summary endpoint sends date range and hot role limit once', async () => {
    await getAdminDashboardSummary({
      startDate: '2026-06-01',
      endDate: '2026-06-07',
      limit: 8
    })

    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/dashboard/summary',
      method: 'get',
      params: {
        startDate: '2026-06-01',
        endDate: '2026-06-07',
        limit: 8
      }
    })
  })
})
