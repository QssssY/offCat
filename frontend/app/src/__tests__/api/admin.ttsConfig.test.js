import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/utils/adminRequest', () => ({
  default: vi.fn((config) => Promise.resolve({ code: 200, data: config }))
}))

vi.mock('@/utils/adminAuth', () => ({
  getAdminToken: vi.fn(() => 'admin-token'),
  getAdminTokenType: vi.fn(() => 'Bearer')
}))

import adminRequest from '@/utils/adminRequest'
import {
  discoverAdminTtsOptions,
  getAdminTtsConfig,
  previewAdminTtsVoice,
  saveAdminTtsConfig,
  testAdminTtsConnectivity
} from '@/api/admin/ttsConfig'

describe('admin TTS config API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn(() => Promise.resolve({
      ok: true,
      blob: () => Promise.resolve(new Blob(['mp3'], { type: 'audio/mpeg' }))
    }))
  })

  it('should call system TTS config admin endpoints', async () => {
    const payload = {
      enabled: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      apiKey: 'sys-key',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    }

    await getAdminTtsConfig()
    await saveAdminTtsConfig(payload)
    await testAdminTtsConnectivity(payload)
    await discoverAdminTtsOptions(payload)

    expect(adminRequest).toHaveBeenNthCalledWith(1, {
      url: '/api/admin/tts-config',
      method: 'get'
    })
    expect(adminRequest).toHaveBeenNthCalledWith(2, {
      url: '/api/admin/tts-config',
      method: 'put',
      data: payload
    })
    expect(adminRequest).toHaveBeenNthCalledWith(3, {
      url: '/api/admin/tts-config/test-connectivity',
      method: 'post',
      data: payload
    })
    expect(adminRequest).toHaveBeenNthCalledWith(4, {
      url: '/api/admin/tts-config/discover',
      method: 'post',
      data: payload
    })
  })

  it('should preview system TTS voice with admin token and audio blob response', async () => {
    const blob = await previewAdminTtsVoice({
      enabled: true,
      ttsProvider: 'openai',
      baseUrl: 'https://tts.example.com/v1',
      apiKey: 'sys-key',
      model: 'tts-1',
      voiceId: 'alloy',
      endpointPath: '/audio/speech'
    })

    expect(blob).toBeInstanceOf(Blob)
    expect(global.fetch).toHaveBeenCalledWith('/api/admin/tts-config/preview', expect.objectContaining({
      method: 'POST',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        Authorization: 'Bearer admin-token'
      })
    }))
  })
})
