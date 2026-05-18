<template>
  <Teleport to="body">
    <Transition name="onboarding-fade">
      <div v-if="dialogVisible" class="onboarding-overlay" @click.self="skipGuide">
        <div class="onboarding-modal">
          <!-- 背景装饰 -->
          <div class="modal-bg-decoration">
            <div class="bg-circle bg-circle-1"></div>
            <div class="bg-circle bg-circle-2"></div>
            <div class="bg-circle bg-circle-3"></div>
          </div>

          <!-- 关闭按钮 -->
          <button class="close-btn" @click="skipGuide" aria-label="关闭引导">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>

          <!-- 引导内容区域 -->
          <div class="onboarding-content">
            <!-- 步骤指示器 -->
            <div class="step-indicator">
              <div
                v-for="(step, index) in steps"
                :key="index"
                class="step-dot"
                :class="{
                  active: index === currentStep,
                  completed: index < currentStep
                }"
                @click="goToStep(index)"
              >
                <div class="dot-inner">
                  <svg v-if="index < currentStep" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
              </div>
            </div>

            <!-- 步骤内容动画容器 -->
            <Transition name="step-slide" mode="out-in">
              <div :key="currentStep" class="step-body">
                <!-- 图标容器 -->
                <div class="step-icon-wrapper">
                  <div class="icon-glow"></div>
                  <!-- 安全提示：v-html 仅渲染硬编码的 SVG 常量，切勿绑定外部数据 -->
                  <div class="step-icon" v-html="steps[currentStep].iconSvg"></div>
                </div>

                <!-- 文本内容 -->
                <div class="step-text">
                  <h2 class="step-title">{{ steps[currentStep].title }}</h2>
                  <p class="step-desc">{{ steps[currentStep].description }}</p>
                </div>
              </div>
            </Transition>
          </div>

          <!-- 底部按钮区域 -->
          <div class="onboarding-footer">
            <button
              v-if="currentStep > 0"
              @click="prevStep"
              class="btn btn-ghost"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              上一步
            </button>
            <div v-else class="footer-placeholder"></div>

            <div class="footer-right">
              <button
                @click="skipGuide"
                class="btn btn-ghost btn-skip"
                :disabled="loading"
              >
                跳过引导
              </button>
              <button
                @click="primaryAction"
                class="btn btn-primary"
                :disabled="loading"
              >
                <span v-if="loading" class="btn-loader"></span>
                <span>{{ primaryButtonText }}</span>
                <svg v-if="!loading" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                  <polyline points="12 5 19 12 12 19"></polyline>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { updateOnboardingStatus } from '@/api/onboarding'

// 引导版本标识
const GUIDE_KEY = 'v1_3_main_onboarding'

// Lucide 图标 SVG
const icons = {
  sparkles: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594zM20 2v4m2-2h-4"/><circle cx="4" cy="20" r="2"/></g></svg>',
  fileUp: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"/><path d="M14 2v5a1 1 0 0 0 1 1h5m-8 4v6m3-3l-3-3l-3 3"/></g></svg>',
  target: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></g></svg>',
  wand: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="m21.64 3.64l-1.28-1.28a1.21 1.21 0 0 0-1.72 0L2.36 18.64a1.21 1.21 0 0 0 0 1.72l1.28 1.28a1.2 1.2 0 0 0 1.72 0L21.64 5.36a1.2 1.2 0 0 0 0-1.72M14 7l3 3M5 6v4m14 4v4M10 2v2M7 8H3m18 8h-4M11 3H9"/></svg>',
  mic: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M12 19v3m7-12v2a7 7 0 0 1-14 0v-2"/><rect width="6" height="13" x="9" y="2" rx="3"/></g></svg>',
  history: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M3 12a9 9 0 1 0 9-9a9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5m4-1v5l4 2"/></g></svg>',
  trendingUp: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M16 7h6v6"/><path d="m22 7l-8.5 8.5l-5-5L2 17"/></g></svg>',
  rocket: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M12 15v5s3.03-.55 4-2c1.08-1.62 0-5 0-5M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09"/><path d="M9 12a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.4 22.4 0 0 1-4 2z"/><path d="M9 12H4s.55-3.03 2-4c1.62-1.08 5 .05 5 .05"/></g></svg>',
  bell: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/><path d="M20 3v4m2-2h-4"/></g></svg>',
  crown: '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M2 17l2-11l4 5l4-8l4 8l4-5l2 11z"/><path d="M2 17h20v2a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1z"/></g></svg>'
}

