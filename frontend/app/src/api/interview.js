import request from '@/utils/request'
import { API_CACHE_TTL, cachedGet, clearApiCacheByPrefix } from '@/utils/apiCache'
import { getToken } from '@/utils/auth'

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
 * @param {number} [data.interactionType] - 交互方式，0 文字面试 / 1 语音面试
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
  return cachedGet('config:jobRoles:interview', API_CACHE_TTL.JOB_ROLES, () =>
    request({
      url: '/api/interview/job-roles',
      method: 'get'
    })
  )
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
 * 注：因为 Axios 不支持原生流式响应，这里使用裸 fetch；调用方必须自行管理 AbortController、token 与超时，
 * 不要把这种实现复制到普通 API 模块。
 * @param {string} sessionId - 会话 ID
 * @param {{content: string, feedbackMode?: string}} data - 消息参数，feedbackMode 控制每题反馈或面完复盘
 * @param {string} token - 登录 token
 * @param {{ signal?: AbortSignal }} [options] - 可选配置，signal 用于路由切换或重复发送时取消旧流
 * @returns {Promise<Response>}
 */
export function streamInterviewMessage(sessionId, data, token, options = {}) {
  return fetch(`/api/interview/session/${sessionId}/message/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({
      ...data,
      fallbackToPlatform: Boolean(options.fallbackToPlatform || data?.fallbackToPlatform)
    }),
    signal: options.signal
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
 * 查询面试会话轻量状态。
 * 用于开场白和报告生成轮询，避免反复拉取聊天记录和评估报告大字段。
 * @param {string} sessionId - 会话 ID
 * @returns {Promise}
 */
export function getInterviewSessionStatus(sessionId) {
  return request({
    url: `/api/interview/session/${sessionId}/status`,
    method: 'get'
  })
}

/**
 * 查询当前语音面试是否可使用用户自定义云端 TTS。
 * 该接口只返回可用性和配置类型，不返回 baseUrl / model / key 等敏感配置。
 * @param {string} sessionId - 会话 ID
 * @returns {Promise}
 */
export function getInterviewTtsCapability(sessionId) {
  return request({
    url: `/api/interview/session/${sessionId}/tts-capability`,
    method: 'get',
    skipDefaultErrorHandler: true
  })
}

/**
 * 调用后端合成面试官播报音频。
 * 这里使用 fetch 接收 audio/mpeg Blob，避免 Axios 默认 JSON 解析干扰二进制响应。
 * @param {string} sessionId - 会话 ID
 * @param {string} text - 待合成文本
 * @param {{ signal?: AbortSignal, voiceId?: string }} [options] - 可选取消信号与播报音色；voiceId 仅对 EdgeTTS 生效
 * @returns {Promise<Blob>}
 */
export async function synthesizeInterviewTts(sessionId, text, options = {}) {
  const token = getToken()
  // voiceId 为空时不写入请求体，保持与旧调用方一致，由后端沿用配置默认音色。
  const payload = options.voiceId ? { text, voiceId: options.voiceId } : { text }
  const response = await fetch(`/api/interview/session/${sessionId}/tts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify(payload),
    signal: options.signal
  })

  if (!response.ok) {
    let message = `TTS 合成失败 (${response.status})`
    let code = null
    try {
      const errorBody = await response.json()
      code = errorBody.code ?? null
      message = errorBody.message || errorBody.msg || message
    } catch { /* 音频接口失败体不一定是 JSON，保留通用错误即可 */ }
    const error = new Error(message)
    error.status = response.status
    error.code = code
    throw error
  }

  return response.blob()
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

/**
 * 清理当前用户的全部面试历史记录。
 * @returns {Promise}
 */
export function clearInterviewHistory() {
  return request({
    url: '/api/interview/history',
    method: 'delete'
  }).then((response) => {
    clearApiCacheByPrefix('user:growthOverview')
    return response
  })
}

/**
 * 删除单条面试会话记录。
 * @param {string} sessionId - 会话 ID
 * @returns {Promise}
 */
export function deleteInterviewSession(sessionId) {
  return request({
    url: `/api/interview/history/${sessionId}`,
    method: 'delete'
  }).then((response) => {
    clearApiCacheByPrefix('user:growthOverview')
    return response
  })
}
