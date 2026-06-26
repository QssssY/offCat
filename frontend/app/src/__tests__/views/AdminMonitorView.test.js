import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import AdminMonitorView from '@/views/admin/AdminMonitorView.vue'
import { getAdminMonitorOverview } from '@/api/admin/monitor'

let currentWrapper = null

vi.mock('@/api/admin/monitor', () => ({
  getAdminMonitorOverview: vi.fn()
}))

const fullMonitorData = {
  pendingResumeTaskCount: 1,
  processingResumeTaskCount: 2,
  failedResumeTaskCount: 3,
  completedResumeTaskCount: 4,
  activeInterviewSessionCount: 5,
  todayInterviewSessionCount: 6,
  todayResumeDiagnosisCount: 7,
  todayResumePolishCount: 8,
  todayJobMatchCount: 9,
  todayCommunityPostCount: 10,
  pendingFeedbackCount: 11,
  processingFeedbackCount: 12,
  todayFeedbackCount: 13,
  pendingCommunityPostCount: 14,
  pendingCommunityCommentCount: 15,
  pendingCommunityReviewCount: 29,
  todayOrderCount: 16
}

const mountView = async () => {
  currentWrapper = mount(AdminMonitorView, {
    attachTo: document.body,
    global: {
      plugins: [ElementPlus],
      stubs: {
        transition: false
      }
    }
  })
  await flushPromises()
  return currentWrapper
}

describe('AdminMonitorView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAdminMonitorOverview.mockResolvedValue({ data: fullMonitorData })
  })

  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
  })

  it('renders grouped business monitor metrics from overview api', async () => {
    const wrapper = await mountView()
    const text = wrapper.text()

    expect(text).toContain('简历任务运行态')
    expect(text).toContain('今日业务量')
    expect(text).toContain('待处理事项')
    expect(text).toContain('已完成简历任务')
    expect(text).toContain('AI 简历润色')
    expect(text).toContain('JD 匹配分析')
    expect(text).toContain('社区发帖')
    expect(text).toContain('用户反馈')
    expect(text).toContain('今日订单')
    expect(text).toContain('社区待审总数')
    expect(text).toContain('待审帖子 14')
    expect(text).toContain('待审评论 15')
  })

  it('uses a fixed four-column desktop grid for monitor metric sections', async () => {
    const wrapper = await mountView()
    const todaySection = wrapper
      .findAll('.monitor-section')
      .find((section) => section.find('.section-title').text() === '今日业务量')

    expect(todaySection.exists()).toBe(true)
    expect(todaySection.find('.monitor-grid').classes()).toContain('monitor-grid--four')
  })

  it('keeps empty state when all monitor metrics are zero', async () => {
    getAdminMonitorOverview.mockResolvedValue({
      data: Object.fromEntries(Object.keys(fullMonitorData).map((key) => [key, 0]))
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('当前暂无监控数据，请稍后刷新或等待业务请求产生。')
  })

  it('shows an error message when monitor overview loading fails', async () => {
    getAdminMonitorOverview.mockRejectedValue(new Error('监控接口异常'))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('监控接口异常')
  })
})
