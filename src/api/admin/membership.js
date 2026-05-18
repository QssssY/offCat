import adminRequest from '@/utils/adminRequest'

export function getAdminMembershipPlans(params = {}) {
  return adminRequest({
    url: '/api/admin/membership/plans',
    method: 'get',
    params: { page: 1, size: 20, ...params }
  })
}

export function createAdminMembershipPlan(data) {
  return adminRequest({ url: '/api/admin/membership/plans', method: 'post', data })
}

export function updateAdminMembershipPlan(data) {
  return adminRequest({ url: '/api/admin/membership/plans', method: 'put', data })
}

export function toggleAdminMembershipPlanActive(id, status) {
  return adminRequest({
    url: `/api/admin/membership/plans/${id}/active`,
    method: 'put',
    params: { status }
  })
}

export function toggleAdminMembershipPlansBatchActive(ids, status) {
  return adminRequest({
    url: '/api/admin/membership/plans/batch/active',
    method: 'put',
    data: { ids, isActive: status }
  })
}

export function deleteAdminMembershipPlan(id) {
  return adminRequest({ url: `/api/admin/membership/plans/${id}`, method: 'delete' })
}

export function deleteAdminMembershipPlansBatch(ids) {
  return adminRequest({ url: '/api/admin/membership/plans/batch-delete', method: 'post', data: ids })
}

export function getAdminMembershipOrders(orderStatus, params = {}) {
  return adminRequest({
    url: '/api/admin/membership/orders',
    method: 'get',
    params: { page: 1, size: 20, ...params, ...(orderStatus ? { orderStatus } : {}) }
  })
}
