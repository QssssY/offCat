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
      <p class="post-summary">{{ post.content }}</p>
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
        <FeatureIcon :name="post.liked ? 'liked' : 'favorite'" size="xs" />
        <span>{{ post.likeCount || 0 }}</span>
      </button>
      <button class="action-btn" @click.stop="$emit('click')">
        <FeatureIcon name="comment" size="xs" />
        <span>{{ post.commentCount || 0 }}</span>
      </button>
      <button
        class="action-btn"
        :class="{ favorited: post.favorited }"
        @click.stop="$emit('favorite')"
      >
        <FeatureIcon name="favorite" size="xs" />
        <span>收藏</span>
      </button>
      <button class="action-btn" @click.stop="$emit('share')">
        <FeatureIcon name="share" size="xs" />
        <span>分享</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import ImageGrid from './ImageGrid.vue'
import { optimizedImages } from '@/utils/optimizedImages'
import { formatTime, categoryLabel } from '@/utils/community'

const defaultAvatar = optimizedImages.userAvatar.webp

defineProps({
  post: {
    type: Object,
    required: true
  }
})

defineEmits(['click', 'like', 'favorite', 'share'])
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
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

.post-summary {
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.75;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  letter-spacing: 0.2px;
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
  gap: 5px;
  padding: 6px 14px;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.action-btn:hover {
  background: var(--orange-light-bg);
  color: var(--orange-main);
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
  color: #f5a623;
}

.action-btn.favorited:hover {
  color: #f5a623;
  background: rgba(245, 166, 35, 0.08);
}

.action-btn svg {
  width: 16px;
  height: 16px;
}
</style>
