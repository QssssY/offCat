import request from '@/utils/request'

/**
 * 分页获取公开版本日志列表。
 * 首页继续使用 latest 接口只取 3 条，更多动态页使用该接口避免永远只显示首页摘要数量。
 * @param {{ page?: number, size?: number }} params
 * @returns {Promise}
 */
export function getPublicVersionLogsPage(params = {}) {
  return request({
    url: '/api/version-logs',
    method: 'get',
    params: { page: 1, size: 10, ...params }
  })
}
