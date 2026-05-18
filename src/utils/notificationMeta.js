const NOTIFICATION_TYPES = {
  resume: {
    key: 'resume',
    label: '简历诊断',
    tagType: 'warning',
    icon: 'document'
  },
  polish: {
    key: 'polish',
    label: 'AI润色',
    tagType: 'primary',
    icon: 'edit'
  },
  interview: {
    key: 'interview',
    label: '模拟面试',
    tagType: 'success',
    icon: 'user'
  },
  quota: {
    key: 'quota',
    label: '额度提醒',
    tagType: 'danger',
    icon: 'alert'
  },
  system: {
    key: 'system',
    label: '系统公告',
    tagType: 'info',
    icon: 'bell'
  },
  activity: {
    key: 'activity',
    label: '活动公告',
    tagType: 'warning',
    icon: 'calendar'
  },
  update: {
    key: 'update',
    label: '版本公告',
    tagType: 'primary',
    icon: 'refresh'
  },
  maintenance: {
    key: 'maintenance',
    label: '维护公告',
    tagType: 'danger',
    icon: 'tool'
  }
}

const DEFAULT_TYPE = {
  key: 'unknown',
  label: '通知',
  tagType: 'info',
  icon: 'bell'
}

const ADMIN_ANNOUNCEMENT_TYPES = new Set(['system', 'activity', 'update', 'maintenance'])

/**
 * 统一管理通知类型的展示元数据，避免铃铛下拉和通知中心出现文案或图标不一致。
 * @param {string} type 通知类型
 * @returns {{ key: string, label: string, tagType: string, icon: string }}
 */
export function getNotificationTypeMeta(type) {
  return NOTIFICATION_TYPES[type] || DEFAULT_TYPE
}

/**
 * 管理端公告类通知需要留在当前页面用弹窗展示全文，业务通知继续跳转业务详情。
 * @param {string} type 通知类型
 * @returns {boolean}
 */
export function isAdminAnnouncementType(type) {
  return ADMIN_ANNOUNCEMENT_TYPES.has(type)
}

/**
 * 格式化通知时间，compact 用于铃铛面板的窄空间展示。
 * @param {string|Date} time 创建时间
 * @param {{ compact?: boolean }} options 格式化选项
 * @returns {string}
 */
export function formatNotificationTime(time, options = {}) {
  if (!time) return ''

  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return String(time)

  const diff = Date.now() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  const spacer = options.compact ? '' : ' '

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}${spacer}分钟前`
  if (hours < 24) return `${hours}${spacer}小时前`
  if (days < 7) return `${days}${spacer}天前`

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  if (options.compact) return `${month}-${day}`

  const year = date.getFullYear()
  return `${year}-${month}-${day}`
}
