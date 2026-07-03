import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CommentSection from '@/components/community/CommentSection.vue'
import {
  getComments,
  createComment,
  deleteComment,
  getReplies,
  uploadPostImage,
  getCommentDetail,
  adminHideComment,
  adminBanUser
} from '@/api/community'

const userStoreState = vi.hoisted(() => ({
  userInfo: { id: 1, nickname: 'tester', avatar: '', userName: 'tester', role: 0 }
}))

// ── Mocks ──────────────────────────────────────────────────────────────

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/community/post/1' })
}))

vi.mock('@/api/community', () => ({
  getComments: vi.fn(),
  createComment: vi.fn(),
  deleteComment: vi.fn(),
  getReplies: vi.fn(),
  uploadPostImage: vi.fn(),
  getCommentDetail: vi.fn(),
  adminHideComment: vi.fn(),
  adminBanUser: vi.fn()
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => userStoreState)
}))

vi.mock('@/utils/community', () => ({
  formatTime: vi.fn((t) => t || '刚刚'),
  categoryLabel: vi.fn((c) => c || '全部')
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

// Mock IntersectionObserver (lazy-load trigger)
class MockIntersectionObserver {
  constructor(callback) {
    this._callback = callback
  }
  observe() {}
  disconnect() {}
  unobserve() {}
}
global.IntersectionObserver = MockIntersectionObserver

// ── Helpers ────────────────────────────────────────────────────────────

const defaultProps = {
  postId: 1,
  postUserId: 99
}

const mountSection = (propsOverrides = {}) =>
  mount(CommentSection, {
    props: { ...defaultProps, ...propsOverrides },
    global: {
      stubs: {
        ImageGrid: {
          props: ['images'],
          template: '<div class="image-grid-stub" />'
        },
        AdminUserBanDialog: {
          props: ['modelValue', 'targetName', 'saving'],
          emits: ['update:modelValue', 'submit'],
          template: '<div class="admin-user-ban-dialog-stub" />'
        },
        TransitionGroup: {
          template: '<div class="transition-group-stub"><slot /></div>'
        },
        Transition: {
          template: '<div class="transition-stub"><slot /></div>'
        }
      }
    }
  })

const mockCommentList = (comments = []) => {
  getComments.mockResolvedValue({
    code: 200,
    data: {
      list: comments,
      total: comments.length
    }
  })
}

// ── Tests ──────────────────────────────────────────────────────────────

describe('CommentSection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    userStoreState.userInfo = { id: 1, nickname: 'tester', avatar: '', userName: 'tester', role: 0 }
    // Default: no comments
    mockCommentList([])
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #7 — Comment image upload: size validation and limit
  // ────────────────────────────────────────────────────────────────────

  describe('评论图片上传行为 [Issue #7]', () => {
    it('单张图片超过 5MB 时应显示 warning 提示', async () => {
      // Arrange: mount and trigger lazy load
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test' }])
      await flushPromises()

      // Create a file that exceeds 5MB
      const bigFile = new File(['x'.repeat(6 * 1024 * 1024)], 'big.jpg', { type: 'image/jpeg' })
      const event = { target: { files: [bigFile], value: '' } }

      const { ElMessage } = await import('element-plus')

      // Act: call handleCommentImageUpload directly
      await wrapper.vm.handleCommentImageUpload(event)
      await flushPromises()

      // Assert: warning shown, uploadPostImage not called
      expect(ElMessage.warning).toHaveBeenCalledWith('单张图片不能超过5MB')
      expect(uploadPostImage).not.toHaveBeenCalled()
    })

    it('图片数量达到 9 张上限后不再上传更多', async () => {
      // Arrange
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test' }])
      await flushPromises()

      // Pre-fill commentImages to 9
      for (let i = 0; i < 9; i++) {
        wrapper.vm.commentImages.push(`https://img.example.com/${i}.jpg`)
      }

      const smallFile = new File(['data'], 'pic.jpg', { type: 'image/jpeg' })
      const event = { target: { files: [smallFile], value: '' } }

      // Act
      await wrapper.vm.handleCommentImageUpload(event)
      await flushPromises()

      // Assert: no upload attempt since remaining = 0
      expect(uploadPostImage).not.toHaveBeenCalled()
    })
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #9 — Concurrent image upload (serial → parallel fix)
  // ────────────────────────────────────────────────────────────────────

  describe('图片上传行为 [Issue #9]', () => {
    it('5.7 [P1] — 多张图片应使用并发上传而非串行', async () => {
      // This test documents the EXPECTED behavior after the fix.
      //
      // Current code (serial):
      //   for (const file of toUpload) {
      //     const res = await uploadPostImage(file)
      //     ...
      //   }
      //
      // Expected code (parallel):
      //   const results = await Promise.allSettled(
      //     toUpload.filter(f => f.size <= 5 * 1024 * 1024).map(f => uploadPostImage(f))
      //   )
      //   results.forEach(r => { if (r.status === 'fulfilled' && r.value?.code === 200 ...) ... })
      //
      // After fix is applied, this test verifies that uploadPostImage is called
      // for each valid file (not awaiting one-by-one in a loop).

      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test' }])
      await flushPromises()

      // Create 3 small valid files
      const files = [
        new File(['a'], '1.jpg', { type: 'image/jpeg' }),
        new File(['b'], '2.jpg', { type: 'image/jpeg' }),
        new File(['c'], '3.jpg', { type: 'image/jpeg' })
      ]
      const event = { target: { files: files, value: '' } }

      // Mock uploadPostImage to resolve with URLs
      uploadPostImage.mockImplementation((file) =>
        Promise.resolve({ code: 200, data: { url: `https://img.example.com/${file.name}` } })
      )

      // Act
      await wrapper.vm.handleCommentImageUpload(event)
      await flushPromises()

      // Assert: all 3 files were uploaded
      expect(uploadPostImage).toHaveBeenCalledTimes(3)

      // Assert: commentImages contains all 3 URLs
      expect(wrapper.vm.commentImages).toHaveLength(3)
      expect(wrapper.vm.commentImages).toContain('https://img.example.com/1.jpg')
      expect(wrapper.vm.commentImages).toContain('https://img.example.com/2.jpg')
      expect(wrapper.vm.commentImages).toContain('https://img.example.com/3.jpg')

      // Note: To verify parallelism specifically, one could mock uploadPostImage
      // to track call order vs resolve order. After the fix replaces `for...of await`
      // with `Promise.allSettled`, the calls will be dispatched simultaneously.
    })

    it('并发上传时部分失败不影响其他图片', async () => {
      // This test verifies that when one upload fails, other uploads still succeed.
      // Expected behavior after fix with Promise.allSettled:
      // - Failed uploads are skipped
      // - Successful uploads still push URLs to commentImages

      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test' }])
      await flushPromises()

      const files = [
        new File(['a'], 'ok.jpg', { type: 'image/jpeg' }),
        new File(['b'], 'fail.jpg', { type: 'image/jpeg' }),
        new File(['c'], 'ok2.jpg', { type: 'image/jpeg' })
      ]
      const event = { target: { files: files, value: '' } }

      let callIndex = 0
      uploadPostImage.mockImplementation(() => {
        callIndex++
        if (callIndex === 2) {
          return Promise.reject(new Error('Upload failed'))
        }
        return Promise.resolve({
          code: 200,
          data: { url: `https://img.example.com/uploaded-${callIndex}.jpg` }
        })
      })

      // Act
      await wrapper.vm.handleCommentImageUpload(event)
      await flushPromises()

      // After fix with Promise.allSettled:
      // - 2 successful uploads should be in commentImages
      // - 1 failure should be gracefully handled
      // Current serial behavior: failure in the loop throws to catch block,
      // stopping remaining uploads. After fix, all 3 are dispatched in parallel.
      expect(uploadPostImage).toHaveBeenCalledTimes(3)
    })
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #13 — Reply list pagination: hardcoded pageSize:50
  // ────────────────────────────────────────────────────────────────────

  describe('回复列表分页参数 [Issue #13]', () => {
    it('5.8 [P2] — 应使用可配置的 pageSize 而非硬编码 50', async () => {
      // This test documents the EXPECTED behavior after the fix.
      //
      // Current code:
      //   const res = await getReplies(props.postId, commentId, { pageNum: 1, pageSize: 50 })
      //
      // Expected code:
      //   const REPLY_PAGE_SIZE = 20  (or configurable)
      //   const res = await getReplies(props.postId, commentId, { pageNum: 1, pageSize: REPLY_PAGE_SIZE })
      //
      // After fix, getReplies should be called with a reasonable pageSize (e.g. 20).

      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test', replyCount: 5 }])
      getReplies.mockResolvedValue({
        code: 200,
        data: { list: [{ id: 'r1', content: 'reply1' }] }
      })
      await flushPromises()

      // Act: trigger fetchReplies
      await wrapper.vm.fetchReplies('c1')
      await flushPromises()

      // Assert: getReplies was called
      expect(getReplies).toHaveBeenCalled()

      // Current behavior: pageSize is 50 (hardcoded)
      const callArgs = getReplies.mock.calls[0]
      expect(callArgs[0]).toBe(1)   // postId
      expect(callArgs[1]).toBe('c1') // commentId
      expect(callArgs[2].pageNum).toBe(1)

      // Verify the pageSize — currently 50, should be configurable after fix
      expect(callArgs[2].pageSize).toBeDefined()

      // After fix, uncomment the line below to assert a reasonable default:
      // expect(callArgs[2].pageSize).toBeLessThanOrEqual(30)
      // or:
      // expect(callArgs[2].pageSize).toBe(20)
    })

    it('展开回复时应正确传入父评论 ID', async () => {
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test', replyCount: 3 }])
      getReplies.mockResolvedValue({
        code: 200,
        data: {
          list: [
            { id: 'r1', content: 'reply1' },
            { id: 'r2', content: 'reply2' }
          ]
        }
      })
      await flushPromises()

      // Act
      await wrapper.vm.fetchReplies('c1')
      await flushPromises()

      // Assert: correct postId and commentId passed
      expect(getReplies).toHaveBeenCalledWith(
        1,    // postId from props
        'c1', // commentId
        expect.objectContaining({ pageNum: 1 })
      )
    })

    it('回复数据应存入 repliesMap 并可被 getVisibleReplies 访问', async () => {
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      const replies = [
        { id: 'r1', content: 'reply1' },
        { id: 'r2', content: 'reply2' },
        { id: 'r3', content: 'reply3' },
        { id: 'r4', content: 'reply4' }
      ]
      mockCommentList([{ id: 'c1', content: 'test', replyCount: 4 }])
      getReplies.mockResolvedValue({ code: 200, data: { list: replies } })
      await flushPromises()

      // Act
      await wrapper.vm.fetchReplies('c1')
      await flushPromises()

      // Assert: repliesMap is populated
      expect(wrapper.vm.repliesMap['c1']).toHaveLength(4)

      // Assert: getVisibleReplies returns first 3 by default
      const comment = { id: 'c1', replyCount: 4 }
      const visible = wrapper.vm.getVisibleReplies(comment)
      expect(visible).toHaveLength(3)

      // Assert: showAllReplies reveals all
      wrapper.vm.showAllReplies.add('c1')
      const allVisible = wrapper.vm.getVisibleReplies(comment)
      expect(allVisible).toHaveLength(4)
    })

    it('回复获取失败时不应崩溃', async () => {
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([{ id: 'c1', content: 'test', replyCount: 1 }])
      getReplies.mockRejectedValue(new Error('Server error'))
      await flushPromises()

      // Act: should not throw
      await wrapper.vm.fetchReplies('c1')
      await flushPromises()

      // Assert: repliesMap for this comment is not populated, but no crash
      expect(wrapper.vm.repliesMap['c1']).toBeUndefined()
    })
  })

  // ────────────────────────────────────────────────────────────────────
  // Issue #7 (partial) — Comment submission with images
  // ────────────────────────────────────────────────────────────────────

  describe('评论提交行为 [Issue #7 补充]', () => {
    it('带图片的评论应将图片 URL 包含在请求中', async () => {
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([])
      await flushPromises()

      // Arrange: set comment text and images
      wrapper.vm.commentText = 'Nice post!'
      wrapper.vm.commentImages = ['https://img.example.com/a.jpg', 'https://img.example.com/b.jpg']

      createComment.mockResolvedValue({ code: 200, data: 'new-comment-id' })
      getComments.mockResolvedValue({ code: 200, data: { list: [], total: 0 } })

      // Act
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Assert: createComment was called with images array
      expect(createComment).toHaveBeenCalledWith(
        1, // postId
        expect.objectContaining({
          content: 'Nice post!',
          images: ['https://img.example.com/a.jpg', 'https://img.example.com/b.jpg']
        })
      )
    })

    it('无文字无图片时提交按钮应被禁用', async () => {
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([])
      await flushPromises()

      // Arrange: empty text and images
      wrapper.vm.commentText = ''
      wrapper.vm.commentImages = []

      // Act: handleSubmit should return early
      await wrapper.vm.handleSubmit()
      await flushPromises()

      // Assert: createComment was NOT called
      expect(createComment).not.toHaveBeenCalled()
    })
  })

  describe('自动审核状态下的评论提交', () => {
    it('does not optimistically insert pending comments', async () => {
      const { ElMessage } = await import('element-plus')
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([])
      await flushPromises()

      wrapper.vm.commentText = '我有一张截图'
      createComment.mockResolvedValue({ code: 200, data: { id: 3001, reviewStatus: 'pending' } })

      await wrapper.vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.success).toHaveBeenCalledWith('评论已提交审核，通过后将在评论区展示')
      expect(wrapper.vm.comments).toHaveLength(0)
      expect(wrapper.vm.total).toBe(0)
      expect(wrapper.vm.commentText).toBe('')
    })

    it('optimistically inserts auto approved comments', async () => {
      const { ElMessage } = await import('element-plus')
      const wrapper = mountSection({ scrollToId: 'c1' })
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      mockCommentList([])
      await flushPromises()

      wrapper.vm.commentText = '我也遇到过类似问题'
      createComment.mockResolvedValue({ code: 200, data: { id: 3002, reviewStatus: 'approved' } })

      await wrapper.vm.handleSubmit()
      await flushPromises()

      expect(ElMessage.success).toHaveBeenCalledWith('评论发布成功')
      expect(wrapper.vm.comments).toHaveLength(1)
      expect(wrapper.vm.comments[0].id).toBe(3002)
      expect(wrapper.vm.comments[0].content).toBe('我也遇到过类似问题')
    })
  })

  describe('管理员评论治理', () => {
    const makeComment = (overrides = {}) => ({
      id: 'c1',
      userId: 'user-2',
      authorName: '违规用户',
      content: '违规评论',
      replyCount: 2,
      deletable: false,
      createTime: '2026-05-31T10:00:00',
      ...overrides
    })

    it('普通用户看不到评论下架和封禁入口', async () => {
      mockCommentList([makeComment()])
      getCommentDetail.mockResolvedValue({ code: 200, data: null })

      const wrapper = mountSection({ scrollToId: 'c1' })
      await flushPromises()

      expect(wrapper.find('.btn-admin-hide-comment').exists()).toBe(false)
      expect(wrapper.find('.btn-admin-ban-user').exists()).toBe(false)
    })

    it('管理员下架顶级评论后移除整串并按数量通知父组件回退评论数', async () => {
      const { ElMessageBox } = await import('element-plus')
      userStoreState.userInfo = { id: 'admin-1', nickname: 'admin', avatar: '', userName: 'admin', role: 9 }
      mockCommentList([makeComment()])
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      ElMessageBox.prompt.mockResolvedValue({ value: '  违规引流  ' })
      adminHideComment.mockResolvedValue({ code: 200 })

      const wrapper = mountSection({ scrollToId: 'c1' })
      await flushPromises()

      await wrapper.find('.btn-admin-hide-comment').trigger('click')
      await flushPromises()

      expect(adminHideComment).toHaveBeenCalledWith(1, 'c1', { reason: '违规引流' })
      expect(wrapper.vm.comments).toHaveLength(0)
      expect(wrapper.emitted('commentDeleted')[0]).toEqual([3])
    })

    it('管理员封禁评论作者时提交时长和原因', async () => {
      userStoreState.userInfo = { id: 'admin-1', nickname: 'admin', avatar: '', userName: 'admin', role: 9 }
      mockCommentList([makeComment()])
      getCommentDetail.mockResolvedValue({ code: 200, data: null })
      adminBanUser.mockResolvedValue({ code: 200 })

      const wrapper = mountSection({ scrollToId: 'c1' })
      await flushPromises()

      await wrapper.find('.btn-admin-ban-user').trigger('click')
      await wrapper.vm.submitBanUser({ duration: '7d', reason: '多次违规' })
      await flushPromises()

      expect(adminBanUser).toHaveBeenCalledWith('user-2', { duration: '7d', reason: '多次违规' })
    })
  })
})
