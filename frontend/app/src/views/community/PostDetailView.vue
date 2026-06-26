<template>
  <div class="post-detail-page">
    <!-- 返回按钮 -->
    <div class="top-bar">
      <button class="back-btn" @click="goBack">
        <FeatureIcon name="back" size="xs" />
        <span>返回</span>
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <FeatureIcon name="loading" size="sm" class="loading-feature-icon" />
      <span>加载中...</span>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="error-state">
      <div class="error-icon-wrapper">
        <FeatureIcon name="error" size="lg" />
      </div>
      <h3 class="error-title">加载失败</h3>
      <p class="error-desc">帖子可能已被删除或不存在</p>
      <div class="error-actions">
        <el-button type="primary" @click="loadPost">重新加载</el-button>
        <el-button @click="goBack">返回社区</el-button>
      </div>
    </div>

    <!-- 主内容 -->
    <template v-else-if="post">
      <!-- 上半部分：帖子内容 -->
      <section class="post-area">
        <div class="post-inner">
          <div class="post-top-row">
            <div class="author-pill">
              <div class="author-avatar">
                <img :src="post.authorAvatar || defaultAvatar" alt="avatar" />
              </div>
              <div class="author-text">
                <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
                <span class="post-time">{{ formatTime(post.createTime) }}</span>
              </div>
            </div>
            <span class="category-tag" :class="post.category">
              {{ categoryLabel(post.category) }}
            </span>
          </div>

          <div class="post-body">
            <h1
              v-if="displayTitle"
              class="post-title"
              :class="{ 'collapsed-title': shouldCollapseTitle }"
              :title="displayTitle"
            >
              {{ displayTitle }}
            </h1>
            <button
              v-if="shouldCollapseTitle"
              type="button"
              class="title-dialog-btn"
              @click="showFullTitleDialog = true"
            >
              查看完整标题
            </button>
            <p class="post-content" :class="{ collapsed: shouldCollapseContent && !contentExpanded }">{{ post.content }}</p>
            <button
              v-if="shouldCollapseContent"
              type="button"
              class="content-toggle"
              @click="contentExpanded = !contentExpanded"
            >
              {{ contentExpanded ? '收起' : '展开全文' }}
            </button>
            <!-- 分享报告帖只在社区保存会话ID，详情页据此渲染站内报告跳转链接。 -->
            <a
              v-if="post.sharedInterviewSessionId"
              class="report-link-card"
              :href="`/interview/report/${post.sharedInterviewSessionId}`"
            >
              <FeatureIcon name="interview-report" size="sm" />
              <span class="report-link-main">查看完整面试报告</span>
              <span class="report-link-title">{{ displayTitle }}</span>
            </a>
            <ImageGrid
              v-if="post.images && post.images.length > 0"
              :images="post.images"
              class="post-images"
            />
          </div>

          <div class="post-actions">
            <button class="act-btn" :class="{ active: post.liked }" @click="handleLike">
              <FeatureIcon :name="post.liked ? 'liked' : 'favorite'" size="xs" />
              <span>{{ post.likeCount || 0 }}</span>
            </button>
            <button class="act-btn" @click="scrollToComments">
              <FeatureIcon name="comment" size="xs" />
              <span>{{ post.commentCount || 0 }}</span>
            </button>
            <button class="act-btn" :class="{ active: post.favorited, 'active-fav': post.favorited }" @click="handleFavorite">
              <FeatureIcon name="favorite" size="xs" />
              <span>收藏</span>
            </button>
            <button class="act-btn" @click="handleShare">
              <FeatureIcon name="share" size="xs" />
              <span>分享</span>
            </button>
            <button
              v-if="isAdminUser"
              class="act-btn admin-hide-detail-btn"
              @click="handleAdminHide"
            >
              <FeatureIcon name="delete" size="xs" />
              <span>下架</span>
            </button>
            <button
              v-if="canAdminBanAuthor"
              class="act-btn admin-ban-detail-btn"
              @click="openBanDialog"
            >
              <FeatureIcon name="warning" size="xs" />
              <span>封禁作者</span>
            </button>
          </div>
        </div>
      </section>

      <!-- 下半部分：评论区 -->
      <section ref="commentSectionRef" class="comment-area">
        <div class="comment-inner">
          <CommentSection :key="postId" :post-id="postId" :post-user-id="post?.userId" :scroll-to-id="scrollToId" :scroll-to-parent-id="scrollToParentId" @comment-deleted="handleCommentDeleted" @comment-added="post.commentCount = (post.commentCount || 0) + 1" />
        </div>
      </section>
    </template>

    <!-- 分享弹窗 -->
    <el-dialog
      v-model="showShareDialog"
      title="分享帖子"
      width="400px"
      :append-to-body="true"
    >
      <div class="share-content">
        <p class="share-label">复制链接分享给好友：</p>
        <div class="share-link-row">
          <el-input v-model="shareLink" readonly size="large" />
          <el-button type="primary" size="large" @click="copyLink">复制链接</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-if="showFullTitleDialog"
      v-model="showFullTitleDialog"
      title="完整标题"
      width="520px"
      :append-to-body="true"
      class="full-title-dialog"
    >
      <p class="full-title-text">{{ displayTitle }}</p>
    </el-dialog>

    <AdminUserBanDialog
      v-model="banDialogVisible"
      :target-name="post?.authorName || '帖子作者'"
      :saving="banSaving"
      @submit="submitBanUser"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminBanUser, adminHidePost, getPostDetail, togglePostLike, togglePostFavorite } from '@/api/community'
