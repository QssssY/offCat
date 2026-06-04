import adminRequest from '@/utils/adminRequest'

/**
 * 获取管理端看板聚合数据。
 * @param {{startDate?: string, endDate?: string, limit?: number}} params
 * @returns {Promise}
 */
export function getAdminDashboardSummary(params = {}) {
  return adminRequest({
    url: '/api/admin/dashboard/summary',
    method: 'get',
    params
  })
}

/**
 * 获取管理端总览统计。
 * @param {{startDate?: string, endDate?: string}} params
 * @returns {Promise}
 */
export function getAdminDashboardOverview(params = {}) {
  return adminRequest({
    url: '/api/admin/dashboard/overview',
    method: 'get',
    params
  })
}

/**
 * 获取管理端趋势数据。
 * @param {{startDate?: string, endDate?: string}} params
 * @returns {Promise}
 */
export function getAdminDashboardTrends(params = {}) {
  return adminRequest({
    url: '/api/admin/dashboard/trends',
    method: 'get',
    params
  })
}

/**
 * 获取管理端热门岗位排行。
 * @param {{startDate?: string, endDate?: string, limit?: number}} params
 * @returns {Promise}
 */
export function getAdminDashboardHotJobRoles(params = {}) {
  return adminRequest({
    url: '/api/admin/dashboard/hot-job-roles',
    method: 'get',
    params
  })
}

/**
 * 获取管理端业务分布数据。
 * @param {{startDate?: string, endDate?: string}} params
 * @returns {Promise}
 */
export function getAdminDashboardBusinessDistribution(params = {}) {
  return adminRequest({
    url: '/api/admin/dashboard/business-distribution',
    method: 'get',
    params
  })
}
