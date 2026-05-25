<template>
  <div class="community-page">
    <!-- 【页面标题横幅】带渐变背景的装饰性banner区域 -->
    <div class="page-banner">
      <button class="my-activity-btn" @click="router.push('/community/my')">
        <FeatureIcon name="community-activity" size="sm" />
        <span>个人动态中心</span>
        <span v-if="unreadCount > 0" class="activity-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </button>
      <div class="page-header">
        <div class="header-icon">
          <FeatureIcon name="community-hub" size="md" />
        </div>
        <div>
          <h1 class="page-title">社区</h1>
          <p class="page-desc">分享面试经验，发现内推机会</p>
        </div>
      </div>
      <!-- 【装饰性背景圆点】营造层次感 -->
      <div class="banner-dot dot-1"></div>
      <div class="banner-dot dot-2"></div>
      <div class="banner-dot dot-3"></div>
    </div>

    <!-- Tab 切换栏 + 排序 -->
    <div class="tab-bar">
      <div class="tab-group">
        <button
          class="tab-item"
          :class="{ active: activeTab === 'all' }"
          @click="switchTab('all')"
        >
          全部
        </button>
        <button
          class="tab-item"
          :class="{ active: activeTab === 'interview_exp' }"
          @click="switchTab('interview_exp')"
        >
          <FeatureIcon name="interview-replay" size="xs" class="tab-icon" />
          面试经验分享
        </button>
        <button
          class="tab-item"
          :class="{ active: activeTab === 'referral' }"
          @click="switchTab('referral')"
        >
          <FeatureIcon name="offer-comparison" size="xs" class="tab-icon" />
          内推广场
        </button>
      </div>
      <div class="sort-group">
        <button
          class="sort-btn"
          :class="{ active: sortBy === 'latest' }"
          @click="switchSort('latest')"
        >
          最新
        </button>
        <button
          class="sort-btn"
          :class="{ active: sortBy === 'hot' }"
          @click="switchSort('hot')"
        >
          最热
        </button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="post-feed">
      <!-- 【加载骨架屏】闪光扫光动画，视觉更高级 -->
      <template v-if="loading && posts.length === 0">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <div class="skeleton-header">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-meta">
              <div class="skeleton-line short"></div>
              <div class="skeleton-line shorter"></div>
            </div>
          </div>
          <div class="skeleton-body">
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line medium"></div>
          </div>
        </div>
      </template>

      <!-- 帖子卡片列表 -->
      <template v-else-if="posts.length > 0">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          v-memo="[post.id, post.liked, post.favorited, post.likeCount, post.commentCount]"
          :post="post"
          class="post-feed-card"
          @click="goToDetail(post.id)"
          @like="handleLike(post)"
          @favorite="handleFavorite(post)"
          @share="handleShare(post)"
        />
      </template>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon-wrapper">
          <FeatureIcon name="empty-state" size="lg" />
        </div>
        <h3 class="empty-title">暂无帖子</h3>
        <p class="empty-desc">成为第一个分享的人吧</p>
      </div>

      <!-- 无限滚动哨兵元素 -->
      <div ref="sentinelRef" class="scroll-sentinel"></div>

      <!-- 加载更多指示器 -->
      <div v-if="loadingMore" class="load-more-area">
        <div class="loading-more">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>
      </div>

      <!-- 【没有更多】装饰性分隔符 -->
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">已经到底了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 悬浮按钮组 -->
    <div class="fab-group">
      <!-- 刷新按钮 -->
      <button class="fab-button fab-refresh" aria-label="刷新帖子" @click="handleRefresh" :class="{ refreshing: isRefreshing }">
        <FeatureIcon name="retry" size="md" />
      </button>
      <!-- 发布按钮 -->
      <button class="fab-button fab-post" aria-label="发布帖子" @click="showEditor = true">
        <FeatureIcon name="edit" size="md" />
      </button>
    </div>

    <!-- 发布帖子弹窗 -->
    <el-dialog
      v-model="showEditor"
      title="发布帖子"
      width="600px"
      :close-on-click-modal="false"
      :append-to-body="true"
      class="post-editor-dialog"
      @closed="resetEditor"
    >
      <PostEditor
        v-if="showEditor"
        @published="onPostPublished"
        @cancel="showEditor = false"
      />
    </el-dialog>

    <!-- 分享面板 -->
    <el-dialog
      v-model="showShareDialog"
      title="分享帖子"
      width="400px"
      :append-to-body="true"
      class="share-dialog"
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
import { ref, onMounted, onUnmounted, nextTick, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostList, togglePostLike, togglePostFavorite, getInteractionUnreadCount } from '@/api/community'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import PostCard from '@/components/community/PostCard.vue'

