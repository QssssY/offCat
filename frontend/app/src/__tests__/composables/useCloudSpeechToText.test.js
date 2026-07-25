import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'

const transcribeMock = vi.fn(() => Promise.resolve('识别文本'))

vi.mock('@/api/interview', () => ({
  transcribeInterviewSpeech: (...args) => transcribeMock(...args),
}))

import { useCloudSpeechToText } from '@/composables/useCloudSpeechToText'

// 可控 MediaRecorder：记录实例，暴露手动触发 stop 的能力。
class MockMediaRecorder {
  constructor(stream, options) {
    this.stream = stream
    this.options = options
    this.state = 'inactive'
    this.ondataavailable = null
    this.onstop = null
    MockMediaRecorder.instances.push(this)
  }

  start() {
    this.state = 'recording'
  }

  stop() {
    this.state = 'inactive'
    // 模拟浏览器先回吐一段音频数据，再触发 onstop。
    this.ondataavailable?.({ data: new Blob(['audio'], { type: 'audio/webm' }) })
    this.onstop?.()
  }
}
MockMediaRecorder.isTypeSupported = () => true

class MockAnalyser {
  constructor() {
    this.fftSize = 1024
  }

  getByteTimeDomainData(samples) {
    // 恒定填充 200，制造持续“有声”信号，便于切段逻辑触发。
    samples.fill(200)
  }
}

class MockAudioContext {
  createAnalyser() {
    return new MockAnalyser()
  }

  createMediaStreamSource() {
    return { connect: vi.fn() }
  }

  close() {}
}

const runInScope = (fn) => {
  const scope = effectScope()
  let result
  scope.run(() => { result = fn() })
  return { result, scope }
}

describe('useCloudSpeechToText', () => {
  beforeEach(() => {
    MockMediaRecorder.instances = []
    transcribeMock.mockClear()
    transcribeMock.mockResolvedValue('识别文本')
    global.window.MediaRecorder = MockMediaRecorder
    global.window.AudioContext = MockAudioContext
    global.navigator.mediaDevices = {
      getUserMedia: vi.fn(() => Promise.resolve({ getTracks: () => [{ stop: vi.fn() }] })),
    }
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('未启用时不支持', () => {
    const { result, scope } = runInScope(() => useCloudSpeechToText({ sessionId: 's1', enabled: false }))
    expect(result.isSupported.value).toBe(false)
    scope.stop()
  })

  it('setEnabled(true) 后在录音器可用时变为支持', () => {
    const { result, scope } = runInScope(() => useCloudSpeechToText({ sessionId: 's1', enabled: false }))
    result.setEnabled(true)
    expect(result.isSupported.value).toBe(true)
    scope.stop()
  })

  it('start 后进入录音态并创建录音器', async () => {
    const { result, scope } = runInScope(() => useCloudSpeechToText({ sessionId: 's1', enabled: true }))
    const startResult = await result.start({ waitForHealthyStart: true })
    expect(startResult.ok).toBe(true)
    expect(result.isRecording.value).toBe(true)
    expect(MockMediaRecorder.instances.length).toBeGreaterThan(0)
    scope.stop()
  })

  it('切段上传成功后把识别文本追加到 finalTranscript', async () => {
    const { result, scope } = runInScope(() => useCloudSpeechToText({ sessionId: 's1', enabled: true }))
    await result.start({ waitForHealthyStart: true })
    // MockAnalyser 恒定返回“有声”，等待几个 VAD 定时器 tick（120ms/次）把 segmentHasSpeech 置真。
    await new Promise((resolve) => setTimeout(resolve, 320))
    // stop() 主动切收尾段，onstop 触发上传，并等待上传 Promise 结算。
    await result.stop()
    expect(transcribeMock).toHaveBeenCalled()
    expect(result.finalTranscript.value).toContain('识别文本')
    scope.stop()
  })

  it('sessionId 缺失时不发起上传', async () => {
    const { result, scope } = runInScope(() => useCloudSpeechToText({ sessionId: '', enabled: true }))
    await result.start({ waitForHealthyStart: true })
    await result.stop()
    expect(transcribeMock).not.toHaveBeenCalled()
    scope.stop()
  })

  it('cancel 清空状态并停止录音', async () => {
    const { result, scope } = runInScope(() => useCloudSpeechToText({ sessionId: 's1', enabled: true }))
    await result.start({ waitForHealthyStart: true })
    result.cancel()
    expect(result.isRecording.value).toBe(false)
    expect(result.finalTranscript.value).toBe('')
    scope.stop()
  })
})
