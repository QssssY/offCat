<template>
  <main class="membership-view">
    <section class="membership-workbench-hero" aria-labelledby="membership-title">
      <article class="membership-status-panel">
        <div class="hero-kicker">
          <FeatureIcon name="membership-center" size="md" class="hero-kicker-icon" />
          <span>会员中心</span>
        </div>

        <div class="hero-copy">
          <h1 id="membership-title">会员与额度工作台</h1>
          <p>{{ membershipTipText }}</p>
        </div>

        <div class="status-meta-grid">
          <div class="status-meta-item">
            <span class="meta-label">当前身份</span>
            <n-tag round :bordered="false" :type="isVipUser ? 'warning' : 'default'" class="status-tag">
              {{ memberIdentityText }}
            </n-tag>
          </div>
          <div class="status-meta-item">
            <span class="meta-label">当前套餐</span>
            <strong>{{ currentPlanName }}</strong>
          </div>
          <div class="status-meta-item">
            <span class="meta-label">到期时间</span>
            <strong>{{ vipExpireTimeText }}</strong>
          </div>
        </div>
      </article>

      <aside class="membership-quota-strip" aria-label="会员额度概览">
        <article class="quota-tile identity">
          <FeatureIcon name="membership-credits" size="lg" class="quota-icon" />
          <span class="quota-label">身份</span>
          <strong class="quota-value">{{ memberIdentityText }}</strong>
        </article>
        <article class="quota-tile">
          <FeatureIcon name="resume-score" size="lg" class="quota-icon" />
          <span class="quota-label">{{ resumeQuotaLabel }}</span>
          <strong class="quota-value">{{ resumeQuotaText }}</strong>
        </article>
        <article class="quota-tile">
          <FeatureIcon name="ai-interviewer" size="lg" class="quota-icon" />
          <span class="quota-label">{{ interviewQuotaLabel }}</span>
          <strong class="quota-value">{{ interviewQuotaText }}</strong>
        </article>
      </aside>
    </section>

    <section class="plan-comparison-section" aria-labelledby="plans-title">
      <div class="section-heading">
        <div>
          <p class="section-eyebrow">套餐对比</p>
          <h2 id="plans-title">选择适合当前求职节奏的方案</h2>
        </div>
        <p class="section-note">开通后立即生效，当前套餐再次购买会按现有规则续费顺延。</p>
      </div>

      <div v-if="plansLoading" class="plans-grid" aria-label="套餐加载中">
        <article v-for="item in 3" :key="item" class="plan-card plan-skeleton-card">
          <n-skeleton text width="42%" />
          <n-skeleton text width="72%" />
          <n-skeleton :height="44" round />
          <div class="skeleton-metric-grid">
            <n-skeleton :height="72" round />
            <n-skeleton :height="72" round />
          </div>
          <n-skeleton :height="46" round />
        </article>
      </div>

      <div v-else-if="plans.length > 0" class="plans-grid">
        <article
          v-for="(plan, index) in plans"
          :key="plan.planCode"
          class="plan-card"
          :class="{ current: isCurrentPlan(plan) }"
          :style="{ '--plan-index': index }"
        >
          <div class="plan-card-head">
            <div class="plan-title-line">
              <FeatureIcon name="membership-center" size="md" class="plan-icon" />
              <div>
                <h3>{{ getPlanNameCn(plan.planName) }}</h3>
                <p>{{ getPlanDescription(plan) }}</p>
              </div>
            </div>

            <div class="plan-tags">
              <n-tag v-if="isCurrentPlan(plan)" round :bordered="false" type="warning">当前套餐</n-tag>
            </div>
          </div>

          <div class="plan-price-row">
            <span class="plan-price">{{ formatPrice(plan.priceAmount) }}</span>
            <span class="plan-duration">/ {{ formatDuration(plan.durationDays) }}</span>
          </div>

          <div class="plan-benefits" aria-label="套餐权益">
            <div v-for="benefit in getPlanBenefits(plan)" :key="benefit" class="benefit-item">
              <FeatureIcon name="success" size="sm" class="benefit-icon" />
              <span>{{ benefit }}</span>
            </div>
          </div>

          <div class="plan-metrics">
            <div class="metric-item">
              <span>每日简历诊断</span>
              <strong>{{ plan.resumeQuota }}</strong>
            </div>
            <div class="metric-item">
              <span>每日模拟面试</span>
              <strong>{{ plan.interviewQuota }}</strong>
            </div>
            <div class="metric-item">
              <span>AI润色/天</span>
              <strong>{{ plan.dailyPolishLimit ?? 1 }}</strong>
            </div>
            <div class="metric-item">
              <span>JD匹配/天</span>
              <strong>{{ plan.dailyJdMatchLimit ?? 3 }}</strong>
            </div>
            <div class="metric-item">
              <span>模板/天</span>
              <strong>{{ plan.dailyTemplateLimit ?? 5 }}</strong>
            </div>
            <div class="metric-item">
              <span>Offer/天</span>
              <strong>{{ plan.dailyOfferLimit ?? 3 }}</strong>
            </div>
          </div>

          <div v-if="(plan.bonusResumeQuota > 0) || (plan.bonusInterviewQuota > 0)" class="plan-bonus">
            购买即送 {{ plan.bonusResumeQuota || 0 }} 次简历诊断 + {{ plan.bonusInterviewQuota || 0 }} 次模拟面试
          </div>

          <p v-if="isCurrentPlan(plan)" class="renewal-note">
            该方案已生效，点击续费后将继续顺延会员到期时间。
          </p>

          <n-button
            v-if="isCurrentPlan(plan)"
            type="primary"
            size="large"
            round
            block
            class="upgrade-btn"
            @click="handleUpgrade(plan)"
          >
            续费
          </n-button>
          <n-button
            v-else-if="canPurchase(plan)"
            type="primary"
            size="large"
            round
            block
            class="upgrade-btn"
            @click="handleUpgrade(plan)"
          >
            立即升级
          </n-button>
          <n-button
            v-else
            size="large"
            round
            block
            class="upgrade-btn"
            disabled
          >
            已订阅更高级套餐
          </n-button>
        </article>
      </div>

      <div v-else class="empty-card">
        <FeatureIcon name="membership-credits" size="lg" class="empty-icon" />
        <h3>暂无可用套餐</h3>
        <p>请稍后再试</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { NButton, NSkeleton, NTag } from 'naive-ui'
