<template>
  <div class="admin-dashboard-view">
    <section class="dashboard-header">
      <div>
        <h1 class="dashboard-title">数据看板</h1>
        <p class="dashboard-subtitle">
          支持日期范围和热门岗位数量筛选，统一刷新看板数据
        </p>
      </div>
      <el-button :loading="loading" @click="loadDashboardData" class="refresh-btn">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </section>

    <section class="filter-panel">
      <el-radio-group
        v-model="filters.quickRange"
        @change="handleQuickRangeChange"
      >
        <el-radio-button value="today">今天</el-radio-button>
        <el-radio-button value="last7">近7天</el-radio-button>
        <el-radio-button value="last30">近30天</el-radio-button>
        <el-radio-button value="custom">自定义</el-radio-button>
      </el-radio-group>

      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        :clearable="true"
        :disabled="filters.quickRange !== 'custom'"
      />

      <el-input-number
        v-model="filters.hotLimit"
        :min="1"
        :max="50"
        :step="1"
      />
      <el-button type="primary" :loading="loading" @click="handleApplyFilters"
        >应用筛选</el-button
      >
      <el-button @click="resetFilters">重置筛选</el-button>
    </section>

    <section class="filter-summary">
      <span>当前统计区间：{{ filterSummary.dateRangeText }}</span>
      <span>热门岗位数量：Top {{ filters.hotLimit }}</span>
    </section>

    <el-alert
      v-if="errorMessage"
      class="dashboard-error"
      type="error"
      :closable="false"
      :title="errorMessage"
    />

    <section class="overview-grid">
      <article class="overview-card">
        <div class="label">总用户数</div>
        <div class="value">{{ overview.totalUserCount }}</div>
      </article>
      <article class="overview-card">
        <div class="label">会员用户数</div>
        <div class="value">{{ overview.vipUserCount }}</div>
      </article>
      <article class="overview-card">
        <div class="label">启用 Prompt 数</div>
        <div class="value">{{ overview.activePromptCount }}</div>
      </article>
      <article class="overview-card">
        <div class="label">启用岗位数</div>
        <div class="value">{{ overview.activeJobRoleCount }}</div>
      </article>
      <article class="overview-card">
        <div class="label">启用 AI 引擎数</div>
        <div class="value">{{ overview.activeAiEngineCount }}</div>
      </article>
      <article class="overview-card">
        <div class="label">面试会话</div>
        <div class="value">{{ overview.todayInterviewSessionCount }}</div>
      </article>
      <article class="overview-card">
        <div class="label">简历诊断</div>
        <div class="value">{{ overview.todayResumeDiagnosisCount }}</div>
      </article>
    </section>

    <section class="middle-grid">
      <article class="panel-card">
        <header class="panel-title">
          热门岗位排行（Top {{ filters.hotLimit }}）
        </header>
        <div class="chart-box-wrap">
          <Bar
            v-if="hasHotRoleData"
            :data="hotRoleChartData"
            :options="hotRoleChartOptions"
            :height="260"
          />
          <div v-else class="empty-chart-tip">暂无岗位排行数据</div>
        </div>
      </article>

      <article class="panel-card">
        <header class="panel-title">业务分布</header>
        <div class="distribution-range">
          统计区间：{{ businessDistribution.startDate || "-" }} ~
          {{ businessDistribution.endDate || "-" }}
        </div>
        <div class="chart-box-wrap">
          <Doughnut
            v-if="hasDistributionData"
            :data="distributionChartData"
            :options="distributionChartOptions"
            :height="260"
          />
          <div v-else class="empty-chart-tip">暂无业务分布数据</div>
        </div>
      </article>
    </section>

    <section class="trend-section panel-card">
      <header class="panel-title">趋势数据</header>
      <div class="chart-box-wrap trend-wrap">
        <Line
          v-if="hasTrendData"
          :data="trendChartData"
          :options="trendChartOptions"
          :height="340"
        />
        <div v-else class="empty-chart-tip">暂无趋势数据</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import { Line, Bar, Doughnut } from "vue-chartjs";
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  BarElement,
  ArcElement,
  Filler,
} from "chart.js";

// 注册 Chart.js 所需模块
ChartJS.register(
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  LineElement,
  PointElement,
  BarElement,
  ArcElement,
  Filler
);

import {
  getAdminDashboardBusinessDistribution,
  getAdminDashboardHotJobRoles,
  getAdminDashboardOverview,
  getAdminDashboardTrends,
} from "@/api/admin/dashboard";

