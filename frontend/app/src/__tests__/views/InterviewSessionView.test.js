import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus, { ElMessage } from 'element-plus'
import InterviewSessionView from '@/views/interview/InterviewSessionView.vue'
import {
  endInterview as apiEndInterview,
  getInterviewSession,
  getInterviewSessionStatus,
  getInterviewTtsCapability,
  streamInterviewMessage,
  synthesizeInterviewTts,
} from '@/api/interview'
import { prefetchInterviewReportRoute } from '@/router/routeLoaders'
import { saveSettingsPreferences } from '@/utils/settingsPreferences'
import { useSpeechToText } from '@/composables/useSpeechToText'

const push = vi.fn()
const back = vi.fn()
const elMessageError = vi.hoisted(() => vi.fn())
const elMessageWarning = vi.hoisted(() => vi.fn())
const elMessageSuccess = vi.hoisted(() => vi.fn())
let useSpeechToTextCall = 0
let useSpeechToTextOptions = []
let mountedWrappers = []
let audioInstances = []

const sttSupported = ref(true)
const sttRecording = ref(false)
const sttFinal = ref('')
const sttInterim = ref('')
const sttError = ref('')
const sttErrorCode = ref('')
const sttLanguage = ref('zh-CN')
const sttToggle = vi.fn()
const sttStart = vi.fn(() => {
  sttRecording.value = true
})
const sttStop = vi.fn(() => {
  sttRecording.value = false
})
const sttCancel = vi.fn(() => {
  sttRecording.value = false
  sttFinal.value = ''
  sttInterim.value = ''
  sttError.value = ''
})
const voiceSttSupported = ref(true)
const voiceSttRecording = ref(false)
const voiceSttFinal = ref('')
const voiceSttInterim = ref('')
const voiceSttError = ref('')
const voiceSttErrorCode = ref('')
const voiceSttEngineStatus = ref('browser-service')
const voiceSttModelReady = ref(false)
const voiceSttLanguage = ref('zh-CN')
const voiceSttStart = vi.fn(() => {
  voiceSttRecording.value = true
})
const voiceSttStop = vi.fn(() => {
  voiceSttRecording.value = false
})
const voiceSttCancel = vi.fn(() => {
  voiceSttRecording.value = false
  voiceSttFinal.value = ''
  voiceSttInterim.value = ''
  voiceSttError.value = ''
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
    back,
    currentRoute: ref({ fullPath: '/interview/session/session-1' }),
  }),
  useRoute: () => ({
    params: {
      sessionId: 'session-1',
    },
  }),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    ElMessage: {
      ...actual.ElMessage,
      error: elMessageError,
      warning: elMessageWarning,
      success: elMessageSuccess,
    },
  }
})

vi.mock('@/api/interview', () => ({
  endInterview: vi.fn(() => Promise.resolve()),
  getInterviewSession: vi.fn(),
  getInterviewSessionStatus: vi.fn(),
  getInterviewTtsCapability: vi.fn(),
  streamInterviewMessage: vi.fn(),
  synthesizeInterviewTts: vi.fn(),
}))

vi.mock('@/router/routeLoaders', () => ({
  prefetchInterviewReportRoute: vi.fn(() => Promise.resolve()),
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'token'),
}))

vi.mock('@/composables/useSpeechToText', () => ({
  useSpeechToText: vi.fn((options = {}) => {
    useSpeechToTextCall += 1
    useSpeechToTextOptions.push(options)
    if (useSpeechToTextCall % 2 === 1) {
      return {
      isSupported: sttSupported,
      isRecording: sttRecording,
      finalTranscript: sttFinal,
      interimTranscript: sttInterim,
      error: sttError,
      errorCode: sttErrorCode,
      language: sttLanguage,
      start: sttStart,
      stop: sttStop,
      cancel: sttCancel,
      toggle: sttToggle,
      }
    }
    return {
      isSupported: voiceSttSupported,
      isRecording: voiceSttRecording,
      isVoiceActive: ref(false),
      voiceActivityAt: ref(0),
      finalTranscript: voiceSttFinal,
      interimTranscript: voiceSttInterim,
      error: voiceSttError,
      errorCode: voiceSttErrorCode,
      engineStatus: voiceSttEngineStatus,
      isModelReady: voiceSttModelReady,
      language: voiceSttLanguage,
      start: voiceSttStart,
      stop: voiceSttStop,
      cancel: voiceSttCancel,
    }
  }),
}))

vi.mock('@/assets/assistant.png', () => ({ default: '/assistant.png' }))
vi.mock('@/assets/user.png', () => ({ default: '/user.png' }))

const baseSession = {
  id: 'session-1',
  status: 0,
  difficulty: 1,
  interviewMode: 'mock',
  feedbackMode: 'after_interview',
  interactionType: 0,
  jobRole: '前端工程师',
  openingPending: false,
  chatLogs: [],
}

const createStreamResponse = (chunks = ['data: {"type":"done"}\n\n']) => {
  const read = vi.fn()
  chunks.forEach((chunk) => {
    read.mockResolvedValueOnce({
      done: false,
      value: new TextEncoder().encode(chunk),
    })
  })
  read.mockResolvedValueOnce({
    done: true,
    value: undefined,
  })
  return {
    ok: true,
    body: {
      getReader: () => ({ read }),
    },
  }
}

const createHttpResponse = (status, body = {}) => ({
  ok: false,
  status,
  json: vi.fn(() => Promise.resolve(body)),
})