import { getMembershipPlans } from '@/api/membership'
import FeatureIcon from '@/components/common/FeatureIcon.vue'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'MembershipView' })

const userStore = useUserStore()

const plans = ref([])
const plansLoading = ref(false)

const MEMBERSHIP_RECHARGE_UNAVAILABLE_MESSAGE = '当前未开放充值功能，请联系管理员进行升级'

const userInfo = computed(() => userStore.userInfo)
const resumeQuotaText = computed(() => Number(userInfo.value?.vipDailyResumeQuota ?? userInfo.value?.resumeQuota ?? 0))
const interviewQuotaText = computed(() => Number(userInfo.value?.vipDailyInterviewQuota ?? userInfo.value?.interviewQuota ?? 0))

const isVipUser = computed(() => {
  const role = userInfo.value?.role
  const vipExpireTime = userInfo.value?.vipExpireTime

  if (role !== 1) return false
  if (!vipExpireTime) return false

  return new Date(vipExpireTime) > new Date()
})

const currentPlanCode = computed(() => {
  if (!isVipUser.value) return ''
  return userInfo.value?.membershipPlanCode || ''
})

const memberIdentityText = computed(() => (isVipUser.value ? '会员用户' : '普通用户'))

const currentPlanName = computed(() => {
  if (!isVipUser.value) return '未开通会员'

  const matchedPlan = plans.value.find((plan) => plan.planCode === currentPlanCode.value)
  if (matchedPlan) return getPlanNameCn(matchedPlan.planName)

  return currentPlanCode.value || '会员用户'
})

