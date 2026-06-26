import request from '@/utils/request'
import { API_CACHE_TTL, cachedGet, clearApiCacheByPrefix } from '@/utils/apiCache'

/**
 * Get the list of membership plans for the membership page.
 * The page uses this data to render every plan card.
 * @returns {Promise}
 */
export function getMembershipPlans() {
  return cachedGet('membership:plans', API_CACHE_TTL.MEMBERSHIP_PLANS, () =>
    request({
      url: '/api/membership/plans',
      method: 'get'
    })
  )
}

/**
 * Trigger the mock membership upgrade flow.
 * The backend will simulate a successful payment and upgrade immediately.
 * @param {Object} data
 * @param {string} data.planCode - Membership plan code such as vip_month
 * @returns {Promise}
 */
export function mockUpgradeMembership(data) {
  return request({
    url: '/api/membership/upgrade/mock',
    method: 'post',
    data
  }).then((response) => {
    // 升级成功后清理会员套餐与用户相关短缓存，避免页面继续展示旧权益。
    clearApiCacheByPrefix('membership')
    clearApiCacheByPrefix('user')
    return response
  })
}
