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
  it('keeps only high-frequency route views alive and shows delayed route feedback', () => {
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
      'GrowthCenterView'
    ])
    expect(source).toContain('setTimeout(() =>')
    expect(source).toContain('}, 120)')
    expect(source).toContain('route-loading-bar')
    expect(source).toContain('<KeepAlive :include="keepAliveViews">')
  })
})
