<template>
  <!-- 骨架屏 -->
  <div v-if="pageLoading" class="dashboard-skeleton">
    <n-skeleton :height="160" round animated />
    <n-skeleton text :repeat="4" animated />
  </div>
  <!-- 错误状态 -->
  <div v-else-if="loadError" class="dashboard-error">
    <FeatureIcon name="error" size="lg" class="error-icon" />
    <h3>加载失败</h3>
    <p>获取数据时出现问题，请重试</p>
    <n-button type="primary" @click="fetchData">重新加载</n-button>
  </div>
  <!-- 正常内容 -->
  <div v-else class="dashboard-view">
    <!-- 顶部区域 -->
    <div class="top-section profile-workbench">
      <!-- 左侧：身份欢迎卡 -->
      <div class="identity-card">
        <div class="card-bg-decoration"></div>
        <div class="card-content">
          <div class="identity-left">
            <div class="avatar-wrapper avatar-lg">
              <div class="avatar-ring avatar-lg">
                <OptimizedImage :sources="optimizedImages.userAvatar" img-class="avatar-img avatar-lg" alt="用户头像" />
              </div>
            </div>
            <div class="user-info">
              <div class="welcome-text">欢迎回来</div>
              <div class="user-name">
                {{ userStore.userInfo?.nickname || userStore.userInfo?.username || "用户" }}
              </div>
              <div class="user-role-tag" :class="roleBadgeClass">
                <span class="role-dot"></span>
                {{ roleBadgeText }}
              </div>
            </div>
          </div>
          <div class="identity-right">
            <div class="vip-badge">
              <FeatureIcon name="membership-center" size="sm" class="vip-icon" />
              <span>注册时间</span>
            </div>
            <div class="vip-expire-time">{{ formatRegisterTime }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧：权益配额卡（6宫格） -->
      <div class="quota-card quota-overview">
        <div class="quota-grid">
          <div v-for="item in quotaItems" :key="item.type" class="quota-cell" :class="{ 'exhausted': item.exhausted }">
            <div class="quota-icon-wrap" :class="item.iconClass">
              <FeatureIcon :name="item.icon" size="lg" class="quota-icon" />
            </div>
            <div class="quota-info">
              <div class="quota-number" :class="{ 'text-danger': item.exhausted }">{{ item.remaining }}</div>
              <div class="quota-label">{{ item.label }}</div>
              <!-- 额度耗尽时显示升级引导 -->
              <router-link v-if="item.exhausted && !isAdmin" to="/membership" class="quota-upgrade-link">
                升级会员
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据概览区 - 4张独立卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon resume">
          <FeatureIcon name="resume-score" size="lg" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ resumeCountThisMonth }}</div>
          <div class="stat-label">本月诊断</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon interview">
          <FeatureIcon name="interview-replay" size="lg" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ interviewCountThisMonth }}</div>
          <div class="stat-label">本月面试</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon resume-left">
          <FeatureIcon name="membership-credits" size="lg" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ isVipUser ? vipDailyResumeQuotaLeft : resumeQuotaLeft }}</div>
          <div class="stat-label">{{ resumeStatLabel }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon interview-left">
          <FeatureIcon name="membership-credits" size="lg" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ isVipUser ? vipDailyInterviewQuotaLeft : interviewQuotaLeft }}</div>
          <div class="stat-label">{{ interviewStatLabel }}</div>
        </div>
      </div>
    </div>

    <!-- 新手任务卡片 -->
    <OnboardingTaskCard
      v-if="onboardingVisible && !onboardingAllCompleted"
      :tasks="onboardingTasks"
      :completed-count="onboardingCompletedCount"
      :total-count="onboardingTotalCount"
    />

    <!-- 成长中心入口 -->
    <router-link to="/growth" class="growth-entry-card">
      <div class="growth-entry-icon">
        <FeatureIcon name="growth-radar" size="lg" />
      </div>
      <div class="growth-entry-content">
        <div class="growth-entry-title">个人成长中心</div>
        <div class="growth-entry-desc">查看你的成长轨迹与个性化建议</div>
      </div>
      <FeatureIcon name="next" size="md" class="growth-entry-arrow" />
    </router-link>

    <!-- 最近记录区 -->
    <div class="records-section">
      <div class="record-column">
        <div class="column-header">
          <div class="header-left">
            <div class="header-icon resume">
              <FeatureIcon name="resume-analysis" size="md" />
            </div>
            <h3 class="column-title">最近简历诊断</h3>
          </div>
          <button class="view-all-btn" type="button" @click="viewAllResume">
            查看全部
            <FeatureIcon name="next" size="md" class="arrow-icon" />
          </button>
        </div>
        <div class="record-list">
          <template v-if="recentResumeRecords.length > 0">
            <component
              :is="record.status === 2 ? 'router-link' : 'div'"
              v-for="record in recentResumeRecords"
              :key="record.taskId"
              :to="record.status === 2 ? `/resume/result/${record.taskId}` : undefined"
              class="record-item"
              :class="{ clickable: record.status === 2 }"
            >
              <div class="record-left">
                <div class="file-icon">
                  <FeatureIcon name="resume-analysis" size="sm" />
                </div>
                <div class="record-info">
                  <div class="record-name">{{ record.fileName }}</div>
                  <div class="record-time">{{ record.time }}</div>
                </div>
              </div>
              <div class="record-status-badge" :class="record.statusClass">
                {{ record.statusText }}
              </div>
            </component>
          </template>
          <template v-else>
            <div class="empty-state">
              <FeatureIcon name="resume-upload" size="lg" class="empty-icon" />
              <div class="empty-text">暂无简历诊断记录</div>
              <n-button text type="primary" @click="startResumeDiagnosis">
                上传简历
              </n-button>
            </div>
          </template>
        </div>
      </div>

      <div class="record-column">
        <div class="column-header">
          <div class="header-left">
            <div class="header-icon interview">
              <FeatureIcon name="mock-interview" size="md" />
            </div>
            <h3 class="column-title">最近模拟面试</h3>
          </div>
          <button class="view-all-btn" type="button" @click="viewAllInterview">
            查看全部
            <FeatureIcon name="next" size="md" class="arrow-icon" />
          </button>
        </div>
        <div class="record-list">
          <template v-if="recentInterviewRecords.length > 0">
            <component
              :is="record.status === 1 ? 'router-link' : 'div'"
              v-for="record in recentInterviewRecords"
              :key="record.sessionId"
              :to="record.status === 1 ? `/interview/report/${record.sessionId}` : undefined"
              class="record-item"
              :class="{ clickable: record.status === 1 }"
            >
              <div class="record-left">
                <div class="interview-icon-wrap">
                  <FeatureIcon name="ai-interviewer" size="sm" />
                </div>
                <div class="record-info">
                  <div class="record-name">{{ record.jobRole }}</div>
                  <div class="record-time">{{ record.time }}</div>
                </div>
              </div>
              <div class="record-score-tag" v-if="record.score !== null && record.score !== undefined">
                <span class="score-value">{{ record.score }}</span>
                <span class="score-unit">分</span>
              </div>
            </component>
          </template>
          <template v-else>
            <div class="empty-state">
              <FeatureIcon name="mock-interview" size="lg" class="empty-icon" />
              <div class="empty-text">暂无模拟面试记录</div>
              <n-button text type="primary" @click="startInterview">
                开始面试
              </n-button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getResumeHistory, extractFileName } from "@/api/resume";
