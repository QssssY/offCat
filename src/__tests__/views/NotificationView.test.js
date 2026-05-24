import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
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
    props: ['type', 'size', 'halo'],
    template: '<span class="notification-type-icon-stub" :data-halo="String(!!halo)"></span>'
  }
}))

const viewSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/notification/NotificationView.vue'), 'utf8')

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

  it('uses compact halos only on interactive notification list icons', () => {
    const source = viewSource()

    expect(source).toContain('<NotificationTypeIcon class="item-icon" :type="item.type" size="md" halo />')
    expect(source).toContain('<NotificationTypeIcon :type="selectedAnnouncement.type" size="sm" />')
    expect(source).toContain('notification-item:hover .item-icon')
    expect(source).toContain('prefers-reduced-motion: reduce')
    expect(source).toMatch(/\.item-icon\s*\{[\s\S]*?width:\s*44px/)
    expect(source).toMatch(/\.item-icon\s+svg\s*\{[\s\S]*?width:\s*24px/)
    expect(source).not.toContain('transition: all')
    expect(source).not.toMatch(/\.item-icon\.type-(resume|polish|interview|quota|system|activity|update|maintenance)\s*\{[\s\S]*?background:/)
  })
})