// 引导步骤定义
const steps = [
  {
    title: '欢迎使用智能简历诊断系统',
    description: '接下来我们将带您快速了解系统的核心功能，帮助您高效准备求职。',
    iconSvg: icons.sparkles
  },
  {
    title: '上传简历，获取诊断报告',
    description: '上传您的简历 PDF，AI 将从多个维度进行分析，给出详细的诊断报告和优化建议。',
    iconSvg: icons.fileUp
  },
  {
    title: '输入目标岗位 JD',
    description: '粘贴目标职位的 JD 描述，系统将自动分析您的简历与岗位的匹配程度。',
    iconSvg: icons.target
  },
  {
    title: 'AI 简历润色',
    description: '基于诊断结果和 JD 匹配分析，AI 将帮您优化简历内容，提升竞争力。',
    iconSvg: icons.wand
  },
  {
    title: '模拟面试练习',
    description: '选择目标岗位，开始 AI 模拟面试。支持普通和压力面试两种模式。',
    iconSvg: icons.mic
  },
  {
    title: '查看历史记录',
    description: '随时查看简历诊断和模拟面试的历史记录，跟踪您的进步。',
    iconSvg: icons.history
  },
  {
    title: '消息通知',
    description: '实时接收诊断结果、面试反馈和系统通知，支持批量管理和删除。',
    iconSvg: icons.bell
  },
  {
    title: '个人成长中心',
    description: '查看你的成长轨迹与个性化建议，了解简历分数和面试表现的变化趋势。',
    iconSvg: icons.trendingUp
  },
  {
    title: '会员权益',
    description: '升级会员解锁更多诊断次数、每日面试额度和专属功能，助力求职提速。',
    iconSvg: icons.crown
  },
  {
    title: '准备就绪！',
    description: '您已了解所有核心功能，现在开始体验吧！',
    iconSvg: icons.rocket
  }
]

// Props：控制弹窗显示/隐藏
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

// 事件：更新 visible 状态
const emit = defineEmits(['update:visible'])

// 当前步骤索引
const currentStep = ref(0)
// 加载状态
const loading = ref(false)

// 弹窗可见性双向绑定
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 主按钮文本：第一步"开始体验"，中间"下一步"，最后"完成"
const primaryButtonText = computed(() => {
  if (currentStep.value === 0) return '开始体验'
  if (currentStep.value === steps.length - 1) return '完成'
  return '下一步'
})

// 监听弹窗打开，重置步骤到初始位置
watch(() => props.visible, (newVal) => {
  if (newVal) {
    currentStep.value = 0
  }
})

/**
 * 跳转到指定步骤（只能跳转到已完成的步骤）
 */
function goToStep(index) {
  if (index < currentStep.value) {
    currentStep.value = index
  }
}

/**
 * 主按钮点击事件
 * 根据当前步骤决定是前进还是完成
 */
async function primaryAction() {
  if (currentStep.value === steps.length - 1) {
    // 最后一步：完成引导
    await completeGuide()
  } else {
    // 其他步骤：前进到下一步
    await nextStep()
  }
}

/**
 * 前进到下一步
 * 调用 API 保存当前进度
 */
async function nextStep() {
  const nextIndex = currentStep.value + 1
  loading.value = true
  try {
    await updateOnboardingStatus({
      guideKey: GUIDE_KEY,
      status: 'in_progress',
      currentStep: nextIndex
    })
    currentStep.value = nextIndex
  } catch (err) {
    // API 失败不阻塞用户操作，仍然允许前进
    currentStep.value = nextIndex
  } finally {
    loading.value = false
  }
}

/**
 * 返回上一步
 * 仅本地操作，不调用 API
 */
function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/**
 * 跳过引导
 * 调用 API 记录跳过状态并关闭弹窗
 */
async function skipGuide() {
  loading.value = true
  try {
    await updateOnboardingStatus({
      guideKey: GUIDE_KEY,
      status: 'skipped'
    })
  } catch (err) {
  } finally {
    loading.value = false
    dialogVisible.value = false
  }
}

/**
 * 完成引导
 * 调用 API 记录完成状态并关闭弹窗
 */
async function completeGuide() {
  loading.value = true
  try {
    await updateOnboardingStatus({
      guideKey: GUIDE_KEY,
      status: 'completed'
    })
  } catch (err) {
  } finally {
    loading.value = false
    dialogVisible.value = false
  }
}
</script>

<style scoped>
/* ==================== 遮罩层 ==================== */
.onboarding-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

/* ==================== 弹窗主体 ==================== */
.onboarding-modal {
  position: relative;
  width: 100%;
  max-width: 520px;
  background: var(--bg-card, rgba(255, 255, 255, 0.85));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.2) inset;
  overflow: hidden;
}

/* ==================== 背景装饰 ==================== */
.modal-bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.3;
}

