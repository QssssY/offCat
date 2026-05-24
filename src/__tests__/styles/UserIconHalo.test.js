import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const globalStyleSource = () =>
  readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')

const headerSource = () =>
  readFileSync(resolve(process.cwd(), 'src/components/AppHeader.vue'), 'utf8')

const homeSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/HomePageView.vue'), 'utf8')

describe('user icon halo global styles', () => {
  it('does not apply halo globally to static user-page icons', () => {
    const source = globalStyleSource()

    expect(source).not.toContain('.main-layout :where(')
    expect(source).not.toContain('--icon-halo-bg')
    expect(source).not.toMatch(/\.main-layout :where\([\s\S]*?notification-type-icon/)
    expect(source).not.toMatch(/\.main-layout :where\([\s\S]*?panel-item-icon/)
    expect(source).not.toMatch(/\.main-layout :where\([\s\S]*?\.item-icon/)
  })

  it('does not add halo or translucent frames to home, navigation, or avatar dropdown icons', () => {
    const header = headerSource()
    const home = homeSource()

    expect(header).not.toMatch(/<FeatureIcon[^>]*\shalo(?:\s|>|=)/)
    expect(header).not.toContain('feature-icon-halo')
    expect(home).not.toMatch(/<FeatureIcon[^>]*\shalo(?:\s|>|=)/)
    expect(home).not.toContain('feature-icon-halo')
    expect(home).not.toMatch(/\.(badge-icon|btn-icon|route-icon|support-icon)[\s\S]{0,220}drop-shadow/)
  })
})
