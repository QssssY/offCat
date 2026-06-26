import request from '@/utils/request'
import { API_CACHE_TTL, cachedGet } from '@/utils/apiCache'

/**
 * 获取个人成长中心概览数据
 * 聚合简历诊断、JD匹配、AI润色、模拟面试等维度的成长数据
 * @returns {Promise}
 */
export function getGrowthOverview() {
  return cachedGet('user:growthOverview', API_CACHE_TTL.GROWTH_OVERVIEW, () =>
    request({
      url: '/api/user/growth/overview',
      method: 'get'
    })
  )
}

/**
 * 获取面试维度雷达数据
 * 包含最新雷达评分、各维度趋势和盲区提示
 * @returns {Promise}
 */
export function getInterviewRadar() {
  return request({
    url: '/api/user/growth/interview-radar',
    method: 'get'
  })
}
