import adminRequest from '@/utils/adminRequest'

/**
 * 获取岗位配置列表。
 * 说明：管理端岗位配置页的表格数据来源。
 * @returns {Promise}
 */
export function getAdminJobRoles() {
  return adminRequest({
    url: '/api/admin/job-roles',
    method: 'get'
  })
}

/**
 * 新增岗位配置。
 * @param {{roleCode: string, roleName: string, interviewTag?: string, tagType?: string, sort: number}} data
 * @returns {Promise}
 */
export function createAdminJobRole(data) {
  return adminRequest({
    url: '/api/admin/job-roles',
    method: 'post',
    data
  })
}

/**
 * 更新岗位配置。
 * @param {{id: number, roleCode?: string, roleName?: string, interviewTag?: string, tagType?: string, isActive?: number, sort?: number}} data
 * @returns {Promise}
 */
export function updateAdminJobRole(data) {
  return adminRequest({
    url: '/api/admin/job-roles',
    method: 'put',
    data
  })
}

/**
 * 启用或禁用岗位配置。
 * @param {number} id
 * @param {number} isActive - 1 启用，0 禁用
 * @returns {Promise}
 */
export function toggleAdminJobRoleActive(id, isActive) {
  return adminRequest({
    url: `/api/admin/job-roles/${id}/active`,
    method: 'put',
    params: { isActive }
  })
}

/**
 * 删除岗位配置（物理删除）
 * @param {number} id 岗位配置ID
 * @returns {Promise}
 */
export function deleteJobRole(id) {
  return adminRequest({
    url: `/api/admin/job-roles/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除岗位配置（物理删除）
 * @param {number[]} ids 岗位配置ID数组
 * @returns {Promise}
 */
export function deleteJobRoles(ids) {
  return adminRequest({
    url: '/api/admin/job-roles/batch-delete',
    method: 'post',
    data: ids
  })
}

/**
 * 批量启用或禁用岗位配置
 * @param {number[]} ids 岗位配置ID数组
 * @param {number} isActive - 1 启用，0 禁用
 * @returns {Promise}
 */
export function toggleJobRolesBatchActive(ids, isActive) {
  return adminRequest({
    url: '/api/admin/job-roles/batch/active',
    method: 'put',
    data: { ids, isActive }
  })
}
