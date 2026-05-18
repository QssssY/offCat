import request from '@/utils/request'

/**
 * 获取当前用户的新手引导状态
 * @returns {Promise}
 */
export function getOnboardingStatus() {
  return request({
    url: '/api/user/onboarding/status',
    method: 'get'
  })
}

/**
 * 更新当前用户的新手引导状态
 * @param {Object} data - 更新参数
 * @param {string} data.guideKey - 引导版本标识
 * @param {string} data.status - 目标状态：in_progress / completed / skipped
 * @param {number} [data.currentStep] - 当前步骤索引，status=in_progress 时必填
 * @returns {Promise}
 */
export function updateOnboardingStatus(data) {
  return request({
    url: '/api/user/onboarding/status',
    method: 'put',
    data
  })
}
