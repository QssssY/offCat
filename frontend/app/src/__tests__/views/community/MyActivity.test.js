import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyActivity from '@/views/community/MyActivity.vue'
import { getMyPosts, getMyComments, getMyInteractions, getInteractionUnreadCount } from '@/api/community'

const push = vi.fn()
const back = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push, back })
}))

vi.mock('@/api/community', () => ({
  getMyPosts: vi.fn(),
  getLikedPosts: vi.fn(),
  getFavoritedPosts: vi.fn(),
  getMyComments: vi.fn(),
  deletePost: vi.fn(),
  getMyInteractions: vi.fn(),
  getInteractionUnreadCount: vi.fn()
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

const DynamicScrollerStub = defineComponent({
  name: 'DynamicScroller',
  props: {
    items: {
      type: Array,
      default: () => []
    },
    pageMode: {
      type: Boolean,
      default: false
    }
  },
  template: '<div class="dynamic-scroller-stub" :data-page-mode="String(pageMode)"><slot v-for="item in items" :item="item" :active="true" /></div>'
})

const DynamicScrollerItemStub = defineComponent({
  name: 'DynamicScrollerItem',
  template: '<div class="dynamic-scroller-item-stub"><slot /></div>'
})

const mountView = () => mount(MyActivity, {
  global: {
    stubs: {
      DynamicScroller: DynamicScrollerStub,
      DynamicScrollerItem: DynamicScrollerItemStub,
      ElDialog: { template: '<div><slot /><slot name="footer" /></div>' },
      ElButton: { template: '<button><slot /></button>' }
    }
  }
})

describe('MyActivity', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    getMyPosts.mockResolvedValue({ code: 200, data: { list: [], total: 0 } })
    getMyComments.mockResolvedValue({ code: 200, data: { list: [], total: 0 } })
    getMyInteractions.mockResolvedValue({
      code: 200,
      data: {
        likes: [],
        totalLikes: 0,
        comments: [],
        totalComments: 0,
        replies: [],
        totalReplies: 0,
        favorites: [],
        totalFavorites: 0
      }
    })
    getInteractionUnreadCount.mockResolvedValue({ code: 200, data: 0 })
  })

  it('should show review status and reason on my pending or rejected posts', async () => {
    getMyPosts.mockResolvedValue({
      code: 200,
      data: {
        total: 2,
        list: [
          {
            id: 'p1',
            category: 'interview_exp',
            title: '待审核帖子',
            content: '这是一条待审核内容',
            reviewStatus: 'pending',
            createTime: '2026-05-31T10:00:00',
            likeCount: 0,
            commentCount: 0,
            images: []
          },
          {
            id: 'p2',
            category: 'referral',
            title: '未通过帖子',
            content: '这是一条未通过内容',
            reviewStatus: 'rejected',
            reviewReason: '包含不合适内容',
            createTime: '2026-05-31T11:00:00',
            likeCount: 0,
            commentCount: 0,
            images: []
          }
        ]
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('待审核')
    expect(wrapper.text()).toContain('未通过')
    expect(wrapper.text()).toContain('原因：包含不合适内容')
  })

  it('should render commented tab through virtual scroller without throwing', async () => {
    getMyComments.mockResolvedValue({
      code: 200,
      data: {
        total: 1,
        list: [{
          commentId: 'c1',
          commentContent: '这是一条评论',
          commentTime: '2026-05-23T10:00:00',
          postId: 'p1',
          postCategory: 'interview_exp',
          postContent: '帖子摘要',
          postAuthorName: '作者',
          postDeleted: false
        }]
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('.tab-btn')[3].trigger('click')
    await flushPromises()

    expect(getMyComments).toHaveBeenCalledWith({ pageNum: 1, pageSize: 5 })
    expect(wrapper.find('.dynamic-scroller-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('这是一条评论')
  })

  it('should not enable page mode so the scroller follows the layout scroll container', async () => {
    getMyComments.mockResolvedValue({
      code: 200,
      data: {
        total: 1,
        list: [{
          commentId: 'c1',
          commentContent: '这是一条评论',
          commentTime: '2026-05-23T10:00:00',
          postId: 'p1',
          postCategory: 'interview_exp',
          postContent: '帖子摘要',
          postAuthorName: '作者',
          postDeleted: false
        }]
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('.tab-btn')[3].trigger('click')
    await flushPromises()

    const scroller = wrapper.findComponent(DynamicScrollerStub)
    expect(scroller.exists()).toBe(true)
    expect(scroller.props('pageMode')).toBe(false)
    expect(scroller.attributes('data-page-mode')).toBe('false')
  })

  it('should render received likes with generated virtual keys', async () => {
    getMyInteractions.mockResolvedValue({
      code: 200,
      data: {
        likes: [{
          userId: 'u1',
          userName: '点赞用户',
          postId: 'p1',
          postContent: '被点赞帖子',
          postCategory: 'referral',
          createTime: '2026-05-23T10:00:00'
        }],
        totalLikes: 1,
        comments: [],
        totalComments: 0,
        replies: [],
        totalReplies: 0,
        favorites: [],
        totalFavorites: 0
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('.tab-btn')[4].trigger('click')
    await flushPromises()

    expect(getMyInteractions).toHaveBeenCalledWith({ pageNum: 1, pageSize: 5 })
    expect(wrapper.vm.receivedLikesState.items.value[0].virtualKey).toMatch(/^like-/)
    expect(wrapper.text()).toContain('点赞用户')
  })
})
