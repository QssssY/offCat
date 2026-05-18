import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomePageView from '@/views/HomePageView.vue'
import { getLatestVersionLogs } from '@/api/versionLog'
import { getPublicStats } from '@/api/stats'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  })
}))

vi.mock('@/api/versionLog', () => ({
  getLatestVersionLogs: vi.fn()
}))

vi.mock('@/api/stats', () => ({
  getPublicStats: vi.fn()
}))

vi.mock('@/utils/auth', () => ({
  isLoggedIn: vi.fn(() => false)
}))

const mountView = () => mount(HomePageView, {
  global: {
    stubs: {
      RouterLink: {
        props: ['to'],
        template: '<a><slot /></a>'
      }
    }
  }
})

describe('HomePageView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render latest logs with title ellipsis hook and theme-driven section classes', async () => {
    getLatestVersionLogs.mockResolvedValue({
      data: [
        {
          id: 1,
          version: '3.0',
          type: 'major',
          title: '这是一个很长很长的标题用于验证首页最近更新卡片标题是否保留省略能力',
          content: '这里是版本更新内容',
          publishedAt: '2026-05-17T08:00:00'
        }
      ]
    })
    getPublicStats.mockResolvedValue({
      data: {
        userCount: 10,
        diagnosisCount: 20,
        interviewCount: 30
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(getLatestVersionLogs).toHaveBeenCalledWith(3)
    const title = wrapper.find('.version-item-title')
    expect(title.exists()).toBe(true)
    expect(title.attributes('title')).toContain('这是一个很长很长的标题')
    expect(wrapper.find('.version-section').exists()).toBe(true)
    expect(wrapper.find('.version-item-desc').exists()).toBe(true)
  })
})
