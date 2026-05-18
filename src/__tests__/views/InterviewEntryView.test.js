import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import InterviewEntryView from '@/views/interview/InterviewEntryView.vue'
import { getInterviewJobRoles } from '@/api/interview'
import { saveSettingsPreferences } from '@/utils/settingsPreferences'

const push = vi.fn()
let routeQuery = {}

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
    currentRoute: { value: { fullPath: '/interview/entry' } }
  }),
  useRoute: () => ({ query: routeQuery })
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({}))
}))

vi.mock('@/api/interview', () => ({
  createInterviewSession: vi.fn(() => Promise.resolve({ data: { sessionId: 'session-1' } })),
  getInterviewJobRoles: vi.fn(() => Promise.resolve({
    data: [
      { roleName: '前端工程师', roleCode: 'frontend', interviewTag: '热门' },
      { roleName: '后端工程师', roleCode: 'backend', interviewTag: '常规' }
    ]
  }))
}))

vi.mock('@/api/resume', () => ({
  getResumeTask: vi.fn(() => Promise.resolve({ data: {} }))
}))

const mountView = () => mount(InterviewEntryView, {
  global: {
    plugins: [ElementPlus]
  }
})

describe('InterviewEntryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    routeQuery = {}
  })

  it('fills interview entry with local default preferences', async () => {
    saveSettingsPreferences({
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate'
    })

    const wrapper = mountView()
    await flushPromises()

    expect(getInterviewJobRoles).toHaveBeenCalled()
    expect(wrapper.vm.selectedJob).toBe('前端工程师')
    expect(wrapper.vm.selectedRoleCode).toBe('frontend')
    expect(wrapper.vm.selectedDifficulty).toBe('advanced')
    expect(wrapper.vm.selectedMode).toBe('tech_leader')
    expect(wrapper.vm.selectedFeedbackMode).toBe('immediate')
  })

  it('lets route query override local default preferences', async () => {
    saveSettingsPreferences({
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'after_interview'
    })
    routeQuery = {
      jobRole: 'backend',
      difficulty: 'intermediate',
      mode: 'stress',
      feedbackMode: 'immediate'
    }

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.vm.selectedJob).toBe('后端工程师')
    expect(wrapper.vm.selectedRoleCode).toBe('backend')
    expect(wrapper.vm.selectedDifficulty).toBe('intermediate')
    expect(wrapper.vm.selectedMode).toBe('stress')
    expect(wrapper.vm.selectedFeedbackMode).toBe('immediate')
  })

  it('does not fill stale default job when it is no longer enabled', async () => {
    saveSettingsPreferences({
      defaultInterviewJobRole: '已下线岗位',
      defaultInterviewJobRoleCode: 'offline-role',
      defaultInterviewDifficulty: 'advanced'
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.vm.selectedJob).toBe('')
    expect(wrapper.vm.selectedRoleCode).toBe('')
    expect(wrapper.vm.selectedDifficulty).toBe('advanced')
  })
})
