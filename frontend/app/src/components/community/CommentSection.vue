<template>
  <div class="comment-section" ref="commentSectionRef">
    <!-- 评论区标题 -->
    <div class="section-header">
      <div class="header-left">
        <FeatureIcon name="comment" size="sm" class="header-icon" />
        <h3 class="header-title">评论</h3>
        <span v-if="total > 0" class="header-count">{{ total }}</span>
      </div>
      <div class="header-line"></div>
    </div>

    <!-- 评论输入框 -->
    <div class="composer">
      <div class="composer-avatar">
        <img :src="userAvatar" alt="avatar" />
      </div>
      <div class="composer-body">
        <textarea
          ref="textareaRef"
          v-model="commentText"
          class="composer-input"
          placeholder="说点什么吧..."
          rows="1"
          maxlength="500"
          @focus="composerFocused = true"
          @blur="onComposerBlur"
          @input="autoResize"
        />
        <!-- 评论图片预览 -->
        <div v-if="commentImages.length > 0" class="comment-images-preview">
          <div v-for="(img, index) in commentImages" :key="index" class="comment-img-thumb">
            <img :src="img" />
            <button class="remove-img-btn" @click="removeCommentImage(index)">
              <FeatureIcon name="close" size="xs" />
            </button>
          </div>
        </div>
        <Transition name="toolbar-slide">
          <div v-if="composerFocused || commentText.trim() || commentImages.length > 0" class="composer-toolbar">
            <div class="toolbar-left">
              <label class="btn-add-image" :class="{ disabled: imageUploading || commentImages.length >= 9 }">
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  hidden
                  @change="handleCommentImageUpload"
                />
                <FeatureIcon v-if="!imageUploading" name="image-upload" size="xs" />
                <span v-else class="btn-spinner sm"></span>
              </label>
              <span class="char-count" :class="{ warn: commentText.length > 450 }">
                {{ commentText.length }}<span class="char-sep">/</span>500
              </span>
            </div>
            <div class="toolbar-actions">
              <button class="btn-cancel" @click="cancelComment">取消</button>
              <button
                class="btn-submit"
                :disabled="(!commentText.trim() && commentImages.length === 0) || submitting"
                @click="handleSubmit"
              >
                <span v-if="submitting" class="btn-spinner"></span>
                <span v-else>发布</span>
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <!-- 懒加载占位：未进入视口时显示 -->
    <div v-if="!shouldLoad" class="lazy-placeholder" ref="observerTarget">
      <div class="lazy-hint">
        <FeatureIcon name="comment" size="sm" class="lazy-icon" />
        <span>滚动加载评论...</span>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="loading" class="loading-state">
      <div class="loading-dots">
        <span></span><span></span><span></span>
      </div>
      <span class="loading-text">加载评论中</span>
    </div>

    <!-- 评论列表 -->
    <TransitionGroup v-else-if="comments.length > 0" name="comment-list" tag="div" class="comment-list">
      <div v-for="(comment, index) in comments" :key="comment.id" :id="`comment-${comment.id}`" class="comment-card" :style="{ '--delay': index * 0.04 + 's' }">
        <div class="comment-avatar">
          <img :src="comment.authorAvatar || defaultAvatar" alt="avatar" />
        </div>
        <div class="comment-body">
          <div class="comment-meta">
            <span class="comment-author">{{ comment.authorName || '匿名用户' }}</span>
            <span v-if="comment.isPostAuthor" class="author-badge">作者</span>
            <span class="comment-dot">·</span>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            <button class="btn-reply" @mousedown.prevent @click="startReply(comment)">回复</button>
            <button
              v-if="comment.deletable"
              class="btn-delete-comment"
              @click="handleDeleteComment(comment)"
              title="删除评论"
            >
              <FeatureIcon name="delete" size="xs" />
            </button>
            <button
              v-if="isAdminUser"
              class="btn-admin-hide-comment"
              @click="handleAdminHideComment(comment)"
              title="下架评论"
            >
              <FeatureIcon name="delete" size="xs" />
            </button>
            <button
              v-if="canAdminBanUser(comment)"
              class="btn-admin-ban-user"
              @click="openBanDialog(comment)"
              title="封禁用户"
            >
              <FeatureIcon name="warning" size="xs" />
            </button>
          </div>
          <p class="comment-content clickable" @click.prevent="startReply(comment)">{{ comment.content }}</p>
          <ImageGrid v-if="comment.images && comment.images.length > 0" :images="comment.images" />

          <!-- 展开/收起回复 -->
          <button
            v-if="comment.replyCount > 0"
            class="btn-toggle-replies"
            @click="toggleReplies(comment)"
            >
            <FeatureIcon :name="expandedReplies.has(comment.id) ? 'collapse' : 'expand'" size="xs" />
            <span v-if="!expandedReplies.has(comment.id)">共{{ comment.replyCount }}条回复，点击查看</span>
            <span v-else>收起回复</span>
          </button>

          <!-- 回复列表 -->
          <Transition name="reply-expand">
            <div v-if="expandedReplies.has(comment.id) && repliesMap[comment.id]" class="reply-list">
              <!-- 显示的回复（默认3条或全部） -->
              <div
                v-for="reply in getVisibleReplies(comment)"
                v-memo="[reply.id, reply.content, reply.authorName, reply.authorAvatar, reply.deletable, reply.replyToUserName, reply.isPostAuthor, reply.images]"
                :key="reply.id"
                :id="`comment-${reply.id}`"
                class="reply-card"
              >
                <div class="reply-avatar">
                  <img :src="reply.authorAvatar || defaultAvatar" alt="avatar" />
                </div>
                <div class="reply-body">
                  <div class="reply-meta">
                    <span class="reply-author">{{ reply.authorName || '匿名用户' }}</span>
                    <span v-if="reply.isPostAuthor" class="author-badge sm">作者</span>
                    <span v-if="reply.replyToUserName" class="reply-to">
                      回复 <span class="reply-to-name">@{{ reply.replyToUserName }}</span>
                    </span>
                    <span class="reply-dot">·</span>
                    <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                    <button class="btn-reply btn-reply-nested" @mousedown.prevent @click="startReply(comment, reply)">回复</button>
                    <button
                      v-if="reply.deletable"
                      class="btn-delete-comment"
                      @click="handleDeleteReply(comment, reply)"
                      title="删除回复"
                    >
                      <FeatureIcon name="delete" size="xs" />
                    </button>
                    <button
                      v-if="isAdminUser"
                      class="btn-admin-hide-comment"
                      @click="handleAdminHideReply(comment, reply)"
                      title="下架回复"
                    >
                      <FeatureIcon name="delete" size="xs" />
                    </button>
                    <button
                      v-if="canAdminBanUser(reply)"
                      class="btn-admin-ban-user"
                      @click="openBanDialog(reply)"
                      title="封禁用户"
                    >
                      <FeatureIcon name="warning" size="xs" />
                    </button>
                  </div>
                  <p class="reply-content">{{ reply.content }}</p>
                  <ImageGrid v-if="reply.images && reply.images.length > 0" :images="reply.images" />
                </div>
              </div>
              <!-- 展开剩余回复 -->
              <button
                v-if="!showAllReplies.has(comment.id) && repliesMap[comment.id].length > 3"
                class="btn-expand-more"
                @click="showAllReplies.add(comment.id)"
              >
                展开剩余{{ repliesMap[comment.id].length - 3 }}条回复
              </button>
            </div>
          </Transition>

          <!-- 回复输入框（内联，出现在回复列表下方） -->
          <div v-if="replyTarget && replyTarget.id === comment.id" class="reply-composer">
            <div class="reply-composer-header">
              <span class="reply-to-hint">回复 @{{ replyTarget._replyToName || replyTarget.authorName }}</span>
              <button class="btn-cancel-reply" @click="cancelReply">
                <FeatureIcon name="close" size="xs" />
              </button>
            </div>
            <div class="reply-composer-body">
              <textarea
                :ref="setReplyInputRef"
                v-model="replyText"
                class="reply-input"
                :placeholder="`回复 @${replyTarget._replyToName || replyTarget.authorName}...`"
                rows="1"
                maxlength="500"
                @input="autoResizeReply"
                @keydown="handleReplyKeydown"
              />
              <!-- 回复图片预览 -->
              <div v-if="replyImages.length > 0" class="comment-images-preview">
                <div v-for="(img, index) in replyImages" :key="index" class="comment-img-thumb">
                  <img :src="img" />
                  <button class="remove-img-btn" @click="removeReplyImage(index)">
                    <FeatureIcon name="close" size="xs" />
                  </button>
                </div>
              </div>
              <div class="reply-composer-actions">
                <div class="toolbar-left">
                  <label class="btn-add-image" :class="{ disabled: replyImageUploading || replyImages.length >= 9 }">
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      hidden
                      @change="handleReplyImageUpload"
                    />
                    <FeatureIcon v-if="!replyImageUploading" name="image-upload" size="xs" />
                    <span v-else class="btn-spinner sm"></span>
                  </label>
                  <span class="char-count" :class="{ warn: replyText.length > 450 }">
                    {{ replyText.length }}<span class="char-sep">/</span>500
                  </span>
                </div>
                <button
                  class="btn-submit"
                  :disabled="(!replyText.trim() && replyImages.length === 0) || submittingReply"
                  @click="submitReply"
                >
                  <span v-if="submittingReply" class="btn-spinner"></span>
                  <span v-else>回复</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </TransitionGroup>

    <!-- 加载更多 -->
    <div v-if="hasMore && comments.length > 0" class="load-more">
      <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
        <span v-if="loadingMore" class="btn-spinner sm"></span>
        <span v-else>加载更多评论</span>
      </button>
    </div>

    <!-- 空评论状态 -->
    <div v-else-if="!loading && comments.length === 0" class="empty-state">
      <div class="empty-illustration">
        <FeatureIcon name="empty-state" size="xl" class="empty-svg" />
      </div>
      <p class="empty-title">还没有评论</p>
      <p class="empty-desc">快来发表第一条评论吧</p>
    </div>

    <AdminUserBanDialog
      v-model="banDialogVisible"
      :target-name="banTargetName"
      :saving="banSaving"
      @submit="submitBanUser"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminBanUser,
  adminHideComment,
  getComments,
  createComment,
  deleteComment,
  getReplies,
  uploadPostImage
} from '@/api/community'
import { useUserStore } from '@/stores/user'
import { optimizedImages } from '@/utils/optimizedImages'
import AdminUserBanDialog from '@/components/admin/AdminUserBanDialog.vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import ImageGrid from '@/components/community/ImageGrid.vue'
import { formatTime } from '@/utils/community'
import { normalizeAdminHideReason, validateAdminHideReason } from '@/utils/communityAdminHide'
import { useScrollToComment } from '@/composables/useScrollToComment'

