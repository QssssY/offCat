import request from '@/utils/request'

/**
 * 用户登录
 * @param {Object} data - 登录参数
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise}
 */
export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {Object} data - 注册参数
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise}
 */
export function register(data) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

/**
 * 获取图形验证码
 * @returns {Promise}
 */
export function getCaptcha() {
  return request({
    url: '/api/auth/captcha',
    method: 'get'
  })
}

/**
 * 获取当前登录用户信息
 * @returns {Promise}
 */
export function getCurrentUser() {
  return request({
    url: '/api/auth/me',
    method: 'get'
  })
}

/**
 * 更新用户昵称
 * @param {Object} data - 更新参数
 * @param {string} data.nickname - 新昵称
 * @returns {Promise}
 */
export function updateNickname(data) {
  return request({
    url: '/api/auth/nickname',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 * @param {Object} data - 修改参数
 * @param {string} data.oldPassword - 原密码
 * @param {string} data.newPassword - 新密码
 * @returns {Promise}
 */
export function updatePassword(data) {
  return request({
    url: '/api/auth/password',
    method: 'put',
    data
  })
}

/**
 * 获取用户的安全问题（忘记密码流程）
 * @param {string} username - 用户名
 * @returns {Promise}
 */
export function getSecurityQuestion(username) {
  return request({
    url: '/api/auth/security-question',
    method: 'get',
    params: { username }
  })
}

/**
 * 通过安全问题验证重置密码
 * @param {Object} data - 重置参数
 * @param {string} data.username - 用户名
 * @param {string} data.securityAnswer - 安全问题答案
 * @param {string} data.newPassword - 新密码
 * @returns {Promise}
 */
export function resetPasswordBySecurity(data) {
  return request({
    url: '/api/auth/reset-password',
    method: 'post',
    data
  })
}

/**
 * 修改安全问题和答案（需登录）
 * @param {Object} data - 修改参数
 * @param {string} data.oldPassword - 原密码
 * @param {string} data.securityQuestion - 安全问题
 * @param {string} data.securityAnswer - 安全答案
 * @returns {Promise}
 */
export function updateSecurityQuestion(data) {
  return request({
    url: '/api/auth/security-question',
    method: 'put',
    data
  })
}

/**
 * 获取当前登录账号的安全问题（账号注销验证使用）
 * @returns {Promise}
 */
export function getCurrentAccountSecurityQuestion() {
  return request({
    url: '/api/user/account/security-question',
    method: 'get'
  })
}

/**
 * 注销当前账号
 * @param {Object} data - 注销确认参数
 * @param {string} data.oldPassword - 当前登录密码
 * @param {string} data.confirmPassword - 再次输入的当前登录密码
 * @param {string} data.securityAnswer - 安全问题答案
 * @returns {Promise}
 */
export function deleteAccount(data) {
  return request({
    url: '/api/user/account/delete',
    method: 'post',
    data
  })
}
