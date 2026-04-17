import request from '@/utils/request'

/**
 * Get the list of membership plans for the membership page.
 * The page uses this data to render every plan card.
 * @returns {Promise}
 */
export function getMembershipPlans() {
  return request({
    url: '/api/membership/plans',
    method: 'get'
  })
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
  })
}
