import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
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

const communityApiSource = () =>
  readFileSync(resolve(process.cwd(), 'src/api/community.js'), 'utf8')

describe('PostEditor', () => {
  it('documents the required title and optional shared report session fields on createPost', () => {
    const source = communityApiSource()

    expect(source).toMatch(/@param \{string\} data\.title/)
    expect(source).toMatch(/@param \{string\} \[data\.sharedInterviewSessionId\]/)
  })

  it('emits a published business event after the editor-owned success toast', async () => {
    createPost.mockResolvedValue({ code: 200 })
    const wrapper = mountEditor()

    wrapper.vm.form.title = '一次有收获的前端面试复盘'
    wrapper.vm.form.content = 'Share a useful interview note.'
    await wrapper.vm.handleSubmit()

    expect(createPost).toHaveBeenCalledWith({
      category: 'interview_exp',
      title: '一次有收获的前端面试复盘',
      content: 'Share a useful interview note.',
      images: [],
    })
    expect(ElMessage.success).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('published')).toHaveLength(1)
    expect(wrapper.emitted('success')).toBeUndefined()
  })
})
