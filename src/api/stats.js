import request from '@/utils/request'

/**
 * 获取平台公开统计数据（无需登录）
 * 返回用户总数、简历诊断完成数、模拟面试完成数
 * @returns {Promise}
 */
export function getPublicStats() {
  return request({
    url: '/api/stats',
    method: 'get'
  })
}

/**
 * 获取当前用户月度统计（需要登录）
 * 返回本月简历诊断完成数、本月模拟面试完成数
 * @returns {Promise}
 */
export function getMonthlyStats() {
  return request({
    url: '/api/user/stats/monthly',
    method: 'get'
  })
}
