import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import MainLayout from '@/layouts/MainLayout.vue'

let currentRoute = {
  fullPath: '/templates',
  name: 'TemplateLibrary',
  meta: { keepAlive: true }
}

vi.mock('vue-router', () => ({
  useRoute: () => currentRoute,
  useRouter: () => ({
    beforeEach: vi.fn(() => vi.fn()),
    afterEach: vi.fn(() => vi.fn()),
    onError: vi.fn(() => vi.fn())
  })
}))

vi.mock('@/components/AppHeader.vue', () => ({
  default: { name: 'AppHeader', template: '<header />' }
}))

vi.mock('@/components/AppFooter.vue', () => ({
  default: { name: 'AppFooter', template: '<footer />' }
}))

vi.mock('@/components/OnboardingGuide.vue', () => ({
  default: { name: 'OnboardingGuide', template: '<div />' }
}))

vi.mock('@/api/onboarding', () => ({
  getOnboardingStatus: vi.fn(() => Promise.resolve({ data: { showGuide: false } }))
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    isLoggedIn: () => true
  })
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'token')
}))

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('MainLayout', () => {
  it('keeps high-frequency user route views alive and shows delayed route feedback', () => {
    const wrapper = mount(MainLayout, {
      global: {
        stubs: {
          AppHeader: { template: '<header />' },
          AppFooter: { template: '<footer />' },
          OnboardingGuide: { template: '<div />' },
          NMessageProvider: { template: '<div><slot /></div>' },
          RouterView: {
            template: '<div />'
          }
        }
      }
    })
    const source = sourceFile('src/layouts/MainLayout.vue')

    expect(wrapper.vm.keepAliveViews).toEqual([
      'TemplateLibraryView',
      'CommunityView',
      'GrowthCenterView',
      'DashboardView',
      'SettingsView',
      'MembershipView',
      'InterviewHistoryView',
      'HistoryView'
    ])
    expect(source).toContain('setTimeout(() =>')
    expect(source).toContain('}, 120)')
    expect(source).toContain('route-loading-bar')
    expect(source).toContain('route-loading-placeholder')
    expect(source).toContain('routeLoadingTargetText')
    expect(source).toContain('isRouteLoadingPlaceholderRoute')
    expect(source).toContain("loadingRoutePath.value.startsWith('/interview/report')")
    expect(source).toContain("loadingRoutePath.value.startsWith('/settings')")
    expect(source).toContain("loadingRoutePath.value.startsWith('/community/my')")
    expect(source).toContain("return '正在打开面试报告'")
    expect(source).toContain("return '正在打开设置中心'")
    expect(source).toContain("return '正在打开个人动态中心'")
    expect(source).toContain('<KeepAlive :include="keepAliveViews">')
    expect(source).toContain('<Transition name="page-fade" mode="out-in">')
    expect(source).toContain('class="page-fade-route"')
    expect(source).toContain('<component :is="Component" />')
    expect(source).toMatch(/\.page-fade-route\s*\{[\s\S]*?display:\s*flex/)
    expect(source).toMatch(/\.page-fade-route\s*\{[\s\S]*?flex:\s*1/)
    expect(source).toMatch(/\.page-fade-route\s*\{[\s\S]*?min-height:\s*100%/)
  })
})
