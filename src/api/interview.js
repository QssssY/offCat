import request from '@/utils/request'

/**
 * 创建面试会话
 * @param {Object} data - 创建参数
 * @param {string} data.jobRole - 面试岗位
 * @param {number} data.difficulty - 难度级别：1-初级，2-中级，3-高级
 * @returns {Promise}
 */
export function createInterviewSession(data) {
  return request({
    url: '/api/interview/session',
    method: 'post',
    data
  })
}

/**
 * 查询当前启用的面试岗位选项
 *
 * 作用：
 * 面试岗位现在必须由管理员在后台配置，前端不能再写死岗位列表。
 * 用户端页面统一通过这个接口读取最新岗位数据。
 *
 * @returns {Promise}
 */
export function getInterviewJobRoles() {
  return request({
    url: '/api/interview/job-roles',
    method: 'get'
  })
}

/**
 * 发送面试消息
 * @param {string} sessionId - 会话ID
 * @param {Object} data - 消息参数
 * @param {string} data.content - 消息内容
 * @returns {Promise}
 */
export function sendInterviewMessage(sessionId, data) {
  return request({
    url: `/api/interview/session/${sessionId}/message`,
    method: 'post',
    data
  })
}

/**
 * 发送面试消息（流式回复）
 * 返回 fetch Response，可用于读取 SSE 流
 */
export function streamInterviewMessage(sessionId, data, token) {
  return fetch(`/api/interview/session/${sessionId}/message/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(data)
  })
}

/**
 * 结束面试
 * @param {string} sessionId - 会话ID
 * @returns {Promise}
 */
export function endInterview(sessionId) {
  return request({
    url: `/api/interview/session/${sessionId}/end`,
    method: 'post'
  })
}

/**
 * 查询会话详情
 * @param {string} sessionId - 会话ID
 * @returns {Promise}
 */
export function getInterviewSession(sessionId) {
  return request({
    url: `/api/interview/session/${sessionId}`,
    method: 'get'
  })
}

/**
 * 查询面试历史记录（分页）
 *
 * @param {Object} params - 分页参数
 * @param {number} params.pageNum - 页码，默认 1
 * @param {number} params.pageSize - 每页大小，默认 5（列表用），统计时请用 1000
 * @returns {Promise}
 */
export function getInterviewHistory(params = { pageNum: 1, pageSize: 5 }) {
  return request({
    url: '/api/interview/history',
    method: 'get',
    params
  })
}
