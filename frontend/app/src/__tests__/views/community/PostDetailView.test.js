import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PostDetailView from '@/views/community/PostDetailView.vue'
import { adminBanUser, adminHidePost, getPostDetail } from '@/api/community'
import { ElMessageBox } from 'element-plus'

const routerMocks = vi.hoisted(() => ({
  back: vi.fn(),
  push: vi.fn(),
}))

const userStoreState = vi.hoisted(() => ({
  userInfo: { id: 'user-1', role: 0 },
}))

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => ({
    beforeEach: vi.fn(),
  })),
  createWebHistory: vi.fn(() => ({})),
  useRouter: () => routerMocks,
  useRoute: () => ({
    params: { postId: 'post-1' },
    query: {},
  }),
}))

vi.mock('@/api/community', () => ({
  getPostDetail: vi.fn(),
  togglePostLike: vi.fn(),
  togglePostFavorite: vi.fn(),
  adminHidePost: vi.fn(),
  adminBanUser: vi.fn(),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => userStoreState,
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      prompt: vi.fn(),
    },
  }
})

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
        AdminUserBanDialog: {
          props: ['modelValue', 'targetName', 'saving'],
          emits: ['update:modelValue', 'submit'],
          template: '<div class="admin-user-ban-dialog-stub" />',
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
  beforeEach(() => {
    vi.clearAllMocks()
    userStoreState.userInfo = { id: 'user-1', role: 0 }
  })

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

  it('collapses very long detail titles and opens the full title dialog on demand', async () => {
    const longTitle = Array.from({ length: 18 }, () => 'Long detail title').join(' ')
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-1',
        authorName: 'tester',
        authorAvatar: '',
        category: 'interview_exp',
        title: longTitle,
        content: '帖子内容',
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

    expect(wrapper.find('.post-title').classes()).toContain('collapsed-title')
    expect(wrapper.find('.title-dialog-btn').exists()).toBe(true)
    expect(wrapper.find('.full-title-text').exists()).toBe(false)

    await wrapper.find('.title-dialog-btn').trigger('click')
    await flushPromises()

    expect(wrapper.find('.full-title-text').text()).toBe(longTitle)
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

  it('does not render the admin hide action for normal users', async () => {
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-2',
        authorName: 'tester',
        category: 'interview_exp',
        title: '普通帖子',
        content: '帖子内容',
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

    expect(wrapper.find('.admin-hide-detail-btn').exists()).toBe(false)
  })

  it('lets administrators hide a post with a required reason and returns to community', async () => {
    userStoreState.userInfo = { id: 'admin-1', role: 9 }
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-2',
        authorName: 'tester',
        category: 'interview_exp',
        title: '违规帖子',
        content: '帖子内容',
        images: [],
        liked: false,
        favorited: false,
        likeCount: 0,
        commentCount: 0,
        createTime: '2026-05-28T10:00:00',
      },
    })
    ElMessageBox.prompt.mockResolvedValue({ value: '包含违规引流内容' })
    adminHidePost.mockResolvedValue({ code: 200 })

    const wrapper = mountView()
    await flushPromises()

    const button = wrapper.find('.admin-hide-detail-btn')
    expect(button.exists()).toBe(true)

    await button.trigger('click')
    await flushPromises()

    expect(ElMessageBox.prompt).toHaveBeenCalled()
    const promptOptions = ElMessageBox.prompt.mock.calls[0][2]
    expect(promptOptions.inputValidator(' ')).toBe('请输入下架原因')
    expect(promptOptions.inputValidator('A'.repeat(201))).toBe('下架原因不能超过200字')
    expect(promptOptions.inputPlaceholder).toContain('200字以内')
    expect(adminHidePost).toHaveBeenCalledWith('post-1', { reason: '包含违规引流内容' })
    expect(routerMocks.push).toHaveBeenCalledWith('/community')
  })

  it('lets administrators ban the post author with duration and reason', async () => {
    userStoreState.userInfo = { id: 'admin-1', role: 9 }
    getPostDetail.mockResolvedValue({
      code: 200,
      data: {
        id: 'post-1',
        userId: 'user-2',
        authorName: 'bad-user',
        category: 'interview_exp',
        title: '违规帖子',
        content: '帖子内容',
        images: [],
        liked: false,
        favorited: false,
        likeCount: 0,
        commentCount: 0,
        createTime: '2026-05-28T10:00:00',
      },
    })
    adminBanUser.mockResolvedValue({ code: 200 })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('.admin-ban-detail-btn').exists()).toBe(true)
    await wrapper.find('.admin-ban-detail-btn').trigger('click')
    await wrapper.vm.submitBanUser({ duration: '7d', reason: '多次违规' })
    await flushPromises()

    expect(adminBanUser).toHaveBeenCalledWith('user-2', { duration: '7d', reason: '多次违规' })
  })
})