const defaultAvatar = optimizedImages.userAvatar.webp

const props = defineProps({
  postId: {
    type: [String, Number],
    required: true
  },
  postUserId: {
    type: [String, Number],
    default: null
  },
  scrollToId: {
    type: [String, Number],
    default: null
  },
  scrollToParentId: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['commentDeleted', 'commentAdded'])

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)
const userAvatar = computed(() => userStore.userInfo?.avatar || defaultAvatar)
const isAdminUser = computed(() => userStore.userInfo?.role === 9)
const banDialogVisible = ref(false)
const banSaving = ref(false)
const banTarget = ref(null)
const banTargetName = computed(() => banTarget.value?.authorName || '目标用户')

const shouldLoad = ref(false)
const observerTarget = ref(null)
const comments = ref([])
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const commentText = ref('')
const commentImages = ref([])
const composerFocused = ref(false)
const imageUploading = ref(false)
const commentImageInput = ref(null)
const pageNum = ref(1)
const pageSize = 20
const commentSectionRef = ref(null)
const hasMore = ref(false)
const textareaRef = ref(null)

// 回复相关状态
const replyTarget = ref(null)
const replyText = ref('')
const replyImages = ref([])
const replyInputRef = ref(null)
const setReplyInputRef = (el) => {
  replyInputRef.value = el
  if (el && document.activeElement !== el) {
    el.focus()
  }
}
const submittingReply = ref(false)
const replyImageUploading = ref(false)
const repliesMap = reactive({})
const expandedReplies = reactive(new Set())
const showAllReplies = reactive(new Set())
const REPLY_PAGE_SIZE = 20
const DEFAULT_VISIBLE_REPLIES = 3

