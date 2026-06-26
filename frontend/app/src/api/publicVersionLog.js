import request from '@/utils/request'
import { API_CACHE_TTL, buildCacheKey, cachedGet } from '@/utils/apiCache'

/**
 * 分页获取公开版本日志列表。
 * 首页继续使用 latest 接口只取 3 条，更多动态页使用该接口避免永远只显示首页摘要数量。
 * @param {{ page?: number, size?: number }} params
 * @returns {Promise}
 */
export function getPublicVersionLogsPage(params = {}) {
  const normalizedParams = { page: 1, size: 10, ...params }
  return cachedGet(buildCacheKey('version:page', normalizedParams), API_CACHE_TTL.VERSION_LOGS, () =>
    request({
      url: '/api/version-logs',
      method: 'get',
      params: normalizedParams
    })
  )
}
