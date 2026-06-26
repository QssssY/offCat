<template>
  <!-- 【帖子卡片】左侧带板块色条的卡片容器 -->
  <div class="post-card" :class="`card-${post.category}`" @click="$emit('click')">
    <!-- 【左侧色条】面试经验=橙色，内推=绿色 -->
    <div class="card-accent"></div>
    <!-- 头部：头像、昵称、时间、板块标签 -->
    <div class="card-header">
      <div class="author-info">
        <div class="avatar-ring-sm">
          <img :src="post.authorAvatar || defaultAvatar" class="avatar-img-sm" />
        </div>
        <div class="author-meta">
          <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
        </div>
      </div>
      <!-- 【板块标签】带圆点指示器 -->
      <span class="category-tag" :class="post.category">
        <span class="tag-dot"></span>
        {{ categoryLabel(post.category) }}
      </span>
    </div>

    <!-- 内容摘要 -->
    <div class="card-body">
      <h2 v-if="displayTitle" class="post-title" :title="displayTitle">{{ displayTitle }}</h2>
      <p class="post-summary" :class="{ collapsed: shouldCollapseContent && !contentExpanded }">{{ post.content }}</p>
      <button
        v-if="shouldCollapseContent"
        type="button"
        class="content-toggle"
        @click.stop="contentExpanded = !contentExpanded"
      >
        {{ contentExpanded ? '收起' : '展开' }}
      </button>
      <a
        v-if="post.sharedInterviewSessionId"
        class="report-link-card"
        :href="`/interview/report/${post.sharedInterviewSessionId}`"
        @click.stop
      >
        <FeatureIcon name="interview-report" size="sm" />
        <span class="report-link-main">查看完整面试报告</span>
        <span class="report-link-title">{{ displayTitle }}</span>
      </a>
      <!-- 图片缩略图 -->
      <ImageGrid
        v-if="post.images && post.images.length > 0"
        :images="post.images"
        class="card-images"
      />
    </div>

    <!-- 底部互动栏 -->
    <div class="card-footer">
      <button
        class="action-btn"
        :class="{ liked: post.liked }"
        @click.stop="$emit('like')"
      >
        <FeatureIcon :name="post.liked ? 'liked' : 'favorite'" size="sm" />
        <span>{{ post.likeCount || 0 }}</span>
      </button>
      <button class="action-btn" @click.stop="$emit('click')">
        <FeatureIcon name="comment" size="sm" />
        <span>{{ post.commentCount || 0 }}</span>
      </button>
      <button
        class="action-btn"
        :class="{ favorited: post.favorited }"
        @click.stop="$emit('favorite')"
      >
        <FeatureIcon name="favorite" size="sm" />
        <span>收藏</span>
      </button>
      <button class="action-btn" @click.stop="$emit('share')">
        <FeatureIcon name="share" size="sm" />
        <span>分享</span>
      </button>
      <button
        v-if="canAdminHide"
        class="action-btn admin-hide-btn"
        @click.stop="$emit('admin-hide')"
      >
        <FeatureIcon name="delete" size="sm" />
        <span>下架</span>
      </button>
      <button
        v-if="canAdminBan"
        class="action-btn admin-ban-btn"
        @click.stop="$emit('admin-ban-user', post)"
      >
        <FeatureIcon name="warning" size="sm" />
        <span>封禁</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import ImageGrid from './ImageGrid.vue'
import { optimizedImages } from '@/utils/optimizedImages'
import { formatTime, categoryLabel } from '@/utils/community'

const defaultAvatar = optimizedImages.userAvatar.webp

const props = defineProps({
  post: {
    type: Object,
    required: true
  },
  canAdminHide: {
    type: Boolean,
    default: false
  },
  canAdminBan: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click', 'like', 'favorite', 'share', 'admin-hide', 'admin-ban-user'])

const contentExpanded = ref(false)

// 旧报告分享帖可能没有 title 字段，前端展示层兜底，避免用户看到无标题内容。
const displayTitle = computed(() => {
  const title = props.post.title?.trim()
  if (title) return title
  return props.post.sharedInterviewSessionId ? '面试报告分享' : ''
})

// 列表卡片只展示摘要，超过阈值时交给用户主动展开，避免长文本撑高信息流。
const shouldCollapseContent = computed(() => (props.post.content || '').length > 220)
</script>

<style scoped>
/* ===== 帖子卡片样式（UI美化版 v2） ===== */

/* 【卡片主体】左侧色条 + 悬停上浮 + 阴影加深 */
.post-card {
  position: relative;
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px 20px 20px 24px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-card);
  border-left: none;
  cursor: pointer;
  transition:
    transform 0.24s cubic-bezier(0.25, 1, 0.5, 1),
    box-shadow 0.24s cubic-bezier(0.25, 1, 0.5, 1),
    border-color 0.24s cubic-bezier(0.25, 1, 0.5, 1);
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.card-header,
.card-body,
.post-title,
.post-summary,
.author-name,
.post-time,
.category-tag {
  cursor: default;
}

/* 【左侧色条】根据板块类型显示不同颜色 */
.card-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 16px 0 0 16px;
  transition: width 0.3s ease;
}

.post-card:hover .card-accent {
  width: 5px;
}

/* 面试经验 = 橙色色条 */
.card-interview_exp .card-accent {
  background: linear-gradient(180deg, var(--orange-main), var(--orange-deep));
}

/* 内推 = 绿色色条 */
.card-referral .card-accent {
  background: linear-gradient(180deg, var(--color-success), #529b2e);
}

/* 面试经验卡片悬停边框 */
.card-interview_exp:hover {
  border-color: var(--orange-border);
}

/* 内推卡片悬停边框 */
.card-referral:hover {
  border-color: rgba(103, 194, 58, 0.4);
}

/* 【卡片头部】头像 + 作者信息 + 板块标签 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 【头像圆环】品牌色柔和光晕，悬停时增强 */
.avatar-ring-sm {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.3), 0 2px 8px rgba(255, 140, 66, 0.2);
  flex-shrink: 0;
  transition: box-shadow 0.3s ease;
}

.post-card:hover .avatar-ring-sm {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.3), 0 2px 12px rgba(255, 140, 66, 0.35);
}

