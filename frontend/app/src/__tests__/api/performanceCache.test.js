import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const requestMock = vi.hoisted(() => vi.fn())

vi.mock('@/utils/request', () => ({
  default: requestMock
}))

describe('performance cache API integration', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-29T10:00:00Z'))
    requestMock.mockResolvedValue({ data: [] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('caches public stats for five minutes', async () => {
    const { getPublicStats } = await import('@/api/stats')

    await getPublicStats()
    await getPublicStats()

    expect(requestMock).toHaveBeenCalledTimes(1)
    expect(requestMock).toHaveBeenCalledWith({ url: '/api/stats', method: 'get' })
  })

  it('caches membership plans and clears them after mock upgrade', async () => {
    const { getMembershipPlans, mockUpgradeMembership } = await import('@/api/membership')

    await getMembershipPlans()
    await getMembershipPlans()
    await mockUpgradeMembership({ planCode: 'vip_month' })
    await getMembershipPlans()

    expect(requestMock).toHaveBeenCalledTimes(3)
    expect(requestMock).toHaveBeenNthCalledWith(1, { url: '/api/membership/plans', method: 'get' })
    expect(requestMock).toHaveBeenNthCalledWith(2, {
      url: '/api/membership/upgrade/mock',
      method: 'post',
      data: { planCode: 'vip_month' }
    })
    expect(requestMock).toHaveBeenNthCalledWith(3, { url: '/api/membership/plans', method: 'get' })
  })

  it('caches interview job roles for thirty minutes', async () => {
    const { getInterviewJobRoles } = await import('@/api/interview')

    await getInterviewJobRoles()
    await getInterviewJobRoles()

    expect(requestMock).toHaveBeenCalledTimes(1)
    expect(requestMock).toHaveBeenCalledWith({ url: '/api/interview/job-roles', method: 'get' })
  })

  it('caches growth overview for two minutes but keeps radar uncached', async () => {
    const { getGrowthOverview, getInterviewRadar } = await import('@/api/growth')

    await getGrowthOverview()
    await getGrowthOverview()
    await getInterviewRadar()
    await getInterviewRadar()

    expect(requestMock).toHaveBeenCalledTimes(3)
    expect(requestMock).toHaveBeenNthCalledWith(1, { url: '/api/user/growth/overview', method: 'get' })
    expect(requestMock).toHaveBeenNthCalledWith(2, { url: '/api/user/growth/interview-radar', method: 'get' })
    expect(requestMock).toHaveBeenNthCalledWith(3, { url: '/api/user/growth/interview-radar', method: 'get' })
  })

  it('caches unread count briefly and invalidates it after notification writes', async () => {
    const { getUnreadCount, markAllAsRead } = await import('@/api/notification')

    await getUnreadCount()
    await getUnreadCount()
    await markAllAsRead()
    await getUnreadCount()

    expect(requestMock).toHaveBeenCalledTimes(3)
    expect(requestMock).toHaveBeenNthCalledWith(1, { url: '/api/user/notifications/unread-count', method: 'get' })
    expect(requestMock).toHaveBeenNthCalledWith(2, { url: '/api/user/notifications/read-all', method: 'post' })
    expect(requestMock).toHaveBeenNthCalledWith(3, { url: '/api/user/notifications/unread-count', method: 'get' })
  })

  it('caches notification list briefly and invalidates it after notification writes', async () => {
    const { getNotifications, markAllAsRead } = await import('@/api/notification')
    const params = { pageNum: 1, size: 10 }

    await getNotifications(params)
    await getNotifications(params)
    await markAllAsRead()
    await getNotifications(params)

    expect(requestMock).toHaveBeenCalledTimes(3)
    expect(requestMock).toHaveBeenNthCalledWith(1, {
      url: '/api/user/notifications',
      method: 'get',
      params
    })
    expect(requestMock).toHaveBeenNthCalledWith(2, { url: '/api/user/notifications/read-all', method: 'post' })
    expect(requestMock).toHaveBeenNthCalledWith(3, {
      url: '/api/user/notifications',
      method: 'get',
      params
    })
  })

  it('invalidates community list caches after post interactions without caching writes', async () => {
    const { getPostList, createPost, togglePostLike } = await import('@/api/community')
    const params = { pageNum: 1, pageSize: 8, sort: 'latest' }

    await getPostList(params)
    await getPostList(params)
    await togglePostLike(100)
    await getPostList(params)
    await createPost({ title: 't', category: 'interview_exp', content: 'c' })

    expect(requestMock).toHaveBeenCalledTimes(4)
    expect(requestMock).toHaveBeenNthCalledWith(1, { url: '/api/community/posts', method: 'get', params })
    expect(requestMock).toHaveBeenNthCalledWith(2, { url: '/api/community/posts/100/like', method: 'post' })
    expect(requestMock).toHaveBeenNthCalledWith(3, { url: '/api/community/posts', method: 'get', params })
    expect(requestMock).toHaveBeenNthCalledWith(4, {
      url: '/api/community/posts',
      method: 'post',
      data: { title: 't', category: 'interview_exp', content: 'c' }
    })
  })
})
