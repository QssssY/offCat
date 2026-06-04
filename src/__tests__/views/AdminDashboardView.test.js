import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminDashboardView from '@/views/admin/AdminDashboardView.vue'
import {
  getAdminDashboardBusinessDistribution,
  getAdminDashboardHotJobRoles,
  getAdminDashboardOverview,
  getAdminDashboardSummary,
  getAdminDashboardTrends
} from '@/api/admin/dashboard'

vi.mock('vue-chartjs', () => ({
  Line: { name: 'Line', template: '<div class="line-chart" />' },
  Bar: { name: 'Bar', template: '<div class="bar-chart" />' },
  Doughnut: { name: 'Doughnut', template: '<div class="doughnut-chart" />' }
}))

vi.mock('chart.js', () => ({
  Chart: { register: vi.fn() },
  Title: {},
  Tooltip: {},
  Legend: {},
  CategoryScale: {},
  LinearScale: {},
  LineElement: {},
  PointElement: {},
  BarElement: {},
  ArcElement: {},
  Filler: {}
}))

vi.mock('@element-plus/icons-vue', () => ({
  Refresh: { name: 'Refresh', template: '<span />' }
}))

vi.mock('element-plus', () => ({
  ElAlert: { name: 'ElAlert', template: '<div class="el-alert-stub" />' },
  ElButton: { name: 'ElButton', template: '<button><slot /></button>' },
  ElDatePicker: { name: 'ElDatePicker', template: '<input />' },
  ElIcon: { name: 'ElIcon', template: '<span><slot /></span>' },
  ElInputNumber: { name: 'ElInputNumber', template: '<input />' },
  ElMessage: {
    warning: vi.fn()
  },
  ElRadioButton: { name: 'ElRadioButton', template: '<button><slot /></button>' },
  ElRadioGroup: { name: 'ElRadioGroup', template: '<div><slot /></div>' }
}))

vi.mock('@/api/admin/dashboard', () => ({
  getAdminDashboardSummary: vi.fn(),
  getAdminDashboardOverview: vi.fn(),
  getAdminDashboardTrends: vi.fn(),
  getAdminDashboardHotJobRoles: vi.fn(),
  getAdminDashboardBusinessDistribution: vi.fn()
}))

const mountView = async () => {
  const wrapper = shallowMount(AdminDashboardView, {
    global: {
      stubs: {
        'el-button': { template: '<button><slot /></button>' },
        'el-icon': { template: '<span><slot /></span>' },
        'el-radio-group': { template: '<div><slot /></div>' },
        'el-radio-button': { template: '<button><slot /></button>' },
        'el-date-picker': { template: '<input />' },
        'el-input-number': { template: '<input />' },
        'el-alert': { template: '<div class="el-alert-stub" />' }
      }
    }
  })
  await flushPromises()
  return wrapper
}

describe('AdminDashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAdminDashboardSummary.mockResolvedValue({
      data: {
        overview: {
          totalUserCount: 12,
          vipUserCount: 2,
          activePromptCount: 3,
          activeJobRoleCount: 4,
          activeAiEngineCount: 5,
          todayInterviewSessionCount: 6,
          todayResumeDiagnosisCount: 7,
          feedbackCount: 8,
          communityPostCount: 9,
          resumePolishCount: 10,
          jdMatchCount: 11,
          orderCount: 12,
          orderRevenue: 99.5
        },
        trends: [
          { date: '2026-06-01', interviewSessionCount: 1, resumeDiagnosisCount: 2, orderCount: 3, orderRevenue: 4 }
        ],
        hotJobRoles: [
          { jobRole: 'Java 后端', sessionCount: 5 }
        ],
        businessDistribution: {
          startDate: '2026-06-01',
          endDate: '2026-06-07',
          interviewCount: 1,
          resumeCount: 2,
          resumePolishCount: 3,
          jdMatchCount: 4,
          communityPostCount: 5,
          totalCount: 15
        }
      }
    })
  })

  it('loads admin dashboard with a single summary request on mount', async () => {
    const wrapper = await mountView()

    expect(getAdminDashboardSummary).toHaveBeenCalledTimes(1)
    expect(getAdminDashboardSummary).toHaveBeenCalledWith(expect.objectContaining({ limit: 10 }))
    expect(getAdminDashboardOverview).not.toHaveBeenCalled()
    expect(getAdminDashboardTrends).not.toHaveBeenCalled()
    expect(getAdminDashboardHotJobRoles).not.toHaveBeenCalled()
    expect(getAdminDashboardBusinessDistribution).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('12')
    expect(wrapper.vm.hotJobRoles[0].jobRole).toBe('Java 后端')
  })
})