import { getInterviewHistory } from "@/api/interview";
import { getMonthlyStats } from "@/api/stats";
import { getOnboardingTasks } from "@/api/onboarding";
import { NButton, NSkeleton } from "naive-ui";
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import OptimizedImage from "@/components/common/OptimizedImage.vue";
import OnboardingTaskCard from "@/components/OnboardingTaskCard.vue";
import { optimizedImages } from "@/utils/optimizedImages";

defineOptions({ name: 'DashboardView' })

const router = useRouter();
const userStore = useUserStore();

// 从 store 获取额度数据
const resumeQuotaLeft = computed(() => {
  return Number(userStore.userInfo?.resumeQuota ?? 0);
});

const interviewQuotaLeft = computed(() => {
  return Number(userStore.userInfo?.interviewQuota ?? 0);
});

const vipDailyResumeQuotaLeft = computed(() => {
  return Number(userStore.userInfo?.vipDailyResumeQuota ?? 0);
});

const vipDailyInterviewQuotaLeft = computed(() => {
  return Number(userStore.userInfo?.vipDailyInterviewQuota ?? 0);
});

// 用户角色判定
const isAdmin = computed(() => userStore.userInfo?.role === 9);
const isVipUser = computed(() => {
  const role = userStore.userInfo?.role;
  const vipExpireTime = userStore.userInfo?.vipExpireTime;
  if (role !== 1) return false;
  if (!vipExpireTime) return false;
  return new Date(vipExpireTime) > new Date();
});
const isNormalUser = computed(() => !isAdmin.value && !isVipUser.value);

