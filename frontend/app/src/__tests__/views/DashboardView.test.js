import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import DashboardView from '@/views/DashboardView.vue'
import { getResumeHistory } from '@/api/resume'
import { getInterviewHistory } from '@/api/interview'
import { getMonthlyStats } from '@/api/stats'
import { getOnboardingTasks } from '@/api/onboarding'

const push = vi.fn()
const fetchUserInfo = vi.fn(() => Promise.resolve())

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    userInfo: {
      username: 'offer-user',
      nickname: '求职者',
      role: 0,
      createTime: '2026-01-01T00:00:00',
      resumeQuota: 4,
      interviewQuota: 2,
      vipDailyResumeQuota: 0,
      vipDailyInterviewQuota: 0
    },
    fetchUserInfo
  })
}))

vi.mock('@/api/resume', () => ({
  getResumeHistory: vi.fn(),
  extractFileName: vi.fn((fileUrl) => fileUrl || 'resume.pdf')
}))

vi.mock('@/api/interview', () => ({
  getInterviewHistory: vi.fn()
}))

vi.mock('@/api/stats', () => ({
  getMonthlyStats: vi.fn()
}))

vi.mock('@/api/onboarding', () => ({
  getOnboardingTasks: vi.fn()
}))

vi.mock('@/components/OnboardingTaskCard.vue', () => ({
  default: {
    name: 'OnboardingTaskCard',
    template: '<div class="onboarding-task-card-stub" />'
  }
}))

const source = () => readFileSync(resolve(process.cwd(), 'src/views/DashboardView.vue'), 'utf8')

const mountView = async () => {
  const wrapper = mount(DashboardView, {
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a><slot /></a>'
        }
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getResumeHistory.mockResolvedValue({
      data: {
        list: [
          { taskId: 'r1', fileUrl: 'resume-a.pdf', status: 2, createTime: '2026-05-20T10:00:00' }
        ]
      }
    })
    getInterviewHistory.mockResolvedValue({
      data: {
        list: [
          { sessionId: 'i1', jobRole: 'frontend', status: 1, score: 82, createTime: '2026-05-21T10:00:00' }
        ]
      }
    })
    getMonthlyStats.mockResolvedValue({
      data: {
        resumeCountThisMonth: 6,
        interviewCountThisMonth: 3
      }
    })
    getOnboardingTasks.mockResolvedValue({
      data: {
        tasks: [],
        completedCount: 0,
        totalCount: 4,
        visible: false,
        allCompleted: true
      }
    })
  })

  it('renders the profile workbench dashboard structure', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('.dashboard-view').exists()).toBe(true)
    expect(wrapper.find('.profile-workbench').exists()).toBe(true)
    expect(wrapper.find('.quota-overview').exists()).toBe(true)
    expect(wrapper.find('.stats-section').exists()).toBe(true)
    expect(wrapper.find('.growth-entry-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('最近简历诊断')
    expect(wrapper.text()).toContain('最近模拟面试')
  })

  it('uses enlarged unframed icons for quota and monthly stats while preserving record icons', async () => {
    const wrapper = await mountView()

    expect(wrapper.findAll('.stat-icon .feature-icon.size-lg')).toHaveLength(4)
    expect(wrapper.findAll('.quota-icon.size-lg').length).toBeGreaterThanOrEqual(2)
    expect(wrapper.find('.growth-entry-icon .feature-icon.size-lg').exists()).toBe(true)
    expect(wrapper.findAll('.record-left .feature-icon.size-sm').length).toBeGreaterThan(0)
    expect(wrapper.find('.view-all-btn .arrow-icon.size-md').exists()).toBe(true)
    expect(wrapper.find('.vip-badge .vip-icon.size-sm').exists()).toBe(true)
  })

  it('keeps view-all actions as lightweight text controls instead of Naive buttons', async () => {
    const wrapper = await mountView()
    const viewAllActions = wrapper.findAll('.view-all-btn')

    expect(viewAllActions).toHaveLength(2)
    viewAllActions.forEach((action) => {
      expect(action.element.tagName).toBe('BUTTON')
      expect(action.classes()).not.toContain('n-button')
    })
  })

  it('uses link semantics for dashboard navigation cards so nested text keeps pointer cursor', () => {
    const viewSource = source()

    expect(viewSource).toContain('<router-link to="/growth" class="growth-entry-card">')
    expect(viewSource).not.toContain('<div class="growth-entry-card" @click="router.push')
    expect(viewSource).toContain(':is="record.status === 2 ? \'router-link\' : \'div\'"')
    expect(viewSource).toContain(':to="record.status === 2 ? `/resume/result/${record.taskId}` : undefined"')
    expect(viewSource).toContain(':is="record.status === 1 ? \'router-link\' : \'div\'"')
    expect(viewSource).toContain(':to="record.status === 1 ? `/interview/report/${record.sessionId}` : undefined"')
  })

  it('renders recent interview score when the score is zero', async () => {
    getInterviewHistory.mockResolvedValueOnce({
      data: {
        list: [
          { sessionId: 'i-zero', jobRole: 'frontend', status: 1, score: 0, createTime: '2026-05-21T10:00:00' }
        ]
      }
    })

    const wrapper = await mountView()

    const scoreTag = wrapper.find('.record-score-tag')
    expect(scoreTag.exists()).toBe(true)
    expect(scoreTag.find('.score-value').text()).toBe('0')
  })

  it('keeps dashboard motion constrained and removes hard icon frames from highlighted metrics', () => {
    const viewSource = source()

    expect(viewSource).toContain('profile-workbench')
    expect(viewSource).toContain('quota-overview')
    expect(viewSource).toContain('@media (prefers-reduced-motion: reduce)')
    expect(viewSource).not.toContain('transition: all')
    expect(viewSource).not.toMatch(/\.stat-icon\s*\{[^}]*background:/)
    expect(viewSource).not.toMatch(/\.quota-icon-wrap\s*\{[^}]*background:/)
  })
})
