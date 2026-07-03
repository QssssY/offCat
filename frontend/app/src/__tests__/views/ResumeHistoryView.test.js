import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ResumeHistoryView from '@/views/resume/HistoryView.vue'
import { getResumeHistory, retryResumeTask } from '@/api/resume'
import { ElMessage } from 'element-plus'
import { prefetchUserRoute } from '@/router/routeLoaders'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}))

vi.mock('@/api/resume', () => ({
  clearResumeHistory: vi.fn(),
  deleteResumeHistory: vi.fn(),
  extractFileName: vi.fn((url) => url),
  getResumeHistory: vi.fn(),
  retryResumeTask: vi.fn(),
}))

vi.mock('@/router/routeLoaders', () => ({
  prefetchUserRoute: vi.fn(() => Promise.resolve()),
}))

vi.mock('element-plus', () => ({
  ElButton: {
    template: '<button><slot /></button>',
  },
  ElIcon: {
    template: '<span><slot /></span>',
  },
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
  ElMessageBox: {
    confirm: vi.fn(),
  },
  ElPagination: {
    template: '<nav />',
  },
  ElSkeleton: {
    template: '<div><slot /></div>',
  },
  ElSkeletonItem: {
    template: '<span />',
  },
  ElTag: {
    template: '<span><slot /></span>',
  },
}))

const mountView = () => shallowMount(ResumeHistoryView, {
  global: {
    stubs: {
      CircleClose: true,
      Clock: true,
      Delete: true,
      Document: true,
      ElButton: true,
      ElIcon: true,
      ElPagination: true,
      ElSkeleton: true,
      ElSkeletonItem: true,
      ElTag: true,
      RefreshRight: true,
      ResumeEmpty: true,
    },
  },
})

describe('ResumeHistoryView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getResumeHistory.mockResolvedValue({
      data: {
        records: [],
        total: 0,
        pages: 0,
        hasNext: false,
        hasPrevious: false,
      },
    })
  })

  it('prefetches the resume result chunk before opening a report', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.viewResult({ taskId: 'task-1' })

    expect(prefetchUserRoute).toHaveBeenCalledWith('/resume/result')
    expect(push).toHaveBeenCalledWith('/resume/result/task-1')
  })

  it('does not navigate when retry response has no new task id', async () => {
    retryResumeTask.mockResolvedValue({ data: '' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.vm.handleRetry({ taskId: 'failed-task' })
    await flushPromises()

    expect(retryResumeTask).toHaveBeenCalledWith('failed-task')
    expect(ElMessage.error).toHaveBeenCalledWith('重试响应异常，请稍后重试')
    expect(push).not.toHaveBeenCalled()
  })

  it('prefetches the resume result chunk before navigating to a retry task', async () => {
    retryResumeTask.mockResolvedValue({ data: 'new-task' })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.vm.handleRetry({ taskId: 'failed-task' })
    await flushPromises()

    expect(prefetchUserRoute).toHaveBeenCalledWith('/resume/result')
    expect(push).toHaveBeenCalledWith('/resume/result/new-task')
  })
})
