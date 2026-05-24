<template>
  <MotionConfig reducedMotion="user">
  <div class="landing-page">
    <section
      class="hero-section background-hero-section"
      aria-labelledby="home-hero-title"
      :style="{ '--home-hero-bg': `url(${homeBackground})` }"
    >
      <div class="hero-cloud hero-motion-cloud cloud-one" aria-hidden="true"></div>
      <div class="hero-cloud hero-motion-cloud cloud-two" aria-hidden="true"></div>
      <div class="hero-cloud hero-motion-cloud cloud-three" aria-hidden="true"></div>
      <motion.div
        class="hero-background-art"
        aria-hidden="true"
        :initial="heroImageInitial"
        :animate="heroImageAnimate"
        :transition="heroImageTransition"
      ></motion.div>
      <motion.div
        class="hero-main motion-hero-shell"
        :variants="heroContainerVariants"
        initial="hidden"
        animate="visible"
      >
        <motion.div class="hero-badge" :variants="heroItemVariants">
          <FeatureIcon name="ai-loading" size="xs" class="badge-icon" />
          <span>AI 驱动的求职准备室</span>
        </motion.div>

        <motion.h1 id="home-hero-title" class="hero-title" :variants="heroItemVariants">
          让每一次求职准备，都有一条清晰路径
        </motion.h1>

        <motion.p class="hero-subtitle" :variants="heroItemVariants">
          从简历诊断、岗位匹配到模拟面试和 Offer 决策，offerCat 陪你把准备变成可执行的下一步。
        </motion.p>

        <motion.div class="hero-actions" :variants="heroItemVariants">
          <n-button
            type="primary"
            size="large"
            class="cta-btn primary-btn"
            @click="handleResume"
          >
            <template #icon>
            <FeatureIcon name="resume-analysis" size="md" class="btn-icon" />
            </template>
            开始简历诊断
          </n-button>
          <n-button
            size="large"
            class="cta-btn secondary-btn"
            @click="handleInterview"
          >
            <template #icon>
            <FeatureIcon name="ai-interviewer" size="md" class="btn-icon" />
            </template>
            开始模拟面试
          </n-button>
        </motion.div>

        <motion.div class="hero-stats" :variants="heroItemVariants">
          <div class="stat-item">
            <n-skeleton v-if="statsLoading" text width="64px" height="32px" />
            <span v-else class="stat-number">{{ formatCount(stats.userCount) }}</span>
            <span class="stat-text">用户使用</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <n-skeleton v-if="statsLoading" text width="64px" height="32px" />
            <span v-else class="stat-number">{{ formatCount(stats.diagnosisCount) }}</span>
            <span class="stat-text">简历诊断</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <n-skeleton v-if="statsLoading" text width="64px" height="32px" />
            <span v-else class="stat-number">{{ formatCount(stats.interviewCount) }}</span>
            <span class="stat-text">模拟面试</span>
          </div>
        </motion.div>
        <motion.div
          class="hero-quick-trails"
          :variants="heroItemVariants"
        >
          <motion.button
            v-for="item in heroHighlights"
            :key="item.title"
            type="button"
            class="hero-trail-pill"
            :whileHover="supportHover"
            :whilePress="tapFeedback"
            @click="goProtected(item.path)"
          >
            <FeatureIcon :name="item.icon" size="md" />
            {{ item.title }}
          </motion.button>
        </motion.div>
      </motion.div>
    </section>

    <motion.section
      class="home-section career-path-section"
      aria-labelledby="feature-map-title"
      :initial="revealInitial"
      :whileInView="revealAnimate"
      :inViewOptions="inViewOptions"
      :transition="sectionTransition"
    >
      <div class="section-heading">
        <span class="section-tag">求职路径</span>
        <h2 id="feature-map-title" class="section-title">沿着真实节奏，把准备一步步推进</h2>
        <p class="section-desc">
          参考背景图的“简历到 Offer”叙事，把工具入口做成一条可行动的路径，而不是一屏卡片堆叠。
        </p>
      </div>

      <div class="path-progress-track" aria-hidden="true">
        <span class="path-progress-line"></span>
      </div>

      <motion.div
        class="career-path-rail motion-path-rail"
        :variants="pathContainerVariants"
        initial="hidden"
        whileInView="visible"
        :inViewOptions="inViewOptions"
      >
        <motion.button
          v-for="feature in featureRoutes"
          :key="feature.title"
          type="button"
          class="career-path-node motion-feature-card"
          :class="`tone-${feature.tone}`"
          :variants="pathItemVariants"
          :whileHover="featureHover"
          :whilePress="tapFeedback"
          @click="goProtected(feature.path)"
        >
          <span class="path-node-index">{{ feature.index }}</span>
          <div class="route-icon">
            <FeatureIcon :name="feature.icon" size="xl" />
          </div>
          <div class="path-node-copy">
            <n-tag size="small" round :bordered="false" class="route-tag">
              {{ feature.stage }}
            </n-tag>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.desc }}</p>
          </div>
          <span class="route-link">
            {{ feature.action }}
            <FeatureIcon name="next" size="md" class="arrow-icon route-arrow" />
          </span>
        </motion.button>
      </motion.div>
    </motion.section>

    <motion.section
      class="home-section workflow-section"
      aria-labelledby="workflow-title"
      :initial="revealInitial"
      :whileInView="revealAnimate"
      :inViewOptions="inViewOptions"
      :transition="sectionTransition"
    >
      <div class="workflow-copy">
        <span class="section-tag">使用路径</span>
        <h2 id="workflow-title" class="section-title">按照真实求职节奏推进</h2>
        <p class="section-desc">
          不把功能分散成孤立入口，而是让用户知道每一步应该做什么、产出什么、下一步去哪里。
        </p>
      </div>
      <motion.div
        class="workflow-steps motion-workflow-steps"
        aria-label="求职准备流程"
        :variants="workflowContainerVariants"
        initial="hidden"
        whileInView="visible"
        :inViewOptions="inViewOptions"
      >
        <motion.div
          v-for="step in workflowSteps"
          :key="step.title"
          class="workflow-step motion-workflow-step"
          :variants="workflowStepVariants"
          :whileHover="workflowHover"
        >
          <div class="step-index" aria-hidden="true">{{ step.index }}</div>
          <div class="step-copy">
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </motion.div>
      </motion.div>
    </motion.section>

    <motion.section
      class="home-section highlights-section"
      aria-labelledby="highlights-title"
      :initial="revealInitial"
      :whileInView="revealAnimate"
      :inViewOptions="inViewOptions"
      :transition="sectionTransition"
    >
      <div class="section-heading compact-heading">
        <span class="section-tag">辅助能力</span>
        <h2 id="highlights-title" class="section-title">不抢主视觉，但随时能进入</h2>
      </div>
      <div class="support-capability-list">
        <motion.button
          v-for="item in supportCapabilities"
          :key="item.title"
          type="button"
          class="support-capability-item motion-support-item"
          :initial="revealInitial"
          :whileInView="revealAnimate"
          :inViewOptions="inViewOptions"
          :transition="supportTransition"
          :whileHover="supportHover"
          :whilePress="tapFeedback"
          @click="goProtected(item.path)"
        >
          <span class="support-icon">
            <FeatureIcon :name="item.icon" size="lg" />
          </span>
          <span class="support-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </span>
          <FeatureIcon name="next" size="md" class="support-next arrow-icon" />
        </motion.button>
      </div>
    </motion.section>

    <section class="version-section" v-if="versionLogs.length > 0">
      <div class="version-header">
        <div>
          <span class="section-tag">产品动态</span>
          <h2 class="version-title">最近更新</h2>
        </div>
        <router-link to="/version-logs" class="version-more">
          更多动态
          <FeatureIcon name="next" size="sm" class="arrow-icon" />
        </router-link>
      </div>
      <div class="version-list">
        <article
          v-for="log in versionLogs"
          :key="log.id"
          class="version-item"
        >
          <div class="version-tag-wrapper">
            <n-tag
              size="small"
              round
              :bordered="false"
              :type="getVersionTagType(log.type)"
              class="version-tag"
            >
              v{{ log.version }}
            </n-tag>
          </div>
          <div class="version-info">
            <h3 class="version-item-title" :title="log.title">{{ log.title }}</h3>
            <p class="version-item-desc">{{ truncate(log.content, 150) }}</p>
            <time class="version-item-time">{{ formatDate(log.publishedAt) }}</time>
          </div>
        </article>
      </div>
    </section>
  </div>
  </MotionConfig>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { motion, MotionConfig } from 'motion-v'
