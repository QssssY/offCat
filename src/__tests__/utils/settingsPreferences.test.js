import { beforeEach, describe, expect, it } from 'vitest'
import {
  DEFAULT_SETTINGS_PREFERENCES,
  getSettingsPreferences,
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
      notificationDefaultType: 'resume'
    })

    expect(getSettingsPreferences()).toEqual({
      notificationRealtimeEnabled: false,
      notificationDefaultUnreadOnly: false,
      notificationDefaultType: 'resume'
    })
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
})
