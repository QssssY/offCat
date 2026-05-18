import request from '@/utils/request'

/**
 * 创建面试会话。
 * 本轮扩展支持岗位定向配置，但仍兼容普通模拟面试请求。
 *
 * @param {Object} data - 创建参数
 * @param {string} data.jobRole - 面试岗位名称
 * @param {string} [data.jobRoleCode] - 岗位编码
 * @param {number} data.difficulty - 难度级别，1 初级 / 2 中级 / 3 高级
 * @param {string} [data.interviewMode] - 面试模式，normal / stress
 * @param {boolean} [data.jobTargeted] - 是否开启岗位定向模拟
 * @param {string|number} [data.resumeTaskId] - 关联的简历诊断任务 ID，普通模拟面试和岗位定向模拟都可携带
 * @param {string} [data.jdText] - 手动输入的岗位 JD 文本
 * @param {boolean} [data.useLatestJobMatch] - 是否优先复用最近一次 JD 对比结果
 * @param {string|number} [data.jobMatchRecordId] - 指定复用的 JD 对比记录 ID
 * @returns {Promise}
 */
export function createInterviewSession(data) {
  return request({
    url: '/api/interview/session',
    method: 'post',
    data,
    timeout: 30000,
    skipDefaultErrorHandler: true
  })
}

/**
 * 查询当前启用的面试岗位选项。
 * @returns {Promise}
 */
export function getInterviewJobRoles() {
  return request({
    url: '/api/interview/job-roles',
    method: 'get'
  })
}

/**
 * 发送面试消息。
 * @param {string} sessionId - 会话 ID
 * @param {{content: string, feedbackMode?: string}} data - 消息参数，feedbackMode 控制每题反馈或面完复盘
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
 * 发送面试消息并获取流式回复。
 * @param {string} sessionId - 会话 ID
 * @param {{content: string, feedbackMode?: string}} data - 消息参数，feedbackMode 控制每题反馈或面完复盘
 * @param {string} token - 登录 token
 * @returns {Promise<Response>}
 */
export function streamInterviewMessage(sessionId, data, token) {
  return fetch(`/api/interview/session/${sessionId}/message/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(data)
  })
}

/**
 * 结束面试。
 * @param {string} sessionId - 会话 ID
 * @returns {Promise}
 */
export function endInterview(sessionId) {
  return request({
    url: `/api/interview/session/${sessionId}/end`,
    method: 'post'
  })
}

/**
 * 查询面试会话详情。
 * @param {string} sessionId - 会话 ID
 * @returns {Promise}
 */
export function getInterviewSession(sessionId) {
  return request({
    url: `/api/interview/session/${sessionId}`,
    method: 'get'
  })
}

/**
 * 查询面试历史记录。
 * @param {Object} params - 分页参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页条数
 * @returns {Promise}
 */
export function getInterviewHistory(params = { pageNum: 1, pageSize: 5 }) {
  return request({
    url: '/api/interview/history',
    method: 'get',
    params
  })
}
