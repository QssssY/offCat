import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import SettingsView from '@/views/settings/SettingsView.vue'
import { getMembershipPlans } from '@/api/membership'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const push = vi.fn()
const fetchUserInfo = vi.fn(() => Promise.resolve())
const clearUserInfo = vi.fn()
const setTheme = vi.fn()
const setFollowSystem = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

vi.mock('@/api/auth', () => ({
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

const mountView = () => mount(SettingsView, {
  global: {
    plugins: [ElementPlus],
    stubs: {
      transition: false
    }
  }
})

const waitForSecurityTransition = () => new Promise((resolve) => setTimeout(resolve, 220))

describe('SettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
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

  it('renders all settings sections', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('设置中心')
    expect(wrapper.text()).toContain('账号资料')
    expect(wrapper.text()).toContain('账号安全')
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
})
