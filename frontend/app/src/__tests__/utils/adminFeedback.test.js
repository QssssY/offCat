import { beforeEach, describe, expect, it, vi } from 'vitest'

const elementPlusMock = vi.hoisted(() => ({
  error: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
  confirm: vi.fn()
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: elementPlusMock.error,
    success: elementPlusMock.success,
    warning: elementPlusMock.warning
  },
  ElMessageBox: {
    confirm: elementPlusMock.confirm
  }
}))

describe('adminFeedback', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('deduplicates the same admin error message within a short window', async () => {
    const { showAdminError } = await import('@/utils/adminFeedback')

    showAdminError('用户名或密码错误')
    showAdminError('用户名或密码错误')

    expect(elementPlusMock.error).toHaveBeenCalledTimes(1)
    expect(elementPlusMock.error).toHaveBeenCalledWith('用户名或密码错误')
  })
})
