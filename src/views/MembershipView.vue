<template>
  <div class="membership-view">
    <!-- 页面顶部 Banner：简洁说明会员价值，不含技术细节 -->
    <section class="hero-card">
      <div class="hero-content">
        <div class="hero-badge">会员中心</div>
        <h1 class="hero-title">解锁全部功能</h1>
        <p class="hero-subtitle">
          升级后立即生效，可享受每日 5 次简历诊断、每日 10 次模拟面试，到期时间自动顺延。
        </p>
      </div>
      <div class="hero-decoration">
        <div class="hero-orb hero-orb-large"></div>
        <div class="hero-orb hero-orb-small"></div>
      </div>
    </section>

    <!-- 状态区：左侧会员信息卡 + 右侧额度卡 -->
    <section class="status-grid">
      <!-- 当前会员信息卡 -->
      <article class="status-card">
        <div class="status-header">
          <div>
            <div class="section-eyebrow">当前状态</div>
            <h2 class="section-title">我的会员</h2>
          </div>
          <div class="status-badge" :class="statusBadgeClass">
            {{ memberIdentityText }}
          </div>
        </div>

        <div class="status-body">
          <div class="status-name">{{ userName }}</div>
          <div class="status-plan">
            当前套餐：<span>{{ currentPlanName }}</span>
          </div>
          <div class="status-expire">
            到期时间：<span>{{ vipExpireTimeText }}</span>
          </div>
          <p class="status-tip">{{ membershipTipText }}</p>
        </div>
      </article>

      <!-- 额度展示卡：用户关心的使用数据，无技术注释 -->
      <section class="quota-panel">
        <article class="quota-card">
          <div class="quota-label">身份</div>
          <div class="quota-value">{{ memberIdentityText }}</div>
        </article>

        <article class="quota-card">
          <div class="quota-label">当前套餐</div>
          <div class="quota-value">{{ currentPlanName }}</div>
        </article>

        <article class="quota-card">
          <div class="quota-label">{{ resumeQuotaLabel }}</div>
          <div class="quota-value">{{ resumeQuotaText }}</div>
        </article>

        <article class="quota-card">
          <div class="quota-label">{{ interviewQuotaLabel }}</div>
          <div class="quota-value">{{ interviewQuotaText }}</div>
        </article>
      </section>
    </section>

    <!-- 套餐列表区 -->
    <section class="plans-section">
      <div class="plans-header">
        <div>
          <div class="section-eyebrow">套餐列表</div>
          <h2 class="section-title">选择适合你的方案</h2>
        </div>
      </div>

      <!-- 加载骨架屏：保持布局稳定 -->
      <div v-if="plansLoading" class="plans-grid">
        <article v-for="item in 3" :key="item" class="plan-card skeleton-card">
          <div class="skeleton-line skeleton-title"></div>
          <div class="skeleton-line skeleton-subtitle"></div>
          <div class="skeleton-line skeleton-price"></div>
          <div class="skeleton-metrics">
            <div class="skeleton-line skeleton-metric"></div>
            <div class="skeleton-line skeleton-metric"></div>
            <div class="skeleton-line skeleton-metric"></div>
          </div>
          <div class="skeleton-button"></div>
        </article>
      </div>

      <!-- 套餐卡片列表 -->
      <div v-else-if="plans.length > 0" class="plans-grid">
        <article
          v-for="plan in plans"
          :key="plan.planCode"
          class="plan-card"
          :class="{ current: isCurrentPlan(plan) }"
        >
          <!-- 套餐标签：轻量开启 / 热门推荐 / 高频推荐 -->
          <div class="plan-tag" v-if="getPlanTag(plan)">
            {{ getPlanTag(plan) }}
          </div>

          <div class="plan-top">
            <div>
              <!-- 套餐名称：中文化，后端返回英文名时做本地映射 -->
              <div class="plan-name">{{ getPlanNameCn(plan.planName) }}</div>
              <div class="plan-desc">
                开通后立即生效，每日 5 次简历诊断、每日 10 次模拟面试
              </div>
            </div>
            <div v-if="isCurrentPlan(plan)" class="current-tag">当前套餐</div>
          </div>

          <div class="plan-price-row">
            <span class="plan-price">{{ formatPrice(plan.priceAmount) }}</span>
            <span class="plan-duration"
              >/{{ formatDuration(plan.durationDays) }}</span
            >
          </div>

          <!-- 权益亮点列表：丰富卡片中部视觉，降低空白感 -->
          <div class="plan-benefits">
            <div
              v-for="benefit in getPlanBenefits(plan)"
              :key="benefit"
              class="benefit-item"
            >
              <span class="benefit-dot"></span>
              <span class="benefit-text">{{ benefit }}</span>
            </div>
          </div>

          <!-- 指标区：4个指标块，填满中下区域 -->
          <div class="plan-metrics">
            <div class="metric-item">
              <span class="metric-label">时长</span>
              <span class="metric-value">{{
                formatDuration(plan.durationDays)
              }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">每日简历次数</span>
              <span class="metric-value">{{ plan.resumeQuota }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">每日面试次数</span>
              <span class="metric-value">{{ plan.interviewQuota }}</span>
            </div>
            <div class="metric-item">
              <span class="metric-label">权益周期</span>
              <span class="metric-value">每日刷新</span>
            </div>
          </div>

          <!-- 场景说明：制造"有人适合"的感知 -->
          <div class="plan-scene">{{ getPlanScene(plan) }}</div>

          <!-- 操作区：当前套餐显示状态说明+续费，非当前显示立即升级 -->
          <div class="plan-action">
            <div v-if="isCurrentPlan(plan)" class="action-status">
              该方案已生效，点击续费后将继续顺延会员到期时间
            </div>
            <el-button
              class="upgrade-btn"
              type="primary"
              size="large"
              :disabled="isUpgradeBusy"
              :loading="upgradingPlanCode === plan.planCode"
              @click="handleUpgrade(plan)"
            >
              {{ isCurrentPlan(plan) ? "续费" : "立即升级" }}
            </el-button>
          </div>
        </article>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-card">
        <div class="empty-title">暂无可用套餐</div>
        <div class="empty-desc">请稍后再试</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { getMembershipPlans, mockUpgradeMembership } from "@/api/membership";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

/* ========================
   状态
   ======================== */
const plans = ref([]);
const plansLoading = ref(false);
const upgradingPlanCode = ref("");

/* ========================
   用户信息派生状态
   ======================== */
const userInfo = computed(() => userStore.userInfo);
const userName = computed(() => userInfo.value?.nickname || userInfo.value?.username || "用户");
const resumeQuotaText = computed(() => Number(userInfo.value?.resumeQuota ?? 0));
const interviewQuotaText = computed(() => Number(userInfo.value?.interviewQuota ?? 0));

/* ========================
   剩余次数标签
   作用：把会员中心里的次数展示改成和后端当前规则一致的文案。
   原来的“剩余次数”会让用户误解为购买套餐后的累计剩余总次数，
   但当前项目里 /api/auth/me 返回的 resumeQuota / interviewQuota 已经不是累计赠送次数：
   普通用户对应总免费额度剩余，VIP 对应后端基于 daily_resume_used / daily_interview_used 计算出的今日剩余。
   所以前端必须显式区分“今日剩余”和“免费剩余”。
   ======================== */
const resumeQuotaLabel = computed(() => {
  return isVipUser.value ? "总剩余简历次数" : "剩余免费简历次数";
});

const interviewQuotaLabel = computed(() => {
  return isVipUser.value ? "总剩余面试次数" : "剩余免费面试次数";
});

/* ========================
   会员状态判断
   ======================== */
const isVipUser = computed(() => {
  const role = userInfo.value?.role;
  const vipExpireTime = userInfo.value?.vipExpireTime;
  if (role !== 1) return false;
  if (!vipExpireTime) return false;
  return new Date(vipExpireTime) > new Date();
});

/* ========================
   当前套餐
   ======================== */
const currentPlanCode = computed(() => {
  if (!isVipUser.value) return "";
  return userInfo.value?.membershipPlanCode || "";
});

/* ========================
   身份文字（中文化）
   ======================== */
const memberIdentityText = computed(() => {
  return isVipUser.value ? "会员用户" : "普通用户";
});

/* ========================
   状态徽章样式
   ======================== */
const statusBadgeClass = computed(() => {
  return isVipUser.value ? "badge-vip" : "badge-normal";
});

/* ========================
   当前套餐名称：优先从套餐列表匹配，否则显示套餐代码
   ======================== */
const currentPlanName = computed(() => {
  if (!isVipUser.value) return "未开通会员";
  const matchedPlan = plans.value.find(
    (plan) => plan.planCode === currentPlanCode.value
  );
  if (matchedPlan) return getPlanNameCn(matchedPlan.planName);
  return currentPlanCode.value || "会员用户";
});

/* ========================
   到期时间：格式化为 YYYY-MM-DD HH:mm
   ======================== */
const vipExpireTimeText = computed(() => {
  if (!isVipUser.value || !userInfo.value?.vipExpireTime) {
    return "--";
  }
  return formatDateTime(userInfo.value.vipExpireTime);
});

/* ========================
   会员提示文案（用户语言，不含技术细节）
   ======================== */
const membershipTipText = computed(() => {
  if (isVipUser.value) {
    return "会员有效期内可使用每日 5 次简历诊断、每日 10 次模拟面试，次日自动刷新。";
  }
  return "普通用户总免费 1 次简历诊断、3 次模拟面试，用完即止。";
});

/* ========================
   忙碌状态
   ======================== */
const isUpgradeBusy = computed(() => upgradingPlanCode.value !== "");

/* ========================
   套餐名称中文化映射表
   ======================== */
const getPlanNameCn = (planName) => {
  const nameMap = {
    "Monthly VIP": "月度会员",
    "Quarterly VIP": "季度会员",
    "Yearly VIP": "年度会员",
  };
  return nameMap[planName] || planName;
};

/* ========================
   套餐标签：每张卡片的推荐标识，非当前套餐时显示推荐标签
   ======================== */
const getPlanTag = (plan) => {
  if (isCurrentPlan(plan)) return "";
  const nameMap = {
    "Monthly VIP": "轻量开启",
    "Quarterly VIP": "热门推荐",
    "Yearly VIP": "高频推荐",
  };
  return nameMap[plan.planName] || "";
};

/* ========================
   权益亮点：根据时长推导2~3条用户视角权益
   ======================== */
const getPlanBenefits = (plan) => {
  const days = plan.durationDays || 0;
  if (plan.planName === "Monthly VIP") {
    return [
      `${days}天内有效`,
      `每日 ${plan.resumeQuota} 次简历诊断`,
      `每日 ${plan.interviewQuota} 次模拟面试`,
    ];
  }
  if (plan.planName === "Quarterly VIP") {
    return [
      `${days}天内有效`,
      `每日 ${plan.resumeQuota} 次简历诊断`,
      `每日 ${plan.interviewQuota} 次模拟面试`,
    ];
  }
  if (plan.planName === "Yearly VIP") {
    return [
      `${days}天内有效`,
      `每日 ${plan.resumeQuota} 次简历诊断`,
      `每日 ${plan.interviewQuota} 次模拟面试`,
    ];
  }
  return [];
};

/* ========================
   场景说明：制造"有人适合"的感知，提升产品感
   ======================== */
const getPlanScene = (plan) => {
  if (plan.planName === "Monthly VIP") return "适合想要快速体验完整功能的用户";
  if (plan.planName === "Quarterly VIP") return "适合有稳定求职需求的用户";
  if (plan.planName === "Yearly VIP") return "适合高频使用者，一步到位最划算";
  return "";
};

/* ========================
   获取套餐列表
   ======================== */
const fetchPlans = async () => {
  plansLoading.value = true;
  try {
    const res = await getMembershipPlans();
    plans.value = (Array.isArray(res.data) ? res.data : []).map(p => ({
      ...p,
      priceAmount: Number(p.priceAmount ?? 0),
      durationDays: Number(p.durationDays ?? 0),
      resumeQuota: Number(p.resumeQuota ?? 0),
      interviewQuota: Number(p.interviewQuota ?? 0),
    }));
  } catch {
    plans.value = [];
    // 拦截器已弹出错误提示
  } finally {
    plansLoading.value = false;
  }
};

/* ========================
   确保用户信息已加载
   ======================== */
const ensureUserInfo = async () => {
  if (userStore.userInfo) return;
  try {
    await userStore.fetchUserInfo();
  } catch {
    // 拦截器已弹出错误提示
  }
};

/* ========================
   是否为当前套餐
   ======================== */
const isCurrentPlan = (plan) => {
  return isVipUser.value && currentPlanCode.value === plan.planCode;
};

/* ========================
   升级操作
   作用：统一处理“立即升级”和“续费”。
   当前套餐不能再被前端拦截，因为同套餐再次购买在当前业务里不是无效操作，
   而是需要继续调用原有升级接口，让后端基于现有 vipExpireTime 做顺延。
   会员并不是购买累计总次数，所以续费只延长有效期，不叠加累计次数。
   ======================== */
const handleUpgrade = async (plan) => {
  if (isUpgradeBusy.value) return;
  const isRenewal = isCurrentPlan(plan);
  upgradingPlanCode.value = plan.planCode;
  try {
    await mockUpgradeMembership({ planCode: plan.planCode });
    await userStore.fetchUserInfo();
    ElMessage.success(
      `${getPlanNameCn(plan.planName)}${isRenewal ? "续费" : "升级"}成功`
    );
  } catch {
    // 拦截器已弹出错误提示
  } finally {
    upgradingPlanCode.value = "";
  }
};

/* ========================
   日期时间格式化：YYYY-MM-DD HH:mm
   ======================== */
const formatDateTime = (value) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const h = String(date.getHours()).padStart(2, "0");
  const min = String(date.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${d} ${h}:${min}`;
};

/* ========================
   价格格式化
   ======================== */
const formatPrice = (value) => {
  const numberValue = Number(value ?? 0);
  return `¥${numberValue.toFixed(2)}`;
};

/* ========================
   时长格式化
   ======================== */
const formatDuration = (days) => {
  return `${days} 天`;
};

/* ========================
   页面初始化
   ======================== */
onMounted(async () => {
  await Promise.all([ensureUserInfo(), fetchPlans()]);
});
</script>

<style scoped>
/* ========================
   页面容器
   ======================== */
.membership-view {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ========================
   顶部 Banner
   ======================== */
.hero-card {
  position: relative;
  overflow: hidden;
  padding: 32px 36px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ff9a5c 0%, #ff8c42 42%, #e67a35 100%);
  color: var(--bg-card, #ffffff);
  box-shadow: 0 10px 28px rgba(255, 140, 66, 0.22);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 620px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hero-title {
  margin: 18px 0 12px;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
}

.hero-subtitle {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.92);
}

.hero-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-orb {
  position: absolute;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-orb-large {
  top: -36px;
  right: -24px;
  width: 220px;
  height: 220px;
}

.hero-orb-small {
  right: 150px;
  bottom: -30px;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.08);
}

/* ========================
   状态区网格
   ======================== */
.status-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 20px;
}

.status-card,
.quota-panel,
.plan-card,
.empty-card {
  background: var(--bg-card, #ffffff);
  border-radius: 20px;
  border: 1px solid var(--border-card, rgba(243, 216, 199, 0.55));
  box-shadow: 0 4px 18px rgba(255, 140, 66, 0.08);
}

.status-card {
  padding: 28px 30px;
}

.status-header,
.plans-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-eyebrow {
  font-size: 12px;
  font-weight: 600;
  color: var(--orange-main);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.section-title {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-title, #2f2f2f);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.status-badge.badge-vip {
  background: linear-gradient(135deg, #fff1e5 0%, #ffe0c7 100%);
  color: #d56e2f;
}

.status-badge.badge-normal {
  background: var(--bg-elevated, #f5f7fa);
  color: var(--text-body);
}

.status-body {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-name {
  font-size: 30px;
  font-weight: 700;
  color: var(--text-title, #2f2f2f);
  line-height: 1.1;
}

.status-plan,
.status-expire {
  font-size: 15px;
  color: var(--text-muted);
}

.status-plan span,
.status-expire span {
  color: var(--text-title, #2f2f2f);
  font-weight: 600;
}

.status-tip {
  margin: 10px 0 0;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.75;
  color: var(--text-muted);
}

/* ========================
   额度面板
   ======================== */
.quota-panel {
  padding: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.quota-card {
  padding: 18px 18px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, var(--bg-page) 0%, var(--bg-card) 100%);
  border: 1px solid var(--border-card);
}

.quota-label {
  font-size: 13px;
  color: var(--text-muted);
}

.quota-value {
  margin-top: 10px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-title, #2f2f2f);
  line-height: 1.2;
}

/* ========================
   套餐区
   ======================== */
.plans-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.plan-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: transform 0.22s ease, box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 26px rgba(255, 140, 66, 0.12);
  border-color: rgba(255, 140, 66, 0.28);
}

.plan-card.current {
  border-color: rgba(255, 140, 66, 0.34);
  box-shadow: 0 10px 28px rgba(255, 140, 66, 0.14);
}

/* 套餐标签：左上角推荐标识，吸引注意力 */
.plan-tag {
  display: inline-flex;
  align-self: flex-start;
  padding: 6px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff9c60 0%, #ff8c42 100%);
  color: var(--bg-card, #ffffff);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

.plan-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.plan-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-title, #2f2f2f);
}

.plan-desc {
  margin-top: 8px;
  min-height: 42px;
  font-size: 13px;
  line-height: 1.6;
  color: #8e7c6d;
}

.current-tag {
  flex-shrink: 0;
  padding: 7px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #ff9c60 0%, #ff8c42 100%);
  color: var(--bg-card, #ffffff);
  font-size: 12px;
  font-weight: 600;
}

.plan-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.plan-price {
  font-size: 38px;
  font-weight: 700;
  color: var(--orange-deep, #e67a35);
  line-height: 1;
}

.plan-duration {
  font-size: 14px;
  color: #9b8a7c;
}

/* 权益亮点列表：填充卡片中部，降低空白感 */
.plan-benefits {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-radius: 14px;
  background: var(--orange-light-bg);
  border: 1px solid rgba(243, 216, 199, 0.45);
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.benefit-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--orange-main);
  flex-shrink: 0;
}

.benefit-text {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

/* 场景说明：制造"有人适合"的感知 */
.plan-scene {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  padding: 0 2px;
}

/* 操作区：统一包裹按钮，当前套餐含状态说明 */
.plan-action {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: auto;
}

/* 状态说明：制造"方案已生效"感知，非禁用感 */
.action-status {
  padding: 10px 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, var(--bg-page) 100%);
  border: 1px solid var(--orange-border);
  font-size: 13px;
  color: var(--orange-deep);
  line-height: 1.5;
  text-align: center;
}

.plan-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 14px;
  border-radius: 14px;
  background: var(--orange-light-bg);
  border: 1px solid rgba(243, 216, 199, 0.45);
}

.metric-label {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.metric-value {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-title, #2f2f2f);
}

.upgrade-btn {
  margin-top: auto;
  height: 46px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #ff9a5c 0%, #ff8c42 100%);
  box-shadow: 0 6px 18px rgba(255, 140, 66, 0.22);
}

.upgrade-btn.is-disabled,
.upgrade-btn.is-disabled:hover {
  background: var(--bg-elevated);
  box-shadow: none;
  color: var(--text-muted);
}

.empty-card {
  padding: 32px 24px;
  text-align: center;
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-title, #2f2f2f);
}

.empty-desc {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-muted);
}

/* ========================
   骨架屏
   ======================== */
.skeleton-card {
  pointer-events: none;
}

.skeleton-line,
.skeleton-button {
  border-radius: 999px;
  background: linear-gradient(90deg, #f8e9de 25%, #fff6f0 50%, #f8e9de 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s infinite linear;
}

.skeleton-title {
  width: 42%;
  height: 26px;
}

.skeleton-subtitle {
  width: 76%;
  height: 14px;
}

.skeleton-price {
  width: 52%;
  height: 40px;
}

.skeleton-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.skeleton-metric {
  height: 76px;
  border-radius: 14px;
}

.skeleton-button {
  width: 100%;
  height: 46px;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* ========================
   响应式
   ======================== */
@media (max-width: 1100px) {
  .status-grid {
    grid-template-columns: 1fr;
  }
  .plans-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .membership-view {
    gap: 18px;
  }
  .hero-card,
  .status-card {
    padding: 22px 20px;
  }
  .hero-title {
    font-size: 28px;
  }
  .status-header,
  .plans-header {
    flex-direction: column;
  }
  .status-name {
    font-size: 24px;
  }
  .quota-panel {
    grid-template-columns: 1fr;
    padding: 16px;
  }
  .plans-grid {
    grid-template-columns: 1fr;
  }
  .plan-card {
    padding: 20px;
  }
  .plan-price {
    font-size: 32px;
  }
}
</style>
