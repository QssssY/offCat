export const SETTINGS_PREFERENCES_KEY = 'ai_resume_settings_preferences'
export const SETTINGS_PREFERENCES_UPDATED_EVENT = 'ai-resume-settings-preferences-updated'

export const INTERVIEW_RETENTION_DAY_OPTIONS = Object.freeze([0, 30, 90, 180, 365])
export const RESUME_RETENTION_DAY_OPTIONS = Object.freeze([0, 30, 90, 180, 365])
export const VOICE_AUTO_SUBMIT_DELAY_OPTIONS = Object.freeze([0, 2000, 3000, 5000])
export const EDGE_CLOUD_TTS_VOICE_PREFERENCE = 'edge_cloud'
export const EDGE_CLOUD_TTS_VOICE_VALUE_PREFIX = `${EDGE_CLOUD_TTS_VOICE_PREFERENCE}:`
export const EDGE_CLOUD_TTS_VOICES = Object.freeze([
  Object.freeze({ id: 'zh-CN-XiaoxiaoNeural', name: '晓晓（女声，普通话）' }),
  Object.freeze({ id: 'zh-CN-XiaoyiNeural', name: '晓伊（女声，普通话）' }),
  Object.freeze({ id: 'zh-CN-YunjianNeural', name: '云健（男声，普通话）' }),
  Object.freeze({ id: 'zh-CN-YunxiNeural', name: '云希（男声，普通话）' }),
  Object.freeze({ id: 'zh-CN-YunxiaNeural', name: '云夏（男声，普通话）' }),
  Object.freeze({ id: 'zh-CN-YunyangNeural', name: '云扬（男声，普通话）' }),
  Object.freeze({ id: 'zh-HK-HiuGaaiNeural', name: '晓佳（女声，粤语）' }),
  Object.freeze({ id: 'zh-HK-HiuMaanNeural', name: '晓曼（女声，粤语）' }),
  Object.freeze({ id: 'zh-HK-WanLungNeural', name: '云龙（男声，粤语）' }),
  Object.freeze({ id: 'zh-TW-HsiaoChenNeural', name: '晓臻（女声，台湾普通话）' }),
  Object.freeze({ id: 'zh-TW-HsiaoYuNeural', name: '晓雨（女声，台湾普通话）' }),
  Object.freeze({ id: 'zh-TW-YunJheNeural', name: '云哲（男声，台湾普通话）' })
])
export const DEFAULT_EDGE_CLOUD_TTS_VOICE_ID = EDGE_CLOUD_TTS_VOICES[0].id
export const EDGE_CLOUD_TTS_VOICE_OPTIONS = Object.freeze([
  Object.freeze({
    label: `EdgeTTS 默认（${EDGE_CLOUD_TTS_VOICES[0].name}）`,
    value: EDGE_CLOUD_TTS_VOICE_PREFERENCE
  }),
  ...EDGE_CLOUD_TTS_VOICES.map((voice) => Object.freeze({
    label: `EdgeTTS ${voice.name}`,
    value: `${EDGE_CLOUD_TTS_VOICE_VALUE_PREFIX}${voice.id}`
  }))
])

const freezePresetGroup = (group) => Object.freeze({
  ...group,
  options: Object.freeze(group.options.map((option) => Object.freeze({ ...option })))
})

export const BROWSER_TTS_VOICE_PRESET_GROUPS = Object.freeze([
  freezePresetGroup({
    label: '女声系列',
    options: [
      { label: '温柔女声', value: 'gentle_female' },
      { label: '专业女声', value: 'pro_female' },
      { label: '活泼女声', value: 'lively_female' },
      { label: '暖心女声', value: 'warm_female' },
      { label: '女声优先', value: 'female' }
    ]
  }),
  freezePresetGroup({
    label: '男声系列',
    options: [
      { label: '磁性男声', value: 'magnetic_male' },
      { label: '专业男声', value: 'pro_male' },
      { label: '沉稳男声', value: 'calm_male' },
      { label: '活力男声', value: 'energetic_male' },
      { label: '男声优先', value: 'male' }
    ]
  }),
  freezePresetGroup({
    label: '通用',
    options: [
      { label: '默认中文自然音色', value: 'natural_zh' },
      { label: '新闻播报', value: 'news_anchor' },
      { label: '慢速清晰', value: 'slow_clear' },
      { label: '系统默认', value: 'system' }
    ]
  }),
  freezePresetGroup({
    label: '云端语音',
    options: EDGE_CLOUD_TTS_VOICE_OPTIONS
  }),
  freezePresetGroup({
    label: '自定义',
    options: [
      { label: '指定浏览器音色', value: 'custom' }
    ]
  })
])

