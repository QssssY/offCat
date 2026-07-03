import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
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

const homePageSource = () => readFileSync(
  join(process.cwd(), 'src/views/HomePageView.vue'),
  'utf-8'
)

describe('HomePageView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.documentElement.removeAttribute('data-theme')
  })

  afterEach(() => {
    vi.useRealTimers()
    document.documentElement.removeAttribute('data-theme')
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
    expect(wrapper.find('.theme-aware-home').exists()).toBe(true)
    expect(wrapper.find('.background-hero-section').exists()).toBe(true)
    expect(wrapper.find('.hero-background-art').exists()).toBe(true)
    expect(wrapper.find('.hero-starry-sky').exists()).toBe(true)
    expect(wrapper.find('.hero-moon').exists()).toBe(true)
    expect(wrapper.findAll('.hero-star')).toHaveLength(12)
    expect(wrapper.findAll('.hero-cloud')).toHaveLength(7)
    expect(wrapper.findAll('.hero-motion-cloud')).toHaveLength(7)
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

  it('should keep dark mode target surfaces available for theme overrides', async () => {
    getLatestVersionLogs.mockResolvedValue({
      data: [
        {
          id: 2,
          version: '3.1',
          type: 'patch',
          title: '暗色模式验证标题',
          content: '暗色模式验证内容',
          publishedAt: '2026-05-24T08:00:00'
        }
      ]
    })
    getPublicStats.mockResolvedValue({
      data: {
        userCount: 1,
        diagnosisCount: 2,
        interviewCount: 3
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.theme-aware-home').exists()).toBe(true)
    expect(wrapper.find('.background-hero-section').exists()).toBe(true)
    expect(wrapper.find('.hero-main').exists()).toBe(true)
    expect(wrapper.find('.hero-starry-sky').exists()).toBe(true)
    expect(wrapper.find('.hero-moon').exists()).toBe(true)
    expect(wrapper.findAll('.hero-star')).toHaveLength(12)
    expect(wrapper.findAll('.hero-cloud')).toHaveLength(7)
    expect(wrapper.findAll('.career-path-node')).toHaveLength(6)
    expect(wrapper.find('.workflow-section').exists()).toBe(true)
    expect(wrapper.findAll('.support-capability-item')).toHaveLength(4)
    expect(wrapper.find('.version-section').exists()).toBe(true)
    expect(wrapper.find('.version-item').exists()).toBe(true)
  })

  it('should trigger a light return class when switching from dark mode back to light mode', async () => {
    vi.useFakeTimers()
    document.documentElement.setAttribute('data-theme', 'dark')
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

    document.documentElement.setAttribute('data-theme', 'light')
    await flushPromises()

    expect(wrapper.find('.theme-aware-home').classes()).toContain('is-light-return')

    vi.advanceTimersByTime(1900)
    await flushPromises()

    expect(wrapper.find('.theme-aware-home').classes()).toContain('is-light-return')

    vi.advanceTimersByTime(1300)
    await flushPromises()

    expect(wrapper.find('.theme-aware-home').classes()).not.toContain('is-light-return')
  })

  it('should drive homepage dark mode through scoped theme variables instead of light hardcoded surfaces', () => {
    const source = homePageSource()
    const darkHomeBlock = source.match(/:global\(html\[data-theme="dark"\]\s+\.theme-aware-home\)\s*\{[\s\S]*?\n\}/)?.[0] || ''
    const darkCloudBlock = source.match(/:global\(html\[data-theme="dark"\]\s+\.hero-cloud\)\s*\{[\s\S]*?\n\}/)?.[0] || ''
    const darkMoonBlock = source.match(/:global\(html\[data-theme="dark"\]\s+\.hero-moon\)\s*\{[\s\S]*?\n\}/)?.[0] || ''

    expect(source).toContain('--home-page-bg')
    expect(source).toContain('--home-hero-bg-mobile')
    expect(source).toContain('toCssImageSet')
    expect(source).toContain('optimizedImages.homeBackground.desktopWebp')
    expect(source).toContain('optimizedImages.homeBackground.mobileWebp')
    expect(source).toContain('--home-hero-surface')
    expect(source).toContain('--home-card-surface')
    expect(source).toContain('--home-workflow-bg')
    expect(source).toContain(':global(html[data-theme="dark"] .theme-aware-home)')
    expect(source).toContain(':global(html[data-theme="dark"] .hero-main)')
    expect(source).toContain(':global(html[data-theme="dark"] .hero-cloud)')
    expect(source).toContain(':global(html[data-theme="dark"] .hero-moon)')
    expect(source).toContain(':global(html[data-theme="dark"] .hero-starry-sky)')
    expect(source).toContain(':global(html[data-theme="dark"] .career-path-node)')
    expect(source).toContain('.hero-starry-sky')
    expect(source).toContain('.hero-star')
    expect(source).toContain('.hero-moon')
    expect(source).toContain('data-phase="half"')
    expect(source).toContain('moon-rise')
    expect(source).toContain('moon-glow-breathe')
    expect(source).toContain('cloud-scatter')
    expect(source).toContain('cloud-regather')
    expect(source).toContain('moon-set')
    expect(source).toContain('is-light-return')
    expect(source).toContain('MutationObserver')
    expect(source).toContain('star-twinkle')
    expect(source).toContain('setTimeout(() => {')
    expect(source).toContain('}, 3000)')
    expect(source).toContain('moon-set 1.45s')
    expect(source).toContain('cloud-regather 1.35s')
    expect(source).toContain('calc(1.1s + var(--cloud-scatter-delay, 0s)) both')
    expect(darkCloudBlock).toContain('cloud-scatter 1.18s')
    expect(darkMoonBlock).toContain('moon-rise 1.05s cubic-bezier(0.22, 1, 0.36, 1) 1.52s both')
    expect(source).toContain('@media (prefers-reduced-motion: reduce)')
    expect(source).not.toContain('--home-moon-cutout')
    expect(darkHomeBlock).not.toContain('--text-title:')
    expect(darkHomeBlock).not.toContain('--border-card:')
    expect(source).toMatch(/--home-page-bg:\s*\n\s*radial-gradient/)
    expect(source).toContain('background: var(--home-page-bg);')
    expect(source).toContain('background: var(--home-hero-bg-layer);')
    expect(source).toContain('background: var(--home-card-surface);')
    expect(source).toMatch(/prefers-reduced-motion:[\s\S]*\.hero-cloud/)
    expect(source).toMatch(/prefers-reduced-motion:[\s\S]*\.hero-moon/)
  })

  it('should keep CTA icon and text spacing comfortable', () => {
    const source = homePageSource()

    expect(source).toContain('<span class="cta-content">')
    expect(source).toContain('.cta-btn :deep(.n-button__content)')
    expect(source).toContain('justify-content: center;')
    expect(source).toContain('.cta-content')
    expect(source).toContain('gap: 12px;')
    expect(source).toContain('white-space: nowrap;')
  })
})
