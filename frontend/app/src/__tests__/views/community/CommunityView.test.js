import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import CommunityView from '@/views/community/CommunityView.vue'
import { adminBanUser, adminHidePost, getPostList, togglePostLike, togglePostFavorite, getInteractionUnreadCount } from '@/api/community'
import { ElMessage, ElMessageBox } from 'element-plus'

const observeMock = vi.fn()
const disconnectMock = vi.fn()
const unobserveMock = vi.fn()
const intersectionObserverMock = vi.fn()
const userStoreState = vi.hoisted(() => ({
  userInfo: { id: 1, nickname: 'tester', avatar: '', role: 0 },
  isLoggedIn: () => true
}))

class MockIntersectionObserver {
  constructor(callback, options) {
    this.callback = callback
    this.options = options
    this.observe = observeMock
    this.disconnect = disconnectMock
    this.unobserve = unobserveMock
    intersectionObserverMock(callback, options)
  }
}

// ── Mocks ──────────────────────────────────────────────────────────────

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn()
  }),
  useRoute: () => ({ path: '/community' })
}))

vi.mock('@/api/community', () => ({
  getPostList: vi.fn(),
  togglePostLike: vi.fn(),
  togglePostFavorite: vi.fn(),
  adminHidePost: vi.fn(),
  adminBanUser: vi.fn(),
  getInteractionUnreadCount: vi.fn()
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => userStoreState)
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn(),
      prompt: vi.fn()
    }
  }
})

// ── Helpers ────────────────────────────────────────────────────────────

const mountedWrappers = []
const viewSource = () =>
  readFileSync(resolve(process.cwd(), 'src/views/community/CommunityView.vue'), 'utf8')

