import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import ElementPlus from 'element-plus'
import InterviewSessionView from '@/views/interview/InterviewSessionView.vue'
import { getInterviewSession, streamInterviewMessage } from '@/api/interview'

const push = vi.fn()
const back = vi.fn()

const sttSupported = ref(true)
const sttRecording = ref(false)
const sttFinal = ref('')
const sttInterim = ref('')
const sttError = ref('')
const sttLanguage = ref('zh-CN')
const sttToggle = vi.fn()
const sttCancel = vi.fn(() => {
  sttRecording.value = false
  sttFinal.value = ''
  sttInterim.value = ''
  sttError.value = ''
})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
    back,
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
  useSpeechToText: () => ({
    isSupported: sttSupported,
    isRecording: sttRecording,
    finalTranscript: sttFinal,
    interimTranscript: sttInterim,
    error: sttError,
    language: sttLanguage,
    cancel: sttCancel,
    toggle: sttToggle,
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
  jobRole: '前端工程师',
  openingPending: false,
  chatLogs: [],
}

const createStreamResponse = () => ({
  ok: true,
  body: {
    getReader: () => ({
      read: vi.fn()
        .mockResolvedValueOnce({
          done: false,
          value: new TextEncoder().encode('data: {"type":"done"}\n\n'),
        })
        .mockResolvedValueOnce({
          done: true,
          value: undefined,
        }),
    }),
  },
})

const mountView = () => mount(InterviewSessionView, {
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

describe('InterviewSessionView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sttSupported.value = true
    sttRecording.value = false
    sttFinal.value = ''
    sttInterim.value = ''
    sttError.value = ''
    sttLanguage.value = 'zh-CN'

    getInterviewSession.mockResolvedValue({
      data: {
        ...baseSession,
        chatLogs: [],
      },
    })
    streamInterviewMessage.mockResolvedValue(createStreamResponse())
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
      'token'
    )
    expect(wrapper.vm.inputMessage).toBe('')
    expect(sttRecording.value).toBe(false)
    expect(sttFinal.value).toBe('')
    expect(sttInterim.value).toBe('')
  })
})