// ==================== 6 宫格额度数据 ====================

/** 6 种额度的免费次数上限 */
const FREE_LIMITS = {
  INTERVIEW: 3,
  RESUME: 1,
  POLISH: 1,
  JD_MATCH: 1,
  TEMPLATE: 2,
  OFFER: 1,
};

/**
 * 6 宫格额度卡片数据
 * VIP 用户显示 VIP 每日剩余；非 VIP 用户显示免费剩余
 */
const quotaItems = computed(() => {
  const info = userStore.userInfo;
  if (!info) return [];

  const items = [
    {
      type: 'INTERVIEW',
      icon: 'mock-interview',
      iconClass: 'interview',
      get remaining() {
        return isVipUser.value
          ? Number(info.vipDailyInterviewQuota ?? 0)
          : Number(info.interviewQuota ?? 0);
      },
      get limit() { return isVipUser.value ? null : FREE_LIMITS.INTERVIEW; },
      get label() { return isVipUser.value ? '今日面试剩余' : '免费面试剩余'; },
      get exhausted() { return this.remaining <= 0; },
    },
    {
      type: 'RESUME',
      icon: 'resume-analysis',
      iconClass: 'resume',
      get remaining() {
        return isVipUser.value
          ? Number(info.vipDailyResumeQuota ?? 0)
          : Number(info.resumeQuota ?? 0);
      },
      get limit() { return isVipUser.value ? null : FREE_LIMITS.RESUME; },
      get label() { return isVipUser.value ? '今日简历剩余' : '免费简历剩余'; },
      get exhausted() { return this.remaining <= 0; },
    },
    {
      type: 'POLISH',
      icon: 'resume-optimization',
      iconClass: 'polish',
      get remaining() {
        return isVipUser.value
          ? Number(info.vipDailyPolishQuota ?? 0)
          : Number(info.freePolishLeft ?? 0);
      },
      get limit() { return isVipUser.value ? null : FREE_LIMITS.POLISH; },
      get label() { return isVipUser.value ? '今日润色剩余' : '免费润色剩余'; },
      get exhausted() { return this.remaining <= 0; },
    },
    {
      type: 'JD_MATCH',
      icon: 'job-match-analysis',
      iconClass: 'jd-match',
      get remaining() {
        return isVipUser.value
          ? Number(info.vipDailyJdMatchQuota ?? 0)
          : Number(info.freeJdMatchLeft ?? 0);
      },
      get limit() { return isVipUser.value ? null : FREE_LIMITS.JD_MATCH; },
      get label() { return isVipUser.value ? '今日匹配剩余' : '免费匹配剩余'; },
      get exhausted() { return this.remaining <= 0; },
    },
    {
      type: 'TEMPLATE',
      icon: 'template-library',
      iconClass: 'template',
      get remaining() {
        return isVipUser.value
          ? Number(info.vipDailyTemplateQuota ?? 0)
          : Number(info.freeTemplateLeft ?? 0);
      },
      get limit() { return isVipUser.value ? null : FREE_LIMITS.TEMPLATE; },
      get label() { return isVipUser.value ? '今日模板剩余' : '免费模板剩余'; },
      get exhausted() { return this.remaining <= 0; },
    },
    {
      type: 'OFFER',
      icon: 'offer-assistant',
      iconClass: 'offer',
      get remaining() {
        return isVipUser.value
          ? Number(info.vipDailyOfferQuota ?? 0)
          : Number(info.freeOfferLeft ?? 0);
      },
      get limit() { return isVipUser.value ? null : FREE_LIMITS.OFFER; },
      get label() { return isVipUser.value ? '今日Offer剩余' : '免费Offer剩余'; },
      get exhausted() { return this.remaining <= 0; },
    },
  ];

  return items;
});

/**
 * 作用：统一首页额度卡和统计卡的标签口径。
 * 之前这里直接写“剩余”，会继续把当前实现表达成“购买套餐后累计剩余次数”，
 * 但当前项目后端已经改成：
 * 1. 普通用户看总免费次数剩余；
 * 2. VIP 用户看后端基于 daily_resume_used / daily_interview_used 计算出的今日剩余。
 * 因此前端必须把 VIP 标成“今日剩余”，把普通用户标成“免费剩余”。
 */
const resumeQuotaLabel = computed(() => {
  return isVipUser.value ? "总剩余简历诊断" : "免费简历诊断剩余";
});

