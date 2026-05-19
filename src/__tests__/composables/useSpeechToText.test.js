import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useSpeechToText } from '@/composables/useSpeechToText'

describe('useSpeechToText', () => {
  let recognitionInstance
  let mediaTrack
  let audioContext
  let analyser
  let sampleValue

  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-19T10:00:00.000Z'))
    sampleValue = 128
    recognitionInstance = null
    mediaTrack = { stop: vi.fn() }
    analyser = {
      fftSize: 0,
      getByteTimeDomainData: vi.fn((samples) => samples.fill(sampleValue)),
    }
    audioContext = {
      createAnalyser: vi.fn(() => analyser),
      createMediaStreamSource: vi.fn(() => ({ connect: vi.fn() })),
      close: vi.fn(),
    }

    window.SpeechRecognition = vi.fn(function SpeechRecognitionMock() {
      recognitionInstance = this
      this.start = vi.fn()
      this.stop = vi.fn()
      this.abort = vi.fn()
    })
    window.AudioContext = vi.fn(function AudioContextMock() {
      return audioContext
    })
    navigator.mediaDevices = {
      getUserMedia: vi.fn(() => Promise.resolve({ getTracks: () => [mediaTrack] })),
    }
  })

  afterEach(() => {
    delete window.SpeechRecognition
    delete window.AudioContext
    delete navigator.mediaDevices
    vi.useRealTimers()
  })

  it('marks voice activity from microphone volume before transcript changes', async () => {
    const speech = useSpeechToText()

    await speech.start()
    sampleValue = 145
    vi.advanceTimersByTime(120)

    expect(speech.isRecording.value).toBe(true)
    expect(speech.isVoiceActive.value).toBe(true)
    expect(speech.voiceActivityAt.value).toBe(Date.now())
    expect(recognitionInstance.start).toHaveBeenCalled()
  })

  it('cleans microphone monitor when cancelled', async () => {
    const speech = useSpeechToText()

    await speech.start()
    speech.cancel()

    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
    expect(speech.isVoiceActive.value).toBe(false)
    expect(recognitionInstance.abort).toHaveBeenCalled()
  })

  it('stops recognition and releases microphone immediately when stopped', async () => {
    const speech = useSpeechToText()

    await speech.start()
    speech.stop()

    expect(recognitionInstance.stop).toHaveBeenCalled()
    expect(speech.isRecording.value).toBe(false)
    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
  })

  it('writes a user-facing error when network recognition fails', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onerror({ error: 'network' })

    expect(speech.error.value).toBe('当前浏览器语音识别服务不可用，已降级为手动输入；可切换 Edge 或检查网络后重试')
    expect(speech.isRecording.value).toBe(false)
    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
  })

  it('writes a manual-input degradation prompt when speech recognition is unsupported', async () => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    const speech = useSpeechToText()

    await speech.start()

    expect(speech.error.value).toBe('当前浏览器不支持语音识别，已降级为手动输入')
    expect(speech.isRecording.value).toBe(false)
    expect(navigator.mediaDevices.getUserMedia).not.toHaveBeenCalled()
  })

  it('writes a manual-input degradation prompt when microphone permission is denied', async () => {
    navigator.mediaDevices.getUserMedia = vi.fn(() => Promise.reject(new Error('denied')))
    const speech = useSpeechToText()

    await speech.start()

    expect(speech.error.value).toBe('麦克风权限被拒绝，已降级为手动输入')
    expect(speech.isRecording.value).toBe(false)
  })

  it('writes a manual-input degradation prompt when microphone capture fails', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onerror({ error: 'audio-capture' })

    expect(speech.error.value).toBe('未检测到可用麦克风，已降级为手动输入')
    expect(speech.isRecording.value).toBe(false)
  })

  it('writes a degradation prompt when no speech is recognized', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onerror({ error: 'no-speech' })

    expect(speech.error.value).toBe('未识别到有效语音内容，已降级为手动输入。错误码：no-speech')
    expect(speech.isRecording.value).toBe(false)
    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
  })

  it('writes a degradation prompt when microphone has audio but recognition returns no text', async () => {
    const speech = useSpeechToText()

    await speech.start()
    sampleValue = 145
    vi.advanceTimersByTime(6240)

    expect(speech.error.value).toBe('检测到麦克风输入，但浏览器未返回识别文字，已降级为手动输入。错误码：no-transcript')
    expect(speech.isRecording.value).toBe(false)
    expect(recognitionInstance.abort).toHaveBeenCalled()
    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
  })

  it('writes a degradation prompt when recognition ends without text', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onend()

    expect(speech.error.value).toBe('语音识别已结束但未返回文字，已降级为手动输入。错误码：end-without-result')
    expect(speech.isRecording.value).toBe(false)
    expect(recognitionInstance.abort).toHaveBeenCalled()
    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
  })
})
