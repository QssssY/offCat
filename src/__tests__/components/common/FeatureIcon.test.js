import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const componentSource = () =>
  readFileSync(resolve(process.cwd(), 'src/components/common/FeatureIcon.vue'), 'utf8')

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

  it('keeps halo styling transparent, motion-safe, and larger than the old icon scale', () => {
    const source = componentSource()

    expect(source).toContain('halo')
    expect(source).toContain('default: false')
    expect(source).toContain('feature-icon-halo')
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
})
