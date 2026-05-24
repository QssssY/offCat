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

  it('should present a background-led career path home page with six journey nodes', async () => {
    getLatestVersionLogs.mockResolvedValue({ data: [] })
    getPublicStats.mockResolvedValue({
      data: {
        userCount: 0,
        diagnosisCount: 0,
        interviewCount: 0
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('简历诊断')
    expect(wrapper.find('.hero-section').exists()).toBe(true)
    expect(wrapper.find('.background-hero-section').exists()).toBe(true)
    expect(wrapper.find('.hero-background-art').exists()).toBe(true)
    expect(wrapper.findAll('.hero-cloud')).toHaveLength(3)
    expect(wrapper.findAll('.hero-motion-cloud')).toHaveLength(3)
    expect(wrapper.find('.hero-main').exists()).toBe(true)
    expect(wrapper.find('.hero-quick-trails').exists()).toBe(true)
    expect(wrapper.find('.career-path-section').exists()).toBe(true)
    expect(wrapper.find('.career-path-rail').exists()).toBe(true)
    expect(wrapper.find('.motion-path-rail').exists()).toBe(true)
    expect(wrapper.find('.motion-hero-shell').exists()).toBe(true)
    expect(wrapper.find('.path-progress-track').exists()).toBe(true)
    expect(wrapper.find('.workflow-section').exists()).toBe(true)
    expect(wrapper.find('.workflow-steps').exists()).toBe(true)
    expect(wrapper.find('.motion-workflow-steps').exists()).toBe(true)
    expect(wrapper.findAll('.workflow-step')).toHaveLength(4)
    expect(wrapper.findAll('.motion-workflow-step')).toHaveLength(4)
    expect(wrapper.findAll('.step-index')).toHaveLength(4)

    expect(wrapper.text()).toContain('岗位匹配')
    expect(wrapper.text()).toContain('模拟面试')
    expect(wrapper.text()).toContain('面试复盘')
    expect(wrapper.text()).toContain('简历模板库')
    expect(wrapper.text()).toContain('Offer 辅助')
    expect(wrapper.text()).toContain('成长中心')
    expect(wrapper.text()).toContain('社区交流')
    expect(wrapper.text()).toContain('会员与额度')
    expect(wrapper.text()).toContain('通知与版本动态')
    expect(wrapper.findAll('.career-path-node')).toHaveLength(6)
    expect(wrapper.findAll('.motion-feature-card')).toHaveLength(6)
    expect(wrapper.findAll('.route-icon .size-xl')).toHaveLength(6)
    expect(wrapper.findAll('.route-arrow.size-md')).toHaveLength(6)
    expect(wrapper.find('.support-capability-list').exists()).toBe(true)
    expect(wrapper.findAll('.support-capability-item')).toHaveLength(4)
    expect(wrapper.findAll('.motion-support-item')).toHaveLength(4)
    expect(wrapper.findAll('.support-icon .size-lg')).toHaveLength(4)
    expect(wrapper.findAll('.support-next.size-md')).toHaveLength(4)
    expect(wrapper.find('.n-button').exists()).toBe(true)
    expect(wrapper.findAll('.cta-btn .btn-icon.size-md')).toHaveLength(2)
  })
})
