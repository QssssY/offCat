<template>
  <div class="admin-page">
    <section class="page-header">
      <div>
        <h2 class="page-title">监控总览</h2>
        <p class="page-subtitle">
          基于应用层统计展示简历任务与面试会话运行状态，便于快速定位异常趋势
        </p>
      </div>
      <el-button
        :loading="loading"
        @click="loadMonitorOverview"
        class="refresh-btn"
      >
        <el-icon><Refresh /></el-icon>
        刷新监控
      </el-button>
    </section>

    <el-alert
      v-if="errorMessage"
      class="monitor-error"
      type="error"
      :closable="false"
      :title="errorMessage"
    />

    <el-alert
      v-else-if="!loading && !hasMonitorData"
      class="monitor-empty"
      type="info"
      :closable="false"
      title="当前暂无监控数据，请稍后刷新或等待业务请求产生。"
    />

    <section
      v-for="section in metricSections"
      :key="section.title"
      class="monitor-section"
    >
      <div class="section-header">
        <h3 class="section-title">{{ section.title }}</h3>
      </div>
      <div class="monitor-grid monitor-grid--four">
        <article
          v-for="item in section.items"
          :key="item.label"
          class="monitor-card"
        >
          <div class="card-icon" :class="item.iconClass">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="card-content">
            <div class="label">{{ item.label }}</div>
            <div class="value" :class="{ danger: item.danger }">
              {{ item.value }}
            </div>
            <div v-if="item.detail" class="detail">{{ item.detail }}</div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  Calendar,
  ChatDotRound,
  Document,
  Files,
  Loading,
  Refresh,
  WarningFilled,
} from "@element-plus/icons-vue";
import { getAdminMonitorOverview } from "@/api/admin/monitor";

const loading = ref(false);
const errorMessage = ref("");

// 监控总览状态：与 /api/admin/monitor/overview 字段一一对应，作为页面唯一数据源。
const monitorOverview = reactive({
  pendingResumeTaskCount: 0,
  processingResumeTaskCount: 0,
  failedResumeTaskCount: 0,
  completedResumeTaskCount: 0,
  activeInterviewSessionCount: 0,
  todayInterviewSessionCount: 0,
  todayResumeDiagnosisCount: 0,
  todayResumePolishCount: 0,
  todayJobMatchCount: 0,
  todayCommunityPostCount: 0,
  pendingFeedbackCount: 0,
  processingFeedbackCount: 0,
  todayFeedbackCount: 0,
  pendingCommunityPostCount: 0,
  pendingCommunityCommentCount: 0,
  pendingCommunityReviewCount: 0,
  todayOrderCount: 0,
});

const toMonitorNumber = (value) => Number(value ?? 0);

// 监控区块：按“运行态 / 今日业务量 / 待处理事项”组织，便于管理员先定位异常再处理待办。
const metricSections = computed(() => [
  {
    title: "简历任务运行态",
    items: [
      {
        label: "待处理简历任务",
        value: monitorOverview.pendingResumeTaskCount,
        icon: Document,
        iconClass: "resume-pending",
      },
      {
        label: "处理中简历任务",
        value: monitorOverview.processingResumeTaskCount,
        icon: Loading,
        iconClass: "resume-processing",
      },
      {
        label: "失败简历任务",
        value: monitorOverview.failedResumeTaskCount,
        icon: WarningFilled,
        iconClass: "resume-failed",
        danger: true,
      },
      {
        label: "已完成简历任务",
        value: monitorOverview.completedResumeTaskCount,
        icon: Files,
        iconClass: "resume-completed",
      },
    ],
  },
  {
    title: "今日业务量",
    items: [
      {
        label: "面试会话",
        value: monitorOverview.todayInterviewSessionCount,
        icon: Calendar,
        iconClass: "interview-today",
      },
      {
        label: "简历诊断",
        value: monitorOverview.todayResumeDiagnosisCount,
        icon: Files,
        iconClass: "resume-today",
      },
      {
        label: "AI 简历润色",
        value: monitorOverview.todayResumePolishCount,
        icon: Document,
        iconClass: "resume-polish",
      },
      {
        label: "JD 匹配分析",
        value: monitorOverview.todayJobMatchCount,
        icon: ChatDotRound,
        iconClass: "job-match",
      },
      {
        label: "社区发帖",
        value: monitorOverview.todayCommunityPostCount,
        icon: ChatDotRound,
        iconClass: "community-post",
      },
      {
        label: "用户反馈",
        value: monitorOverview.todayFeedbackCount,
        icon: WarningFilled,
        iconClass: "feedback-today",
      },
      {
        label: "今日订单",
        value: monitorOverview.todayOrderCount,
        icon: Calendar,
        iconClass: "order-today",
      },
    ],
  },
  {
    title: "待处理事项",
    items: [
      {
        label: "活跃面试会话",
        value: monitorOverview.activeInterviewSessionCount,
        icon: ChatDotRound,
        iconClass: "interview-active",
      },
      {
        label: "反馈待处理",
        value: monitorOverview.pendingFeedbackCount,
        icon: Document,
        iconClass: "feedback-pending",
      },
      {
        label: "反馈处理中",
        value: monitorOverview.processingFeedbackCount,
        icon: Loading,
        iconClass: "feedback-processing",
      },
      {
        label: "社区待审总数",
        value: monitorOverview.pendingCommunityReviewCount,
        icon: WarningFilled,
        iconClass: "community-review",
        detail: `待审帖子 ${monitorOverview.pendingCommunityPostCount} / 待审评论 ${monitorOverview.pendingCommunityCommentCount}`,
      },
    ],
  },
]);

/**
 * 监控数据是否为空。
 * 作用：在“接口成功但暂无业务量”时给出明确空状态提示，避免用户误判为加载失败。
 */
