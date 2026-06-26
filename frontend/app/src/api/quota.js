import request from '@/utils/request'

/**
 * 查询当前用户额度消费记录（分页 + 类型筛选）
 * @param {Object} params - 查询参数
 * @param {string} [params.quotaType] - 额度类型筛选（不传=全部）
 * @param {number} [params.pageNum=1] - 页码
 * @param {number} [params.pageSize=20] - 每页条数
 * @returns {Promise}
 */
export function getConsumptionLog(params) {
  return request({
    url: '/api/user/quota/consumption-log',
    method: 'get',
    params
  })
}
