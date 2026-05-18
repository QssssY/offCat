import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import NotificationView from '@/views/notification/NotificationView.vue'
import { getNotifications, getUnreadCount } from '@/api/notification'
import { saveSettingsPreferences } from '@/utils/settingsPreferences'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

vi.mock('@/api/notification', () => ({
  getNotifications: vi.fn(),
  getUnreadCount: vi.fn(),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
  deleteNotification: vi.fn(),
  batchDeleteNotifications: vi.fn()
}))

vi.mock('@/components/notification/NotificationTypeIcon.vue', () => ({
  default: {
    name: 'NotificationTypeIcon',
    template: '<span class="notification-type-icon-stub"></span>'
  }
}))

describe('NotificationView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    getUnreadCount.mockResolvedValue({ code: 200, data: { unreadCount: 1 } })
    getNotifications.mockResolvedValue({
      code: 200,
      data: {
        records: [],
        total: 0,
        unreadCount: 1
      }
    })
  })

  it('uses saved default filters when loading notifications', async () => {
    saveSettingsPreferences({
      notificationDefaultUnreadOnly: true,
      notificationDefaultType: 'resume'
    })

    mount(NotificationView)
    await flushPromises()

    expect(getNotifications).toHaveBeenCalledWith({
      pageNum: 1,
      size: 5,
      type: 'resume',
      readStatus: 0
    })
  })
})