export const BROWSER_TTS_PRESET_PARAMETERS = Object.freeze({
  natural_zh: Object.freeze({ rate: 0.92, pitch: 1.06 }),
  gentle_female: Object.freeze({ rate: 0.85, pitch: 1.12 }),
  pro_female: Object.freeze({ rate: 0.95, pitch: 1 }),
  lively_female: Object.freeze({ rate: 0.98, pitch: 1.15 }),
  warm_female: Object.freeze({ rate: 0.88, pitch: 1.08 }),
  magnetic_male: Object.freeze({ rate: 0.88, pitch: 0.9 }),
  pro_male: Object.freeze({ rate: 0.95, pitch: 0.95 }),
  calm_male: Object.freeze({ rate: 0.85, pitch: 0.88 }),
  energetic_male: Object.freeze({ rate: 0.98, pitch: 1.05 }),
  news_anchor: Object.freeze({ rate: 1, pitch: 1 }),
  slow_clear: Object.freeze({ rate: 0.75, pitch: 1.02 }),
  female: Object.freeze({ rate: 0.92, pitch: 1.06 }),
  male: Object.freeze({ rate: 0.92, pitch: 1.06 })
})

/**
 * 浏览器 TTS 预设绑定参数。
 * 返回新对象避免调用方误改全局预设；system/custom 不绑定滑块值。
 */
export function getBrowserTtsPresetParameters(presetKey) {
  const parameters = BROWSER_TTS_PRESET_PARAMETERS[presetKey]
  return parameters ? { ...parameters } : null
}

export function isEdgeCloudTtsVoicePreference(value) {
  return value === EDGE_CLOUD_TTS_VOICE_PREFERENCE ||
    (typeof value === 'string' && value.startsWith(EDGE_CLOUD_TTS_VOICE_VALUE_PREFIX))
}

/**
 * 从本地偏好值解析 EdgeTTS voice id。
 * 旧版 edge_cloud 没有携带具体 voice，继续回退默认晓晓，避免历史缓存失效。
 */
export function getEdgeCloudTtsVoiceId(preferenceValue) {
  if (typeof preferenceValue !== 'string' || preferenceValue === EDGE_CLOUD_TTS_VOICE_PREFERENCE) {
    return DEFAULT_EDGE_CLOUD_TTS_VOICE_ID
  }
  if (!preferenceValue.startsWith(EDGE_CLOUD_TTS_VOICE_VALUE_PREFIX)) {
    return DEFAULT_EDGE_CLOUD_TTS_VOICE_ID
  }

  const voiceId = preferenceValue.slice(EDGE_CLOUD_TTS_VOICE_VALUE_PREFIX.length)
  return EDGE_CLOUD_TTS_VOICES.some((voice) => voice.id === voiceId)
    ? voiceId
    : DEFAULT_EDGE_CLOUD_TTS_VOICE_ID
}

export function getEdgeCloudTtsPreferenceValue(voiceId) {
  if (voiceId === DEFAULT_EDGE_CLOUD_TTS_VOICE_ID) {
    return EDGE_CLOUD_TTS_VOICE_PREFERENCE
  }
  return EDGE_CLOUD_TTS_VOICES.some((voice) => voice.id === voiceId)
    ? `${EDGE_CLOUD_TTS_VOICE_VALUE_PREFIX}${voiceId}`
    : EDGE_CLOUD_TTS_VOICE_PREFERENCE
}

