import adminRequest from '@/utils/adminRequest'
import { getAdminToken, getAdminTokenType } from '@/utils/adminAuth'

/**
 * 查询当前系统级 TTS 配置；后端只返回脱敏 API Key。
 * @returns {Promise}
 */
export function getAdminTtsConfig() {
  return adminRequest({
    url: '/api/admin/tts-config',
    method: 'get'
  })
}

/**
 * 保存系统级 TTS 配置；编辑态 API Key 为空或脱敏值时由后端复用已保存密钥。
 * @param {{enabled: boolean, ttsProvider?: string, baseUrl?: string, apiKey?: string, model?: string, voiceId?: string, endpointPath?: string}} data
 * @returns {Promise}
 */
export function saveAdminTtsConfig(data) {
  return adminRequest({
    url: '/api/admin/tts-config',
    method: 'put',
    data
  })
}

/**
 * 使用当前表单值测试系统级 TTS 连通性，不保存配置。
 * @param {{enabled?: boolean, ttsProvider?: string, baseUrl: string, apiKey?: string, model: string, voiceId: string, endpointPath?: string}} data
 * @returns {Promise}
 */
export function testAdminTtsConnectivity(data) {
  return adminRequest({
    url: '/api/admin/tts-config/test-connectivity',
    method: 'post',
    data
  })
}

/**
 * 使用当前表单值发现系统级 TTS 可用模型和音色，不保存配置。
 * @param {{enabled?: boolean, ttsProvider?: string, baseUrl: string, apiKey?: string}} data
 * @returns {Promise}
 */
export function discoverAdminTtsOptions(data) {
  return adminRequest({
    url: '/api/admin/tts-config/discover',
    method: 'post',
    data
  })
}

/**
 * 系统级 TTS 试音：通过 fetch 接收 audio/mpeg，避免 Axios 默认 JSON 解析干扰二进制响应。
 * @param {{enabled?: boolean, ttsProvider?: string, baseUrl: string, apiKey?: string, model: string, voiceId: string, endpointPath?: string}} data
 * @returns {Promise<Blob>}
 */
export async function previewAdminTtsVoice(data) {
  const token = getAdminToken()
  const tokenType = getAdminTokenType()
  const response = await fetch('/api/admin/tts-config/preview', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `${tokenType || 'Bearer'} ${token}` : ''
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    let message = `系统 TTS 试音失败 (${response.status})`
    try {
      const errorBody = await response.json()
      message = errorBody.message || errorBody.msg || message
    } catch { /* 非 JSON 响应保留通用错误 */ }
    throw new Error(message)
  }

  return response.blob()
}
