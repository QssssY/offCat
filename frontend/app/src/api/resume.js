import request from '@/utils/request'

/**
 * 上传简历PDF文件并创建诊断任务
 * @param {File} file - PDF文件对象
 * @returns {Promise}
 */
export function uploadResume(file, options = {}) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/resume/upload',
    method: 'post',
    params: {
      fallbackToPlatform: Boolean(options.fallbackToPlatform)
    },
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 查询任务详情
 * @param {number|string} taskId - 任务ID
 * @returns {Promise}
 */
export function getResumeTask(taskId) {
  return request({
    url: `/api/resume/task/${taskId}`,
    method: 'get'
  })
}

/**
 * 查询任务轻量状态。
 * 等待页轮询只读取状态和阶段，避免反复拉取简历原文与完整诊断结果。
 * @param {number|string} taskId - 任务ID
 * @returns {Promise}
 */
export function getResumeTaskStatus(taskId) {
  return request({
    url: `/api/resume/task/${taskId}/status`,
    method: 'get'
  })
}

/**
 * 查询历史记录（分页）
 *
 * @param {Object} params - 分页参数
 * @param {number} params.pageNum - 页码，默认 1
 * @param {number} params.pageSize - 每页大小，默认 10（列表用），统计时请用 1000
 * @returns {Promise}
 */
export function getResumeHistory(params = { pageNum: 1, pageSize: 10 }) {
  return request({
    url: '/api/resume/history',
    method: 'get',
    params
  })
}

/**
 * 清理当前用户的全部简历诊断历史记录。
 * @returns {Promise}
 */
export function clearResumeHistory() {
  return request({
    url: '/api/resume/history',
    method: 'delete'
  })
}

/**
 * 删除单条简历诊断记录。
 * @param {string|number} taskId - 任务 ID
 * @returns {Promise}
 */
export function deleteResumeHistory(taskId) {
  return request({
    url: `/api/resume/history/${taskId}`,
    method: 'delete'
  })
}

/**
 * 重试失败的简历诊断任务（复用原文件，24h 内有效）。
 * @param {string|number} taskId - 原失败任务 ID
 * @returns {Promise} 新任务 ID
 */
export function retryResumeTask(taskId) {
  return request({
    url: `/api/resume/task/${taskId}/retry`,
    method: 'post'
  })
}

/**
 * 执行岗位 JD 对比分析
 * @param {{resumeTaskId: string|number, resumeText: string, jdText: string}} data - 分析请求参数
 * @returns {Promise}
 */
export function analyzeResumeJobMatch(data, options = {}) {
  return request({
    url: '/api/resume/job-match/analyze',
    method: 'post',
    data: {
      ...data,
      fallbackToPlatform: Boolean(options.fallbackToPlatform || data?.fallbackToPlatform)
    },
    skipDefaultErrorHandler: true
  })
}

/**
 * 执行 AI 简历润色
 * @param {{resumeTaskId: string|number, resumeText: string, jdText?: string}} data - 润色请求参数
 * @returns {Promise}
 */
export function analyzeResumePolish(data, options = {}) {
  return request({
    url: '/api/resume/polish/analyze',
    method: 'post',
    data: {
      ...data,
      fallbackToPlatform: Boolean(options.fallbackToPlatform || data?.fallbackToPlatform)
    },
    timeout: 180000,
    skipDefaultErrorHandler: true
  })
}

/**
 * 保存用户编辑的简历文档
 * @param {string|number} polishRecordId - 润色记录 ID
 * @param {{documentJson: string, editedPlainText?: string}} data - 文档保存请求
 * @returns {Promise}
 */
export function savePolishDocument(polishRecordId, data) {
  return request({
    url: `/api/resume/polish-records/${polishRecordId}/document`,
    method: 'put',
    data
  })
}

/**
 * 从文件URL中提取文件名
 * @param {string} fileUrl - 文件URL
 * @returns {string} 文件名
 */
export function extractFileName(fileUrl) {
  if (!fileUrl) return '未知文件'
  const parts = fileUrl.split('/')
  const fullName = parts[parts.length - 1]
  // 移除时间戳前缀（如果有）
  const match = fullName.match(/\d+_(.+)/)
  return match ? match[1] : fullName
}
