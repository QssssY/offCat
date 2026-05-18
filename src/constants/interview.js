/**
 * 面试模块共享常量
 */

/** 难度级别映射：数值 → 中文标签 */
export const DIFFICULTY_LABEL_MAP = {
  1: '初级',
  2: '中级',
  3: '高级'
}

/** 难度级别映射：数值 → Element Plus Tag 类型 */
export const DIFFICULTY_TAG_MAP = {
  1: { text: '初级', type: 'success' },
  2: { text: '中级', type: 'warning' },
  3: { text: '高级', type: 'danger' }
}

/**
 * 获取难度级别的中文标签
 * @param {number} difficulty - 难度值（1/2/3）
 * @param {string} fallback - 默认值
 * @returns {string}
 */
export function getDifficultyLabel(difficulty, fallback = '未知') {
  return DIFFICULTY_LABEL_MAP[difficulty] || fallback
}

/** 难度级别映射：英文 key → 数值（用于前端选项值转 API 参数） */
export const STRING_TO_DIFFICULTY = {
  primary: 1,
  intermediate: 2,
  advanced: 3
}

/** 难度级别映射：数值 → 英文 key（用于路由 query 传递） */
export const DIFFICULTY_KEY_MAP = {
  1: 'primary',
  2: 'intermediate',
  3: 'advanced'
}

/** 面试模式标签映射 */
export const INTERVIEW_MODE_LABEL_MAP = {
  normal: '普通面试',
  stress: '压力面试',
  job_targeted: '岗位定向模拟',
  big_company_hr: '大厂 HR 面',
  tech_leader: '技术 Leader 面',
  foreign_interviewer: '外企面试官'
}

/** 入口页固定面试官模式选项：只开放有限人设，不支持自定义 */
export const INTERVIEW_MODE_OPTIONS = [
  { label: '普通面试', value: 'normal', hint: '标准流程' },
  { label: '压力面试', value: 'stress', hint: '高压情境' },
  { label: '大厂 HR 面', value: 'big_company_hr', hint: '行为文化' },
  { label: '技术 Leader 面', value: 'tech_leader', hint: '项目深挖' },
  { label: '外企面试官', value: 'foreign_interviewer', hint: '英文逻辑' }
]

/**
 * 反馈模式选项（入口页使用）
 */
export const FEEDBACK_MODE_OPTIONS = [
  { label: '面完复盘', value: 'after_interview', hint: '面试结束后统一分析' },
  { label: '每题反馈', value: 'immediate', hint: '回答后即时点评' }
]

/**
 * 反馈模式标签映射
 */
export const FEEDBACK_MODE_LABEL_MAP = {
  after_interview: '面完复盘',
  immediate: '每题反馈'
}

/**
 * 获取反馈模式中文标签
 * @param {string} mode - 反馈模式
 * @param {string} fallback - 默认值
 * @returns {string}
 */
export function getFeedbackModeLabel(mode, fallback = '面完复盘') {
  return FEEDBACK_MODE_LABEL_MAP[mode] || fallback
}

/**
 * 获取面试模式中文标签
 * @param {string} mode - 面试模式
 * @param {string} fallback - 默认值
 * @returns {string}
 */
export function getInterviewModeLabel(mode, fallback = '普通面试') {
  return INTERVIEW_MODE_LABEL_MAP[mode] || fallback
}
