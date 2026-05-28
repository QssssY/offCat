import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PostDetailView from '@/views/community/PostDetailView.vue'
import { getPostDetail } from '@/api/community'

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
  })),
  createWebHistory: vi.fn(() => ({})),
  useRouter: () => ({
    back: vi.fn(),
    push: vi.fn(),
  }),
  useRoute: () => ({
    params: { postId: 'post-1' },
    query: {},
  }),
}))

vi.mock('@/api/community', () => ({
  getPostDetail: vi.fn(),
  togglePostLike: vi.fn(),
  togglePostFavorite: vi.fn(),
}))

vi.mock('@/utils/optimizedImages', () => ({
  optimizedImages: {
    userAvatar: {
      webp: '/avatar.webp',
    },
  },
}))

const mountView = () =>
  mount(PostDetailView, {
    global: {
      stubs: {
        FeatureIcon: {
          props: ['name'],
          template: '<span class="feature-icon-stub">{{ name }}</span>',
        },
        ImageGrid: {
          template: '<div class="image-grid-stub" />',
        },
        CommentSection: {
          template: '<div class="comment-section-stub" />',
        },
        ElButton: {
          template: '<button><slot /></button>',
        },
        ElDialog: {
          template: '<div><slot /><slot name="footer" /></div>',
        },
        ElInput: {
          props: ['modelValue'],
          template: '<input :value="modelValue" />',
        },
      },
    },
  })

describe('PostDetailView', () => {
  it('renders the post title and shared interview report link in the detail body', async () => {
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-1',
        authorName: 'tester',
        authorAvatar: '',
        category: 'interview_exp',
        title: 'Frontend Engineer 面试报告',
        content: '这次模拟面试暴露了表达结构问题。',
        sharedInterviewSessionId: 'session-1',
        images: [],
        liked: false,
        favorited: false,
        likeCount: 3,
        commentCount: 2,
        createTime: '2026-05-28T10:00:00',
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.post-title').text()).toBe('Frontend Engineer 面试报告')
    const link = wrapper.find('.report-link-card')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('/interview/report/session-1')
    expect(link.text()).toContain('查看完整面试报告')
    expect(link.text()).toContain('Frontend Engineer 面试报告')
  })

  it('collapses long detail content by default and expands it on demand', async () => {
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-1',
        authorName: 'tester',
        authorAvatar: '',
        category: 'interview_exp',
        title: '一段很长的面试复盘',
        content: '面试过程里有很多细节需要记录。'.repeat(70),
        sharedInterviewSessionId: '',
        images: [],
        liked: false,
        favorited: false,
        likeCount: 3,
        commentCount: 2,
        createTime: '2026-05-28T10:00:00',
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.post-content').classes()).toContain('collapsed')
    expect(wrapper.find('.content-toggle').text()).toBe('展开全文')

    await wrapper.find('.content-toggle').trigger('click')

    expect(wrapper.find('.post-content').classes()).not.toContain('collapsed')
    expect(wrapper.find('.content-toggle').text()).toBe('收起')
  })

  it('uses a fallback title for legacy shared report detail records without a title', async () => {
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-1',
        authorName: 'tester',
        authorAvatar: '',
        category: 'interview_exp',
        title: '',
        content: '我分享了一份面试报告。',
        sharedInterviewSessionId: 'legacy-session-1',
        images: [],
        liked: false,
        favorited: false,
        likeCount: 0,
        commentCount: 0,
        createTime: '2026-05-28T10:00:00',
      },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.post-title').text()).toBe('面试报告分享')
    expect(wrapper.find('.report-link-title').text()).toBe('面试报告分享')
  })
})
