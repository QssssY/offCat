import adminRequest from '@/utils/adminRequest'

export function getAdminGrowthConfigs(groupName, params = {}) {
  return adminRequest({
    url: '/api/admin/growth-config',
    method: 'get',
    params: { page: 1, size: 20, ...params, ...(groupName ? { groupName } : {}) }
  })
}

export function createAdminGrowthConfig(data) {
  return adminRequest({ url: '/api/admin/growth-config', method: 'post', data })
}

export function updateAdminGrowthConfig(data) {
  return adminRequest({ url: '/api/admin/growth-config', method: 'put', data })
}

export function deleteAdminGrowthConfig(id) {
  return adminRequest({ url: `/api/admin/growth-config/${id}`, method: 'delete' })
}

export function deleteAdminGrowthConfigsBatch(ids) {
  return adminRequest({ url: '/api/admin/growth-config/batch-delete', method: 'post', data: ids })
}
