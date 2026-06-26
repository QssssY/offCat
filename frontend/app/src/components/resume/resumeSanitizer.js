import DOMPurify from 'dompurify'

/** 对富文本 HTML 进行消毒，仅保留安全的标签和 style 属性。 */
export function sanitizeRichTextHtml(html) {
  return DOMPurify.sanitize(String(html || ''), {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 'span', 'div', 'ul', 'ol', 'li'],
    ALLOWED_ATTR: ['style'],
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed'],
  })
}
