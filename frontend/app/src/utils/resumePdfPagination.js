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
  minSinglePageScale = 0.85,
}) {
  if (canvasWidth <= 0 || canvasHeight <= 0) {
    throw new Error('canvas size must be positive')
  }
  if (pageWidth <= margin * 2 || pageHeight <= margin * 2) {
    throw new Error('pdf content size must be positive')
  }
  if (minSinglePageScale <= 0 || minSinglePageScale > 1) {
    throw new Error('single page scale must be within (0, 1]')
  }

  const contentWidth = pageWidth - margin * 2
  const contentHeight = pageHeight - margin * 2
  let renderWidth = contentWidth
  let renderHeight = (canvasHeight * renderWidth) / canvasWidth

  const singlePageScale = contentHeight / renderHeight
  if (singlePageScale < 1 && singlePageScale >= minSinglePageScale) {
    // 接近一页的简历优先轻微缩小并居中，避免少量尾部内容被硬切到第二页。
    renderWidth = contentWidth * singlePageScale
    renderHeight = contentHeight
  }

  const x = margin + (contentWidth - renderWidth) / 2
  const pageCount = Math.max(1, Math.ceil(renderHeight / contentHeight - 1e-9))

  return Array.from({ length: pageCount }, (_, index) => ({
    x,
    y: margin - index * contentHeight,
    width: renderWidth,
    height: renderHeight,
    addPage: index > 0,
  }))
}
