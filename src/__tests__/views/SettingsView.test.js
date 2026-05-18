import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import SettingsView from '@/views/settings/SettingsView.vue'
import { getGrowthOverview } from '@/api/growth'
import { clearInterviewHistory, getInterviewJobRoles } from '@/api/interview'
import { getMembershipPlans } from '@/api/membership'
import { clearResumeHistory } from '@/api/resume'
import { deleteAccount } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { getSettingsPreferences, saveSettingsPreferences } from '@/utils/settingsPreferences'

const push = vi.fn()
const fetchUserInfo = vi.fn(() => Promise.resolve())
const clearUserInfo = vi.fn()
const setTheme = vi.fn()
const setFollowSystem = vi.fn()
let currentWrapper = null

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

vi.mock('@/api/auth', () => ({
  deleteAccount: vi.fn(() => Promise.resolve()),
  updatePassword: vi.fn(() => Promise.resolve()),
  updateSecurityQuestion: vi.fn(() => Promise.resolve())
}))

vi.mock('@/api/membership', () => ({
  getMembershipPlans: vi.fn(() => Promise.resolve({
    data: [
      { planCode: 'vip_month', planName: 'Monthly VIP' }
    ]
  }))
}))

vi.mock('@/api/interview', () => ({
  clearInterviewHistory: vi.fn(() => Promise.resolve({ data: { deletedCount: 2 } })),
  getInterviewJobRoles: vi.fn(() => Promise.resolve({
    data: [
      { roleName: '前端工程师', roleCode: 'frontend', interviewTag: '热门' },
      { roleName: '后端工程师', roleCode: 'backend', interviewTag: '常规' }
    ]
  }))
}))

vi.mock('@/api/resume', () => ({
  clearResumeHistory: vi.fn(() => Promise.resolve({ data: { deletedCount: 3 } }))
}))

vi.mock('@/api/growth', () => ({
  getGrowthOverview: vi.fn(() => Promise.resolve({
    data: {
      summary: {
        resumeDiagnosisCount: 7,
        mockInterviewCount: 5,
        jobMatchCount: 3,
        polishCount: 2
      }
    }
  }))
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

vi.mock('@/stores/theme', () => ({
  useThemeStore: vi.fn()
}))

vi.mock('@/components/OnboardingGuide.vue', () => ({
  default: {
    name: 'OnboardingGuide',
    template: '<div class="onboarding-guide-stub"></div>'
  }
}))

const mountView = () => {
  currentWrapper = mount(SettingsView, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        transition: false
      }
    }
  })
  return currentWrapper
}

const waitForSecurityTransition = () => new Promise((resolve) => setTimeout(resolve, 220))

