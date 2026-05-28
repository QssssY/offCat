import request from '@/utils/request'

/** 使用模板前检查并扣减配额。 */
export function checkTemplateQuota() {
  return request({ url: '/api/template/use', method: 'post' })
}