const loading = ref(false);
const errorMessage = ref("");

const getDefaultDateRange = () => {
  const today = new Date();
  const toFF = (d) => {
    const y = d.getFullYear();
    const m = `${d.getMonth() + 1}`.padStart(2, "0");
    const day = `${d.getDate()}`.padStart(2, "0");
    return `${y}-${m}-${day}`;
  };
  const endDate = toFF(today);
  const start = new Date(today);
  start.setDate(start.getDate() - 6);
  return [toFF(start), endDate];
};

// 看板筛选状态
const filters = reactive({
  quickRange: "last7",
  dateRange: getDefaultDateRange(),
  hotLimit: 10,
});

// 总览卡片数据
const overview = reactive({
  totalUserCount: 0,
  vipUserCount: 0,
  activePromptCount: 0,
  activeJobRoleCount: 0,
  activeAiEngineCount: 0,
  todayInterviewSessionCount: 0,
  todayResumeDiagnosisCount: 0,
});

const trends = ref([]);
const hotJobRoles = ref([]);

const businessDistribution = reactive({
  startDate: "",
  endDate: "",
  interviewCount: 0,
  resumeCount: 0,
  totalCount: 0,
  interviewPercent: 0,
  resumePercent: 0,
});

// 趋势图系列名称
const TREND_SERIES_NAMES = ["面试会话", "简历诊断"];

/**
 * 趋势数据归一化
 */
const normalizedTrendRows = computed(() => {
  if (!Array.isArray(trends.value)) return [];
  return trends.value
    .filter((item) => item && typeof item === "object")
    .map((item, index) => ({
      date: item.date ? String(item.date) : `第${index + 1}项`,
      interviewSessionCount: Number(item.interviewSessionCount ?? 0),
      resumeDiagnosisCount: Number(item.resumeDiagnosisCount ?? 0),
    }));
});

const hasTrendData = computed(() => normalizedTrendRows.value.length > 0);
const hasHotRoleData = computed(
  () => Array.isArray(hotJobRoles.value) && hotJobRoles.value.length > 0
);
const hasDistributionData = computed(
  () => Number(businessDistribution.totalCount || 0) > 0
);

/**
 * ==============================
 * 趋势图配置（Chart.js Line）
 * ==============================
 */
const trendChartData = computed(() => ({
  labels: normalizedTrendRows.value.map((item) => item.date),
  datasets: [
    {
      label: TREND_SERIES_NAMES[0],
      data: normalizedTrendRows.value.map((item) => item.interviewSessionCount),
      borderColor: "#ff8c42",
      backgroundColor: "rgba(255, 140, 66, 0.16)",
      fill: true,
      tension: 0.4,
      pointRadius: 4,
      pointHoverRadius: 6,
    },
    {
      label: TREND_SERIES_NAMES[1],
      data: normalizedTrendRows.value.map((item) => item.resumeDiagnosisCount),
      borderColor: "#f6b37d",
      backgroundColor: "rgba(246, 179, 125, 0.12)",
      fill: true,
      tension: 0.4,
      pointRadius: 4,
      pointHoverRadius: 6,
    },
  ],
}));

const trendChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  animation: { duration: 600 },
  plugins: {
    legend: {
      position: "top",
      labels: { color: "#8f572f", font: { size: 12 } },
    },
    tooltip: { mode: "index", intersect: false },
  },
  scales: {
    x: {
      ticks: { color: "#9a633e" },
      grid: { color: "rgba(246, 224, 207, 0.4)" },
    },
    y: {
      beginAtZero: true,
      ticks: { color: "#9a633e" },
      grid: { color: "#f6e0cf" },
    },
  },
}));

/**
 * ==============================
 * 热门岗位图配置（Chart.js Bar）
 * ==============================
 */
const hotRoleChartData = computed(() => {
  const sorted = [...hotJobRoles.value].reverse();
  return {
    labels: sorted.map((item) => item.jobRole || "未命名岗位"),
    datasets: [
      {
        label: "会话数",
        data: sorted.map((item) => Number(item.sessionCount || 0)),
        backgroundColor: "#ff8f42",
        borderRadius: 6,
        barThickness: 16,
      },
    ],
  };
});

const hotRoleChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: "y",
  animation: { duration: 600 },
  plugins: {
    legend: { display: false },
    tooltip: { mode: "index", intersect: false },
  },
  scales: {
    x: {
      beginAtZero: true,
      ticks: { color: "#9a633e" },
      grid: { color: "#f6e0cf" },
    },
    y: {
      ticks: { color: "#8f572f", font: { size: 12 } },
      grid: { display: false },
    },
  },
}));