const viewSource = () => readFileSync(resolve(process.cwd(), 'src/views/interview/InterviewSessionView.vue'), 'utf8')

const mountView = () => {
  const wrapper = mount(InterviewSessionView, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        ElDialog: {
          template: '<div><slot /><slot name="footer" /></div>',
        },
        ElResult: {
          props: ['title', 'subTitle', 'icon'],
          template: '<div><slot name="extra" /></div>',
        },
        ElInput: {
          props: ['modelValue'],
          emits: ['update:modelValue', 'keyup'],
          template: `
            <textarea
              :value="modelValue"
              @input="$emit('update:modelValue', $event.target.value)"
              @keyup="$emit('keyup', $event)"
            />
          `,
        },
        ElButton: {
          props: ['disabled', 'loading', 'type', 'plain', 'size', 'link', 'circle'],
          emits: ['click'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
        ElIcon: {
          template: '<span><slot /></span>',
        },
      },
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('InterviewSessionView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mountedWrappers = []
    useSpeechToTextCall = 0
    useSpeechToTextOptions = []
    sttSupported.value = true
    sttRecording.value = false
    sttFinal.value = ''
    sttInterim.value = ''
    sttError.value = ''
    sttErrorCode.value = ''
    sttLanguage.value = 'zh-CN'
    voiceSttSupported.value = true
    voiceSttRecording.value = false
    voiceSttFinal.value = ''
    voiceSttInterim.value = ''
    voiceSttError.value = ''
    voiceSttErrorCode.value = ''
    voiceSttEngineStatus.value = 'browser-service'
    voiceSttModelReady.value = false
    voiceSttLanguage.value = 'zh-CN'
    window.speechSynthesis = {
      getVoices: vi.fn(() => [{ lang: 'zh-CN' }]),
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      onvoiceschanged: null
    }
    window.SpeechSynthesisUtterance = vi.fn(function SpeechSynthesisUtterance(text) {
      this.text = text
    })
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

    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: 'assistant-history', createTime: '2026-05-19 14:00:00' },
          { id: 2, messageRole: 'user', content: 'user-history', createTime: '2026-05-19 14:01:00' },
        ],
      },
    })
    getInterviewTtsCapability.mockResolvedValue({
      data: { available: false, engine: 'browser' },
    })
    synthesizeInterviewTts.mockResolvedValue(new Blob(['mp3'], { type: 'audio/mpeg' }))
    streamInterviewMessage.mockResolvedValue(createStreamResponse())
  })

  afterEach(() => {
    mountedWrappers.forEach((wrapper) => wrapper.unmount())
    mountedWrappers = []
  })

  it('preserves manual edits by cancelling speech input instead of overwriting the draft', async () => {
    const wrapper = mountView()
    await flushPromises()

    const textarea = wrapper.find('textarea')
    await textarea.setValue('已有草稿')

    sttRecording.value = true
    sttFinal.value = '语音'
    sttInterim.value = '输入'
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.inputMessage).toBe('已有草稿语音输入')

    await textarea.setValue('已有草稿语音输入，手动补充')
    await wrapper.vm.$nextTick()

    expect(sttCancel).toHaveBeenCalledTimes(1)
    expect(wrapper.vm.inputMessage).toBe('已有草稿语音输入，手动补充')
  })

  it('cancels active speech recognition before sending so the draft stays cleared', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '准备发送的回答'
    sttRecording.value = true
    await wrapper.vm.$nextTick()

    await wrapper.vm.sendMessage()
    await flushPromises()

    expect(sttCancel).toHaveBeenCalled()
    expect(streamInterviewMessage).toHaveBeenCalledWith(
      'session-1',
      expect.objectContaining({
        content: '准备发送的回答',
      }),
      'token',
      expect.objectContaining({ signal: expect.anything() })
    )
    expect(wrapper.vm.inputMessage).toBe('')
    expect(sttRecording.value).toBe(false)
    expect(sttFinal.value).toBe('')
    expect(sttInterim.value).toBe('')
  })

  it('uses the input draft when a button click event is passed to sendMessage', async () => {
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '语音输入后的回答'
    await wrapper.vm.sendMessage(new MouseEvent('click'))
    await flushPromises()

    expect(streamInterviewMessage).toHaveBeenCalledWith(
      'session-1',
      expect.objectContaining({
        content: '语音输入后的回答',
      }),
      'token',
      expect.objectContaining({ signal: expect.anything() })
    )
    expect(wrapper.vm.inputMessage).toBe('')
  })

  it('does not reconnect automatically when streaming reply fails', async () => {
    streamInterviewMessage.mockRejectedValueOnce(new Error('network down'))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '准备发送的回答'
    await wrapper.vm.sendMessage()
    await flushPromises()

    expect(streamInterviewMessage).toHaveBeenCalledTimes(1)
    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
  })

  it('marks the assistant message as failed when SSE sends an error payload', async () => {
    streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
      'data: {"type":"error","message":"AI 服务暂时不可用"}\n\n',
    ]))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '准备发送的回答'
    await wrapper.vm.sendMessage()
    await flushPromises()

    const assistantMessage = wrapper.vm.sessionData.chatLogs.at(-1)
    expect(assistantMessage.messageRole).toBe('assistant')
    expect(assistantMessage.status).toBe('error')
    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
  })

  it('marks the assistant message as failed when SSE ends before done', async () => {
    streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
      'data: {"type":"content","content":"半截回复"}\n\n',
    ]))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '准备发送的回答'
    await wrapper.vm.sendMessage()
    await flushPromises()

    const assistantMessage = wrapper.vm.sessionData.chatLogs.at(-1)
    expect(assistantMessage.rawContent).toBe('半截回复')
    expect(assistantMessage.status).toBe('error')
    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
  })

  it('recovers the persisted assistant reply when SSE closes after the server saved it', async () => {
    getInterviewSession
      .mockResolvedValueOnce({
        data: {
          ...baseSession,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: 'assistant-history', createTime: '2026-05-19 14:00:00' },
            { id: 2, messageRole: 'user', content: 'user-history', createTime: '2026-05-19 14:01:00' },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          ...baseSession,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: 'assistant-history', createTime: '2026-05-19 14:00:00' },
            { id: 2, messageRole: 'user', content: 'user-history', createTime: '2026-05-19 14:01:00' },
            { id: 3, messageRole: 'user', content: '准备发送的回答', createTime: '2026-05-19 14:02:00' },
            { id: 4, messageRole: 'assistant', content: '服务端已保存的回复', createTime: '2026-05-19 14:02:08' },
          ],
        },
      })
    streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
      'data: {"type":"content","content":"服务端已保存的回复"}\n\n',
    ]))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '准备发送的回答'
    await wrapper.vm.sendMessage()
    await flushPromises()

    expect(getInterviewSession).toHaveBeenCalledTimes(2)
    expect(ElMessage.error).not.toHaveBeenCalled()
    const assistantMessage = wrapper.vm.sessionData.chatLogs.at(-1)
    expect(assistantMessage.messageRole).toBe('assistant')
    expect(assistantMessage.content).toBe('服务端已保存的回复')
    expect(assistantMessage.status).toBe('done')
    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
  })

  it('unlocks the input and redirects when streaming returns 401', async () => {
    streamInterviewMessage.mockResolvedValueOnce(createHttpResponse(401))
    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.inputMessage = '准备发送的回答'
    await wrapper.vm.sendMessage()
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/interview/session/session-1' },
    })
    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
    expect(wrapper.vm.sessionData.chatLogs.at(-1).status).toBe('error')
  })

  it('keeps voice call active and shows a clear reminder when streaming is rate limited', async () => {
    streamInterviewMessage.mockResolvedValueOnce(createHttpResponse(429, {
      message: '请求过于频繁，请稍后再试'
    }))
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.sendMessage('我负责订单模块')
    await flushPromises()

    expect(wrapper.vm.voiceCall.isVoiceMode.value).toBe(true)
    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.vm.sending).toBe(false)
    expect(ElMessage.warning).toHaveBeenCalledWith('发送太频繁，请稍后继续。10 分钟内最多 60 轮对话。')
    expect(ElMessage.error).not.toHaveBeenCalledWith('请求过于频繁，请稍后再试')
  })

  it('polls pending opening speech quickly before falling back to the long interval', async () => {
    vi.useFakeTimers()
    try {
      getInterviewSession.mockResolvedValueOnce({
        data: {
          ...baseSession,
          interactionType: 1,
          openingPending: true,
          chatLogs: [],
        },
      })
      getInterviewSessionStatus.mockResolvedValueOnce({
        data: {
          sessionId: 'session-1',
          status: 0,
          openingPending: false,
          reportReady: false,
        },
      })
      getInterviewSession.mockResolvedValueOnce({
        data: {
          ...baseSession,
          interactionType: 1,
          openingPending: false,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: '你好，请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
          ],
        },
      })

      const wrapper = mountView()
      await flushPromises()

      expect(wrapper.text()).toContain('AI 面试官正在准备中')
      expect(getInterviewSession).toHaveBeenCalledTimes(1)

      await vi.advanceTimersByTimeAsync(500)
      await flushPromises()

      expect(getInterviewSessionStatus).toHaveBeenCalledTimes(1)
      expect(getInterviewSession).toHaveBeenCalledTimes(2)
      expect(wrapper.text()).toContain('你好，请先做一个自我介绍。')
    } finally {
      vi.useRealTimers()
    }
  })

  it('stops opening speech polling when lightweight status reports generated', async () => {
    vi.useFakeTimers()
    try {
      getInterviewSession.mockResolvedValueOnce({
        data: {
          ...baseSession,
          interactionType: 1,
          openingPending: true,
          chatLogs: [],
        },
      })
      getInterviewSessionStatus.mockResolvedValueOnce({
        data: {
          sessionId: 'session-1',
          status: 0,
          openingPending: true,
          openingGenerated: true,
          reportReady: false,
        },
      })
      getInterviewSession.mockResolvedValueOnce({
        data: {
          ...baseSession,
          interactionType: 1,
          openingPending: false,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: '你好，请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
          ],
        },
      })

      const wrapper = mountView()
      await flushPromises()

      await vi.advanceTimersByTimeAsync(500)
      await flushPromises()

      expect(getInterviewSessionStatus).toHaveBeenCalledTimes(1)
      expect(getInterviewSession).toHaveBeenCalledTimes(2)
      expect(wrapper.text()).toContain('你好，请先做一个自我介绍。')

      await vi.advanceTimersByTimeAsync(3000)
      await flushPromises()
      expect(getInterviewSessionStatus).toHaveBeenCalledTimes(1)
    } finally {
      getInterviewSession.mockReset()
      getInterviewSessionStatus.mockReset()
      vi.useRealTimers()
    }
  })

  it('prefetches the report waiting page and navigates there after ending without reloading full session detail', async () => {
    getInterviewSession.mockResolvedValueOnce({
      data: {
        ...baseSession,
        status: 0,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.endInterview()
    expect(prefetchInterviewReportRoute).toHaveBeenCalledTimes(1)

    await wrapper.vm.confirmEndInterview()
    await flushPromises()

    expect(apiEndInterview).toHaveBeenCalledWith('session-1')
    expect(push).toHaveBeenCalledWith('/interview/report/session-1')
    expect(getInterviewSession).toHaveBeenCalledTimes(1)
  })

  it('shows voice call overlay above chat for voice interview without auto starting microphone', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: 'assistant-history', createTime: '2026-05-19 14:00:00' },
          { id: 2, messageRole: 'user', content: 'user-history', createTime: '2026-05-19 14:01:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('语音面试待开始')
    expect(wrapper.find('.voice-call-overlay').exists()).toBe(true)
    expect(wrapper.find('.chat-messages').exists()).toBe(true)
    expect(wrapper.find('.input-area').exists()).toBe(false)
    expect(wrapper.text()).toContain('assistant-history')
    expect(wrapper.text()).toContain('user-history')
    expect(wrapper.find('.voice-call-overlay [title="折叠到聊天界面"]').exists()).toBe(true)
    expect(wrapper.find('.voice-call-overlay [title="开始通话"]').exists()).toBe(true)
    expect(wrapper.find('.voice-call-overlay [title="停止收听并发送"]').exists()).toBe(false)
    expect(wrapper.find('.voice-call-overlay [title="挂断语音通话"]').exists()).toBe(true)
    expect(sttStart).not.toHaveBeenCalled()
    expect(voiceSttStart).not.toHaveBeenCalled()
  })

  it('keeps voice interview overlay icons visually centered and prominent', () => {
    const source = viewSource()

    expect(source).toContain('width: clamp(224px, 22vw, 252px);')
    expect(source).toContain('height: clamp(224px, 22vw, 252px);')
    expect(source).toContain('.voice-icon-btn :deep(.feature-icon)')
    expect(source).toContain('width: 32px;')
    expect(source).toContain('height: 32px;')
    expect(source).toContain('background: rgba(255, 140, 66, 0.045);')
  })

  it('exposes refined session surface hooks for the redesigned conversation UI', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.interview-session-shell').exists()).toBe(true)
    expect(wrapper.find('.session-main-surface').exists()).toBe(true)
    expect(wrapper.find('.conversation-surface').exists()).toBe(true)
    expect(wrapper.findAll('.message-row.message-entrance')).toHaveLength(2)
    expect(wrapper.find('.assistant-row .message-role-pill').exists()).toBe(true)
    expect(wrapper.find('.user-row .message-role-pill').exists()).toBe(true)
  })

  it('keeps interview session dark mode and motion rules scoped correctly', () => {
    const source = viewSource()

    expect(source).toContain(':global(html[data-theme="dark"] .interview-session-view)')
    expect(source).toContain('@keyframes sessionSurfaceIn')
    expect(source).toContain('@keyframes messageFloatIn')
    expect(source).toContain('@keyframes voiceCallEnter')
    expect(source).toContain('animation: messageFloatIn')
    expect(source).toContain('prefers-reduced-motion: reduce')
    expect(source).toContain('sessionSurfaceIn,')
    expect(source).not.toMatch(/\n\[data-theme="dark"\]\s+\.session-status-bar/)
  })

  it('keeps the first assistant message fully visible below the top status bar', () => {
    const source = viewSource()

    expect(source).toContain('scroll-padding-top: 24px;')
    expect(source).toMatch(/\.chat-messages\s*\{[\s\S]*padding:\s*24px 16px 12px 0;/)
    expect(source).toMatch(/@media \(max-width: 767px\)[\s\S]*\.chat-messages\s*\{[\s\S]*padding-top:\s*18px;/)
  })

  it('hides duplicate top voice window controls on mobile because dock controls remain available', () => {
    const source = viewSource()

    expect(source).toContain('@media (max-width: 767px)')
    expect(source).toMatch(/@media \(max-width: 767px\)[\s\S]*\.voice-window-bar\s*\{[\s\S]*display:\s*none;/)
    expect(source).toMatch(/@media \(max-width: 767px\)[\s\S]*\.voice-dock-actions\s*\{/)
  })

  it('starts voice call microphone without toggling text speech input', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(voiceSttStart).toHaveBeenCalledTimes(1)
    expect(sttStart).not.toHaveBeenCalled()
    expect(sttRecording.value).toBe(false)
    expect(voiceSttRecording.value).toBe(true)
    expect(wrapper.find('.voice-call-overlay [title="停止收听并发送"]').exists()).toBe(true)
  })

  it('speaks the next assistant reply after a voice answer is sent', async () => {
    streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
      'data: {"type":"content","content":"好的，我们继续。"}\n\n',
      'data: {"type":"done"}\n\n',
    ]))
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    window.speechSynthesis.speak.mock.calls[0][0].onstart()
    window.speechSynthesis.speak.mock.calls[0][0].onend()
    await wrapper.vm.$nextTick()
    window.speechSynthesis.speak.mockClear()
    window.speechSynthesis.cancel.mockClear()

    await wrapper.vm.sendMessage('好的')
    await flushPromises()

    expect(window.speechSynthesis.cancel).toHaveBeenCalled()
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].text).toBe('好的，我们继续。')
    window.speechSynthesis.speak.mock.calls[0][0].onstart()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.replyLocked).toBe(false)
    expect(wrapper.find('.voice-call-overlay [title="切换静音"]').attributes('disabled')).toBeDefined()

    window.speechSynthesis.speak.mock.calls[0][0].onend()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.voice-call-overlay [title="切换静音"]').attributes('disabled')).toBeUndefined()
  })

  it('retries the next assistant reply when Chrome accepts streamed speech but never starts it', async () => {
    vi.useFakeTimers()
    try {
      streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
        'data: {"type":"content","content":"好的，我们继续。"}\n\n',
        'data: {"type":"done"}\n\n',
      ]))
      getInterviewSession.mockResolvedValue({
        data: {
          ...baseSession,
          interactionType: 1,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
          ],
        },
      })

      const wrapper = mountView()
      await flushPromises()

      await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
      window.speechSynthesis.speak.mock.calls[0][0].onstart()
      window.speechSynthesis.speaking = false
      window.speechSynthesis.pending = false
      window.speechSynthesis.speak.mock.calls[0][0].onend()
      await wrapper.vm.$nextTick()
      window.speechSynthesis.speak.mockClear()
      window.speechSynthesis.cancel.mockClear()

      window.speechSynthesis.speaking = true
      window.speechSynthesis.pending = true
      await wrapper.vm.sendMessage('好的')
      await flushPromises()

      expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)

      await vi.advanceTimersByTimeAsync(6000)
      await wrapper.vm.$nextTick()

      expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
      expect(window.speechSynthesis.speak.mock.calls[1][0].text).toBe('好的，我们继续。')
      expect(window.speechSynthesis.speak.mock.calls[1][0].voice).toBeUndefined()
    } finally {
      vi.useRealTimers()
    }
  })

  it('speaks the follow-up part after an immediate feedback block in a voice reply', async () => {
    streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
      'data: {"type":"content","content":"<FEEDBACK>上一题反馈：回答偏短。</FEEDBACK>"}\n\n',
      'data: {"type":"content","content":"我们换一个问题，请介绍你的教学方法。"}\n\n',
      'data: {"type":"done"}\n\n',
    ]))
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        feedbackMode: 'immediate',
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    window.speechSynthesis.speak.mock.calls[0][0].onstart()
    window.speechSynthesis.speak.mock.calls[0][0].onend()
    await wrapper.vm.$nextTick()
    window.speechSynthesis.speak.mockClear()

    await wrapper.vm.sendMessage('好的')
    await flushPromises()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].text).toBe('我们换一个问题，请介绍你的教学方法。')
  })

  it('does not switch from voice mode to text mode while the interviewer is speaking', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    window.speechSynthesis.speak.mock.calls[0][0].onstart()
    await wrapper.vm.$nextTick()

    const switchButton = wrapper.find('.voice-call-overlay [title="切换文本模式"]')
    expect(switchButton.attributes('disabled')).toBeDefined()
    const micButton = wrapper.find('.voice-call-overlay [title="切换静音"]')
    expect(micButton.attributes('disabled')).toBeDefined()

    wrapper.vm.switchToTextMode()
    await wrapper.vm.$nextTick()
    wrapper.vm.handleMicControl()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.preferTextInput).toBe(false)
    expect(wrapper.find('.voice-call-overlay').exists()).toBe(true)
    expect(ElMessage.warning).toHaveBeenCalledWith('AI 面试官播报结束后再切换模式')
    expect(voiceSttStop).not.toHaveBeenCalled()
  })

  it('does not switch from text fallback to voice mode while the interviewer is speaking', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    wrapper.vm.switchToTextMode()
    wrapper.vm.textToSpeech.speak('请继续介绍你的项目经历。')
    window.speechSynthesis.speak.mock.calls[0][0].onstart()
    await wrapper.vm.$nextTick()

    const voiceModeButton = wrapper.find('.input-actions button')
    expect(voiceModeButton.text()).toContain('语音模式')
    expect(voiceModeButton.attributes('disabled')).toBeDefined()

    wrapper.vm.switchToVoiceMode()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.preferTextInput).toBe(true)
    expect(voiceSttStart).not.toHaveBeenCalled()
    expect(ElMessage.warning).toHaveBeenCalledWith('AI 面试官播报结束后再切换模式')
  })

  it('uses explicit local speech recognition language preference', async () => {
    saveSettingsPreferences({
      voiceRecognitionLanguage: 'en-US'
    })

    const wrapper = mountView()
    await flushPromises()

    expect(sttLanguage.value).toBe('en-US')
    expect(voiceSttLanguage.value).toBe('en-US')
  })

  it('passes local recognition engine preference into both speech recognizers', async () => {
    saveSettingsPreferences({
      voiceRecognitionEngine: 'system_local'
    })

    mountView()
    await flushPromises()

    expect(useSpeechToText).toHaveBeenCalledTimes(2)
    expect(useSpeechToTextOptions[0]).toEqual({})
    expect(useSpeechToTextOptions[1]).toEqual({})
  })

  it('ignores legacy offline recognition preference after offline engine removal', async () => {
    saveSettingsPreferences({
      voiceRecognitionEngine: 'offline_sherpa'
    })

    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    mountView()
    await flushPromises()

    expect(useSpeechToTextOptions[0]).toEqual({})
    expect(useSpeechToTextOptions[1]).toEqual({})
    expect(voiceSttStart).not.toHaveBeenCalled()
  })
  it('speaks the opening message once when the first voice call starts', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].text).toBe('你好，我是本次 AI 面试官。请先做一个自我介绍。')
    window.speechSynthesis.speak.mock.calls[0][0].onstart()

    wrapper.vm.handleEndVoiceCall()
    await wrapper.vm.$nextTick()
    await wrapper.findAll('.voice-call-actions button')[0].trigger('click')
    await wrapper.vm.$nextTick()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
  })

  it('uses cloud TTS for the opening message when user custom TTS is available', async () => {
    getInterviewTtsCapability.mockResolvedValue({
      data: { available: true, engine: 'user_custom_tts', configType: 'interview' },
    })
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await flushPromises()

    expect(getInterviewTtsCapability).toHaveBeenCalledWith('session-1')
    expect(synthesizeInterviewTts).toHaveBeenCalledWith(
      'session-1',
      '你好，我是本次 AI 面试官。请先做一个自我介绍。',
      expect.any(Object)
    )
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
    expect(audioInstances[0].play).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('播报音色：自定义云端 TTS')
  })

  it('labels cloud TTS as system source when system fallback is available', async () => {
    getInterviewTtsCapability.mockResolvedValue({
      data: { available: true, engine: 'system', systemTtsAvailable: true },
    })
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(getInterviewTtsCapability).toHaveBeenCalledWith('session-1')
    expect(wrapper.text()).toContain('播报音色：系统云端 TTS')
  })

  it('shows cloud TTS preparing state before real audio playback starts', async () => {
    let resolveAudio
    synthesizeInterviewTts.mockReturnValueOnce(new Promise((resolve) => {
      resolveAudio = resolve
    }))
    getInterviewTtsCapability.mockResolvedValue({
      data: { available: true, engine: 'user_custom_tts', configType: 'interview' },
    })
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('AI 语音准备中')
    expect(wrapper.find('.voice-wave').classes()).not.toContain('speaking')

    resolveAudio(new Blob(['mp3'], { type: 'audio/mpeg' }))
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('AI 正在播报')
    expect(wrapper.find('.voice-wave').classes()).toContain('speaking')
  })

  it('uses cloud TTS for streamed voice replies without browser speech synthesis', async () => {
    getInterviewTtsCapability.mockResolvedValue({
      data: { available: true, engine: 'user_custom_tts', configType: 'interview' },
    })
    streamInterviewMessage.mockResolvedValueOnce(createStreamResponse([
      'data: {"type":"content","content":"好的，我们继续。"}\n\n',
      'data: {"type":"done"}\n\n',
    ]))
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.sendMessage('我负责订单模块')
    await flushPromises()

    expect(synthesizeInterviewTts).toHaveBeenCalledWith('session-1', '好的，我们继续。', expect.any(Object))
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  })

  it('falls back to browser TTS once when cloud TTS synthesis fails', async () => {
    getInterviewTtsCapability.mockResolvedValue({
      data: { available: true, engine: 'user_custom_tts', configType: 'interview' },
    })
    synthesizeInterviewTts.mockRejectedValue(new Error('cloud unavailable'))
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await flushPromises()

    expect(ElMessage.warning).toHaveBeenCalledWith('云端语音暂不可用，已切回浏览器播报')
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].text).toBe('你好，我是本次 AI 面试官。')

    wrapper.vm.handleEndVoiceCall()
    await wrapper.vm.$nextTick()
    await wrapper.findAll('.voice-call-actions button')[0].trigger('click')
    await flushPromises()

    expect(ElMessage.warning).toHaveBeenCalledTimes(1)
  })

  it('falls back to opening speech after Chrome voices fail to load', async () => {
    vi.useFakeTimers()
    window.speechSynthesis.getVoices = vi.fn(() => [])
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(window.speechSynthesis.resume).toHaveBeenCalled()
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()

    await vi.advanceTimersByTimeAsync(900)
    await wrapper.vm.$nextTick()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
    expect(window.speechSynthesis.speak.mock.calls[0][0].text).toBe('你好，我是本次 AI 面试官。请先做一个自我介绍。')
    vi.useRealTimers()
  })

  it('retries the opening message when Chrome accepts speech but never starts it', async () => {
    vi.useFakeTimers()
    try {
      getInterviewSession.mockResolvedValue({
        data: {
          ...baseSession,
          interactionType: 1,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
          ],
        },
      })

      const wrapper = mountView()
      await flushPromises()

      await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
      await wrapper.vm.$nextTick()

      expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
      await vi.advanceTimersByTimeAsync(6000)
      await wrapper.vm.$nextTick()

      expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(2)
      expect(window.speechSynthesis.speak.mock.calls[1][0].text).toBe('你好，我是本次 AI 面试官。请先做一个自我介绍。')
      expect(voiceSttStart).not.toHaveBeenCalled()

      window.speechSynthesis.speak.mock.calls[1][0].onstart()
      window.speechSynthesis.speak.mock.calls[1][0].onend()
      await vi.advanceTimersByTimeAsync(800)
      await wrapper.vm.$nextTick()

      expect(voiceSttStart).toHaveBeenCalledTimes(1)
    } finally {
      vi.useRealTimers()
    }
  })

  it('uses local voice speaking preferences for opening speech', async () => {
    saveSettingsPreferences({
      voicePreferredType: 'system',
      voiceSpeakingRate: 1.1,
      voicePitch: 0.95,
      voiceVolume: 0.6
    })
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    const utterance = window.speechSynthesis.speak.mock.calls[0][0]
    expect(utterance.rate).toBe(1.1)
    expect(utterance.pitch).toBe(0.95)
    expect(utterance.volume).toBe(0.6)
  })

  it('uses selected preset speaking parameters over stored slider values for opening speech', async () => {
    saveSettingsPreferences({
      voicePreferredType: 'slow_clear',
      voiceSpeakingRate: 1.1,
      voicePitch: 0.95,
      voiceVolume: 0.6
    })
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    const utterance = window.speechSynthesis.speak.mock.calls[0][0]
    expect(utterance.rate).toBe(0.75)
    expect(utterance.pitch).toBe(1.02)
    expect(utterance.volume).toBe(0.6)
  })

  it('defers voice recognition until the opening message finishes speaking', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，欢迎参加初级数学老师面试。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(voiceSttStart).not.toHaveBeenCalled()
    expect(voiceSttCancel).not.toHaveBeenCalled()
    expect(voiceSttRecording.value).toBe(false)
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)

    vi.useFakeTimers()
    window.speechSynthesis.speak.mock.calls[0][0].onstart()
    window.speechSynthesis.speak.mock.calls[0][0].onend()
    await wrapper.vm.$nextTick()
    await vi.advanceTimersByTimeAsync(1500)
    vi.useRealTimers()
    await wrapper.vm.$nextTick()

    expect(voiceSttStart).toHaveBeenCalledTimes(1)
  })

  it('does not speak opening message again when continuing a voice interview with user replies', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '你好，我是本次 AI 面试官。请先做一个自我介绍。', createTime: '2026-05-19 14:00:00' },
          { id: 2, messageRole: 'user', content: '我已经完成了自我介绍。', createTime: '2026-05-19 14:01:00' },
          { id: 3, messageRole: 'assistant', content: '请继续介绍你的项目经历。', createTime: '2026-05-19 14:02:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(voiceSttStart).toHaveBeenCalledTimes(1)
    expect(voiceSttCancel).not.toHaveBeenCalled()
    expect(voiceSttRecording.value).toBe(true)
    expect(window.speechSynthesis.speak).not.toHaveBeenCalled()
  })

  it('manually stops voice listening and sends recognized transcript from the overlay', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    voiceSttFinal.value = '我负责订单模块'
    await wrapper.vm.$nextTick()

    await wrapper.find('.voice-call-overlay [title="停止收听并发送"]').trigger('click')
    await flushPromises()

    expect(streamInterviewMessage).toHaveBeenCalledWith(
      'session-1',
      expect.objectContaining({
        content: '我负责订单模块',
      }),
      'token',
      expect.objectContaining({ signal: expect.anything() })
    )
    expect(voiceSttStop).toHaveBeenCalled()
  })

  it('keeps the voice call active and auto-recovers when recognition has a recoverable interruption', async () => {
    vi.useFakeTimers()
    try {
      getInterviewSession.mockResolvedValue({
        data: {
          ...baseSession,
          interactionType: 1,
          chatLogs: [],
        },
      })

      const wrapper = mountView()
      await flushPromises()

      await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
      voiceSttFinal.value = '我负责订单模块'
      await wrapper.vm.$nextTick()

      voiceSttRecording.value = false
      voiceSttErrorCode.value = 'no-transcript'
      voiceSttError.value = '检测到麦克风输入，但浏览器未返回识别文字，已降级为手动输入。错误码：no-transcript'
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.voice-call-overlay').exists()).toBe(true)
      expect(wrapper.text()).not.toContain('等待继续收音')
      expect(wrapper.find('.voice-call-overlay [title="停止收听并发送"]').exists()).toBe(true)
      expect(wrapper.find('.voice-call-overlay [title="开始通话"]').exists()).toBe(false)
      expect(voiceSttCancel).not.toHaveBeenCalled()

      await vi.advanceTimersByTimeAsync(1000)
      await wrapper.vm.$nextTick()

      expect(voiceSttStart).toHaveBeenCalledTimes(2)
      expect(wrapper.vm.voiceCall.pendingMessage.value).toBe('我负责订单模块')
    } finally {
      vi.useRealTimers()
    }
  })

  it('keeps the voice interview running and submits text fallback answers when browser speech service is unavailable', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '请介绍你的项目经历。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    voiceSttInterim.value = '我负责订单模块'
    await wrapper.vm.$nextTick()

    voiceSttRecording.value = false
    voiceSttErrorCode.value = 'network'
    voiceSttError.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
    expect(wrapper.text()).toContain('语音识别暂不可用，已自动切换为文本回答')
    expect(wrapper.text()).not.toContain('仍要重试语音面试吗')
    expect(wrapper.find('.voice-degraded-banner [title="重试语音面试"]').exists()).toBe(true)
    expect(wrapper.find('.voice-text-fallback-card').exists()).toBe(false)
    expect(wrapper.vm.inputMessage).toBe('我负责订单模块')

    expect(wrapper.find('.input-container textarea').exists()).toBe(true)

    await wrapper.find('.input-container textarea').setValue('我负责订单模块和支付链路')
    await wrapper.find('.send-btn').trigger('click')
    await flushPromises()

    expect(wrapper.vm.voiceCall.isVoiceMode.value).toBe(true)
    expect(streamInterviewMessage).toHaveBeenCalledWith(
      'session-1',
      expect.objectContaining({
        content: '我负责订单模块和支付链路',
      }),
      'token',
      expect.objectContaining({ signal: expect.anything() })
    )
  })

  it('keeps text fallback after a text answer receives an interviewer reply', async () => {
    vi.useFakeTimers()
    try {
      getInterviewSession.mockResolvedValue({
        data: {
          ...baseSession,
          interactionType: 1,
          chatLogs: [
            { id: 1, messageRole: 'assistant', content: 'assistant question', createTime: '2026-05-19 14:00:00' },
          ],
        },
      })

      const wrapper = mountView()
      await flushPromises()

      await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
      voiceSttRecording.value = false
      voiceSttErrorCode.value = 'network'
      voiceSttError.value = 'speech service unavailable'
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.voiceCall.isVoiceMode.value).toBe(true)
      expect(wrapper.vm.voiceCall.isTextFallbackMode.value).toBe(true)
      expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
      expect(wrapper.find('.input-container textarea').exists()).toBe(true)

      voiceSttStart.mockClear()
      await wrapper.find('.input-container textarea').setValue('fallback answer')
      await wrapper.find('.send-btn').trigger('click')
      await flushPromises()
      await vi.advanceTimersByTimeAsync(0)
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.voiceCall.isVoiceMode.value).toBe(true)
      expect(wrapper.vm.voiceCall.isTextFallbackMode.value).toBe(true)
      expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
      expect(wrapper.find('.input-container textarea').exists()).toBe(true)
      expect(voiceSttStart).not.toHaveBeenCalled()
    } finally {
      vi.useRealTimers()
    }
  })

  it('shows text answer fallback when voice speech recognition is unsupported', async () => {
    voiceSttSupported.value = false
    voiceSttEngineStatus.value = 'unsupported'
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '请介绍你的项目经历。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
    expect(wrapper.text()).toContain('当前浏览器不支持语音，已自动切换为文本回答')
    expect(wrapper.find('[title="重试语音面试"]').exists()).toBe(false)
    expect(wrapper.find('.voice-text-fallback-card').exists()).toBe(false)

    expect(wrapper.find('.input-container textarea').exists()).toBe(true)

    await wrapper.find('.input-container textarea').setValue('我负责订单模块和支付链路')
    await wrapper.find('.send-btn').trigger('click')
    await flushPromises()

    expect(voiceSttStart).not.toHaveBeenCalled()
    expect(streamInterviewMessage).toHaveBeenCalledWith(
      'session-1',
      expect.objectContaining({
        content: '我负责订单模块和支付链路',
      }),
      'token',
      expect.objectContaining({ signal: expect.anything() })
    )
  })

  it('shows text answer fallback when browser text to speech is unsupported', async () => {
    delete window.speechSynthesis
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: '请介绍你的项目经历。', createTime: '2026-05-19 14:00:00' },
        ],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.voice-text-fallback-card').exists()).toBe(false)
    expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
    expect(wrapper.find('.input-container textarea').exists()).toBe(true)
    expect(wrapper.text()).toContain('当前浏览器不支持语音，已自动切换为文本回答')
  })

  it('offers a retry voice action above the text input when speech recognition degrades', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    voiceSttRecording.value = false
    voiceSttErrorCode.value = 'service-not-allowed'
    voiceSttError.value = '当前浏览器语音服务暂不可用，可继续输入回答；需要恢复语音时请手动点击重试语音。'
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.voice-text-fallback-card').exists()).toBe(false)
    expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
    expect(wrapper.find('.input-container textarea').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('仍要重试语音面试吗')

    voiceSttStart.mockClear()
    await wrapper.find('[title="重试语音面试"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(voiceSttStart).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.voice-call-overlay').exists()).toBe(true)
  })

  it('does not show local language pack install prompts inside the voice call overlay', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.voice-call-overlay').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('可安装浏览器本地语音包')
    expect(wrapper.text()).not.toContain('浏览器本地语音包已安装')
    expect(wrapper.find('[title="安装浏览器本地语音包"]').exists()).toBe(false)
  })

  it('shows browser recognition status during voice calls', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })
    voiceSttStart.mockImplementationOnce(() => {})

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('识别引擎：浏览器语音识别')
    expect(wrapper.text()).not.toContain('正在聆听')
    expect(wrapper.text()).toContain('通话准备中')
    expect(wrapper.text()).not.toContain('sherpa-onnx')
  })

  it('labels browser recognition as listening after recording starts', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('.voice-dock-actions .voice-icon-btn')[1].trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('识别引擎：浏览器语音识别')
    expect(wrapper.text()).toContain('正在聆听')
    expect(wrapper.text()).not.toContain('通话准备中')
  })

  it('shows browser recognition failures without suggesting an offline package download', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })
    voiceSttEngineStatus.value = 'unavailable'

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('识别引擎：浏览器语音识别不可用')
    expect(wrapper.text()).not.toContain('离线语音包')
  })

  it('collapses voice call overlay back to the bottom call bar', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[aria-label="折叠到聊天界面"]').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.voice-call-overlay').exists()).toBe(false)
    expect(wrapper.find('.voice-call-collapsed-stage').exists()).toBe(true)
    expect(wrapper.find('.input-area').exists()).toBe(true)
    expect(wrapper.text()).toContain('开始通话')
  })

  it('shows a complete set of same-surface voice controls after starting collapsed call', async () => {
    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        interactionType: 1,
        chatLogs: [],
      },
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[aria-label="折叠到聊天界面"]').trigger('click')
    await wrapper.vm.$nextTick()
    await wrapper.find('.voice-call-actions button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('.voice-call-actions button')).toHaveLength(4)
    expect(wrapper.text()).toContain('停止收听并发送')
    expect(wrapper.text()).toContain('展开')
    expect(wrapper.text()).toContain('挂断')
  })
})
