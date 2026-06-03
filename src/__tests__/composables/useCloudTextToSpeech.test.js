import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCloudTextToSpeech } from '@/composables/useCloudTextToSpeech'
import { synthesizeInterviewTts } from '@/api/interview'

vi.mock('@/api/interview', () => ({
  synthesizeInterviewTts: vi.fn(),
}))

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('useCloudTextToSpeech', () => {
  let audioInstances

  beforeEach(() => {
    vi.clearAllMocks()
    audioInstances = []
    URL.createObjectURL = vi.fn((blob) => `blob:${blob.size}:${audioInstances.length}`)
    URL.revokeObjectURL = vi.fn()
    window.Audio = vi.fn(function Audio(url) {
      this.src = url
      this.play = vi.fn(() => Promise.resolve())
      this.pause = vi.fn()
      this.onended = null
      this.onerror = null
      audioInstances.push(this)
    })
  })

  it('queues cloud speech by sentence and releases blob urls after playback', async () => {
    synthesizeInterviewTts
      .mockResolvedValueOnce(new Blob(['first'], { type: 'audio/mpeg' }))
      .mockResolvedValueOnce(new Blob(['second'], { type: 'audio/mpeg' }))
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    tts.speakStreaming('你好。下一句')
    await flushPromises()

    expect(synthesizeInterviewTts).toHaveBeenCalledWith('session-1', '你好。', expect.any(Object))
    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(true)

    audioInstances[0].onended()
    await flushPromises()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:5:0')

    tts.flushRemaining()
    await flushPromises()

    expect(synthesizeInterviewTts).toHaveBeenCalledWith('session-1', '下一句', expect.any(Object))
    audioInstances[1].onended()
    await flushPromises()
    expect(tts.isSpeaking.value).toBe(false)
  })

  it('notifies fallback once and disables cloud playback when synthesis fails', async () => {
    synthesizeInterviewTts.mockRejectedValue(new Error('upstream failed'))
    const onFallback = vi.fn()
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true, onFallback })

    tts.speak('云端失败。')
    await flushPromises()

    expect(onFallback).toHaveBeenCalledTimes(1)
    expect(onFallback).toHaveBeenCalledWith(expect.objectContaining({
      text: '云端失败。',
      reason: expect.any(Error),
    }))
    expect(tts.isSupported.value).toBe(false)

    tts.speak('后续不再请求云端。')
    await flushPromises()

    expect(synthesizeInterviewTts).toHaveBeenCalledTimes(1)
  })

  it('stops active audio and revokes current blob url', async () => {
    synthesizeInterviewTts.mockResolvedValueOnce(new Blob(['active'], { type: 'audio/mpeg' }))
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    tts.speak('正在播放。')
    await flushPromises()
    tts.stop()

    expect(audioInstances[0].pause).toHaveBeenCalledTimes(1)
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:6:0')
    expect(tts.isSpeaking.value).toBe(false)
  })
})
