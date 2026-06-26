import request from '@/utils/request'
import { API_CACHE_TTL, buildCacheKey, cachedGet } from '@/utils/apiCache'

export function getLatestVersionLogs(limit = 5) {
  return cachedGet(buildCacheKey('version:latest', { limit }), API_CACHE_TTL.VERSION_LOGS, () =>
    request({
      url: '/api/version-logs/latest',
      method: 'get',
      params: { limit }
    })
  )
}
