import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import AdminVersionLogView from '@/views/admin/AdminVersionLogView.vue'
import { getAdminVersionLogs } from '@/api/admin/versionLogs'

let currentWrapper = null

vi.mock('@/api/admin/versionLogs', () => ({
  getAdminVersionLogs: vi.fn(() => Promise.resolve({
    data: { records: [], total: 0, page: 1, size: 20 }
  })),
  createAdminVersionLog: vi.fn(() => Promise.resolve({ data: 1 })),
  updateAdminVersionLog: vi.fn(() => Promise.resolve()),
  publishAdminVersionLog: vi.fn(() => Promise.resolve()),
  publishAdminVersionLogsBatch: vi.fn(() => Promise.resolve()),
  deleteAdminVersionLog: vi.fn(() => Promise.resolve()),
  deleteAdminVersionLogsBatch: vi.fn(() => Promise.resolve())
}))

vi.mock('@/utils/adminFeedback', () => ({
  showAdminError: vi.fn(),
  showAdminSuccess: vi.fn()
}))

const mountView = async () => {
  currentWrapper = mount(AdminVersionLogView, {
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

describe('AdminVersionLogView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
  })

  it('loads version logs with selected filter params and resets to first page', async () => {
    const wrapper = await mountView()

    wrapper.vm.currentPage = 4
    wrapper.vm.filterForm.type = 'major'
    wrapper.vm.filterForm.status = '1'
    wrapper.vm.keyword = '体验优化'
    wrapper.vm.handleFilterChange()
    await flushPromises()

    expect(wrapper.vm.currentPage).toBe(1)
    expect(getAdminVersionLogs).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      type: 'major',
      status: 1,
      keyword: '体验优化'
    })
  })

  it('clears all filters before reloading the list', async () => {
    const wrapper = await mountView()

    wrapper.vm.filterForm.type = 'patch'
    wrapper.vm.filterForm.status = '0'
    wrapper.vm.keyword = '修复'
    wrapper.vm.resetFilters()
    await flushPromises()

    expect(getAdminVersionLogs).toHaveBeenLastCalledWith({ page: 1, size: 20 })
  })
})
