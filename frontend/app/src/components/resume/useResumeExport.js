/**
 * 简历模板导出逻辑
 * 负责 DOM 克隆、静态化替换、纯文本提取等导出相关操作
 */
export function useResumeExport({ resumeRef, header, sections, clearActiveState }) {
  function stripHtmlToText(html) {
    if (!html) return ''
    const wrapper = document.createElement('div')
    wrapper.innerHTML = html
    wrapper.querySelectorAll('br').forEach((node) => {
      node.replaceWith('\n')
    })
    return wrapper.textContent?.replace(/ /g, ' ').trim() || ''
  }

  function readHeaderFieldText(field) {
    return stripHtmlToText(field?.html || '')
  }

  function buildLabelPlainText(block) {
    return `${block.label || ''}${block.value || ''}`.trim()
  }

  function sanitizeRichTextClone(rootNode) {
    rootNode.querySelectorAll('[contenteditable]').forEach((node) => {
      node.removeAttribute('contenteditable')
      node.removeAttribute('role')
      node.removeAttribute('tabindex')
    })
    rootNode.querySelectorAll('.ProseMirror-focused, .has-focus, .is-active, .is-focused').forEach((node) => {
      node.classList.remove('ProseMirror-focused', 'has-focus', 'is-active', 'is-focused')
    })
  }

  function copyScopedAttributes(sourceNode, targetNode) {
    Array.from(sourceNode.attributes).forEach((attribute) => {
      if (attribute.name.startsWith('data-v-')) {
        targetNode.setAttribute(attribute.name, attribute.value)
      }
    })
  }

  function createStaticFieldNode(fieldNode) {
    const nextNode = fieldNode.ownerDocument.createElement('div')
    copyScopedAttributes(fieldNode, nextNode)
    nextNode.className = `${fieldNode.className} export-static-field`.trim()
    nextNode.classList.remove('resume-inline-input', 'resume-textarea-input')
    if (fieldNode.getAttribute('style')) {
      nextNode.setAttribute('style', fieldNode.getAttribute('style'))
    }
    nextNode.textContent = fieldNode.value || fieldNode.placeholder || ''
    return nextNode
  }

  function replaceFormFieldWithStaticText(rootNode) {
    rootNode.querySelectorAll('input, textarea').forEach((fieldNode) => {
      fieldNode.replaceWith(createStaticFieldNode(fieldNode))
    })
  }

  function replacePhotoFrameButtonWithStaticNode(rootNode) {
    rootNode.querySelectorAll('.photo-frame--button').forEach((buttonNode) => {
      const nextNode = buttonNode.ownerDocument.createElement('div')
      copyScopedAttributes(buttonNode, nextNode)
      nextNode.className = buttonNode.className
      nextNode.innerHTML = buttonNode.innerHTML
      buttonNode.replaceWith(nextNode)
    })
  }

  function applyExportSafeBackgrounds(rootNode) {
    rootNode.querySelectorAll('.section-tab').forEach((node) => {
      node.style.backgroundImage = 'none'
      node.style.backgroundColor = '#edf4f2'
    })
    rootNode.querySelectorAll('.section-line').forEach((node) => {
      node.style.backgroundImage = 'none'
      node.style.backgroundColor = '#d8ddd8'
    })
    rootNode.querySelectorAll('.photo-frame, .photo-placeholder').forEach((node) => {
      node.style.backgroundImage = 'none'
      node.style.backgroundColor = '#f3f6f5'
    })
  }

  function buildExportElement() {
    clearActiveState()

    if (!resumeRef.value) {
      return null
    }

    const clone = resumeRef.value.cloneNode(true)
    clone.classList.remove('resume-template--preview')
    clone.classList.add('resume-template--print')
    clone.querySelector('.editor-toolbar')?.remove()
    clone
      .querySelectorAll(
        '.drag-handle, .editor-ghost-btn, .profile-meta-tools, .photo-input, .photo-actions, .block-drop-indicator, .section-drop-tail, .inline-rich-placeholder',
      )
      .forEach((node) => node.remove())

    sanitizeRichTextClone(clone)
    replaceFormFieldWithStaticText(clone)
    replacePhotoFrameButtonWithStaticNode(clone)
    applyExportSafeBackgrounds(clone)
    clone.querySelector('.photo-tip')?.remove()

    return clone
  }

  function getResumePlainText() {
    const lines = []

    if (header.value.sectionTitle?.trim()) {
      lines.push(header.value.sectionTitle.trim())
    }

    const name = readHeaderFieldText(header.value.name)
    if (name) lines.push(name)

    const jobTarget = readHeaderFieldText(header.value.jobTarget)
    if (jobTarget) lines.push(jobTarget)

    const profileMeta = header.value.metaItems.map((item) => readHeaderFieldText(item)).filter(Boolean)
    if (profileMeta.length) lines.push(profileMeta.join(' | '))

    header.value.summaryLines
      .map((item) => readHeaderFieldText(item))
      .filter(Boolean)
      .forEach((item) => lines.push(item))

    sections.value.forEach((section) => {
      if (section.title?.trim()) {
        lines.push('')
        lines.push(section.title.trim())
      }

      section.blocks.forEach((block) => {
        if (block.type === 'banner_title') {
          const title = block.title?.trim() || ''
          if (title) {
            lines.push('')
            lines.push(title)
          }
          return
        }
        if (block.type === 'row') {
          const rowText = block.items.map((item) => item.value.trim()).filter(Boolean).join(' | ')
          if (rowText) lines.push(rowText)
          return
        }
        if (block.type === 'label') {
          const labelText = buildLabelPlainText(block)
          if (labelText) lines.push(labelText)
          return
        }
        const text = stripHtmlToText(block.html)
        if (!text) return
        if (block.variant === 'bullet') {
          lines.push(`- ${text}`)
          return
        }
        if (block.type === 'section_title') lines.push('')
        lines.push(text)
      })
    })

    return lines.join('\n').trim()
  }

  function getResumeName() {
    return readHeaderFieldText(header.value.name)
  }

  return {
    buildExportElement,
    getResumePlainText,
    getResumeName,
    /** 导出供组件侧使用的工具方法 */
    stripHtmlToText,
    readHeaderFieldText,
    buildLabelPlainText,
  }
}