const vipExpireTimeText = computed(() => {
  if (!isVipUser.value || !userInfo.value?.vipExpireTime) return '--'
  return formatDateTime(userInfo.value.vipExpireTime)
})

const resumeQuotaLabel = computed(() => (
  isVipUser.value ? '今日简历额度' : '免费简历额度'
))

const interviewQuotaLabel = computed(() => (
  isVipUser.value ? '今日面试额度' : '免费面试额度'
))

const membershipTipText = computed(() => {
  if (isVipUser.value) {
    return '会员有效期内可使用每日 5 次简历诊断、每日 10 次模拟面试，次日自动刷新。'
  }

  return '普通用户总免费 1 次简历诊断、3 次模拟面试，用完后可升级会员继续使用。'
})

const currentPlanSort = computed(() => {
  if (!isVipUser.value || !currentPlanCode.value) return 0
  const matched = plans.value.find((p) => p.planCode === currentPlanCode.value)
  return matched?.sort ?? 0
})

const canPurchase = (plan) => {
  if (!isVipUser.value) return true
  if (isCurrentPlan(plan)) return true
  if ((plan.sort ?? 0) > currentPlanSort.value) return true
  return false
}

const getPlanNameCn = (planName) => {
  const nameMap = {
    'Monthly VIP': '月度会员',
    'Quarterly VIP': '季度会员',
    'Yearly VIP': '年度会员'
  }

  return nameMap[planName] || planName
}

const getPlanTag = (plan) => {
  if (isCurrentPlan(plan)) return ''

  const nameMap = {
    'Monthly VIP': '轻量开启',
    'Quarterly VIP': '热门推荐',
    'Yearly VIP': '高频推荐'
  }

  return nameMap[plan.planName] || ''
}

const getPlanBenefits = (plan) => {
  // 优先使用后端返回的 benefits 数组
  if (plan.benefits && Array.isArray(plan.benefits) && plan.benefits.length > 0) {
    return plan.benefits
  }
  // 兜底
  return [
    `${plan.durationDays || 0} 天内有效`,
    `每日 ${plan.resumeQuota} 次简历诊断`,
    `每日 ${plan.interviewQuota} 次模拟面试`
  ]
}

const getPlanScene = (plan) => {
  if (plan.planName === 'Monthly VIP') return '适合短期集中投递和快速体验完整功能。'
  if (plan.planName === 'Quarterly VIP') return '适合持续求职、反复打磨简历和面试表达。'
  if (plan.planName === 'Yearly VIP') return '适合高频使用和长期职业成长管理。'

  return '适合需要更高额度和完整求职辅助的用户。'
}

const getPlanDescription = (plan) => plan.description || getPlanScene(plan)

const fetchPlans = async () => {
  plansLoading.value = true

  try {
    const res = await getMembershipPlans()
    plans.value = (Array.isArray(res.data) ? res.data : []).slice(0, 6).map((plan) => ({
      ...plan,
      priceAmount: Number(plan.priceAmount ?? 0),
      durationDays: Number(plan.durationDays ?? 0),
      resumeQuota: Number(plan.resumeQuota ?? 0),
      interviewQuota: Number(plan.interviewQuota ?? 0),
      dailyPolishLimit: Number(plan.dailyPolishLimit ?? 1),
      dailyJdMatchLimit: Number(plan.dailyJdMatchLimit ?? 3),
      dailyTemplateLimit: Number(plan.dailyTemplateLimit ?? 5),
      dailyOfferLimit: Number(plan.dailyOfferLimit ?? 3),
      bonusResumeQuota: Number(plan.bonusResumeQuota ?? 0),
      bonusInterviewQuota: Number(plan.bonusInterviewQuota ?? 0),
      sort: Number(plan.sort ?? 0)
    }))
  } catch {
    plans.value = []
  } finally {
    plansLoading.value = false
  }
}

const ensureUserInfo = async () => {
  if (userStore.userInfo) return

  try {
    await userStore.fetchUserInfo()
  } catch {
    // Request interceptor owns user-facing error handling.
  }
}

