import adminRequest from '@/utils/adminRequest'

/**
 * 查询当前系统级 STT（语音识别）配置；后端只返回脱敏 API Key。
 * @returns {Promise}
 */
export function getAdminSttConfig() {
  return adminRequest({
    url: '/api/admin/stt-config',
    method: 'get'
  })
}

/**
 * 保存系统级 STT 配置；编辑态 API Key 为空或脱敏值时由后端复用已保存密钥。
 * @param {{enabled: boolean, baseUrl?: string, apiKey?: string, model?: string, endpointPath?: string}} data
 * @returns {Promise}
 */
export function saveAdminSttConfig(data) {
  return adminRequest({
    url: '/api/admin/stt-config',
    method: 'put',
    data
  })
}

/**
 * 使用当前表单值测试系统级 STT 连通性，不保存配置。
 * @param {{enabled?: boolean, baseUrl: string, apiKey?: string, model: string, endpointPath?: string}} data
 * @returns {Promise}
 */
export function testAdminSttConnectivity(data) {
  return adminRequest({
    url: '/api/admin/stt-config/test-connectivity',
    method: 'post',
    data
  })
}

/**
 * 使用当前表单值发现可用 STT 模型列表（后端调用 OpenAI 兼容 GET /models），不保存配置。
 * @param {{baseUrl: string, apiKey?: string, endpointPath?: string}} data
 * @returns {Promise}
 */
export function discoverAdminSttModels(data) {
  return adminRequest({
    url: '/api/admin/stt-config/discover',
    method: 'post',
    data
  })
}
