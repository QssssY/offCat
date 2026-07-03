import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
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
        props: ['appendToBody'],
        template: '<div class="drawer-stub" :data-append-to-body="String(appendToBody)"><slot /></div>'
      },
      ElTooltip: {
        template: '<span><slot /></span>'
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

const headerSource = () =>
  readFileSync(resolve(process.cwd(), 'src/components/AppHeader.vue'), 'utf8')

const globalStyleSource = () =>
  readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

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

  it('keeps the motion navigation structure for desktop and mobile header interactions', () => {
    const wrapper = mountHeader()

    expect(wrapper.find('.motion-app-header').exists()).toBe(true)
    expect(wrapper.find('.motion-desktop-nav').exists()).toBe(true)
    expect(wrapper.find('.motion-brand-mark').exists()).toBe(true)
    expect(wrapper.find('.motion-hamburger-btn').exists()).toBe(true)
    expect(wrapper.find('.motion-mobile-nav').exists()).toBe(true)
    expect(wrapper.find('.motion-mobile-nav').text()).toContain('首页')
    expect(wrapper.find('.motion-mobile-nav').text()).toContain('设置中心')
    expect(wrapper.findAll('.desktop-nav .nav-link').length).toBeGreaterThanOrEqual(7)
    expect(wrapper.findAll('.mobile-nav-link').length).toBeGreaterThanOrEqual(10)
  })

  it('opens mobile drawer from hamburger button and appends drawer to body for responsive layout', async () => {
    const wrapper = mountHeader()

    expect(wrapper.vm.drawerVisible).toBe(false)
    expect(wrapper.find('.drawer-stub').attributes('data-append-to-body')).toBe('true')

    await wrapper.find('.motion-hamburger-btn').trigger('click')

    expect(wrapper.vm.drawerVisible).toBe(true)
  })

  it('uses halo-sized header icons without hard notification type blocks', () => {
    const source = headerSource()

    expect(source).toContain('--header-dropdown-icon-size: 28px')
    expect(source).toContain('fetch-priority="high"')
    expect(source).toContain('<OptimizedImage :sources="optimizedImages.logo"')
    expect(source).toContain('optimizedImages.userAvatar')
    expect(source).toContain('.logo-box :deep(.logo-img)')
    expect(source).toContain('class="nav-feature-icon" critical')
    expect(source).toContain('name="menu" size="sm" critical')
    expect(source).toContain('notification-bell:hover :deep(.feature-icon)')
    expect(source).toContain('panel-item:hover .panel-item-icon')
    expect(source).not.toMatch(/\.panel-item-icon\.type-(resume|polish|interview|quota|system)\s*\{[\s\S]*?background:/)
    expect(source).not.toContain('transition: all')
  })

  it('prefetches only high-frequency user navigation routes on intent signals', () => {
    const source = headerSource()
    const routeLoaderSource = readFileSync(resolve(process.cwd(), 'src/router/routeLoaders.js'), 'utf8')

    expect(source).toContain('prefetchUserRoute')
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/resume/upload')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/interview/entry')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/templates')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/community')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/growth')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/offer')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/settings')\"")
    expect(source).toContain("@focus=\"prefetchNavigationRoute('/settings')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/settings')\"")
    expect(source).toContain("@mouseenter=\"prefetchNavigationRoute('/community/my')\"")
    expect(source).toContain("@focus=\"prefetchNavigationRoute('/community/my')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/community/my')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/resume/upload')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/interview/entry')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/templates')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/community')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/growth')\"")
    expect(source).toContain("@touchstart.passive=\"prefetchNavigationRoute('/offer')\"")
    expect(source).toContain('warmupHighFrequencyUserRoutes')
    expect(routeLoaderSource).toContain("'/templates': templateLibraryRouteLoader")
    expect(routeLoaderSource).toContain("'/community': communityRouteLoader")
    expect(routeLoaderSource).toContain("'/settings': settingsRouteLoader")
    expect(routeLoaderSource).toContain("'/community/my': communityMyRouteLoader")
    expect(routeLoaderSource).toContain("'/growth': growthCenterRouteLoader")
    expect(routeLoaderSource).toContain("'/offer': offerAssistRouteLoader")
    expect(routeLoaderSource).toContain("'/resume/upload': resumeUploadRouteLoader")
    expect(routeLoaderSource).toContain("'/interview/entry': interviewEntryRouteLoader")
    expect(source).not.toMatch(/rel=["']preload["'][^>]*routes?/)
  })

  it('keeps history dropdown trigger and menu text on clickable cursors', () => {
    const source = headerSource()
    const globalStyle = globalStyleSource()

    expect(source).toContain('class="nav-link history-trigger"')
    expect(source).toMatch(/class="nav-link history-trigger"[\s\S]*?@mousedown\.prevent/)
    expect(source).toMatch(/command="resume"[\s\S]*?@mousedown\.prevent/)
    expect(source).toMatch(/command="interview"[\s\S]*?@mousedown\.prevent/)
    expect(source).toMatch(/\.history-trigger,[\s\S]*?\.history-trigger\s+:deep\(\*\)[\s\S]*?cursor:\s*pointer;/)
    expect(globalStyle).toMatch(/\.history-dropdown-menu\s+\.el-dropdown-menu__item,[\s\S]*?\.history-dropdown-menu\s+\.el-dropdown-menu__item\s+\*[\s\S]*?cursor:\s*pointer(?:\s*!important)?;/)
    expect(globalStyle).toMatch(/\.history-dropdown-menu\s+\.el-dropdown-menu__item:focus,[\s\S]*?\.history-dropdown-menu\s+\.el-dropdown-menu__item:active,[\s\S]*?\.history-dropdown-menu\s+\.el-dropdown-menu__item\.is-active[\s\S]*?cursor:\s*pointer\s*!important;/)
    expect(globalStyle).toMatch(/\.history-dropdown-menu\s+\.el-dropdown-menu__item,[\s\S]*?user-select:\s*none;/)
  })

  it('uses semantic buttons for notification hover targets so nested text keeps pointer cursor', () => {
    const source = headerSource()

    expect(source).toContain('<button ref="bellRef" type="button" class="notification-bell"')
    expect(source).toContain('<button')
    expect(source).toContain('class="panel-item"')
    expect(source).toContain('type="button"')
    expect(source).toContain('class="panel-footer"')
    expect(source).toMatch(/\.panel-item\s*\{[\s\S]*?border:\s*0;[\s\S]*?background:\s*transparent;[\s\S]*?text-align:\s*left;/)
    expect(source).toMatch(/\.panel-footer\s*\{[\s\S]*?width:\s*100%;[\s\S]*?border:\s*0;[\s\S]*?background:\s*transparent;/)
  })
})
