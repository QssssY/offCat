import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('apiCache', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-29T10:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.resetModules()
  })

  it('deduplicates concurrent cacheable requests and reuses values before ttl expires', async () => {
    const { cachedGet, clearApiCache } = await import('@/utils/apiCache')
    clearApiCache()
    const loader = vi.fn(() => Promise.resolve({ data: { value: 1 } }))

    const first = cachedGet('public:stats', 300000, loader)
    const second = cachedGet('public:stats', 300000, loader)

    await expect(first).resolves.toEqual({ data: { value: 1 } })
    await expect(second).resolves.toEqual({ data: { value: 1 } })
    expect(loader).toHaveBeenCalledTimes(1)

    await expect(cachedGet('public:stats', 300000, loader)).resolves.toEqual({ data: { value: 1 } })
    expect(loader).toHaveBeenCalledTimes(1)
  })

  it('defines short TTLs for repeat-access noisy GET endpoints', async () => {
    const { API_CACHE_TTL } = await import('@/utils/apiCache')

    expect(API_CACHE_TTL.VERSION_LOGS).toBe(60 * 1000)
    expect(API_CACHE_TTL.ONBOARDING_STATUS).toBe(60 * 1000)
    expect(API_CACHE_TTL.COMMUNITY_DETAIL).toBe(15 * 1000)
    expect(API_CACHE_TTL.COMMUNITY_COMMENTS).toBe(15 * 1000)
  })

  it('clears failed pending entries so the next request can retry', async () => {
    const { cachedGet, clearApiCache } = await import('@/utils/apiCache')
    clearApiCache()
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('temporary failure'))
      .mockResolvedValueOnce({ data: { value: 2 } })

    await expect(cachedGet('version:latest:limit=3', 1000, loader)).rejects.toThrow('temporary failure')
    await expect(cachedGet('version:latest:limit=3', 1000, loader)).resolves.toEqual({ data: { value: 2 } })

    expect(loader).toHaveBeenCalledTimes(2)
  })

  it('reloads after ttl expires and supports prefix invalidation', async () => {
    const { cachedGet, clearApiCacheByPrefix } = await import('@/utils/apiCache')
    const loader = vi
      .fn()
      .mockResolvedValueOnce({ data: { value: 1 } })
      .mockResolvedValueOnce({ data: { value: 2 } })
      .mockResolvedValueOnce({ data: { value: 3 } })

    await cachedGet('membership:plans', 1000, loader)
    vi.advanceTimersByTime(1001)
    await expect(cachedGet('membership:plans', 1000, loader)).resolves.toEqual({ data: { value: 2 } })

    clearApiCacheByPrefix('membership')
    await expect(cachedGet('membership:plans', 1000, loader)).resolves.toEqual({ data: { value: 3 } })
    expect(loader).toHaveBeenCalledTimes(3)
  })
})