/**
 * ==============================
 * 业务分布图配置（Chart.js Doughnut）
 * ==============================
 */
const distributionChartData = computed(() => ({
  labels: ["面试", "简历"],
  datasets: [
    {
      data: [
        Number(businessDistribution.interviewCount || 0),
        Number(businessDistribution.resumeCount || 0),
      ],
      backgroundColor: ["#ff8f42", "#f6b37d"],
      borderWidth: 2,
      borderColor: "#fff",
    },
  ],
}));

const distributionChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: "55%",
  animation: { duration: 600 },
  plugins: {
    legend: {
      position: "bottom",
      labels: { color: "#8f572f", font: { size: 12 } },
    },
    tooltip: {
      callbacks: {
        label: (ctx) => {
          const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
          const pct = total > 0 ? ((ctx.raw / total) * 100).toFixed(0) : 0;
          return `${ctx.label}: ${ctx.raw}（${pct}%）`;
        },
      },
    },
  },
}));

/**
 * ==============================
 * 日期工具函数
 * ==============================
 */
const formatDate = (date) => {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const applyQuickRange = (quickRange) => {
  const today = new Date();
  const endDate = formatDate(today);
  if (quickRange === "today") {
    filters.dateRange = [endDate, endDate];
    return;
  }
  if (quickRange === "last7") {
    const start = new Date(today);
    start.setDate(start.getDate() - 6);
    filters.dateRange = [formatDate(start), endDate];
    return;
  }
  if (quickRange === "last30") {
    const start = new Date(today);
    start.setDate(start.getDate() - 29);
    filters.dateRange = [formatDate(start), endDate];
    return;
  }
};

const filterSummary = computed(() => {
  if (!Array.isArray(filters.dateRange) || filters.dateRange.length !== 2) {
    return { dateRangeText: "近7天" };
  }
  return { dateRangeText: `${filters.dateRange[0]} ~ ${filters.dateRange[1]}` };
});

const dateParams = computed(() => {
  if (!Array.isArray(filters.dateRange) || filters.dateRange.length !== 2) {
    return {};
  }
  return {
    startDate: filters.dateRange[0],
    endDate: filters.dateRange[1],
  };
});

const validateFilters = () => {
  if (!Number.isInteger(filters.hotLimit) || filters.hotLimit <= 0) {
    ElMessage.warning("热门岗位数量必须大于 0");
    return false;
  }
  if (filters.quickRange === "custom") {
    if (!Array.isArray(filters.dateRange) || filters.dateRange.length !== 2) {
      ElMessage.warning("请选择完整的自定义日期范围");
      return false;
    }
  }
  if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
    const start = new Date(filters.dateRange[0]);
    const end = new Date(filters.dateRange[1]);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      ElMessage.warning("日期格式无效，请重新选择");
      return false;
    }
    if (start.getTime() > end.getTime()) {
      ElMessage.warning("开始日期不能大于结束日期");
      return false;
    }
    const dayDiff =
      Math.floor((end.getTime() - start.getTime()) / 86400000) + 1;
    if (dayDiff > 90) {
      ElMessage.warning("查询范围不能超过 90 天");
      return false;
    }
  }
  return true;
};

/**
 * ==============================
 * 数据加载
 * ==============================
 */
const loadDashboardData = async () => {
  if (!validateFilters()) return;
  loading.value = true;
  errorMessage.value = "";
  const commonParams = { ...dateParams.value };

  try {
    const [overviewRes, trendsRes, hotRolesRes, distributionRes] =
      await Promise.all([
        getAdminDashboardOverview(commonParams),
        getAdminDashboardTrends(commonParams),
        getAdminDashboardHotJobRoles({
          ...commonParams,
          limit: filters.hotLimit,
        }),
        getAdminDashboardBusinessDistribution(commonParams),
      ]);

    const od = overviewRes?.data || {};
    overview.totalUserCount = Number(od.totalUserCount ?? 0);
    overview.vipUserCount = Number(od.vipUserCount ?? 0);
    overview.activePromptCount = Number(od.activePromptCount ?? 0);
    overview.activeJobRoleCount = Number(od.activeJobRoleCount ?? 0);
    overview.activeAiEngineCount = Number(od.activeAiEngineCount ?? 0);
    overview.todayInterviewSessionCount = Number(od.todayInterviewSessionCount ?? 0);
    overview.todayResumeDiagnosisCount = Number(od.todayResumeDiagnosisCount ?? 0);
    trends.value = Array.isArray(trendsRes?.data) ? trendsRes.data : [];
    hotJobRoles.value = Array.isArray(hotRolesRes?.data)
      ? hotRolesRes.data
      : [];
    Object.assign(businessDistribution, distributionRes?.data || {});
  } catch (error) {
    errorMessage.value = error?.message || "加载管理端看板数据失败";
  } finally {
    loading.value = false;
  }
};