const getVisibleReplies = (comment) => {
  const replies = repliesMap[comment.id]
  if (!replies) return []
  if (showAllReplies.has(comment.id)) return replies
  return replies.slice(0, DEFAULT_VISIBLE_REPLIES)
}

const fetchComments = async (page = 1, silent = false) => {
  if (page === 1 && !silent) loading.value = true
  else if (page > 1) loadingMore.value = true

  try {
    const res = await getComments(props.postId, { pageNum: page, pageSize })
    if (res.code === 200) {
      const records = res.data?.list || []
      total.value = res.data?.total || 0
      if (page === 1) {
        comments.value = records
      } else {
        comments.value.push(...records)
      }
      hasMore.value = comments.value.length < total.value
      pageNum.value = page
    }
  } catch (err) {
    console.error('获取评论失败:', err)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  fetchComments(pageNum.value + 1)
}

const autoResize = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

const onComposerBlur = () => {
  if (!commentText.value.trim()) {
    composerFocused.value = false
  }
}

const cancelComment = () => {
  commentText.value = ''
  commentImages.value = []
  composerFocused.value = false
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.blur()
  }
}

const handleCommentImageUpload = async (e) => {
  const files = Array.from(e.target.files)
  if (!files.length) return

  const remaining = 9 - commentImages.value.length
  const toUpload = files.slice(0, remaining)

  imageUploading.value = true
  try {
    const validFiles = toUpload.filter(file => {
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.warning('单张图片不能超过2MB')
        return false
      }
      return true
    })

    if (validFiles.length > 0) {
      const results = await Promise.allSettled(
        validFiles.map(file => uploadPostImage(file))
      )
      results.forEach(result => {
        if (result.status === 'fulfilled' && result.value?.code === 200 && result.value.data?.url) {
          commentImages.value.push(result.value.data.url)
        }
      })
    }
  } catch (err) {
    ElMessage.error('图片上传失败')
  } finally {
    imageUploading.value = false
    e.target.value = ''
  }
}