const interviewQuotaLabel = computed(() => {
  return isVipUser.value ? "总剩余模拟面试" : "免费模拟面试剩余";
});

const resumeStatLabel = computed(() => {
  return isVipUser.value ? "今日简历剩余" : "免费简历剩余";
});

const interviewStatLabel = computed(() => {
  return isVipUser.value ? "今日面试剩余" : "免费面试剩余";
});

// 角色徽章文本
const roleBadgeText = computed(() => {
  if (isAdmin.value) return "管理员";
  if (isVipUser.value) return "会员";
  return "普通用户";
});

// 角色徽章样式类
const roleBadgeClass = computed(() => {
  if (isAdmin.value) return "badge-admin";
  if (isVipUser.value) return "badge-vip";
  return "badge-normal";
});

// 注册时间格式化，首页身份卡不再展示会员到期时间。
const formatRegisterTime = computed(() => {
  const createTime = userStore.userInfo?.createTime;
  if (!createTime) return "--";
  const date = new Date(createTime);
  if (Number.isNaN(date.getTime())) return "--";
  return date.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
});

// 本月统计数据（由后端接口返回）
const resumeCountThisMonth = ref(0);
const interviewCountThisMonth = ref(0);

// 展示用历史记录
const allResumeHistoryForDisplay = ref([]);
const allInterviewHistoryForDisplay = ref([]);

// 新手任务卡片数据
const onboardingTasks = ref([]);
const onboardingCompletedCount = ref(0);
const onboardingTotalCount = ref(4);
const onboardingVisible = ref(false);
const onboardingAllCompleted = ref(false);

// 页面加载与错误状态
const pageLoading = ref(true);
const loadError = ref(false);

/**
 * 从 API 响应中提取列表数据
 * 后端统一返回 Result<PageResult<T>> 或 Result<List<T>>
 */
const extractPageList = (res) => {
  if (!res?.data) return [];
  if (Array.isArray(res.data)) return res.data;
  if (Array.isArray(res.data.list)) return res.data.list;
  return [];
};

/**
 * 从记录项中提取时间字段值
 */
const extractTimeFromRecord = (item) => {
  if (!item) return null;
  return (
    item.createTime ||
    item.createdAt ||
    item.startTime ||
    item.created_time ||
    item.updateTime ||
    item.updatedAt ||
    null
  );
};

// 状态映射 - 橙色主题
const statusMap = {
  0: { text: "排队中", class: "status-pending" },
  1: { text: "解析中", class: "status-processing" },
  2: { text: "已完成", class: "status-success" },
  3: { text: "失败", class: "status-failed" },
};

const getStatusText = (status) => {
  return statusMap[status]?.text || "未知";
};

const getStatusClass = (status) => {
  return statusMap[status]?.class || "";
};

// 时间格式化
const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// 最近记录
const recentResumeRecords = computed(() => {
  return allResumeHistoryForDisplay.value.slice(0, 5).map((item) => ({
    taskId: item.taskId,
    fileName: extractFileName(item.fileUrl),
    time: formatTime(extractTimeFromRecord(item)),
    status: item.status,
    statusText: getStatusText(item.status),
    statusClass: getStatusClass(item.status),
  }));
});

const recentInterviewRecords = computed(() => {
  return allInterviewHistoryForDisplay.value.slice(0, 5).map((item) => ({
    sessionId: item.sessionId,
    jobRole: jobRoleMap[item.jobRole] || item.jobRole || "未知岗位",
    time: formatTime(extractTimeFromRecord(item)),
    score: item.comprehensiveScore ?? item.score,
    status: item.status,
    difficulty: item.difficulty,
    mode: item.interviewMode || item.mode || "normal",
  }));
});

// 岗位名称映射
const jobRoleMap = {
  frontend: "前端开发工程师",
  backend: "后端开发工程师",
  java: "Java开发工程师",
  product: "产品经理",
  algorithm: "算法工程师",
  operation: "运营",
  marketing: "市场/销售",
};

// 获取简历诊断历史记录（展示最近 5 条）
const fetchResumeHistory = async () => {
  const res = await getResumeHistory({ pageNum: 1, pageSize: 5 });
  allResumeHistoryForDisplay.value = extractPageList(res);
};

// 获取模拟面试历史记录（展示最近 5 条）
const fetchInterviewHistory = async () => {
  const res = await getInterviewHistory({ pageNum: 1, pageSize: 5 });
  allInterviewHistoryForDisplay.value = extractPageList(res);
};

