import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import request from '@/utils/request'
import { clearApiCache } from '@/utils/apiCache'
import {
  createComment,
  getComments,
  getPostDetail,
} from '@/api/community'

describe('community API cache', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearApiCache()
  })

  it('reuses the same post detail request during the short cache window', async () => {
    await getPostDetail(1)
    await getPostDetail(1)

    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith({
      url: '/api/community/posts/1',
      method: 'get'
    })
  })

  it('keeps post detail cache entries isolated by post id', async () => {
    await getPostDetail(1)
    await getPostDetail(2)
    await getPostDetail(1)

    expect(request).toHaveBeenCalledTimes(2)
    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/api/community/posts/1',
      method: 'get'
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/api/community/posts/2',
      method: 'get'
    })
  })

  it('reuses the same comment page request during the short cache window', async () => {
    const params = { pageNum: 1, pageSize: 20 }

    await getComments(1, params)
    await getComments(1, params)

    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith({
      url: '/api/community/posts/1/comments',
      method: 'get',
      params
    })
  })

  it('clears comment cache after creating a comment', async () => {
    const params = { pageNum: 1, pageSize: 20 }

    await getComments(1, params)
    await getComments(1, params)
    await createComment(1, { content: 'hello' })
    await getComments(1, params)

    expect(request).toHaveBeenCalledTimes(3)
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/api/community/posts/1/comments',
      method: 'post',
      data: { content: 'hello' }
    })
    expect(request).toHaveBeenNthCalledWith(3, {
      url: '/api/community/posts/1/comments',
      method: 'get',
      params
    })
  })
})
