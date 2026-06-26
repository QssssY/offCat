import request from '@/utils/request'

export function createUserFeedback(data) {
  return request({
    url: '/api/user/feedback',
    method: 'post',
    data
  })
}
