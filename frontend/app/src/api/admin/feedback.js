import adminRequest from '@/utils/adminRequest'

export function getAdminFeedbackList(params = {}) {
  return adminRequest({
    url: '/api/admin/feedback',
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}

export function getAdminFeedbackDetail(id) {
  return adminRequest({ url: `/api/admin/feedback/${id}`, method: 'get' })
}

export function updateAdminFeedbackStatus(id, data) {
  return adminRequest({ url: `/api/admin/feedback/${id}/status`, method: 'put', data })
}

export function deleteAdminFeedbackBatch(ids) {
  return adminRequest({ url: '/api/admin/feedback/batch-delete', method: 'post', data: ids })
}
