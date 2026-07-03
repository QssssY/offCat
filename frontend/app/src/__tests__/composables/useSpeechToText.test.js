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
    vi.setSystemTime(new Date('2026-06-01T10:00:00.000Z'))
    sampleValue = 128
    recognitionInstance = null
    mediaTrack = { stop: vi.fn() }
    analyser = {
      fftSize: 0,
      getByteTimeDomainData: vi.fn((samples) => samples.fill(sampleValue)),
    }
    audioContext = {
      state: 'running',
      resume: vi.fn(() => Promise.resolve()),
      createAnalyser: vi.fn(() => analyser),
      createMediaStreamSource: vi.fn(() => ({ connect: vi.fn(), disconnect: vi.fn() })),
      close: vi.fn(),
    }

    window.SpeechRecognition = vi.fn(function SpeechRecognitionMock() {
      recognitionInstance = this
      this.start = vi.fn()
      this.stop = vi.fn()
      this.abort = vi.fn()
    })
    window.SpeechRecognition.available = vi.fn(() => Promise.resolve('available'))
    window.SpeechRecognition.install = vi.fn(() => Promise.resolve(true))
    window.AudioContext = vi.fn(function AudioContextMock() {
      return audioContext
    })
    window.Worker = vi.fn()
    navigator.mediaDevices = {
      getUserMedia: vi.fn(() => Promise.resolve({ getTracks: () => [mediaTrack] })),
    }
  })

  afterEach(() => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    delete window.AudioContext
    delete window.Worker
    delete navigator.mediaDevices
    vi.useRealTimers()
  })

  const waitForRecognitionSetup = async () => {
    for (let index = 0; index < 8 && !recognitionInstance; index += 1) {
      await vi.advanceTimersByTimeAsync(0)
    }
    expect(recognitionInstance).toBeTruthy()
  }

  it('uses browser recognition even when legacy offline options are passed', async () => {
    const speech = useSpeechToText({ preferOffline: true, prewarmOffline: true })

    await speech.start()
    recognitionInstance.onstart()

    expect(window.SpeechRecognition).toHaveBeenCalledTimes(1)
    expect(recognitionInstance.start).toHaveBeenCalledTimes(1)
    expect(recognitionInstance.processLocally).toBeUndefined()
    expect(window.SpeechRecognition.available).not.toHaveBeenCalled()
    expect(window.SpeechRecognition.install).not.toHaveBeenCalled()
    expect(window.Worker).not.toHaveBeenCalled()
    expect(speech.engineStatus.value).toBe('browser-service')
    expect(speech.capabilityStatus.value).toBe('webspeech-ready')
    expect(speech.isRecording.value).toBe(true)
    expect(speech.startConfirmed.value).toBe(true)
    expect(speech).not.toHaveProperty('installLocalSpeech')
    expect(speech).not.toHaveProperty('localSpeechInstallAvailable')
    expect(speech).not.toHaveProperty('isInstallingLocalSpeech')
    expect(speech).not.toHaveProperty('prepareOfflineRecognition')
    expect(speech).not.toHaveProperty('downloadOfflineModel')
    expect(speech).not.toHaveProperty('clearOfflineModel')
    expect(speech).not.toHaveProperty('offlineEngineSuggested')
  })

  it('reports unsupported when the browser has no Web Speech recognition', async () => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    const speech = useSpeechToText({ preferOffline: true })

    await speech.start()

    expect(speech.engineStatus.value).toBe('unsupported')
    expect(speech.isSupported.value).toBe(false)
    expect(speech.isRecording.value).toBe(false)
    expect(speech.error.value).toBe('当前浏览器不支持语音识别，已降级为手动输入')
    expect(speech.error.value).not.toContain('离线')
    expect(navigator.mediaDevices.getUserMedia).not.toHaveBeenCalled()
  })

  it('does not suggest downloading an offline engine when browser recognition fails', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    recognitionInstance.onerror({ error: 'network' })

    expect(speech.errorCode.value).toBe('network')
    expect(speech.engineStatus.value).toBe('unavailable')
    expect(speech.error.value).toBe('当前浏览器语音识别服务不可用，已降级为手动输入')
    expect(speech.error.value).not.toContain('离线')
    expect(speech.isRecording.value).toBe(false)
    expect(mediaTrack.stop).toHaveBeenCalled()
    expect(audioContext.close).toHaveBeenCalled()
  })

  it('collects final and interim browser recognition transcripts', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    recognitionInstance.onresult({
      resultIndex: 0,
      results: [
        { isFinal: true, 0: { transcript: '我负责订单模块' } },
        { isFinal: false, 0: { transcript: '以及支付' } }
      ]
    })

    expect(speech.finalTranscript.value).toBe('我负责订单模块')
    expect(speech.interimTranscript.value).toBe('以及支付')
    expect(speech.error.value).toBe('')
  })

  it('marks voice activity from the optional microphone monitor', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    sampleValue = 145
    vi.advanceTimersByTime(120)

    expect(speech.isVoiceActive.value).toBe(true)
    expect(speech.voiceActivityAt.value).toBe(Date.now())
  })

  it('clears a no-transcript failure before the next browser recognition start', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    sampleValue = 145
    vi.advanceTimersByTime(6120)

    expect(speech.errorCode.value).toBe('no-transcript')
    expect(speech.engineStatus.value).toBe('unavailable')
    expect(speech.isRecording.value).toBe(false)

    await speech.start()
    recognitionInstance.onstart()

    expect(window.SpeechRecognition).toHaveBeenCalledTimes(2)
    expect(recognitionInstance.start).toHaveBeenCalledTimes(1)
    expect(speech.error.value).toBe('')
    expect(speech.errorCode.value).toBe('')
    expect(speech.engineStatus.value).toBe('browser-service')
    expect(speech.isRecording.value).toBe(true)
  })

  it('marks startup as temporarily unavailable when onstart does not fire', async () => {
    const speech = useSpeechToText()

    await speech.start()
    await vi.advanceTimersByTimeAsync(2500)

    expect(speech.errorCode.value).toBe('start-timeout')
    expect(speech.engineStatus.value).toBe('unavailable')
    expect(speech.capabilityStatus.value).toBe('temporarily-unavailable')
    expect(speech.isRecording.value).toBe(false)
    expect(speech.startConfirmed.value).toBe(false)
    expect(recognitionInstance.abort).toHaveBeenCalled()
  })

  it('resolves healthy start only after onstart survives the observation window', async () => {
    const speech = useSpeechToText()
    let resolved = false

    const startPromise = speech.start({ waitForHealthyStart: true }).then((result) => {
      resolved = true
      return result
    })
    await waitForRecognitionSetup()

    expect(resolved).toBe(false)

    recognitionInstance.onstart()
    await vi.advanceTimersByTimeAsync(999)

    expect(resolved).toBe(false)

    await vi.advanceTimersByTimeAsync(1)

    await expect(startPromise).resolves.toEqual({
      ok: true,
      code: '',
    })
    expect(speech.startConfirmed.value).toBe(true)
    expect(speech.isRecording.value).toBe(true)
  })

  it('reports unhealthy start when recognition ends during the observation window', async () => {
    const speech = useSpeechToText()

    const startPromise = speech.start({ waitForHealthyStart: true })
    await waitForRecognitionSetup()

    recognitionInstance.onstart()
    recognitionInstance.onend()

    await expect(startPromise).resolves.toEqual({
      ok: false,
      code: 'end-without-result',
    })
    expect(speech.errorCode.value).toBe('end-without-result')
    expect(speech.isRecording.value).toBe(false)
  })

  it('times out when no effective recognition event arrives after onstart', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    await vi.advanceTimersByTimeAsync(5999)

    expect(speech.errorCode.value).toBe('')
    expect(speech.isRecording.value).toBe(true)

    await vi.advanceTimersByTimeAsync(1)

    expect(speech.errorCode.value).toBe('start-timeout')
    expect(speech.capabilityStatus.value).toBe('temporarily-unavailable')
    expect(speech.isRecording.value).toBe(false)
  })

  it('treats service-not-allowed as a temporary browser service failure', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    recognitionInstance.onerror({ error: 'service-not-allowed' })

    expect(speech.errorCode.value).toBe('service-not-allowed')
    expect(speech.capabilityStatus.value).toBe('temporarily-unavailable')
    expect(speech.error.value).toBe('当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。')
  })

  it('treats an empty recognition end as a temporary browser service failure', async () => {
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()
    recognitionInstance.onend()

    expect(speech.errorCode.value).toBe('end-without-result')
    expect(speech.capabilityStatus.value).toBe('temporarily-unavailable')
    expect(speech.isRecording.value).toBe(false)
  })

  it('ignores downloadable browser local language packs', async () => {
    window.SpeechRecognition.available = vi.fn(() => Promise.resolve('downloadable'))
    window.SpeechRecognition.install = vi.fn(() => Promise.resolve(true))
    const speech = useSpeechToText()

    await speech.start()
    recognitionInstance.onstart()

    expect(speech.capabilityStatus.value).toBe('webspeech-ready')
    expect(speech.engineStatus.value).toBe('browser-service')
    expect(recognitionInstance.processLocally).toBeUndefined()
    expect(window.SpeechRecognition.available).not.toHaveBeenCalled()
    expect(window.SpeechRecognition.install).not.toHaveBeenCalled()
    expect(speech).not.toHaveProperty('installLocalSpeech')
  })
})