// 获取本月统计数据（后端 SQL COUNT，无需前端过滤）
const fetchMonthlyStats = async () => {
  const res = await getMonthlyStats();
  if (res?.data) {
    resumeCountThisMonth.value = res.data.resumeCountThisMonth ?? 0;
    interviewCountThisMonth.value = res.data.interviewCountThisMonth ?? 0;
  }
};

// 获取新手任务列表
const fetchOnboardingTasks = async () => {
  const res = await getOnboardingTasks();
  if (res?.data) {
    onboardingTasks.value = res.data.tasks ?? [];
    onboardingCompletedCount.value = res.data.completedCount ?? 0;
    onboardingTotalCount.value = res.data.totalCount ?? 4;
    onboardingVisible.value = res.data.visible ?? false;
    onboardingAllCompleted.value = res.data.allCompleted ?? false;
  }
};

// 按时间倒序排列记录
const sortByTime = (list) => {
  list.sort((a, b) => {
    const timeA = new Date(extractTimeFromRecord(a)).getTime();
    const timeB = new Date(extractTimeFromRecord(b)).getTime();
    return timeB - timeA;
  });
};

// 统一加载数据（用户信息 + 历史记录 + 月度统计并发，骨架屏覆盖全部加载）
const fetchData = async () => {
  pageLoading.value = true;
  loadError.value = false;
  const fetches = [fetchResumeHistory(), fetchInterviewHistory(), fetchMonthlyStats(), fetchOnboardingTasks()];
  if (!userStore.userInfo) {
    fetches.push(userStore.fetchUserInfo());
  }
  const results = await Promise.allSettled(fetches);
  // 全部失败时显示错误状态
  const allFailed = results.slice(0, 2).every((r) => r.status === "rejected");
  if (allFailed) {
    loadError.value = true;
  } else {
    sortByTime(allResumeHistoryForDisplay.value);
    sortByTime(allInterviewHistoryForDisplay.value);
  }
  pageLoading.value = false;
};

// 页面加载时获取用户信息和历史记录
onMounted(() => {
  fetchData();
});

const startResumeDiagnosis = () => {
  router.push("/resume/upload");
};

const startInterview = () => {
  router.push("/interview/entry");
};

// 查看全部简历诊断
const viewAllResume = () => {
  router.push("/resume/history");
};

const viewAllInterview = () => {
  router.push("/interview/history");
};
</script>

<style scoped>
.dashboard-skeleton {
  padding: 24px;
  min-height: 60vh;
}

.dashboard-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: 12px;
  color: var(--text-muted);
}

.dashboard-error .error-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--tag-bg-danger);
  color: var(--color-danger);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
}

.dashboard-error h3 {
  margin: 0;
  color: var(--text-title);
}

.dashboard-error p {
  margin: 0;
  font-size: 14px;
}

.dashboard-view {
  --dashboard-ease: cubic-bezier(0.25, 1, 0.5, 1);
  min-height: 100%;
  padding: 0;
}

.top-section {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
  margin-bottom: 24px;
}

.profile-workbench {
  align-items: stretch;
}

.identity-card {
  position: relative;
  display: flex;
  align-items: center;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bg-card) 86%, var(--orange-main) 14%), var(--bg-card));
  border-radius: 20px;
  padding: 28px 32px;
  color: var(--text-title);
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--border-card) 78%, var(--orange-main) 22%);
  box-shadow: 0 16px 42px rgba(255, 140, 66, 0.08);
}

.card-bg-decoration {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.card-bg-decoration::after {
  content: "";
  position: absolute;
  top: 20px;
  left: 20px;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.card-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
}

.identity-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 身份卡内的头像放大 */
.identity-card .avatar-wrapper.avatar-lg,
.identity-card .avatar-ring.avatar-lg {
  width: 84px;
  height: 84px;
}

.identity-card .avatar-img.avatar-lg {
  width: 74px;
  height: 74px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.welcome-text {
  font-size: 15px;
  opacity: 0.85;
  font-weight: 400;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.user-role-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 5px 14px;
  background: color-mix(in srgb, var(--bg-card) 74%, var(--orange-main) 26%);
  color: var(--orange-deep);
  border-radius: 20px;
  margin-top: 8px;
  width: fit-content;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.identity-right {
  text-align: right;
}

.vip-badge {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 6px;
}

.vip-icon {
  width: 26px;
  height: 26px;
  filter: drop-shadow(0 4px 8px rgba(255, 140, 66, 0.18));
}

.vip-expire-time {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.guest-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  opacity: 0.85;
}

.guest-icon {
  width: 16px;
  height: 16px;
}

.quota-card {
  background: var(--bg-card);
  border-radius: 20px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.08);
  border: 1px solid var(--border-card);
}

.quota-overview {
  align-content: center;
}

/* 6宫格布局 */
.quota-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px 24px;
  width: 100%;
}

.quota-cell {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
}

.quota-cell.exhausted .quota-icon-wrap {
  opacity: 0.55;
}

.quota-icon-wrap {
  width: 70px;
  height: 70px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 10px 18px rgba(255, 140, 66, 0.16));
}

