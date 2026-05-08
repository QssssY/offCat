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