import { useUserStore } from '@/stores/user'
import AdminUserBanDialog from '@/components/admin/AdminUserBanDialog.vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import ImageGrid from '@/components/community/ImageGrid.vue'
import CommentSection from '@/components/community/CommentSection.vue'
import { optimizedImages } from '@/utils/optimizedImages'
import { formatTime, categoryLabel } from '@/utils/community'
import {
  DETAIL_TITLE_COLLAPSE_LENGTH,
  normalizeAdminHideReason,
  validateAdminHideReason
} from '@/utils/communityAdminHide'

const defaultAvatar = optimizedImages.userAvatar.webp

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const postId = computed(() => route.params.postId)
const scrollToId = computed(() => route.query.commentId || null)
const scrollToParentId = computed(() => route.query.parentCommentId || null)

const post = ref(null)
const loading = ref(true)
const loadError = ref(false)
const commentSectionRef = ref(null)
const showShareDialog = ref(false)
const showFullTitleDialog = ref(false)
const shareLink = ref('')
const contentExpanded = ref(false)
const isAdminUser = computed(() => userStore.userInfo?.role === 9)
const banDialogVisible = ref(false)
const banSaving = ref(false)
const canAdminBanAuthor = computed(() => {
  if (!isAdminUser.value || !post.value?.userId) return false
  return String(post.value.userId) !== String(userStore.userInfo?.id || '')
})

// 旧报告分享帖可能没有 title 字段，详情页需要兜底显示稳定标题。
const displayTitle = computed(() => {
  const title = post.value?.title?.trim()
  if (title) return title
  return post.value?.sharedInterviewSessionId ? '面试报告分享' : ''
})

// 详情页默认收起超长正文，避免长文本直接占满首屏和评论入口。
const shouldCollapseContent = computed(() => (post.value?.content || '').length > 600)

// 超长标题默认压缩展示，完整标题交给弹窗查看，避免详情页首屏被标题挤满。
const shouldCollapseTitle = computed(() => displayTitle.value.length > DETAIL_TITLE_COLLAPSE_LENGTH)

// 路由参数变化时重新加载（Vue Router 会复用组件，onMounted 不会重新触发）
watch(() => route.params.postId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    post.value = null
    contentExpanded.value = false
    loadPost()
  }
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/community')
  }
}

const handleLike = async () => {
  try {
    await togglePostLike(post.value.id)
    post.value.liked = !post.value.liked
    post.value.likeCount = post.value.liked
      ? (post.value.likeCount || 0) + 1
      : Math.max(0, (post.value.likeCount || 0) - 1)
  } catch (err) {
    console.error('点赞失败:', err)
    ElMessage.error('操作失败，请重试')
  }
}

const handleFavorite = async () => {
  try {
    await togglePostFavorite(post.value.id)
    post.value.favorited = !post.value.favorited
  } catch (err) {
    console.error('收藏失败:', err)
    ElMessage.error('操作失败，请重试')
  }
}

