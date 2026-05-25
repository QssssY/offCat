import { describe, expect, it } from 'vitest'
import {
  criticalFeatureIconSources,
  featureIconLabels,
  featureIcons,
  featureIconSources,
  getFeatureIcon,
  getCriticalFeatureIconSource,
  getFeatureIconLabel,
  getFeatureIconSource,
  loadFeatureIconSource
} from '@/utils/featureIcons'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const orderedFeatureIconKeys = [
  'home-dashboard',
  'resume-upload',
  'resume-analysis',
  'resume-optimization',
  'mock-interview',
  'interview-report',
  'history-records',
  'template-library',
  'community-hub',
  'growth-center',
  'offer-assistant',
  'settings',
  'resume-notifications',
  'resume-polish-notifications',
  'interview-notifications',
  'membership-credits',
  'system-notifications',
  'event-notifications',
  'user-profile',
  'membership-center',
  'beginner-guide',
  'data-management',
  'account-security',
  'ai-interviewer',
  'ai-loading',
  'announcement',
  'attachment',
  'back',
  'close',
  'collapse',
  'comment',
  'community-activity',
  'copy',
  'dark-mode',
  'data-cleanup',
  'delete',
  'download',
  'edit',
  'empty-state',
  'error',
  'exit-fullscreen',
  'expand',
  'favorite',
  'feedback-center',
  'fullscreen',
  'growth-milestone',
  'growth-radar',
  'image-upload',
  'interview-answer',
  'interview-end',
  'interview-feedback',
  'interview-pause',
  'interview-question',
  'interview-replay',
  'interview-start',
  'job-match-analysis',
  'light-mode',
  'liked',
  'loading',
  'mark-read',
  'menu',
  'message',
  'microphone-off',
  'microphone-on',
  'more',
  'next',
  'notification-center',
  'notification-settings',
  'offer-comparison',
  'onboarding-task',
  'password',
  'preview',
  'previous',
  'privacy',
  'processing',
  'profile-edit',
  'resume-editor',
  'resume-export',
  'resume-score',
  'retry',
  'salary-negotiation',
  'salary-script',
  'save',
  'search',
  'security-question',
  'share',
  'success',
  'template-editor',
  'unread',
  'upload-file',
  'version-log',
  'voice-interview',
  'voice-settings',
  'warning'
]

describe('featureIcons', () => {
  it('should keep only first-screen feature icons in the synchronous source map', () => {
    const unknownLabel = getFeatureIconLabel('unknown-feature')
    const criticalKeys = Object.keys(criticalFeatureIconSources)

    for (const key of criticalKeys) {
      expect(getFeatureIcon(key)).toContain(`${key}.png`)
      expect(getFeatureIconSource(key).png).toContain(`${key}.png`)
      expect(getFeatureIconSource(key).png).toContain('png-fallback')
      expect(getFeatureIconSource(key).webp).toContain(`${key}.webp`)
      expect(getFeatureIconLabel(key)).not.toBe(unknownLabel)
    }

    expect(criticalKeys).toContain('home-dashboard')
    expect(criticalKeys).toContain('ai-interviewer')
    expect(criticalKeys).not.toContain('growth-radar')
    expect(getCriticalFeatureIconSource('growth-radar')).toBeNull()
    expect(Object.keys(featureIcons)).toHaveLength(criticalKeys.length)
    expect(Object.keys(featureIconSources)).toEqual(Object.keys(featureIcons))
    expect(Object.keys(featureIconLabels)).toEqual(orderedFeatureIconKeys)
  })

  it('should fallback to system notifications for unknown feature icon keys', () => {
    expect(getFeatureIcon('unknown-feature')).toContain('system-notifications.png')
    expect(getFeatureIconSource('unknown-feature').png).toContain('system-notifications.png')
    expect(getFeatureIconSource('unknown-feature').webp).toContain('system-notifications.webp')
  })

  it('should load non-first-screen icons asynchronously with WebP and PNG fallback', async () => {
    const source = await loadFeatureIconSource('growth-radar')
    const unknownSource = await loadFeatureIconSource('unknown-feature')
    const moduleSource = readFileSync(resolve(process.cwd(), 'src/utils/featureIcons.js'), 'utf8')

    expect(source.png).toContain('growth-radar.png')
    expect(source.png).toContain('png-fallback')
    expect(source.webp).toContain('growth-radar.webp')
    expect(unknownSource.png).toContain('system-notifications.png')
    expect(unknownSource.webp).toContain('system-notifications.webp')
    expect(moduleSource).not.toContain("import.meta.glob('/src/assets/feature-icons/{old,new}/*.webp', {\n  eager: true")
    expect(moduleSource).toContain("'/src/assets/feature-icons/{old,new}/*.webp'")
    expect(moduleSource).toContain("'/src/assets/feature-icons/png-fallback/{old,new}/*.png'")
    expect(moduleSource).toContain("'!/src/assets/feature-icons/old/home-dashboard.webp'")
  })
})
