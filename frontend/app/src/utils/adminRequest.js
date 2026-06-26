import axios from 'axios'
import router from '@/router'
import {
  clearAdminSession,
  getAdminToken,
  getAdminTokenType,
  isAdminLoggedIn
} from '@/utils/adminAuth'
import {
  ADMIN_FEEDBACK_TEXT,
  resolveAdminStatusErrorMessage,
  showAdminError
} from '@/utils/adminFeedback'

/**
 * 解析 JSON 字符串并保留超长整型精度。
 * 为什么这样做：
 * 1. Java Long 超过 JS 安全整数范围时，默认 JSON 解析会出现精度丢失。
 * 2. 管理端用户权益接口路径参数依赖 userId，精度丢失会导致“用户不存在”。
 * 3. 这里将 16 位及以上整数先转成字符串再 JSON.parse，确保 userId 在前端链路中可保真传递。
 * @param {string | any} rawData
 * @returns {any}
 */
const parseJsonPreserveLongInteger = (rawData) => {
  if (typeof rawData !== 'string') return rawData
  const trimmed = rawData.trim()
  if (!trimmed) return rawData

  try {
    const quotedLongInteger = trimmed
      // 处理对象属性值中的超长整型
      .replace(/(:\s*)(-?\d{16,})(\s*[,}\]])/g, '$1"$2"$3')
      // 处理数组元素中的超长整型
      .replace(/([\[,]\s*)(-?\d{16,})(\s*[,}\]])/g, '$1"$2"$3')
    return JSON.parse(quotedLongInteger)
  } catch {
    // 回退到默认 JSON 解析，避免非 JSON 响应被误伤。
    try {
      return JSON.parse(trimmed)
    } catch {
      return rawData
    }
  }
}

const adminRequest = axios.create({
  baseURL: '',
  timeout: 30000,
  transformResponse: [(data) => parseJsonPreserveLongInteger(data)]
})

const createAdminRequestError = (message, cause) => {
  const error = new Error(message || ADMIN_FEEDBACK_TEXT.requestFailed)
  error.isAdminRequestError = true
  if (cause) {
    error.cause = cause
  }
  return error
}

// 统一 401 处理锁：避免并发请求同时过期时重复弹错并重复跳转。
let isHandlingUnauthorized = false

// 管理端请求拦截：仅注入管理端 token，避免与用户端 token 混用。
adminRequest.interceptors.request.use(
  (config) => {
    if (isAdminLoggedIn()) {
      config.headers.Authorization = `${getAdminTokenType()} ${getAdminToken()}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 管理端响应拦截：统一以 body.code === 200 作为成功标准。
adminRequest.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.code === 200) {
      return body
    }

    // 兼容后端通过业务码返回未授权场景，统一走会话失效处理。
    if (body?.code === 401) {
      if (!isHandlingUnauthorized) {
        isHandlingUnauthorized = true
        clearAdminSession()
        showAdminError(ADMIN_FEEDBACK_TEXT.sessionExpired)
        const currentPath = router.currentRoute.value.fullPath
        router.push({
          path: '/admin/login',
          query: { redirect: currentPath }
        }).finally(() => {
          isHandlingUnauthorized = false
        })
      }
      return Promise.reject(createAdminRequestError(ADMIN_FEEDBACK_TEXT.sessionExpired))
    }

    return Promise.reject(createAdminRequestError(body?.message || ADMIN_FEEDBACK_TEXT.requestFailed))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.message

      if (status === 401) {
        if (!isHandlingUnauthorized) {
          isHandlingUnauthorized = true
          clearAdminSession()
          showAdminError(ADMIN_FEEDBACK_TEXT.sessionExpired)
          const currentPath = router.currentRoute.value.fullPath
          router.push({
            path: '/admin/login',
            query: { redirect: currentPath }
          }).finally(() => {
            isHandlingUnauthorized = false
          })
        }
        return Promise.reject(createAdminRequestError(ADMIN_FEEDBACK_TEXT.sessionExpired, error))
      } else {
        return Promise.reject(createAdminRequestError(resolveAdminStatusErrorMessage(status, message), error))
      }
    } else if (error.request) {
      return Promise.reject(createAdminRequestError(ADMIN_FEEDBACK_TEXT.networkError, error))
    }
    return Promise.reject(createAdminRequestError(error.message || ADMIN_FEEDBACK_TEXT.requestFailed, error))
  }
)

export default adminRequest
