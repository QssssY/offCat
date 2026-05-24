import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import PostEditor from '@/components/community/PostEditor.vue'
import { createPost } from '@/api/community'

vi.mock('@/api/community', () => ({
  createPost: vi.fn(),
  uploadPostImage: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

const mountEditor = () =>
  mount(PostEditor, {
    global: {
      stubs: {
        FeatureIcon: {
          props: ['name'],
          template: '<span class="feature-icon-stub">{{ name }}</span>',
        },
        ElButton: {
          props: ['disabled', 'loading'],
          emits: ['click'],
          template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
        },
      },
    },
  })

describe('PostEditor', () => {
  it('emits a published business event after the editor-owned success toast', async () => {
    createPost.mockResolvedValue({ code: 200 })
    const wrapper = mountEditor()

    wrapper.vm.form.content = 'Share a useful interview note.'
    await wrapper.vm.handleSubmit()

    expect(createPost).toHaveBeenCalledWith({
      category: 'interview_exp',
      content: 'Share a useful interview note.',
      images: [],
    })
    expect(ElMessage.success).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('published')).toHaveLength(1)
    expect(wrapper.emitted('success')).toBeUndefined()
  })
})
