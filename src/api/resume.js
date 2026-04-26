import request from '@/utils/request'

/**
 * 上传简历PDF文件并创建诊断任务
 * @param {File} file - PDF文件对象
 * @returns {Promise}
 */
export function uploadResume(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: '/api/resume/upload',
    method: 'post',
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
 * 执行岗位 JD 对比分析
 * @param {{resumeTaskId: string|number, resumeText: string, jdText: string}} data - 分析请求参数
 * @returns {Promise}
 */
export function analyzeResumeJobMatch(data) {
  return request({
    url: '/api/resume/job-match/analyze',
    method: 'post',
    data
  })
}

/**
 * 执行 AI 简历润色
 * @param {{resumeTaskId: string|number, resumeText: string, jdText?: string}} data - 润色请求参数
 * @returns {Promise}
 */
export function analyzeResumePolish(data) {
  return request({
    url: '/api/resume/polish/analyze',
    method: 'post',
    data,
    timeout: 180000,
    skipDefaultErrorHandler: true
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
