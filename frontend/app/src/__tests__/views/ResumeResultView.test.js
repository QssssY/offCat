import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { reactive } from 'vue'
import ResumeResultView from '@/views/resume/ResultView.vue'
import { getResumeTask, getResumeTaskStatus, retryResumeTask } from '@/api/resume'

const push = vi.fn()
const replace = vi.fn(async (path) => {
  routeState.params.taskId = String(path).split('/').pop()
})
const routeState = reactive({
  params: {
    taskId: 'failed-task',
  },
})

const message = {
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    push: vi.fn(),
  })),
  createWebHistory: vi.fn(),
  useRouter: () => ({
    push,
    replace,
    currentRoute: { value: { fullPath: '/resume/result/failed-task' } },
  }),
  useRoute: () => routeState,
  onBeforeRouteLeave: vi.fn(),
}))

vi.mock('naive-ui', () => ({
  NButton: {
    name: 'NButton',
    props: ['loading', 'type', 'ghost', 'secondary', 'quaternary', 'size'],
    emits: ['click'],
    template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
  },
  NInput: {
    name: 'NInput',
    props: ['value'],
    emits: ['update:value'],
    template: '<textarea />',
  },
  useMessage: () => message,
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    fetchUserInfo: vi.fn(() => Promise.resolve()),
  }),
}))

vi.mock('@/api/resume', () => ({
  analyzeResumeJobMatch: vi.fn(),
  analyzeResumePolish: vi.fn(),
  getResumeTask: vi.fn(),
  getResumeTaskStatus: vi.fn(),
  retryResumeTask: vi.fn(),
  savePolishDocument: vi.fn(),
}))

vi.mock('@/api/onboarding', () => ({
  completeOnboardingTask: vi.fn(() => Promise.resolve()),
}))

vi.mock('@/utils/resumePdfPagination', () => ({
  createResumePdfImagePages: vi.fn(),
}))

vi.mock('html2canvas', () => ({
  default: vi.fn(() => Promise.resolve({
    width: 100,
    height: 100,
    toBlob: (callback) => callback(new Blob(['png'], { type: 'image/png' })),
  })),
}))

const mountedWrappers = []

