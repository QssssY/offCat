import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  getAdminMembershipPlans,
  createAdminMembershipPlan,
  updateAdminMembershipPlan,
  toggleAdminMembershipPlanActive,
  toggleAdminMembershipPlansBatchActive,
  deleteAdminMembershipPlan,
  deleteAdminMembershipPlansBatch,
  getAdminMembershipOrders
} from '@/api/admin/membership'

describe('admin membership API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminMembershipPlans calls correct endpoint', async () => {
    await getAdminMembershipPlans()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/plans',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })

  it('getAdminMembershipPlans merges pagination params', async () => {
    await getAdminMembershipPlans({ page: 2, size: 10 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/plans',
      method: 'get',
      params: { page: 2, size: 10 }
    })
  })

  it('createAdminMembershipPlan sends data', async () => {
    const data = { planCode: 'vip_month', planName: '月卡VIP', priceAmount: 29.90, durationDays: 30 }
    await createAdminMembershipPlan(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/membership/plans', method: 'post', data })
  })

  it('updateAdminMembershipPlan sends data', async () => {
    const data = { id: 1, planName: '年卡VIP' }
    await updateAdminMembershipPlan(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/membership/plans', method: 'put', data })
  })

  it('toggleAdminMembershipPlanActive sends params', async () => {
    await toggleAdminMembershipPlanActive(1, 0)
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/plans/1/active',
      method: 'put',
      params: { status: 0 }
    })
  })

  it('toggleAdminMembershipPlansBatchActive sends ids and status', async () => {
    await toggleAdminMembershipPlansBatchActive([1, 2], 1)
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/plans/batch/active',
      method: 'put',
      data: { ids: [1, 2], isActive: 1 }
    })
  })

  it('deleteAdminMembershipPlan calls correct endpoint', async () => {
    await deleteAdminMembershipPlan(1)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/membership/plans/1', method: 'delete' })
  })

  it('deleteAdminMembershipPlansBatch sends ids', async () => {
    await deleteAdminMembershipPlansBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/plans/batch-delete',
      method: 'post',
      data: [1, 2]
    })
  })

  it('getAdminMembershipOrders sends optional filter', async () => {
    await getAdminMembershipOrders('PAID')
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/orders',
      method: 'get',
      params: { page: 1, size: 20, orderStatus: 'PAID' }
    })
  })

  it('getAdminMembershipOrders omits params when no filter', async () => {
    await getAdminMembershipOrders()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/membership/orders',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })
})
