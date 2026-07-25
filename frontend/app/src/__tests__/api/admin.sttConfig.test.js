import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const adminRequestMock = vi.fn()

vi.mock('@/utils/adminRequest', () => ({
  default: (...args) => adminRequestMock(...args)
}))

import {
  getAdminSttConfig,
  saveAdminSttConfig,
  testAdminSttConnectivity
} from '@/api/admin/sttConfig'

describe('admin sttConfig api', () => {
  beforeEach(() => {
    adminRequestMock.mockReset()
    adminRequestMock.mockResolvedValue({ code: 200, data: {} })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('getAdminSttConfig 走 GET /api/admin/stt-config', async () => {
    await getAdminSttConfig()
    expect(adminRequestMock).toHaveBeenCalledWith({
      url: '/api/admin/stt-config',
      method: 'get'
    })
  })

  it('saveAdminSttConfig 走 PUT 并携带配置体', async () => {
    const payload = { enabled: true, baseUrl: 'https://api.siliconflow.cn/v1', model: 'FunAudioLLM/SenseVoiceSmall' }
    await saveAdminSttConfig(payload)
    expect(adminRequestMock).toHaveBeenCalledWith({
      url: '/api/admin/stt-config',
      method: 'put',
      data: payload
    })
  })

  it('testAdminSttConnectivity 走 POST /test-connectivity', async () => {
    const payload = { baseUrl: 'https://api.siliconflow.cn/v1', model: 'FunAudioLLM/SenseVoiceSmall' }
    await testAdminSttConnectivity(payload)
    expect(adminRequestMock).toHaveBeenCalledWith({
      url: '/api/admin/stt-config/test-connectivity',
      method: 'post',
      data: payload
    })
  })
})
