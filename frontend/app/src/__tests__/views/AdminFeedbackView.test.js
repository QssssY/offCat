import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = readFileSync(
  resolve(process.cwd(), 'src/views/admin/AdminFeedbackView.vue'),
  'utf8'
)

describe('AdminFeedbackView', () => {
  it('should keep feedback detail dialog fixed and only scroll feedback content', () => {
    expect(source).toContain('class="feedback-detail-dialog"')
    expect(source).toContain('detail-scroll-sections')
    expect(source).toContain('scroll-content-box')
    expect(source).toContain('feedback-content-box')
    expect(source).toContain('remark-content-box')
    expect(source).toContain(':global(.el-overlay:has(.feedback-detail-dialog))')
    expect(source).toContain(':global(.el-overlay-dialog:has(.feedback-detail-dialog))')
    expect(source).toContain('.feedback-detail-dialog :deep(.el-dialog)')
    expect(source).toContain('margin: 0 auto !important;')
    expect(source).toContain('overflow: hidden;')
    expect(source).toContain('.feedback-detail-dialog :deep(.el-dialog__body)')
    expect(source).toContain('padding: 20px 0 44px;')
    expect(source).toContain('display: flex;')
    expect(source).toContain('max-height: calc(100dvh - 64px);')
    expect(source).toContain('.feedback-content-box { max-height: clamp(140px, 26vh, 280px); }')
    expect(source).toContain('.remark-content-box { max-height: clamp(88px, 15vh, 170px); }')
    expect(source).toContain('overflow-y: auto;')
  })
})