.quota-icon {
  color: var(--orange-main);
}

.quota-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quota-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-title);
  line-height: 1;
}

.quota-label {
  font-size: 13px;
  color: var(--text-muted);
}

/* 额度耗尽数字变红 */
.text-danger {
  color: var(--color-danger) !important;
}

.quota-upgrade-link {
  display: inline-block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--orange-main);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.quota-upgrade-link:hover {
  color: var(--orange-deep);
  text-decoration: underline;
}

.quota-divider {
  width: 1px;
  height: 52px;
  background: linear-gradient(
    180deg,
    transparent 0%,
    #f3d8c7 50%,
    transparent 100%
  );
  margin: 0 28px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 22px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
  transition:
    transform 0.18s var(--dashboard-ease),
    box-shadow 0.2s var(--dashboard-ease),
    border-color 0.2s var(--dashboard-ease);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.1);
}

.stat-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  filter: drop-shadow(0 10px 18px rgba(255, 140, 66, 0.14));
}

.stat-icon.resume {
  color: var(--orange-deep);
}

.stat-icon.interview {
  color: var(--orange-main);
}

.stat-icon.resume-left {
  color: var(--color-success);
}

.stat-icon.interview-left {
  color: var(--color-warning);
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-title);
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

/* 成长中心入口卡片 */
.growth-entry-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #ff9a5c 0%, var(--el-color-primary) 40%, var(--el-color-primary-dark-2) 100%);
  border-radius: 16px;
  color: inherit;
  cursor: pointer;
  text-decoration: none;
  transition:
    transform 0.2s var(--dashboard-ease),
    box-shadow 0.2s var(--dashboard-ease),
    border-color 0.2s var(--dashboard-ease);
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.2);
}

.growth-entry-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 140, 66, 0.3);
}

.growth-entry-card:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--orange-main) 70%, #ffffff 30%);
  outline-offset: 4px;
}

.growth-entry-icon {
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  filter: drop-shadow(0 12px 20px rgba(255, 255, 255, 0.18));
}

.growth-entry-content {
  flex: 1;
}

.growth-entry-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 2px;
}

.growth-entry-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.growth-entry-arrow {
  color: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
  transition: transform 0.18s var(--dashboard-ease);
}

.growth-entry-card:hover .growth-entry-arrow {
  transform: translateX(4px);
}

.records-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.record-column {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid var(--border-card);
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--bg-page);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon svg {
  width: 18px;
  height: 18px;
}

.header-icon.resume {
  color: var(--orange-main);
}

.header-icon.interview {
  color: var(--orange-deep);
}

.column-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 13px;
  font-family: inherit;
  line-height: 1;
  color: var(--orange-main);
  cursor: pointer;
  transition: color 0.18s var(--dashboard-ease);
}

.view-all-btn:hover {
  color: var(--orange-deep);
}

.view-all-btn:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--orange-main) 64%, transparent);
  outline-offset: 4px;
  border-radius: 999px;
}

.arrow-icon {
  color: var(--orange-main);
  transition: transform 0.18s var(--dashboard-ease);
}

.view-all-btn:hover .arrow-icon {
  transform: translateX(3px);
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 10px;
  color: inherit;
  text-decoration: none;
  transition:
    background-color 0.15s var(--dashboard-ease),
    transform 0.15s var(--dashboard-ease);
  user-select: none;
}

.record-item.clickable {
  cursor: pointer;
}

.record-item:hover {
  background: var(--bg-card-hover);
}

.record-item.clickable:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--orange-main) 64%, transparent);
  outline-offset: 2px;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.file-icon,
.interview-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-icon svg,
.interview-icon-wrap svg {
  color: var(--orange-main);
}

.record-info {
  flex: 1;
  min-width: 0;
}

.record-name {
  font-size: 14px;
  color: var(--text-title);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.record-status-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
  flex-shrink: 0;
}

.record-status-badge.status-success {
  background: var(--orange-light-bg);
  color: var(--orange-deep);
}

