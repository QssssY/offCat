import adminRequest from '@/utils/adminRequest'

export function getAdminVersionLogs(params = {}) {
  return adminRequest({ url: '/api/admin/version-logs', method: 'get', params: { page: 1, size: 20, ...params } })
}

export function createAdminVersionLog(data) {
  return adminRequest({ url: '/api/admin/version-logs', method: 'post', data })
}

export function updateAdminVersionLog(data) {
  return adminRequest({ url: '/api/admin/version-logs', method: 'put', data })
}

export function publishAdminVersionLog(id) {
  return adminRequest({ url: `/api/admin/version-logs/${id}/publish`, method: 'put' })
}

export function publishAdminVersionLogsBatch(ids) {
  return adminRequest({ url: '/api/admin/version-logs/batch/publish', method: 'put', data: ids })
}

export function deleteAdminVersionLog(id) {
  return adminRequest({ url: `/api/admin/version-logs/${id}`, method: 'delete' })
}

export function deleteAdminVersionLogsBatch(ids) {
  return adminRequest({ url: '/api/admin/version-logs/batch-delete', method: 'post', data: ids })
}
