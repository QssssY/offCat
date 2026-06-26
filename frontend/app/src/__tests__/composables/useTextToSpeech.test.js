import { describe, expect, it, vi, beforeEach } from 'vitest'
import { useTextToSpeech } from '@/composables/useTextToSpeech'

describe('useTextToSpeech', () => {
  let spokenUtterances

  beforeEach(() => {
    spokenUtterances = []
    window.speechSynthesis = {
      getVoices: vi.fn(() => [{ lang: 'zh-CN' }]),
      speak: vi.fn((utterance) => {
        spokenUtterances.push(utterance)
      }),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      onvoiceschanged: null,
    }
    window.SpeechSynthesisUtterance = vi.fn(function SpeechSynthesisUtterance(text) {
      this.text = text
    })
  })

  it('speaks streaming chunks by sentence boundary and flushes remaining text', () => {
    const tts = useTextToSpeech()

    tts.speakStreaming('你好，')
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    tts.speakStreaming('请介绍自己。下一句还没结束')
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].text).toBe('你好，请介绍自己。')

    tts.flushRemaining()
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)

    spokenUtterances[0].onend()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(spokenUtterances[1].text).toBe('下一句还没结束')
  })

  it('plays streamed interview questions sequentially without releasing speaking state between sentences', () => {
    const tts = useTextToSpeech()

    tts.speakStreaming('没关系，这个知识点比较深。我们换个角度，你在用Vue3开发的时候，用过哪些生命周期钩子？它们分别在什么阶段触发？')

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].text).toBe('没关系，这个知识点比较深。')
    expect(tts.isSpeaking.value).toBe(true)

    spokenUtterances[0].onend()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(spokenUtterances[1].text).toBe('我们换个角度，你在用Vue3开发的时候，用过哪些生命周期钩子？')
    expect(tts.isSpeaking.value).toBe(true)

    spokenUtterances[1].onend()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(3)
    expect(spokenUtterances[2].text).toBe('它们分别在什么阶段触发？')
    expect(tts.isSpeaking.value).toBe(true)

    spokenUtterances[2].onend()

    expect(tts.isSpeaking.value).toBe(false)
  })

  it('filters feedback blocks before speaking', () => {
    const tts = useTextToSpeech()

    tts.speak('继续说说你的项目。<FEEDBACK>这里不应朗读</FEEDBACK>')

    expect(spokenUtterances[0].text).toBe('继续说说你的项目。')
  })

  it('preserves English spaces across streaming chunk boundaries', () => {
    const tts = useTextToSpeech()

    tts.speakStreaming('Please describe ')
    tts.speakStreaming('your project.')

    expect(spokenUtterances[0].text).toBe('Please describe your project.')
  })

  it('uses a softer default speaking style and prefers natural Chinese voices', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'en-US', name: 'English Voice' },
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural' },
      { lang: 'zh-CN', name: 'Basic Chinese Voice' },
    ])
    const tts = useTextToSpeech()

    tts.speak('你好。')

    expect(tts.rate.value).toBe(0.92)
    expect(tts.pitch.value).toBe(1.06)
    expect(spokenUtterances[0].voice.name).toBe('Microsoft Xiaoxiao Natural')
    expect(spokenUtterances[0].rate).toBe(0.92)
    expect(spokenUtterances[0].pitch).toBe(1.06)
    expect(window.speechSynthesis.resume).toHaveBeenCalled()
  })

  it('uses a specific female preset voice and its bound speaking style', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Yunxi Natural', voiceURI: 'yunxi-uri', localService: true },
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-uri', localService: true },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'gentle_female' } })

    tts.speak('你好。')

    expect(spokenUtterances[0].voice.name).toBe('Microsoft Xiaoxiao Natural')
    expect(spokenUtterances[0].rate).toBe(0.85)
    expect(spokenUtterances[0].pitch).toBe(1.12)
    expect(tts.getPresetParameters('gentle_female')).toEqual({ rate: 0.85, pitch: 1.12 })
  })

  it('does not bind Chinese-specific presets to English browser voices', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'en-US', name: 'Microsoft Jenny Online', voiceURI: 'jenny-uri', localService: false },
      { lang: 'en-US', name: 'Microsoft David Desktop', voiceURI: 'david-uri', localService: true },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'pro_female' } })

    tts.speak('你好。')

    expect(tts.isPresetAvailable('pro_female')).toBe(false)
    expect(tts.voicePreferenceStatus.value.isRequestedPresetAvailable).toBe(false)
    expect(spokenUtterances[0].voice).toBeUndefined()
  })

  it('checks browser voice preset availability against the current voice list', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-uri', localService: true },
      { lang: 'en-US', name: 'English Voice', voiceURI: 'english-uri', localService: true },
    ])
    const tts = useTextToSpeech()

    expect(tts.isPresetAvailable('gentle_female')).toBe(true)
    expect(tts.isPresetAvailable('lively_female')).toBe(false)
    expect(tts.isPresetAvailable('news_anchor')).toBe(true)
    expect(tts.isPresetAvailable('custom')).toBe(true)
  })

  it('waits for browser voices before speaking the first utterance', async () => {
    vi.useFakeTimers()
    let currentVoices = []
    window.speechSynthesis.getVoices = vi.fn(() => currentVoices)
    const tts = useTextToSpeech()

    tts.speak('你好。')

    expect(tts.isSpeaking.value).toBe(true)
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    currentVoices = [{ lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural' }]
    window.speechSynthesis.onvoiceschanged()
    await Promise.resolve()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].text).toBe('你好。')
    expect(spokenUtterances[0].voice.name).toBe('Microsoft Xiaoxiao Natural')
    vi.useRealTimers()
  })

  it('falls back to the system default voice after Chrome voices fail to load during user gesture playback', async () => {
    vi.useFakeTimers()
    window.speechSynthesis.getVoices = vi.fn(() => [])
    const tts = useTextToSpeech()

    tts.prepareForUserGesture()
    tts.speak('你好，我是本次 AI 面试官。')

    expect(window.speechSynthesis.resume).toHaveBeenCalled()
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    vi.advanceTimersByTime(900)
    await Promise.resolve()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].text).toBe('你好，我是本次 AI 面试官。')
    expect(spokenUtterances[0].voice).toBeUndefined()
    vi.useRealTimers()
  })

  it('waits briefly for Chrome voices after user gesture before falling back to the system default voice', async () => {
    vi.useFakeTimers()
    let currentVoices = []
    window.speechSynthesis.getVoices = vi.fn(() => currentVoices)
    const tts = useTextToSpeech()

    tts.prepareForUserGesture()
    tts.speak('Hello.')

    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    currentVoices = [{ lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural' }]
    window.speechSynthesis.onvoiceschanged()
    await Promise.resolve()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].voice.name).toBe('Microsoft Xiaoxiao Natural')
    vi.useRealTimers()
  })

  it('still waits for preferred Chrome voices when default voice fallback is allowed', async () => {
    vi.useFakeTimers()
    let currentVoices = []
    window.speechSynthesis.getVoices = vi.fn(() => currentVoices)
    const tts = useTextToSpeech()

    tts.prepareForUserGesture()
    tts.speak('Hello.', { allowDefaultVoice: true, requireStartEvent: true })

    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    currentVoices = [{ lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural' }]
    window.speechSynthesis.onvoiceschanged()
    await Promise.resolve()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].voice.name).toBe('Microsoft Xiaoxiao Natural')
    vi.useRealTimers()
  })

  it('keeps local Chrome system voices when a later remote Google voice is the only alternative', async () => {
    vi.useFakeTimers()
    let currentVoices = [
      { lang: 'zh-CN', name: 'Microsoft Huihui Desktop', voiceURI: 'huihui-desktop', localService: true },
    ]
    window.speechSynthesis.getVoices = vi.fn(() => currentVoices)
    const tts = useTextToSpeech()

    tts.prepareForUserGesture()
    tts.speak('你好，我是你的 AI 面试官。')

    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    currentVoices = [
      { lang: 'zh-CN', name: 'Microsoft Huihui Desktop', voiceURI: 'huihui-desktop', localService: true },
      { lang: 'zh-CN', name: 'Google 普通话（中国大陆）', voiceURI: 'google-zh-cn', localService: false },
    ]
    window.speechSynthesis.onvoiceschanged()
    await Promise.resolve()

    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    vi.advanceTimersByTime(900)
    await Promise.resolve()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].voice.name).toBe('Microsoft Huihui Desktop')
    vi.useRealTimers()
  })

  it('falls back to legacy Chrome system voices after waiting for better voices times out', async () => {
    vi.useFakeTimers()
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Huihui Desktop', voiceURI: 'huihui-desktop', localService: true },
    ])
    const tts = useTextToSpeech()

    tts.prepareForUserGesture()
    tts.speak('你好，我是你的 AI 面试官。')

    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    vi.advanceTimersByTime(900)
    await Promise.resolve()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(spokenUtterances[0].voice.name).toBe('Microsoft Huihui Desktop')
    vi.useRealTimers()
  })

  it('retries streamed speech with the browser default voice when Chrome never starts playback', async () => {
    vi.useFakeTimers()
    try {
      window.speechSynthesis.speaking = true
      window.speechSynthesis.pending = true
      window.speechSynthesis.getVoices = vi.fn(() => [
        { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-natural', localService: true },
      ])
      const tts = useTextToSpeech()

      tts.speakStreaming('后续追问应该继续播报。', { allowDefaultVoice: true, requireStartEvent: true })
      await Promise.resolve()

      expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)

      await vi.advanceTimersByTimeAsync(6000)

      expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
      expect(spokenUtterances[1].text).toBe('后续追问应该继续播报。')
      expect(spokenUtterances[1].voice).toBeUndefined()
    } finally {
      vi.useRealTimers()
    }
  })

  it('reports utterance start and end details to the caller', () => {
    const onStart = vi.fn()
    const onEnd = vi.fn()
    const tts = useTextToSpeech()

    tts.speak('你好。', { onStart, onEnd })
    spokenUtterances[0].onstart()
    spokenUtterances[0].onend()

    expect(onStart).toHaveBeenCalledTimes(1)
    expect(onEnd).toHaveBeenCalledWith(expect.objectContaining({
      reason: 'end',
      started: true,
      text: '你好。',
    }))
  })

  it('keeps active utterances strongly referenced until completion', () => {
    const tts = useTextToSpeech()

    tts.speak('The interviewer should finish this sentence before listening.')

    expect(tts.activeUtteranceCount.value).toBe(1)

    spokenUtterances[0].onend()

    expect(tts.activeUtteranceCount.value).toBe(0)
  })

  it('retries once with the browser default voice when Chrome accepts an utterance but never starts it', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Broken Remote Voice', voiceURI: 'broken-remote', localService: false },
    ])
    window.speechSynthesis.speak = vi.fn((utterance) => {
      window.speechSynthesis.speaking = true
      spokenUtterances.push(utterance)
    })
    const tts = useTextToSpeech({
      onEnd,
      voicePreference: {
        type: 'custom',
        name: 'Broken Remote Voice',
        voiceURI: 'broken-remote',
        lang: 'zh-CN',
      },
    })

    tts.speak('你好。')
    vi.advanceTimersByTime(6000)

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(spokenUtterances[0].voice.name).toBe('Broken Remote Voice')
    expect(spokenUtterances[1].voice).toBeUndefined()
    expect(tts.isSpeaking.value).toBe(true)
    expect(onEnd).not.toHaveBeenCalled()

    spokenUtterances[1].onstart()
    spokenUtterances[1].onend()

    expect(onEnd).toHaveBeenCalledTimes(1)
    expect(tts.isSpeaking.value).toBe(false)
    vi.useRealTimers()
  })

  it('reports a start timeout after the browser default voice retry also fails', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    window.speechSynthesis.speak = vi.fn((utterance) => {
      window.speechSynthesis.speaking = true
      spokenUtterances.push(utterance)
    })
    const tts = useTextToSpeech()

    tts.speak('你好。', { onEnd })
    vi.advanceTimersByTime(6000)

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(onEnd).not.toHaveBeenCalled()

    vi.advanceTimersByTime(6000)

    expect(onEnd).toHaveBeenCalledWith(expect.objectContaining({
      reason: 'start-timeout',
      started: false,
      text: '你好。',
    }))
    vi.useRealTimers()
  })

  it('ignores stale callbacks from a cancelled utterance after replacement speech starts', () => {
    const onEnd = vi.fn()
    const tts = useTextToSpeech({ onEnd })

    tts.speak('第一句。')
    const staleUtterance = spokenUtterances[0]
    tts.speak('第二句。')

    staleUtterance.onend()

    expect(tts.isSpeaking.value).toBe(true)
    expect(onEnd).not.toHaveBeenCalled()
    spokenUtterances[1].onend()
    expect(tts.isSpeaking.value).toBe(false)
    expect(onEnd).toHaveBeenCalledTimes(1)
  })

  it('exposes browser TTS engine status without requiring enhanced voice package', () => {
    const tts = useTextToSpeech()

    expect(tts.engineStatus.value).toBe('system-tts')
    expect(tts.enhancedVoiceReady.value).toBe(false)
  })

  it('uses configured speaking style when provided', () => {
    const tts = useTextToSpeech({ rate: 1.1, pitch: 0.95, volume: 0.6 })

    tts.speak('你好。')

    expect(tts.rate.value).toBe(1.1)
    expect(tts.pitch.value).toBe(0.95)
    expect(tts.volume.value).toBe(0.6)
    expect(spokenUtterances[0].rate).toBe(1.1)
    expect(spokenUtterances[0].pitch).toBe(0.95)
    expect(spokenUtterances[0].volume).toBe(0.6)
  })

  it('uses custom configured browser voice when available', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-uri' },
      { lang: 'zh-CN', name: 'Microsoft Yunxi Natural', voiceURI: 'yunxi-uri' },
    ])
    const tts = useTextToSpeech({
      voicePreference: {
        type: 'custom',
        name: 'Microsoft Yunxi Natural',
        voiceURI: 'yunxi-uri',
        lang: 'zh-CN',
      },
    })

    tts.speak('Hello.')

    expect(spokenUtterances[0].voice.name).toBe('Microsoft Yunxi Natural')
  })

  it('can use system default voice without assigning a browser voice', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-uri' },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'system' } })

    tts.speak('Hello.')

    expect(tts.voice.value).toBeNull()
    expect(spokenUtterances[0].voice).toBeUndefined()
    expect(spokenUtterances[0].lang).toBe('zh-CN')
  })

  it('prefers configured gender voice when browser names expose one', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-uri' },
      { lang: 'zh-CN', name: 'Microsoft Yunxi Natural', voiceURI: 'yunxi-uri' },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'male' } })

    tts.speak('Hello.')

    expect(spokenUtterances[0].voice.name).toBe('Microsoft Yunxi Natural')
  })

  it('keeps default natural Chinese on browser default when Chrome only exposes a gendered legacy voice', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Kangkang - Chinese (Simplified, PRC)', voiceURI: 'kangkang-uri', localService: true },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'natural_zh' } })

    tts.speak('Hello.')

    expect(tts.voice.value).toBeNull()
    expect(spokenUtterances[0].voice).toBeUndefined()
    expect(spokenUtterances[0].lang).toBe('zh-CN')
    expect(tts.voicePreferenceStatus.value).toMatchObject({
      requestedType: 'natural_zh',
      selectedGender: 'unknown',
      usesBrowserDefaultVoice: true,
    })
  })

  it('does not use an explicit male voice when female preference is unavailable in Chrome', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Kangkang - Chinese (Simplified, PRC)', voiceURI: 'kangkang-uri', localService: true },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'female' } })

    tts.speak('Hello.')

    expect(tts.voice.value).toBeNull()
    expect(spokenUtterances[0].voice).toBeUndefined()
    expect(tts.voicePreferenceStatus.value).toMatchObject({
      requestedType: 'female',
      selectedGender: 'unknown',
      isDegraded: true,
      hasRequestedGenderVoice: false,
      usesBrowserDefaultVoice: true,
    })
  })

  it('marks male preference degraded and avoids explicit female voices when Chrome exposes no male voice', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Xiaoxiao Natural', voiceURI: 'xiaoxiao-uri', localService: true },
      { lang: 'zh-CN', name: 'Google 普通话（中国大陆）', voiceURI: 'google-zh-cn', localService: false },
    ])
    const tts = useTextToSpeech({ voicePreference: { type: 'male' } })

    tts.speak('Hello.')

    expect(spokenUtterances[0].voice.name).toBe('Google 普通话（中国大陆）')
    expect(tts.voicePreferenceStatus.value).toMatchObject({
      requestedType: 'male',
      selectedGender: 'unknown',
      isDegraded: true,
      hasRequestedGenderVoice: false,
    })
  })

  it('prefers local Chinese voices over generic remote browser voices for default playback reliability', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Browser Chinese Voice', voiceURI: 'browser-zh-cn', localService: false },
      { lang: 'zh-CN', name: 'Windows Chinese Voice', voiceURI: 'local-zh-cn', localService: true },
    ])
    const tts = useTextToSpeech()

    tts.speak('你好，我是本次 AI 面试官。')

    expect(spokenUtterances[0].voice.name).toBe('Windows Chinese Voice')
  })

  it('prefers local Chrome Chinese voices over remote Google voices for playback reliability', () => {
    window.speechSynthesis.getVoices = vi.fn(() => [
      { lang: 'zh-CN', name: 'Microsoft Huihui Desktop', voiceURI: 'huihui-desktop', localService: true },
      { lang: 'zh-CN', name: 'Google 普通话（中国大陆）', voiceURI: 'google-zh-cn', localService: false },
    ])
    const tts = useTextToSpeech()

    tts.speak('你好，我是你的 AI 面试官。')

    expect(spokenUtterances[0].voice.name).toBe('Microsoft Huihui Desktop')
  })

  it('stop clears speech queue', () => {
    const tts = useTextToSpeech()

    tts.speakStreaming('尚未结束')
    tts.stop()
    tts.flushRemaining()

    expect(window.speechSynthesis.cancel).toHaveBeenCalled()
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  })

  it('releases speaking state when browser TTS never emits end or error', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    const tts = useTextToSpeech({ onEnd })

    tts.speak('浣犲ソ锛岃浠嬬粛鑷繁銆?')

    expect(tts.isSpeaking.value).toBe(true)
    vi.advanceTimersByTime(20000)

    expect(window.speechSynthesis.cancel).toHaveBeenCalled()
    expect(tts.isSpeaking.value).toBe(false)
    expect(onEnd).toHaveBeenCalled()
    vi.useRealTimers()
  })

  it('does not cancel a long utterance while the browser is still speaking', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    window.speechSynthesis.speaking = true
    window.speechSynthesis.pending = false
    const tts = useTextToSpeech({ onEnd })

    tts.speak('This is a long interview response that should keep playing until the browser reports the utterance has ended.')
    window.speechSynthesis.cancel.mockClear()

    vi.advanceTimersByTime(20000)

    expect(window.speechSynthesis.cancel).not.toHaveBeenCalled()
    expect(tts.isSpeaking.value).toBe(true)

    window.speechSynthesis.speaking = false
    spokenUtterances[0].onend()

    expect(tts.isSpeaking.value).toBe(false)
    expect(onEnd).toHaveBeenCalledTimes(1)
    vi.useRealTimers()
  })

  it('does not cancel a started Chrome utterance at the first watchdog check while it is still speaking', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    window.speechSynthesis.speak = vi.fn((utterance) => {
      window.speechSynthesis.speaking = true
      spokenUtterances.push(utterance)
      utterance.onstart()
    })
    const tts = useTextToSpeech({ onEnd })

    tts.speak('你好，我是本次 AI 面试官。')
    window.speechSynthesis.cancel.mockClear()

    vi.advanceTimersByTime(13000)

    expect(window.speechSynthesis.cancel).not.toHaveBeenCalled()
    expect(tts.isSpeaking.value).toBe(true)
    expect(onEnd).not.toHaveBeenCalled()

    window.speechSynthesis.speaking = false
    spokenUtterances[0].onend()

    expect(tts.isSpeaking.value).toBe(false)
    expect(onEnd).toHaveBeenCalledTimes(1)
    vi.useRealTimers()
  })

  it('resets the global browser speech queue before an idle utterance starts', () => {
    const tts = useTextToSpeech()

    tts.speak('你好，我是本次 AI 面试官。')

    expect(window.speechSynthesis.cancel).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
  })

  it('resets stale browser speech state for a fresh composable instance before preview playback', () => {
    const interviewTts = useTextToSpeech()
    interviewTts.speak('上一轮面试官播报。')
    spokenUtterances[0].onend()
    window.speechSynthesis.cancel.mockClear()

    const previewTts = useTextToSpeech()
    previewTts.speak('你好，我是你的 AI 面试官。')

    expect(window.speechSynthesis.cancel).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(spokenUtterances[1].text).toBe('你好，我是你的 AI 面试官。')
  })

  it('releases speaking state when Chrome still never starts after the default voice retry', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    window.speechSynthesis.speak = vi.fn((utterance) => {
      window.speechSynthesis.speaking = true
      spokenUtterances.push(utterance)
    })
    const tts = useTextToSpeech({ onEnd })

    tts.speak('你好，我是本次 AI 面试官。')

    expect(tts.isSpeaking.value).toBe(true)
    vi.advanceTimersByTime(6000)

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(tts.isSpeaking.value).toBe(true)

    vi.advanceTimersByTime(6000)

    expect(window.speechSynthesis.cancel).toHaveBeenCalled()
    expect(tts.isSpeaking.value).toBe(false)
    expect(onEnd).toHaveBeenCalledTimes(1)
    vi.useRealTimers()
  })

  it('releases speaking state after the conservative hard timeout when Chrome keeps reporting speaking', () => {
    vi.useFakeTimers()
    const onEnd = vi.fn()
    window.speechSynthesis.speak = vi.fn((utterance) => {
      window.speechSynthesis.speaking = true
      spokenUtterances.push(utterance)
      utterance.onstart()
    })
    const tts = useTextToSpeech({ onEnd })

    tts.speak('你好，我是本次 AI 面试官。')

    expect(tts.isSpeaking.value).toBe(true)
    vi.advanceTimersByTime(61000)

    expect(window.speechSynthesis.cancel).toHaveBeenCalled()
    expect(tts.isSpeaking.value).toBe(false)
    expect(onEnd).toHaveBeenCalledTimes(1)
    vi.useRealTimers()
  })
})