.record-status-badge.status-processing {
  background: var(--tag-bg-warning);
  color: var(--tag-text-warning);
}

.record-status-badge.status-pending {
  background: var(--bg-elevated);
  color: var(--color-info);
}

.record-status-badge.status-failed {
  background: var(--tag-bg-danger);
  color: var(--tag-text-danger);
}

.record-score-tag {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 4px 10px;
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.08) 100%);
  border-radius: 6px;
  flex-shrink: 0;
}

.score-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--orange-deep);
}

.score-unit {
  font-size: 12px;
  color: var(--orange-deep);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  gap: 8px;
}

.empty-icon {
  width: 40px;
  height: 40px;
  color: var(--orange-border);
  margin-bottom: 4px;
}

.empty-text {
  font-size: 13px;
  color: var(--text-muted);
}

/* ============================================
   响应式断点
   ============================================ */

/* 大屏：≥1280px - 默认样式 */

/* 中屏：1024px - 1279px */
@media (max-width: 1279px) {
  .top-section {
    gap: 16px;
  }
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .identity-card,
  .quota-card {
    padding: 20px 24px;
  }
  .quota-number {
    font-size: 24px;
  }
  .stat-value {
    font-size: 22px;
  }
  .welcome-text {
    font-size: 13px;
  }
  .user-name {
    font-size: 22px;
  }
  .vip-expire-time {
    font-size: 17px;
  }
  .identity-card .avatar-wrapper.avatar-lg,
  .identity-card .avatar-ring.avatar-lg {
    width: 68px;
    height: 68px;
  }
  .identity-card .avatar-img.avatar-lg {
    width: 60px;
    height: 60px;
  }
  .quota-icon-wrap {
    width: 56px;
    height: 56px;
    border-radius: 14px;
  }
}

/* 小屏：≤1023px - 平板竖屏，保持左右排版 */
@media (max-width: 1023px) {
  .top-section {
    grid-template-columns: 1fr 1fr;
    gap: 14px;
    align-items: stretch;
  }
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .records-section {
    grid-template-columns: 1fr 1fr;
    gap: 14px;
  }
  .card-bg-decoration {
    display: none;
  }
  .identity-card {
    padding: 18px 18px;
    display: flex;
    align-items: center;
  }
  .identity-left {
    gap: 12px;
    align-items: center;
  }
  .card-content {
    gap: 8px;
    align-items: center;
  }
  .quota-card {
    padding: 16px 18px;
  }
  .quota-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
  .quota-icon-wrap {
    width: 56px;
    height: 56px;
    border-radius: 12px;
  }
  .quota-number {
    font-size: 20px;
  }
  .stat-card {
    padding: 16px 18px;
  }
  .stat-icon {
    width: 56px;
    height: 56px;
  }
  .stat-value {
    font-size: 20px;
  }
  .welcome-text {
    font-size: 13px;
  }
  .user-name {
    font-size: 19px;
  }
  .vip-expire-time {
    font-size: 15px;
  }
  .identity-card .avatar-wrapper.avatar-lg,
  .identity-card .avatar-ring.avatar-lg {
    width: 58px;
    height: 58px;
  }
  .identity-card .avatar-img.avatar-lg {
    width: 50px;
    height: 50px;
  }
  .record-column {
    padding: 16px;
  }

  /* lg 图标在平板端 */
  .quota-icon-wrap :deep(.feature-icon.size-lg) {
    width: 48px;
    height: 48px;
  }
  .stat-icon :deep(.feature-icon.size-lg) {
    width: 48px;
    height: 48px;
  }
  .growth-entry-icon :deep(.feature-icon.size-lg) {
    width: 48px;
    height: 48px;
  }
}

