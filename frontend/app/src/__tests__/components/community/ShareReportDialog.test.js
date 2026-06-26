import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import ShareReportDialog from '@/components/community/ShareReportDialog.vue'
import { createPost } from '@/api/community'

vi.mock('@/api/community', () => ({
  createPost: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElButton: {
    props: ['disabled', 'loading'],
    emits: ['click'],
    template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
  },
  ElDialog: {
    name: 'ElDialog',
    props: ['modelValue', 'appendToBody', 'closeOnClickModal'],
    emits: ['update:modelValue', 'open'],
    template: `
      <div
        class="dialog-stub"
        :data-model-value="String(modelValue)"
        :data-append-to-body="String(appendToBody)"
        :data-close-on-click-modal="String(closeOnClickModal)"
      >
        <slot />
        <slot name="footer" />
      </div>
    `,
  },
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

const sessionData = {
  sessionId: 'session-1',
  jobRole: 'Frontend Engineer',
  difficultyDesc: '中级',
  comprehensiveScore: 86,
  evaluationReport: {
    summary: '整体表达清晰。',
    strengths: ['项目表达完整'],
    improvementSuggestions: ['补充量化结果'],
  },
}

const mountDialog = () =>
  mount(ShareReportDialog, {
    props: {
      visible: true,
      sessionData,
    },
    global: {
      stubs: {
        ElDialog: {
          name: 'ElDialog',
          props: ['modelValue', 'appendToBody', 'closeOnClickModal'],
          emits: ['update:modelValue', 'open'],
          template: `
            <div
              class="dialog-stub"
              :data-model-value="String(modelValue)"
              :data-append-to-body="String(appendToBody)"
              :data-close-on-click-modal="String(closeOnClickModal)"
            >
              <slot />
              <slot name="footer" />
            </div>
          `,
        },
        ElButton: {
          props: ['disabled'],
          template: '<button :disabled="disabled"><slot /></button>',
        },
      },
    },
  })

describe('ShareReportDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('keeps the share success toast owned by the dialog without emitting a generic success event', async () => {
    createPost.mockResolvedValue({ code: 200 })
    const wrapper = mountDialog()

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()
    await wrapper.vm.handleSubmit()

    expect(ElMessage.success).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    expect(wrapper.emitted('success')).toBeUndefined()
  })

  it('publishes an interview report as a titled link post instead of copying the full report text', async () => {
    createPost.mockResolvedValue({ code: 200 })
    const wrapper = mountDialog()

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()
    wrapper.vm.userText = '这次模拟面试暴露了表达结构问题。'
    await wrapper.vm.handleSubmit()

    expect(createPost).toHaveBeenCalledWith({
      category: 'interview_exp',
      title: 'Frontend Engineer 面试报告',
      content: '这次模拟面试暴露了表达结构问题。',
      images: [],
      sharedInterviewSessionId: 'session-1',
    })
  })

  it('renders an editable title input with the generated report title as default value', async () => {
    const wrapper = mountDialog()

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()

    const titleInput = wrapper.find('.title-input')
    expect(titleInput.exists()).toBe(true)
    expect(titleInput.element.value).toBe('Frontend Engineer 面试报告')

    await titleInput.setValue('我复盘的一次前端模拟面试')
    expect(wrapper.vm.reportTitle).toBe('我复盘的一次前端模拟面试')
  })

  it('does not submit when the report share title is empty', async () => {
    const wrapper = mountDialog()

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()
    wrapper.vm.reportTitle = '   '
    await nextTick()
    await wrapper.vm.handleSubmit()

    expect(createPost).not.toHaveBeenCalled()
  })

  it('submits the user edited report title with the shared interview session id', async () => {
    createPost.mockResolvedValue({ code: 200 })
    const wrapper = mountDialog()

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()
    wrapper.vm.reportTitle = '自定义报告标题'
    wrapper.vm.userText = '这是我的报告分享说明。'
    await wrapper.vm.handleSubmit()

    expect(createPost).toHaveBeenCalledWith({
      category: 'interview_exp',
      title: '自定义报告标题',
      content: '这是我的报告分享说明。',
      images: [],
      sharedInterviewSessionId: 'session-1',
    })
  })

  it('teleports the community share dialog to body so the report page mask cannot hide the dialog panel', async () => {
    const wrapper = mountDialog()

    const dialog = wrapper.find('.dialog-stub')
    expect(dialog.attributes('data-model-value')).toBe('true')
    expect(dialog.attributes('data-append-to-body')).toBe('true')
    expect(dialog.attributes('data-close-on-click-modal')).toBe('false')

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()

    expect(wrapper.text()).toContain('Frontend Engineer')
    expect(wrapper.text()).toContain('/interview/report/session-1')
    expect(wrapper.text()).not.toContain('整体表达清晰。')
  })
})
