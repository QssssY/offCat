import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { optimizedImages, toCssImageSet } from '@/utils/optimizedImages'

const asset = (path) => resolve(process.cwd(), `src/assets/${path}`)

describe('optimizedImages', () => {
  it('keeps WebP sources for large images with PNG fallback assets', () => {
    const pairs = [
      ['background.png', 'optimized/background-desktop.webp'],
      ['background.png', 'optimized/background-mobile.webp'],
      ['logo.png', 'optimized/logo.webp'],
      ['assistant.png', 'optimized/assistant.webp'],
      ['user.png', 'optimized/user.webp']
    ]

    for (const [png, webp] of pairs) {
      expect(existsSync(asset(png))).toBe(true)
      expect(existsSync(asset(webp))).toBe(true)
      expect(statSync(asset(webp)).size).toBeLessThan(statSync(asset(png)).size)
    }

    expect(optimizedImages.logo.webp).toContain('logo.webp')
    expect(optimizedImages.logo.png).toContain('logo.png')
    expect(optimizedImages.assistantAvatar.webp).toContain('assistant.webp')
    expect(optimizedImages.assistantAvatar.png).toContain('assistant.png')
    expect(optimizedImages.userAvatar.webp).toContain('user.webp')
    expect(optimizedImages.userAvatar.png).toContain('user.png')
  })

  it('builds CSS image-set values for WebP first and PNG fallback second', () => {
    const value = toCssImageSet('/assets/background-desktop.webp', '/assets/background.png')

    expect(value).toContain('type("image/webp")')
    expect(value).toContain('type("image/png")')
    expect(value.indexOf('background-desktop.webp')).toBeLessThan(value.indexOf('background.png'))
  })

  it('keeps the picture wrapper from changing existing image layout boxes', () => {
    const source = readFileSync(
      resolve(process.cwd(), 'src/components/common/OptimizedImage.vue'),
      'utf8'
    )

    expect(source).toContain('optimized-picture')
    expect(source).toMatch(/\.optimized-picture\s*\{[\s\S]*?display:\s*contents/)
  })

  it('keeps scoped callers responsible for sizing optimized image internals', () => {
    const sources = [
      readFileSync(resolve(process.cwd(), 'src/components/AppHeader.vue'), 'utf8'),
      readFileSync(resolve(process.cwd(), 'src/views/auth/LoginView.vue'), 'utf8'),
      readFileSync(resolve(process.cwd(), 'src/views/interview/InterviewSessionView.vue'), 'utf8'),
      readFileSync(resolve(process.cwd(), 'src/views/settings/SettingsView.vue'), 'utf8')
    ].join('\n')

    expect(sources).toContain('.logo-box :deep(.logo-img)')
    expect(sources).toContain('.brand-logo-link :deep(.brand-logo)')
    expect(sources).toContain('.message-avatar :deep(img)')
    expect(sources).toContain('.voice-avatar-wrap :deep(.voice-avatar)')
    expect(sources).toContain('.profile-summary :deep(.profile-avatar)')
  })
})
