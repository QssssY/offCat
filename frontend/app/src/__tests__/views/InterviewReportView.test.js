import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus from 'element-plus'
import InterviewReportView from '@/views/interview/InterviewReportView.vue'
import { getInterviewSession, getInterviewSessionStatus } from '@/api/interview'

const push = vi.fn()
const elMessageSuccess = vi.hoisted(() => vi.fn())

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()

  return {
    ...actual,
    ElMessage: {
      ...actual.ElMessage,
      success: elMessageSuccess,
    },
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
  }),
  useRoute: () => ({
    params: {
      sessionId: 'session-1',
    },
  }),
}))

vi.mock('@/api/interview', () => ({
  getInterviewSession: vi.fn(),
  getInterviewSessionStatus: vi.fn(),
}))

vi.mock('@/api/onboarding', () => ({
  completeOnboardingTask: vi.fn(() => Promise.resolve()),
}))

vi.mock('@/components/resume/RadarChart.vue', () => ({
  default: {
    name: 'RadarChart',
    template: '<div class="radar-chart-stub" />',
  },
}))

vi.mock('@/components/resume/RadarScorePanel.vue', () => ({
  default: {
    name: 'RadarScorePanel',
    template: '<div class="radar-score-panel-stub" />',
  },
}))

vi.mock('@/components/common/AiLoadingState.vue', () => ({
  default: {
    name: 'AiLoadingState',
    template: '<div class="ai-loading-state-stub"><slot name="actions" /></div>',
  },
}))

vi.mock('@/components/community/ShareReportDialog.vue', () => ({
  default: {
    name: 'ShareReportDialog',
    props: ['visible', 'sessionData'],
    emits: ['update:visible', 'success'],
    template: '<div class="share-report-dialog-stub" />',
  },
}))

const report = {
  level: 'A',
  summary: 'Structured report summary for the candidate.',
  strengths: ['Clear project story'],
  weaknesses: ['Needs sharper tradeoff analysis'],
  missingCompetencies: ['Needs sharper tradeoff analysis'],
  followUpLossPoints: ['Follow-up answers were too short', 'Follow-up answers were too short'],
  commonLossPatterns: ['Missed specific examples', 'Missed specific examples'],
  improvementSuggestions: [
    'Practice a two-minute architecture answer',
    'Practice a two-minute architecture answer',
  ],
  suggestions: ['Practice a two-minute architecture answer'],
  immediateActions: [
    'Rewrite the project intro with result first.',
    'Prepare one performance optimization case.',
    'Record a two-minute mock answer.',
  ],
  technicalDepth: { score: 82, comment: 'Solid fundamentals' },
  projectExpression: { score: 85, comment: 'Good structure' },
  communication: { score: 80, comment: 'Clear enough' },
  problemSolving: { score: 78, comment: 'Can explain alternatives' },
  pressureResistance: { score: 76, comment: 'Stable' },
  jobMatch: { score: 84, comment: 'Matches frontend role' },
  questionPerformance: [
    {
      question: 'How do you optimize rendering?',
      answer: 'I profile first and then reduce unnecessary updates.',
      score: 82,
      comment: 'Good answer',
      knowledgeTags: ['Vue'],
    },
  ],
  roundReviews: [
    {
      roundNo: 1,
      question: 'How do you optimize rendering?',
      answer: 'I profile first and then reduce unnecessary updates.',
      score: 82,
      replayAnalysis: 'Good answer',
      missedFollowUp: 'Follow-up answers were too short',
      nextPractice: 'Practice with one measurable example',
    },
    {
      roundNo: 1,
      question: 'How do you optimize rendering?',
      answer: 'I profile first and then reduce unnecessary updates.',
      score: 82,
      replayAnalysis: 'Good answer',
      missedFollowUp: 'Follow-up answers were too short',
      nextPractice: 'Practice with one measurable example',
    },
  ],
}