const PostEditor = defineAsyncComponent(() => import('@/components/community/PostEditor.vue'))

const router = useRouter()

const activeTab = ref('all')
const sortBy = ref('latest')
const posts = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const pageNum = ref(1)
const pageSize = 8
const hasMore = ref(false)
const showEditor = ref(false)
const showShareDialog = ref(false)
const shareLink = ref('')
const unreadCount = ref(0)
const isRefreshing = ref(false)
const LAST_SEEN_KEY = 'community_last_interaction_seen'

// 无限滚动相关
const sentinelRef = ref(null)
let observer = null

const fetchPosts = async (page = 1, append = false) => {
  if (page === 1) loading.value = true
  else loadingMore.value = true

  try {
    const params = {
      pageNum: page,
      pageSize,
      sort: sortBy.value
    }
    if (activeTab.value !== 'all') {
      params.category = activeTab.value
    }
    const res = await getPostList(params)
    if (res.code === 200) {
      const records = res.data?.list || []
      const total = res.data?.total || 0
      if (append) {
        posts.value.push(...records)
      } else {
        posts.value = records
      }
      hasMore.value = posts.value.length < total
      pageNum.value = page
    }
  } catch (err) {
    console.error('[社区] 获取帖子失败:', err)
  } finally {
    loading.value = false
    loadingMore.value = false
    // 数据加载完成后重新设置 observer
    nextTick(setupObserver)
  }
}

const switchTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  posts.value = []
  hasMore.value = false
  pageNum.value = 1
  fetchPosts(1)
}

const switchSort = (sort) => {
  if (sortBy.value === sort) return
  sortBy.value = sort
  posts.value = []
  hasMore.value = false
  pageNum.value = 1
  fetchPosts(1)
}

const loadMore = () => {
  if (loadingMore.value || !hasMore.value) return
  fetchPosts(pageNum.value + 1, true)
}

const getScrollRoot = () => document.querySelector('.layout-content')

// 设置 Intersection Observer 实现无限滚动
const setupObserver = () => {
  if (observer) observer.disconnect()

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry.isIntersecting && hasMore.value && !loadingMore.value && !loading.value) {
        loadMore()
      }
    },
    {
      // 社区首页滚动发生在 layout-content 内，observer 需要绑定真实滚动容器。
      root: getScrollRoot(),
      rootMargin: '200px',
      threshold: 0
    }
  )

  if (sentinelRef.value) {
    observer.observe(sentinelRef.value)
  }
}

const goToDetail = (postId) => {
  router.push(`/community/post/${postId}`)
}

const handleLike = async (post) => {
  try {
    await togglePostLike(post.id)
    post.liked = !post.liked
    post.likeCount = post.liked
      ? (post.likeCount || 0) + 1
      : Math.max(0, (post.likeCount || 0) - 1)
  } catch (err) {
    console.error('点赞失败:', err)
    ElMessage.error('操作失败，请重试')
  }
}

const handleFavorite = async (post) => {
  try {
    await togglePostFavorite(post.id)
    post.favorited = !post.favorited
  } catch (err) {
    console.error('收藏失败:', err)
    ElMessage.error('操作失败，请重试')
  }
}

const handleShare = (post) => {
  shareLink.value = `${window.location.origin}/community/post/${post.id}`
  showShareDialog.value = true
}

const copyLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制到剪贴板')
    showShareDialog.value = false
  } catch {
    // Clipboard API 不可用时，提示用户手动复制
    ElMessage.info('请手动复制链接')
  }
}

const onPostPublished = () => {
  showEditor.value = false
  fetchPosts(1)
}

const handleRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  posts.value = []
  hasMore.value = false
  pageNum.value = 1
  await fetchPosts(1)
  isRefreshing.value = false
}

const resetEditor = () => {
  // dialog closed
}