const handleShare = () => {
  shareLink.value = window.location.href
  showShareDialog.value = true
}

const promptAdminHideReason = async () => {
  const { value } = await ElMessageBox.prompt('请填写下架原因，系统会通知发帖用户。', '下架帖子', {
    confirmButtonText: '确认下架',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请填写200字以内的下架原因',
    inputValidator: validateAdminHideReason,
    type: 'warning'
  })
  return normalizeAdminHideReason(value)
}

const handleAdminHide = async () => {
  if (!post.value) return
  try {
    const reason = await promptAdminHideReason()
    await adminHidePost(post.value.id, { reason })
    ElMessage.success('帖子已下架，并已通知用户')
    router.push('/community')
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    console.error('[帖子详情] 管理员下架帖子失败:', err)
    ElMessage.error('下架失败，请稍后重试')
  }
}

const openBanDialog = () => {
  if (!post.value?.userId) {
    ElMessage.error('无法识别要封禁的用户')
    return
  }
  banDialogVisible.value = true
}

const submitBanUser = async (payload) => {
  if (!post.value?.userId) return
  banSaving.value = true
  try {
    await adminBanUser(post.value.userId, payload)
    ElMessage.success('用户已封禁')
    banDialogVisible.value = false
  } catch (err) {
    console.error('[帖子详情] 管理员封禁用户失败:', err)
    ElMessage.error('封禁失败，请稍后重试')
  } finally {
    banSaving.value = false
  }
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制到剪贴板')
    showShareDialog.value = false
  } catch {
    ElMessage.info('请手动复制链接')
  }
}

const scrollToComments = () => {
  commentSectionRef.value?.scrollIntoView({ behavior: 'smooth' })
}

const handleCommentDeleted = (count = 1) => {
  post.value.commentCount = Math.max(0, (post.value.commentCount || 0) - Number(count || 1))
}