const session = {
  id: 'session-1',
  status: 1,
  difficulty: 2,
  interviewMode: 'mock',
  jobRole: 'Frontend Engineer',
  comprehensiveScore: 86,
  evaluationReport: report,
  replayRounds: [
    {
      roundNo: 1,
      answerMessageId: 'answer-1',
      questionContent: 'Tell me about your project.',
      answerContent: 'I led the frontend module.',
      feedbackContent: 'Follow up on metrics.',
    },
  ],
}

const mountView = async () => {
  getInterviewSession.mockResolvedValue({ data: session })
  const wrapper = mount(InterviewReportView, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        FeatureIcon: {
          props: ['name'],
          template: '<span class="feature-icon-stub">{{ name }}</span>',
        },
      },
    },
  })
  await flushPromises()
  return wrapper
}

const viewSource = () =>
  readFileSync(
    resolve(process.cwd(), 'src/views/interview/InterviewReportView.vue'),
    'utf8'
  )

afterEach(() => {
  vi.clearAllMocks()
  vi.useRealTimers()
})

describe('InterviewReportView', () => {
  it('renders the interview report as a review workspace', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('.interview-report-shell').exists()).toBe(true)
    expect(wrapper.find('.report-hero-shell').exists()).toBe(true)
    expect(wrapper.find('.report-score-panel').exists()).toBe(true)
    expect(wrapper.find('.report-summary-panel').exists()).toBe(true)
    expect(wrapper.find('.report-priority-grid').exists()).toBe(true)
    expect(wrapper.find('.report-section-grid').exists()).toBe(true)
    expect(wrapper.find('.report-diagnosis-stack').exists()).toBe(true)
    expect(wrapper.find('.dimension-detail-section').exists()).toBe(true)
    expect(wrapper.find('.round-review-list').exists()).toBe(false)
    expect(wrapper.text()).toContain('86')
    expect(wrapper.text()).toContain('Frontend Engineer')
    expect(wrapper.text()).toContain(report.summary)
    expect(wrapper.findAll('.action-plan-item')).toHaveLength(3)
  })

  it('polls lightweight status before reloading full report details', async () => {
    vi.useFakeTimers()
    getInterviewSession
      .mockResolvedValueOnce({ data: { ...session, evaluationReport: null, comprehensiveScore: null } })
      .mockResolvedValueOnce({ data: session })
    getInterviewSessionStatus.mockResolvedValueOnce({
      data: {
        sessionId: 'session-1',
        status: 1,
        openingPending: false,
        reportReady: true,
        comprehensiveScore: 86,
      },
    })

    mount(InterviewReportView, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          FeatureIcon: {
            props: ['name'],
            template: '<span class="feature-icon-stub">{{ name }}</span>',
          },
        },
      },
    })
    await flushPromises()

    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()

    expect(getInterviewSessionStatus).toHaveBeenCalledWith('session-1')
    expect(getInterviewSession).toHaveBeenCalledTimes(2)
  })

  it('keeps polling when report status is ready but full detail reload fails once', async () => {
    vi.useFakeTimers()
    getInterviewSession
      .mockResolvedValueOnce({ data: { ...session, evaluationReport: null, comprehensiveScore: null } })
      .mockRejectedValueOnce(new Error('temporary detail failure'))
      .mockResolvedValueOnce({ data: session })
    getInterviewSessionStatus
      .mockResolvedValueOnce({
        data: {
          sessionId: 'session-1',
          status: 1,
          openingPending: false,
          reportReady: true,
          comprehensiveScore: 86,
        },
      })
      .mockResolvedValueOnce({
        data: {
          sessionId: 'session-1',
          status: 1,
          openingPending: false,
          reportReady: true,
          comprehensiveScore: 86,
        },
      })

    mount(InterviewReportView, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          FeatureIcon: {
            props: ['name'],
            template: '<span class="feature-icon-stub">{{ name }}</span>',
          },
        },
      },
    })
    await flushPromises()

    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()

    expect(getInterviewSessionStatus).toHaveBeenCalledTimes(1)
    expect(getInterviewSession).toHaveBeenCalledTimes(2)
    expect(elMessageSuccess).not.toHaveBeenCalledWith('评估报告已生成')

    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()

    expect(getInterviewSessionStatus).toHaveBeenCalledTimes(2)
    expect(getInterviewSession).toHaveBeenCalledTimes(3)
    expect(elMessageSuccess).toHaveBeenCalledWith('评估报告已生成')
  })

  it('backs off long-running report status polling after the fast rounds', async () => {
    vi.useFakeTimers()
    getInterviewSession.mockResolvedValueOnce({
      data: { ...session, evaluationReport: null, comprehensiveScore: null },
    })
    getInterviewSessionStatus.mockResolvedValue({
      data: {
        sessionId: 'session-1',
        status: 1,
        openingPending: false,
        reportReady: false,
      },
    })

    const wrapper = mount(InterviewReportView, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          FeatureIcon: {
            props: ['name'],
            template: '<span class="feature-icon-stub">{{ name }}</span>',
          },
        },
      },
    })
    await flushPromises()

    for (let round = 1; round <= 6; round += 1) {
      await vi.advanceTimersByTimeAsync(3000)
      await flushPromises()
      expect(getInterviewSessionStatus).toHaveBeenCalledTimes(round)
    }

    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()
    expect(getInterviewSessionStatus).toHaveBeenCalledTimes(6)

    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()
    expect(getInterviewSessionStatus).toHaveBeenCalledTimes(7)

    wrapper.unmount()
  })

  it('keeps existing report actions reachable', async () => {
    const wrapper = await mountView()

    const actionText = wrapper.find('.action-section').text()
    expect(actionText).toContain('返回')
    expect(actionText).toContain('会话')
    expect(actionText).toContain('分享')
    expect(actionText).toContain('再来')
  })

  it('keeps share success toast owned by the share dialog instead of showing a second parent toast', async () => {
    const wrapper = await mountView()

    wrapper.findComponent({ name: 'ShareReportDialog' }).vm.$emit('success')

    expect(elMessageSuccess).not.toHaveBeenCalledWith('分享成功')
  })

  it('deduplicates repeated report list items from generated reports', async () => {
    const wrapper = await mountView()

    expect(wrapper.findAll('.suggestion-list .simple-list li')).toHaveLength(1)
    expect(wrapper.findAll('.weakness-list .simple-list li')).toHaveLength(1)
    expect(wrapper.findAll('.loss-pattern-column .simple-list li')).toHaveLength(2)
    expect(wrapper.text().match(/Practice a two-minute architecture answer/g)).toHaveLength(1)
    expect(wrapper.text().match(/Needs sharper tradeoff analysis/g)).toHaveLength(1)
    expect(wrapper.text().match(/Follow-up answers were too short/g)).toHaveLength(1)
  })

  it('opens round reviews without duplicating repeated generated review rows', async () => {
    const wrapper = await mountView()

    await wrapper.findAll('.section-toggle')[1].trigger('click')

    expect(wrapper.find('.round-review-list').exists()).toBe(true)
    expect(wrapper.findAll('.round-review-item')).toHaveLength(1)
    expect(wrapper.findAll('.round-review-block')).toHaveLength(3)
  })

  it('keeps report dark mode and motion rules maintainable', () => {
    const source = viewSource()

    expect(source).toContain(':global(html[data-theme="dark"] .interview-report-view)')
    expect(source).toContain('@keyframes reportSurfaceIn')
    expect(source).toContain('@keyframes reportExpandIn')
    expect(source).toContain('.report-diagnosis-stack')
    expect(source).toContain(':global(html[data-theme="dark"]) :deep(.el-collapse-item__wrap)')
    expect(source).toContain('@media (prefers-reduced-motion: reduce)')
    expect(source).not.toMatch(/\n\[data-theme="dark"\]\s+\./)
    expect(source).not.toContain('transition: all')
    expect(source).not.toContain('border-left: 3px solid')
    expect(source).not.toContain('grid-template-columns: repeat(2, minmax(0, 1fr));')
    expect(source).not.toContain(':has(.radar-layout)')
    expect(source).toContain('grid-template-columns: minmax(0, 1fr);')
    expect(source).not.toContain('grid-auto-rows: 1fr;')
  })
})