const removeCommentImage = (index) => {
  commentImages.value.splice(index, 1)
}

const handleSubmit = async () => {
  const content = commentText.value.trim()
  if (!content && commentImages.value.length === 0) return

  submitting.value = true
  try {
    const data = { content }
    if (commentImages.value.length > 0) {
      data.images = [...commentImages.value]
    }
    const res = await createComment(props.postId, data)
    const reviewStatus = res?.data?.reviewStatus
    if (reviewStatus !== 'approved') {
      ElMessage.success('评论已提交审核，通过后将在评论区展示')
      commentText.value = ''
      commentImages.value = []
      composerFocused.value = false
      if (textareaRef.value) textareaRef.value.style.height = 'auto'
      return
    }
    ElMessage.success('评论发布成功')
    // 本地立即插入新评论
    const newComment = {
      id: res.data.id,
      postId: props.postId,
      userId: currentUserId.value,
      authorName: userStore.userInfo?.nickname || userStore.userInfo?.userName || '匿名用户',
      authorAvatar: userStore.userInfo?.avatar || '',
      content,
      images: commentImages.value.length > 0 ? [...commentImages.value] : null,
      createTime: new Date().toISOString(),
      isPostAuthor: currentUserId.value === props.postUserId,
      deletable: true,
      parentCommentId: null,
      replyCount: 0
    }
    comments.value.unshift(newComment)
    total.value++
    emit('commentAdded')
    commentText.value = ''
    commentImages.value = []
    composerFocused.value = false
    if (textareaRef.value) textareaRef.value.style.height = 'auto'
    // 后台静默刷新同步服务器数据
    fetchComments(1, true)
  } catch (err) {
    console.error('评论失败:', err)
  } finally {
    submitting.value = false
  }
}

const handleDeleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(props.postId, comment.id)
    ElMessage.success('评论已删除')
    emit('commentDeleted')
    fetchComments(1, true)
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除评论失败:', err)
    }
  }
}

// 聚焦由 setReplyInputRef callback ref 自动处理，无需 watch

const startReply = (comment, replyTo) => {
  // 切换行为：点击同一评论的回复按钮则关闭，点击其他评论则切换
  // comment 是顶层评论，replyTo 是被回复的子回复（如果有）
  const targetId = replyTo ? replyTo.id : comment.id
  if (replyTarget.value && replyTarget.value._targetId === targetId) {
    cancelReply()
    return
  }
  replyTarget.value = {
    ...comment,
    _targetId: targetId,
    _replyToName: replyTo ? replyTo.authorName : comment.authorName,
    _replyToUserId: replyTo ? replyTo.userId : comment.userId,
    _ancestorId: comment.id
  }
  replyText.value = ''
  replyImages.value = []
  nextTick(() => {
    replyInputRef.value?.focus()
  })
}

const cancelReply = () => {
  replyTarget.value = null
  replyText.value = ''
  replyImages.value = []
}

const handleReplyImageUpload = async (e) => {
  const files = Array.from(e.target.files)
  if (!files.length) return

  const remaining = 9 - replyImages.value.length
  const toUpload = files.slice(0, remaining)

  replyImageUploading.value = true
  try {
    const validFiles = toUpload.filter(file => {
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.warning('单张图片不能超过2MB')
        return false
      }
      return true
    })

    if (validFiles.length > 0) {
      const results = await Promise.allSettled(
        validFiles.map(file => uploadPostImage(file))
      )
      results.forEach(result => {
        if (result.status === 'fulfilled' && result.value?.code === 200 && result.value.data?.url) {
          replyImages.value.push(result.value.data.url)
        }
      })
    }
  } catch (err) {
    ElMessage.error('图片上传失败')
  } finally {
    replyImageUploading.value = false
    e.target.value = ''
  }
}

const removeReplyImage = (index) => {
  replyImages.value.splice(index, 1)
}