.avatar-img-sm {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.author-meta {
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

/* 【板块标签】带圆点指示器的胶囊标签 */
.category-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

/* 【标签圆点】4px圆形色块 */
.tag-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.category-tag.interview_exp {
  background: rgba(255, 140, 66, 0.1);
  color: var(--orange-main);
}

.category-tag.interview_exp .tag-dot {
  background: var(--orange-main);
}

.category-tag.referral {
  background: rgba(103, 194, 58, 0.1);
  color: var(--color-success);
}

.category-tag.referral .tag-dot {
  background: var(--color-success);
}

/* 【卡片正文】内容摘要区域 */
.card-body {
  margin-bottom: 14px;
}

.post-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0 0 8px;
  color: var(--text-title);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-word;
}

.post-summary {
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.75;
  margin: 0 0 8px;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-summary.collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.content-toggle {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  margin: 0 0 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--orange-main);
  font-size: 13px;
  font-weight: 700;
}

.content-toggle:hover {
  color: var(--orange-deep);
}

.report-link-card {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 10px 12px;
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
  font-size: 13px;
  font-weight: 700;
}

.report-link-title {
  min-width: 0;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-images {
  margin-top: 8px;
}

/* 【底部互动栏】分隔线 + 操作按钮 */
.card-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  border-top: 1px solid var(--border-divider);
  padding-top: 12px;
}

/* 【操作按钮】胶囊形悬停效果 */
.action-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 44px;
  padding: 8px 14px;
  border: 1px solid transparent;
  background: none;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: 999px;
  transition:
    transform 0.16s cubic-bezier(0.25, 1, 0.5, 1),
    color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    background-color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    border-color 0.2s cubic-bezier(0.25, 1, 0.5, 1),
    box-shadow 0.2s cubic-bezier(0.25, 1, 0.5, 1);
}

.action-btn:hover {
  background: var(--orange-light-bg);
  color: var(--orange-main);
  transform: translateY(-1px);
}

.action-btn:active {
  transform: translateY(0) scale(0.97);
}

.action-btn:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

/* 【已点赞状态】红色高亮 */
.action-btn.liked {
  color: #ff4d6a;
}

.action-btn.liked:hover {
  color: #ff4d6a;
  background: rgba(255, 77, 106, 0.08);
}

/* 【已收藏状态】金色高亮 */
.action-btn.favorited {
  color: #b45309;
  background: color-mix(in srgb, var(--bg-card) 68%, #f5a623 32%);
  border-color: color-mix(in srgb, var(--border-card) 45%, #f5a623 55%);
  box-shadow: 0 8px 20px rgba(245, 166, 35, 0.16);
  font-weight: 700;
}

.action-btn.favorited:hover {
  color: #92400e;
  background: color-mix(in srgb, var(--bg-card) 58%, #f5a623 42%);
  border-color: #f5a623;
  box-shadow: 0 10px 24px rgba(245, 166, 35, 0.22);
}

.admin-hide-btn {
  margin-left: auto;
  color: var(--color-danger);
}

.admin-hide-btn:hover {
  background: rgba(245, 63, 63, 0.08);
  color: var(--color-danger);
}

.admin-ban-btn {
  color: #b42318;
}

.admin-ban-btn:hover {
  background: rgba(180, 35, 24, 0.08);
  color: #b42318;
}

.action-btn svg {
  width: 22px;
  height: 22px;
}

.action-btn.favorited svg {
  filter: drop-shadow(0 4px 8px rgba(245, 166, 35, 0.24));
}

@media (prefers-reduced-motion: reduce) {
  .post-card,
  .card-accent,
  .avatar-ring-sm,
  .report-link-card,
  .action-btn,
  .content-toggle {
    transition-duration: 0.01ms;
  }

  .post-card:hover,
  .report-link-card:hover,
  .action-btn:hover,
  .action-btn:active {
    transform: none;
  }
}
</style>
