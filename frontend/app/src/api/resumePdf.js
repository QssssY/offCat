import axios from 'axios'
import { getToken, getTokenType, removeToken, isLoggedIn } from '@/utils/auth'
import router from '@/router'

/**
 * 构建带认证头的请求配置
 */
function buildAuthConfig(extraConfig = {}) {
  if (!isLoggedIn()) {
    removeToken()
    const currentPath = router.currentRoute?.value?.fullPath || '/'
    router.push({ path: '/login', query: { redirect: currentPath } })
    throw new Error('请先登录')
  }

  const headers = {}
  const token = getToken()
  if (token) {
    headers.Authorization = `${getTokenType()} ${token}`
  }

  return {
    headers,
    timeout: 60000,
    ...extraConfig,
  }
}

/**
 * 步骤一：调用后端生成 PDF
 *
 * 后端返回 Result 格式 JSON: { code: 200, message: "PDF 生成成功", data: { fileId, fileName, fileSize } }
 *
 * @param {string} html - 完整 HTML（含内联 CSS）
 * @returns {Promise<{ fileId: string, fileName: string, fileSize: number }>}
 */
export async function exportPdfFromHtml(html) {
  const config = buildAuthConfig({ timeout: 60000 })

  try {
    const res = await axios.post('/api/resume/export-pdf', { html }, config)

    if (res.data && res.data.code === 200 && res.data.data) {
      return res.data.data
    }

    throw new Error((res.data && res.data.message) || 'PDF 生成失败')
  } catch (err) {
    if (err.response) {
      const { status, data, headers: resHeaders } = err.response

      if (status === 401) {
        removeToken()
        const currentPath = router.currentRoute?.value?.fullPath || '/'
        router.push({ path: '/login', query: { redirect: currentPath } })
        throw new Error('登录已过期，请重新登录')
      }

      if (data instanceof Blob) {
        const ct = (resHeaders?.['content-type'] || '').toLowerCase()
        if (ct.includes('text') || ct.includes('json')) {
          return data.text().then((text) => {
            let message = text
            try {
              const parsed = JSON.parse(text)
              message = parsed.message || parsed.error || text
            } catch (_) { /* 非 JSON 文本 */ }
            throw new Error(message || `服务端返回错误状态码: ${status}`)
          })
        }
      }

      throw new Error(`服务端返回错误状态码: ${status}`)
    }

    if (err.code === 'ECONNABORTED') {
      throw new Error('PDF 导出请求超时，请稍后重试')
    }
    if (err.message === 'Network Error') {
      throw new Error('网络连接失败，请检查网络后重试')
    }
    throw err
  }
}

/**
 * 步骤二：根据 fileId 下载 PDF 文件到本地
 *
 * 后端返回 application/pdf 文件流，用 Axios 携带认证头下载，避免登录 token 暴露在 URL 中。
 *
 * @param {string} fileId - 步骤一返回的文件唯一标识
 * @param {string} fileName - 下载文件名
 * @returns {Promise<void>}
 */
export async function downloadPdfFile(fileId, fileName) {
  if (!fileId) {
    throw new Error('缺少文件标识，无法下载')
  }

  const config = buildAuthConfig({
    responseType: 'blob',
    timeout: 60000,
  })

  try {
    const res = await axios.get(`/api/resume/download-pdf/${fileId}`, config)
    const contentType = (res.headers?.['content-type'] || '').toLowerCase()
    if (contentType.includes('json') || contentType.includes('text')) {
      const text = await res.data.text()
      let message = text
      try {
        const parsed = JSON.parse(text)
        message = parsed.message || parsed.error || text
      } catch (_) { /* 非 JSON 文本 */ }
      throw new Error(message || 'PDF 下载失败')
    }
    if (!res.data || res.data.size === 0) {
      throw new Error('服务端返回了空的 PDF 文件')
    }

    const url = URL.createObjectURL(res.data)
    try {
      const a = document.createElement('a')
      a.href = url
      a.download = fileName || `resume-${fileId}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    } finally {
      URL.revokeObjectURL(url)
    }
  } catch (err) {
    if (err.response?.status === 401) {
      removeToken()
      const currentPath = router.currentRoute?.value?.fullPath || '/'
      router.push({ path: '/login', query: { redirect: currentPath } })
      throw new Error('登录已过期，请重新登录')
    }
    if (err.code === 'ECONNABORTED') {
      throw new Error('PDF 下载请求超时，请稍后重试')
    }
    if (err.message === 'Network Error') {
      throw new Error('网络连接失败，请检查网络后重试')
    }
    throw err
  }
}

/**
 * 将 DOM 元素序列化为带内联 CSS 的完整 HTML 字符串。
 * @param {HTMLElement} element - 克隆后的导出元素
 * @param {string} cssText - 要内联的 CSS 文本
 * @returns {string} 完整 HTML
 */
export function serializeElementToHtml(element, cssText) {
  const clone = element.cloneNode(true)

  clone.querySelectorAll('*').forEach((node) => {
    Array.from(node.attributes).forEach((attr) => {
      if (attr.name.startsWith('data-v-')) node.removeAttribute(attr.name)
    })
  })
  Array.from(clone.attributes).forEach((attr) => {
    if (attr.name.startsWith('data-v-')) clone.removeAttribute(attr.name)
  })

  clone.querySelectorAll('style').forEach((s) => {
    s.removeAttribute('scoped')
    s.removeAttribute('data-v-')
  })

  clone.querySelectorAll('.photo-frame img, .header-photo img').forEach((img) => {
    const src = img.getAttribute('src') || ''
    if (!src || src === 'about:blank') {
      img.remove()
    }
  })

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=190mm">
<style>${cssText}</style>
</head>
<body>${clone.outerHTML}</body>
</html>`
}

/**
 * 清除 CSS 中的 [data-v-xxxxx] scoped 选择器后缀。
 * @param {string} css
 * @returns {string}
 */
export function stripScopedSelectors(css) {
  return css.replace(/\[data-v-[a-f0-9]+\]/g, '')
}
