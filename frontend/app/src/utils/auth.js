// Token 存储键名
const TOKEN_KEY = 'ai_resume_token'
const TOKEN_TYPE_KEY = 'ai_resume_token_type'

/**
 * 获取 Token
 * @returns {string|null}
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置 Token
 * @param {string} token - Token 字符串
 * @param {string} tokenType - Token 类型，默认 Bearer
 */
export function setToken(token, tokenType = 'Bearer') {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(TOKEN_TYPE_KEY, tokenType)
}

/**
 * 清除 Token
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_TYPE_KEY)
}

/**
 * 获取 Token 类型
 * @returns {string}
 */
export function getTokenType() {
  return localStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'
}

/**
 * 判断是否已登录
 * @returns {boolean}
 */
export function isLoggedIn() {
  return !!getToken()
}