const refreshUnreadCount = () => {
  const lastSeen = localStorage.getItem(LAST_SEEN_KEY)
  if (!lastSeen) return
  getInteractionUnreadCount(lastSeen).then(res => {
    if (res.code === 200) {
      unreadCount.value = res.data || 0
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchPosts(1)
  refreshUnreadCount()
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
})
</script>

<style scoped>
/* ===== 社区主页面样式（UI美化版 v2） ===== */

/* 【社区页面容器】注意：不能在此元素上使用带 transform 的动画，
   否则会创建新的包含块，导致内部 position: fixed 的悬浮按钮定位失效 */
.community-page {
  min-height: 0;
  padding: 0 0 40px;
  position: relative;
}

/* 【标题横幅】渐变背景卡片，带装饰性浮动圆点 */
.page-banner {
  position: relative;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 215, 191, 0.3) 100%);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 24px;
  border: 1px solid var(--orange-border);
  overflow: hidden;
}

/* 【装饰性背景圆点】三个浮动半透明圆，营造空间层次 */
.banner-dot {
  position: absolute;
  border-radius: 50%;
  background: var(--orange-main);
  opacity: 0.06;
  pointer-events: none;
}

.banner-dot.dot-1 {
  width: 120px;
  height: 120px;
  top: -30px;
  right: -20px;
}

.banner-dot.dot-2 {
  width: 60px;
  height: 60px;
  bottom: -10px;
  right: 80px;
}

.banner-dot.dot-3 {
  width: 40px;
  height: 40px;
  top: 10px;
  right: 140px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.my-activity-btn {
  position: absolute;
  top: 16px;
  right: 20px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: var(--text-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 10px;
  backdrop-filter: blur(6px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.25s;
  letter-spacing: 0.2px;
}

.my-activity-btn:hover {
  background: #fff;
  color: var(--orange-main);
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.15);
  gap: 8px;
}

.my-activity-btn svg {
  width: 16px;
  height: 16px;
}

.activity-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #f53f3f;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  border-radius: 10px;
  pointer-events: none;
  box-shadow: 0 1px 3px rgba(245, 63, 63, 0.4);
}

.header-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--orange-main) 0%, var(--orange-deep) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.35);
}

.header-icon svg {
  width: 26px;
  height: 26px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-title);
  letter-spacing: 0.5px;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 0.3px;
}

/* 【Tab 切换栏】使用项目设计令牌，增强玻璃质感 */
.tab-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.tab-group {
  display: flex;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 4px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  letter-spacing: 0.2px;
  position: relative;
}

.tab-item:hover {
  color: var(--text-title);
  background: var(--bg-page);
}

/* 【Tab激活态】品牌渐变背景 + 底部指示器圆点 */
.tab-item.active {
  background: linear-gradient(135deg, var(--orange-main) 0%, var(--orange-deep) 100%);
  color: #fff;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.35);
}

.tab-icon {
  width: 16px;
  height: 16px;
}

/* 【排序切换组】与Tab栏视觉风格统一 */
.sort-group {
  display: flex;
  gap: 4px;
  background: var(--bg-card);
  border-radius: 10px;
  padding: 3px;
  border: 1px solid var(--border-card);
}

.sort-btn {
  padding: 6px 14px;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sort-btn:hover {
  color: var(--text-title);
}

.sort-btn.active {
  background: var(--orange-light-bg);
  color: var(--orange-main);
  font-weight: 500;
}

/* 【帖子列表】卡片间距优化 */
.post-feed {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-feed-card {
  display: block;
  content-visibility: auto;
  contain-intrinsic-size: 180px;
}

/* 【骨架屏】闪光扫光动画（shimmer），视觉更高级 */
.skeleton-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--border-card);
  overflow: hidden;
}

.skeleton-header {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(90deg, var(--bg-elevated) 25%, #f0e6de 50%, var(--bg-elevated) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

.skeleton-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
}

.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-elevated) 25%, #f0e6de 50%, var(--bg-elevated) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

.skeleton-line.short { width: 40%; }
.skeleton-line.shorter { width: 25%; }
.skeleton-line.medium { width: 70%; }

/* 【闪光扫光动画】从左到右的渐变位移 */
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 【空状态】优雅的留白与图标呈现 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  text-align: center;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, var(--orange-border) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  animation: float-breathe 3s ease-in-out infinite;
}

/* 【空状态图标呼吸动画】缓慢上下浮动 */
@keyframes float-breathe {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-icon-wrapper svg {
  width: 40px;
  height: 40px;
  color: var(--orange-main);
}

.empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title);
}

.empty-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

