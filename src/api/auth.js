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
