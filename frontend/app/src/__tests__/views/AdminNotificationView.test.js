import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import AdminNotificationView from '@/views/admin/AdminNotificationView.vue'
import { getAdminNotifications } from '@/api/admin/notifications'

let currentWrapper = null

vi.mock('@/api/admin/notifications', () => ({
  getAdminNotifications: vi.fn(() => Promise.resolve({
    data: { records: [], total: 0, page: 1, size: 20 }
  })),
  createAdminNotification: vi.fn(() => Promise.resolve({ data: 1 })),
  publishAdminNotification: vi.fn(() => Promise.resolve()),
  publishAdminNotificationsBatch: vi.fn(() => Promise.resolve()),
  deleteAdminNotification: vi.fn(() => Promise.resolve()),
  deleteAdminNotificationsBatch: vi.fn(() => Promise.resolve())
}))

vi.mock('@/utils/adminFeedback', () => ({
  showAdminError: vi.fn(),
  showAdminSuccess: vi.fn()
}))

const mountView = async () => {
  currentWrapper = mount(AdminNotificationView, {
    attachTo: document.body,
    global: {
      plugins: [ElementPlus],
      stubs: {
        transition: false
      }
    }
  })
  await flushPromises()
  return currentWrapper
}

describe('AdminNotificationView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
  })

  it('loads notifications with selected filter params and resets to first page', async () => {
    const wrapper = await mountView()

    wrapper.vm.currentPage = 3
    wrapper.vm.filterForm.type = 'activity'
    wrapper.vm.filterForm.status = '0'
    wrapper.vm.filterForm.targetType = 'vip'
    wrapper.vm.keyword = '维护窗口'
    wrapper.vm.handleFilterChange()
    await flushPromises()

    expect(wrapper.vm.currentPage).toBe(1)
    expect(getAdminNotifications).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      type: 'activity',
      status: 0,
      targetType: 'vip',
      keyword: '维护窗口'
    })
  })

  it('clears all filters before reloading the list', async () => {
    const wrapper = await mountView()

    wrapper.vm.filterForm.type = 'update'
    wrapper.vm.filterForm.status = '1'
    wrapper.vm.filterForm.targetType = 'normal'
    wrapper.vm.keyword = '版本'
    wrapper.vm.resetFilters()
    await flushPromises()

    expect(getAdminNotifications).toHaveBeenLastCalledWith({ page: 1, size: 20 })
  })
})
