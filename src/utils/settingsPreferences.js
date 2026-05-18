export const SETTINGS_PREFERENCES_KEY = 'ai_resume_settings_preferences'
export const SETTINGS_PREFERENCES_UPDATED_EVENT = 'ai-resume-settings-preferences-updated'

export const RESPONSE_DETAIL_PREFERENCE_OPTIONS = Object.freeze(['concise', 'standard', 'detailed'])
export const INTERVIEW_RETENTION_DAY_OPTIONS = Object.freeze([0, 30, 90, 180, 365])

export const DEFAULT_SETTINGS_PREFERENCES = Object.freeze({
  notificationRealtimeEnabled: true,
  notificationDefaultUnreadOnly: false,
  notificationDefaultType: '',
  defaultInterviewJobRole: '',
  defaultInterviewJobRoleCode: '',
  defaultInterviewDifficulty: 'primary',
  defaultInterviewMode: 'normal',
  defaultFeedbackMode: 'after_interview',
  responseDetailPreference: 'standard',
  interviewRetentionDays: 0
})

const LOCAL_CACHE_KEYS = Object.freeze([
  SETTINGS_PREFERENCES_KEY,
  'theme',
  'followSystem'
])

const DIFFICULTY_VALUES = Object.freeze(['primary', 'intermediate', 'advanced'])
const INTERVIEW_MODE_VALUES = Object.freeze([
  'normal',
  'stress',
  'big_company_hr',
  'tech_leader',
  'foreign_interviewer'
])
const FEEDBACK_MODE_VALUES = Object.freeze(['after_interview', 'immediate'])

const booleanOrDefault = (value, defaultValue) => (
  typeof value === 'boolean' ? value : defaultValue
)

const stringOrDefault = (value, defaultValue) => (
  typeof value === 'string' ? value : defaultValue
)

const optionOrDefault = (value, options, defaultValue) => (
  options.includes(value) ? value : defaultValue
)

const retentionDaysOrDefault = (value) => {
  const parsed = Number(value)
  return INTERVIEW_RETENTION_DAY_OPTIONS.includes(parsed)
    ? parsed
    : DEFAULT_SETTINGS_PREFERENCES.interviewRetentionDays
}

/**
 * 统一归一化设置中心本机偏好。
 * 新增偏好字段只影响当前浏览器，非法缓存值会回退默认值，避免旧缓存污染页面状态。
 */
export function normalizeSettingsPreferences(preferences = {}) {
  return {
    notificationRealtimeEnabled: booleanOrDefault(
      preferences.notificationRealtimeEnabled,
      DEFAULT_SETTINGS_PREFERENCES.notificationRealtimeEnabled
    ),
    notificationDefaultUnreadOnly: booleanOrDefault(
      preferences.notificationDefaultUnreadOnly,
      DEFAULT_SETTINGS_PREFERENCES.notificationDefaultUnreadOnly
    ),
    notificationDefaultType: stringOrDefault(
      preferences.notificationDefaultType,
      DEFAULT_SETTINGS_PREFERENCES.notificationDefaultType
    ),
    defaultInterviewJobRole: stringOrDefault(
      preferences.defaultInterviewJobRole,
      DEFAULT_SETTINGS_PREFERENCES.defaultInterviewJobRole
    ),
    defaultInterviewJobRoleCode: stringOrDefault(
      preferences.defaultInterviewJobRoleCode,
      DEFAULT_SETTINGS_PREFERENCES.defaultInterviewJobRoleCode
    ),
    defaultInterviewDifficulty: optionOrDefault(
      preferences.defaultInterviewDifficulty,
      DIFFICULTY_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.defaultInterviewDifficulty
    ),
    defaultInterviewMode: optionOrDefault(
      preferences.defaultInterviewMode,
      INTERVIEW_MODE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.defaultInterviewMode
    ),
    defaultFeedbackMode: optionOrDefault(
      preferences.defaultFeedbackMode,
      FEEDBACK_MODE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.defaultFeedbackMode
    ),
    responseDetailPreference: optionOrDefault(
      preferences.responseDetailPreference,
      RESPONSE_DETAIL_PREFERENCE_OPTIONS,
      DEFAULT_SETTINGS_PREFERENCES.responseDetailPreference
    ),
    interviewRetentionDays: retentionDaysOrDefault(preferences.interviewRetentionDays)
  }
}

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
    return normalizeSettingsPreferences(parsed)
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

  const normalized = normalizeSettingsPreferences(merged)

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

/**
 * 清空设置中心管理的浏览器本机缓存。
 * 登录态和管理端登录态不在清理范围内，避免用户误以为只是清缓存却被登出。
 */
export function clearLocalSettingsCache() {
  if (typeof localStorage !== 'undefined') {
    LOCAL_CACHE_KEYS.forEach((key) => localStorage.removeItem(key))
  }
  const defaults = { ...DEFAULT_SETTINGS_PREFERENCES }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(SETTINGS_PREFERENCES_UPDATED_EVENT, { detail: defaults }))
  }
  return defaults
}
