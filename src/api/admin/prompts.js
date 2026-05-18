import adminRequest from '@/utils/adminRequest'

/**
 * 获取 Prompt 模板列表。
 * @returns {Promise}
 */
export function getAdminPrompts() {
  return adminRequest({
    url: '/api/admin/prompts',
    method: 'get'
  })
}

/**
 * 新增 Prompt 模板。
 * @param {{scenarioType: number, jobRoleCode: string, difficulty: number, promptContent: string}} data
 * @returns {Promise}
 */
export function createAdminPrompt(data) {
  return adminRequest({
    url: '/api/admin/prompts',
    method: 'post',
    data
  })
}

/**
 * 更新 Prompt 模板。
 * @param {{id: number, scenarioType?: number, jobRoleCode?: string, difficulty?: number, promptContent?: string, isActive?: number}} data
 * @returns {Promise}
 */
export function updateAdminPrompt(data) {
  return adminRequest({
    url: '/api/admin/prompts',
    method: 'put',
    data
  })
}

/**
 * 启用或禁用 Prompt 模板。
 * @param {number} id
 * @param {number} isActive - 1 启用，0 禁用
 * @returns {Promise}
 */
export function toggleAdminPromptActive(id, isActive) {
  return adminRequest({
    url: `/api/admin/prompts/${id}/active`,
    method: 'put',
    params: { isActive }
  })
}

/**
 * 删除 Prompt 模板（物理删除）
 * @param {number} id Prompt模板ID
 * @returns {Promise}
 */
export function deletePrompt(id) {
  return adminRequest({
    url: `/api/admin/prompts/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除 Prompt 模板（物理删除）
 * @param {number[]} ids Prompt模板ID数组
 * @returns {Promise}
 */
export function deletePrompts(ids) {
  return adminRequest({
    url: '/api/admin/prompts/batch-delete',
    method: 'post',
    data: ids
  })
}

/**
 * 批量启用或禁用 Prompt 模板
 * @param {number[]} ids Prompt模板ID数组
 * @param {number} isActive - 1 启用，0 禁用
 * @returns {Promise}
 */
export function togglePromptsBatchActive(ids, isActive) {
  return adminRequest({
    url: '/api/admin/prompts/batch/active',
    method: 'put',
    data: { ids, isActive }
  })
}
