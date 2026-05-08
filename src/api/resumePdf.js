import axios from 'axios'
import { getToken, getTokenType, removeToken, isLoggedIn } from '@/utils/auth'
import router from '@/router'

/**
 * 将 HTML 发送到后端，由 Chrome headless 生成文字型 PDF 并返回。
 * @param {string} html - 完整 HTML（含内联 CSS）
 * @returns {Promise<Blob>} PDF 二进制
 */
export function exportPdfFromHtml(html) {
  // 发送前先检查登录状态，避免发出必然失败的请求
  if (!isLoggedIn()) {
    removeToken()
    const currentPath = router.currentRoute?.value?.fullPath || '/'
    router.push({ path: '/login', query: { redirect: currentPath } })
    return Promise.reject(new Error('请先登录'))
  }

  const headers = {}
  const token = getToken()
  if (token) {
    headers.Authorization = `${getTokenType()} ${token}`
  }

  return axios.post('/api/resume/export-pdf', { html }, {
    responseType: 'blob',
    headers,
    timeout: 60000,
  }).then((res) => {
    // 响应为 200，但 Blob 为空
    if (!res.data || res.data.size === 0) {
      const contentType = (res.headers?.['content-type'] || '').toLowerCase()
      const contentLength = res.headers?.['content-length'] || '未提供'
      console.error(
        '[PDF导出] 收到空响应:',
        `status=${res.status}`,
        `content-type=${contentType}`,
        `content-length=${contentLength}`,
        `blob-size=${res.data ? res.data.size : 'null'}`
      )
      throw new Error('服务端返回了空的 PDF 数据，请尝试重新登录后重试')
    }

    const contentType = (res.headers?.['content-type'] || '').toLowerCase()
    // 如果 Content-Type 是文本或 JSON（说明服务端返回了错误），尝试读取
    if (contentType.includes('text') || contentType.includes('json')) {
      return res.data.text().then((text) => {
        console.error('[PDF导出] 服务端返回了非 PDF 内容:', text.substring(0, 200))
        throw new Error(text || '服务端返回了非 PDF 内容')
      })
    }

    console.log('[PDF导出] 成功，PDF 大小:', (res.data.size / 1024 / 1024).toFixed(2), 'MB')
    return res.data
  }).catch((err) => {
    // 服务端返回了错误响应（非 2xx）
    if (err.response) {
      const { status, data, headers: resHeaders } = err.response

      // 401 认证过期 → 清除 token 并跳转登录页
      if (status === 401) {
        removeToken()
        console.error('[PDF导出] 认证已过期，请重新登录')
        const currentPath = router.currentRoute?.value?.fullPath || '/'
        router.push({ path: '/login', query: { redirect: currentPath } })
        throw new Error('登录已过期，请重新登录')
      }

      // 尝试从 blob 响应中读取服务端错误消息
      if (data instanceof Blob) {
        const ct = (resHeaders?.['content-type'] || '').toLowerCase()
        if (ct.includes('text') || ct.includes('json')) {
          return data.text().then((text) => {
            let message = text
            try {
              const parsed = JSON.parse(text)
              message = parsed.message || parsed.error || text
            } catch (_) { /* 非 JSON 文本 */ }
            console.error('[PDF导出] 服务端错误:', message, `(状态码: ${status})`)
            throw new Error(message || `服务端返回错误状态码: ${status}`)
          })
        }
      }

      console.error('[PDF导出] 服务端错误:', `状态码 ${status}`)
      throw new Error(`服务端返回错误状态码: ${status}`)
    }

    if (err.code === 'ECONNABORTED') {
      console.error('[PDF导出] 请求超时')
      throw new Error('PDF 导出请求超时，请稍后重试')
    }
    if (err.message === 'Network Error') {
      console.error('[PDF导出] 网络错误')
      throw new Error('网络连接失败，请检查网络后重试')
    }
    throw err
  })
}

/**
 * 将 DOM 元素序列化为带内联 CSS 的完整 HTML 字符串。
 * @param {HTMLElement} element - 克隆后的导出元素
 * @param {string} cssText - 要内联的 CSS 文本
 * @returns {string} 完整 HTML
 */
export function serializeElementToHtml(element, cssText) {
  const clone = element.cloneNode(true)

  /* 去掉 Vue scoped 属性残留（遍历所有后代，而非依赖无效的 CSS 属性选择器 [data-v-]） */
  clone.querySelectorAll('*').forEach((node) => {
    Array.from(node.attributes).forEach((attr) => {
      if (attr.name.startsWith('data-v-')) node.removeAttribute(attr.name)
    })
  })
  Array.from(clone.attributes).forEach((attr) => {
    if (attr.name.startsWith('data-v-')) clone.removeAttribute(attr.name)
  })

  /* 去掉 style 标签上的 scoped 属性 */
  clone.querySelectorAll('style').forEach((s) => {
    s.removeAttribute('scoped')
    s.removeAttribute('data-v-')
  })

  /* 清除照片区域残留的无效 <img>（src 为空或 about:blank 的残留图片） */
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
 * 清除 CSS 中的 [data-v-xxxxx] scoped 选择器后缀，使选择器能在导出 HTML 中生效。
 * @param {string} css
 * @returns {string}
 */
export function stripScopedSelectors(css) {
  return css.replace(/\[data-v-[a-f0-9]+\]/g, '')
}