const submitReply = async () => {
  const content = replyText.value.trim()
  if ((!content && replyImages.value.length === 0) || !replyTarget.value) return

  submittingReply.value = true
  try {
    const parentId = replyTarget.value._ancestorId || replyTarget.value.id
    const replyToUserId = replyTarget.value._replyToUserId
    const data = { content, parentCommentId: parentId, replyToUserId }
    if (replyImages.value.length > 0) {
      data.images = [...replyImages.value]
    }
    const res = await createComment(props.postId, data)
    const reviewStatus = res?.data?.reviewStatus
    if (reviewStatus !== 'approved') {
      ElMessage.success('回复已提交审核，通过后将在评论区展示')
      replyText.value = ''
      replyImages.value = []
      replyTarget.value = null
      return
    }
    ElMessage.success('回复成功')
    // 本地更新父评论的回复数
    const parentComment = comments.value.find(c => c.id === parentId)
    if (parentComment) parentComment.replyCount = (parentComment.replyCount || 0) + 1
    total.value++
    emit('commentAdded')
    replyText.value = ''
    replyImages.value = []
    replyTarget.value = null
    // 自动展开并刷新该评论的回复
    expandedReplies.add(parentId)
    fetchReplies(parentId)
    // 后台静默刷新同步服务器数据
    fetchComments(1, true)
  } catch (err) {
    console.error('回复失败:', err)
  } finally {
    submittingReply.value = false
  }
}

const handleReplyKeydown = (e) => {
  // Alt+Enter 发送回复
  if (e.altKey && e.key === 'Enter') {
    e.preventDefault()
    submitReply()
  }
}

const toggleReplies = (comment) => {
  if (expandedReplies.has(comment.id)) {
    expandedReplies.delete(comment.id)
  } else {
    expandedReplies.add(comment.id)
    if (!repliesMap[comment.id]) {
      fetchReplies(comment.id)
    }
  }
}

const fetchReplies = async (commentId) => {
  try {
    const res = await getReplies(props.postId, commentId, { pageNum: 1, pageSize: REPLY_PAGE_SIZE })
    if (res.code === 200) {
      repliesMap[commentId] = res.data?.list || []
    }
  } catch (err) {
    console.error('获取回复失败:', err)
  }
}

const handleDeleteReply = async (parentComment, reply) => {
  try {
    await ElMessageBox.confirm('确定要删除这条回复吗？', '删除回复', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(props.postId, reply.id)
    ElMessage.success('回复已删除')
    emit('commentDeleted')
    fetchReplies(parentComment.id)
    fetchComments(1)
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除回复失败:', err)
    }
  }
}

const canAdminBanUser = (item) => {
  if (!isAdminUser.value || !item?.userId) return false
  return String(item.userId) !== String(currentUserId.value || '')
}

const promptAdminHideReason = async (title) => {
  const { value } = await ElMessageBox.prompt('请输入下架原因，系统会通知对应用户。', title, {
    confirmButtonText: '确认下架',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请输入200字以内的下架原因',
    inputValidator: validateAdminHideReason,
    type: 'warning'
  })
  return normalizeAdminHideReason(value)
}

const handleAdminHideComment = async (comment) => {
  try {
    const reason = await promptAdminHideReason('下架评论')
    await adminHideComment(props.postId, comment.id, { reason })
    const hiddenCount = 1 + Math.max(0, Number(comment.replyCount || 0))
    comments.value = comments.value.filter(item => item.id !== comment.id)
    delete repliesMap[comment.id]
    expandedReplies.delete(comment.id)
    showAllReplies.delete(comment.id)
    total.value = Math.max(0, total.value - 1)
    emit('commentDeleted', hiddenCount)
    ElMessage.success('评论已下架，并已通知用户')
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    console.error('管理员下架评论失败:', err)
    ElMessage.error('下架失败，请稍后重试')
  }
}

const handleAdminHideReply = async (parentComment, reply) => {
  try {
    const reason = await promptAdminHideReason('下架回复')
    await adminHideComment(props.postId, reply.id, { reason })
    repliesMap[parentComment.id] = (repliesMap[parentComment.id] || []).filter(item => item.id !== reply.id)
    parentComment.replyCount = Math.max(0, Number(parentComment.replyCount || 0) - 1)
    emit('commentDeleted', 1)
    ElMessage.success('回复已下架，并已通知用户')
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    console.error('管理员下架回复失败:', err)
    ElMessage.error('下架失败，请稍后重试')
  }
}

const openBanDialog = (item) => {
  if (!item?.userId) {
    ElMessage.error('无法识别要封禁的用户')
    return
  }
  banTarget.value = item
  banDialogVisible.value = true
}

