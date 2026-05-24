import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import ShareReportDialog from '@/components/community/ShareReportDialog.vue'
import { createPost } from '@/api/community'

vi.mock('@/api/community', () => ({
  createPost: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

const sessionData = {
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
          template: '<button><slot /></button>',
        },
      },
    },
  })

describe('ShareReportDialog', () => {
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

  it('teleports the community share dialog to body so the report page mask cannot hide the dialog panel', async () => {
    const wrapper = mountDialog()

    const dialog = wrapper.find('.dialog-stub')
    expect(dialog.attributes('data-model-value')).toBe('true')
    expect(dialog.attributes('data-append-to-body')).toBe('true')
    expect(dialog.attributes('data-close-on-click-modal')).toBe('false')

    wrapper.findComponent({ name: 'ElDialog' }).vm.$emit('open')
    await nextTick()

    expect(wrapper.text()).toContain('Frontend Engineer')
    expect(wrapper.text()).toContain('整体表达清晰。')
  })
})
