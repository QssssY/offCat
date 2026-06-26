<template>
  <div class="my-activity-page">
    <!-- 顶部导航 -->
    <div class="top-bar">
      <button class="back-btn" @click="goBack">
        <FeatureIcon name="back" size="xs" />
        <span>返回</span>
      </button>
      <h1 class="page-title">个人动态中心</h1>
    </div>

    <!-- 标签栏 -->
    <div class="tab-bar-wrapper">
      <div class="tab-bar">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="tab.hasUnread && unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
        <div class="tab-indicator" :style="indicatorStyle"></div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-state">
      <FeatureIcon name="loading" size="sm" class="loading-feature-icon" />
      <span>加载中...</span>
    </div>

    <!-- 评论列表（评论过的帖子标签） -->
    <div v-else-if="activeTab === 'commented' && commentedState.comments.value.length > 0" class="post-list">
      <DynamicScroller :items="commentedState.comments.value" :min-item-size="150" key-field="commentId" class="post-list-inner virtual-activity-list" :buffer="700">
        <template #default="{ item, active }">
          <DynamicScrollerItem :item="item" :active="active" :size-dependencies="[item.commentContent, item.postContent, item.postDeleted]">
        <div class="my-comment-card virtual-activity-card" :class="{ 'post-deleted': item.postDeleted }" @click="!item.postDeleted && goToDetail(item.postId, item.commentId, item.parentCommentId)">
          <!-- 我的评论（主要） -->
          <div class="comment-primary">
            <div class="comment-primary-header">
              <FeatureIcon name="comment" size="xs" class="comment-icon" />
              <span class="comment-label">我的评论</span>
              <span class="comment-time">{{ formatTime(item.commentTime) }}</span>
            </div>
            <p class="comment-text">{{ item.commentContent }}</p>
          </div>
          <!-- 所属帖子（次要） -->
          <div v-if="item.postDeleted" class="comment-post-ref deleted">
            <FeatureIcon name="error" size="xs" />
            <span class="deleted-text">原帖已被删除</span>
          </div>
          <div v-else class="comment-post-ref">
            <span class="category-dot" :class="item.postCategory"></span>
            <span class="category-label">{{ categoryLabel(item.postCategory) }}</span>
            <span class="post-author">@{{ item.postAuthorName }}</span>
            <span class="post-snippet">{{ item.postTitle || item.postContent?.slice(0, 60) }}{{ !item.postTitle && (item.postContent?.length || 0) > 60 ? '...' : '' }}</span>
            <span v-if="getImageCount(item.postImages) > 0" class="post-img-count">
              <FeatureIcon name="image-upload" size="xs" />
              {{ getImageCount(item.postImages) }}
            </span>
          </div>
        </div>
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>

      <!-- 加载更多 -->
      <div v-if="commentedState.hasMore.value" class="load-more">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="btn-spinner"></span>
          <span v-else>加载更多</span>
        </button>
      </div>

      <div v-if="!commentedState.hasMore.value && commentedState.comments.value.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">没有更多了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 帖子列表（我的帖子 / 点赞过的帖子 / 收藏的帖子） -->
    <div v-else-if="['mine', 'liked', 'favorited'].includes(activeTab) && currentTabState.posts.value.length > 0" class="post-list">
      <DynamicScroller :items="currentTabState.posts.value" :min-item-size="150" key-field="id" class="post-list-inner virtual-activity-list" :buffer="700">
        <template #default="{ item: post, active }">
          <DynamicScrollerItem :item="post" :active="active" :size-dependencies="[post.title, post.content, post.images?.length, post.likeCount, post.commentCount, post.reviewStatus, post.reviewReason]">
        <div class="my-post-card virtual-activity-card">
          <div class="card-main" @click="goToDetail(post.id)">
            <div class="card-header">
              <span class="category-dot" :class="post.category"></span>
              <span class="category-label">{{ categoryLabel(post.category) }}</span>
              <span
                v-if="activeTab === 'mine' && shouldShowReviewStatus(post.reviewStatus)"
                class="review-status-badge"
                :class="`review-${post.reviewStatus}`"
              >
                {{ reviewStatusText(post.reviewStatus) }}
              </span>
              <span class="card-time">{{ formatTime(post.createTime) }}</span>
            </div>
            <h2 v-if="post.title" class="card-title">{{ post.title }}</h2>
            <p class="card-content">{{ post.content }}</p>
            <p
              v-if="activeTab === 'mine' && post.reviewReason"
              class="review-reason"
            >
              原因：{{ post.reviewReason }}
            </p>
            <div v-if="post.sharedInterviewSessionId" class="card-report-link">
              <FeatureIcon name="interview-report" size="xs" />
              <span>面试报告链接</span>
            </div>
            <div v-if="post.images && post.images.length > 0" class="card-image-hint">
              <FeatureIcon name="image-upload" size="xs" />
              <span>{{ post.images.length }}张图片</span>
            </div>
            <div class="card-stats">
              <span class="stat">
                <FeatureIcon name="liked" size="xs" />
                {{ post.likeCount || 0 }}
              </span>
              <span class="stat">
                <FeatureIcon name="comment" size="xs" />
                {{ post.commentCount || 0 }}
              </span>
            </div>
          </div>
          <button v-if="activeTab === 'mine'" class="delete-btn" @click.stop="confirmDelete(post)" title="删除帖子">
            <FeatureIcon name="delete" size="xs" />
          </button>
        </div>
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>

      <!-- 加载更多 -->
      <div v-if="currentTabState.hasMore.value" class="load-more">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="btn-spinner"></span>
          <span v-else>加载更多</span>
        </button>
      </div>

      <div v-if="!currentTabState.hasMore.value && currentTabState.posts.value.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">没有更多了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 收到的点赞列表 -->
    <div v-else-if="activeTab === 'receivedLikes' && receivedLikesState.items.value.length > 0" class="post-list">
      <DynamicScroller :items="receivedLikesState.items.value" :min-item-size="105" key-field="virtualKey" class="post-list-inner virtual-activity-list" :buffer="700">
        <template #default="{ item, active }">
          <DynamicScrollerItem :item="item" :active="active" :size-dependencies="[item.postContent, item.createTime]">
        <div class="interaction-card virtual-activity-card" :class="{ 'is-new': isNewInteraction(item.createTime) }" @click="goToDetail(item.postId)">
          <div class="interaction-card-main">
            <span class="actor-name">{{ item.userName }}</span>
            <span v-if="isNewInteraction(item.createTime)" class="new-badge">新</span>
            <span class="action-text">赞了你的帖子</span>
            <span class="interaction-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="interaction-post-ref">
            <span class="category-dot" :class="item.postCategory"></span>
            <span class="post-snippet">{{ item.postTitle || item.postContent }}</span>
          </div>
        </div>
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>

      <!-- 加载更多 -->
      <div v-if="receivedLikesState.hasMore.value" class="load-more">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="btn-spinner"></span>
          <span v-else>加载更多</span>
        </button>
      </div>

      <div v-if="!receivedLikesState.hasMore.value && receivedLikesState.items.value.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">没有更多了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 收到的评论列表 -->
    <div v-else-if="activeTab === 'receivedComments' && receivedCommentsState.items.value.length > 0" class="post-list">
      <DynamicScroller :items="receivedCommentsState.items.value" :min-item-size="130" key-field="virtualKey" class="post-list-inner virtual-activity-list" :buffer="700">
        <template #default="{ item, active }">
          <DynamicScrollerItem :item="item" :active="active" :size-dependencies="[item.commentContent, item.postContent, item.createTime]">
        <div class="interaction-card virtual-activity-card" :class="{ 'is-new': isNewInteraction(item.createTime) }" @click="goToDetail(item.postId, item.commentId)">
          <div class="interaction-card-main">
            <span class="actor-name">{{ item.userName }}</span>
            <span v-if="isNewInteraction(item.createTime)" class="new-badge">新</span>
            <span class="action-text">评论了你的帖子</span>
            <span class="interaction-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <p class="comment-content">{{ item.commentContent }}</p>
          <div class="interaction-post-ref">
            <span class="category-dot" :class="item.postCategory"></span>
            <span class="post-snippet">{{ item.postTitle || item.postContent }}</span>
          </div>
        </div>
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>

      <!-- 加载更多 -->
      <div v-if="receivedCommentsState.hasMore.value" class="load-more">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="btn-spinner"></span>
          <span v-else>加载更多</span>
        </button>
      </div>

      <div v-if="!receivedCommentsState.hasMore.value && receivedCommentsState.items.value.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">没有更多了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 收到的回复列表 -->
    <div v-else-if="activeTab === 'receivedReplies' && receivedRepliesState.items.value.length > 0" class="post-list">
      <DynamicScroller :items="receivedRepliesState.items.value" :min-item-size="155" key-field="virtualKey" class="post-list-inner virtual-activity-list" :buffer="700">
        <template #default="{ item, active }">
          <DynamicScrollerItem :item="item" :active="active" :size-dependencies="[item.replyContent, item.parentCommentContent, item.postContent, item.createTime]">
        <div class="interaction-card virtual-activity-card" :class="{ 'is-new': isNewInteraction(item.createTime) }" @click="goToDetail(item.postId, item.replyId, item.parentCommentId)">
          <div class="interaction-card-main">
            <span class="actor-name">{{ item.userName }}</span>
            <span v-if="isNewInteraction(item.createTime)" class="new-badge">新</span>
            <span class="action-text">回复了你的评论</span>
            <span class="interaction-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <p class="comment-content">{{ item.replyContent }}</p>
          <div v-if="item.parentCommentContent" class="reply-parent">
            <span class="reply-parent-label">原评论：</span>
            <span class="reply-parent-text">{{ item.parentCommentContent }}</span>
          </div>
          <div class="interaction-post-ref">
            <span class="category-dot" :class="item.postCategory"></span>
            <span class="post-snippet">{{ item.postTitle || item.postContent }}</span>
          </div>
        </div>
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>

      <!-- 加载更多 -->
      <div v-if="receivedRepliesState.hasMore.value" class="load-more">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="btn-spinner"></span>
          <span v-else>加载更多</span>
        </button>
      </div>

      <div v-if="!receivedRepliesState.hasMore.value && receivedRepliesState.items.value.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">没有更多了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 收到的收藏列表 -->
    <div v-else-if="activeTab === 'receivedFavorites' && receivedFavoritesState.items.value.length > 0" class="post-list">
      <DynamicScroller :items="receivedFavoritesState.items.value" :min-item-size="105" key-field="virtualKey" class="post-list-inner virtual-activity-list" :buffer="700">
        <template #default="{ item, active }">
          <DynamicScrollerItem :item="item" :active="active" :size-dependencies="[item.postContent, item.createTime]">
        <div class="interaction-card virtual-activity-card" :class="{ 'is-new': isNewInteraction(item.createTime) }" @click="goToDetail(item.postId)">
          <div class="interaction-card-main">
            <span class="actor-name">{{ item.userName }}</span>
            <span v-if="isNewInteraction(item.createTime)" class="new-badge">新</span>
            <span class="action-text">收藏了你的帖子</span>
            <span class="interaction-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="interaction-post-ref">
            <span class="category-dot" :class="item.postCategory"></span>
            <span class="post-snippet">{{ item.postTitle || item.postContent }}</span>
          </div>
        </div>
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>

      <!-- 加载更多 -->
      <div v-if="receivedFavoritesState.hasMore.value" class="load-more">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <span v-if="loadingMore" class="btn-spinner"></span>
          <span v-else>加载更多</span>
        </button>
      </div>

      <div v-if="!receivedFavoritesState.hasMore.value && receivedFavoritesState.items.value.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">没有更多了</span>
        <span class="no-more-line"></span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon-wrapper">
        <FeatureIcon name="empty-state" size="lg" />
      </div>
      <h3 class="empty-title">{{ emptyInfo.title }}</h3>
      <p class="empty-desc">{{ emptyInfo.desc }}</p>
      <button v-if="activeTab === 'mine'" class="empty-action" @click="goToCommunity">去发帖</button>
    </div>

    <!-- 删除确认弹窗 -->
    <el-dialog
      v-model="showDeleteDialog"
      title="删除帖子"
      width="400px"
      :append-to-body="true"
    >
      <div class="delete-dialog-body">
        <p class="delete-warning">确定要删除这条帖子吗？删除后无法恢复。</p>
        <p v-if="deletingPost" class="delete-preview">{{ deletingPost.content?.slice(0, 80) }}{{ (deletingPost.content?.length || 0) > 80 ? '...' : '' }}</p>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="handleDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import { getMyPosts, getLikedPosts, getFavoritedPosts, getMyComments, deletePost, getMyInteractions, getInteractionUnreadCount } from '@/api/community'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import { formatTime, categoryLabel } from '@/utils/community'

