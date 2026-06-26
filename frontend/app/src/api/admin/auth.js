import adminRequest from '@/utils/adminRequest'

/**
 * 管理端登录接口。
 * 说明：管理端与用户端使用同一认证接口，但后续会强校验管理员角色。
 * @param {{username: string, password: string}} data
 * @returns {Promise}
 */
export function adminLogin(data) {
  return adminRequest({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

/**
 * 获取当前登录用户信息。
 * 说明：管理端登录后需要调用该接口判断 role 是否为管理员。
 * @returns {Promise}
 */
export function getAdminCurrentUser() {
  return adminRequest({
    url: '/api/auth/me',
    method: 'get'
  })
}