const handleQuickRangeChange = (quickRange) => {
  applyQuickRange(quickRange);
};

const handleApplyFilters = () => {
  loadDashboardData();
};

const resetFilters = () => {
  filters.quickRange = "last7";
  filters.dateRange = getDefaultDateRange();
  filters.hotLimit = 10;
  loadDashboardData();
};

onMounted(() => {
  loadDashboardData();
});
</script>

<style scoped>
.admin-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-header {
  background: linear-gradient(135deg, #fffcf8 0%, #fff8f3 50%, #ffefe0 100%);
  border: 1px solid rgba(230, 126, 34, 0.15);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.08);
  position: relative;
  overflow: hidden;
}

.dashboard-header::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 200px;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 140, 66, 0.05));
  pointer-events: none;
}

.dashboard-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dashboard-subtitle {
  margin: 6px 0 0;
  color: #a08060;
  font-size: 14px;
}

.filter-panel {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(230, 126, 34, 0.1);
  border-radius: 14px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.06);
}

.filter-panel :deep(.el-radio-button__inner) {
  border-radius: 10px;
  font-weight: 500;
}

.filter-panel :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border-color: #e67e22;
  box-shadow: 0 2px 8px rgba(230, 126, 34, 0.3);
}

.filter-panel :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(217, 180, 154, 0.4);
}

.filter-panel :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(230, 126, 34, 0.3);
}

.filter-panel :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(230, 126, 34, 0.15), 0 0 0 1px #e67e22;
}

.filter-panel :deep(.el-input-number) {
  width: 100px;
}

.filter-panel :deep(.el-input-number .el-input__wrapper) {
  border-radius: 10px;
}

.filter-summary {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  color: #8f572f;
  font-size: 13px;
  background: linear-gradient(135deg, #fff8f3 0%, #fff3e8 100%);
  border: 1px solid rgba(230, 126, 34, 0.1);
  border-radius: 12px;
  padding: 12px 18px;
  font-weight: 500;
}

.dashboard-error {
  margin-bottom: 2px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 16px;
}

.overview-card {
  background: var(--bg-card);
  border: 1px solid rgba(217, 196, 170, 0.3);
  border-radius: 16px;
  padding: 22px;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.06);
  position: relative;
  overflow: hidden;
}

.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #e67e22, #f5a623);
  opacity: 0;
  transition: opacity 0.35s ease;
}

.overview-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(143, 69, 27, 0.12);
  border-color: rgba(230, 126, 34, 0.3);
}

.overview-card:hover::before {
  opacity: 1;
}

.label {
  font-size: 13px;
  color: #a08060;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.value {
  margin-top: 10px;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #5a4030 0%, #8f451b 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.panel-card {
  background: var(--bg-card);
  border: 1px solid rgba(217, 196, 170, 0.25);
  border-radius: 16px;
  padding: 22px;
  box-shadow: 0 4px 20px rgba(143, 69, 27, 0.06);
}

.middle-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.trend-section {
  width: 100%;
}

.panel-title {
  font-size: 17px;
  font-weight: 700;
  color: #5a4030;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 2px solid rgba(230, 126, 34, 0.1);
  position: relative;
}

.panel-title::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, #e67e22, #f5a623);
  border-radius: 1px;
}

.distribution-range {
  color: #a08060;
  font-size: 13px;
  margin-bottom: 14px;
  font-weight: 500;
}

.chart-box-wrap {
  position: relative;
  min-height: 260px;
}

.trend-wrap {
  min-height: 340px;
}

.empty-chart-tip {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #c4a888;
  pointer-events: none;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(160px, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }
  .middle-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .dashboard-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
  .overview-grid {
    grid-template-columns: 1fr;
  }
  .filter-panel {
    flex-direction: column;
    align-items: stretch;
  }
}

.refresh-btn {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  border-radius: 12px;
  color: var(--bg-card);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  box-shadow: 0 4px 16px rgba(230, 126, 34, 0.25);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.refresh-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(230, 126, 34, 0.35);
}

.refresh-btn :deep(.el-icon) {
  font-size: 16px;
}
</style>