const router = useRouter()

// 标签页配置
const tabs = [
  { key: 'mine', label: '我的帖子' },
  { key: 'liked', label: '点赞过的帖子' },
  { key: 'favorited', label: '收藏的帖子' },
  { key: 'commented', label: '评论过的帖子' },
  { key: 'receivedLikes', label: '收到的点赞', hasUnread: true },
  { key: 'receivedComments', label: '收到的评论', hasUnread: true },
  { key: 'receivedReplies', label: '收到的回复', hasUnread: true },
  { key: 'receivedFavorites', label: '收到的收藏', hasUnread: true }
]

const activeTab = ref('mine')

// 标签下划线指示器位置
const indicatorStyle = computed(() => {
  const idx = tabs.findIndex(t => t.key === activeTab.value)
  return { transform: `translateX(${idx * 100}%)` }
})

// 每个标签独立的状态
const mineState = { posts: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }
const likedState = { posts: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }
const favoritedState = { posts: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }
const commentedState = { comments: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }

// 收到的互动状态（每个类型独立）
const receivedLikesState = { items: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }
const receivedCommentsState = { items: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }
const receivedRepliesState = { items: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }
const receivedFavoritesState = { items: ref([]), pageNum: ref(1), hasMore: ref(false), loading: ref(false) }