const loadPost = async () => {
  loading.value = true
  loadError.value = false
  // 重置滚动位置到顶部
  const scrollContainer = document.querySelector('.layout-content')
  if (scrollContainer) scrollContainer.scrollTop = 0
  try {
    const res = await getPostDetail(postId.value)
    if (res.code === 200) {
      post.value = res.data
      contentExpanded.value = false
    } else {
      loadError.value = true
    }
  } catch (err) {
    console.error('[帖子详情] 加载失败:', err)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

loadPost()
</script>

<style scoped>
/* ===== 整体页面 ===== */
.post-detail-page {
  min-height: 0;
  animation: pageIn 0.35s ease both;
}

@keyframes pageIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 顶部返回栏 ===== */
.top-bar {
  padding: 0 0 16px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  background: var(--bg-card);
  color: var(--text-body);
  font-size: 14px;
  cursor: pointer;
  border-radius: 10px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  transition: background-color 0.25s ease, color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
}

.back-btn:hover {
  color: var(--orange-main);
  background: var(--orange-light-bg);
  border-color: var(--orange-border);
  gap: 10px;
}

.back-btn svg {
  width: 18px;
  height: 18px;
  transition: transform 0.25s;
}

.back-btn:hover svg {
  transform: translateX(-2px);
}

/* ===== 加载/错误状态 ===== */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  gap: 16px;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-feature-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  text-align: center;
}

.error-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.error-icon-wrapper svg { width: 40px; height: 40px; color: var(--color-danger); }
.error-title { margin: 0 0 8px; font-size: 18px; font-weight: 600; color: var(--text-title); }
.error-desc { margin: 0 0 24px; font-size: 14px; color: var(--text-muted); }
.error-actions { display: flex; gap: 12px; }

/* ===== 上半部分：帖子区 ===== */
.post-area {
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  margin-bottom: 20px;
  overflow: hidden;
}

.post-area,
.post-title,
.post-content,
.author-name,
.post-time,
.category-tag {
  cursor: default;
}

.post-inner {
  padding: 28px 32px 24px;
}

.post-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.author-pill {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 0 0 2px var(--bg-card), 0 0 0 3.5px var(--orange-border);
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.author-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
}

.post-time {
  font-size: 12px;
  color: var(--text-placeholder);
}

.category-tag {
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 10px;
  font-weight: 600;
  letter-spacing: 0.3px;
  flex-shrink: 0;
}

.category-tag.interview_exp {
  background: rgba(255, 140, 66, 0.1);
  color: var(--orange-main);
}

.category-tag.referral {
  background: rgba(103, 194, 58, 0.1);
  color: var(--color-success);
}

/* —— 帖子正文 —— */
.post-body {
  margin-bottom: 20px;
}

.post-title {
  margin: 0 0 12px;
  color: var(--text-title);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-word;
}

.post-title.collapsed-title {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.title-dialog-btn {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  margin: -2px 0 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--orange-main);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.title-dialog-btn:hover {
  color: var(--orange-deep);
}

.full-title-text {
  margin: 0;
  color: var(--text-body);
  font-size: 15px;
  line-height: 1.8;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.post-content {
  font-size: 15px;
  color: var(--text-body);
  line-height: 1.85;
  margin: 0 0 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-content.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 8;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.content-toggle {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  margin: 0 0 16px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--orange-main);
  font-size: 14px;
  font-weight: 700;
}

.content-toggle:hover {
  color: var(--orange-deep);
}

.report-link-card {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid var(--orange-border);
  border-radius: 10px;
  background: var(--orange-light-bg);
  color: var(--orange-deep);
  text-decoration: none;
  transition:
    transform 0.16s cubic-bezier(0.25, 1, 0.5, 1),
    border-color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    background-color 0.2s cubic-bezier(0.25, 1, 0.5, 1);
}

.report-link-card:hover {
  border-color: var(--orange-main);
  background: color-mix(in srgb, var(--orange-light-bg) 70%, #ffffff 30%);
  transform: translateY(-1px);
}

.report-link-main {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 700;
}

.report-link-title {
  min-width: 0;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-images {
  margin-top: 12px;
}

/* —— 操作栏 —— */
.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid var(--border-divider);
  padding-top: 16px;
}

.act-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border: none;
  background: var(--bg-elevated);
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: 20px;
  transition: background-color 0.25s ease, color 0.25s ease;
}

.act-btn:hover {
  background: var(--orange-light-bg);
  color: var(--orange-main);
}

.act-btn.active {
  color: #ff4d6a;
  background: rgba(255, 77, 106, 0.06);
}

.act-btn.active:hover {
  background: rgba(255, 77, 106, 0.1);
}

.act-btn.active-fav {
  color: #f5a623;
  background: rgba(245, 166, 35, 0.06);
}

.act-btn.active-fav:hover {
  background: rgba(245, 166, 35, 0.1);
}

.admin-hide-detail-btn {
  margin-left: auto;
  color: var(--color-danger);
  background: rgba(245, 63, 63, 0.06);
}

.admin-hide-detail-btn:hover {
  color: var(--color-danger);
  background: rgba(245, 63, 63, 0.1);
}

.admin-ban-detail-btn {
  color: #b42318;
  background: rgba(180, 35, 24, 0.06);
}

.admin-ban-detail-btn:hover {
  color: #b42318;
  background: rgba(180, 35, 24, 0.1);
}

.act-btn svg {
  width: 16px;
  height: 16px;
}

/* ===== 下半部分：评论区 ===== */
.comment-area {
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  min-height: 300px;
}

.comment-inner {
  padding: 28px 32px;
}

/* ===== 分享弹窗 ===== */
.share-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.share-label {
  font-size: 14px;
  color: var(--text-body);
  margin: 0;
}

.share-link-row {
  display: flex;
  gap: 8px;
}

.share-link-row .el-input {
  flex: 1;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .post-inner,
  .comment-inner {
    padding: 20px 18px;
  }

  .post-content {
    font-size: 14px;
  }

  .post-title {
    font-size: 19px;
  }
}

@media (max-width: 480px) {
  .post-inner,
  .comment-inner {
    padding: 16px 14px;
  }

  .post-top-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .report-link-card {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .report-link-title {
    white-space: normal;
  }
}

@media (prefers-reduced-motion: reduce) {
  .post-detail-page,
  .report-link-card,
  .content-toggle {
    animation-duration: 0.01ms;
    transition-duration: 0.01ms;
  }

  .report-link-card:hover {
    transform: none;
  }
}
</style>
