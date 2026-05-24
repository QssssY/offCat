import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const styleSource = () => readFileSync(
  join(process.cwd(), 'src/styles/index.css'),
  'utf-8'
)

const sourceFile = (path) => readFileSync(
  join(process.cwd(), path),
  'utf-8'
)

describe('dark theme tokens', () => {
  it('should use warm brown global dark tokens instead of the old cool palette', () => {
    const source = styleSource()

    expect(source).toContain('--bg-page: #1F1511;')
    expect(source).toContain('--bg-card: #2A1B14;')
    expect(source).toContain('--bg-card-hover: #332116;')
    expect(source).toContain('--text-title: #FFF3E8;')
    expect(source).toContain('--text-body: #F0D1BD;')
    expect(source).toContain('--text-muted: #CAA189;')

    expect(source).not.toContain('#1a1a2e')
    expect(source).not.toContain('#22223b')
    expect(source).not.toContain('#2a2a40')
    expect(source).not.toContain('#3A3A50')
  })

  it('should align Element Plus dark bridge with the same warm palette', () => {
    const source = styleSource()

    expect(source).toContain('--el-bg-color: #1F1511;')
    expect(source).toContain('--el-bg-color-overlay: #2A1B14;')
    expect(source).toContain('--el-bg-color-page: #1F1511;')
    expect(source).toContain('--el-text-color-primary: #FFF3E8;')
    expect(source).toContain('--el-text-color-regular: #F0D1BD;')
    expect(source).toContain('--el-text-color-secondary: #CAA189;')
  })

  it('should remove old cool dark hardcodes from shared user-facing surfaces', () => {
    const sources = [
      sourceFile('src/components/resume/RadarChart.vue'),
      sourceFile('src/components/resume/LineChart.vue'),
      sourceFile('src/components/OnboardingGuide.vue')
    ].join('\n')

    expect(sources).toContain('#1F1511')
    expect(sources).toContain('#FFF3E8')
    expect(sources).toContain('#CAA189')
    expect(sources).not.toContain('#1a1a2e')
    expect(sources).not.toContain('#7A7A90')
    expect(sources).not.toContain('#E8E8F0')
  })

  it('should keep admin dark mode on a neutral local Element Plus bridge', () => {
    const source = sourceFile('src/layouts/AdminLayout.vue')
    const adminDarkBlock = source.match(/:global\(html\[data-theme="dark"\]\s+\.admin-layout\)\s*\{[\s\S]*?\n\}/)?.[0] || ''

    expect(adminDarkBlock).toContain('--bg-page: #171D26;')
    expect(adminDarkBlock).toContain('--bg-card: #202838;')
    expect(adminDarkBlock).toContain('--el-bg-color: #171D26;')
    expect(adminDarkBlock).toContain('--el-bg-color-overlay: #202838;')
    expect(adminDarkBlock).toContain('--el-text-color-primary: #F2F5FA;')
    expect(adminDarkBlock).not.toContain('#1F1511')
    expect(adminDarkBlock).not.toContain('#2A1B14')
    expect(adminDarkBlock).not.toContain('#332116')
  })
})