// 未读互动数量
const unreadCount = ref(0)
const LAST_SEEN_KEY = 'community_last_interaction_seen'
let interactionsViewed = false
const lastSeenTime = ref(localStorage.getItem(LAST_SEEN_KEY) || '')
const isNewInteraction = (time) => {
  if (!lastSeenTime.value || !time) return false
  return new Date(time) > new Date(lastSeenTime.value)
}

const postStateMap = { mine: mineState, liked: likedState, favorited: favoritedState }
const interactionStateMap = {
  receivedLikes: receivedLikesState,
  receivedComments: receivedCommentsState,
  receivedReplies: receivedRepliesState,
  receivedFavorites: receivedFavoritesState
}

const currentTabState = computed(() => {
  if (activeTab.value === 'commented') return commentedState
  if (interactionStateMap[activeTab.value]) return interactionStateMap[activeTab.value]
  return postStateMap[activeTab.value]
})

const pageSize = 5
const loadingMore = ref(false)

// 加载状态判断
const isLoading = computed(() => {
  if (activeTab.value === 'commented') return commentedState.loading.value && commentedState.comments.value.length === 0
  if (interactionStateMap[activeTab.value]) return interactionStateMap[activeTab.value].loading.value && interactionStateMap[activeTab.value].items.value.length === 0
  return currentTabState.value.loading.value && currentTabState.value.posts.value.length === 0
})

