import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('ExportToolbar', () => {
  it('loads heavy export dependencies only when export actions run', () => {
    const source = sourceFile('src/components/template/ExportToolbar.vue')

    expect(source).not.toContain("import html2canvas from 'html2canvas'")
    expect(source).not.toContain("import jsPDF from 'jspdf'")
    expect(source).toContain("await import('html2canvas')")
    expect(source).toContain("await import('jspdf')")
  })
})