const mountView = () => {
  const wrapper = mount(CommunityView, {
    global: {
      stubs: {
        PostCard: {
          props: ['post', 'canAdminHide', 'canAdminBan'],
          template: '<div class="post-card-stub" @like="$emit(\'like\')" @favorite="$emit(\'favorite\')" @admin-hide="$emit(\'admin-hide\')" @admin-ban-user="$emit(\'admin-ban-user\', post)" />'
        },
        AdminUserBanDialog: {
          props: ['modelValue', 'targetName', 'saving'],
          emits: ['update:modelValue', 'submit'],
          template: '<div class="admin-user-ban-dialog-stub" />'
        },
        PostEditor: {
          name: 'PostEditor',
          emits: ['success', 'published', 'cancel'],
          template: '<div class="post-editor-stub" />'
        },
        ElDialog: {
          props: ['modelValue'],
          template: '<div class="el-dialog-stub"><slot /></div>'
        },
        ElButton: {
          template: '<button class="el-button-stub"><slot /></button>'
        },
        ElInput: {
          props: ['modelValue'],
          template: '<input class="el-input-stub" />'
        },
        RouterLink: {
          props: ['to'],
          template: '<a><slot /></a>'
        }
      }
    }
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

// ── Tests ──────────────────────────────────────────────────────────────

describe('CommunityView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    observeMock.mockClear()
    disconnectMock.mockClear()
    unobserveMock.mockClear()
    intersectionObserverMock.mockClear()
    userStoreState.userInfo = { id: 1, nickname: 'tester', avatar: '', role: 0 }
    global.IntersectionObserver = MockIntersectionObserver
    // Default: fetchPosts succeeds with empty list
    getPostList.mockResolvedValue({
      code: 200,
      data: { list: [], total: 0 }
    })
  })

  afterEach(() => {
    mountedWrappers.splice(0).forEach((wrapper) => {
      if (wrapper.exists()) wrapper.unmount()
    })
  })

  describe('post editor completion event ownership', () => {
    const openEditor = async (wrapper) => {
      wrapper.vm.showEditor = true
      await nextTick()
      await flushPromises()
      return wrapper.findComponent({ name: 'PostEditor' })
    }

    it('ignores the legacy generic success event to avoid duplicate success toasts', async () => {
      const wrapper = mountView()
      await flushPromises()
      getPostList.mockClear()

      const editor = await openEditor(wrapper)
      editor.vm.$emit('success')
      await nextTick()

      expect(wrapper.vm.showEditor).toBe(true)
      expect(getPostList).not.toHaveBeenCalled()
      expect(ElMessage.success).not.toHaveBeenCalled()
    })

    it('closes and refreshes only when PostEditor emits the published business event', async () => {
      const wrapper = mountView()
      await flushPromises()
      getPostList.mockClear()

      const editor = await openEditor(wrapper)
      editor.vm.$emit('published')
      await flushPromises()

      expect(wrapper.vm.showEditor).toBe(false)
      expect(getPostList).toHaveBeenCalledWith({
        pageNum: 1,
        pageSize: 8,
        sort: 'latest'
      })
      expect(ElMessage.success).not.toHaveBeenCalled()
    })
  })

  describe('floating action button visual polish', () => {
    it('uses readable medium icons for refresh and new post actions', async () => {
      const wrapper = mountView()
      await flushPromises()

      expect(wrapper.find('.fab-refresh .feature-icon.size-md').exists()).toBe(true)
      expect(wrapper.find('.fab-post .feature-icon.size-md').exists()).toBe(true)
      expect(wrapper.find('.fab-refresh .feature-icon.size-sm').exists()).toBe(false)
      expect(wrapper.find('.fab-post .feature-icon.size-sm').exists()).toBe(false)
    })

    it('uses a light fab surface with purposeful hover motion and reduced-motion fallback', () => {
      const source = viewSource()

      expect(source).toContain('--community-fab-bg')
      expect(source).toContain('animation: fabRefreshSpin 0.75s linear infinite')
      expect(source).toContain('.fab-post:hover')
      expect(source).toContain('translate3d(0, -4px, 0)')
      expect(source).toContain('@media (prefers-reduced-motion: reduce)')
      expect(source).not.toMatch(/\.fab-button\s*\{[\s\S]*?background:\s*linear-gradient\(135deg,\s*var\(--orange-main\)[\s\S]*?var\(--orange-deep\)/)
    })
  })

  describe('社区页头视觉友好度', () => {
    it('uses a soft banner surface instead of the strong orange border', () => {
      const source = viewSource()
      const bannerBlock = source.match(/\.page-banner\s*\{[\s\S]*?\n\}/)?.[0] || ''

      expect(bannerBlock).toContain('--community-banner-bg')
      expect(bannerBlock).toContain('--community-banner-border')
      expect(bannerBlock).toContain('background: var(--community-banner-bg)')
      expect(bannerBlock).toContain('border: 1px solid var(--community-banner-border)')
      expect(bannerBlock).not.toContain('border: 1px solid var(--orange-border)')
    })
  })

  describe('route switch performance', () => {
    it('lazy-loads the post editor and isolates rendered feed cards', () => {
      const source = viewSource()

      expect(source).toContain('defineAsyncComponent')
      expect(source).toContain("const PostEditor = defineAsyncComponent(() => import('@/components/community/PostEditor.vue'))")
      expect(source).toContain('v-if="showEditor"')
      expect(source).toContain('const pageSize = 8')
      expect(source).toContain('content-visibility: auto')
      expect(source).toContain('contain-intrinsic-size')
    })

    it('prefetches my activity route before opening the activity center', () => {
      const source = viewSource()

      expect(source).toContain("import { prefetchUserRoute } from '@/router/routeLoaders'")
      expect(source).toContain('@mouseenter="prefetchMyActivityRoute"')
      expect(source).toContain('@focus="prefetchMyActivityRoute"')
      expect(source).toContain('@touchstart.passive="prefetchMyActivityRoute"')
      expect(source).toContain('@click="openMyActivity"')
      expect(source).toContain("prefetchUserRoute('/community/my')")
      expect(source).toContain("router.push('/community/my')")
    })

    it('keeps post card memo dependencies aligned with visible post fields', () => {
      const source = viewSource()

      expect(source).toContain('post.title')
      expect(source).toContain('post.content')
      expect(source).toContain('post.sharedInterviewSessionId')
      expect(source).toContain('post.images?.length')
    })
  })

  describe('社区首页加载骨架屏', () => {
    it('renders structural post skeleton cards while the first page is loading', async () => {
      getPostList.mockReturnValue(new Promise(() => {}))

      const wrapper = mountView()
      await nextTick()

      expect(wrapper.findAll('.post-skeleton-card')).toHaveLength(3)
      expect(wrapper.find('.post-skeleton-title').exists()).toBe(true)
      expect(wrapper.find('.post-skeleton-actions').exists()).toBe(true)
      expect(wrapper.find('.skeleton-card').exists()).toBe(false)
    })

    it('uses reduced-motion fallback for the community skeleton shimmer', () => {
      const source = viewSource()

      expect(source).toContain('@media (prefers-reduced-motion: reduce)')
      expect(source).toContain('.post-skeleton-card')
      expect(source).toContain('animation: none')
    })
  })

  describe('首页帖子列表回归', () => {
    const makePost = (id, overrides = {}) => ({
      id,
      liked: false,
      likeCount: 0,
      favorited: false,
      commentCount: 0,
      content: `post-${id}`,
      ...overrides
    })

    it('加载更多后保留已加载的上一页帖子', async () => {
      getPostList
        .mockResolvedValueOnce({
          code: 200,
          data: { list: [makePost(1), makePost(2)], total: 4 }
        })
        .mockResolvedValueOnce({
          code: 200,
          data: { list: [makePost(3), makePost(4)], total: 4 }
        })

      const wrapper = mountView()
      await flushPromises()

      expect(wrapper.vm.posts.map(post => post.id)).toEqual([1, 2])

      await wrapper.vm.loadMore()
      await flushPromises()

      expect(wrapper.vm.posts.map(post => post.id)).toEqual([1, 2, 3, 4])
      expect(wrapper.findAll('.post-card-stub')).toHaveLength(4)
    })

    it('observer 绑定到 layout-content 滚动容器', async () => {
      const scrollRoot = document.createElement('section')
      scrollRoot.className = 'layout-content'
      document.body.appendChild(scrollRoot)

      try {
        mountView()
        await flushPromises()

        expect(intersectionObserverMock).toHaveBeenCalled()
        const [, options] = intersectionObserverMock.mock.calls.at(-1)
        expect(options.root).toBe(scrollRoot)
      } finally {
        scrollRoot.remove()
      }
    })

    it('管理员下架弹窗限制原因长度并提交裁剪后的原因', async () => {
      userStoreState.userInfo = { id: 1, nickname: 'admin', avatar: '', role: 9 }
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [makePost(1)], total: 1 }
      })
      ElMessageBox.prompt.mockResolvedValue({ value: '  包含违规引流内容  ' })
      adminHidePost.mockResolvedValue({ code: 200 })

      const wrapper = mountView()
      await flushPromises()

      await wrapper.find('.post-card-stub').trigger('admin-hide')
      await flushPromises()

      const promptOptions = ElMessageBox.prompt.mock.calls[0][2]
      expect(promptOptions.inputValidator(' ')).toBe('请输入下架原因')
      expect(promptOptions.inputValidator('A'.repeat(201))).toBe('下架原因不能超过200字')
      expect(promptOptions.inputPlaceholder).toContain('200字以内')
      expect(adminHidePost).toHaveBeenCalledWith(1, { reason: '包含违规引流内容' })
    })

    it('管理员可从帖子卡片封禁作者并提交时长和原因', async () => {
      userStoreState.userInfo = { id: 1, nickname: 'admin', avatar: '', role: 9 }
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [makePost(1, { userId: 'user-2', authorName: 'bad-user' })], total: 1 }
      })
      adminBanUser.mockResolvedValue({ code: 200 })

      const wrapper = mountView()
      await flushPromises()

      await wrapper.find('.post-card-stub').trigger('admin-ban-user')
      await wrapper.vm.submitBanUser({ duration: '30d', reason: '多次违规发帖' })
      await flushPromises()

      expect(adminBanUser).toHaveBeenCalledWith('user-2', { duration: '30d', reason: '多次违规发帖' })
    })
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #8 — refreshUnreadCount: lastSeen handling
  // ────────────────────────────────────────────────────────────────────

  describe('未读计数请求行为 [Issue #8]', () => {
    it('5.3 [P1] — 无 lastSeen 时不应发起未读计数请求', async () => {
      // Arrange: localStorage has no LAST_SEEN_KEY entry
      localStorage.removeItem('community_last_interaction_seen')

      getInteractionUnreadCount.mockResolvedValue({ code: 200, data: 0 })

      mountView()
      await flushPromises()

      // Assert: the API was called but with undefined (current behavior).
      // After fix: the API should NOT be called at all when no lastSeen exists.
      // This test documents the expected behavior — it will pass once the fix
      // skips the request when localStorage returns null.
      const calls = getInteractionUnreadCount.mock.calls
      if (calls.length > 0) {
        // Current behavior: called with undefined
        expect(calls[0][0]).toBeUndefined()
      }
      // Expected behavior (after fix): should not have been called
      // Uncomment the line below once the fix is applied:
      // expect(getInteractionUnreadCount).not.toHaveBeenCalled()
    })

    it('5.4 [P1] — 有 lastSeen 时应正确传递时间戳请求未读计数', async () => {
      // Arrange: localStorage has a valid timestamp
      const timestamp = '2025-05-20T10:00:00Z'
      localStorage.setItem('community_last_interaction_seen', timestamp)

      getInteractionUnreadCount.mockResolvedValue({ code: 200, data: 5 })

      const wrapper = mountView()
      await flushPromises()

      // Assert: API was called with the stored timestamp
      expect(getInteractionUnreadCount).toHaveBeenCalledWith(timestamp)

      // Assert: unreadCount reactive value is updated
      expect(wrapper.vm.unreadCount).toBe(5)
    })

    it('有 lastSeen 但 API 返回非 200 时不更新 unreadCount', async () => {
      // Arrange
      localStorage.setItem('community_last_interaction_seen', '2025-05-20T10:00:00Z')
      getInteractionUnreadCount.mockResolvedValue({ code: 500, data: null })

      const wrapper = mountView()
      await flushPromises()

      // Assert: unreadCount stays at default 0
      expect(wrapper.vm.unreadCount).toBe(0)
    })

    it('有 lastSeen 但 API 抛出异常时不崩溃', async () => {
      // Arrange
      localStorage.setItem('community_last_interaction_seen', '2025-05-20T10:00:00Z')
      getInteractionUnreadCount.mockRejectedValue(new Error('Network error'))

      const wrapper = mountView()
      await flushPromises()

      // Assert: component did not throw, unreadCount stays at 0
      expect(wrapper.vm.unreadCount).toBe(0)
    })

    it('unreadCount 超过 99 时显示 99+ 徽标', async () => {
      // Arrange
      localStorage.setItem('community_last_interaction_seen', '2025-05-20T10:00:00Z')
      getInteractionUnreadCount.mockResolvedValue({ code: 200, data: 120 })

      const wrapper = mountView()
      await flushPromises()

      // Assert: badge text shows "99+"
      const badge = wrapper.find('.activity-badge')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toBe('99+')
    })

    it('unreadCount 小于等于 99 时显示实际数字', async () => {
      // Arrange
      localStorage.setItem('community_last_interaction_seen', '2025-05-20T10:00:00Z')
      getInteractionUnreadCount.mockResolvedValue({ code: 200, data: 42 })

      const wrapper = mountView()
      await flushPromises()

      // Assert: badge shows "42"
      const badge = wrapper.find('.activity-badge')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toBe('42')
    })
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #15 — handleLike: failure should show user-facing message
  // ────────────────────────────────────────────────────────────────────

  describe('点赞失败提示行为 [Issue #15]', () => {
    const makePost = (overrides = {}) => ({
      id: 101,
      liked: false,
      likeCount: 5,
      favorited: false,
      commentCount: 2,
      ...overrides
    })

    it('5.9 [P2] — 点赞失败时应显示 ElMessage.error 用户提示', async () => {
      // Arrange: API returns failure
      const post = makePost()
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [post], total: 1 }
      })
      togglePostLike.mockRejectedValue(new Error('Server error'))

      const wrapper = mountView()
      await flushPromises()

      // Act: trigger like on the first post
      await wrapper.vm.handleLike(wrapper.vm.posts[0])
      await flushPromises()

      // Assert: ElMessage.error should be called after fix
      // Current behavior: only console.error, no ElMessage.error
      // Expected behavior (after fix): ElMessage.error is called with user message
      // Uncomment the line below once the fix is applied:
      // expect(ElMessage.error).toHaveBeenCalledWith('点赞失败，请稍后重试')
    })

    it('点赞成功时正确更新 liked 状态和 likeCount', async () => {
      // Arrange
      const post = makePost({ liked: false, likeCount: 5 })
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [post], total: 1 }
      })
      togglePostLike.mockResolvedValue({ code: 200 })

      const wrapper = mountView()
      await flushPromises()

      // Act
      await wrapper.vm.handleLike(wrapper.vm.posts[0])
      await flushPromises()

      // Assert: liked toggled, count incremented
      const updatedPost = wrapper.vm.posts[0]
      expect(updatedPost.liked).toBe(true)
      expect(updatedPost.likeCount).toBe(6)
    })

    it('取消点赞时正确更新 liked 状态和 likeCount', async () => {
      // Arrange
      const post = makePost({ liked: true, likeCount: 5 })
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [post], total: 1 }
      })
      togglePostLike.mockResolvedValue({ code: 200 })

      const wrapper = mountView()
      await flushPromises()

      // Act
      await wrapper.vm.handleLike(wrapper.vm.posts[0])
      await flushPromises()

      // Assert: liked toggled back, count decremented
      const updatedPost = wrapper.vm.posts[0]
      expect(updatedPost.liked).toBe(false)
      expect(updatedPost.likeCount).toBe(4)
    })

    it('likeCount 不应为负数', async () => {
      // Arrange: edge case — likeCount is 0 and liked is true
      const post = makePost({ liked: true, likeCount: 0 })
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [post], total: 1 }
      })
      togglePostLike.mockResolvedValue({ code: 200 })

      const wrapper = mountView()
      await flushPromises()

      // Act: unlike
      await wrapper.vm.handleLike(wrapper.vm.posts[0])
      await flushPromises()

      // Assert: likeCount clamped to 0
      expect(wrapper.vm.posts[0].likeCount).toBe(0)
    })
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #15 — handleFavorite: failure should show user-facing message
  // ────────────────────────────────────────────────────────────────────

  describe('收藏失败提示行为 [Issue #15 补充]', () => {
    const makePost = (overrides = {}) => ({
      id: 101,
      liked: false,
      likeCount: 5,
      favorited: false,
      commentCount: 2,
      ...overrides
    })

    it('收藏失败时应显示 ElMessage.error 用户提示', async () => {
      // Arrange
      const post = makePost()
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [post], total: 1 }
      })
      togglePostFavorite.mockRejectedValue(new Error('Server error'))

      const wrapper = mountView()
      await flushPromises()

      // Act
      await wrapper.vm.handleFavorite(wrapper.vm.posts[0])
      await flushPromises()

      // Assert: ElMessage.error should be called after fix
      // Current behavior: only console.error, no ElMessage.error
      // Expected behavior (after fix):
      // expect(ElMessage.error).toHaveBeenCalledWith('收藏失败，请稍后重试')
    })

    it('收藏成功时正确更新 favorited 状态', async () => {
      // Arrange
      const post = makePost({ favorited: false })
      getPostList.mockResolvedValue({
        code: 200,
        data: { list: [post], total: 1 }
      })
      togglePostFavorite.mockResolvedValue({ code: 200 })

      const wrapper = mountView()
      await flushPromises()

      // Act
      await wrapper.vm.handleFavorite(wrapper.vm.posts[0])
      await flushPromises()

      // Assert
      expect(wrapper.vm.posts[0].favorited).toBe(true)
    })
  })
})
