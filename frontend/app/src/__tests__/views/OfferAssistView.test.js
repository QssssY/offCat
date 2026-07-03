import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const source = () => readFileSync(resolve(process.cwd(), 'src/views/offer/OfferAssistView.vue'), 'utf8')

describe('OfferAssistView', () => {
  it('keeps the result panel aligned with the input panel and scrolls long results internally', () => {
    const viewSource = source()

    expect(viewSource).toContain('class="result-scroll"')
    expect(viewSource).toContain('ref="inputPanelRef"')
    expect(viewSource).toContain(':style="outputPanelStyle"')
    expect(viewSource).toContain('ResizeObserver')
    expect(viewSource).toContain('height: `${inputPanelHeight.value}px`')
    expect(viewSource).toMatch(/\.workbench\s*\{[\s\S]*?align-items:\s*stretch/)
    expect(viewSource).toMatch(/\.output-panel\s*\{[\s\S]*?display:\s*flex[\s\S]*?flex-direction:\s*column[\s\S]*?overflow:\s*hidden/)
    expect(viewSource).toMatch(/\.result-scroll\s*\{[\s\S]*?display:\s*flex[\s\S]*?flex-direction:\s*column[\s\S]*?flex:\s*1\s+1\s+auto[\s\S]*?min-height:\s*0[\s\S]*?overflow-y:\s*auto/)
    expect(viewSource).toMatch(/@media\s*\(max-width:\s*1100px\)\s*\{[\s\S]*?\.result-scroll\s*\{[\s\S]*?overflow:\s*visible/)
  })
})
