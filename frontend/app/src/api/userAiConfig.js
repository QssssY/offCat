import request from '@/utils/request'
import { getToken, getTokenType } from '@/utils/auth'

/**
 * 查询当前用户自定义 AI 配置列表。
 * @returns {Promise}
 */
export function getUserAiConfigs() {
  return request({
    url: '/api/user/ai-config',
    method: 'get'
  })
}

/**
 * 保存当前用户指定类型的 OpenAI 兼容配置；TTS 字段只应随 default/interview 配置提交。
 * @param {{configType: string, providerName?: string, baseUrl: string, apiKey: string, model: string, supportsMultimodal?: boolean, ttsBaseUrl?: string, ttsApiKey?: string, ttsModel?: string, ttsVoiceId?: string}} data
 * @returns {Promise}
 */
export function saveUserAiConfig(data) {
  return request({
    url: '/api/user/ai-config',
    method: 'post',
    data,
    skipDefaultErrorHandler: true
  })
}

/**
 * 删除当前用户指定类型的自定义 AI 配置。
 * @param {string} configType
 * @returns {Promise}
 */
export function deleteUserAiConfig(configType) {
  return request({
    url: `/api/user/ai-config/${configType}`,
    method: 'delete'
  })
}

/**
 * 启用或禁用当前用户指定类型的自定义 AI 配置。
 * @param {string} configType
 * @param {boolean} enabled
 * @returns {Promise}
 */
export function toggleUserAiConfig(configType, enabled) {
  return request({
    url: `/api/user/ai-config/${configType}/toggle`,
    method: 'put',
    data: { enabled }
  })
}

/**
 * 使用当前表单值执行连通测试，不落库。
 * @param {{baseUrl: string, apiKey: string, model: string, supportsMultimodal?: boolean}} data
 * @returns {Promise}
 */
export function testUserAiConnectivity(data) {
  return request({
    url: '/api/user/ai-config/test-connectivity',
    method: 'post',
    data: {
      baseUrl: data.baseUrl,
      apiKey: data.apiKey,
      model: data.model,
      supportsMultimodal: data.supportsMultimodal
    },
    skipDefaultErrorHandler: true
  })
}

/**
 * 使用当前 TTS 表单值执行 OpenAI 兼容 /audio/speech 连通测试，不落库。
 * @param {{ttsBaseUrl: string, ttsApiKey: string, ttsModel: string, ttsVoiceId: string, ttsEndpointPath?: string, ttsProvider?: string}} data
 * @returns {Promise}
 */
export function testUserTtsConnectivity(data) {
  return request({
    url: '/api/user/ai-config/test-tts-connectivity',
    method: 'post',
    data: {
      baseUrl: data.ttsBaseUrl,
      apiKey: data.ttsApiKey,
      model: data.ttsModel,
      voiceId: data.ttsVoiceId,
      endpointPath: data.ttsEndpointPath || undefined,
      ttsProvider: data.ttsProvider || undefined
    },
    skipDefaultErrorHandler: true
  })
}

/**
 * 查询当前用户今日自定义 AI 调用用量。
 * @returns {Promise}
 */
export function getUserAiUsage() {
  return request({
    url: '/api/user/ai-config/usage',
    method: 'get'
  })
}

/**
 * 发现 TTS 可用模型和音色列表，不落库。
 * @param {{ttsBaseUrl: string, ttsApiKey: string, ttsProvider?: string}} data
 * @returns {Promise<{success: boolean, models: Array<{id: string, name: string}>, voices: Array<{id: string, name: string}>, voiceDiscoverySupported: boolean, message?: string}>}
 */
/**
 * 查询系统级 TTS 是否可作为当前用户未配置自定义 TTS 时的兜底能力。
 * @returns {Promise}
 */
export function getSystemTtsStatus() {
  return request({
    url: '/api/user/ai-config/system-tts-status',
    method: 'get'
  })
}

export function discoverTtsModelsAndVoices(data) {
  return request({
    url: '/api/user/ai-config/tts-discovery',
    method: 'post',
    data: {
      baseUrl: data.ttsBaseUrl,
      apiKey: data.ttsApiKey,
      provider: data.ttsProvider || undefined
    },
    skipDefaultErrorHandler: true
  })
}

/**
 * 根据当前填写的 OpenAI 兼容地址和真实 Key 拉取模型列表，不保存配置。
 * @param {{baseUrl: string, apiKey: string}} data
 * @returns {Promise}
 */
export function fetchUserAiModels(data) {
  return request({
    url: '/api/user/ai-config/models',
    method: 'post',
    data: {
      baseUrl: data.baseUrl,
      apiKey: data.apiKey
    },
    skipDefaultErrorHandler: true
  })
}

/**
 * TTS \u97f3\u8272\u8bd5\u542c\uff1a\u4f7f\u7528\u8868\u5355\u53c2\u6570\u5408\u6210\u6700\u77ed\u97f3\u9891\u5e76\u8fd4\u56de Blob\u3002
 * \u4f7f\u7528 fetch \u63a5\u6536 audio/mpeg Blob\uff0c\u907f\u514d Axios \u9ed8\u8ba4 JSON \u89e3\u6790\u5e72\u6270\u4e8c\u8fdb\u5236\u54cd\u5e94\u3002
 * @param {{ttsBaseUrl: string, ttsApiKey: string, ttsModel: string, ttsVoiceId: string, ttsEndpointPath?: string, ttsProvider?: string}} data
 * @returns {Promise<Blob>}
 */
export async function previewTtsVoice(data) {
  const token = getToken()
  const tokenType = getTokenType()
  const response = await fetch('/api/user/ai-config/tts-preview', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `${tokenType || 'Bearer'} ${token}` : ''
    },
    body: JSON.stringify({
      baseUrl: data.ttsBaseUrl,
      apiKey: data.ttsApiKey,
      model: data.ttsModel,
      voiceId: data.ttsVoiceId,
      endpointPath: data.ttsEndpointPath || undefined,
      ttsProvider: data.ttsProvider || undefined
    })
  })

  if (!response.ok) {
    let message = `TTS \u8bd5\u542c\u5931\u8d25 (${response.status})`
    try {
      const errorBody = await response.json()
      message = errorBody.message || errorBody.msg || message
    } catch { /* \u975e JSON \u54cd\u5e94\uff0c\u4fdd\u7559\u901a\u7528\u9519\u8bef */ }
    throw new Error(message)
  }

  return response.blob()
}
