import DOMPurify from 'dompurify'

/**
 * 简历结构化模型 → DOCX 导出
 * 从 ResumeTemplate 的 block-based JSON 模型生成可编辑 Word 文件。
 */

// DOCX 样式常量
const FONT_FAMILY = 'Microsoft YaHei'
const COLOR_BODY = '1f2933'
const COLOR_HEADING = '1b5b57'
const COLOR_META = '52606d'
const SIZE_NAME = 56        // 28pt（half-points）
const SIZE_SECTION = 28     // 14pt
const SIZE_BODY = 22        // 11pt
const SIZE_META = 20        // 10pt
const SIZE_SUBHEADING = 24  // 12pt

// DOCX 只需要基础富文本标签，写入 detached DOM 前先消毒，避免事件属性或脚本节点被解析。
function sanitizeDocxHtml(html) {
  return DOMPurify.sanitize(String(html || ''), {
    ALLOWED_TAGS: ['p', 'strong', 'b', 'em', 'i', 'br', 'span', 'ul', 'ol', 'li'],
    ALLOWED_ATTR: [],
  })
}

/**
 * 解析 HTML 字符串为 docx TextRun 配置数组。
 * 处理 <strong>/<b>/<em>/<i>/<br> 等基础富文本标签。
 * @param {string} html - 富文本 HTML
 * @returns {Array<Object>} TextRun 配置对象数组
 */
export function parseHtmlToDocxRuns(html) {
  if (!html) return [{ text: '', font: FONT_FAMILY, size: SIZE_BODY, color: COLOR_BODY }]

  const wrapper = document.createElement('div')
  wrapper.innerHTML = sanitizeDocxHtml(html)

  const runs = []
  collectRuns(wrapper, { bold: false, italic: false }, runs)

  if (runs.length === 0) {
    return [{ text: '', font: FONT_FAMILY, size: SIZE_BODY, color: COLOR_BODY }]
  }
  return runs
}

// 递归遍历 DOM 节点收集 TextRun 配置
function collectRuns(node, style, runs) {
  for (const child of node.childNodes) {
    if (child.nodeType === 3) {
      const text = child.textContent.replace(/ /g, ' ')
      if (text) {
        runs.push({
          text,
          bold: style.bold,
          italics: style.italic,
          font: FONT_FAMILY,
          size: SIZE_BODY,
          color: COLOR_BODY,
        })
      }
      continue
    }

    if (child.nodeType !== 1) continue

    const tag = child.tagName.toLowerCase()

    if (tag === 'br') {
      runs.push({ break: 1 })
      continue
    }

    const nextStyle = { ...style }
    if (tag === 'strong' || tag === 'b') nextStyle.bold = true
    if (tag === 'em' || tag === 'i') nextStyle.italic = true

    collectRuns(child, nextStyle, runs)
  }
}

/**
 * 将 header 对象转为段落配置数组
 */
function convertHeaderToParagraphs(header) {
  const paragraphs = []

  // 姓名：居中 Heading1
  const nameText = stripHtml(header.name?.html)
  if (nameText) {
    paragraphs.push({
      _type: 'heading',
      level: 'HEADING_1',
      alignment: 'CENTER',
      children: [{ text: nameText, bold: true, font: FONT_FAMILY, size: SIZE_NAME, color: COLOR_BODY }],
      spacing: { after: 80 },
    })
  }

  // 求职意向：居中
  const jobTargetText = stripHtml(header.jobTarget?.html)
  if (jobTargetText) {
    paragraphs.push({
      alignment: 'CENTER',
      children: [{ text: jobTargetText, font: FONT_FAMILY, size: SIZE_SUBHEADING, color: COLOR_META }],
      spacing: { after: 60 },
    })
  }

  // 联系方式：metaItems 用 " | " 连接居中
  const metaTexts = (header.metaItems || [])
    .map(item => stripHtml(item?.html))
    .filter(Boolean)
  if (metaTexts.length) {
    paragraphs.push({
      alignment: 'CENTER',
      children: [{ text: metaTexts.join('  |  '), font: FONT_FAMILY, size: SIZE_META, color: COLOR_META }],
      spacing: { after: 80 },
    })
  }

  // 个人总结行
  const summaryLines = (header.summaryLines || [])
    .map(item => stripHtml(item?.html))
    .filter(Boolean)
  summaryLines.forEach(line => {
    paragraphs.push({
      children: [{ text: line, font: FONT_FAMILY, size: SIZE_BODY, color: COLOR_BODY }],
      spacing: { after: 40 },
    })
  })

  // 分隔线（底部边框模拟）
  if (paragraphs.length) {
    paragraphs.push({
      children: [{ text: '', font: FONT_FAMILY, size: 4 }],
      border: { bottom: { style: 'SINGLE', size: 6, color: 'cccccc' } },
      spacing: { after: 120 },
    })
  }

  return paragraphs
}

