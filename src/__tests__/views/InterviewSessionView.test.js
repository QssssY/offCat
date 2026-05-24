import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus from 'element-plus'
import InterviewSessionView from '@/views/interview/InterviewSessionView.vue'
import { getInterviewSession, streamInterviewMessage } from '@/api/interview'
import { saveSettingsPreferences } from '@/utils/settingsPreferences'

const push = vi.fn()
const back = vi.fn()
let useSpeechToTextCall = 0
let mountedWrappers = []

const sttSupported = ref(true)
const sttRecording = ref(false)
const sttFinal = ref('')
const sttInterim = ref('')
const sttError = ref('')
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

vi.mock('@/api/interview', () => ({
  endInterview: vi.fn(() => Promise.resolve()),
  getInterviewSession: vi.fn(),
  streamInterviewMessage: vi.fn(),
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => 'token'),
}))

vi.mock('@/composables/useSpeechToText', () => ({
  useSpeechToText: vi.fn(() => {
    useSpeechToTextCall += 1
    if (useSpeechToTextCall % 2 === 1) {
      return {
      isSupported: sttSupported,
      isRecording: sttRecording,
      finalTranscript: sttFinal,
      interimTranscript: sttInterim,
      error: sttError,
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
    sttSupported.value = true
    sttRecording.value = false
    sttFinal.value = ''
    sttInterim.value = ''
    sttError.value = ''
    sttLanguage.value = 'zh-CN'
    voiceSttSupported.value = true
    voiceSttRecording.value = false
    voiceSttFinal.value = ''
    voiceSttInterim.value = ''
    voiceSttError.value = ''
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

    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        chatLogs: [
          { id: 1, messageRole: 'assistant', content: 'assistant-history', createTime: '2026-05-19 14:00:00' },
          { id: 2, messageRole: 'user', content: 'user-history', createTime: '2026-05-19 14:01:00' },
        ],
      },
    })
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

  it('uses explicit local speech recognition language preference', async () => {
    saveSettingsPreferences({
      voiceRecognitionLanguage: 'en-US'
    })

    const wrapper = mountView()
    await flushPromises()

    expect(sttLanguage.value).toBe('en-US')
    expect(voiceSttLanguage.value).toBe('en-US')
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

    wrapper.vm.handleEndVoiceCall()
    await wrapper.vm.$nextTick()
    await wrapper.findAll('.voice-call-actions button')[0].trigger('click')
    await wrapper.vm.$nextTick()

    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
  })

  it('uses local voice speaking preferences for opening speech', async () => {
    saveSettingsPreferences({
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

  it('cancels voice recognition before speaking the opening message', async () => {
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

    expect(voiceSttStart).toHaveBeenCalledTimes(1)
    expect(voiceSttCancel).toHaveBeenCalledTimes(1)
    expect(voiceSttRecording.value).toBe(false)
    expect(window.speechSynthesis.speak).toHaveBeenCalledTimes(1)
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
