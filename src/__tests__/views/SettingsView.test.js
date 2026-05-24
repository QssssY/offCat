import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus, { ElMessageBox } from 'element-plus'
import SettingsView from '@/views/settings/SettingsView.vue'
import { getGrowthOverview } from '@/api/growth'
import { clearInterviewHistory, getInterviewJobRoles } from '@/api/interview'
import { getMembershipPlans } from '@/api/membership'
import { clearResumeHistory } from '@/api/resume'
import { getUserSettings, saveUserSettings } from '@/api/userSettings'
import { deleteAccount, getCurrentAccountSecurityQuestion } from '@/api/auth'
import { createUserFeedback } from '@/api/feedback'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { getSettingsPreferences, saveSettingsPreferences } from '@/utils/settingsPreferences'

const push = vi.fn()
const fetchUserInfo = vi.fn(() => Promise.resolve())
const clearUserInfo = vi.fn()
const setTheme = vi.fn()
const setFollowSystem = vi.fn()
let currentWrapper = null

vi.setConfig({ testTimeout: 15000 })

vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

vi.mock('@/api/auth', () => ({
  deleteAccount: vi.fn(() => Promise.resolve()),
  getCurrentAccountSecurityQuestion: vi.fn(() => Promise.resolve({ data: { securityQuestion: '你的出生城市是哪里？' } })),
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

vi.mock('@/api/userSettings', () => ({
  getUserSettings: vi.fn(() => Promise.resolve({
    data: {
      interviewRetentionDays: 0,
      resumeRetentionDays: 0
    }
  })),
  saveUserSettings: vi.fn((data) => Promise.resolve({ data }))
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

vi.mock('@/api/feedback', () => ({
  createUserFeedback: vi.fn(() => Promise.resolve({ data: 100 }))
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

const settingsViewSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/settings/SettingsView.vue'), 'utf8')

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
        createTime: '2026-05-01T09:30:00',
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
    vi.useRealTimers()
  })

  it('renders all settings sections', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('设置中心')
    expect(wrapper.text()).toContain('账号资料')
    expect(wrapper.text()).toContain('面试偏好')
    expect(wrapper.text()).toContain('默认交互方式')
    expect(wrapper.text()).toContain('语音通话偏好')
    expect(wrapper.text()).toContain('账号安全')
    expect(wrapper.text()).toContain('注销账号')
    expect(wrapper.find('.settings-nav').text()).not.toContain('注销账号')
    expect(wrapper.text()).toContain('隐私与数据')
    expect(wrapper.text()).toContain('数据管理')
    expect(wrapper.text()).toContain('问题反馈')
    expect(wrapper.text()).toContain('外观偏好')
    expect(wrapper.text()).toContain('通知偏好')
    expect(wrapper.text()).toContain('新手引导')
    expect(wrapper.text()).toContain('会员与额度')
  }, 15000)

  it('uses readable local feature icon sizes inside the personal settings center', () => {
    const source = settingsViewSource()

    expect(source).toContain('<FeatureIcon :name="section.icon" size="md" class="settings-nav-icon" />')
    expect(source).toContain('<FeatureIcon name="voice-interview" size="md" class="voice-preview-icon" />')
    expect(source).toContain('<FeatureIcon name="account-security" size="md" class="settings-alert-icon" />')
    expect(source).toContain('<FeatureIcon name="growth-radar" size="md" class="settings-refresh-icon" />')
    expect(source).toContain('<FeatureIcon name="account-security" size="md" />')
    expect(source).toContain('.settings-nav-item:hover .settings-nav-icon')
    expect(source).toContain('width: 48px;')
  })

  it('shows subscription plan name without exposing internal identifiers', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getMembershipPlans).toHaveBeenCalled()
    expect(wrapper.text()).toContain('订阅套餐')
    expect(wrapper.text()).toContain('月度会员')
    expect(wrapper.text()).toContain('注册时间')
    expect(wrapper.text()).toContain('2026-05-01')
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
    expect(wrapper.find('section[aria-labelledby="security-title"] .settings-form .el-select').exists()).toBe(false)

    wrapper.vm.handleSecurityModeChange('securityQuestion')
    await flushPromises()
    await waitForSecurityTransition()

    expect(wrapper.find('input[autocomplete="new-password"]').exists()).toBe(false)
    expect(wrapper.find('section[aria-labelledby="security-title"] .settings-form .el-select').exists()).toBe(true)
  }, 15000)

  it('renders account deletion as an account security tab', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'security'
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()

    const tabs = wrapper.findAll('.security-mode-tab').map((tab) => tab.text())
    expect(tabs).toEqual(['修改密码', '修改安全问题', '注销账号'])
    expect(wrapper.find('.settings-nav').text()).not.toContain('注销账号')
    expect(wrapper.text()).toContain('注销后不可恢复')
    expect(wrapper.find('.account-delete-context').exists()).toBe(true)
    expect(wrapper.find('.account-delete-form').exists()).toBe(true)
  })

  it('does not repeat security section title inside the selected form', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.settings-form h3').exists()).toBe(false)
  }, 15000)

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
    wrapper.vm.interviewPreferenceForm.defaultInterviewInteractionType = 1
    wrapper.vm.interviewPreferenceForm.voiceSpeakingRate = 1.1
    wrapper.vm.interviewPreferenceForm.voicePitch = 0.95
    wrapper.vm.interviewPreferenceForm.voiceVolume = 0.6
    wrapper.vm.interviewPreferenceForm.voiceMuteResumeMode = 'manual'
    wrapper.vm.interviewPreferenceForm.voiceAutoSubmitDelayMs = 5000
    wrapper.vm.interviewPreferenceForm.voiceRecognitionLanguage = 'en-US'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'custom'
    wrapper.vm.interviewPreferenceForm.voiceName = 'Microsoft Xiaoxiao Natural'
    wrapper.vm.interviewPreferenceForm.voiceURI = 'xiaoxiao-uri'
    wrapper.vm.interviewPreferenceForm.voiceLang = 'zh-CN'
    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 90
    wrapper.vm.interviewPreferenceForm.resumeRetentionDays = 180
    await wrapper.vm.handleDefaultJobChange('frontend')
    await flushPromises()

    const preferences = getSettingsPreferences()
    expect(saveUserSettings).not.toHaveBeenCalled()
    expect(preferences).toMatchObject({
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate',
      defaultInterviewInteractionType: 1,
      voiceSpeakingRate: 1.1,
      voicePitch: 0.95,
      voiceVolume: 0.6,
      voiceMuteResumeMode: 'manual',
      voiceAutoSubmitDelayMs: 5000,
      voiceRecognitionLanguage: 'en-US',
      voicePreferredType: 'custom',
      voiceName: 'Microsoft Xiaoxiao Natural',
      voiceURI: 'xiaoxiao-uri',
      voiceLang: 'zh-CN',
      interviewRetentionDays: 90,
      resumeRetentionDays: 180
    })
  }, 15000)

  it('resets local voice preferences without changing other interview settings', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.defaultInterviewDifficulty = 'advanced'
    wrapper.vm.interviewPreferenceForm.voiceSpeakingRate = 1.1
    wrapper.vm.interviewPreferenceForm.voicePitch = 0.95
    wrapper.vm.interviewPreferenceForm.voiceVolume = 0.6
    wrapper.vm.interviewPreferenceForm.voiceMuteResumeMode = 'manual'
    wrapper.vm.interviewPreferenceForm.voiceAutoSubmitDelayMs = 5000
    wrapper.vm.interviewPreferenceForm.voiceRecognitionLanguage = 'en-US'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'custom'
    wrapper.vm.interviewPreferenceForm.voiceName = 'Microsoft Xiaoxiao Natural'
    wrapper.vm.interviewPreferenceForm.voiceURI = 'xiaoxiao-uri'
    wrapper.vm.interviewPreferenceForm.voiceLang = 'zh-CN'
    wrapper.vm.handleInterviewPreferenceSave()
    await flushPromises()

    wrapper.vm.handleVoicePreferenceReset()
    await flushPromises()

    expect(getSettingsPreferences()).toMatchObject({
      defaultInterviewDifficulty: 'advanced',
      voiceSpeakingRate: 0.92,
      voicePitch: 1.06,
      voiceVolume: 1,
      voiceMuteResumeMode: 'auto',
      voiceAutoSubmitDelayMs: 3000,
      voiceRecognitionLanguage: 'auto',
      voicePreferredType: 'natural_zh',
      voiceName: '',
      voiceURI: '',
      voiceLang: ''
    })
  }, 15000)

  it('uses responsive class for custom browser voice selector', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'interview'
    wrapper.vm.interviewPreferenceForm.voicePreferredType = 'custom'
    await flushPromises()

    expect(wrapper.find('.browser-voice-select').exists()).toBe(true)
    const browserVoiceSelect = wrapper.findAllComponents({ name: 'ElSelect' })
      .find((select) => select.classes().includes('browser-voice-select'))
    expect(browserVoiceSelect.props('fitInputWidth')).toBe(true)
    expect(browserVoiceSelect.props('popperClass')).toBe('browser-voice-select-popper')
  }, 15000)

  it('renders voice preview as an accessible icon button', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'interview'
    await flushPromises()

    const previewButton = wrapper.find('.voice-preview-button')
    expect(previewButton.exists()).toBe(true)
    expect(previewButton.attributes('aria-label')).toBe('试听当前 AI 播报声音')
    expect(previewButton.find('.voice-preview-icon img').exists()).toBe(true)
  })

  it('loads server settings and renders resume retention preference', async () => {
    getUserSettings.mockResolvedValueOnce({
      data: {
        interviewRetentionDays: 30,
        resumeRetentionDays: 90
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(getUserSettings).toHaveBeenCalled()
    expect(wrapper.text()).toContain('简历诊断保留天数')
    expect(getSettingsPreferences()).toMatchObject({
      interviewRetentionDays: 30,
      resumeRetentionDays: 90
    })
  }, 15000)

  it('does not change local preferences when server settings save fails', async () => {
    saveSettingsPreferences({ interviewRetentionDays: 30, resumeRetentionDays: 90 })
    getUserSettings.mockResolvedValueOnce({
      data: {
        interviewRetentionDays: 30,
        resumeRetentionDays: 90
      }
    })
    saveUserSettings.mockRejectedValueOnce(new Error('failed'))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 365
    await expect(wrapper.vm.handleDataManagementSettingsSave()).rejects.toThrow('failed')

    expect(getSettingsPreferences()).toMatchObject({
      interviewRetentionDays: 30,
      resumeRetentionDays: 90
    })
  })

  it('saves data management retention settings only after explicit save', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.interviewPreferenceForm.interviewRetentionDays = 90
    wrapper.vm.interviewPreferenceForm.resumeRetentionDays = 180

    expect(saveUserSettings).not.toHaveBeenCalled()
    await wrapper.vm.handleDataManagementSettingsSave()

    expect(saveUserSettings).toHaveBeenCalledWith({
      interviewRetentionDays: 90,
      resumeRetentionDays: 180
    })
    expect(getSettingsPreferences()).toMatchObject({
      interviewRetentionDays: 90,
      resumeRetentionDays: 180
    })
  })

  it('renders enabled destructive backend actions', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('注销账号')
    expect(wrapper.text()).toContain('面试记录清理')
    expect(wrapper.text()).toContain('简历诊断清理')
    expect(wrapper.findAll('button').some((button) => button.text().includes('待后端接入'))).toBe(false)
    const destructiveButtons = wrapper.findAll('button').filter((button) => ['清理记录'].includes(button.text()))
    expect(destructiveButtons).toHaveLength(2)
    expect(destructiveButtons.every((button) => button.attributes('disabled') === undefined)).toBe(true)
  })

  it('deletes account and clears login state after password and security confirmation', async () => {
    localStorage.setItem('ai_resume_token', 'user-token')
    localStorage.setItem('ai_resume_token_type', 'Bearer')
    const wrapper = mountView()
    await flushPromises()

    await wrapper.vm.handleAccountDelete({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })

    expect(deleteAccount).toHaveBeenCalledWith({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })
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

    await expect(wrapper.vm.handleAccountDelete({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })).rejects.toThrow('failed')

    expect(localStorage.getItem('ai_resume_token')).toBe('user-token')
    expect(clearUserInfo).not.toHaveBeenCalled()
  }, 15000)

  it('loads security question without blocking account delete form before confirmation dialog', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'security'
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()

    expect(getCurrentAccountSecurityQuestion).toHaveBeenCalled()
    expect(wrapper.text()).toContain('注销后不可恢复')
    expect(wrapper.text()).toContain('你的出生城市是哪里？')
    const submitButton = wrapper.find('.account-delete-form button.el-button--danger')
    expect(submitButton.attributes('disabled')).toBeUndefined()
    expect(submitButton.text()).toContain('确认注销')
  }, 15000)

  it('starts account deletion countdown only inside the confirmation dialog', async () => {
    const wrapper = mountView()
    await flushPromises()
    vi.useFakeTimers()

    expect(wrapper.vm.accountDeleteCountdown).toBe(0)

    wrapper.vm.onDialogOpen()

    expect(wrapper.vm.accountDeleteCountdown).toBe(15)
    expect(wrapper.vm.dialogConfirmButtonText).toBe('等待 15 秒')

    vi.advanceTimersByTime(3000)
    expect(wrapper.vm.accountDeleteCountdown).toBe(12)

    vi.advanceTimersByTime(12000)
    expect(wrapper.vm.accountDeleteCountdown).toBe(0)
    expect(wrapper.vm.dialogConfirmButtonText).toBe('确认注销')
  })

  it('submits account deletion from security tab after cooldown with existing payload', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'security'
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()
    wrapper.vm.accountDeleteForm = {
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    }
    await flushPromises()

    await wrapper.vm.handleAccountDeleteSubmit()
    expect(deleteAccount).not.toHaveBeenCalled()

    wrapper.vm.accountDeleteCountdown = 0
    wrapper.vm.accountDeleteConfirmText = wrapper.vm.accountDeleteExpectedText
    await wrapper.vm.handleDialogConfirm()

    expect(deleteAccount).toHaveBeenCalledWith({
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    })
  }, 15000)

  it('opens text confirmation before deleting account', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'security'
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()
    wrapper.vm.accountDeleteForm = {
      oldPassword: 'current-password',
      confirmPassword: 'current-password',
      securityAnswer: 'answer'
    }
    await flushPromises()

    await expect(wrapper.vm.handleAccountDeleteSubmit()).resolves.toBeUndefined()

    expect(wrapper.vm.accountDeleteConfirmDialogVisible).toBe(true)
    expect(deleteAccount).not.toHaveBeenCalled()
  }, 15000)

  it('collapses and expands long account deletion security questions', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'security'
    wrapper.vm.handleSecurityModeChange('accountDeletion')
    await flushPromises()
    await waitForSecurityTransition()
    await flushPromises()
    wrapper.vm.accountDeleteSecurityQuestion = '这是一条非常长的安全问题文本用于验证默认折叠展示，包含足够多的文字以避免撑破注销账号表单布局'
    await flushPromises()

    const card = wrapper.find('.security-question-card')
    const toggle = wrapper.find('.security-question-toggle')
    expect(toggle.exists()).toBe(true)
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(card.classes()).not.toContain('expanded')

    await toggle.trigger('click')

    expect(wrapper.find('.security-question-toggle').attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('.security-question-card').classes()).toContain('expanded')
  }, 15000)

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

  it('submits user feedback from settings center and resets form', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.activeSection = 'feedback'
    wrapper.vm.feedbackForm = {
      type: 'suggestion',
      title: ' 增加导出提示 ',
      content: '这里是一段超过十个字符的功能建议内容',
      contact: 'user@example.com'
    }
    await wrapper.vm.handleFeedbackSubmit()

    expect(createUserFeedback).toHaveBeenCalledWith({
      type: 'suggestion',
      title: '增加导出提示',
      content: '这里是一段超过十个字符的功能建议内容',
      contact: 'user@example.com'
    })
    expect(wrapper.vm.feedbackForm).toMatchObject({
      type: 'bug',
      title: '',
      content: '',
      contact: ''
    })
  })
})
