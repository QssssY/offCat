const SETTINGS_PREFERENCES_KEY = 'ai_resume_settings_preferences'
export const SETTINGS_PREFERENCES_UPDATED_EVENT = 'ai-resume-settings-preferences-updated'

export const DEFAULT_SETTINGS_PREFERENCES = Object.freeze({
  notificationRealtimeEnabled: true,
  notificationDefaultUnreadOnly: false,
  notificationDefaultType: ''
})

const booleanOrDefault = (value, defaultValue) => (
  typeof value === 'boolean' ? value : defaultValue
)

const stringOrDefault = (value, defaultValue) => (
  typeof value === 'string' ? value : defaultValue
)

/**
 * 读取当前浏览器内的设置偏好。
 * 设置中心第一版不新增后端接口，因此通知偏好只影响本机展示与筛选默认值。
 */
export function getSettingsPreferences() {
  if (typeof localStorage === 'undefined') {
    return { ...DEFAULT_SETTINGS_PREFERENCES }
  }

  try {
    const raw = localStorage.getItem(SETTINGS_PREFERENCES_KEY)
    if (!raw) {
      return { ...DEFAULT_SETTINGS_PREFERENCES }
    }

    const parsed = JSON.parse(raw)
    return {
      notificationRealtimeEnabled: booleanOrDefault(
        parsed.notificationRealtimeEnabled,
        DEFAULT_SETTINGS_PREFERENCES.notificationRealtimeEnabled
      ),
      notificationDefaultUnreadOnly: booleanOrDefault(
        parsed.notificationDefaultUnreadOnly,
        DEFAULT_SETTINGS_PREFERENCES.notificationDefaultUnreadOnly
      ),
      notificationDefaultType: stringOrDefault(
        parsed.notificationDefaultType,
        DEFAULT_SETTINGS_PREFERENCES.notificationDefaultType
      )
    }
  } catch {
    return { ...DEFAULT_SETTINGS_PREFERENCES }
  }
}

/**
 * 合并保存设置偏好，避免调用方只更新一个字段时覆盖其他字段。
 */
export function saveSettingsPreferences(nextPreferences) {
  const merged = {
    ...getSettingsPreferences(),
    ...nextPreferences
  }

  const normalized = {
    notificationRealtimeEnabled: booleanOrDefault(
      merged.notificationRealtimeEnabled,
      DEFAULT_SETTINGS_PREFERENCES.notificationRealtimeEnabled
    ),
    notificationDefaultUnreadOnly: booleanOrDefault(
      merged.notificationDefaultUnreadOnly,
      DEFAULT_SETTINGS_PREFERENCES.notificationDefaultUnreadOnly
    ),
    notificationDefaultType: stringOrDefault(
      merged.notificationDefaultType,
      DEFAULT_SETTINGS_PREFERENCES.notificationDefaultType
    )
  }

  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(SETTINGS_PREFERENCES_KEY, JSON.stringify(normalized))
  }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(SETTINGS_PREFERENCES_UPDATED_EVENT, { detail: normalized }))
  }

  return normalized
}

/**
 * 恢复默认设置，主要用于设置中心重置和单元测试。
 */
export function resetSettingsPreferences() {
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem(SETTINGS_PREFERENCES_KEY)
  }
  const defaults = { ...DEFAULT_SETTINGS_PREFERENCES }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(SETTINGS_PREFERENCES_UPDATED_EVENT, { detail: defaults }))
  }
  return defaults
}
