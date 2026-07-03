<template>
  <!-- AI 加载状态通用组件：SVG 轨道环动画 + 阶段指示器 + 轮播文案 -->
  <div class="ai-loading-state" :class="{ 'no-card': noCard }">
    <!-- SVG 轨道环动画 -->
    <div class="animation-area">
      <FeatureIcon name="ai-loading" size="lg" class="ai-loading-feature-icon" />
      <svg class="orbit-svg" width="120" height="120" viewBox="0 0 120 120">
        <!-- 外环：虚线圆，顺时针慢转 -->
        <circle
          class="orbit-outer"
          cx="60" cy="60" r="52"
          fill="none"
          :stroke="orbitOuterColor"
          stroke-width="2"
          stroke-dasharray="6 4"
        />
        <!-- 中环：实线弧段，逆时针转 -->
        <circle
          class="orbit-middle"
          cx="60" cy="60" r="42"
          fill="none"
          :stroke="orbitMiddleColor"
          stroke-width="3"
          stroke-linecap="round"
          stroke-dasharray="198 66"
        />
        <!-- 内核：呼吸缩放圆 -->
        <circle
          class="pulse-core"
          cx="60" cy="60" r="12"
          :fill="coreFillColor"
        />
        <!-- 内核发光 -->
        <circle
          class="pulse-glow"
          cx="60" cy="60" r="18"
          :fill="glowFillColor"
        />
      </svg>
    </div>

    <!-- 标题 -->
    <div class="loading-title">{{ title }}</div>

    <!-- 阶段指示器 -->
    <div class="stages-bar" v-if="stages.length">
      <div
        v-for="(stage, idx) in stages"
        :key="stage.key"
        class="stage-item"
        :class="{ active: idx === currentStageIndex, completed: idx < currentStageIndex }"
      >
        <div class="stage-dot">
          <FeatureIcon v-if="idx < currentStageIndex" name="success" size="xs" />
        </div>
        <span class="stage-label">{{ stage.label }}</span>
        <div v-if="idx < stages.length - 1" class="stage-line"></div>
      </div>
    </div>

    <!-- 轮播文案 -->
    <div class="message-area" v-if="messages.length">
      <transition name="msg-fade" mode="out-in">
        <p class="rotating-message" :key="currentMessageIndex">
          {{ messages[currentMessageIndex] }}
        </p>
      </transition>
    </div>

    <!-- 已等待时间 -->
    <div class="elapsed-time" v-if="showElapsedTime">
      已等待 {{ formattedElapsed }}
    </div>

    <!-- 操作按钮区 -->
    <div class="action-area" v-if="showRefreshButton || $slots.actions">
      <el-button v-if="showRefreshButton" type="primary" size="small" :loading="refreshLoading" @click="$emit('refresh')">
        {{ refreshLoading ? '刷新中...' : '手动刷新' }}
      </el-button>
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import FeatureIcon from '@/components/common/FeatureIcon.vue'

/**
 * AI 加载状态通用组件
 * 用于简历诊断等待、面试报告生成等待等需要长时间等待的场景
 * 特性：SVG 轨道环动画、多阶段进度指示、轮播鼓励文案、计时器、进度条
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  /** 主标题 */
  title: { type: String, default: 'AI 正在处理中' },
  /** 阶段列表 [{ key, label }] */
  stages: { type: Array, default: () => [] },
  /** 当前阶段索引（0-based） */
  currentStageIndex: { type: Number, default: 0 },
  /** 轮播鼓励文案 */
  messages: { type: Array, default: () => [] },
  /** 是否显示已等待时间 */
  showElapsedTime: { type: Boolean, default: true },
  /** 是否显示手动刷新按钮 */
  showRefreshButton: { type: Boolean, default: false },
  /** 刷新按钮加载状态 */
  refreshLoading: { type: Boolean, default: false },
  /** 无卡片模式：去掉背景、边框、阴影，适合全页居中场景 */
  noCard: { type: Boolean, default: false }
})

defineEmits(['refresh'])

// ---- 动画颜色（通过 CSS 变量适配暗色模式） ----
const orbitOuterColor = 'var(--orange-border)'
const orbitMiddleColor = 'var(--orange-main)'
const coreFillColor = 'var(--orange-main)'
const glowFillColor = 'var(--orange-light-bg)'

// ---- 轮播文案 ----
const currentMessageIndex = ref(0)
let messageTimer = null

function startMessageRotation() {
  if (props.messages.length <= 1) return
  messageTimer = setInterval(() => {
    currentMessageIndex.value = (currentMessageIndex.value + 1) % props.messages.length
  }, 4000)
}

// 当 messages 变化时重置
watch(() => props.messages, () => {
  currentMessageIndex.value = 0
  clearInterval(messageTimer)
  startMessageRotation()
}, { immediate: false })

// ---- 已等待时间计时器 ----
const elapsedSeconds = ref(0)
let elapsedTimer = null
const startTime = Date.now()

const formattedElapsed = computed(() => {
  const m = Math.floor(elapsedSeconds.value / 60)
  const s = elapsedSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

// ---- 减少动画偏好检测 ----
const reducedMotion = ref(false)
let motionQuery = null

onMounted(() => {
  // 启动轮播
  startMessageRotation()

  // 启动计时器（用 Date.now 差值避免 setInterval 漂移）
  elapsedTimer = setInterval(() => {
    elapsedSeconds.value = Math.floor((Date.now() - startTime) / 1000)
  }, 1000)

  // 检测 prefers-reduced-motion
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion.value = motionQuery.matches
  motionQuery.addEventListener('change', onMotionChange)
})

onUnmounted(() => {
  clearInterval(messageTimer)
  clearInterval(elapsedTimer)
  if (motionQuery) {
    motionQuery.removeEventListener('change', onMotionChange)
  }
})

function onMotionChange(e) {
  reducedMotion.value = e.matches
}
</script>

<style scoped>
/* ============================================
   容器
   ============================================ */
.ai-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 32px 40px;
  background: var(--bg-card);
  border-radius: 20px;
  border: 1px solid var(--border-card);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.06);
  max-width: 560px;
  width: 100%;
}

