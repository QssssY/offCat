export const ADMIN_HIDE_REASON_MAX_LENGTH = 200

export const DETAIL_TITLE_COLLAPSE_LENGTH = 80

export const validateAdminHideReason = (value) => {
  const normalized = value?.trim() || ''
  if (!normalized) return '请输入下架原因'
  if (normalized.length > ADMIN_HIDE_REASON_MAX_LENGTH) {
    return `下架原因不能超过${ADMIN_HIDE_REASON_MAX_LENGTH}字`
  }
  return true
}

export const normalizeAdminHideReason = (value) => value.trim()
