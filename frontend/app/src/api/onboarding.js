import request from '@/utils/request'
import { API_CACHE_TTL, cachedGet, clearApiCacheByPrefix } from '@/utils/apiCache'

/**
 * 获取当前用户的新手引导状态
 * @returns {Promise}
 */
export function getOnboardingStatus() {
  return cachedGet('onboarding:status', API_CACHE_TTL.ONBOARDING_STATUS, () =>
    request({
      url: '/api/user/onboarding/status',
      method: 'get'
    })
  )
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
  }).then((response) => {
    clearApiCacheByPrefix('onboarding')
    return response
  })
}

/**
 * 获取新手任务列表和完成进度
 * @returns {Promise}
 */
export function getOnboardingTasks() {
  return request({
    url: '/api/user/onboarding/tasks',
    method: 'get'
  })
}

/**
 * 上报新手任务完成（幂等）
 * @param {string} taskKey - 任务标识
 * @returns {Promise}
 */
export function completeOnboardingTask(taskKey) {
  return request({
    url: '/api/user/onboarding/tasks/complete',
    method: 'post',
    data: { taskKey }
  }).then((response) => {
    clearApiCacheByPrefix('onboarding')
    return response
  })
}
