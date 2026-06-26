import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { readdirSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const componentSource = () =>
  readFileSync(resolve(process.cwd(), 'src/components/common/FeatureIcon.vue'), 'utf8')

const appSource = () =>
  [
    readFileSync(resolve(process.cwd(), 'index.html'), 'utf8'),
    readFileSync(resolve(process.cwd(), 'src/components/AppHeader.vue'), 'utf8'),
    readFileSync(resolve(process.cwd(), 'src/views/HomePageView.vue'), 'utf8')
  ].join('\n')

describe('FeatureIcon', () => {
  it('renders readable icon sizes without adding halo to static icons by default', () => {
    const mediumIcon = mount(FeatureIcon, {
      props: {
        name: 'resume-analysis',
        size: 'md'
      }
    })

    expect(mediumIcon.find('.feature-icon').classes()).toContain('size-md')
    expect(mediumIcon.find('.feature-icon').classes()).not.toContain('feature-icon-halo')
    expect(mediumIcon.find('picture').exists()).toBe(true)
    expect(mediumIcon.find('source[type="image/webp"]').exists()).toBe(true)
    expect(mediumIcon.find('img').attributes('loading')).toBe('lazy')
    expect(mediumIcon.find('img').attributes('decoding')).toBe('async')
    expect(mediumIcon.find('img').attributes('fetchpriority')).toBeUndefined()
  })

  it('adds a soft halo only when an interactive parent explicitly requests it', () => {
    const interactiveIcon = mount(FeatureIcon, {
      props: {
        name: 'resume-analysis',
        size: 'sm',
        halo: true
      }
    })

    expect(interactiveIcon.find('.feature-icon').classes()).toContain('size-sm')
    expect(interactiveIcon.find('.feature-icon').classes()).toContain('feature-icon-halo')
  })

  it('promotes critical above-the-fold icons without changing normal defaults', () => {
    const criticalIcon = mount(FeatureIcon, {
      props: {
        name: 'home-dashboard',
        critical: true
      }
    })

    expect(criticalIcon.find('img').attributes('loading')).toBe('eager')
    expect(criticalIcon.find('img').attributes('fetchpriority')).toBe('high')
  })

  it('allows callers to override loading priority explicitly', () => {
    const lowPriorityIcon = mount(FeatureIcon, {
      props: {
        name: 'notification-center',
        loading: 'eager',
        fetchPriority: 'low'
      }
    })

    expect(lowPriorityIcon.find('img').attributes('loading')).toBe('eager')
    expect(lowPriorityIcon.find('img').attributes('fetchpriority')).toBe('low')
  })

  it('loads non-critical feature icon sources asynchronously without high priority', async () => {
    const lazyIcon = mount(FeatureIcon, {
      props: {
        name: 'resume-score'
      }
    })

    expect(lazyIcon.find('img').attributes('loading')).toBe('lazy')
    expect(lazyIcon.find('img').attributes('fetchpriority')).toBeUndefined()

    await flushPromises()
    await vi.waitFor(() => {
      expect(lazyIcon.find('img').attributes('src')).toContain('resume-score.png')
    })

    expect(lazyIcon.find('source[type="image/webp"]').attributes('srcset')).toContain('resume-score.webp')
  })

  it('renders loading.webp immediately for loading states instead of the notification fallback', () => {
    const loadingIcon = mount(FeatureIcon, {
      props: {
        name: 'loading'
      }
    })

    expect(loadingIcon.find('img').attributes('src')).toContain('loading.png')
    expect(loadingIcon.find('source[type="image/webp"]').attributes('srcset')).toContain('loading.webp')
    expect(loadingIcon.find('img').attributes('src')).not.toContain('system-notifications')
  })

  it('keeps halo styling transparent, motion-safe, and larger than the old icon scale', () => {
    const source = componentSource()

    expect(source).toContain('halo')
    expect(source).toContain('default: false')
    expect(source).toContain('feature-icon-halo')
    expect(source).toContain('getCriticalFeatureIconSource')
    expect(source).toContain('loadFeatureIconSource')
    expect(source).toContain('type="image/webp"')
    expect(source).toContain('critical')
    expect(source).toContain('fetchPriority')
    expect(source).toContain('::before')
    expect(source).toContain('prefers-reduced-motion: reduce')
    expect(source).not.toContain("['md', 'lg', 'xl'].includes(props.size)")
    expect(source).not.toMatch(/\.feature-icon\s*\{[\s\S]*?filter:/)
    expect(source).toMatch(/\.size-xs\s*\{[\s\S]*?width:\s*22px/)
    expect(source).toMatch(/\.size-sm\s*\{[\s\S]*?width:\s*28px/)
    expect(source).toMatch(/\.size-md\s*\{[\s\S]*?width:\s*40px/)
    expect(source).toMatch(/\.size-lg\s*\{[\s\S]*?width:\s*64px/)
    expect(source).toMatch(/\.size-xl\s*\{[\s\S]*?width:\s*88px/)
    expect(source).not.toContain('transition: all')
  })

  it('keeps WebP assets available without preloading the whole feature icon library', () => {
    const newIconWebps = readdirSync(resolve(process.cwd(), 'src/assets/feature-icons/new'))
      .filter((name) => name.endsWith('.webp'))
    const oldIconWebps = readdirSync(resolve(process.cwd(), 'src/assets/feature-icons/old'))
      .filter((name) => name.endsWith('.webp'))
    const newMixedPngs = readdirSync(resolve(process.cwd(), 'src/assets/feature-icons/new'))
      .filter((name) => name.endsWith('.png'))
    const oldMixedPngs = readdirSync(resolve(process.cwd(), 'src/assets/feature-icons/old'))
      .filter((name) => name.endsWith('.png'))
    const fallbackNewPngs = readdirSync(resolve(process.cwd(), 'src/assets/feature-icons/png-fallback/new'))
      .filter((name) => name.endsWith('.png'))
    const fallbackOldPngs = readdirSync(resolve(process.cwd(), 'src/assets/feature-icons/png-fallback/old'))
      .filter((name) => name.endsWith('.png'))
    const source = appSource()

    expect(newIconWebps.length + oldIconWebps.length).toBe(94)
    expect(newMixedPngs.length + oldMixedPngs.length).toBe(0)
    expect(fallbackNewPngs.length + fallbackOldPngs.length).toBe(94)
    expect(source).not.toMatch(/rel=["']preload["'][^>]*feature-icons/)
    expect(source).not.toMatch(/feature-icons[^>]*rel=["']preload["']/)
  })
})
