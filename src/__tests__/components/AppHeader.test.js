import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { updateNickname } from '@/api/auth'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/' })
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

vi.mock('@/stores/theme', () => ({
  useThemeStore: vi.fn()
}))

vi.mock('@/api/auth', () => ({
  updateNickname: vi.fn(() => Promise.resolve())
}))

vi.mock('@/api/notification', () => ({
  getNotifications: vi.fn(() => Promise.resolve({ code: 200, data: { records: [], unreadCount: 0 } })),
  getUnreadCount: vi.fn(() => Promise.resolve({ code: 200, data: { unreadCount: 0 } })),
  markAsRead: vi.fn(() => Promise.resolve()),
  markAllAsRead: vi.fn(() => Promise.resolve()),
  connectNotificationStream: vi.fn(() => ({ abort: vi.fn() }))
}))

vi.mock('@/utils/settingsPreferences', () => ({
  SETTINGS_PREFERENCES_UPDATED_EVENT: 'ai-resume-settings-preferences-updated',
  getSettingsPreferences: vi.fn(() => ({ notificationRealtimeEnabled: false }))
}))

vi.mock('@/components/notification/NotificationTypeIcon.vue', () => ({
  default: {
    name: 'NotificationTypeIcon',
    template: '<span class="notification-type-icon-stub"></span>'
  }
}))

const mountHeader = () => mount(AppHeader, {
  global: {
    stubs: {
      RouterLink: {
        props: ['to'],
        template: '<a><slot /></a>'
      },
      ElDropdown: {
        template: '<div><slot /><slot name="dropdown" /></div>'
      },
      ElDropdownMenu: {
        template: '<div><slot /></div>'
      },
      ElDropdownItem: {
        template: '<div><slot /></div>'
      },
      ElPopover: {
        template: '<div><slot name="reference" /><slot /></div>'
      },
      ElDrawer: {
        template: '<div><slot /></div>'
      },
      ElDialog: {
        template: '<div class="dialog-stub" :data-width="$attrs.width"><slot name="header" /><slot /><slot name="footer" /></div>'
      },
      ElForm: {
        template: '<form><slot /></form>',
        methods: {
          validate: () => Promise.resolve(),
          resetFields: () => {}
        }
      },
      ElFormItem: {
        template: '<div><slot /></div>'
      },
      ElInput: {
        props: ['modelValue'],
        template: '<input :value="modelValue" autocomplete="nickname" />'
      },
      ElButton: {
        template: '<button><slot /></button>'
      },
      ElTag: {
        template: '<span><slot /></span>'
      }
    }
  }
})

describe('AppHeader', () => {
  const fetchUserInfo = vi.fn(() => Promise.resolve())

  beforeEach(() => {
    vi.clearAllMocks()
    useUserStore.mockReturnValue({
      userInfo: {
        username: 'tester',
        nickname: '测试用户',
        role: 0
      },
      isLoggedIn: () => true,
      isVip: () => false,
      fetchUserInfo,
      clearUserInfo: vi.fn()
    })
    useThemeStore.mockReturnValue({
      resolvedTheme: 'light',
      toggleTheme: vi.fn()
    })
  })

  it('keeps nickname and settings entries while removing password and security question entries', () => {
    const wrapper = mountHeader()
    const text = wrapper.text()

    expect(text).toContain('设置中心')
    expect(text).toContain('修改昵称')
    expect(text).not.toContain('修改密码')
    expect(text).not.toContain('修改安全问题')
  })

  it('saves nickname from avatar menu dialog and refreshes user info', async () => {
    const wrapper = mountHeader()

    wrapper.vm.handleCommand('nickname')
    wrapper.vm.nicknameForm.nickname = '新昵称'
    await wrapper.vm.handleNicknameSave()

    expect(updateNickname).toHaveBeenCalledWith({ nickname: '新昵称' })
    expect(fetchUserInfo).toHaveBeenCalled()
  })

  it('uses responsive width for nickname dialog', () => {
    const wrapper = mountHeader()
    const dialogWidths = wrapper.findAll('.dialog-stub').map((dialog) => dialog.attributes('data-width'))

    expect(dialogWidths).toContain('min(440px, calc(100vw - 24px))')
  })
})
