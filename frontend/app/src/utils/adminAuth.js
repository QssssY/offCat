// 管理端 Token 与角色信息本地存储键名
const ADMIN_TOKEN_KEY = 'ai_resume_admin_token'
const ADMIN_TOKEN_TYPE_KEY = 'ai_resume_admin_token_type'
const ADMIN_ROLE_KEY = 'ai_resume_admin_role'

/**
 * 获取管理端 Token。
 * @returns {string|null}
 */
export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY)
}

/**
 * 获取管理端 Token 类型。
 * @returns {string}
 */
export function getAdminTokenType() {
  return localStorage.getItem(ADMIN_TOKEN_TYPE_KEY) || 'Bearer'
}

/**
 * 获取管理端角色编码。
 * @returns {number|null}
 */
export function getAdminRole() {
  const roleValue = localStorage.getItem(ADMIN_ROLE_KEY)
  if (roleValue === null || roleValue === '') {
    return null
  }
  const parsed = Number(roleValue)
  return Number.isNaN(parsed) ? null : parsed
}

/**
 * 保存管理端登录会话。
 * @param {string} token
 * @param {string} tokenType
 * @param {number} role
 */
export function setAdminSession(token, tokenType = 'Bearer', role = 9) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
  localStorage.setItem(ADMIN_TOKEN_TYPE_KEY, tokenType)
  localStorage.setItem(ADMIN_ROLE_KEY, String(role))
}

/**
 * 更新管理端角色信息。
 * @param {number} role
 */
export function setAdminRole(role) {
  localStorage.setItem(ADMIN_ROLE_KEY, String(role))
}

/**
 * 清除管理端登录会话。
 */
export function clearAdminSession() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_TOKEN_TYPE_KEY)
  localStorage.removeItem(ADMIN_ROLE_KEY)
}

/**
 * 判断管理端是否已登录（仅基于管理端 token）。
 * @returns {boolean}
 */
export function isAdminLoggedIn() {
  return !!getAdminToken()
}

/**
 * 判断当前管理端会话是否具备管理员角色。
 * 角色约定：9 表示管理员。
 * @returns {boolean}
 */
export function hasAdminRole() {
  return getAdminRole() === 9
}