const isCurrentPlan = (plan) => isVipUser.value && currentPlanCode.value === plan.planCode

const handleUpgrade = () => {
  // 测试项目暂不开放真实充值和 mock 升级，续费与升级入口统一给出管理员联系提示。
  ElMessage.warning(MEMBERSHIP_RECHARGE_UNAVAILABLE_MESSAGE)
}

const formatDateTime = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const formatPrice = (value) => {
  const numberValue = Number(value ?? 0)
  return `¥${numberValue.toFixed(2)}`
}

const formatDuration = (days) => `${days} 天`

onMounted(async () => {
  await Promise.all([ensureUserInfo(), fetchPlans()])
})
</script>

<style scoped>
.membership-view {
  --membership-ease: cubic-bezier(0.25, 1, 0.5, 1);
  --membership-surface: color-mix(in srgb, var(--bg-card) 88%, var(--orange-light-bg) 12%);
  --membership-soft: color-mix(in srgb, var(--bg-page) 78%, var(--orange-light-bg) 22%);
  --membership-shadow: 0 18px 46px rgba(132, 75, 32, 0.09);
  width: min(1180px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: membership-page-enter 0.42s var(--membership-ease) both;
}

.membership-workbench-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(340px, 0.82fr);
  gap: 20px;
  align-items: stretch;
}

.membership-status-panel,
.membership-quota-strip,
.plan-card,
.empty-card {
  border: 1px solid var(--border-card);
  background: var(--membership-surface);
  box-shadow: var(--membership-shadow);
}

.membership-status-panel {
  min-height: 280px;
  padding: clamp(24px, 4vw, 38px);
  border-radius: 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  color: var(--orange-deep);
  font-size: 13px;
  font-weight: 700;
}

.hero-kicker-icon,
.plan-icon,
.benefit-icon,
.quota-icon,
.empty-icon {
  filter: drop-shadow(0 7px 12px rgba(255, 140, 66, 0.15));
}

.hero-copy h1,
.section-heading h2,
.plan-card h3,
.empty-card h3 {
  margin: 0;
  color: var(--text-title);
}

.hero-copy h1 {
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-copy p,
.section-note,
.plan-title-line p,
.renewal-note,
.empty-card p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.hero-copy p {
  max-width: 650px;
  margin-top: 14px;
  font-size: 15px;
}

.status-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.status-meta-item {
  min-width: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--membership-soft);
  border: 1px solid color-mix(in srgb, var(--border-card) 78%, transparent);
}

.status-meta-item strong,
.quota-value,
.metric-item strong {
  display: block;
  margin-top: 8px;
  color: var(--text-title);
  font-size: 17px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.meta-label,
.quota-label,
.metric-item span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.membership-quota-strip {
  min-height: 280px;
  padding: 18px;
  border-radius: 22px;
  display: grid;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quota-tile {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  grid-template-areas:
    'icon label'
    'icon value';
  column-gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--bg-card) 72%, var(--orange-light-bg) 28%);
  border: 1px solid color-mix(in srgb, var(--border-card) 72%, transparent);
  transition:
    transform 0.2s var(--membership-ease),
    border-color 0.2s var(--membership-ease),
    box-shadow 0.2s var(--membership-ease);
}

.quota-tile:hover {
  transform: translateY(-2px);
  border-color: var(--orange-border);
  box-shadow: 0 10px 24px rgba(255, 140, 66, 0.1);
}

.quota-icon {
  grid-area: icon;
}

.quota-label {
  grid-area: label;
}

.quota-value {
  grid-area: value;
  margin-top: 2px;
  font-size: 20px;
}

.plan-comparison-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-end;
}