// 空状态文案
const emptyInfo = computed(() => {
  const map = {
    mine: { title: '还没有发布过帖子', desc: '去社区分享你的面试经验吧' },
    liked: { title: '还没有点赞过帖子', desc: '去社区浏览并点赞感兴趣的帖子吧' },
    favorited: { title: '还没有收藏过帖子', desc: '去社区收藏感兴趣的帖子吧' },
    commented: { title: '还没有评论过帖子', desc: '去社区参与讨论吧' },
    receivedLikes: { title: '还没有收到点赞', desc: '发布帖子后，其他用户的点赞会显示在这里' },
    receivedComments: { title: '还没有收到评论', desc: '发布帖子后，其他用户的评论会显示在这里' },
    receivedReplies: { title: '还没有收到回复', desc: '评论帖子后，其他用户的回复会显示在这里' },
    receivedFavorites: { title: '还没有收到收藏', desc: '发布帖子后，其他用户的收藏会显示在这里' }
  }
  return map[activeTab.value]
})

// 我的帖子需要回显审核状态，让作者知道内容是否仍在审核池或已被处理。
const reviewStatusText = (status) => {
  const map = {
    pending: '待审核',
    rejected: '未通过',
    hidden: '已隐藏',
    approved: '已通过'
  }
  return map[status] || '待审核'
}

