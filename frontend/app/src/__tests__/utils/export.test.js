import { describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { exportToXlsx } from '@/utils/export'

const sourceFile = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

const aoaToSheet = vi.fn(() => ({}))
const bookNew = vi.fn(() => ({}))
const bookAppendSheet = vi.fn()
const writeFile = vi.fn()

vi.mock('xlsx', () => ({
  utils: {
    aoa_to_sheet: aoaToSheet,
    book_new: bookNew,
    book_append_sheet: bookAppendSheet
  },
  writeFile
}))

describe('xlsx export loading strategy', () => {
  it('does not statically import xlsx in the export utility', () => {
    const source = sourceFile('src/utils/export.js')

    expect(source).not.toMatch(/import\s+\*\s+as\s+XLSX\s+from\s+['"]xlsx['"]/)
    expect(source).toContain("import('xlsx')")
  })

  it('generates workbook after loading xlsx on demand', async () => {
    await exportToXlsx({
      headers: ['姓名', '职位'],
      rows: [['张三', '前端工程师']],
      filename: '候选人',
      sheetName: '名单'
    })

    expect(aoaToSheet).toHaveBeenCalledWith([
      ['姓名', '职位'],
      ['张三', '前端工程师']
    ])
    expect(bookAppendSheet).toHaveBeenCalledWith(expect.any(Object), expect.any(Object), '名单')
    expect(writeFile).toHaveBeenCalledWith(expect.any(Object), '候选人.xlsx')
  })
})
