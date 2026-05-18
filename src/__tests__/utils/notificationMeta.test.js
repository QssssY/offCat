import { describe, expect, it, vi } from 'vitest'
import {
  formatNotificationTime,
  getNotificationTypeMeta,
  isAdminAnnouncementType
} from '@/utils/notificationMeta'

describe('notificationMeta', () => {
  it('returns shared meta for known notification types', () => {
    expect(getNotificationTypeMeta('activity')).toMatchObject({
      key: 'activity',
      label: '活动公告',
      tagType: 'warning',
      icon: 'calendar'
    })
    expect(getNotificationTypeMeta('update')).toMatchObject({
      key: 'update',
      label: '版本公告',
      tagType: 'primary',
      icon: 'refresh'
    })
  })

  it('falls back to generic notification meta for unknown types', () => {
    expect(getNotificationTypeMeta('custom')).toEqual({
      key: 'unknown',
      label: '通知',
      tagType: 'info',
      icon: 'bell'
    })
  })

  it('recognizes admin announcement types only', () => {
    expect(isAdminAnnouncementType('system')).toBe(true)
    expect(isAdminAnnouncementType('maintenance')).toBe(true)
    expect(isAdminAnnouncementType('resume')).toBe(false)
    expect(isAdminAnnouncementType('polish')).toBe(false)
  })

  it('formats relative and compact notification time', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-17T12:00:00Z'))

    expect(formatNotificationTime('2026-05-17T09:00:00Z')).toBe('3 小时前')
    expect(formatNotificationTime('2026-05-15T12:00:00Z', { compact: true })).toBe('2天前')
    expect(formatNotificationTime('2026-05-01T12:00:00Z')).toBe('2026-05-01')

    vi.useRealTimers()
  })
})