import { NButton, NSkeleton, NTag } from 'naive-ui'
import { useRouter } from 'vue-router'
import { getLatestVersionLogs } from '@/api/versionLog'
import { isLoggedIn } from '@/utils/auth'
import { getPublicStats } from '@/api/stats'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import homeBackground from '@/assets/background.png'

const router = useRouter()

const versionLogs = ref([])

const easeOut = [0.22, 1, 0.36, 1]
const revealInitial = { opacity: 0, y: 18 }
const revealAnimate = { opacity: 1, y: 0 }
const inViewOptions = { once: true, margin: '-80px' }
const panelTransition = { duration: 0.42, ease: easeOut, delay: 0.14 }
const heroImageInitial = { scale: 0.985 }
const heroImageAnimate = { scale: 1 }
const heroImageTransition = { duration: 0.72, ease: easeOut }
const sectionTransition = { duration: 0.42, ease: easeOut }
const cardTransition = { duration: 0.34, ease: easeOut }
const supportTransition = { duration: 0.3, ease: easeOut }
const tapFeedback = { scale: 0.965 }
const featureHover = { y: -6, scale: 1.015 }
const supportHover = { y: -3, scale: 1.01 }
const workflowHover = { y: -4 }

const heroContainerVariants = {
  hidden: { opacity: 1 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.08,
      delayChildren: 0.04
    }
  }
}

