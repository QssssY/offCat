import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'
import { describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import App from '@/App.vue'
import { useThemeStore } from '@/stores/theme'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    beforeEach: vi.fn(() => vi.fn()),
    afterEach: vi.fn(() => vi.fn()),
    onError: vi.fn(() => vi.fn())
  }),
  useRoute: () => ({
    meta: {
      useLayout: false
    }
  })
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    fetchUserInfo: vi.fn(),
    clearUserInfo: vi.fn()
  })
}))

vi.mock('@/stores/theme', () => ({
  useThemeStore: vi.fn()
}))

vi.mock('@/layouts/MainLayout.vue', () => ({
  default: {
    name: 'MainLayout',
    template: '<section class="main-layout-stub"><slot /></section>'
  }
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => null),
  removeToken: vi.fn()
}))

const mountApp = (resolvedTheme = 'light') => {
  useThemeStore.mockReturnValue({
    resolvedTheme
  })

  return mount(App, {
    global: {
      stubs: {
        RouterView: {
          template: '<main class="router-view-stub" />'
        }
      }
    }
  })
}

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('App', () => {
  it('should bridge dark mode into Naive UI config provider', () => {
    const wrapper = mountApp('dark')
    const provider = wrapper.findComponent(NConfigProvider)

    expect(provider.exists()).toBe(true)
    expect(provider.props('theme')).toBeTruthy()
    expect(provider.props('themeOverrides').common.primaryColor).toBe('#FF8C42')
    expect(provider.props('themeOverrides').common.bodyColor).toBe('#1F1511')
    expect(provider.props('themeOverrides').common.cardColor).toBe('#2A1B14')
    expect(provider.props('themeOverrides').common.textColor1).toBe('#FFF3E8')
    expect(provider.props('themeOverrides').common.textColor2).toBe('#F0D1BD')
    expect(provider.props('themeOverrides').common.textColor3).toBe('#CAA189')
  })

  it('should keep Naive UI in light mode when resolved theme is light', () => {
    const wrapper = mountApp('light')
    const provider = wrapper.findComponent(NConfigProvider)

    expect(provider.exists()).toBe(true)
    expect(provider.props('theme')).toBeNull()
    expect(provider.props('themeOverrides').common.bodyColor).toBe('#FFF8F3')
  })

  it('shows delayed global route loading feedback for useLayout false high-risk entries', () => {
    const source = sourceFile('src/App.vue')

    expect(source).toContain('global-route-loading')
    expect(source).toContain('isGlobalRouteLoadingRoute')
    expect(source).toContain("targetPath.startsWith('/admin')")
    expect(source).toContain("targetPath.startsWith('/interview/session')")
    expect(source).toContain("targetPath.startsWith('/templates/editor')")
    expect(source).toContain('router.beforeEach')
    expect(source).toContain('router.afterEach')
    expect(source).toContain('router.onError')
    expect(source).toContain('}, 120)')
  })
})