/* 无卡片模式：去掉背景/边框/阴影，紧凑内边距 */
.ai-loading-state.no-card {
  background: transparent;
  border: none;
  box-shadow: none;
  border-radius: 0;
  max-width: none;
  padding: 0;
}

/* ============================================
   SVG 轨道环动画
   ============================================ */
.animation-area {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
}

.ai-loading-feature-icon {
  position: absolute;
  z-index: 1;
}

.orbit-svg {
  display: block;
}

/* 外环：顺时针慢转 */
.orbit-outer {
  transform-origin: 60px 60px;
  animation: orbit-cw 8s linear infinite;
}

/* 中环：逆时针转 */
.orbit-middle {
  transform-origin: 60px 60px;
  animation: orbit-ccw 5s linear infinite;
}

/* 内核：呼吸缩放 */
.pulse-core {
  transform-origin: 60px 60px;
  animation: pulse-breathe 2s ease-in-out infinite;
}

/* 内核发光：同步呼吸 */
.pulse-glow {
  transform-origin: 60px 60px;
  animation: pulse-breathe 2s ease-in-out infinite;
  opacity: 0.4;
}

@keyframes orbit-cw {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes orbit-ccw {
  from { transform: rotate(0deg); }
  to { transform: rotate(-360deg); }
}

@keyframes pulse-breathe {
  0%, 100% { transform: scale(0.85); }
  50% { transform: scale(1.05); }
}

/* 减少动画模式 */
@media (prefers-reduced-motion: reduce) {
  .orbit-outer,
  .orbit-middle {
    animation: none !important;
  }
  .pulse-core,
  .pulse-glow {
    animation: pulse-static 3s ease-in-out infinite !important;
    transform: scale(1) !important;
  }
}

@keyframes pulse-static {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* ============================================
   标题
   ============================================ */
.loading-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 24px;
  text-align: center;
}

/* ============================================
   阶段指示器
   ============================================ */
.stages-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 24px;
  flex-wrap: nowrap;
  width: 100%;
  padding: 0 8px;
}

.stage-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex-shrink: 1;
}

/* 圆点 */
.stage-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--border-card);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background-color 0.3s ease, border-color 0.3s ease, color 0.3s ease, transform 0.3s ease;
  background: var(--bg-card);
  color: var(--bg-card);
}

/* 活跃阶段：填充 + 脉冲 */
.stage-item.active .stage-dot {
  background: var(--orange-main);
  border-color: var(--orange-main);
  box-shadow: 0 0 0 4px var(--orange-light-bg);
  animation: dot-pulse 2s ease-in-out infinite;
}

/* 已完成阶段：填充 + 对勾 */
.stage-item.completed .stage-dot {
  background: var(--color-success);
  border-color: var(--color-success);
  color: #fff;
}

@keyframes dot-pulse {
  0%, 100% { box-shadow: 0 0 0 4px var(--orange-light-bg); }
  50% { box-shadow: 0 0 0 8px transparent; }
}

@media (prefers-reduced-motion: reduce) {
  .stage-item.active .stage-dot {
    animation: none !important;
  }
}

/* 阶段文字 */
.stage-label {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
  transition: color 0.3s ease;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stage-item.active .stage-label {
  color: var(--orange-main);
  font-weight: 500;
}

.stage-item.completed .stage-label {
  color: var(--text-body);
}

/* 连接线 */
.stage-line {
  flex: 1;
  min-width: 12px;
  max-width: 40px;
  height: 2px;
  background: var(--border-card);
  margin: 0 4px;
  flex-shrink: 1;
  transition: background 0.3s ease;
}

.stage-item.completed .stage-line {
  background: var(--color-success);
}

/* ============================================
   轮播文案
   ============================================ */
.message-area {
  min-height: 24px;
  margin-bottom: 20px;
  text-align: center;
}

.rotating-message {
  font-size: 14px;
  color: var(--text-body);
  line-height: 1.6;
}

/* 交叉淡入淡出 */
.msg-fade-enter-active,
.msg-fade-leave-active {
  transition: opacity 0.3s ease;
}

.msg-fade-enter-from,
.msg-fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .msg-fade-enter-active,
  .msg-fade-leave-active {
    transition: none !important;
  }
}

/* ============================================
   已等待时间
   ============================================ */
.elapsed-time {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

/* ============================================
   刷新按钮
   ============================================ */
.action-area {
  margin-top: 4px;
  display: flex;
  gap: 12px;
  align-items: center;
}

/* ============================================
   响应式适配
   ============================================ */
@media (max-width: 480px) {
  .ai-loading-state {
    padding: 32px 16px 28px;
    max-width: 100%;
    border-radius: 16px;
  }

  .animation-area {
    margin-bottom: 20px;
  }

  .orbit-svg {
    width: 88px;
    height: 88px;
  }

  .loading-title {
    font-size: 17px;
    margin-bottom: 18px;
  }

  .stage-dot {
    width: 20px;
    height: 20px;
  }

  .stage-label {
    font-size: 12px;
  }

  .stage-line {
    min-width: 8px;
    max-width: 24px;
    margin: 0 2px;
  }

  .stage-item {
    gap: 4px;
  }

  .message-area {
    margin-bottom: 14px;
  }

  .rotating-message {
    font-size: 13px;
  }
}
</style>
