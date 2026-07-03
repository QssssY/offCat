import adminRequest from '@/utils/adminRequest'

export function getAdminAuditLogs(params = {}) {
  return adminRequest({
    url: '/api/admin/audit-logs',
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}