describe('SettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    useUserStore.mockReturnValue({
      userInfo: {
        id: 1,
        username: 'tester',
        nickname: '测试用户',
        role: 0,
        status: 1,
        membershipPlanCode: 'vip_month',
        vipExpireTime: '2026-05-18T12:00:00',
        resumeQuota: 3,
        interviewQuota: 4,
        vipDailyResumeQuota: 5,
        vipDailyInterviewQuota: 6
      },
      isVip: () => true,
      fetchUserInfo,
      clearUserInfo
    })
    useThemeStore.mockReturnValue({
      followSystem: false,
      manualTheme: 'light',
      resolvedTheme: 'light',
      setTheme,
      setFollowSystem
    })
  })

  afterEach(() => {
    currentWrapper?.unmount()
    currentWrapper = null
  })

  it('renders all settings sections', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('设置中心')
    expect(wrapper.text()).toContain('账号资料')
    expect(wrapper.text()).toContain('面试偏好')
    expect(wrapper.text()).toContain('账号安全')
    expect(wrapper.text()).toContain('隐私与数据')
    expect(wrapper.text()).toContain('数据管理')
    expect(wrapper.text()).toContain('外观偏好')
    expect(wrapper.text()).toContain('通知偏好')
    expect(wrapper.text()).toContain('新手引导')
    expect(wrapper.text()).toContain('会员与额度')
  })

  it('shows subscription plan name without exposing internal identifiers', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getMembershipPlans).toHaveBeenCalled()
    expect(wrapper.text()).toContain('订阅套餐')
    expect(wrapper.text()).toContain('月度会员')
    expect(wrapper.text()).toContain('会员到期时间')
    expect(wrapper.text()).toContain('2026-05-18')
    expect(wrapper.text()).not.toContain('2026-05-18 12:00')
    expect(wrapper.text()).not.toContain('用户 ID')
    expect(wrapper.text()).not.toContain('vip_month')
  })

  it('keeps profile nickname read-only in settings center', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('测试用户')
    expect(wrapper.text()).not.toContain('保存昵称')
    expect(wrapper.find('input[autocomplete="nickname"]').exists()).toBe(false)
  })

  it('switches account security forms with one form visible at a time', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.vm.securityMode).toBe('password')
    expect(wrapper.find('input[autocomplete="new-password"]').exists()).toBe(true)
    expect(wrapper.find('.settings-form .el-select').exists()).toBe(false)

    wrapper.vm.handleSecurityModeChange('securityQuestion')
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.find('input[autocomplete="new-password"]').exists()).toBe(false)
    expect(wrapper.find('.settings-form .el-select').exists()).toBe(true)
  })

  it('does not repeat security section title inside the selected form', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.settings-form h3').exists()).toBe(false)
  })

  it('updates theme via theme store handlers', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('已保存到当前浏览器')
    wrapper.vm.handleThemeChange('dark')
    expect(wrapper.vm.themeChoice).toBe('dark')
    expect(setTheme).toHaveBeenCalledWith('dark')
  })

  it('saves local interview preferences from settings center', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getInterviewJobRoles).toHaveBeenCalled()
    wrapper.vm.interviewPreferenceForm.defaultInterviewDifficulty = 'advanced'
    wrapper.vm.interviewPreferenceForm.defaultInterviewMode = 'tech_leader'
    wrapper.vm.interviewPreferenceForm.defaultFeedbackMode = 'immediate'
    wrapper.vm.interviewPreferenceForm.responseDetailPreference = 'detailed'
    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 90
    wrapper.vm.handleDefaultJobChange('frontend')

    const preferences = getSettingsPreferences()
    expect(preferences).toMatchObject({
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate',
      responseDetailPreference: 'detailed',
      interviewRetentionDays: 90
    })
  })

  it('renders enabled destructive backend actions', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('账号注销')
    expect(wrapper.text()).toContain('面试记录清理')
    expect(wrapper.text()).toContain('简历诊断清理')
    expect(wrapper.findAll('button').some((button) => button.text().includes('待后端接入'))).toBe(false)
    const destructiveButtons = wrapper.findAll('button').filter((button) => ['注销账号', '清理记录'].includes(button.text()))
    expect(destructiveButtons).toHaveLength(3)
    expect(destructiveButtons.every((button) => button.attributes('disabled') === undefined)).toBe(true)
  })

  it('deletes account and clears login state after password confirmation', async () => {
    localStorage.setItem('ai_resume_token', 'user-token')
    localStorage.setItem('ai_resume_token_type', 'Bearer')
    const wrapper = mountView()
    await flushPromises()

    await wrapper.vm.handleAccountDelete('current-password')

    expect(deleteAccount).toHaveBeenCalledWith({ oldPassword: 'current-password' })
    expect(localStorage.getItem('ai_resume_token')).toBeNull()
    expect(clearUserInfo).toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith('/login')
  })

  it('clears interview and resume history then refreshes account overview', async () => {
    const wrapper = mountView()
    await flushPromises()
    getGrowthOverview.mockClear()

    await wrapper.vm.handleInterviewHistoryClear()
    await wrapper.vm.handleResumeHistoryClear()

    expect(clearInterviewHistory).toHaveBeenCalled()
    expect(clearResumeHistory).toHaveBeenCalled()
    expect(getGrowthOverview).toHaveBeenCalledTimes(2)
  })

  it('keeps login state when account deletion api fails', async () => {
    deleteAccount.mockRejectedValueOnce(new Error('failed'))
    localStorage.setItem('ai_resume_token', 'user-token')
    const wrapper = mountView()
    await flushPromises()

    await expect(wrapper.vm.handleAccountDelete('current-password')).rejects.toThrow('failed')

    expect(localStorage.getItem('ai_resume_token')).toBe('user-token')
    expect(clearUserInfo).not.toHaveBeenCalled()
  })

  it('shows account data overview from existing growth overview api', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getGrowthOverview).toHaveBeenCalled()
    expect(wrapper.text()).toContain('简历诊断次数')
    expect(wrapper.text()).toContain('模拟面试次数')
    expect(wrapper.text()).toContain('JD 匹配次数')
    expect(wrapper.text()).toContain('AI 润色次数')
    expect(wrapper.text()).toContain('7')
    expect(wrapper.text()).toContain('5')
  })

  it('clears local settings cache without removing login tokens', async () => {
    localStorage.setItem('ai_resume_token', 'user-token')
    localStorage.setItem('ai_resume_admin_token', 'admin-token')
    localStorage.setItem('theme', 'dark')
    localStorage.setItem('followSystem', 'false')
    saveSettingsPreferences({ defaultInterviewMode: 'stress' })

    const wrapper = mountView()
    await flushPromises()
    wrapper.vm.handleClearLocalCache()

    expect(localStorage.getItem('theme')).toBeNull()
    expect(localStorage.getItem('followSystem')).toBeNull()
    expect(getSettingsPreferences().defaultInterviewMode).toBe('normal')
    expect(localStorage.getItem('ai_resume_token')).toBe('user-token')
    expect(localStorage.getItem('ai_resume_admin_token')).toBe('admin-token')
  })
})