const shouldShowReviewStatus = (status) => status && status !== 'approved'

const getImageCount = (imagesJson) => {
  if (!imagesJson) return 0
  try {
    const arr = JSON.parse(imagesJson)
    return Array.isArray(arr) ? arr.length : 0
  } catch { return 0 }
}

const withVirtualKeys = (items, prefix, keyFn) => items.map((item, index) => ({
  ...item,
  virtualKey: `${prefix}-${keyFn(item)}-${index}`
}))

// 获取指定标签的数据
const fetchTabData = async (tab, page = 1, append = false) => {
  if (tab === 'commented') {
    // 评论标签：获取我的评论列表
    const state = commentedState
    if (page === 1) state.loading.value = true
    else loadingMore.value = true
    try {
      const res = await getMyComments({ pageNum: page, pageSize })
      if (res.code === 200) {
        const records = res.data?.list || []
        const total = res.data?.total || 0
        if (append) {
          state.comments.value.push(...records)
        } else {
          state.comments.value = records
        }
        state.hasMore.value = state.comments.value.length < total
        state.pageNum.value = page
      }
    } catch (err) {
      console.error('[个人动态] 获取评论失败:', err)
    } finally {
      state.loading.value = false
      loadingMore.value = false
    }
  } else if (tab === 'receivedLikes' || tab === 'receivedComments' || tab === 'receivedReplies' || tab === 'receivedFavorites') {
    // 收到的互动标签：获取互动信息
    const state = interactionStateMap[tab]
    if (page === 1) state.loading.value = true
    else loadingMore.value = true
    try {
      const res = await getMyInteractions({ pageNum: page, pageSize })
      if (res.code === 200) {
        const data = res.data || {}
        let newItems = []
        let total = 0
        if (tab === 'receivedLikes') { newItems = withVirtualKeys(data.likes || [], 'like', i => `${i.userId}-${i.postId}-${i.createTime}`); total = data.totalLikes || 0 }
        else if (tab === 'receivedComments') { newItems = withVirtualKeys(data.comments || [], 'comment', i => `${i.commentId}-${i.createTime}`); total = data.totalComments || 0 }
        else if (tab === 'receivedReplies') { newItems = withVirtualKeys(data.replies || [], 'reply', i => `${i.replyId}-${i.createTime}`); total = data.totalReplies || 0 }
        else if (tab === 'receivedFavorites') { newItems = withVirtualKeys(data.favorites || [], 'favorite', i => `${i.userId}-${i.postId}-${i.createTime}`); total = data.totalFavorites || 0 }

        if (append) {
          const keyFn = tab === 'receivedLikes' || tab === 'receivedFavorites'
            ? (i) => `${i.userId}-${i.postId}`
            : (i) => `${i.userId}-${i.commentId || i.replyId}-${i.createTime}`
          const existing = new Set(state.items.value.map(keyFn))
          state.items.value.push(...newItems.filter(i => !existing.has(keyFn(i))))
        } else {
          state.items.value = newItems
        }
        state.hasMore.value = state.items.value.length < total
        state.pageNum.value = page
      }
    } catch (err) {
      console.error(`[个人动态] 获取${tab}失败:`, err)
    } finally {
      state.loading.value = false
      loadingMore.value = false
    }
  } else {
    // 帖子标签：获取帖子列表
    const state = postStateMap[tab]
    const apiMap = { mine: getMyPosts, liked: getLikedPosts, favorited: getFavoritedPosts }
    if (page === 1) state.loading.value = true
    else loadingMore.value = true
    try {
      const res = await apiMap[tab]({ pageNum: page, pageSize })
      if (res.code === 200) {
        const records = res.data?.list || []
        const total = res.data?.total || 0
        if (append) {
          state.posts.value.push(...records)
        } else {
          state.posts.value = records
        }
        state.hasMore.value = state.posts.value.length < total
        state.pageNum.value = page
      }
    } catch (err) {
      console.error('[个人动态] 获取帖子失败:', err)
    } finally {
      state.loading.value = false
      loadingMore.value = false
    }
  }
}