/**
 * 将单个 block 转为段落配置数组（一个 block 可能产生多个段落）
 */
export function convertBlockToParagraphs(block) {
  if (!block) return []

  switch (block.type) {
    case 'banner_title':
      return [{
        _type: 'heading',
        level: 'HEADING_2',
        children: [{ text: block.title || '', bold: true, font: FONT_FAMILY, size: SIZE_SECTION, color: COLOR_HEADING }],
        border: { bottom: { style: 'SINGLE', size: 6, color: COLOR_HEADING } },
        spacing: { before: 200, after: 100 },
      }]

    case 'section_title':
      return [{
        _type: 'heading',
        level: 'HEADING_3',
        children: parseHtmlToDocxRuns(block.html).map(r => ({
          ...r,
          bold: true,
          size: SIZE_SUBHEADING,
          color: COLOR_HEADING,
        })),
        spacing: { before: 160, after: 60 },
      }]

    case 'text':
      if (block.variant === 'bullet') {
        return [{
          bullet: { level: 0 },
          children: parseHtmlToDocxRuns(block.html),
          spacing: { after: 40 },
        }]
      }
      if (block.variant === 'heading') {
        return [{
          children: parseHtmlToDocxRuns(block.html).map(r => ({
            ...r,
            bold: true,
            size: SIZE_SUBHEADING,
          })),
          spacing: { before: 120, after: 60 },
        }]
      }
      return [{
        children: parseHtmlToDocxRuns(block.html),
        spacing: { after: 80 },
      }]

    case 'row': {
      const rowText = (block.items || [])
        .map(item => (item.value || '').trim())
        .filter(Boolean)
        .join('  |  ')
      if (!rowText) return []
      return [{
        children: [{ text: rowText, font: FONT_FAMILY, size: SIZE_BODY, color: COLOR_BODY }],
        spacing: { after: 60 },
      }]
    }

    case 'label':
      return [{
        children: [
          { text: (block.label || '') + ' ', bold: true, font: FONT_FAMILY, size: SIZE_BODY, color: COLOR_BODY },
          { text: block.value || '', font: FONT_FAMILY, size: SIZE_BODY, color: COLOR_BODY },
        ],
        spacing: { after: 60 },
      }]

    default:
      return []
  }
}

/**
 * 将完整 section 转为段落配置数组
 */
function convertSectionToParagraphs(section) {
  const paragraphs = []

  // section 标题 → Heading2
  if (section.title?.trim()) {
    paragraphs.push({
      _type: 'heading',
      level: 'HEADING_2',
      children: [{ text: section.title.trim(), bold: true, font: FONT_FAMILY, size: SIZE_SECTION, color: COLOR_HEADING }],
      border: { bottom: { style: 'SINGLE', size: 6, color: COLOR_HEADING } },
      spacing: { before: 240, after: 100 },
    })
  }

  // 遍历 blocks
  for (const block of (section.blocks || [])) {
    paragraphs.push(...convertBlockToParagraphs(block))
  }

  return paragraphs
}

