import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const mainSource = () => readFileSync(resolve(process.cwd(), 'src/main.js'), 'utf8')

const expectCssImport = (source, cssPath) => {
  expect(source).toMatch(new RegExp(`^import '${cssPath}'$`, 'm'))
}

describe('Element Plus service styles', () => {
  it('keeps service component styles imported when using Element Plus on demand', () => {
    const source = mainSource()

    expectCssImport(source, 'element-plus/es/components/message/style/css')
    expectCssImport(source, 'element-plus/es/components/message-box/style/css')
  })

  it('keeps non Element Plus global css imports executable', () => {
    const source = mainSource()

    expectCssImport(source, 'vue-virtual-scroller/dist/vue-virtual-scroller.css')
  })
})
