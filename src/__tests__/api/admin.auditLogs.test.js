import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import { getAdminAuditLogs } from '@/api/admin/auditLogs'

describe('admin auditLogs API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminAuditLogs sends defaults', async () => {
    await getAdminAuditLogs()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/audit-logs',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })

  it('getAdminAuditLogs merges custom params', async () => {
    await getAdminAuditLogs({ userId: 10, page: 2 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/audit-logs',
      method: 'get',
      params: { page: 2, size: 20, userId: 10 }
    })
  })
})
