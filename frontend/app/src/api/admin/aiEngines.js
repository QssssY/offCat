import adminRequest from '@/utils/adminRequest'

/**
 * 获取 AI 引擎配置列表。
 * @returns {Promise}
 */
export function getAdminAiEngines() {
  return adminRequest({
    url: '/api/admin/ai-engines',
    method: 'get'
  })
}

/**
 * 新增 AI 引擎配置。
 * @param {{engineCode: string, engineName: string, providerType: string, businessType: string, modelName: string, baseUrl: string, apiKey: string, temperature: number, maxTokens: number, timeoutMs: number, isActive: number, sort: number, remark?: string}} data
 * @returns {Promise}
 */
export function createAdminAiEngine(data) {
  return adminRequest({
    url: '/api/admin/ai-engines',
    method: 'post',
    data
  })
}

/**
 * 更新 AI 引擎配置。
 * @param {{id: number, engineCode?: string, engineName?: string, providerType?: string, businessType?: string, modelName?: string, baseUrl?: string, apiKey?: string, temperature?: number, maxTokens?: number, timeoutMs?: number, isActive?: number, sort?: number, remark?: string}} data
 * @returns {Promise}
 */
export function updateAdminAiEngine(data) {
  return adminRequest({
    url: '/api/admin/ai-engines',
    method: 'put',
    data
  })
}

/**
 * 测试 AI 引擎配置连通性。
 * @param {{id?: number, providerType: string, modelName: string, baseUrl: string, apiKey?: string, thinkingMode?: string, temperature?: number, maxTokens?: number, timeoutMs?: number}} data
 * @returns {Promise}
 */
export function testAdminAiEngineConnectivity(data) {
  return adminRequest({
    url: '/api/admin/ai-engines/connectivity-test',
    method: 'post',
    data
  })
}

/**
 * 启用或禁用 AI 引擎配置。
 * @param {number} id
 * @param {number} isActive - 1 启用，0 禁用
 * @returns {Promise}
 */
export function toggleAdminAiEngineActive(id, isActive) {
  return adminRequest({
    url: `/api/admin/ai-engines/${id}/active`,
    method: 'put',
    params: { isActive }
  })
}

/**
 * 删除 AI 引擎配置（物理删除）
 * @param {number} id AI引擎配置ID
 * @returns {Promise}
 */
export function deleteAiEngine(id) {
  return adminRequest({
    url: `/api/admin/ai-engines/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除 AI 引擎配置（物理删除）
 * @param {number[]} ids AI引擎配置ID数组
 * @returns {Promise}
 */
export function deleteAiEngines(ids) {
  return adminRequest({
    url: '/api/admin/ai-engines/batch-delete',
    method: 'post',
    data: ids
  })
}

/**
 * 批量启用或禁用 AI 引擎配置
 * @param {number[]} ids AI引擎配置ID数组
 * @param {number} isActive - 1 启用，0 禁用
 * @returns {Promise}
 */
export function toggleAiEnginesBatchActive(ids, isActive) {
  return adminRequest({
    url: '/api/admin/ai-engines/batch/active',
    method: 'put',
    data: { ids, isActive }
  })
}

/**
 * 查询用户自定义 AI 每日调用上限。
 * @returns {Promise}
 */
export function getCustomAiDailyLimit() {
  return adminRequest({
    url: '/api/admin/custom-ai/daily-limit',
    method: 'get'
  })
}

/**
 * 更新用户自定义 AI 每日调用上限。
 * @param {number} limit
 * @returns {Promise}
 */
export function updateCustomAiDailyLimit(limit) {
  return adminRequest({
    url: '/api/admin/custom-ai/daily-limit',
    method: 'put',
    data: { limit }
  })
}

/**
 * 查询用户自定义 AI 用量统计。
 * @param {{date?: string, startDate?: string, endDate?: string, page?: number, pageSize?: number}} params
 * @returns {Promise}
 */
export function getCustomAiUsageStats(params = {}) {
  const requestParams = {
    page: params.page || 1,
    pageSize: params.pageSize || 20
  }
  if (params.date) {
    requestParams.date = params.date
  }
  if (params.startDate) {
    requestParams.startDate = params.startDate
  }
  if (params.endDate) {
    requestParams.endDate = params.endDate
  }

  return adminRequest({
    url: '/api/admin/custom-ai/usage-stats',
    method: 'get',
    params: requestParams
  })
}

/**
 * 查询用户自定义 AI 按日趋势。
 * @param {{startDate?: string, endDate?: string}} params
 * @returns {Promise}
 */
export function getCustomAiUsageTrends(params = {}) {
  return adminRequest({
    url: '/api/admin/custom-ai/usage-trends',
    method: 'get',
    params: {
      startDate: params.startDate,
      endDate: params.endDate
    }
  })
}

/**
 * 根据管理端当前 AI 引擎表单拉取 OpenAI 兼容模型列表；编辑态可由后端复用已保存密钥。
 * @param {{id?: number, providerType: string, baseUrl: string, apiKey?: string, timeoutMs?: number}} data
 * @returns {Promise}
 */
export function fetchAdminAiModels(data) {
  return adminRequest({
    url: '/api/admin/ai-engines/models',
    method: 'post',
    data
  })
}
