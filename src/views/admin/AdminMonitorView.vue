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
    Object.assign(monitorOverview, res?.data || {});
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
  gap: 20px;
}

.page-header {
  background: linear-gradient(135deg, #fff5e6 0%, #fff 100%);
  border: 1px solid #f2d4be;
  border-radius: 14px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(230, 126, 34, 0.1);
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #2c3e50;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #7f8c8d;
  font-size: 14px;
}

.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(230, 126, 34, 0.3);
  color: #fff;
}

.refresh-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.4);
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
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.monitor-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-color: #e67e22;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.card-icon.resume-pending {
  background: linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%);
  color: #d35400;
}

.card-icon.resume-processing {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  color: #fff;
}

.card-icon.resume-failed {
  background: linear-gradient(135deg, #ff7675 0%, #d63031 100%);
  color: #fff;
}

.card-icon.interview-active {
  background: linear-gradient(135deg, #55efc4 0%, #00b894 100%);
  color: #fff;
}

.card-icon.interview-today {
  background: linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%);
  color: #fff;
}

.card-icon.resume-today {
  background: linear-gradient(135deg, #81ecec 0%, #00cec9 100%);
  color: #fff;
}

.card-content {
  flex: 1;
}

.label {
  font-size: 14px;
  color: #7f8c8d;
  font-weight: 500;
}

.value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
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
    padding: 16px;
  }

  .card-icon {
    width: 48px;
    height: 48px;
    font-size: 20px;
  }

  .value {
    font-size: 24px;
  }
}
</style>
