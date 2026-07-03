import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCloudTextToSpeech } from '@/composables/useCloudTextToSpeech'
import { synthesizeInterviewTts } from '@/api/interview'

vi.mock('@/api/interview', () => ({
  synthesizeInterviewTts: vi.fn(),
}))

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

const createDeferred = () => {
  let resolve
  let reject
  const promise = new Promise((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
}

describe('useCloudTextToSpeech', () => {
  let audioInstances

  beforeEach(() => {
    vi.resetAllMocks()
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

  it('marks cloud synthesis as preparing before real audio playback starts', async () => {
    const audioDeferred = createDeferred()
    const onStart = vi.fn()
    synthesizeInterviewTts.mockReturnValueOnce(audioDeferred.promise)
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    tts.speak('你好，请介绍一下自己。', { onStart })
    await flushPromises()

    expect(synthesizeInterviewTts).toHaveBeenCalledWith('session-1', '你好，请介绍一下自己。', expect.any(Object))
    expect(tts.isPreparing.value).toBe(true)
    expect(tts.isSpeaking.value).toBe(false)
    expect(tts.isActive.value).toBe(true)
    expect(onStart).not.toHaveBeenCalled()

    audioDeferred.resolve(new Blob(['first'], { type: 'audio/mpeg' }))
    await flushPromises()

    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(tts.isPreparing.value).toBe(false)
    expect(tts.isSpeaking.value).toBe(true)
    expect(tts.isActive.value).toBe(true)
    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({
      started: true,
      text: '你好，请介绍一下自己。',
    }))
  })

  it('pre-synthesizes the next sentence while current cloud audio is playing', async () => {
    const secondAudioDeferred = createDeferred()
    synthesizeInterviewTts
      .mockResolvedValueOnce(new Blob(['first'], { type: 'audio/mpeg' }))
      .mockReturnValueOnce(secondAudioDeferred.promise)
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    tts.speakStreaming('第一句。第二句。')
    await flushPromises()

    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(true)
    expect(synthesizeInterviewTts).toHaveBeenCalledTimes(2)
    expect(synthesizeInterviewTts).toHaveBeenNthCalledWith(2, 'session-1', '第二句。', expect.any(Object))

    secondAudioDeferred.resolve(new Blob(['second'], { type: 'audio/mpeg' }))
    await flushPromises()
    expect(audioInstances).toHaveLength(1)

    audioInstances[0].onended()
    await flushPromises()

    expect(audioInstances[1].play).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(true)
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