const submitBanUser = async (payload) => {
  if (!banTarget.value?.userId) return
  banSaving.value = true
  try {
    await adminBanUser(banTarget.value.userId, payload)
    ElMessage.success('用户已封禁')
    banDialogVisible.value = false
  } catch (err) {
    console.error('管理员封禁评论用户失败:', err)
    ElMessage.error('封禁失败，请稍后重试')
  } finally {
    banSaving.value = false
  }
}

const autoResizeReply = () => {
  const el = replyInputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 100) + 'px'
}


// 点击空白区域收起回复框
const handleClickOutside = (e) => {
  if (!replyTarget.value) return
  // 检查点击的目标是否在评论区域内
  if (commentSectionRef.value && !commentSectionRef.value.contains(e.target)) {
    cancelReply()
  }
}

let lazyObserver = null

const { scrollToTarget } = useScrollToComment({
  commentSectionRef,
  comments,
  repliesMap,
  expandedReplies,
  showAllReplies,
  fetchReplies,
  scrollToParentId: props.scrollToParentId,
  postId: props.postId
})

const initComments = async () => {
  if (shouldLoad.value) return
  shouldLoad.value = true
  if (lazyObserver) { lazyObserver.disconnect(); lazyObserver = null }
  await nextTick()
  await fetchComments()
  document.addEventListener('mousedown', handleClickOutside)
  if (props.scrollToId) {
    // 重置滚动位置，确保 scrollIntoView 从干净状态开始
    const scrollContainer = document.querySelector('.layout-content')
    if (scrollContainer) scrollContainer.scrollTop = 0
    await nextTick()
    await scrollToTarget(props.scrollToId)
  }
}

onMounted(() => {
  // 有 scrollToId 时跳过懒加载，直接加载评论
  if (props.scrollToId) {
    initComments()
    return
  }
  // IntersectionObserver 懒加载：评论区进入视口时才请求评论
  lazyObserver = new IntersectionObserver((entries) => {
    if (entries.some(e => e.isIntersecting)) {
      lazyObserver.disconnect()
      lazyObserver = null
      initComments()
    }
  }, { rootMargin: '200px' })
  watch(() => observerTarget.value, (el) => {
    if (el && lazyObserver) lazyObserver.observe(el)
  }, { immediate: true })
  // 如果 observerTarget 已经存在（模板已渲染），直接观察
  if (observerTarget.value && lazyObserver) {
    lazyObserver.observe(observerTarget.value)
  }
})

watch(() => props.scrollToId, async (newId) => {
  if (newId) {
    await scrollToTarget(newId)
  }
})

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside)
  if (lazyObserver) { lazyObserver.disconnect(); lazyObserver = null }
})
</script>

<style scoped>
/* ===== 评论区样式 ===== */

.comment-section {
  /* 由父容器 comment-wrapper 统一控制间距 */
}

/* —— 标题区域 —— */
.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.header-icon {
  width: 20px;
  height: 20px;
  color: var(--orange-main);
}

.header-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-title);
  margin: 0;
  letter-spacing: 0.3px;
}

.header-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--orange-main);
  background: var(--orange-light-bg);
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 24px;
  text-align: center;
}

.header-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--orange-border), transparent);
}

/* —— 评论输入框 —— */
.composer {
  display: flex;
  gap: 14px;
  margin-bottom: 28px;
  align-items: flex-start;
}

.composer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 0 0 2px var(--bg-card), 0 0 0 4px var(--orange-border);
  transition: box-shadow 0.3s ease;
}

.composer-avatar:hover {
  box-shadow: 0 0 0 2px var(--bg-card), 0 0 0 4px var(--orange-main);
}

.composer-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.composer-body {
  flex: 1;
  min-width: 0;
}

.composer-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid var(--border-input);
  border-radius: 14px;
  font-size: 14px;
  color: var(--text-title);
  background: var(--bg-card);
  resize: none;
  min-height: 44px;
  max-height: 120px;
  font-family: inherit;
  line-height: 1.6;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.composer-input:focus {
  outline: none;
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.1);
}

.composer-input::placeholder {
  color: var(--text-placeholder);
  transition: color 0.3s;
}

.composer-input:focus::placeholder {
  color: var(--text-muted);
}

/* —— 输入框工具栏 —— */
.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding: 0 2px;
}

.char-count {
  font-size: 12px;
  color: var(--text-placeholder);
  transition: color 0.3s;
}

.char-count.warn {
  color: var(--color-warning);
}

.char-sep {
  margin: 0 1px;
  opacity: 0.5;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.btn-cancel {
  padding: 6px 16px;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.btn-cancel:hover {
  background: var(--bg-elevated);
  color: var(--text-body);
}

.btn-submit {
  padding: 6px 20px;
  border: none;
  background: linear-gradient(135deg, var(--orange-main), var(--orange-deep));
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.3);
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.4);
}

