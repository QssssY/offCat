import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import ResumeResultView from '@/views/resume/ResultView.vue'
import { getResumeTask, retryResumeTask } from '@/api/resume'

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
  retryResumeTask: vi.fn(),
  savePolishDocument: vi.fn(),
}))

vi.mock('@/utils/resumePdfPagination', () => ({
  createResumePdfImagePages: vi.fn(),
}))

const mountView = () => shallowMount(ResumeResultView, {
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

describe('ResumeResultView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.params.taskId = 'failed-task'
  })

  it('refetches task detail after retry navigates to the new task id', async () => {
    getResumeTask
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

    expect(getResumeTask).toHaveBeenNthCalledWith(1, 'failed-task')
    expect(wrapper.vm.task.taskId).toBe('failed-task')

    await wrapper.vm.handleRetry()
    await flushPromises()
    await flushPromises()

    expect(retryResumeTask).toHaveBeenCalledWith('failed-task')
    expect(replace).toHaveBeenCalledWith('/resume/result/new-task')
    expect(getResumeTask).toHaveBeenNthCalledWith(2, 'new-task')
    expect(wrapper.vm.task.taskId).toBe('new-task')
  })
})
