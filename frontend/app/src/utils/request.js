import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, getTokenType, removeToken, isLoggedIn } from '@/utils/auth'
import { getErrorMessage } from '@/utils/errorMessages'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 30000
})

const VIP_UPGRADE_CODES = new Set([2013, 2015, 2016, 5005, 5006, 5007])

// 用户端 401 处理锁：同一个失效 token 只提示一次；用户重新登录拿到新 token 后仍可再次提示。
let handledUnauthorizedToken = undefined
let isHandlingUnauthorized = false

const handleUnauthorized = () => {
  const currentToken = getToken()
  if (isHandlingUnauthorized && (!currentToken || handledUnauthorizedToken === currentToken)) {
    return
  }

  handledUnauthorizedToken = currentToken
  isHandlingUnauthorized = true
  removeToken()
  ElMessage.error('登录已过期，请重新登录')

  const currentPath = router.currentRoute.value.fullPath
  if (currentPath === '/login') {
    return
  }

  router.push({
    path: '/login',
    query: { redirect: currentPath }
  })
}

// 请求拦截器：自动注入登录态 token。
request.interceptors.request.use(
  (config) => {
    if (isLoggedIn()) {
      const token = getToken()
      const tokenType = getTokenType()
      config.headers.Authorization = `${tokenType} ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理业务码与网络异常。
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }

    // 后端业务码 401 也表示登录态失效，必须和 HTTP 401 共用同一把锁避免重复弹窗。
    if (res.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(res.message || '登录已过期，请重新登录'))
    }

    // 某些长耗时接口会在页面内自行处理异常，此处允许跳过默认弹窗。
    if (response.config?.skipDefaultErrorHandler) {
      const error = new Error(res.message || '请求失败')
      // 自定义 AI 失败需要页面识别 4090/4091 后展示平台回退入口，不能只保留 message。
      error.code = res.code
      error.data = res.data
      return Promise.reject(error)
    }

    // 会员专属/额度不足类错误：弹出升级弹窗替代普通错误提示。
    if (VIP_UPGRADE_CODES.has(res.code)) {
      window.dispatchEvent(new CustomEvent('show-vip-upgrade'))
      return Promise.reject(new Error(res.message || '该功能为会员专属'))
    }

    // 优先使用错误码映射获取用户友好提示。
    const errorInfo = getErrorMessage(res.code, res.message)
    if (errorInfo) {
      ElMessage({
        message: errorInfo.description ? `${errorInfo.title}：${errorInfo.description}` : errorInfo.title,
        type: 'error',
        duration: 5000
      })
    } else {
      ElMessage.error(res.message || '请求失败')
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // 允许单个请求自行兜底处理超时、重试和状态回查。
    if (error.config?.skipDefaultErrorHandler) {
      return Promise.reject(error)
    }

    const friendlyMessage = '网络异常，请稍后重试'

    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401: {
          handleUnauthorized()
          break
        }
        case 403:
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器繁忙，请稍后重试')
          break
        case 502:
        case 503:
        case 504:
          ElMessage.error('服务暂时不可用，请稍后重试')
          break
        default:
          ElMessage.error(data?.message || friendlyMessage)
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络后重试')
    } else {
      ElMessage.error(friendlyMessage)
    }

    return Promise.reject(new Error(friendlyMessage))
  }
)

export default request
