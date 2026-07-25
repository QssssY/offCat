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

  it('threads the selected EdgeTTS voice through each synthesis request', async () => {
    synthesizeInterviewTts.mockResolvedValue(new Blob(['first'], { type: 'audio/mpeg' }))
    const tts = useCloudTextToSpeech({
      sessionId: 'session-1',
      enabled: true,
      voiceId: 'zh-CN-YunxiNeural',
    })

    tts.speak('你好，请介绍一下自己。')
    await flushPromises()

    // 每次合成都带上所选音色，后端据此在 EdgeTTS 上生效，声音设置不再被面试链路吞掉。
    expect(synthesizeInterviewTts).toHaveBeenCalledWith(
      'session-1',
      '你好，请介绍一下自己。',
      expect.objectContaining({ voiceId: 'zh-CN-YunxiNeural' }),
    )
  })

  it('pre-synthesizes the next merged segment while current cloud audio is playing', async () => {
    const secondAudioDeferred = createDeferred()
    synthesizeInterviewTts
      .mockResolvedValueOnce(new Blob(['first'], { type: 'audio/mpeg' }))
      .mockReturnValueOnce(secondAudioDeferred.promise)
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    // 首段用较小阈值抢首句出声：14+ 字且落在句末即切段。
    const firstSegment = `${'甲'.repeat(14)}。`
    // 后续段用较大阈值合并短句：56+ 字才切段，掩盖下一段合成延迟。
    const secondSegment = `${'乙'.repeat(56)}。`
    tts.speakStreaming(firstSegment)
    tts.speakStreaming(secondSegment)
    await flushPromises()

    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(true)
    // 首段播放期间已预合成第二段，避免播完首段等待网络往返造成断流。
    expect(synthesizeInterviewTts).toHaveBeenCalledTimes(2)
    expect(synthesizeInterviewTts).toHaveBeenNthCalledWith(2, 'session-1', secondSegment, expect.any(Object))

    secondAudioDeferred.resolve(new Blob(['second'], { type: 'audio/mpeg' }))
    await flushPromises()
    expect(audioInstances).toHaveLength(1)

    audioInstances[0].onended()
    await flushPromises()

    expect(audioInstances[1].play).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(true)
  })

  it('merges consecutive short sentences into one segment to cut cloud round-trips', async () => {
    synthesizeInterviewTts.mockResolvedValue(new Blob(['audio'], { type: 'audio/mpeg' }))
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    // 一段回复含多个短句：逐句切段会产生多次云端往返导致卡顿；合并后应显著减少请求数。
    tts.speakStreaming('你好。')
    tts.speakStreaming('很高兴见到你。')
    tts.speakStreaming('请先做个自我介绍。')
    tts.flushRemaining()
    await flushPromises()

    // 三个短句合并进极少数几段，而不是三次独立请求。
    expect(synthesizeInterviewTts.mock.calls.length).toBeLessThan(3)
    const spokenText = synthesizeInterviewTts.mock.calls.map((call) => call[1]).join('')
    // 合并不丢字：所有句子内容都被播报。
    expect(spokenText).toContain('你好')
    expect(spokenText).toContain('很高兴见到你')
    expect(spokenText).toContain('请先做个自我介绍')
  })

  it('queues cloud speech by segment and releases blob urls after playback', async () => {
    synthesizeInterviewTts
      .mockResolvedValueOnce(new Blob(['first'], { type: 'audio/mpeg' }))
      .mockResolvedValueOnce(new Blob(['second'], { type: 'audio/mpeg' }))
    const tts = useCloudTextToSpeech({ sessionId: 'session-1', enabled: true })

    // 首段满足小阈值即在流式阶段切出并播放。
    const firstSegment = `${'首'.repeat(14)}。`
    tts.speakStreaming(firstSegment)
    await flushPromises()

    expect(synthesizeInterviewTts).toHaveBeenCalledWith('session-1', firstSegment, expect.any(Object))
    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(true)

    audioInstances[0].onended()
    await flushPromises()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:5:0')

    // done 时 flush 剩余不足阈值的尾段也要播报。
    tts.speakStreaming('下一句')
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