// 切换标签（懒加载）
const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'commented') {
    if (commentedState.comments.value.length === 0 && !commentedState.loading.value) {
      fetchTabData(tab, 1)
    }
  } else if (tab === 'receivedLikes' || tab === 'receivedComments' || tab === 'receivedReplies' || tab === 'receivedFavorites') {
    // 先保留当前 lastSeen 用于标记新互动，再更新为当前时间
    const d = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    const now = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    localStorage.setItem(LAST_SEEN_KEY, now)
    // 延迟更新 lastSeenTime，让用户先看到哪些是新的
    setTimeout(() => { lastSeenTime.value = now }, 5000)
    unreadCount.value = 0
    interactionsViewed = true
    const state = interactionStateMap[tab]
    if (state.items.value.length === 0 && !state.loading.value) {
      fetchTabData(tab, 1)
    }
  } else {
    const state = postStateMap[tab]
    if (state.posts.value.length === 0 && !state.loading.value) {
      fetchTabData(tab, 1)
    }
  }
}

const loadMore = () => {
  const state = currentTabState.value
  fetchTabData(activeTab.value, state.pageNum.value + 1, true)
}

const goToDetail = (postId, commentId, parentCommentId) => {
  const query = commentId ? { commentId } : {}
  if (parentCommentId) query.parentCommentId = parentCommentId
  router.push({ path: `/community/post/${postId}`, query })
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/community')
  }
}

const goToCommunity = () => {
  router.push('/community')
}

// 删除帖子相关（仅 mine 标签）
const showDeleteDialog = ref(false)
const deletingPost = ref(null)
const deleting = ref(false)

const confirmDelete = (post) => {
  deletingPost.value = post
  showDeleteDialog.value = true
}

const handleDelete = async () => {
  if (!deletingPost.value) return
  deleting.value = true
  try {
    await deletePost(deletingPost.value.id)
    ElMessage.success('帖子已删除')
    mineState.posts.value = mineState.posts.value.filter(p => p.id !== deletingPost.value.id)
    showDeleteDialog.value = false
    deletingPost.value = null
  } catch (err) {
    console.error('删除失败:', err)
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  fetchTabData('mine', 1)
  // 查询未读互动数量
  const lastSeen = localStorage.getItem(LAST_SEEN_KEY)
  if (lastSeen) {
    getInteractionUnreadCount(lastSeen).then(res => {
      if (res.code === 200 && !interactionsViewed) {
        unreadCount.value = res.data || 0
      }
    }).catch(() => {})
  }
})
</script>

<style scoped>
.my-activity-page {
  min-height: 0;
  padding: 0 0 40px;
  animation: pageIn 0.35s ease both;
}

@keyframes pageIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 顶部导航 ===== */
.top-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
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

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-title);
  letter-spacing: 0.3px;
}

/* ===== 标签栏 ===== */
.tab-bar-wrapper {
  margin-bottom: 24px;
}

