<template>
  <div class="image-grid" :class="[attrs.class, `grid-count-${Math.min(images.length, 9)}`]">
    <div
      v-for="(img, index) in displayImages"
      :key="index"
      class="grid-item"
      @click.stop="openPreview(index)"
    >
      <img :src="img" :alt="`图片${index + 1}`" loading="lazy" @error="onImgError" />
      <div class="hover-overlay">
        <FeatureIcon name="search" size="sm" class="zoom-icon" />
      </div>
      <div v-if="index === 8 && images.length > 9" class="more-overlay">
        +{{ images.length - 9 }}
      </div>
    </div>
  </div>

  <!-- 自定义图片预览（不使用 el-image-viewer，避免 focus-trap 与 Teleport 清理副作用） -->
  <div v-if="viewerVisible" class="community-viewer" @click.self="closePreview">
        <!-- 关闭按钮 -->
        <button class="community-viewer-close" @click="closePreview">
          <FeatureIcon name="close" size="sm" />
        </button>
        <!-- 左箭头 -->
        <button v-if="images.length > 1" class="viewer-arrow community-viewer-arrow community-viewer-prev" @click.stop="prevImage">
          <FeatureIcon name="previous" size="sm" />
        </button>
        <!-- 图片 -->
        <div class="community-viewer-canvas" @click.self="closePreview">
          <img :src="images[viewerIndex]" class="community-viewer-img" draggable="false" />
        </div>
        <!-- 右箭头 -->
        <button v-if="images.length > 1" class="viewer-arrow community-viewer-arrow community-viewer-next" @click.stop="nextImage">
          <FeatureIcon name="next" size="sm" />
        </button>
        <!-- 计数器 -->
        <div v-if="images.length > 1" class="community-viewer-counter">
          {{ viewerIndex + 1 }} / {{ images.length }}
        </div>
  </div>
</template>

<script setup>
import { computed, ref, onBeforeUnmount, useAttrs } from 'vue'
import FeatureIcon from '@/components/common/FeatureIcon.vue'

defineOptions({
  inheritAttrs: false
})

const attrs = useAttrs()

const props = defineProps({
  images: {
    type: Array,
    default: () => []
  }
})

const viewerVisible = ref(false)
const viewerIndex = ref(0)

const displayImages = computed(() => props.images.slice(0, 9))

const openPreview = (index) => {
  viewerIndex.value = index
  viewerVisible.value = true
  document.addEventListener('keydown', onKeydown)
}

const closePreview = () => {
  viewerVisible.value = false
  document.removeEventListener('keydown', onKeydown)
}

const prevImage = () => {
  viewerIndex.value = (viewerIndex.value - 1 + props.images.length) % props.images.length
}

const nextImage = () => {
  viewerIndex.value = (viewerIndex.value + 1) % props.images.length
}

const onKeydown = (e) => {
  if (e.key === 'Escape') closePreview()
  else if (e.key === 'ArrowLeft') prevImage()
  else if (e.key === 'ArrowRight') nextImage()
}

const onImgError = (e) => {
  e.target.style.display = 'none'
}

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ===== 图片网格样式（UI美化版 v2） ===== */

.image-grid {
  display: grid;
  gap: 6px;
  border-radius: 10px;
  overflow: hidden;
  max-width: 360px;
}

/* 【网格项】圆角图片卡片，悬停时显示放大镜遮罩 */
.grid-item {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border-radius: 8px;
  background: var(--bg-elevated);
  aspect-ratio: 1;
}

.grid-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.grid-item:hover img {
  transform: scale(1.08);
}

/* 【悬停放大遮罩】半透明渐变遮罩 + 放大镜图标 */
.hover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(0, 0, 0, 0.35) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.grid-item:hover .hover-overlay {
  opacity: 1;
}

.zoom-icon {
  width: 28px;
  height: 28px;
  color: #fff;
  filter: drop-shadow(0 1px 4px rgba(0, 0, 0, 0.3));
  transform: scale(0.8);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.grid-item:hover .zoom-icon {
  transform: scale(1);
}

/* 【更多遮罩】半透明黑色覆盖层 */
.more-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

/* 【响应式网格】根据图片数量自适应列数 */
.grid-count-1 {
  grid-template-columns: 1fr;
  max-width: 280px;
}

.grid-count-2 {
  grid-template-columns: 1fr 1fr;
  max-width: 280px;
}

.grid-count-3 {
  grid-template-columns: 1fr 1fr 1fr;
  max-width: 360px;
}

.grid-count-4 {
  grid-template-columns: 1fr 1fr;
  max-width: 240px;
}

.grid-count-5,
.grid-count-6 {
  grid-template-columns: 1fr 1fr 1fr;
  max-width: 360px;
}

.grid-count-7,
.grid-count-8,
.grid-count-9 {
  grid-template-columns: 1fr 1fr 1fr;
  max-width: 360px;
}
</style>

<style>
/* 自定义图片预览样式（非 scoped，因为 Teleport 到 body） */
.community-viewer {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  user-select: none;
}

.community-viewer-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.community-viewer-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

.community-viewer-close svg {
  width: 22px;
  height: 22px;
}

.community-viewer-canvas {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 60px;
}

.community-viewer-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
}

.community-viewer-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.community-viewer-arrow:hover {
  background: rgba(255, 255, 255, 0.3);
}

.community-viewer-arrow svg {
  width: 22px;
  height: 22px;
}

.community-viewer-prev {
  left: 16px;
}

.community-viewer-next {
  right: 16px;
}

.community-viewer-counter {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px 14px;
  border-radius: 12px;
}

.community-viewer-fade-enter-active,
.community-viewer-fade-leave-active {
  transition: opacity 0.25s ease;
}

.community-viewer-fade-enter-from,
.community-viewer-fade-leave-to {
  opacity: 0;
}
</style>
