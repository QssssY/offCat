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
            <p class="post-content">{{ post.content }}</p>
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
          </div>
        </div>
      </section>

      <!-- 下半部分：评论区 -->
      <section ref="commentSectionRef" class="comment-area">
        <div class="comment-inner">
          <CommentSection :key="postId" :post-id="postId" :post-user-id="post?.userId" :scroll-to-id="scrollToId" :scroll-to-parent-id="scrollToParentId" @comment-deleted="post.commentCount = Math.max(0, (post.commentCount || 0) - 1)" @comment-added="post.commentCount = (post.commentCount || 0) + 1" />
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
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostDetail, togglePostLike, togglePostFavorite } from '@/api/community'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import ImageGrid from '@/components/community/ImageGrid.vue'
import CommentSection from '@/components/community/CommentSection.vue'
import { optimizedImages } from '@/utils/optimizedImages'
import { formatTime, categoryLabel } from '@/utils/community'

const defaultAvatar = optimizedImages.userAvatar.webp

const router = useRouter()
const route = useRoute()
const postId = computed(() => route.params.postId)
const scrollToId = computed(() => route.query.commentId || null)
const scrollToParentId = computed(() => route.query.parentCommentId || null)

const post = ref(null)
const loading = ref(true)
const loadError = ref(false)
const commentSectionRef = ref(null)
const showShareDialog = ref(false)
const shareLink = ref('')

// 路由参数变化时重新加载（Vue Router 会复用组件，onMounted 不会重新触发）
watch(() => route.params.postId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    post.value = null
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
  transition: all 0.25s;
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

.post-content {
  font-size: 15px;
  color: var(--text-body);
  line-height: 1.85;
  margin: 0 0 16px;
  white-space: pre-wrap;
  word-break: break-all;
  letter-spacing: 0.2px;
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
  transition: all 0.25s;
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
}
</style>
