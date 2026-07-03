import adminRequest from '@/utils/adminRequest'

/**
 * 获取管理端监控总览。
 * @returns {Promise}
 */
export function getAdminMonitorOverview() {
  return adminRequest({
    url: '/api/admin/monitor/overview',
    method: 'get'
  })
}