const heroItemVariants = {
  hidden: { opacity: 0, y: 18 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.42, ease: easeOut }
  }
}

const pathContainerVariants = {
  hidden: { opacity: 1 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.09,
      delayChildren: 0.1
    }
  }
}

const pathItemVariants = {
  hidden: { opacity: 0, y: 22, scale: 0.985 },
  visible: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { duration: 0.42, ease: easeOut }
  }
}

const cardRevealVariants = {
  hidden: revealInitial,
  visible: revealAnimate
}

const workflowContainerVariants = {
  hidden: { opacity: 1 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.12,
      delayChildren: 0.12
    }
  }
}

const workflowStepVariants = {
  hidden: { opacity: 0, x: -18 },
  visible: {
    opacity: 1,
    x: 0,
    transition: { duration: 0.38, ease: easeOut }
  }
}

// 首页只组织现有用户端能力入口，不新增业务能力或接口。
const featureRoutes = [
  {
    index: '01',
    title: '简历诊断',
    desc: '上传简历后获得结构、表达、匹配度和风险项反馈，先找出最影响投递转化的问题。',
    icon: 'resume-analysis',
    stage: '准备',
    action: '上传简历',
    path: '/resume/upload',
    tone: 'orange'
  },
  {
    index: '02',
    title: '岗位匹配',
    desc: '结合目标岗位拆解要求，帮助判断简历和岗位之间的差距与补强方向。',
    icon: 'job-match-analysis',
    stage: '定位',
    action: '分析匹配',
    path: '/resume/upload',
    tone: 'blue'
  },
  {
    index: '03',
    title: '模拟面试',
    desc: '按岗位进入 AI 面试场景，练习回答结构、追问应对和临场表达。',
    icon: 'mock-interview',
    stage: '训练',
    action: '开始面试',
    path: '/interview/entry',
    tone: 'orange'
  },
  {
    index: '04',
    title: '面试复盘',
    desc: '沉淀历史面试表现，回看评分、优势、短板和下一轮训练建议。',
    icon: 'interview-replay',
    stage: '复盘',
    action: '查看历史',
    path: '/interview/history',
    tone: 'blue'
  },
  {
    index: '05',
    title: '简历模板库',
    desc: '用更清晰的版式承载内容，避免好经历被凌乱排版拖累。',
    icon: 'template-library',
    stage: '呈现',
    action: '选择模板',
    path: '/templates',
    tone: 'sage'
  },
  {
    index: '06',
    title: 'Offer 辅助',
    desc: '面向多个机会做对比、谈薪准备和决策辅助，把最后一步做稳。',
    icon: 'offer-assistant',
    stage: '决策',
    action: '进入辅助',
    path: '/offer',
    tone: 'blue'
  }
]

const heroHighlights = [
  { title: '简历诊断', icon: 'resume-analysis', path: '/resume/upload' },
  { title: '面试训练', icon: 'mock-interview', path: '/interview/entry' },
  { title: 'Offer 辅助', icon: 'offer-assistant', path: '/offer' }
]

const workflowSteps = [
  {
    index: '01',
    title: '先校准材料',
    desc: '从简历诊断和岗位匹配开始，明确当前材料是否能支撑目标岗位。'
  },
  {
    index: '02',
    title: '再练习表达',
    desc: '进入模拟面试，把经历转化成可复用的回答结构。'
  },
  {
    index: '03',
    title: '复盘短板',
    desc: '通过报告和成长中心查看薄弱项，形成下一轮练习任务。'
  },
  {
    index: '04',
    title: '推进 Offer',
    desc: '在机会变多时做横向对比、准备谈薪话术和最终选择。'
  }
]

const supportCapabilities = [
  {
    title: '成长中心',
    desc: '查看练习轨迹、能力雷达和下一轮建议。',
    icon: 'growth-center',
    path: '/growth'
  },
  {
    title: '社区交流',
    desc: '分享复盘、查看经验和收到互动反馈。',
    icon: 'community-hub',
    path: '/community'
  },
  {
    title: '会员与额度',
    desc: '查看当前套餐、额度消耗和会员状态。',
    icon: 'membership-center',
    path: '/membership'
  },
  {
    title: '通知与版本动态',
    desc: '跟进系统消息、产品更新和任务提醒。',
    icon: 'notification-center',
    path: '/notifications'
  }
]

