import request from '@/utils/request'

/**
 * 薪资谈判模拟。
 * @param {Object} data - 谈判场景
 * @returns {Promise}
 */
export function simulateSalaryNegotiation(data) {
  return request({
    url: '/api/offer/salary-negotiation/simulate',
    method: 'post',
    data,
    timeout: 180000
  })
}

/**
 * 生成谈薪话术模板。
 * @param {Object} data - 谈薪目标
 * @returns {Promise}
 */
export function generateSalaryScript(data) {
  return request({
    url: '/api/offer/salary-negotiation/script',
    method: 'post',
    data,
    timeout: 180000
  })
}
