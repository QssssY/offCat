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

    <section class="monitor-grid">
      <article class="monitor-card">
        <div class="card-icon resume-pending">
          <el-icon><Document /></el-icon>
        </div>
        <div class="card-content">
          <div class="label">待处理简历任务</div>
          <div class="value">{{ monitorOverview.pendingResumeTaskCount }}</div>
        </div>
      </article>
      <article class="monitor-card">
        <div class="card-icon resume-processing">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="card-content">
          <div class="label">处理中简历任务</div>
          <div class="value">
            {{ monitorOverview.processingResumeTaskCount }}
          </div>
        </div>
      </article>
      <article class="monitor-card">
        <div class="card-icon resume-failed">
          <el-icon><WarningFilled /></el-icon>
        </div>
        <div class="card-content">
          <div class="label">失败简历任务</div>
          <div class="value danger">
            {{ monitorOverview.failedResumeTaskCount }}
          </div>
        </div>
      </article>
      <article class="monitor-card">
        <div class="card-icon interview-active">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="card-content">
          <div class="label">活跃面试会话</div>
          <div class="value">
            {{ monitorOverview.activeInterviewSessionCount }}
          </div>
        </div>
      </article>
      <article class="monitor-card">
        <div class="card-icon interview-today">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="card-content">
          <div class="label">面试会话</div>
          <div class="value">
            {{ monitorOverview.todayInterviewSessionCount }}
          </div>
        </div>
      </article>
      <article class="monitor-card">
        <div class="card-icon resume-today">
          <el-icon><Files /></el-icon>
        </div>
        <div class="card-content">
          <div class="label">简历诊断</div>
          <div class="value">
            {{ monitorOverview.todayResumeDiagnosisCount }}
          </div>
        </div>
      </article>
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
  activeInterviewSessionCount: 0,
  todayInterviewSessionCount: 0,
  todayResumeDiagnosisCount: 0,
});

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
    monitorOverview.pendingResumeTaskCount = Number(d.pendingResumeTaskCount ?? 0);
    monitorOverview.processingResumeTaskCount = Number(d.processingResumeTaskCount ?? 0);
    monitorOverview.failedResumeTaskCount = Number(d.failedResumeTaskCount ?? 0);
    monitorOverview.activeInterviewSessionCount = Number(d.activeInterviewSessionCount ?? 0);
    monitorOverview.todayInterviewSessionCount = Number(d.todayInterviewSessionCount ?? 0);
    monitorOverview.todayResumeDiagnosisCount = Number(d.todayResumeDiagnosisCount ?? 0);
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

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 16px;
}

.monitor-card {
  background: var(--bg-card);
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 18px;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 18px;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
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

.card-content {
  flex: 1;
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
}

.value.danger {
  color: #d63031;
}

@media (max-width: 1200px) {
  .monitor-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .monitor-grid {
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
