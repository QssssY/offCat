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
      fullPath: '/dashboard'
    }
  },
  push: vi.fn(() => Promise.resolve())
}))

const authMock = vi.hoisted(() => ({
  getToken: vi.fn(() => 'user-token'),
  getTokenType: vi.fn(() => 'Bearer'),
  isLoggedIn: vi.fn(() => true),
  removeToken: vi.fn()
}))

const messageMock = vi.hoisted(() => ({
  error: vi.fn()
}))

vi.mock('axios', () => ({
  default: {
    create: axiosMock.create
  }
}))

vi.mock('@/router', () => ({
  default: routerMock
}))

vi.mock('@/utils/auth', () => authMock)
vi.mock('@/utils/errorMessages', () => ({
  getErrorMessage: vi.fn(() => null)
}))
vi.mock('element-plus', () => ({
  ElMessage: messageMock
}))

describe('request', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.clearAllMocks()
    axiosMock.handlers.requestFulfilled = null
    axiosMock.handlers.requestRejected = null
    axiosMock.handlers.responseFulfilled = null
    axiosMock.handlers.responseRejected = null
    authMock.isLoggedIn.mockReturnValue(true)
    authMock.getToken.mockReturnValue('user-token')
    await import('@/utils/request')
  })

  it('shows only one expired-login prompt for concurrent http 401 responses', async () => {
    const unauthorizedError = {
      response: {
        status: 401,
        data: {
          message: '未授权'
        }
      }
    }

    await Promise.allSettled([
      axiosMock.handlers.responseRejected(unauthorizedError),
      axiosMock.handlers.responseRejected(unauthorizedError),
      axiosMock.handlers.responseRejected(unauthorizedError)
    ])

    expect(messageMock.error).toHaveBeenCalledTimes(1)
    expect(messageMock.error).toHaveBeenCalledWith('登录已过期，请重新登录')
    expect(authMock.removeToken).toHaveBeenCalledTimes(1)
    expect(routerMock.push).toHaveBeenCalledTimes(1)
    expect(routerMock.push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/dashboard' }
    })
  })

  it('keeps the 401 guard active after removeToken clears the stored token', async () => {
    let token = 'user-token'
    authMock.getToken.mockImplementation(() => token)
    authMock.removeToken.mockImplementation(() => {
      token = undefined
    })

    await Promise.allSettled([
      axiosMock.handlers.responseRejected({
        response: {
          status: 401,
          data: { message: 'Unauthorized' }
        }
      }),
      axiosMock.handlers.responseRejected({
        response: {
          status: 401,
          data: { message: 'Unauthorized' }
        }
      })
    ])

    expect(messageMock.error).toHaveBeenCalledTimes(1)
    expect(authMock.removeToken).toHaveBeenCalledTimes(1)
    expect(routerMock.push).toHaveBeenCalledTimes(1)
  })

  it('uses the same expired-login guard for business code 401 responses', async () => {
    await Promise.allSettled([
      axiosMock.handlers.responseFulfilled({
        data: {
          code: 401,
          message: '未授权'
        }
      }),
      axiosMock.handlers.responseFulfilled({
        data: {
          code: 401,
          message: '未授权'
        }
      })
    ])

    expect(messageMock.error).toHaveBeenCalledTimes(1)
    expect(authMock.removeToken).toHaveBeenCalledTimes(1)
    expect(routerMock.push).toHaveBeenCalledTimes(1)
  })

  it('preserves custom AI business code when page handles the error itself', async () => {
    const result = axiosMock.handlers.responseFulfilled({
      config: {
        skipDefaultErrorHandler: true
      },
      data: {
        code: 4090,
        message: '用户自定义 AI 调用失败'
      }
    })

    await expect(result).rejects.toMatchObject({
      code: 4090,
      message: '用户自定义 AI 调用失败'
    })
  })
})
