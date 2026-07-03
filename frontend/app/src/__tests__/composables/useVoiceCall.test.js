import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, nextTick, ref } from 'vue'
import { useVoiceCall } from '@/composables/useVoiceCall'

describe('useVoiceCall', () => {
  let speech
  let textToSpeech
  let isReplying
  let onSend

  beforeEach(() => {
    vi.useFakeTimers()
    speech = {
      isSupported: ref(true),
      isRecording: ref(false),
      isVoiceActive: ref(false),
      voiceActivityAt: ref(0),
      finalTranscript: ref(''),
      interimTranscript: ref(''),
      error: ref(''),
      errorCode: ref(''),
      engineStatus: ref('browser-service'),
      startConfirmed: ref(true),
      start: vi.fn(() => {
        speech.isRecording.value = true
      }),
      stop: vi.fn(() => {
        speech.isRecording.value = false
      }),
      cancel: vi.fn(() => {
        speech.isRecording.value = false
      }),
    }
    textToSpeech = {
      isSupported: ref(true),
      isPreparing: ref(false),
      isSpeaking: ref(false),
      stop: vi.fn(),
    }
    textToSpeech.isActive = computed(() => textToSpeech.isPreparing.value || textToSpeech.isSpeaking.value)
    isReplying = ref(false)
    onSend = vi.fn(() => Promise.resolve())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('starts speech recognition and call timer', () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })

    expect(call.startVoiceCall()).toBe(true)
    vi.advanceTimersByTime(1000)

    expect(speech.start).toHaveBeenCalled()
    expect(call.callDuration.value).toBe(1)
  })

  it('can defer initial listening until opening speech finishes', () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })

    expect(call.startVoiceCall({ startListening: false })).toBe(true)
    vi.advanceTimersByTime(1000)

    expect(call.isVoiceMode.value).toBe(true)
    expect(call.callDuration.value).toBe(1)
    expect(speech.start).not.toHaveBeenCalled()

    call.resumeListening()

    expect(speech.start).toHaveBeenCalledTimes(1)
  })

  it('resumes listening shortly after opening speech ends', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })

    call.startVoiceCall({ startListening: false })
    textToSpeech.isSpeaking.value = true
    await nextTick()
    textToSpeech.isSpeaking.value = false
    await nextTick()

    await vi.advanceTimersByTimeAsync(799)

    expect(speech.start).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1)

    expect(speech.start).toHaveBeenCalledTimes(1)
  })

  it('does not resume listening while AI audio is preparing but not yet speaking', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })

    call.startVoiceCall({ startListening: false })
    textToSpeech.isPreparing.value = true
    await nextTick()
    call.resumeListening()

    expect(speech.start).not.toHaveBeenCalled()
  })

  it('auto sends after three seconds of silence', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()
    vi.advanceTimersByTime(3000)

    expect(onSend).toHaveBeenCalledWith('我负责订单模块')
    expect(call.pendingMessage.value).toBe('')
  })

  it('auto sends after silence when browser recognition only has interim text before stop', async () => {
    speech.stop = vi.fn(() => {
      speech.isRecording.value = false
      speech.finalTranscript.value = '我负责订单模块'
      speech.interimTranscript.value = ''
      return Promise.resolve()
    })
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    speech.interimTranscript.value = '我负责订单模块'
    await nextTick()

    expect(call.pendingMessage.value).toBe('我负责订单模块')

    await vi.advanceTimersByTimeAsync(3000)

    expect(speech.stop).toHaveBeenCalled()
    expect(onSend).toHaveBeenCalledWith('我负责订单模块')
    expect(call.pendingMessage.value).toBe('')
  })

  it('does not auto send when microphone activity has no transcript text', async () => {
    speech.stop = vi.fn(() => {
      speech.isRecording.value = false
      speech.finalTranscript.value = '我负责订单模块'
      return Promise.resolve()
    })
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    speech.voiceActivityAt.value = Date.now()
    await nextTick()

    await vi.advanceTimersByTimeAsync(3000)

    expect(speech.stop).not.toHaveBeenCalled()
    expect(onSend).not.toHaveBeenCalled()
    expect(call.pendingMessage.value).toBe('')
  })

  it('does not auto send when silence timeout is disabled', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend, silenceTimeoutMs: 0 })
    call.startVoiceCall()

    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()
    vi.advanceTimersByTime(5000)

    expect(onSend).not.toHaveBeenCalled()
    expect(call.pendingMessage.value).toBe('我负责订单模块')
  })

  it('preserves English spaces across transcript updates', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    speech.finalTranscript.value = 'Please describe '
    await nextTick()
    speech.finalTranscript.value = 'Please describe your project.'
    await nextTick()
    vi.advanceTimersByTime(3000)

    expect(onSend).toHaveBeenCalledWith('Please describe your project.')
  })

  it('does not auto send while ai is replying', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()

    isReplying.value = true
    await nextTick()
    vi.advanceTimersByTime(3000)

    expect(onSend).not.toHaveBeenCalled()
    expect(speech.stop).toHaveBeenCalled()
  })

  it('cleans resources when ending call', () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    call.endVoiceCall()

    expect(call.isVoiceMode.value).toBe(false)
    expect(call.isMuted.value).toBe(false)
    expect(speech.cancel).toHaveBeenCalled()
    expect(textToSpeech.stop).toHaveBeenCalled()
  })

  it('falls back to manual input when speech recognition is unsupported', () => {
    speech.isSupported.value = false
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })

    expect(call.startVoiceCall()).toBe(false)

    expect(call.error.value).toBe('当前浏览器不支持语音识别，已降级为手动输入')
    expect(call.isVoiceMode.value).toBe(false)
    expect(speech.start).not.toHaveBeenCalled()
  })

  it('automatically restarts listening after a recoverable speech interruption', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()

    speech.isRecording.value = false
    speech.errorCode.value = 'no-transcript'
    speech.error.value = '检测到麦克风输入，但浏览器未返回识别文字，已降级为手动输入。错误码：no-transcript'
    await nextTick()

    expect(call.error.value).toContain('no-transcript')
    expect(call.isVoiceMode.value).toBe(true)
    expect(call.isManualResumePending.value).toBe(false)
    expect(call.pendingMessage.value).toBe('我负责订单模块')
    expect(textToSpeech.stop).not.toHaveBeenCalled()
    expect(speech.cancel).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(1000)

    expect(speech.start).toHaveBeenCalledTimes(2)
    expect(call.pendingMessage.value).toBe('我负责订单模块')
  })

  it('enters text fallback after repeated recoverable speech interruptions', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()

    for (let index = 0; index < 2; index += 1) {
      speech.isRecording.value = false
      speech.errorCode.value = 'no-transcript'
      speech.error.value = `检测到麦克风输入，但浏览器未返回识别文字。第 ${index + 1} 次`
      await nextTick()
      await vi.advanceTimersByTimeAsync(1000)
    }

    expect(speech.start).toHaveBeenCalledTimes(3)
    expect(call.isManualResumePending.value).toBe(false)
    expect(call.pendingMessage.value).toBe('我负责订单模块')

    speech.isRecording.value = false
    speech.errorCode.value = 'no-transcript'
    speech.error.value = '检测到麦克风输入，但浏览器未返回识别文字。第 3 次'
    await nextTick()
    await vi.advanceTimersByTimeAsync(1000)

    expect(speech.start).toHaveBeenCalledTimes(3)
    expect(call.isManualResumePending.value).toBe(false)
    expect(call.isTextFallbackMode.value).toBe(true)
    expect(call.pendingMessage.value).toBe('我负责订单模块')
  })

  it('keeps voice mode and switches to text fallback when microphone permission is blocked', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    speech.errorCode.value = 'not-allowed'
    speech.error.value = '麦克风权限被拒绝，已降级为手动输入'
    await nextTick()

    expect(call.error.value).toBe('麦克风权限被拒绝，已降级为手动输入')
    expect(call.isVoiceMode.value).toBe(true)
    expect(call.isTextFallbackMode.value).toBe(true)
    expect(textToSpeech.stop).not.toHaveBeenCalled()
    expect(speech.cancel).not.toHaveBeenCalled()
  })

  it('keeps text fallback after temporary browser service failures until the user retries', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()

    speech.isRecording.value = false
    speech.errorCode.value = 'network'
    speech.error.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await nextTick()

    expect(call.isVoiceMode.value).toBe(true)
    expect(call.isTextFallbackMode.value).toBe(true)
    expect(call.pendingMessage.value).toBe('我负责订单模块')
    expect(speech.cancel).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(60000)
    expect(speech.start).toHaveBeenCalledTimes(1)
    expect(call.isTextFallbackMode.value).toBe(true)

    isReplying.value = true
    await nextTick()
    isReplying.value = false
    await nextTick()
    await vi.advanceTimersByTimeAsync(0)

    expect(speech.start).toHaveBeenCalledTimes(1)
    expect(call.isTextFallbackMode.value).toBe(true)
  })

  it('manual speech retry triggers one immediate recovery probe from text fallback', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.isRecording.value = false
    speech.errorCode.value = 'service-not-allowed'
    speech.error.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await nextTick()
    speech.start.mockClear()

    expect(await call.retrySpeechNow()).toBe(true)

    expect(speech.start).toHaveBeenCalledTimes(1)
    expect(call.isTextFallbackMode.value).toBe(false)
  })

  it('keeps text fallback when recovery probe reports unhealthy even if recording starts', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.isRecording.value = false
    speech.errorCode.value = 'start-timeout'
    speech.error.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await nextTick()
    speech.start.mockImplementation(() => {
      speech.isRecording.value = true
      return Promise.resolve({ ok: false, code: 'start-timeout' })
    })
    speech.start.mockClear()

    await expect(call.retrySpeechNow()).resolves.toBe(false)

    expect(speech.start).toHaveBeenCalledWith({ waitForHealthyStart: true })
    expect(call.isTextFallbackMode.value).toBe(true)
  })

  it('leaves text fallback only after a healthy recovery probe succeeds', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.isRecording.value = false
    speech.errorCode.value = 'service-not-allowed'
    speech.error.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await nextTick()
    speech.start.mockImplementation(() => {
      speech.isRecording.value = true
      return Promise.resolve({ ok: true, code: '' })
    })
    speech.start.mockClear()

    await expect(call.retrySpeechNow()).resolves.toBe(true)

    expect(speech.start).toHaveBeenCalledWith({ waitForHealthyStart: true })
    expect(call.isTextFallbackMode.value).toBe(false)
  })

  it('does not leave text fallback merely because startup confirmation flips after a failed probe', async () => {
    speech.startConfirmed.value = false
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.isRecording.value = false
    speech.errorCode.value = 'start-timeout'
    speech.error.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await nextTick()
    speech.start.mockImplementation(() => {
      speech.isRecording.value = true
      speech.startConfirmed.value = true
      return Promise.resolve({ ok: false, code: 'start-timeout' })
    })
    speech.start.mockClear()

    await expect(call.retrySpeechNow()).resolves.toBe(false)

    expect(speech.start).toHaveBeenCalledTimes(1)
    expect(call.isTextFallbackMode.value).toBe(true)
  })

  it('does not probe speech recovery while AI audio is speaking', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.isRecording.value = false
    speech.errorCode.value = 'network'
    speech.error.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await nextTick()

    textToSpeech.isSpeaking.value = true
    await nextTick()
    await vi.advanceTimersByTimeAsync(15000)

    expect(speech.start).toHaveBeenCalledTimes(1)
    expect(call.isTextFallbackMode.value).toBe(true)
  })

  it('pauses recognition while muted and resumes after unmute', () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    expect(call.toggleMute()).toBe(true)
    expect(call.isMuted.value).toBe(true)
    expect(speech.stop).toHaveBeenCalled()

    expect(call.toggleMute()).toBe(false)
    expect(call.isMuted.value).toBe(false)
    expect(speech.start).toHaveBeenCalledTimes(2)
  })

  it('waits for a second click after unmute in manual resume mode', () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend, muteResumeMode: 'manual' })
    call.startVoiceCall()

    expect(call.toggleMute()).toBe(true)
    expect(call.toggleMute()).toBe(false)

    expect(call.isMuted.value).toBe(false)
    expect(call.isManualResumePending.value).toBe(true)
    expect(speech.start).toHaveBeenCalledTimes(1)

    expect(call.toggleMute()).toBe(false)

    expect(call.isManualResumePending.value).toBe(false)
    expect(speech.start).toHaveBeenCalledTimes(2)
  })

  it('keeps waiting while microphone detects voice activity', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()

    vi.advanceTimersByTime(2500)
    speech.isVoiceActive.value = true
    vi.advanceTimersByTime(500)

    expect(onSend).not.toHaveBeenCalled()

    speech.isVoiceActive.value = false
    vi.advanceTimersByTime(3000)

    expect(onSend).toHaveBeenCalledWith('我负责订单模块')
  })

  it('manually stops listening and sends pending transcript even when voice activity continues', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.finalTranscript.value = '我负责订单模块'
    await nextTick()

    speech.isVoiceActive.value = true
    const sent = await call.stopListeningAndSend()

    expect(sent).toBe(true)
    expect(onSend).toHaveBeenCalledWith('我负责订单模块')
    expect(speech.stop).toHaveBeenCalled()
    expect(call.pendingMessage.value).toBe('')
    expect(call.isMuted.value).toBe(false)
  })

  it('keeps listening when manual stop has no pending transcript', async () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()
    speech.stop.mockClear()

    const sent = await call.stopListeningAndSend()

    expect(sent).toBe(false)
    expect(onSend).not.toHaveBeenCalled()
    expect(speech.stop).not.toHaveBeenCalled()
    expect(call.isVoiceMode.value).toBe(true)
  })

  it('restarts speech recognition when browser ends listening during a voice call', () => {
    const call = useVoiceCall({ speech, textToSpeech, isReplying, onSend })
    call.startVoiceCall()

    speech.isRecording.value = false
    vi.advanceTimersByTime(500)

    expect(speech.start).toHaveBeenCalledTimes(2)
  })
})
