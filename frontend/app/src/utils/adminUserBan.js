export const ADMIN_BAN_REASON_MAX_LENGTH = 200

export const ADMIN_BAN_DURATION_OPTIONS = [
  { label: '1天', value: '1d' },
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' },
  { label: '永久', value: 'permanent' }
]

export const normalizeAdminBanReason = (value) => String(value || '').trim()

export const validateAdminBanReason = (value) => {
  const reason = normalizeAdminBanReason(value)
  if (!reason) return '请输入封禁原因'
  if (reason.length > ADMIN_BAN_REASON_MAX_LENGTH) return `封禁原因不能超过${ADMIN_BAN_REASON_MAX_LENGTH}字`
  return true
}

export const normalizeAdminUnbanReason = (value) => normalizeAdminBanReason(value)
