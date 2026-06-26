import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus from 'element-plus'
import InterviewEntryView from '@/views/interview/InterviewEntryView.vue'
import { createInterviewSession, getInterviewJobRoles } from '@/api/interview'
import { saveSettingsPreferences } from '@/utils/settingsPreferences'

const push = vi.fn()
let routeQuery = {}
const routePrefetchMocks = vi.hoisted(() => ({
  prefetchInterviewSessionRoute: vi.fn(() => Promise.resolve())
}))

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

vi.mock('@/router/routeLoaders', () => routePrefetchMocks)

const mountView = () => mount(InterviewEntryView, {
  global: {
    plugins: [ElementPlus]
  }
})

const viewSource = () => readFileSync(
  resolve(process.cwd(), 'src/views/interview/InterviewEntryView.vue'),
  'utf8'
)

describe('InterviewEntryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    routeQuery = {}
    window.SpeechRecognition = vi.fn()
    window.speechSynthesis = {
      getVoices: vi.fn(() => []),
    }
    routePrefetchMocks.prefetchInterviewSessionRoute.mockResolvedValue()
  })

  it('fills interview entry with local default preferences', async () => {
    saveSettingsPreferences({
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate',
      defaultInterviewInteractionType: 1
    })

    const wrapper = mountView()
    await flushPromises()

    expect(getInterviewJobRoles).toHaveBeenCalled()
    expect(wrapper.vm.selectedJob).toBe('前端工程师')
    expect(wrapper.vm.selectedRoleCode).toBe('frontend')
    expect(wrapper.vm.selectedDifficulty).toBe('advanced')
    expect(wrapper.vm.selectedMode).toBe('tech_leader')
    expect(wrapper.vm.selectedFeedbackMode).toBe('after_interview')
    expect(wrapper.vm.selectedInteractionType).toBe(1)
  })

  it('keeps text interaction when default voice preference is unsupported', async () => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    delete window.speechSynthesis
    saveSettingsPreferences({
      defaultInterviewInteractionType: 1
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.vm.selectedInteractionType).toBe(0)
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

  it('sends voice interaction type when voice mode is selected', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.selectedJob = '前端工程师'
    wrapper.vm.selectedRoleCode = 'frontend'
    wrapper.vm.selectInteractionType(1)
    await wrapper.vm.handleStart()
    await flushPromises()

    expect(createInterviewSession).toHaveBeenCalledWith(expect.objectContaining({
      interactionType: 1
    }))
  })

  it('prefetches the interview session route while creating a session', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.selectedJob = '鍓嶇宸ョ▼甯?'
    wrapper.vm.selectedRoleCode = 'frontend'
    await wrapper.vm.handleStart()
    await flushPromises()

    expect(routePrefetchMocks.prefetchInterviewSessionRoute).toHaveBeenCalledTimes(1)
    expect(routePrefetchMocks.prefetchInterviewSessionRoute.mock.invocationCallOrder[0]).toBeLessThan(
      createInterviewSession.mock.invocationCallOrder[0]
    )
    expect(push).toHaveBeenCalledWith('/interview/session/session-1')
  })

  it('switches voice interviews to after-interview feedback', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.selectedJob = '前端工程师'
    wrapper.vm.selectedRoleCode = 'frontend'
    wrapper.vm.selectedFeedbackMode = 'immediate'
    wrapper.vm.selectInteractionType(1)
    await wrapper.vm.handleStart()
    await flushPromises()

    expect(wrapper.vm.selectedFeedbackMode).toBe('after_interview')
    expect(createInterviewSession).toHaveBeenCalledWith(expect.objectContaining({
      interactionType: 1,
      feedbackMode: 'after_interview'
    }))
  })

  it('does not allow immediate feedback while voice interaction is selected', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.selectInteractionType(1)
    wrapper.vm.selectFeedbackMode('immediate')

    expect(wrapper.vm.selectedInteractionType).toBe(1)
    expect(wrapper.vm.selectedFeedbackMode).toBe('after_interview')
  })

  it('keeps text interaction type when speech api is unsupported', async () => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    delete window.speechSynthesis

    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.selectInteractionType(1)

    expect(wrapper.vm.selectedInteractionType).toBe(0)
  })

  it('keeps the ready hint friendly with a larger centered microphone icon', () => {
    const source = viewSource()
    const readyBarBlock = source.match(/\.ready-bar\s*\{[\s\S]*?\n\}/)?.[0] || ''
    const readyIconBlock = source.match(/\.ready-icon\s*\{[\s\S]*?\n\}/)?.[0] || ''

    expect(source).toContain('<FeatureIcon name="microphone-on" size="md"')
    expect(source).toContain('.ready-icon :deep(.feature-icon)')
    expect(readyBarBlock).toContain('--interview-ready-bg')
    expect(readyBarBlock).toContain('--interview-ready-border')
    expect(readyBarBlock).not.toContain('border: 1px solid var(--orange-border)')
    expect(readyIconBlock).toContain('width: 52px;')
    expect(readyIconBlock).toContain('height: 52px;')
  })
})
