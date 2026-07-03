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
  ElButton: {
    props: ['disabled', 'loading'],
    emits: ['click'],
    template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
  },
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

const submitPost = async (wrapper) => {
  wrapper.vm.form.title = '一次有收获的前端面试复盘'
  wrapper.vm.form.content = 'Share a useful interview note.'
  await wrapper.vm.handleSubmit()
}

describe('PostEditor', () => {
  it('documents the create response review status on createPost', () => {
    const source = communityApiSource()

    expect(source).toMatch(/@param \{string\} data\.title/)
    expect(source).toMatch(/@param \{string\} \[data\.sharedInterviewSessionId\]/)
    expect(source).toMatch(/reviewStatus: 'approved'\|'pending'/)
  })

  it('shows public success when backend auto approves the post', async () => {
    createPost.mockResolvedValue({ code: 200, data: { id: 1001, reviewStatus: 'approved' } })
    const wrapper = mountEditor()

    await submitPost(wrapper)

    expect(ElMessage.success).toHaveBeenCalledWith('发布成功，已公开展示')
    expect(wrapper.emitted('published')).toHaveLength(1)
  })

  it('shows review success when backend keeps the post pending', async () => {
    createPost.mockResolvedValue({ code: 200, data: { id: 1002, reviewStatus: 'pending' } })
    const wrapper = mountEditor()

    await submitPost(wrapper)

    expect(ElMessage.success).toHaveBeenCalledWith('已提交审核，通过后将在社区展示')
    expect(wrapper.emitted('published')).toHaveLength(1)
  })
})
