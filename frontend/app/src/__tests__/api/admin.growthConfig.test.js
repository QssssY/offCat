import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

import adminRequest from '@/utils/adminRequest'
import {
  getAdminGrowthConfigs,
  createAdminGrowthConfig,
  updateAdminGrowthConfig,
  deleteAdminGrowthConfig,
  deleteAdminGrowthConfigsBatch
} from '@/api/admin/growthConfig'

describe('admin growthConfig API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getAdminGrowthConfigs without group', async () => {
    await getAdminGrowthConfigs()
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/growth-config',
      method: 'get',
      params: { page: 1, size: 20 }
    })
  })

  it('getAdminGrowthConfigs with group filter', async () => {
    await getAdminGrowthConfigs('achievement')
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/growth-config',
      method: 'get',
      params: { page: 1, size: 20, groupName: 'achievement' }
    })
  })

  it('getAdminGrowthConfigs merges pagination params', async () => {
    await getAdminGrowthConfigs('achievement', { page: 2, size: 10 })
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/growth-config',
      method: 'get',
      params: { page: 2, size: 10, groupName: 'achievement' }
    })
  })

  it('createAdminGrowthConfig sends data', async () => {
    const data = { configKey: 'k', configValue: 'v', groupName: 'default' }
    await createAdminGrowthConfig(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/growth-config', method: 'post', data })
  })

  it('updateAdminGrowthConfig sends data', async () => {
    const data = { id: 1, configValue: 'new' }
    await updateAdminGrowthConfig(data)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/growth-config', method: 'put', data })
  })

  it('deleteAdminGrowthConfig calls correct endpoint', async () => {
    await deleteAdminGrowthConfig(1)
    expect(adminRequest).toHaveBeenCalledWith({ url: '/api/admin/growth-config/1', method: 'delete' })
  })

  it('deleteAdminGrowthConfigsBatch sends ids', async () => {
    await deleteAdminGrowthConfigsBatch([1, 2])
    expect(adminRequest).toHaveBeenCalledWith({
      url: '/api/admin/growth-config/batch-delete',
      method: 'post',
      data: [1, 2]
    })
  })
})