const truncate = (text, maxLen = 150) => {
  if (!text || text.length <= maxLen) return text
  return `${text.slice(0, maxLen).replace(/\s+\S*$/, '')}...`
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return dateStr
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

// 平台统计数据
const statsLoading = ref(true)
const stats = ref({
  userCount: 0,
  diagnosisCount: 0,
  interviewCount: 0
})

const formatCount = (num) => {
  if (!num || num <= 0) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1).replace(/\.0$/, '')}万+`
  if (num >= 1000) return `${Math.floor(num / 1000)},${String(num % 1000).padStart(3, '0')}+`
  return `${num}+`
}

const getVersionTagType = (type) => {
  if (type === 'major') return 'error'
  if (type === 'minor') return 'warning'
  return 'default'
}

onMounted(async () => {
  try {
    const versionRes = await getLatestVersionLogs(3)
    versionLogs.value = versionRes?.data || []
  } catch {
    versionLogs.value = []
  }
  try {
    const res = await getPublicStats()
    if (res.data) {
      stats.value = res.data
    }
  } catch {
    // 获取失败时保持默认值 0，不影响首页公开展示。
  } finally {
    statsLoading.value = false
  }
})

const goProtected = (path) => {
  if (isLoggedIn()) {
    router.push(path)
  } else {
    router.push('/login')
  }
}

const handleResume = () => {
  goProtected('/resume/upload')
}

const handleInterview = () => {
  goProtected('/interview/entry')
}
</script>

<style scoped>
.landing-page {
  min-height: calc(100vh - 60px);
  overflow-x: hidden;
  background: linear-gradient(180deg, #fff7ec 0%, #fffaf6 42%, var(--bg-card) 100%);
}

.hero-section,
.home-section,
.version-section {
  width: min(1180px, calc(100vw - 48px));
  margin: 0 auto;
}

.hero-section {
  position: relative;
  min-height: min(760px, calc(100vh - 126px));
  display: flex;
  align-items: center;
  padding: 58px 0 46px;
}

.background-hero-section {
  width: min(1240px, calc(100vw - 48px));
  isolation: isolate;
  overflow: hidden;
  border: 1px solid rgba(249, 214, 174, 0.82);
  border-radius: 18px;
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 255, 255, 0.92), transparent 30%),
    linear-gradient(135deg, #fff8ed 0%, #fff2df 48%, #ffe3bd 100%);
  box-shadow: 0 28px 80px rgba(148, 88, 36, 0.12);
}

.hero-background-art {
  position: absolute;
  right: clamp(-128px, -7vw, -36px);
  bottom: clamp(-48px, -2vw, -12px);
  z-index: 0;
  width: clamp(720px, 70vw, 940px);
  aspect-ratio: 3 / 2;
  filter: drop-shadow(0 34px 54px rgba(141, 88, 40, 0.2));
  pointer-events: none;
  will-change: transform;
  transition:
    opacity 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.36s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.36s cubic-bezier(0.22, 1, 0.36, 1);
}

.hero-background-art::after {
  position: absolute;
  inset: 0;
  content: '';
  background: var(--home-hero-bg) center bottom / contain no-repeat;
  opacity: 0.96;
  transform: translate3d(18px, 12px, 0) scale(1.018);
  animation: hero-art-reveal 0.86s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both,
    hero-art-breathe 7.2s ease-in-out 1.1s infinite;
  will-change: transform, opacity;
}

.background-hero-section::before {
  position: absolute;
  inset: 0;
  z-index: -1;
  content: '';
  background:
    radial-gradient(circle at 80% 22%, rgba(255, 255, 255, 0.42), transparent 18%),
    linear-gradient(90deg, rgba(255, 249, 240, 0.5) 0%, rgba(255, 249, 240, 0.2) 50%, rgba(255, 249, 240, 0) 100%);
}

.hero-main,
.hero-path-panel {
  position: relative;
  z-index: 1;
}

.hero-main {
  width: min(530px, calc(100% - 48px));
  margin-left: clamp(22px, 4.5vw, 64px);
  padding: clamp(22px, 3vw, 36px);
  border: 1px solid rgba(255, 226, 191, 0.76);
  border-radius: 14px;
  background: rgba(255, 253, 248, 0.82);
  box-shadow: 0 16px 42px rgba(132, 75, 32, 0.08);
}

.hero-cloud {
  position: absolute;
  z-index: 0;
  display: block;
  width: var(--cloud-width);
  height: calc(var(--cloud-width) * 0.34);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow:
    calc(var(--cloud-width) * 0.18) calc(var(--cloud-width) * -0.1) 0 calc(var(--cloud-width) * 0.02) rgba(255, 255, 255, 0.68),
    calc(var(--cloud-width) * 0.42) calc(var(--cloud-width) * -0.03) 0 calc(var(--cloud-width) * 0.04) rgba(255, 255, 255, 0.62),
    0 14px 36px rgba(213, 145, 72, 0.08);
  pointer-events: none;
  opacity: 0;
  transform: translate3d(var(--cloud-drift-x, 0), 10px, 0);
  animation: cloud-enter 0.7s cubic-bezier(0.22, 1, 0.36, 1) var(--cloud-delay, 0s) both,
    cloud-drift var(--cloud-duration, 8s) ease-in-out calc(var(--cloud-delay, 0s) + 0.7s) infinite;
  will-change: transform, opacity;
}

.cloud-one {
  --cloud-width: 150px;
  --cloud-delay: 0.16s;
  --cloud-drift-x: -8px;
  --cloud-duration: 8.5s;
  --cloud-opacity: 0.74;
  top: 52px;
  left: 48%;
}

.cloud-two {
  --cloud-width: 104px;
  --cloud-delay: 0.28s;
  --cloud-drift-x: 10px;
  --cloud-duration: 9.4s;
  --cloud-opacity: 0.64;
  right: 48px;
  top: 78px;
}

.cloud-three {
  --cloud-width: 122px;
  --cloud-delay: 0.38s;
  --cloud-drift-x: -6px;
  --cloud-duration: 10.2s;
  --cloud-opacity: 0.52;
  right: 36%;
  bottom: 78px;
}

.hero-badge,
.section-tag,
.board-eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  color: var(--orange-deep);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
}

.hero-badge {
  gap: 8px;
  padding: 7px 12px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 140, 66, 0.24);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 8px 22px rgba(117, 72, 42, 0.06);
}

.badge-icon {
  filter: drop-shadow(0 4px 8px rgba(255, 140, 66, 0.18));
}

.hero-title {
  max-width: 560px;
  margin: 0;
  color: var(--text-title);
  font-size: clamp(38px, 5.2vw, 64px);
  font-weight: 850;
  line-height: 1.06;
}

.hero-subtitle {
  max-width: 520px;
  margin: 16px 0 0;
  color: var(--text-body);
  font-size: 17px;
  line-height: 1.72;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.cta-btn {
  min-width: 178px;
  min-height: 54px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 850;
  letter-spacing: 0;
  transition: transform 0.18s ease-out, box-shadow 0.18s ease-out, border-color 0.18s ease-out;
}

.primary-btn {
  color: #fff7ed;
  border: 0;
  background:
    linear-gradient(135deg, #ff8a2a 0%, #ff6b1a 100%);
  box-shadow:
    0 16px 34px rgba(255, 107, 26, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.24);
}

.secondary-btn {
  color: var(--orange-deep);
  border: 1px solid rgba(255, 140, 66, 0.34);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 248, 241, 0.88));
  box-shadow: 0 12px 26px rgba(148, 88, 36, 0.1);
}

.cta-btn:hover {
  transform: translateY(-2px);
}

.primary-btn:hover {
  box-shadow:
    0 20px 42px rgba(255, 107, 26, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.secondary-btn:hover {
  border-color: rgba(255, 107, 26, 0.48);
  box-shadow: 0 16px 34px rgba(148, 88, 36, 0.14);
}

.cta-btn:active {
  transform: translateY(0) scale(0.985);
}

.cta-btn:focus-visible {
  outline: 3px solid rgba(255, 140, 66, 0.28);
  outline-offset: 3px;
}

.cta-btn :deep(.n-button__content) {
  gap: 10px;
}

.cta-btn :deep(.n-button__border),
.cta-btn :deep(.n-button__state-border) {
  display: none;
}

.primary-btn :deep(.n-button__content) {
  color: #fff7ed;
}

.btn-icon {
  filter: drop-shadow(0 3px 6px rgba(0, 0, 0, 0.12));
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 26px;
}

.hero-quick-trails {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-trail-pill {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 46px;
  padding: 0 14px 0 10px;
  border: 1px solid rgba(255, 180, 112, 0.36);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--text-title);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 72px;
}

.stat-number {
  color: var(--text-title);
  font-size: 24px;
  font-weight: 820;
  line-height: 1;
}

.stat-text {
  color: var(--text-muted);
  font-size: 13px;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: var(--border-divider);
}

.hero-path-panel {
  overflow: hidden;
  padding: 20px;
  border: 1px solid rgba(243, 216, 199, 0.86);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 44px rgba(117, 72, 42, 0.09);
}

.board-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.board-header strong {
  color: var(--text-title);
  font-size: 19px;
  line-height: 1.35;
}

.board-header p {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.path-progress-track {
  position: relative;
  height: 3px;
  margin: 2px 0 16px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(243, 216, 199, 0.72);
}

.path-progress-line {
  display: block;
  width: 38%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(255, 107, 26, 0.95), rgba(255, 166, 92, 0.72));
  transform-origin: left center;
  animation: path-flow 2.6s ease-out both;
}

.board-flow {
  display: grid;
  gap: 12px;
}

.flow-item {
  display: grid;
  grid-template-columns: 34px 32px 1fr;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border: 1px solid rgba(243, 216, 199, 0.72);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.74);
  color: var(--text-title);
  font-weight: 700;
}

.flow-item.is-active {
  border-color: rgba(255, 140, 66, 0.34);
  background: rgba(255, 248, 243, 0.9);
  box-shadow: inset 3px 0 0 rgba(255, 107, 26, 0.72);
}

.flow-index {
  color: rgba(255, 107, 26, 0.72);
  font-size: 12px;
  font-weight: 800;
}

.home-section {
  padding: 42px 0;
}

.career-path-section {
  padding-top: 56px;
}

.section-heading {
  display: grid;
  gap: 12px;
  max-width: 740px;
  margin-bottom: 28px;
}

.compact-heading {
  max-width: 560px;
}

.section-title {
  margin: 0;
  color: var(--text-title);
  font-size: clamp(28px, 3.6vw, 44px);
  font-weight: 820;
  line-height: 1.15;
}

.section-desc {
  margin: 0;
  color: var(--text-body);
  font-size: 16px;
  line-height: 1.75;
}

.career-path-rail {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
}

.career-path-rail::before {
  position: absolute;
  top: 70px;
  left: 5%;
  right: 5%;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, rgba(255, 140, 66, 0.14), rgba(255, 140, 66, 0.38), rgba(255, 140, 66, 0.14));
}

.career-path-node {
  position: relative;
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: 12px;
  min-height: 322px;
  padding: 18px 16px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 251, 248, 0.82)),
    rgba(255, 255, 255, 0.86);
  box-shadow: 0 12px 30px rgba(117, 72, 42, 0.06);
  cursor: pointer;
  color: inherit;
  text-align: left;
  transition: transform 0.18s ease-out, border-color 0.18s ease-out, box-shadow 0.18s ease-out;
}

.career-path-node:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 140, 66, 0.34);
  box-shadow: 0 18px 42px rgba(117, 72, 42, 0.1);
}

.path-node-index {
  color: rgba(255, 107, 26, 0.74);
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.route-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  margin-left: -6px;
  background: transparent;
  filter: drop-shadow(0 12px 18px var(--route-shadow, rgba(255, 140, 66, 0.18)));
  transform-origin: center;
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1), filter 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.path-node-copy {
  display: grid;
  gap: 9px;
  min-width: 0;
}

.tone-orange {
  --route-bg: rgba(255, 140, 66, 0.1);
  --route-shadow: rgba(255, 140, 66, 0.2);
}

.tone-blue {
  --route-bg: rgba(74, 111, 165, 0.1);
  --route-shadow: rgba(74, 111, 165, 0.18);
}

.tone-sage {
  --route-bg: rgba(117, 151, 123, 0.12);
  --route-shadow: rgba(117, 151, 123, 0.18);
}

.route-tag {
  color: var(--text-body);
  background: rgba(255, 248, 243, 0.95);
}

.career-path-node h3 {
  margin: 0;
  color: var(--text-title);
  font-size: 17px;
  font-weight: 800;
}

.career-path-node p {
  margin: 0;
  color: var(--text-body);
  font-size: 13px;
  line-height: 1.65;
}

.route-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  justify-self: start;
  width: fit-content;
  min-height: 36px;
  margin-top: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--orange-deep);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.arrow-icon {
  flex: 0 0 auto;
  opacity: 0.86;
  transition: transform 0.18s ease-out, opacity 0.18s ease-out;
}

.career-path-node:hover .route-arrow,
.support-capability-item:hover .support-next,
.version-more:hover .arrow-icon {
  opacity: 1;
  transform: translateX(6px) scale(1.08);
}

.career-path-node:hover .route-icon {
  filter: drop-shadow(0 16px 24px var(--route-shadow, rgba(255, 140, 66, 0.22)));
  transform: translateY(-2px) scale(1.04);
}

.workflow-section {
  position: relative;
  display: block;
  margin-top: 4px;
  padding: 38px 0 46px;
  overflow: hidden;
  border-top: 1px solid rgba(243, 216, 199, 0.78);
  border-bottom: 1px solid rgba(243, 216, 199, 0.6);
  border-radius: 0;
  background:
    radial-gradient(circle at 12% 20%, rgba(255, 255, 255, 0.82), transparent 24%),
    linear-gradient(180deg, rgba(255, 250, 244, 0.62), rgba(255, 244, 231, 0.34));
}

.workflow-section::before {
  position: absolute;
  right: 0;
  bottom: 42px;
  left: 0;
  height: 1px;
  content: '';
  background: linear-gradient(90deg, transparent, rgba(255, 140, 66, 0.32), transparent);
  pointer-events: none;
}

.workflow-copy {
  display: grid;
  grid-template-columns: minmax(240px, 0.56fr) minmax(0, 0.76fr);
  gap: 28px;
  align-items: end;
  min-width: 0;
  margin-bottom: 28px;
}

.workflow-copy .section-tag {
  grid-column: 1 / -1;
}

.workflow-copy .section-title,
.workflow-copy .section-desc {
  max-width: none;
}

.workflow-steps {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  padding: 0;
}

.workflow-step {
  position: relative;
  display: grid;
  grid-template-rows: 58px auto;
  gap: 14px;
  min-width: 0;
  padding: 0 18px 0 0;
  color: inherit;
  transition: transform 0.18s ease-out;
}

.workflow-step::after {
  position: absolute;
  top: 28px;
  right: 0;
  left: 56px;
  height: 1px;
  content: '';
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255, 140, 66, 0.34), rgba(255, 140, 66, 0.08));
}

.workflow-step:last-child::after {
  display: none;
}

.workflow-step:hover {
  transform: translateY(-2px);
}

.workflow-step:hover .step-index {
  transform: scale(1.06);
  box-shadow: 0 16px 30px rgba(117, 72, 42, 0.12);
}

.workflow-step:hover .step-copy {
  transform: translateX(2px);
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 1px solid rgba(255, 140, 66, 0.28);
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 245, 236, 0.92));
  color: var(--orange-deep);
  box-shadow: 0 12px 24px rgba(117, 72, 42, 0.08);
  font-weight: 850;
  transition: transform 0.18s ease-out, box-shadow 0.18s ease-out;
}

.step-copy {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding-right: 8px;
  transition: transform 0.18s ease-out;
}

.workflow-step h3,
.version-item-title {
  margin: 0;
  color: var(--text-title);
}

.workflow-step h3 {
  font-size: 18px;
  font-weight: 800;
}

.workflow-step p {
  margin: 0;
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.68;
}

.support-capability-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.support-capability-item {
  display: grid;
  grid-template-columns: 66px 1fr 32px;
  align-items: center;
  gap: 16px;
  min-height: 96px;
  padding: 16px 18px;
  border: 1px solid rgba(243, 216, 199, 0.72);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.82);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease-out, border-color 0.18s ease-out, box-shadow 0.18s ease-out;
}

@keyframes path-flow {
  from {
    opacity: 0.4;
    transform: scaleX(0.18);
  }

  to {
    opacity: 1;
    transform: scaleX(1);
  }
}

@keyframes hero-art-reveal {
  from {
    opacity: 0;
    transform: translate3d(18px, 12px, 0) scale(1.018);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes hero-art-breathe {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    transform: translate3d(0, -8px, 0) scale(1.006);
  }
}

@keyframes cloud-enter {
  from {
    opacity: 0;
    transform: translate3d(var(--cloud-drift-x, 0), 10px, 0);
  }

  to {
    opacity: var(--cloud-opacity, 0.68);
    transform: translate3d(0, 0, 0);
  }
}

@keyframes cloud-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(var(--cloud-drift-x, 0), -7px, 0);
  }
}

.support-capability-item:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 140, 66, 0.32);
  box-shadow: 0 12px 30px rgba(117, 72, 42, 0.08);
}

.support-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 66px;
  height: 66px;
  background: transparent;
  flex: 0 0 auto;
  filter: drop-shadow(0 12px 18px rgba(255, 140, 66, 0.14));
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1), filter 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}

.support-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.support-copy strong {
  color: var(--text-title);
  font-size: 16px;
  font-weight: 800;
}

.support-copy span {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.support-next {
  opacity: 0.82;
  justify-self: end;
}

.support-capability-item:hover .support-icon {
  filter: drop-shadow(0 16px 24px rgba(255, 140, 66, 0.18));
  transform: translateY(-2px) scale(1.04);
}

.version-section {
  padding: 54px 0 86px;
}

.version-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.version-title {
  margin: 8px 0 0;
  color: var(--text-title);
  font-size: 28px;
  font-weight: 820;
}

.version-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 40px;
  color: var(--orange-deep);
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.version-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.version-item {
  display: flex;
  gap: 14px;
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: var(--shadow-card);
  transition: border-color 0.18s ease-out, box-shadow 0.18s ease-out;
}

.version-item:hover {
  border-color: var(--orange-border);
  box-shadow: var(--shadow-hover);
}

.version-tag-wrapper {
  flex-shrink: 0;
  padding-top: 2px;
}

.version-info {
  flex: 1;
  min-width: 0;
}

.version-item-title {
  overflow: hidden;
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-item-desc {
  display: -webkit-box;
  margin: 8px 0;
  overflow: hidden;
  color: var(--text-body);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  word-break: break-word;
}

.version-item-time {
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 1080px) {
  .hero-section {
    grid-template-columns: 1fr;
    gap: 24px;
    padding-top: 44px;
  }

  .hero-background-art {
    right: clamp(-160px, -16vw, -96px);
    bottom: 8px;
    width: clamp(620px, 74vw, 820px);
    opacity: 0.9;
    filter: drop-shadow(0 24px 42px rgba(141, 88, 40, 0.16));
  }

  .career-path-rail,
  .version-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workflow-section {
    padding: 34px 0 38px;
  }

}

@media (max-width: 768px) {
  .hero-section,
  .home-section,
  .version-section {
    width: min(100% - 28px, 680px);
  }

  .hero-section {
    padding: 34px 0 28px;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .cta-btn {
    width: 100%;
  }

  .hero-stats {
    flex-wrap: wrap;
    gap: 16px;
  }

  .stat-divider {
    display: none;
  }

  .background-hero-section {
    min-height: 690px;
    align-items: flex-end;
    background:
      radial-gradient(circle at 50% 12%, rgba(255, 255, 255, 0.88), transparent 24%),
      linear-gradient(180deg, #fff6e8 0%, #ffe7c7 100%);
  }

  .hero-background-art {
    right: clamp(-88px, -13vw, -42px);
    bottom: 292px;
    width: clamp(520px, 112vw, 680px);
    opacity: 0.86;
    background-position: right bottom;
  }

  .hero-main {
    width: min(100% - 28px, 640px);
    margin: 0 auto 18px;
    padding: 20px;
  }

  .cloud-one {
    top: 36px;
    left: 18px;
  }

  .cloud-two {
    right: 22px;
    top: 118px;
  }

  .cloud-three {
    right: 44%;
    bottom: 306px;
  }

  .home-section,
  .version-section {
    padding: 34px 0;
  }

  .career-path-rail,
  .version-list,
  .support-capability-list {
    grid-template-columns: 1fr;
  }

  .career-path-rail::before {
    top: 0;
    bottom: 0;
    left: 23px;
    right: auto;
    width: 1px;
    height: auto;
    background: linear-gradient(180deg, rgba(255, 140, 66, 0.12), rgba(255, 140, 66, 0.34), rgba(255, 140, 66, 0.12));
  }

  .career-path-node {
    grid-template-columns: 88px 1fr;
    grid-template-rows: auto auto auto;
    min-height: auto;
  }

  .path-node-index,
  .route-icon {
    grid-column: 1;
  }

  .path-node-copy,
  .route-link {
    grid-column: 2;
  }

  .route-icon {
    width: 84px;
    height: 84px;
    margin-left: -4px;
  }

  .workflow-step {
    grid-template-columns: 56px 1fr;
    grid-template-rows: auto;
    gap: 14px;
    padding: 0 0 22px;
  }

  .workflow-copy,
  .workflow-steps {
    grid-template-columns: 1fr;
  }

  .workflow-copy {
    gap: 12px;
    margin-bottom: 24px;
  }

  .workflow-copy .section-tag {
    grid-column: auto;
  }

  .workflow-step::after {
    top: 60px;
    bottom: 0;
    left: 28px;
    width: 1px;
    height: auto;
    background: linear-gradient(180deg, rgba(255, 140, 66, 0.34), rgba(255, 140, 66, 0.08));
  }

  .version-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .background-hero-section {
    min-height: auto;
    padding-top: 26px;
  }

  .hero-background-art {
    right: -220px;
    bottom: 64px;
    width: 520px;
    opacity: 0;
    filter: drop-shadow(0 18px 26px rgba(141, 88, 40, 0.08));
  }

  .hero-main {
    width: min(100% - 20px, 440px);
    margin-bottom: 10px;
  }

  .hero-title {
    font-size: 31px;
  }

  .hero-quick-trails {
    gap: 8px;
  }

  .hero-trail-pill {
    min-height: 44px;
    padding-right: 12px;
  }

  .career-path-node {
    grid-template-columns: 78px 1fr;
    gap: 10px 12px;
    padding: 16px 14px;
  }

  .route-icon {
    width: 76px;
    height: 76px;
  }

  .support-capability-item {
    grid-template-columns: 58px 1fr 28px;
    gap: 12px;
    padding: 14px;
  }

  .workflow-section {
    padding: 28px 0 32px;
  }

  .support-icon {
    width: 58px;
    height: 58px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-background-art,
  .route-icon,
  .support-icon,
  .feature-route-card,
  .career-path-node,
  .workflow-step,
  .step-index,
  .step-copy,
  .support-capability-item,
  .version-item {
    transition: none;
  }

  .path-progress-line {
    animation: none;
  }

  .hero-background-art::after,
  .hero-cloud {
    animation: none;
    transform: none;
    will-change: auto;
  }

  .hero-background-art::after {
    opacity: 0.96;
  }

  .hero-cloud {
    opacity: var(--cloud-opacity, 0.62);
  }

  .arrow-icon {
    transition: none;
  }

  .career-path-node:hover,
  .workflow-step:hover,
  .support-capability-item:hover {
    transform: none;
  }

  .workflow-step:hover .step-index,
  .workflow-step:hover .step-copy {
    transform: none;
  }

  .career-path-node:hover .route-arrow,
  .support-capability-item:hover .support-next,
  .version-more:hover .arrow-icon {
    transform: none;
  }
}
</style>