const hasMonitorData = computed(() => {
  return Object.values(monitorOverview).some((value) => Number(value) > 0)
})

/**
 * 加载监控总览数据。
 * 作用：统一处理加载状态、接口异常和数据回填，保证页面可观测性稳定。
 */
const loadMonitorOverview = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const res = await getAdminMonitorOverview();
    const d = res?.data || {};
    monitorOverview.pendingResumeTaskCount = toMonitorNumber(d.pendingResumeTaskCount);
    monitorOverview.processingResumeTaskCount = toMonitorNumber(d.processingResumeTaskCount);
    monitorOverview.failedResumeTaskCount = toMonitorNumber(d.failedResumeTaskCount);
    monitorOverview.completedResumeTaskCount = toMonitorNumber(d.completedResumeTaskCount);
    monitorOverview.activeInterviewSessionCount = toMonitorNumber(d.activeInterviewSessionCount);
    monitorOverview.todayInterviewSessionCount = toMonitorNumber(d.todayInterviewSessionCount);
    monitorOverview.todayResumeDiagnosisCount = toMonitorNumber(d.todayResumeDiagnosisCount);
    monitorOverview.todayResumePolishCount = toMonitorNumber(d.todayResumePolishCount);
    monitorOverview.todayJobMatchCount = toMonitorNumber(d.todayJobMatchCount);
    monitorOverview.todayCommunityPostCount = toMonitorNumber(d.todayCommunityPostCount);
    monitorOverview.pendingFeedbackCount = toMonitorNumber(d.pendingFeedbackCount);
    monitorOverview.processingFeedbackCount = toMonitorNumber(d.processingFeedbackCount);
    monitorOverview.todayFeedbackCount = toMonitorNumber(d.todayFeedbackCount);
    monitorOverview.pendingCommunityPostCount = toMonitorNumber(d.pendingCommunityPostCount);
    monitorOverview.pendingCommunityCommentCount = toMonitorNumber(d.pendingCommunityCommentCount);
    monitorOverview.pendingCommunityReviewCount = toMonitorNumber(d.pendingCommunityReviewCount);
    monitorOverview.todayOrderCount = toMonitorNumber(d.todayOrderCount);
  } catch (error) {
    errorMessage.value = error?.message || "加载监控总览失败";
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadMonitorOverview();
});
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 100%);
  border: 1px solid rgba(230, 126, 34, 0.12);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.08);
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #a08060;
  font-size: 14px;
}

.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3);
  color: var(--bg-card);
}

.refresh-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(230, 126, 34, 0.4);
}

.monitor-error {
  margin-bottom: 2px;
}

.monitor-empty {
  margin-bottom: 2px;
}

.monitor-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  margin: 0;
  color: #5a4030;
  font-size: 17px;
  font-weight: 700;
}

.monitor-grid {
  display: grid;
  gap: 16px;
}

.monitor-grid--four {
  /* 桌面端固定四列，避免宽屏把“今日业务量”自动挤成六列。 */
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.monitor-card {
  background: var(--bg-card);
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 116px;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
  box-shadow: 0 6px 20px rgba(143, 69, 27, 0.06);
}

.monitor-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(143, 69, 27, 0.12);
  border-color: rgba(230, 126, 34, 0.35);
}

.card-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.card-icon.resume-pending {
  background: linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%);
  color: #d35400;
}

.card-icon.resume-processing {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  color: var(--bg-card);
}

.card-icon.resume-failed {
  background: linear-gradient(135deg, #ff7675 0%, #d63031 100%);
  color: var(--bg-card);
}

.card-icon.resume-completed {
  background: linear-gradient(135deg, #c8f7c5 0%, #27ae60 100%);
  color: var(--bg-card);
}

.card-icon.interview-active {
  background: linear-gradient(135deg, #55efc4 0%, #00b894 100%);
  color: var(--bg-card);
}

.card-icon.interview-today {
  background: linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%);
  color: var(--bg-card);
}

.card-icon.resume-today {
  background: linear-gradient(135deg, #81ecec 0%, #00cec9 100%);
  color: var(--bg-card);
}

.card-icon.resume-polish {
  background: linear-gradient(135deg, #fab1a0 0%, #e17055 100%);
  color: var(--bg-card);
}

.card-icon.job-match {
  background: linear-gradient(135deg, #fdcb6e 0%, #e17055 100%);
  color: var(--bg-card);
}

.card-icon.community-post {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  color: var(--bg-card);
}

.card-icon.feedback-today,
.card-icon.feedback-pending,
.card-icon.feedback-processing {
  background: linear-gradient(135deg, #ffeaa7 0%, #f39c12 100%);
  color: #7a3f00;
}

.card-icon.order-today {
  background: linear-gradient(135deg, #dfe6e9 0%, #636e72 100%);
  color: var(--bg-card);
}

.card-icon.community-review {
  background: linear-gradient(135deg, #ff7675 0%, #c0392b 100%);
  color: var(--bg-card);
}

.card-content {
  flex: 1;
  min-width: 0;
}

.label {
  font-size: 14px;
  color: #a08060;
  font-weight: 600;
}

.value {
  margin-top: 10px;
  font-size: 32px;
  font-weight: 700;
  color: #5a4030;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.value.danger {
  color: #d63031;
}

.detail {
  margin-top: 8px;
  color: #a08060;
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 1180px) {
  .monitor-grid--four {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .monitor-grid--four {
    grid-template-columns: 1fr;
  }

  .monitor-card {
    padding: 18px;
  }

  .card-icon {
    width: 52px;
    height: 52px;
    font-size: 22px;
  }

  .value {
    font-size: 26px;
  }
}
</style>
