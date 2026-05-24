import { mount } from '@vue/test-utils'
import { NConfigProvider } from 'naive-ui'
import { describe, expect, it, vi } from 'vitest'
import App from '@/App.vue'
import { useThemeStore } from '@/stores/theme'

vi.mock('vue-router', () => ({
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
})
