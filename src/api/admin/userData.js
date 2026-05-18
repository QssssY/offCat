import adminRequest from '@/utils/adminRequest'

export function getAdminUserInterviews(userId, params = {}) {
  return adminRequest({
    url: `/api/admin/users/${userId}/interviews`,
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}

export function getAdminUserResumeTasks(userId, params = {}) {
  return adminRequest({
    url: `/api/admin/users/${userId}/resume-tasks`,
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}
