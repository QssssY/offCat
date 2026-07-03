/** 导出时的 DOM 清理和降级辅助函数。 */

export function sanitizeRichTextClone(rootNode) {
  rootNode.querySelectorAll('[contenteditable]').forEach((node) => {
    node.removeAttribute('contenteditable')
    node.removeAttribute('role')
    node.removeAttribute('tabindex')
  })

  rootNode.querySelectorAll('.ProseMirror-focused, .has-focus, .is-active, .is-focused').forEach((node) => {
    node.classList.remove('ProseMirror-focused', 'has-focus', 'is-active', 'is-focused')
  })
}

export function copyScopedAttributes(sourceNode, targetNode) {
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

export function replaceFormFieldWithStaticText(rootNode) {
  rootNode.querySelectorAll('input, textarea').forEach((fieldNode) => {
    fieldNode.replaceWith(createStaticFieldNode(fieldNode))
  })
}

export function replacePhotoFrameButtonWithStaticNode(rootNode) {
  rootNode.querySelectorAll('.photo-frame--button').forEach((buttonNode) => {
    const nextNode = buttonNode.ownerDocument.createElement('div')
    copyScopedAttributes(buttonNode, nextNode)
    nextNode.className = buttonNode.className
    buttonNode.childNodes.forEach((childNode) => {
      nextNode.appendChild(childNode.cloneNode(true))
    })
    buttonNode.replaceWith(nextNode)
  })
}

export function applyExportSafeBackgrounds(rootNode) {
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

const EXPORT_REMOVE_SELECTORS = [
  '.drag-handle',
  '.editor-ghost-btn',
  '.profile-meta-tools',
  '.photo-input',
  '.photo-actions',
  '.block-drop-indicator',
  '.section-drop-tail',
  '.inline-rich-placeholder',
].join(', ')

export function cleanExportClone(clone) {
  clone.classList.remove('resume-template--preview')
  clone.classList.add('resume-template--print')
  clone.querySelector('.editor-toolbar')?.remove()
  clone.querySelectorAll(EXPORT_REMOVE_SELECTORS).forEach((node) => node.remove())
  sanitizeRichTextClone(clone)
  replaceFormFieldWithStaticText(clone)
  replacePhotoFrameButtonWithStaticNode(clone)
  applyExportSafeBackgrounds(clone)
  clone.querySelector('.photo-tip')?.remove()
  return clone
}
