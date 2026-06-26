<template>
  <div class="interview-history-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">面试历史</h1>
        <p class="page-desc">查看你的模拟面试记录与岗位定向面试结果</p>
      </div>
      <el-button
        v-if="total > 0 && !loading && !error"
        class="clear-all-btn"
        text
        size="small"
        @click="handleClearAll"
      >
        <FeatureIcon name="delete" size="xs" class="button-feature-icon" />
        清空全部
      </el-button>
    </div>

    <div v-if="loading" class="loading-section">
      <div class="skeleton-list">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <el-skeleton :rows="0" animated :loading="true">
            <template #default>
              <div class="skeleton-header">
                <el-skeleton-item variant="rect" style="width: 180px; height: 24px;" />
                <el-skeleton-item variant="rect" style="width: 90px; height: 24px; margin-left: auto;" />
              </div>
              <div class="skeleton-info">
                <el-skeleton-item variant="rect" style="width: 100px; height: 16px;" />
                <el-skeleton-item variant="rect" style="width: 100px; height: 16px;" />
              </div>
              <div class="skeleton-footer">
                <el-skeleton-item variant="rect" style="width: 120px; height: 16px;" />
                <div class="skeleton-actions">
                  <el-skeleton-item variant="rect" style="width: 80px; height: 32px;" />
                  <el-skeleton-item variant="rect" style="width: 80px; height: 32px;" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>
    </div>

    <div v-else-if="error" class="error-section">
      <div class="error-card">
        <div class="error-icon">
          <FeatureIcon name="error" size="lg" />
        </div>
        <div class="error-content">
          <div class="error-title">加载失败</div>
          <div class="error-desc">{{ error }}</div>
          <div class="error-actions">
            <el-button type="primary" @click="fetchHistory">重试</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="total === 0" class="empty-section">
      <div class="empty-content">
        <InterviewEmpty :size="140" />
        <div class="empty-title">暂无面试记录</div>
        <div class="empty-desc">选择目标岗位后开始模拟面试，系统会在这里保留普通面试和岗位定向面试历史。</div>
        <el-button type="primary" class="empty-action-btn" @click="goToEntry">开始面试</el-button>
      </div>
    </div>

    <div v-else class="history-list">
      <div v-for="item in historyList" :key="item.sessionId" class="history-card">
        <div class="card-header">
          <div class="title-block">
            <div class="title-row">
              <h3 class="job-title">{{ item.jobRole || "未知岗位" }}</h3>
              <el-button
                class="card-delete"
                text
                size="small"
                @click="handleDelete(item)"
                title="删除记录"
              >
                <FeatureIcon name="delete" size="xs" />
              </el-button>
            </div>
            <div class="title-tags">
              <el-tag
                size="small"
                effect="plain"
                :type="item.interactionType === INTERACTION_TYPE_VOICE ? 'success' : 'info'"
              >
                <FeatureIcon
                  :name="item.interactionType === INTERACTION_TYPE_VOICE ? 'voice-interview' : 'message'"
                  size="xs"
                  class="tag-icon"
                />
                {{ getInteractionTypeLabel(item.interactionType) }}
              </el-tag>
              <el-tag
                v-if="item.jobTargeted"
                size="small"
                type="warning"
                effect="plain"
              >
                岗位定向
              </el-tag>
              <el-tag v-if="item.sourceTypeText" size="small" effect="plain">
                {{ item.sourceTypeText }}
              </el-tag>
            </div>
          </div>
          <el-tag :type="getStatusType(item)" size="small" class="status-tag">
            {{ getStatusText(item) }}
          </el-tag>
        </div>

        <div class="card-info">
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">难度</span>
              <el-tag :type="getDifficultyType(item)" size="small">
                {{ getDifficultyText(item) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">模式</span>
              <span class="info-value">{{ getModeText(item) }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">综合评分</span>
              <span class="info-value score" v-if="item.comprehensiveScore !== null && item.comprehensiveScore !== undefined">
                {{ item.comprehensiveScore }} 分
              </span>
              <span class="info-value" v-else>--</span>
            </div>
            <div class="info-item">
              <span class="info-label">消息数</span>
              <span class="info-value">{{ item.messageCount || 0 }}</span>
            </div>
            <div class="info-item" v-if="item.feedbackMode">
              <span class="info-label">反馈</span>
              <span class="info-value">{{ getFeedbackModeText(item) }}</span>
            </div>
          </div>
        </div>

        <div class="card-footer">
          <div class="time-info">
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="card-actions">
            <template v-if="item.status === 1">
              <el-button link type="primary" size="small" @click="viewSession(item)">查看会话</el-button>
              <el-button type="primary" size="small" @click="viewReport(item)">查看报告</el-button>
            </template>
            <template v-else>
              <el-button type="primary" size="small" @click="continueSession(item)">继续面试</el-button>
              <el-button link type="primary" size="small" @click="viewSession(item)">查看会话</el-button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > 0" class="pagination-section">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getInterviewHistory, clearInterviewHistory, deleteInterviewSession } from "@/api/interview";
import { ElMessage, ElMessageBox } from "element-plus";
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import InterviewEmpty from "@/components/empty/InterviewEmpty.vue";
import {
  DIFFICULTY_TAG_MAP,
  getFeedbackModeLabel,
  getInteractionTypeLabel,
  getInterviewModeLabel,
  INTERACTION_TYPE_VOICE,
} from '@/constants/interview'

defineOptions({ name: 'InterviewHistoryView' })

const router = useRouter();

const loading = ref(true);
const error = ref("");
const historyList = ref([]);
const pageNum = ref(1);
const pageSize = ref(5);
const total = ref(0);

const difficultyMap = DIFFICULTY_TAG_MAP

const statusMap = {
  0: { text: "进行中", type: "warning" },
  1: { text: "已结束", type: "success" },
};

const sourceTypeMap = {
  manual_jd: "手动 JD",
  manual_jd_with_job_match: "JD + 对比结果",
  latest_job_match: "最近一次 JD 对比",
};

const normalizeHistoryList = (list) => {
  return (Array.isArray(list) ? list : []).map((item) => ({
    ...item,
    sourceTypeText: item.sourceType ? sourceTypeMap[item.sourceType] || item.sourceType : "",
  }));
};

const getDifficultyText = (item) => item.difficultyDesc || difficultyMap[item.difficulty]?.text || "未知";
const getDifficultyType = (item) => difficultyMap[item.difficulty]?.type || "info";
const getModeText = (item) => {
  if (item?.jobTargeted) {
    return item.interviewModeDesc || getInterviewModeLabel("job_targeted");
  }
  return item.interviewModeDesc || getInterviewModeLabel(item.interviewMode);
};
const getStatusText = (item) => item.statusDesc || statusMap[item.status]?.text || "未知";
const getStatusType = (item) => statusMap[item.status]?.type || "info";
const getFeedbackModeText = (item) => getFeedbackModeLabel(item?.feedbackMode);

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const fetchHistory = async () => {
  loading.value = true;
  error.value = "";
  try {
    const res = await getInterviewHistory({ pageNum: pageNum.value, pageSize: pageSize.value });
    const data = res.data;
    if (Array.isArray(data)) {
      historyList.value = normalizeHistoryList(data);
      total.value = Number(data.length) || 0;
      return;
    }
    historyList.value = normalizeHistoryList(data?.list || []);
    total.value = Number(data?.total) || 0;
    pageNum.value = Number(data?.pageNum) || 1;
    pageSize.value = Number(data?.pageSize) || 5;
  } catch (err) {
    error.value = err.message || "获取历史记录失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};

const handleCurrentChange = (val) => {
  pageNum.value = val;
  fetchHistory();
};

const viewSession = (item) => {
  if (item?.sessionId) {
    router.push(`/interview/session/${item.sessionId}`);
  }
};

const continueSession = viewSession;

const viewReport = (item) => {
  if (item?.sessionId) {
    router.push(`/interview/report/${item.sessionId}`);
  }
};

const goToEntry = () => {
  router.push("/interview/entry");
};

// 删除单条
const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${item.jobRole || "未知岗位"}」的面试记录？`,
      "删除确认",
      { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" }
    );
    await deleteInterviewSession(item.sessionId);
    historyList.value = historyList.value.filter(
      (r) => r.sessionId !== item.sessionId
    );
    total.value = Math.max(0, total.value - 1);
    ElMessage.success("已删除");
    if (historyList.value.length === 0 && pageNum.value > 1) {
      pageNum.value--;
      fetchHistory();
    }
  } catch {
    // 用户取消或删除失败
  }
};

// 清空全部
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
      "确定清空所有面试记录？此操作不可恢复。",
      "清空确认",
      { confirmButtonText: "清空", cancelButtonText: "取消", type: "warning" }
    );
    await clearInterviewHistory();
    historyList.value = [];
    total.value = 0;
    ElMessage.success("已清空全部记录");
  } catch {
    // 用户取消
  }
};

onMounted(() => {
  fetchHistory();
});
</script>

<style scoped>
.interview-history-view {
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
}

.clear-all-btn {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: 13px;
}
.button-feature-icon {
  margin-right: 4px;
}
.clear-all-btn:hover {
  color: var(--color-danger);
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted, #888888);
}

.loading-section {
  padding: 20px 0;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.skeleton-card,
.history-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--orange-border, #f3d8c7);
  border-radius: 16px;
  padding: 28px 32px;
}

.skeleton-header,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--orange-light-bg, #fff8f3);
}

.skeleton-info {
  display: flex;
  gap: 48px;
  margin-bottom: 20px;
}

.skeleton-footer,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
}

.skeleton-actions,
.card-actions {
  display: flex;
  gap: 12px;
}

.error-section,
.empty-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.error-card {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  background-color: var(--bg-card, #ffffff);
  border: 1px solid var(--orange-border, #f3d8c7);
  border-radius: 12px;
  padding: 32px;
  max-width: 500px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.error-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-danger);
  margin-bottom: 8px;
}

.error-desc,
.empty-desc {
  font-size: 14px;
  color: var(--text-muted, #888888);
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-action-btn {
  margin-top: 24px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
  margin: 20px 0 8px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 32px;
}

.history-card {
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}

.history-card:hover {
  border-color: var(--orange-main, #ff8c42);
  box-shadow: 0 6px 24px rgba(255, 140, 66, 0.12);
  transform: translateY(-2px);
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

/* 删除按钮 */
.card-delete {
  flex-shrink: 0;
  color: transparent;
  transition: color 0.15s ease;
}
.history-card:hover .card-delete {
  color: var(--text-muted);
}
.card-delete:hover {
  color: var(--color-danger) !important;
}

.title-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-icon {
  margin-right: 4px;
  vertical-align: -4px;
}

.job-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
  margin: 0;
}

.card-info {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--orange-light-bg, #fff8f3);
}

.info-row {
  display: flex;
  gap: 48px;
  margin-bottom: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 120px;
}

.info-label {
  font-size: 13px;
  color: var(--text-muted, #999999);
  min-width: 50px;
}

.info-value {
  font-size: 14px;
  color: var(--text-body, #555555);
}

.info-value.score {
  font-weight: 600;
  color: var(--orange-main, #ff8c42);
  font-size: 16px;
}

.time-info {
  font-size: 13px;
  color: var(--text-muted, #999999);
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 24px 0;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .page-title {
    font-size: 20px;
  }

  .info-row {
    flex-direction: column;
    gap: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  /* 移动端始终显示删除 */
  .card-delete { color: var(--text-muted); opacity: 0.5; }
  .card-delete:hover { opacity: 1; }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 18px;
  }

  .page-desc {
    font-size: 13px;
  }

  .job-title {
    font-size: 16px;
  }

  .history-card,
  .skeleton-card {
    padding: 20px 16px;
  }

  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
