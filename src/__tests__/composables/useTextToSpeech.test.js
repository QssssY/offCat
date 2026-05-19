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
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
    expect(spokenUtterances[1].text).toBe('下一句还没结束')
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
  })

  it('stop clears speech queue', () => {
    const tts = useTextToSpeech()

    tts.speakStreaming('尚未结束')
    tts.stop()
    tts.flushRemaining()

    expect(window.speechSynthesis.cancel).toHaveBeenCalled()
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  })
})
