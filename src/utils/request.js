import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, getTokenType, removeToken, isLoggedIn } from '@/utils/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 30000
})

// 请求拦截器：自动注入登录态 token
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

// 响应拦截器：统一处理业务码与网络异常
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }

    // 某些长耗时接口会在页面内自行处理异常，此处允许跳过默认弹窗
    if (response.config?.skipDefaultErrorHandler) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // 允许单个请求自行兜底处理超时、重试和状态回查
    if (error.config?.skipDefaultErrorHandler) {
      return Promise.reject(error)
    }

    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401: {
          removeToken()
          ElMessage.error('登录已过期，请重新登录')
          const currentPath = router.currentRoute.value.fullPath
          router.push({
            path: '/login',
            query: { redirect: currentPath }
          })
          break
        }
        case 403:
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('请求失败')
    }

    return Promise.reject(error)
  }
)

export default request