.bg-circle-1 {
  width: 200px;
  height: 200px;
  top: -60px;
  right: -40px;
  background: linear-gradient(135deg, #FF8C42 0%, #FFB07A 100%);
}

.bg-circle-2 {
  width: 160px;
  height: 160px;
  bottom: -40px;
  left: -30px;
  background: linear-gradient(135deg, #FF8C42 0%, #E67A35 100%);
}

.bg-circle-3 {
  width: 120px;
  height: 120px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: linear-gradient(135deg, #FFB07A 0%, #FFC9A6 100%);
}

/* ==================== 关闭按钮 ==================== */
.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card, rgba(255, 255, 255, 0.6));
  border: 1px solid var(--border-card, rgba(255, 255, 255, 0.8));
  border-radius: 10px;
  color: var(--text-body, #666);
  cursor: pointer;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: var(--bg-card, rgba(255, 255, 255, 0.9));
  color: var(--text-title, #333);
  transform: scale(1.05);
}

/* ==================== 内容区域 ==================== */
.onboarding-content {
  position: relative;
  padding: 48px 40px 32px;
  text-align: center;
}

/* ==================== 步骤指示器 ==================== */
.step-indicator {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 40px;
}

.step-dot {
  width: 32px;
  height: 6px;
  border-radius: 3px;
  background: var(--border-divider, rgba(0, 0, 0, 0.08));
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.step-dot:hover {
  background: rgba(0, 0, 0, 0.12);
}

.step-dot.active {
  width: 48px;
  background: linear-gradient(90deg, #FF8C42 0%, #E67A35 100%);
}

.step-dot.completed {
  background: linear-gradient(90deg, #FF8C42 0%, #FFB07A 100%);
  opacity: 0.6;
}

.dot-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: white;
}

/* ==================== 步骤内容 ==================== */
.step-body {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 图标容器 */
.step-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 28px;
}

.icon-glow {
  position: absolute;
  inset: -10px;
  background: linear-gradient(135deg, #FF8C42 0%, #FFB07A 100%);
  border-radius: 24px;
  opacity: 0.15;
  filter: blur(20px);
  animation: pulse-glow 3s ease-in-out infinite;
}

.step-icon {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card, rgba(255, 255, 255, 0.8));
  border: 1px solid var(--border-card, rgba(255, 255, 255, 0.9));
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  color: var(--orange-main, #FF8C42);
}

.step-icon :deep(svg) {
  width: 36px;
  height: 36px;
}

/* 文本内容 */
.step-text {
  max-width: 380px;
}

.step-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title, #1a1a2e);
  margin: 0 0 12px;
  line-height: 1.4;
  letter-spacing: -0.02em;
}

.step-desc {
  font-size: 15px;
  color: var(--text-body, #64748b);
  margin: 0;
  line-height: 1.7;
}

/* ==================== 底部按钮区域 ==================== */
.onboarding-footer {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px 24px;
  background: var(--bg-card, rgba(255, 255, 255, 0.5));
  border-top: 1px solid var(--border-divider, rgba(0, 0, 0, 0.04));
}

.footer-placeholder {
  width: 1px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ==================== 按钮样式 ==================== */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-ghost {
  background: transparent;
  color: var(--text-body, #64748b);
}

.btn-ghost:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.04);
  color: var(--text-title, #334155);
}

.btn-skip {
  color: var(--text-muted, #94a3b8);
  font-size: 13px;
  padding: 8px 12px;
}

.btn-skip:hover:not(:disabled) {
  color: var(--text-body, #64748b);
  background: transparent;
}

.btn-primary {
  background: linear-gradient(135deg, #FF8C42 0%, #E67A35 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 140, 66, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.4);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-loader {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ==================== 动画 ==================== */
@keyframes pulse-glow {
  0%, 100% {
    opacity: 0.15;
    transform: scale(1);
  }
  50% {
    opacity: 0.25;
    transform: scale(1.05);
  }
}


/* 遮罩层淡入淡出 */
.onboarding-fade-enter-active {
  transition: opacity 0.3s ease;
}

.onboarding-fade-leave-active {
  transition: opacity 0.2s ease;
}

.onboarding-fade-enter-from,
.onboarding-fade-leave-to {
  opacity: 0;
}

/* 弹窗进入动画 */
.onboarding-fade-enter-active .onboarding-modal {
  animation: modal-in 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 步骤切换动画 */
.step-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.step-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.step-slide-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.step-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* ==================== 移动端适配 ==================== */
@media (max-width: 768px) {
  .onboarding-modal {
    max-width: 100%;
    border-radius: 20px;
  }

  .onboarding-content {
    padding: 36px 24px 24px;
  }

  .step-icon-wrapper {
    width: 64px;
    height: 64px;
    margin-bottom: 20px;
  }

  .step-icon {
    border-radius: 16px;
  }

  .step-icon :deep(svg) {
    width: 28px;
    height: 28px;
  }

  .step-title {
    font-size: 20px;
  }

  .step-desc {
    font-size: 14px;
  }

  .onboarding-footer {
    padding: 16px 20px 20px;
  }

  .btn {
    padding: 8px 16px;
    font-size: 13px;
  }
}

/* Dark mode is handled by CSS variables defined in styles/index.css */

/* 尊重用户减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .onboarding-fade-enter-active,
  .onboarding-fade-leave-active,
  .step-slide-enter-active,
  .step-slide-leave-active {
    transition: none;
  }

  .onboarding-fade-enter-active .onboarding-modal {
    animation: none;
  }

  .icon-glow {
    animation: none;
  }

  .btn-primary:hover:not(:disabled) {
    transform: none;
  }
}
</style>