.section-eyebrow {
  margin: 0 0 8px;
  color: var(--orange-main);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.section-heading h2 {
  font-size: clamp(22px, 3vw, 30px);
}

.section-note {
  max-width: 360px;
  font-size: 13px;
  text-align: right;
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.plan-card {
  min-height: 520px;
  padding: 22px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  animation: plan-card-enter 0.42s var(--membership-ease) both;
  animation-delay: calc(var(--plan-index, 0) * 70ms);
  transition:
    transform 0.22s var(--membership-ease),
    box-shadow 0.22s var(--membership-ease),
    border-color 0.22s var(--membership-ease);
}

.plan-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--orange-main) 36%, var(--border-card));
  box-shadow: 0 20px 52px rgba(132, 75, 32, 0.13);
}

.plan-card.current {
  border-color: color-mix(in srgb, var(--orange-main) 44%, var(--border-card));
  background: color-mix(in srgb, var(--bg-card) 82%, var(--orange-light-bg) 18%);
}

.plan-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.plan-title-line {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.plan-title-line h3 {
  font-size: 21px;
  line-height: 1.25;
}

.plan-title-line p {
  margin-top: 6px;
  font-size: 13px;
}

.plan-tags {
  flex: 0 0 auto;
}

.plan-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.plan-price {
  color: var(--orange-deep);
  font-size: clamp(34px, 4vw, 44px);
  font-weight: 800;
  line-height: 1;
}

.plan-duration {
  color: var(--text-muted);
  font-size: 14px;
}

.plan-benefits {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.5;
}

.plan-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.plan-bonus {
  margin-top: 12px;
  padding: 8px 12px;
  background: var(--el-color-warning-light-9);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-color-warning-dark-2);
  text-align: center;
}

.metric-item {
  padding: 14px;
  border-radius: 14px;
  background: var(--membership-soft);
  border: 1px solid color-mix(in srgb, var(--border-card) 74%, transparent);
}

.metric-item strong {
  font-size: 22px;
}

.renewal-note {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--orange-border);
  background: color-mix(in srgb, var(--orange-light-bg) 72%, var(--bg-card) 28%);
  color: var(--orange-deep);
  font-size: 13px;
}

.upgrade-btn {
  margin-top: auto;
  min-height: 46px;
  font-weight: 700;
  transition:
    transform 0.14s var(--membership-ease),
    box-shadow 0.2s var(--membership-ease);
}

.upgrade-btn:active {
  transform: scale(0.98);
}

.plan-skeleton-card {
  pointer-events: none;
}

.skeleton-metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.empty-card {
  min-height: 260px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
}

.empty-card h3 {
  font-size: 20px;
}

@keyframes membership-page-enter {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes plan-card-enter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:global(html[data-theme='dark']) .membership-view {
  --membership-surface: color-mix(in srgb, var(--bg-card) 90%, rgba(255, 140, 66, 0.08) 10%);
  --membership-soft: color-mix(in srgb, var(--bg-page) 76%, rgba(255, 140, 66, 0.12) 24%);
  --membership-shadow: 0 18px 46px rgba(0, 0, 0, 0.24);
}

@media (max-width: 1100px) {
  .membership-workbench-hero {
    grid-template-columns: 1fr;
  }

  .membership-quota-strip {
    min-height: auto;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    grid-template-rows: auto;
  }

  .plans-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .membership-view {
    gap: 18px;
  }

  .membership-status-panel,
  .membership-quota-strip,
  .plan-card {
    border-radius: 16px;
  }

  .status-meta-grid,
  .membership-quota-strip,
  .plans-grid,
  .section-heading {
    grid-template-columns: 1fr;
  }

  .section-heading {
    display: grid;
    align-items: start;
  }

  .section-note {
    max-width: none;
    text-align: left;
  }

  .plan-card {
    min-height: auto;
    padding: 18px;
  }

  .plan-card-head {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .plan-metrics {
    grid-template-columns: 1fr;
  }

  .quota-tile {
    grid-template-columns: 44px minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .membership-view,
  .plan-card {
    animation: none;
  }

  .quota-tile,
  .plan-card,
  .upgrade-btn {
    transition-duration: 0.01ms;
  }

  .quota-tile:hover,
  .plan-card:hover,
  .upgrade-btn:active {
    transform: none;
  }
}
</style>
