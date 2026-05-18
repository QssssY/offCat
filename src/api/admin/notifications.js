import adminRequest from '@/utils/adminRequest'

export function getAdminNotifications(params = {}) {
  return adminRequest({ url: '/api/admin/notifications', method: 'get', params: { page: 1, size: 20, ...params } })
}

export function getAdminNotificationDetail(id) {
  return adminRequest({ url: `/api/admin/notifications/${id}`, method: 'get' })
}

export function createAdminNotification(data) {
  return adminRequest({ url: '/api/admin/notifications', method: 'post', data })
}

export function publishAdminNotification(id) {
  return adminRequest({ url: `/api/admin/notifications/${id}/publish`, method: 'put' })
}

export function publishAdminNotificationsBatch(ids) {
  return adminRequest({ url: '/api/admin/notifications/batch/publish', method: 'put', data: ids })
}

export function deleteAdminNotification(id) {
  return adminRequest({ url: `/api/admin/notifications/${id}`, method: 'delete' })
}

export function deleteAdminNotificationsBatch(ids) {
  return adminRequest({ url: '/api/admin/notifications/batch-delete', method: 'post', data: ids })
}