export const DEFAULT_SETTINGS_PREFERENCES = Object.freeze({
  notificationRealtimeEnabled: true,
  notificationDefaultUnreadOnly: false,
  notificationDefaultType: '',
  defaultInterviewJobRole: '',
  defaultInterviewJobRoleCode: '',
  defaultInterviewDifficulty: 'primary',
  defaultInterviewMode: 'normal',
  defaultFeedbackMode: 'after_interview',
  defaultInterviewInteractionType: 0,
  voiceSpeakingRate: 0.92,
  voicePitch: 1.06,
  voiceVolume: 1,
  voiceMuteResumeMode: 'auto',
  voiceAutoSubmitDelayMs: 3000,
  voiceRecognitionLanguage: 'auto',
  voiceRecognitionEngine: 'system_local',
  voicePreferredType: 'edge_cloud',
  voiceName: '',
  voiceURI: '',
  voiceLang: '',
  interviewRetentionDays: 0,
  resumeRetentionDays: 0
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
const INTERACTION_TYPE_VALUES = Object.freeze([0, 1])
const VOICE_MUTE_RESUME_MODE_VALUES = Object.freeze(['auto', 'manual'])
const VOICE_RECOGNITION_LANGUAGE_VALUES = Object.freeze(['auto', 'zh-CN', 'en-US'])
const VOICE_RECOGNITION_ENGINE_VALUES = Object.freeze(['system_local'])
const VOICE_PREFERRED_TYPE_VALUES = Object.freeze(
  BROWSER_TTS_VOICE_PRESET_GROUPS.flatMap((group) => group.options.map((option) => option.value))
)

const booleanOrDefault = (value, defaultValue) => (
  typeof value === 'boolean' ? value : defaultValue
)

const stringOrDefault = (value, defaultValue) => (
  typeof value === 'string' ? value : defaultValue
)

const optionOrDefault = (value, options, defaultValue) => (
  options.includes(value) ? value : defaultValue
)

const retentionDaysOrDefault = (value, options, defaultValue) => {
  const parsed = Number(value)
  return options.includes(parsed)
    ? parsed
    : defaultValue
}

const numberInRangeOrDefault = (value, min, max, defaultValue) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < min || parsed > max) {
    return defaultValue
  }
  return parsed
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
    defaultInterviewInteractionType: optionOrDefault(
      Number(preferences.defaultInterviewInteractionType),
      INTERACTION_TYPE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.defaultInterviewInteractionType
    ),
    voiceSpeakingRate: numberInRangeOrDefault(
      preferences.voiceSpeakingRate,
      0.7,
      1.2,
      DEFAULT_SETTINGS_PREFERENCES.voiceSpeakingRate
    ),
    voicePitch: numberInRangeOrDefault(
      preferences.voicePitch,
      0.8,
      1.3,
      DEFAULT_SETTINGS_PREFERENCES.voicePitch
    ),
    voiceVolume: numberInRangeOrDefault(
      preferences.voiceVolume,
      0,
      1,
      DEFAULT_SETTINGS_PREFERENCES.voiceVolume
    ),
    voiceMuteResumeMode: optionOrDefault(
      preferences.voiceMuteResumeMode,
      VOICE_MUTE_RESUME_MODE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.voiceMuteResumeMode
    ),
    voiceAutoSubmitDelayMs: retentionDaysOrDefault(
      preferences.voiceAutoSubmitDelayMs,
      VOICE_AUTO_SUBMIT_DELAY_OPTIONS,
      DEFAULT_SETTINGS_PREFERENCES.voiceAutoSubmitDelayMs
    ),
    voiceRecognitionLanguage: optionOrDefault(
      preferences.voiceRecognitionLanguage,
      VOICE_RECOGNITION_LANGUAGE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.voiceRecognitionLanguage
    ),
    voiceRecognitionEngine: optionOrDefault(
      preferences.voiceRecognitionEngine,
      VOICE_RECOGNITION_ENGINE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.voiceRecognitionEngine
    ),
    voicePreferredType: optionOrDefault(
      preferences.voicePreferredType,
      VOICE_PREFERRED_TYPE_VALUES,
      DEFAULT_SETTINGS_PREFERENCES.voicePreferredType
    ),
    voiceName: stringOrDefault(
      preferences.voiceName,
      DEFAULT_SETTINGS_PREFERENCES.voiceName
    ),
    voiceURI: stringOrDefault(
      preferences.voiceURI,
      DEFAULT_SETTINGS_PREFERENCES.voiceURI
    ),
    voiceLang: stringOrDefault(
      preferences.voiceLang,
      DEFAULT_SETTINGS_PREFERENCES.voiceLang
    ),
    interviewRetentionDays: retentionDaysOrDefault(
      preferences.interviewRetentionDays,
      INTERVIEW_RETENTION_DAY_OPTIONS,
      DEFAULT_SETTINGS_PREFERENCES.interviewRetentionDays
    ),
    resumeRetentionDays: retentionDaysOrDefault(
      preferences.resumeRetentionDays,
      RESUME_RETENTION_DAY_OPTIONS,
      DEFAULT_SETTINGS_PREFERENCES.resumeRetentionDays
    )
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
