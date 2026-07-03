import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import GrowthCenterView from '@/views/growth/GrowthCenterView.vue'
import { getGrowthOverview, getInterviewRadar } from '@/api/growth'

vi.mock('@/api/growth', () => ({
  getGrowthOverview: vi.fn(() => Promise.resolve({
    data: {
      summary: {
        latestResumeScore: 82,
        latestInterviewScore: 76,
        resumeDiagnosisCount: 3,
        mockInterviewCount: 2
      },
      resumeScoreTrend: [{ date: '05/20', score: 82 }],
      interviewScoreTrend: [{ date: '05/21', score: 76 }],
      growthConfig: {
        encouragementMessages: ['继续打磨岗位匹配能力'],
        milestones: [
          {
            configKey: 'milestone_first_interview',
            title: '完成第一次模拟面试',
            description: '开始沉淀面试反馈',
            sort: 1
          }
        ]
      }
    }
  })),
  getInterviewRadar: vi.fn(() => Promise.resolve({ data: { sessionCount: 0 } }))
}))

let currentWrapper = null
const viewSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/growth/GrowthCenterView.vue'), 'utf8')

const mountView = () => {
  currentWrapper = mount(GrowthCenterView, {
    global: {
      plugins: [ElementPlus],
      mocks: {
        $router: { push: vi.fn() }
      },
      stubs: {
        LineChart: { template: '<div class="line-chart-stub"></div>' },
        RadarChart: { template: '<div class="radar-chart-stub"></div>' },
        RadarScorePanel: { template: '<div class="radar-panel-stub"></div>' }
      }
    }
  })
  return currentWrapper
}

describe('GrowthCenterView', () => {
  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
    vi.clearAllMocks()
  })

  it('renders overview data without blocking on radar details', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getGrowthOverview).toHaveBeenCalled()
    expect(getInterviewRadar).toHaveBeenCalled()
    expect(wrapper.text()).toContain('82')
    expect(wrapper.text()).toContain('76')
    expect(wrapper.text()).toContain('继续打磨岗位匹配能力')
    expect(wrapper.text()).toContain('完成第一次模拟面试')
  })

  it('lazy-loads chart surfaces while keeping stable chart placeholders', () => {
    const source = viewSource()

    expect(source).toContain('defineAsyncComponent')
    expect(source).toContain("const LineChart = defineAsyncComponent(() => import('@/components/resume/LineChart.vue'))")
    expect(source).toContain("const RadarChart = defineAsyncComponent(() => import('@/components/resume/RadarChart.vue'))")
    expect(source).toContain("const RadarScorePanel = defineAsyncComponent(() => import('@/components/resume/RadarScorePanel.vue'))")
    expect(source).toContain('.chart-card :deep(.line-chart-wrapper)')
    expect(source).toContain('min-height: 260px')
    expect(source).toContain('.radar-chart-col :deep(.radar-chart-wrapper)')
    expect(source).toContain('.radar-panel-col')
  })
})
