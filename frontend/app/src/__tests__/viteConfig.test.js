import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('vite config', () => {
  it('pre-optimizes heavy admin dependencies in dev mode to avoid route-triggered reloads', () => {
    const source = sourceFile('vite.config.js')

    expect(source).toContain('optimizeDeps')
    expect(source).toContain('include')
    expect(source).toContain("'chart.js'")
    expect(source).toContain("'vue-chartjs'")
    expect(source).toContain("'element-plus'")
    expect(source).toContain("'@element-plus/icons-vue'")
    expect(source).toContain("'naive-ui'")
  })

  it('keeps xlsx out of dev pre-optimization so exports load it only on demand', () => {
    const source = sourceFile('vite.config.js')
    const optimizeDepsBlock = source.match(/optimizeDeps:\s*{[\s\S]*?},\n  test:/)?.[0] || ''

    expect(optimizeDepsBlock).not.toContain("'xlsx'")
  })
})
