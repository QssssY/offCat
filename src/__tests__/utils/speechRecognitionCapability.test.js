import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  detectSpeechRecognitionCapability,
  SPEECH_RECOGNITION_CAPABILITY_STATUS,
} from '@/utils/speechRecognitionCapability'

describe('speechRecognitionCapability', () => {
  beforeEach(() => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    Object.defineProperty(navigator, 'permissions', {
      configurable: true,
      value: {
        query: vi.fn(() => Promise.resolve({ state: 'granted' })),
      },
    })
  })

  it('reports unsupported when Web Speech recognition is missing', async () => {
    const capability = await detectSpeechRecognitionCapability()

    expect(capability.status).toBe(SPEECH_RECOGNITION_CAPABILITY_STATUS.UNSUPPORTED)
    expect(capability.SpeechRecognition).toBeNull()
  })

  it('reports permission-blocked when microphone permission is denied', async () => {
    window.SpeechRecognition = vi.fn()
    navigator.permissions.query.mockResolvedValueOnce({ state: 'denied' })

    const capability = await detectSpeechRecognitionCapability({ lang: 'zh-CN' })

    expect(navigator.permissions.query).toHaveBeenCalledWith({ name: 'microphone' })
    expect(capability.status).toBe(SPEECH_RECOGNITION_CAPABILITY_STATUS.PERMISSION_BLOCKED)
    expect(capability.permissionState).toBe('denied')
  })

  it('ignores experimental local language pack APIs and reports regular Web Speech', async () => {
    window.SpeechRecognition = vi.fn()
    window.SpeechRecognition.available = vi.fn(() => Promise.resolve('available'))
    window.SpeechRecognition.install = vi.fn(() => Promise.resolve(true))

    const capability = await detectSpeechRecognitionCapability({ lang: 'zh-CN' })

    expect(window.SpeechRecognition.available).not.toHaveBeenCalled()
    expect(window.SpeechRecognition.install).not.toHaveBeenCalled()
    expect(capability.status).toBe(SPEECH_RECOGNITION_CAPABILITY_STATUS.WEBSPEECH_READY)
  })

  it('reports regular Web Speech when browser recognition exists', async () => {
    window.SpeechRecognition = vi.fn()

    const capability = await detectSpeechRecognitionCapability({ lang: 'zh-CN' })

    expect(capability.status).toBe(SPEECH_RECOGNITION_CAPABILITY_STATUS.WEBSPEECH_READY)
  })
})