/* 超小屏：≤767px - 手机，身份卡+配额上下排，其余保持左右 */
@media (max-width: 767px) {
  /* lg 图标 */
  .quota-icon-wrap :deep(.feature-icon.size-lg) {
    width: 42px;
    height: 42px;
  }
  .stat-icon :deep(.feature-icon.size-lg) {
    width: 42px;
    height: 42px;
  }
  .growth-entry-icon :deep(.feature-icon.size-lg) {
    width: 42px;
    height: 42px;
  }
  .dashboard-skeleton {
    padding: 12px;
  }

  .top-section {
    grid-template-columns: 1fr;
    gap: 12px;
    margin-bottom: 16px;
  }

  .stats-section {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    margin-bottom: 16px;
  }

  .stat-card {
    padding: 12px;
    gap: 10px;
    border-radius: 12px;
  }
  .stat-icon {
    width: 52px;
    height: 52px;
  }
  .stat-value {
    font-size: 18px;
  }
  .stat-label {
    font-size: 11px;
  }

  .identity-card {
    padding: 14px;
    border-radius: 14px;
    display: flex;
    align-items: center;
  }
  .identity-left {
    gap: 12px;
    align-items: center;
  }
  .identity-card .avatar-wrapper.avatar-lg,
  .identity-card .avatar-ring.avatar-lg {
    width: 52px;
    height: 52px;
  }
  .identity-card .avatar-img.avatar-lg {
    width: 44px;
    height: 44px;
  }
  .user-name {
    font-size: 17px;
  }
  .vip-expire-time {
    font-size: 14px;
  }
  .welcome-text {
    font-size: 12px;
  }

  .quota-card {
    padding: 14px;
    border-radius: 14px;
  }
  .quota-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px 12px;
  }
  .quota-cell {
    gap: 10px;
  }
  .quota-icon-wrap {
    width: 50px;
    height: 50px;
    border-radius: 10px;
  }
  .quota-number {
    font-size: 18px;
  }
  .quota-label {
    font-size: 11px;
  }

  .growth-entry-card {
    padding: 14px 16px;
    gap: 12px;
    margin-bottom: 16px;
    border-radius: 12px;
  }
  .growth-entry-icon {
    width: 56px;
    height: 56px;
  }
  .growth-entry-title {
    font-size: 14px;
  }
  .growth-entry-desc {
    font-size: 12px;
  }

  .records-section {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .record-column {
    padding: 14px;
    border-radius: 12px;
  }
  .column-header {
    margin-bottom: 12px;
    padding-bottom: 12px;
  }
  .column-title {
    font-size: 14px;
  }

  /* 防止记录条目右侧溢出 */
  .record-item {
    padding: 10px 10px;
    gap: 8px;
    border-radius: 8px;
  }
  .record-left {
    gap: 8px;
    min-width: 0;
    flex: 1;
    overflow: hidden;
  }
  .file-icon,
  .interview-icon-wrap {
    flex-shrink: 0;
  }
  .file-icon svg,
  .interview-icon-wrap svg {
    width: 16px;
    height: 16px;
  }
  .record-name {
    font-size: 13px;
    max-width: 100%;
  }
  .record-time {
    font-size: 11px;
  }
  .record-info {
    overflow: hidden;
  }
  .record-status-badge {
    font-size: 11px;
    padding: 3px 8px;
    flex-shrink: 0;
  }
  .record-score-tag {
    padding: 3px 8px;
    flex-shrink: 0;
  }
  .score-value {
    font-size: 13px;
  }
  .score-unit {
    font-size: 11px;
  }

  .empty-state {
    padding: 20px 0;
  }
}

/* 极小屏手机：≤380px */
@media (max-width: 380px) {
  .stats-section {
    grid-template-columns: 1fr 1fr;
    gap: 6px;
  }

  .stat-card {
    padding: 10px;
    gap: 8px;
  }
  .stat-icon {
    width: 48px;
    height: 48px;
  }
  .stat-value {
    font-size: 16px;
  }

  .quota-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px 10px;
  }
  .quota-icon-wrap {
    width: 44px;
    height: 44px;
  }
  .quota-number {
    font-size: 16px;
  }

  .growth-entry-icon {
    width: 48px;
    height: 48px;
  }

  .quota-icon-wrap :deep(.feature-icon.size-lg) {
    width: 36px;
    height: 36px;
  }
  .stat-icon :deep(.feature-icon.size-lg) {
    width: 36px;
    height: 36px;
  }
  .growth-entry-icon :deep(.feature-icon.size-lg) {
    width: 36px;
    height: 36px;
  }

  .record-column {
    padding: 10px;
  }
  .record-item {
    padding: 8px;
  }
  .record-name {
    font-size: 12px;
  }
  .record-status-badge,
  .record-score-tag {
    padding: 2px 6px;
    font-size: 10px;
  }
}

/* ===== 暗色模式适配 ===== */

@media (prefers-reduced-motion: reduce) {
  .stat-card,
  .growth-entry-card,
  .growth-entry-arrow,
  .view-all-btn .arrow-icon,
  .record-item {
    transition-duration: 0.01ms;
  }

  .stat-card:hover,
  .growth-entry-card:hover,
  .growth-entry-card:hover .growth-entry-arrow,
  .view-all-btn:hover .arrow-icon {
    transform: none;
  }
}
</style>
