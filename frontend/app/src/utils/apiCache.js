const cacheStore = new Map()

function now() {
  return Date.now()
}

function isFresh(entry) {
  return entry && entry.expiresAt > now()
}

/**
 * 前端只缓存明确声明的稳定 GET 数据。
 * 写操作、轮询、SSE 和报告生成状态不走这里，避免把实时状态缓存成旧数据。
 */
export function cachedGet(key, ttlMs, loader) {
  const existing = cacheStore.get(key)
  if (existing?.pending) {
    return existing.pending
  }
  if (isFresh(existing)) {
    return Promise.resolve(existing.value)
  }

  const pending = Promise.resolve()
    .then(loader)
    .then((value) => {
      cacheStore.set(key, {
        value,
        expiresAt: now() + ttlMs
      })
      return value
    })
    .catch((error) => {
      cacheStore.delete(key)
      throw error
    })

  cacheStore.set(key, {
    pending,
    expiresAt: now() + ttlMs
  })

  return pending
}

export function clearApiCache(key) {
  if (key) {
    cacheStore.delete(key)
    return
  }
  cacheStore.clear()
}

export function clearApiCacheByPrefix(prefix) {
  for (const key of cacheStore.keys()) {
    if (key === prefix || key.startsWith(`${prefix}:`)) {
      cacheStore.delete(key)
    }
  }
}

export function buildCacheKey(prefix, params = {}) {
  const normalizedParams = Object.keys(params)
    .sort()
    .map((key) => `${key}=${String(params[key] ?? '')}`)
    .join('&')
  return normalizedParams ? `${prefix}:${normalizedParams}` : prefix
}

export const API_CACHE_TTL = {
  PUBLIC_STATS: 5 * 60 * 1000,
  MEMBERSHIP_PLANS: 30 * 60 * 1000,
  JOB_ROLES: 30 * 60 * 1000,
  GROWTH_OVERVIEW: 2 * 60 * 1000,
  NOTIFICATION_UNREAD: 30 * 1000,
  NOTIFICATION_LIST: 15 * 1000,
  COMMUNITY_LIST: 30 * 1000,
  VERSION_LOGS: 60 * 1000,
  ONBOARDING_STATUS: 60 * 1000,
  COMMUNITY_DETAIL: 15 * 1000,
  COMMUNITY_COMMENTS: 15 * 1000
}