/**
 * 主导出函数：将结构化 JSON → .docx Blob → 触发浏览器下载
 * @param {string} jsonString - ResumeTemplate.getResumeDocumentJson() 返回的 JSON 字符串
 * @param {string} filename - 下载文件名（不含扩展名）
 * @throws {Error} 数据异常时抛出中文错误
 */
export async function exportResumeToDocx(jsonString, filename) {
  if (!jsonString || jsonString === '{}') {
    throw new Error('缺少结构化简历数据，请先保存编辑内容后重试')
  }

  let model
  try {
    model = JSON.parse(jsonString)
  } catch {
    throw new Error('简历数据格式异常，无法生成 Word 文件')
  }

  if (!model.header && (!model.sections || model.sections.length === 0)) {
    throw new Error('简历内容为空，无法生成 Word 文件')
  }

  // 收集所有段落配置
  const paragraphConfigs = []

  if (model.header) {
    paragraphConfigs.push(...convertHeaderToParagraphs(model.header))
  }

  for (const section of (model.sections || [])) {
    paragraphConfigs.push(...convertSectionToParagraphs(section))
  }

  if (paragraphConfigs.length === 0) {
    throw new Error('简历内容为空，无法生成 Word 文件')
  }

  // 动态导入 docx 库，利用 Vite code splitting
  const {
    Document, Paragraph, TextRun, HeadingLevel, AlignmentType,
    Packer, BorderStyle, NumberFormat,
  } = await import('docx')

  const headingMap = {
    HEADING_1: HeadingLevel.HEADING_1,
    HEADING_2: HeadingLevel.HEADING_2,
    HEADING_3: HeadingLevel.HEADING_3,
  }

  const alignmentMap = {
    CENTER: AlignmentType.CENTER,
  }

  // 将配置对象转为真实 docx 对象
  const children = paragraphConfigs.map(config => {
    const runs = (config.children || []).map(runConfig => {
      if (runConfig.break) return new TextRun({ break: runConfig.break })
      return new TextRun({
        text: runConfig.text || '',
        bold: runConfig.bold || false,
        italics: runConfig.italics || false,
        font: runConfig.font || FONT_FAMILY,
        size: runConfig.size || SIZE_BODY,
        color: runConfig.color || COLOR_BODY,
      })
    })

    const paragraphOptions = { children: runs }

    if (config._type === 'heading' && config.level) {
      paragraphOptions.heading = headingMap[config.level]
    }
    if (config.alignment) {
      paragraphOptions.alignment = alignmentMap[config.alignment]
    }
    if (config.spacing) {
      paragraphOptions.spacing = config.spacing
    }
    if (config.bullet) {
      paragraphOptions.bullet = config.bullet
    }
    if (config.border) {
      const border = {}
      if (config.border.bottom) {
        border.bottom = {
          style: BorderStyle.SINGLE,
          size: config.border.bottom.size || 6,
          color: config.border.bottom.color || 'cccccc',
        }
      }
      paragraphOptions.border = border
    }

    return new Paragraph(paragraphOptions)
  })

  const doc = new Document({
    sections: [{
      properties: {
        page: {
          margin: { top: 720, bottom: 720, left: 720, right: 720 },
          size: { width: 11906, height: 16838 },
          pageNumbers: { start: 1, formatType: NumberFormat.DECIMAL },
        },
      },
      children,
    }],
  })

  const blob = await Packer.toBlob(doc)

  // 触发浏览器下载
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename || 'resume'}.docx`
  try {
    link.click()
  } finally {
    URL.revokeObjectURL(url)
  }
}

// 辅助：从 HTML 中提取纯文本
function stripHtml(html) {
  if (!html) return ''
  const wrapper = document.createElement('div')
  wrapper.innerHTML = sanitizeDocxHtml(html)
  wrapper.querySelectorAll('br').forEach(node => node.replaceWith('\n'))
  return wrapper.textContent?.replace(/ /g, ' ').trim() || ''
}
