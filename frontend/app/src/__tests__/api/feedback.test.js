import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import request from '@/utils/request'
import { createUserFeedback } from '@/api/feedback'

describe('user feedback API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('createUserFeedback sends feedback payload', async () => {
    const data = { type: 'bug', title: '问题标题', content: '这里是一段反馈内容', contact: 'user@example.com' }
    await createUserFeedback(data)
    expect(request).toHaveBeenCalledWith({
      url: '/api/user/feedback',
      method: 'post',
      data
    })
  })
})
