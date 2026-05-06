<template>
  <!-- AI 加载状态通用组件：SVG 轨道环动画 + 阶段指示器 + 轮播文案 + 进度条 -->
  <div class="ai-loading-state" :class="{ 'no-card': noCard }">
    <!-- SVG 轨道环动画 -->
    <div class="animation-area">
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
          <svg v-if="idx < currentStageIndex" width="12" height="12" viewBox="0 0 12 12">
            <polyline points="2.5,6 5,8.5 9.5,3.5" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
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

    <!-- 进度条（仅传入 progressPercent 时显示） -->
    <div class="progress-section" v-if="showProgress">
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
      </div>
      <span class="progress-label">{{ Math.round(progressPercent) }}%</span>
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
  /** 进度百分比 0-100，不传则不显示进度条 */
  progressPercent: { type: Number, default: -1 },
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

// 是否显示进度条
const showProgress = computed(() => props.progressPercent >= 0)

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
  max-width: 480px;
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
  margin-bottom: 28px;
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
  flex-wrap: wrap;
}

.stage-item {
  display: flex;
  align-items: center;
  gap: 6px;
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
  transition: all 0.3s ease;
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
  width: 32px;
  height: 2px;
  background: var(--border-card);
  margin: 0 8px;
  flex-shrink: 0;
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
   进度条
   ============================================ */
.progress-section {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: var(--border-card);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--orange-main) 0%, var(--orange-deep) 100%);
  border-radius: 3px;
  transition: width 0.6s cubic-bezier(0.25, 1, 0.5, 1);
  position: relative;
}

/* 进度条微光效果 */
.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 100%
  );
  animation: progress-shimmer 2s ease-in-out infinite;
}

@keyframes progress-shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@media (prefers-reduced-motion: reduce) {
  .progress-fill::after {
    animation: none !important;
  }
}

.progress-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--orange-main);
  min-width: 36px;
  text-align: right;
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
</style>
