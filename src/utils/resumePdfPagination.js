export const A4_RESUME_PDF_PAGE = Object.freeze({
  width: 210,
  height: 297,
})

/**
 * 计算简历截图写入 PDF 时的分页绘制参数。
 * 核心规则：始终按 A4 内容区宽度等比铺满，再通过负向 y 偏移分页，
 * 避免长简历被整体压进单页后左右留白过大。
 *
 * @param {object} options
 * @param {number} options.canvasWidth - 截图 canvas 宽度（px）
 * @param {number} options.canvasHeight - 截图 canvas 高度（px）
 * @param {number} [options.pageWidth=210] - PDF 页面宽度（mm）
 * @param {number} [options.pageHeight=297] - PDF 页面高度（mm）
 * @param {number} [options.margin=0] - PDF 额外边距（mm）
 * @returns {Array<{x:number,y:number,width:number,height:number,addPage:boolean}>}
 */
export function createResumePdfImagePages({
  canvasWidth,
  canvasHeight,
  pageWidth = A4_RESUME_PDF_PAGE.width,
  pageHeight = A4_RESUME_PDF_PAGE.height,
  margin = 0,
}) {
  if (canvasWidth <= 0 || canvasHeight <= 0) {
    throw new Error('canvas size must be positive')
  }
  if (pageWidth <= margin * 2 || pageHeight <= margin * 2) {
    throw new Error('pdf content size must be positive')
  }

  const contentWidth = pageWidth - margin * 2
  const contentHeight = pageHeight - margin * 2
  const renderWidth = contentWidth
  const renderHeight = (canvasHeight * renderWidth) / canvasWidth
  const pageCount = Math.max(1, Math.ceil(renderHeight / contentHeight - 1e-9))

  return Array.from({ length: pageCount }, (_, index) => ({
    x: margin,
    y: margin - index * contentHeight,
    width: renderWidth,
    height: renderHeight,
    addPage: index > 0,
  }))
}
