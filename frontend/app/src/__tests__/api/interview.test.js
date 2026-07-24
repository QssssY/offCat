import { beforeEach, describe, expect, it, vi } from 'vitest'

import request from '@/utils/request'
import {
  getInterviewSessionStatus,
  getInterviewTtsCapability,
  streamInterviewMessage,
  synthesizeInterviewTts,
} from '@/api/interview'

vi.mock('@/utils/request', () => ({
  default: vi.fn(),
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'token'),
}))

describe('interview API fallbackToPlatform', () => {
  beforeEach(() => {
    request.mockReset()
    global.fetch = vi.fn(() => Promise.resolve({ ok: true }))
  })

  it('should request lightweight session status', async () => {
    request.mockResolvedValueOnce({ data: { sessionId: 'session-1', reportReady: true } })

    await getInterviewSessionStatus('session-1')

    expect(request).toHaveBeenCalledWith({
      url: '/api/interview/session/session-1/status',
      method: 'get'
    })
  })

  it('should include fallbackToPlatform in stream body when requested', async () => {
    await streamInterviewMessage(
      'session-1',
      { content: 'answer', feedbackMode: 'immediate' },
      'token',
      { fallbackToPlatform: true }
    )

    expect(global.fetch).toHaveBeenCalledWith('/api/interview/session/session-1/message/stream', expect.objectContaining({
      body: JSON.stringify({
        content: 'answer',
        feedbackMode: 'immediate',
        fallbackToPlatform: true
      })
    }))
  })

  it('should request voice interview TTS capability', async () => {
    request.mockResolvedValueOnce({ data: { available: true, engine: 'user_custom_tts' } })

    await getInterviewTtsCapability('session-1')

    expect(request).toHaveBeenCalledWith({
      url: '/api/interview/session/session-1/tts-capability',
      method: 'get',
      skipDefaultErrorHandler: true
    })
  })

  it('should synthesize voice interview TTS as audio blob', async () => {
    const audioBlob = new Blob(['mp3'], { type: 'audio/mpeg' })
    global.fetch = vi.fn(() => Promise.resolve({
      ok: true,
      blob: () => Promise.resolve(audioBlob),
    }))

    const result = await synthesizeInterviewTts('session-1', '你好')

    expect(global.fetch).toHaveBeenCalledWith('/api/interview/session/session-1/tts', expect.objectContaining({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token'
      },
      body: JSON.stringify({ text: '你好' })
    }))
    expect(result).toBe(audioBlob)
  })

  it('should include an explicitly selected EdgeTTS voice in the synthesis body', async () => {
    const audioBlob = new Blob(['mp3'], { type: 'audio/mpeg' })
    global.fetch = vi.fn(() => Promise.resolve({
      ok: true,
      blob: () => Promise.resolve(audioBlob),
    }))

    await synthesizeInterviewTts('session-1', '浣犲ソ', {
      voiceId: 'zh-CN-YunxiNeural'
    })

    expect(global.fetch).toHaveBeenCalledWith('/api/interview/session/session-1/tts', expect.objectContaining({
      body: JSON.stringify({
        text: '浣犲ソ',
        voiceId: 'zh-CN-YunxiNeural'
      })
    }))
  })
  it('should expose backend TTS errors without leaking response internals', async () => {
    global.fetch = vi.fn(() => Promise.resolve({
      ok: false,
      status: 409,
      json: () => Promise.resolve({ code: 4090, message: '自定义 TTS 调用失败' }),
    }))

    await expect(synthesizeInterviewTts('session-1', '你好')).rejects.toMatchObject({
      code: 4090,
      message: '自定义 TTS 调用失败',
      status: 409,
    })
  })
})