const mountView = () => {
  const wrapper = shallowMount(ResumeResultView, {
    global: {
      stubs: {
        AiLoadingState: true,
        OverallEvaluation: true,
        HighlightsSection: true,
        SkillsSection: true,
        WorkExperienceSection: true,
        RadarChart: true,
        RadarScorePanel: true,
        ResumeTemplate: true,
      },
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

const viewSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/resume/ResultView.vue'), 'utf8')

describe('ResumeResultView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.params.taskId = 'failed-task'
  })

  afterEach(() => {
    mountedWrappers.splice(0).forEach((wrapper) => wrapper.unmount())
    vi.useRealTimers()
  })

  it('refetches task detail after retry navigates to the new task id', async () => {
    getResumeTaskStatus
      .mockResolvedValueOnce({
        data: {
          taskId: 'failed-task',
          status: 3,
          errorMsg: 'old error',
        },
      })
      .mockResolvedValueOnce({
        data: {
          taskId: 'new-task',
          status: 0,
        },
      })
    retryResumeTask.mockResolvedValue({ data: 'new-task' })

    const wrapper = mountView()
    await flushPromises()

    expect(getResumeTaskStatus).toHaveBeenNthCalledWith(1, 'failed-task')
    expect(wrapper.vm.task.taskId).toBe('failed-task')

    await wrapper.vm.handleRetry()
    await flushPromises()
    await flushPromises()

    expect(retryResumeTask).toHaveBeenCalledWith('failed-task')
    expect(replace).toHaveBeenCalledWith('/resume/result/new-task')
    expect(getResumeTaskStatus).toHaveBeenNthCalledWith(2, 'new-task')
    expect(wrapper.vm.task.taskId).toBe('new-task')
    expect(getResumeTask).not.toHaveBeenCalled()
  })

  it('revokes image object URL when download click throws', async () => {
    getResumeTaskStatus.mockResolvedValue({
      data: {
        taskId: 'completed-task',
        status: 2,
      },
    })
    getResumeTask.mockResolvedValue({
      data: {
        taskId: 'completed-task',
        status: 2,
        result: '{"overallEvaluation":{"totalScore":80}}',
      },
    })
    routeState.params.taskId = 'completed-task'
    globalThis.requestAnimationFrame = vi.fn((callback) => callback())
    const clickError = new Error('download blocked')
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:image-url')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    vi.spyOn(document, 'createElement').mockImplementation(function (tag) {
      if (tag === 'a') return { href: '', download: '', click: vi.fn(() => { throw clickError }) }
      return Document.prototype.createElement.call(document, tag)
    })

    const wrapper = mountView()
    await flushPromises()
    wrapper.vm.resumeTemplateRef = {
      buildExportElement: () => document.createElement('div'),
      getResumeName: () => 'resume',
    }

    await wrapper.vm.exportResumeImage()

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:image-url')
    expect(message.error).toHaveBeenCalled()
  })

  it('renders local feature icons on job match analysis and AI polish action buttons', () => {
    const source = viewSource()

    expect(source).toContain('<FeatureIcon name="job-match-analysis" size="xs" class="button-feature-icon" />')
    expect(source).toContain('<FeatureIcon name="resume-optimization" size="xs" class="button-feature-icon" />')
  })

  it('renders the polish template when saved document data exists without original polished text', async () => {
    getResumeTaskStatus.mockResolvedValue({
      data: {
        taskId: 'completed-task',
        status: 2,
      },
    })
    getResumeTask.mockResolvedValue({
      data: {
        taskId: 'completed-task',
        status: 2,
        result: '{"overallEvaluation":{"totalScore":80}}',
        latestPolishResult: {
          polishRecordId: 'polish-1',
          resumeTaskId: 'completed-task',
          polishedResumeText: '',
          documentJson: '{"header":{"sectionTitle":"个人信息"},"sections":[]}',
          editedPlainText: '编辑后的简历内容',
          modificationNotes: ['优化项目描述'],
        },
      },
    })
    routeState.params.taskId = 'completed-task'

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.polish-preview-shell').exists()).toBe(true)
  })

  it('polls lightweight status while waiting and loads full detail only after completion', async () => {
    vi.useFakeTimers()
    routeState.params.taskId = 'pending-task'
    getResumeTaskStatus
      .mockResolvedValueOnce({
        data: {
          taskId: 'pending-task',
          status: 0,
          statusDesc: '排队中',
        },
      })
      .mockResolvedValueOnce({
        data: {
          taskId: 'pending-task',
          status: 1,
          stage: 'ai_analyzing',
        },
      })
      .mockResolvedValueOnce({
        data: {
          taskId: 'pending-task',
          status: 2,
        },
      })
    getResumeTask.mockResolvedValue({
      data: {
        taskId: 'pending-task',
        status: 2,
        diagnosisResult: '{"overallEvaluation":{"totalScore":82}}',
      },
    })

    mountView()
    await flushPromises()

    expect(getResumeTaskStatus).toHaveBeenCalledTimes(1)
    expect(getResumeTask).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()
    expect(getResumeTaskStatus).toHaveBeenCalledTimes(2)
    expect(getResumeTask).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(5999)
    await flushPromises()
    expect(getResumeTaskStatus).toHaveBeenCalledTimes(2)
    expect(getResumeTask).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1)
    await flushPromises()
    expect(getResumeTaskStatus).toHaveBeenCalledTimes(3)
    expect(getResumeTask).toHaveBeenCalledTimes(1)
    expect(getResumeTask).toHaveBeenCalledWith('pending-task')

    vi.useRealTimers()
  })

  it('does not statically import export helpers into the initial result page chunk', () => {
    const source = viewSource()

    expect(source).not.toContain("import { createResumePdfImagePages } from '@/utils/resumePdfPagination'")
    expect(source).not.toContain("import { exportResumeToDocx } from '@/utils/resumeDocxExport'")
    expect(source).toContain("await import('@/utils/resumePdfPagination')")
    expect(source).toContain("await import('@/utils/resumeDocxExport')")
  })

  it('does not infer education score from total score when education dimension is missing', () => {
    const source = viewSource()

    expect(source).not.toContain('computeEducationFallback')
    expect(source).toContain('return parsedDiagnosisResult.value?.educationEvaluation?.score || 0')
    expect(source).toContain('education: result.educationEvaluation?.score || 0')
  })
})