.btn-submit:active:not(:disabled) {
  transform: translateY(0);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.btn-spinner.sm {
  width: 12px;
  height: 12px;
  border-color: var(--border-divider);
  border-top-color: var(--orange-main);
}

/* —— 工具栏动画 —— */
.toolbar-slide-enter-active {
  animation: slideDown 0.25s ease;
}

.toolbar-slide-leave-active {
  animation: slideDown 0.2s ease reverse;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* —— 加载状态 —— */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 48px 0;
}

.loading-dots {
  display: flex;
  gap: 6px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--orange-border);
  animation: dotPulse 1.2s ease-in-out infinite;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes dotPulse {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
    background: var(--orange-main);
  }
}

.loading-text {
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 0.5px;
}

/* —— 评论列表 —— */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* —— 评论卡片 —— */
.comment-card {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 14px;
  transition: background 0.25s ease;
  animation: commentEnter 0.35s ease both;
  animation-delay: var(--delay, 0s);
}

.comment-card:hover {
  background: var(--bg-elevated);
}

@keyframes commentEnter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-elevated);
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-title);
}

.author-badge {
  font-size: 10px;
  font-weight: 700;
  color: var(--orange-main);
  background: var(--orange-light-bg);
  border: 1px solid var(--orange-border);
  padding: 1px 6px;
  border-radius: 6px;
  letter-spacing: 0.5px;
  line-height: 1.4;
}

.btn-delete-comment {
  margin-left: auto;
  padding: 4px;
  border: none;
  background: none;
  color: var(--text-placeholder);
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s ease, color 0.2s ease, opacity 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
}

.comment-card:hover .btn-delete-comment {
  opacity: 1;
}

.btn-delete-comment:hover {
  color: var(--color-danger);
  background: rgba(245, 108, 108, 0.08);
}

.btn-admin-hide-comment,
.btn-admin-ban-user {
  padding: 4px;
  border: none;
  background: none;
  color: var(--text-placeholder);
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s ease, color 0.2s ease, opacity 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
}

.comment-card:hover .btn-admin-hide-comment,
.comment-card:hover .btn-admin-ban-user,
.reply-card:hover .btn-admin-hide-comment,
.reply-card:hover .btn-admin-ban-user {
  opacity: 1;
}

.btn-admin-hide-comment:hover {
  color: var(--color-danger);
  background: rgba(245, 108, 108, 0.08);
}

.btn-admin-ban-user:hover {
  color: #b42318;
  background: rgba(180, 35, 24, 0.08);
}

.btn-delete-comment svg {
  width: 14px;
  height: 14px;
}

.comment-dot {
  color: var(--text-placeholder);
  font-size: 10px;
}

.comment-time {
  font-size: 12px;
  color: var(--text-placeholder);
}

.comment-content {
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.75;
  margin: 0;
  word-break: break-all;
  letter-spacing: 0.2px;
}

.comment-content.clickable {
  cursor: pointer;
  padding: 6px 8px;
  margin: -6px -8px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.comment-content.clickable:hover {
  background-color: var(--bg-elevated);
}

/* —— 评论列表动画 —— */
.comment-list-enter-active {
  animation: commentEnter 0.35s ease both;
}

.comment-list-leave-active {
  animation: commentEnter 0.2s ease reverse;
}

/* —— 加载更多 —— */
.load-more {
  text-align: center;
  padding: 16px 0 8px;
}

.load-more-btn {
  padding: 8px 24px;
  border: 1px solid var(--border-divider);
  background: var(--bg-card);
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: 20px;
  transition: background-color 0.25s ease, color 0.25s ease, border-color 0.25s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--orange-border);
  color: var(--orange-main);
  background: var(--orange-light-bg);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* —— 空状态 —— */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0 36px;
}

.empty-illustration {
  width: 72px;
  height: 72px;
  margin-bottom: 16px;
  color: var(--text-placeholder);
  animation: emptyFloat 3s ease-in-out infinite;
}

.empty-svg {
  width: 100%;
  height: 100%;
}

@keyframes emptyFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  margin: 0 0 4px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-placeholder);
  margin: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* —— 回复按钮 —— */
.btn-reply {
  padding: 2px 8px;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s ease, color 0.2s ease;
  flex-shrink: 0;
}

.btn-reply:hover {
  color: var(--orange-main);
  background: var(--orange-light-bg);
}

.btn-reply-nested {
  font-size: 11px;
  padding: 1px 6px;
}

