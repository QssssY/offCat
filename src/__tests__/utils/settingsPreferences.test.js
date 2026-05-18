import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearLocalSettingsCache,
  DEFAULT_SETTINGS_PREFERENCES,
  getSettingsPreferences,
  normalizeSettingsPreferences,
  resetSettingsPreferences,
  saveSettingsPreferences
} from '@/utils/settingsPreferences'

describe('settingsPreferences', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns defaults when nothing is stored', () => {
    expect(getSettingsPreferences()).toEqual(DEFAULT_SETTINGS_PREFERENCES)
  })

  it('merges and persists saved preferences', () => {
    saveSettingsPreferences({
      notificationRealtimeEnabled: false,
      notificationDefaultType: 'resume',
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate',
      responseDetailPreference: 'detailed',
      interviewRetentionDays: 90
    })

    expect(getSettingsPreferences()).toEqual({
      notificationRealtimeEnabled: false,
      notificationDefaultUnreadOnly: false,
      notificationDefaultType: 'resume',
      defaultInterviewJobRole: '前端工程师',
      defaultInterviewJobRoleCode: 'frontend',
      defaultInterviewDifficulty: 'advanced',
      defaultInterviewMode: 'tech_leader',
      defaultFeedbackMode: 'immediate',
      responseDetailPreference: 'detailed',
      interviewRetentionDays: 90
    })
  })

  it('normalizes invalid stored interview preferences to defaults', () => {
    const normalized = normalizeSettingsPreferences({
      defaultInterviewJobRole: 123,
      defaultInterviewJobRoleCode: null,
      defaultInterviewDifficulty: 'invalid',
      defaultInterviewMode: 'custom',
      defaultFeedbackMode: 'later',
      responseDetailPreference: 'verbose',
      interviewRetentionDays: 7
    })

    expect(normalized).toEqual(DEFAULT_SETTINGS_PREFERENCES)
  })

  it('resets preferences back to defaults', () => {
    saveSettingsPreferences({
      notificationRealtimeEnabled: false,
      notificationDefaultUnreadOnly: true,
      notificationDefaultType: 'quota'
    })

    expect(resetSettingsPreferences()).toEqual(DEFAULT_SETTINGS_PREFERENCES)
    expect(getSettingsPreferences()).toEqual(DEFAULT_SETTINGS_PREFERENCES)
  })

  it('clears local settings cache without removing login tokens', () => {
    localStorage.setItem('ai_resume_token', 'user-token')
    localStorage.setItem('ai_resume_admin_token', 'admin-token')
    localStorage.setItem('theme', 'dark')
    localStorage.setItem('followSystem', 'false')
    saveSettingsPreferences({ defaultInterviewMode: 'stress' })

    expect(clearLocalSettingsCache()).toEqual(DEFAULT_SETTINGS_PREFERENCES)
    expect(localStorage.getItem('theme')).toBeNull()
    expect(localStorage.getItem('followSystem')).toBeNull()
    expect(getSettingsPreferences()).toEqual(DEFAULT_SETTINGS_PREFERENCES)
    expect(localStorage.getItem('ai_resume_token')).toBe('user-token')
    expect(localStorage.getItem('ai_resume_admin_token')).toBe('admin-token')
  })
})
