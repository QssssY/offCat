import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import request from '@/utils/request'
import { clearApiCache } from '@/utils/apiCache'
import {
  completeOnboardingTask,
  getOnboardingStatus,
  updateOnboardingStatus,
} from '@/api/onboarding'

describe('onboarding API cache', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearApiCache()
  })

  it('reuses onboarding status reads during the short cache window', async () => {
    await getOnboardingStatus()
    await getOnboardingStatus()

    expect(request).toHaveBeenCalledTimes(1)
    expect(request).toHaveBeenCalledWith({
      url: '/api/user/onboarding/status',
      method: 'get'
    })
  })

  it('clears onboarding status cache after status update succeeds', async () => {
    await getOnboardingStatus()
    await getOnboardingStatus()
    await updateOnboardingStatus({ guideKey: 'main', status: 'completed' })
    await getOnboardingStatus()

    expect(request).toHaveBeenCalledTimes(3)
    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/api/user/onboarding/status',
      method: 'get'
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/api/user/onboarding/status',
      method: 'put',
      data: { guideKey: 'main', status: 'completed' }
    })
    expect(request).toHaveBeenNthCalledWith(3, {
      url: '/api/user/onboarding/status',
      method: 'get'
    })
  })

  it('clears onboarding status cache after completing a task', async () => {
    await getOnboardingStatus()
    await getOnboardingStatus()
    await completeOnboardingTask('interview_completed')
    await getOnboardingStatus()

    expect(request).toHaveBeenCalledTimes(3)
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/api/user/onboarding/tasks/complete',
      method: 'post',
      data: { taskKey: 'interview_completed' }
    })
  })
})