/* 【无限滚动哨兵】 */
.scroll-sentinel {
  height: 1px;
  width: 100%;
}

/* 【加载更多】居中布局 */
.load-more-area {
  text-align: center;
  padding: 24px 0;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
}

/* 【加载动画】品牌色旋转指示器 */
.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 【到底了】装饰性双线分隔符 */
.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 28px 0;
}

.no-more-line {
  display: block;
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--orange-border), transparent);
}

.no-more-text {
  font-size: 13px;
  color: var(--text-placeholder);
  letter-spacing: 2px;
}

/* 【悬浮按钮组】固定在右下角 */
.fab-group {
  position: fixed;
  bottom: 32px;
  right: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 100;
}

/* 【悬浮按钮】使用浅暖色表面，避免深色圆底压住业务图标 */
.fab-button {
  --community-fab-bg: color-mix(in srgb, var(--bg-card) 78%, #fff0df 22%);
  --community-fab-bg-hover: color-mix(in srgb, var(--bg-card) 66%, #ffe3c6 34%);
  --community-fab-border: color-mix(in srgb, var(--orange-main) 42%, #ffffff 58%);
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--community-fab-bg);
  border: 1px solid var(--community-fab-border);
  color: var(--orange-deep);
  cursor: pointer;
  box-shadow:
    0 12px 26px rgba(132, 75, 32, 0.13),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    transform 220ms cubic-bezier(0.22, 1, 0.36, 1),
    background-color 220ms cubic-bezier(0.22, 1, 0.36, 1),
    border-color 220ms cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.fab-button :deep(.feature-icon) {
  width: 40px;
  height: 40px;
}

.fab-button:hover {
  background: var(--community-fab-bg-hover);
  border-color: color-mix(in srgb, var(--orange-main) 58%, #ffffff 42%);
  box-shadow:
    0 16px 32px rgba(132, 75, 32, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.fab-refresh:hover {
  transform: translateY(-2px) scale(1.03);
}

.fab-refresh:hover :deep(.feature-icon),
.fab-refresh.refreshing :deep(.feature-icon) {
  animation: fabRefreshSpin 0.75s linear infinite;
}

.fab-post:hover {
  transform: translate3d(0, -4px, 0) scale(1.02);
}

.fab-button:active {
  transform: translateY(0) scale(0.96);
}

.fab-post:active {
  transform: translateY(-1px) scale(0.96);
}

/* 【刷新按钮】旋转动画 */
@keyframes fabRefreshSpin {
  to { transform: rotate(360deg); }
}

:global(html[data-theme="dark"]) .fab-button {
  --community-fab-bg: color-mix(in srgb, var(--bg-card) 82%, rgba(255, 176, 122, 0.16) 18%);
  --community-fab-bg-hover: color-mix(in srgb, var(--bg-card) 72%, rgba(255, 176, 122, 0.24) 28%);
  --community-fab-border: rgba(255, 176, 122, 0.28);
  box-shadow:
    0 14px 28px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 220, 190, 0.09);
}

@media (prefers-reduced-motion: reduce) {
  .fab-button {
    transition-duration: 0.01ms;
  }

  .fab-refresh:hover,
  .fab-post:hover,
  .fab-button:active,
  .fab-post:active {
    transform: none;
  }

  .fab-refresh:hover :deep(.feature-icon),
  .fab-refresh.refreshing :deep(.feature-icon) {
    animation: none;
  }
}

/* 【发布弹窗】统一圆角与阴影 */
.post-editor-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

.post-editor-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 0;
}

.post-editor-dialog :deep(.el-dialog__body) {
  padding: 20px 24px 24px;
}

/* 【分享弹窗】与发布弹窗风格一致 */
.share-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

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

/* 【响应式】移动端布局适配 */
@media (max-width: 768px) {
  .page-banner {
    padding: 20px;
    border-radius: 12px;
  }

  .tab-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .tab-group {
    overflow-x: auto;
  }

  .sort-group {
    align-self: flex-end;
  }

  .fab-group {
    bottom: 20px;
    right: 20px;
  }

  .fab-button {
    width: 58px;
    height: 58px;
  }

  .fab-button :deep(.feature-icon) {
    width: 36px;
    height: 36px;
  }
}

@media (max-width: 480px) {
  .tab-item {
    padding: 6px 12px;
    font-size: 13px;
  }

  .tab-icon {
    display: none;
  }
}
</style>