.tab-bar {
  display: flex;
  position: relative;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 4px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
}

.tab-btn {
  flex: 1;
  position: relative;
  z-index: 1;
  padding: 10px 0;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 9px;
  transition: color 0.25s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-btn.active {
  color: #fff;
  font-weight: 600;
}

.tab-label {
  position: relative;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -12px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: #f53f3f;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
  z-index: 2;
  pointer-events: none;
  box-shadow: 0 1px 3px rgba(245, 63, 63, 0.4);
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc((100% - 8px) / 8);
  height: calc(100% - 8px);
  background: linear-gradient(135deg, var(--orange-main), var(--orange-deep));
  border-radius: 9px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(255, 140, 66, 0.3);
}

/* ===== 加载状态 ===== */
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

/* ===== 帖子列表 ===== */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.post-list-inner {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.virtual-activity-list {
  overflow: visible;
}

.virtual-activity-card {
  margin-bottom: 12px;
}

/* ===== 评论卡片（评论过的帖子） ===== */
.my-comment-card {
  background: var(--bg-card);
  border-radius: 14px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  overflow: hidden;
  cursor: pointer;
  content-visibility: auto;
  contain-intrinsic-size: 180px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.my-comment-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border-color: var(--orange-border);
}

.my-comment-card.post-deleted {
  cursor: default;
  opacity: 0.75;
}

.my-comment-card.post-deleted:hover {
  box-shadow: var(--shadow-card);
  border-color: var(--border-card);
}

.comment-primary {
  padding: 18px 20px 14px;
}

.comment-primary-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.comment-icon {
  width: 16px;
  height: 16px;
  color: var(--orange-main);
  flex-shrink: 0;
}

.comment-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--orange-main);
}

.comment-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-left: auto;
}

.comment-text {
  font-size: 14px;
  color: var(--text-title);
  line-height: 1.7;
  margin: 0;
  word-break: break-all;
}

.comment-post-ref {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--bg-elevated);
  border-top: 1px solid var(--border-divider);
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
}

.comment-post-ref.deleted {
  gap: 6px;
  color: var(--text-placeholder);
}

.deleted-text {
  font-size: 12px;
  color: var(--text-placeholder);
}

.comment-post-ref .category-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.comment-post-ref .category-dot.interview_exp {
  background: var(--orange-main);
}

.comment-post-ref .category-dot.referral {
  background: var(--color-success);
}

.comment-post-ref .category-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  flex-shrink: 0;
}

.post-author {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-body);
  flex-shrink: 0;
}

.post-snippet {
  font-size: 12px;
  color: var(--text-placeholder);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.post-img-count {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--text-placeholder);
  flex-shrink: 0;
  margin-left: auto;
}

/* ===== 帖子卡片 ===== */
.my-post-card {
  display: flex;
  align-items: stretch;
  background: var(--bg-card);
  border-radius: 14px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  overflow: hidden;
  content-visibility: auto;
  contain-intrinsic-size: 180px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.my-post-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border-color: var(--orange-border);
}

.card-main {
  flex: 1;
  min-width: 0;
  padding: 18px 20px;
  cursor: pointer;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.category-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.category-dot.interview_exp {
  background: var(--orange-main);
}

.category-dot.referral {
  background: var(--color-success);
}

.category-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.3px;
}

.review-status-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  flex-shrink: 0;
}

.review-status-badge.review-pending {
  color: #ad5b00;
  background: #fff4df;
  border: 1px solid #ffd79a;
}

.review-status-badge.review-rejected {
  color: #c93535;
  background: #fff0f0;
  border: 1px solid #ffcaca;
}

.review-status-badge.review-hidden {
  color: #64748b;
  background: #f1f5f9;
  border: 1px solid #d8e0e8;
}

.card-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-left: auto;
}