/* —— 展开/收起回复 —— */
.btn-toggle-replies {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding: 4px 10px;
  border: none;
  background: none;
  color: var(--orange-main);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.btn-toggle-replies:hover {
  background: var(--orange-light-bg);
}

.btn-toggle-replies svg {
  transition: transform 0.25s;
}

.btn-toggle-replies svg.rotated {
  transform: rotate(180deg);
}

/* —— 回复列表（楼中楼） —— */
.reply-list {
  margin-top: 10px;
  margin-left: 4px;
  padding: 6px 0 6px 14px;
  border-left: 2.5px solid var(--orange-border);
  background: linear-gradient(90deg, var(--orange-light-bg) 0%, transparent 100%);
  border-radius: 0 0 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
}

.reply-card {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  transition: background 0.2s;
}

.reply-card:hover {
  background: rgba(255, 255, 255, 0.5);
}

.reply-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-elevated);
}

.reply-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 3px;
  flex-wrap: wrap;
}

.reply-author {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-title);
}

.author-badge.sm {
  font-size: 9px;
  padding: 0px 5px;
}

.reply-to {
  font-size: 12px;
  color: var(--text-muted);
}

.reply-to-name {
  color: var(--orange-main);
  font-weight: 500;
}

.reply-dot {
  color: var(--text-placeholder);
  font-size: 9px;
}

.reply-time {
  font-size: 11px;
  color: var(--text-placeholder);
}

.reply-content {
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.6;
  margin: 0;
  word-break: break-all;
}

/* —— 展开剩余回复 —— */
.btn-expand-more {
  display: block;
  width: 100%;
  padding: 8px 14px;
  border: none;
  background: none;
  color: var(--orange-main);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: background-color 0.2s ease;
}

.btn-expand-more:hover {
  background: rgba(255, 140, 66, 0.06);
}

/* —— 回复输入框 —— */
.reply-composer {
  margin-top: 10px;
  margin-left: 4px;
  padding: 12px;
  background: var(--bg-elevated);
  border-radius: 10px;
  border: 1px solid var(--border-divider);
}

.reply-composer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.reply-to-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.btn-cancel-reply {
  padding: 2px;
  border: none;
  background: none;
  color: var(--text-placeholder);
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s ease, color 0.2s ease;
  display: flex;
  align-items: center;
}

.btn-cancel-reply:hover {
  color: var(--text-body);
  background: var(--bg-card);
}

.reply-composer-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-input {
  width: 100%;
  padding: 8px 12px;
  border: 1.5px solid var(--border-input);
  border-radius: 10px;
  font-size: 13px;
  color: var(--text-title);
  background: var(--bg-card);
  resize: none;
  min-height: 36px;
  max-height: 100px;
  font-family: inherit;
  line-height: 1.5;
  cursor: text;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.reply-input:focus {
  outline: none;
  border-color: var(--orange-main);
  box-shadow: 0 0 0 2px rgba(255, 140, 66, 0.08);
}

.reply-input::placeholder {
  color: var(--text-placeholder);
}

.reply-composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* —— 回复展开动画 —— */
.reply-expand-enter-active {
  animation: expandIn 0.3s ease;
}

.reply-expand-leave-active {
  animation: expandIn 0.2s ease reverse;
}

@keyframes expandIn {
  from {
    opacity: 0;
    max-height: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    max-height: 800px;
    transform: translateY(0);
  }
}

/* —— 评论图片上传预览 —— */
.comment-images-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.comment-img-thumb {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-divider);
}

.comment-img-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.remove-img-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.remove-img-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.remove-img-btn svg {
  width: 11px;
  height: 11px;
}

/* —— 图片上传按钮 —— */
.btn-add-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-muted);
  transition: background-color 0.2s ease, color 0.2s ease;
  flex-shrink: 0;
}

.btn-add-image:hover {
  color: var(--orange-main);
  background: var(--orange-light-bg);
}

.btn-add-image.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

.btn-add-image svg {
  width: 18px;
  height: 18px;
}

/* —— 工具栏左侧区域 —— */
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* —— 响应式 —— */
@media (max-width: 480px) {
  .comment-card {
    padding: 14px 12px;
  }

  .composer {
    gap: 10px;
  }

  .composer-avatar {
    width: 34px;
    height: 34px;
  }
}

/* —— 懒加载占位 —— */
.lazy-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
}

.lazy-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-placeholder);
  font-size: 13px;
}

.lazy-icon {
  width: 18px;
  height: 18px;
  opacity: 0.5;
}

/* —— 评论高亮动画 —— */
.highlight-flash {
  animation: flashHighlight 2s ease;
}

@keyframes flashHighlight {
  0% { background-color: rgba(255, 140, 66, 0.2); }
  100% { background-color: transparent; }
}
</style>
