import request from '@/utils/request'

export function getUserSettings() {
  return request({
    url: '/api/user/settings',
    method: 'get'
  })
}

export function saveUserSettings(data) {
  return request({
    url: '/api/user/settings',
    method: 'put',
    data
  })
}