.card-content {
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.7;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.card-title {
  margin: 0 0 8px;
  color: var(--text-title);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-word;
}

.review-reason {
  margin: -2px 0 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff7f7;
  border: 1px solid #ffd7d7;
  color: #b63b3b;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.card-report-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
  padding: 4px 10px;
  border-radius: 8px;
  background: var(--orange-light-bg);
  color: var(--orange-deep);
  font-size: 12px;
  font-weight: 600;
}

.card-image-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-elevated);
  padding: 3px 10px;
  border-radius: 6px;
  margin-bottom: 10px;
}

.card-image-hint svg {
  width: 14px;
  height: 14px;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.stat svg {
  width: 14px;
  height: 14px;
}

/* ===== 互动信息卡片 ===== */
.interaction-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border-card);
  cursor: pointer;
  content-visibility: auto;
  contain-intrinsic-size: 120px;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.interaction-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: var(--orange-border);
}

.interaction-card.is-new {
  background: linear-gradient(135deg, #fff8f5 0%, #fff 100%);
  border-color: var(--orange-border);
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.1);
}

.interaction-card-main {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.actor-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--orange-main);
}

.new-badge {
  display: inline-block;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff4d6a, #f53f3f);
  border-radius: 9px;
  margin-left: 4px;
  animation: newPulse 1.5s ease-in-out infinite;
}

@keyframes newPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.action-text {
  font-size: 13px;
  color: var(--text-body);
}

.interaction-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-left: auto;
}

.comment-content {
  font-size: 13px;
  color: var(--text-title);
  line-height: 1.6;
  margin: 0 0 8px;
  padding: 8px 10px;
  background: var(--bg-elevated);
  border-radius: 8px;
  word-break: break-all;
}

.interaction-post-ref {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.interaction-post-ref .category-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.interaction-post-ref .category-dot.interview_exp {
  background: var(--orange-main);
}

.interaction-post-ref .category-dot.referral {
  background: var(--color-success);
}

.reply-parent {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 6px 10px;
  background: var(--bg-elevated);
  border-radius: 6px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.reply-parent-label {
  flex-shrink: 0;
  font-weight: 600;
  color: var(--text-placeholder);
}

.reply-parent-text {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-snippet {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

/* ===== 删除按钮 ===== */
.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  flex-shrink: 0;
  border: none;
  background: none;
  color: var(--text-placeholder);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
  border-left: 1px solid var(--border-divider);
}

.delete-btn:hover {
  background: rgba(245, 63, 63, 0.06);
  color: var(--color-danger);
}

.delete-btn svg {
  width: 18px;
  height: 18px;
}

/* ===== 列表动画 ===== */
.post-card-enter-active {
  animation: cardIn 0.3s ease both;
}

.post-card-leave-active {
  animation: cardOut 0.25s ease both;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cardOut {
  from { opacity: 1; transform: translateX(0); max-height: 200px; }
  to { opacity: 0; transform: translateX(30px); max-height: 0; margin: 0; padding: 0; }
}

/* ===== 加载更多 ===== */
.load-more {
  text-align: center;
  padding: 20px 0;
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

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 0;
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

/* ===== 空状态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
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
  animation: floatBreathe 3s ease-in-out infinite;
}

@keyframes floatBreathe {
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
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--text-muted);
}

.empty-action {
  padding: 10px 28px;
  border: none;
  background: linear-gradient(135deg, var(--orange-main), var(--orange-deep));
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.3);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.empty-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 18px rgba(255, 140, 66, 0.4);
}

/* ===== 删除弹窗 ===== */
.delete-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.delete-warning {
  font-size: 14px;
  color: var(--text-body);
  margin: 0;
}

.delete-preview {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
  padding: 12px;
  background: var(--bg-elevated);
  border-radius: 10px;
  line-height: 1.6;
  word-break: break-all;
}

/* ===== 响应式 ===== */
@media (max-width: 600px) {
  .top-bar {
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 17px;
  }

  .tab-btn {
    font-size: 13px;
    padding: 9px 12px;
  }

  .card-main {
    padding: 14px 16px;
  }

  .delete-btn {
    width: 40px;
  }
}
</style>
