import request from '@/utils/request'

export function getLatestVersionLogs(limit = 5) {
  return request({
    url: '/api/version-logs/latest',
    method: 'get',
    params: { limit }
  })
}
