import { beforeEach, describe, expect, it, vi } from 'vitest'

const axiosMock = vi.hoisted(() => {
  const handlers = {
    requestFulfilled: null,
    requestRejected: null,
    responseFulfilled: null,
    responseRejected: null
  }
  const instance = {
    interceptors: {
      request: {
        use: vi.fn((fulfilled, rejected) => {
          handlers.requestFulfilled = fulfilled
          handlers.requestRejected = rejected
        })
      },
      response: {
        use: vi.fn((fulfilled, rejected) => {
          handlers.responseFulfilled = fulfilled
          handlers.responseRejected = rejected
        })
      }
    }
  }
  return {
    create: vi.fn(() => instance),
    handlers
  }
})

const routerMock = vi.hoisted(() => ({
  currentRoute: {
    value: {
      fullPath: '/admin/dashboard'
    }
  },
  push: vi.fn(() => Promise.resolve())
}))

const adminAuthMock = vi.hoisted(() => ({
  clearAdminSession: vi.fn(),
  getAdminToken: vi.fn(() => 'admin-token'),
  getAdminTokenType: vi.fn(() => 'Bearer'),
  isAdminLoggedIn: vi.fn(() => false)
}))

const feedbackMock = vi.hoisted(() => ({
  ADMIN_FEEDBACK_TEXT: {
    sessionExpired: '管理端会话已失效，请重新登录',
    networkError: '管理端网络异常，请检查连接',
    requestFailed: '管理端请求失败，请稍后重试'
  },
  resolveAdminStatusErrorMessage: vi.fn((status, backendMessage) => backendMessage || `status-${status}`),
  showAdminError: vi.fn()
}))

vi.mock('axios', () => ({
  default: {
    create: axiosMock.create
  }
}))

vi.mock('@/router', () => ({
  default: routerMock
}))

vi.mock('@/utils/adminAuth', () => adminAuthMock)
vi.mock('@/utils/adminFeedback', () => feedbackMock)

describe('adminRequest', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.clearAllMocks()
    axiosMock.handlers.requestFulfilled = null
    axiosMock.handlers.requestRejected = null
    axiosMock.handlers.responseFulfilled = null
    axiosMock.handlers.responseRejected = null
    await import('@/utils/adminRequest')
  })

  it('does not show duplicate feedback for business errors handled by page code', async () => {
    await expect(
      axiosMock.handlers.responseFulfilled({
        data: {
          code: 400,
          message: '用户名或密码错误'
        }
      })
    ).rejects.toThrow('用户名或密码错误')

    expect(feedbackMock.showAdminError).not.toHaveBeenCalled()
  })

  it('does not show duplicate feedback for non-auth http errors handled by page code', async () => {
    await expect(
      axiosMock.handlers.responseRejected({
        response: {
          status: 500,
          data: {
            message: '保存失败'
          }
        }
      })
    ).rejects.toThrow('保存失败')

    expect(feedbackMock.showAdminError).not.toHaveBeenCalled()
  })

  it('keeps one global feedback for expired admin sessions', async () => {
    await expect(
      axiosMock.handlers.responseFulfilled({
        data: {
          code: 401
        }
      })
    ).rejects.toThrow(feedbackMock.ADMIN_FEEDBACK_TEXT.sessionExpired)

    expect(feedbackMock.showAdminError).toHaveBeenCalledTimes(1)
    expect(feedbackMock.showAdminError).toHaveBeenCalledWith(feedbackMock.ADMIN_FEEDBACK_TEXT.sessionExpired)
    expect(adminAuthMock.clearAdminSession).toHaveBeenCalled()
    expect(routerMock.push).toHaveBeenCalledWith({
      path: '/admin/login',
      query: { redirect: '/admin/dashboard' }
    })
  })
})
