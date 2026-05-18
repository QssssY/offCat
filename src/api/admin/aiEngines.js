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
