<template>
  <div class="resume-history-view">
    <!-- 页面标题 -->
    <header class="page-header">
      <div>
        <h1 class="page-title">诊断历史</h1>
        <p class="page-desc">查看您所有的简历 AI 诊断记录</p>
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
    </header>

    <!-- 加载态 - 骨架卡片 -->
    <div v-if="loading" class="card-list">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <el-skeleton :rows="0" animated :loading="true">
          <template #default>
            <div class="skeleton-head">
              <div style="display: flex; align-items: center; gap: 14px;">
                <el-skeleton-item variant="circle" style="width: 44px; height: 44px;" />
                <div style="display: flex; flex-direction: column; gap: 8px;">
                  <el-skeleton-item variant="text" style="width: 200px; height: 18px;" />
                  <el-skeleton-item variant="rect" style="width: 64px; height: 22px; border-radius: 4px;" />
                </div>
              </div>
              <el-skeleton-item variant="rect" style="width: 68px; height: 24px; border-radius: 4px;" />
            </div>
            <div class="skeleton-foot">
              <el-skeleton-item variant="text" style="width: 140px; height: 14px;" />
              <div style="display: flex; gap: 8px;">
                <el-skeleton-item variant="rect" style="width: 80px; height: 32px; border-radius: 6px;" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="centered-state">
      <div class="error-card">
        <div class="error-icon-ring">
          <FeatureIcon name="error" size="md" />
        </div>
        <div class="error-body">
          <span class="error-title">加载失败</span>
          <span class="error-desc">{{ error }}</span>
        </div>
        <el-button type="primary" size="small" @click="fetchHistory">重试</el-button>
      </div>
    </div>

    <!-- 空态 -->
    <div v-else-if="total === 0" class="centered-state">
      <div class="empty-content">
        <ResumeEmpty :size="140" />
        <div class="empty-title">暂无诊断记录</div>
        <p class="empty-desc">上传简历后，AI 会在这里保留每一次诊断结果</p>
        <el-button type="primary" @click="goToUpload">上传简历</el-button>
      </div>
    </div>

    <!-- 卡片列表 -->
    <div v-else class="card-list">
      <div
        v-for="item in historyList"
        :key="item.taskId"
        class="diag-card"
        :class="`diag-card--status-${item.status}`"
      >
        <!-- 上部：文件 + 状态 -->
        <div class="card-head">
          <div class="file-block">
            <div class="file-icon" :class="`file-icon--${item.status}`">
              <FeatureIcon name="resume-analysis" size="lg" />
            </div>
            <div class="file-meta">
              <div class="file-name-row">
                <span class="status-dot" :class="`status-dot--${item.status}`"></span>
                <span class="file-name" :title="getFileName(item)">{{ getFileName(item) }}</span>
              </div>
              <div class="file-badges">
                <el-tag
                  :type="getStatusType(item.status)"
                  size="small"
                  effect="light"
                >
                  {{ item.statusDesc || getStatusText(item.status) }}
                </el-tag>
                <span v-if="item.parseMode" class="parse-hint">{{ getParseModeLabel(item.parseMode) }}</span>
              </div>
            </div>
          </div>
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

        <!-- 下部：时间 + 操作 -->
        <div class="card-foot">
          <div class="time-block">
            <el-icon :size="13" style="flex-shrink: 0;"><Clock /></el-icon>
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="card-actions">
            <!-- 已完成 -->
            <el-button
              v-if="item.status === 2"
              type="primary"
              size="small"
              @click="viewResult(item)"
            >
              查看报告
            </el-button>
            <!-- 失败 -->
            <template v-if="item.status === 3">
              <el-button size="small" type="primary" :loading="retryingTaskId === item.taskId" @click="handleRetry(item)">
                <FeatureIcon v-if="retryingTaskId !== item.taskId" name="retry" size="xs" class="button-feature-icon" />
                重新诊断
              </el-button>
              <el-button link type="primary" size="small" @click="viewResult(item)">
                查看详情
              </el-button>
            </template>
            <!-- 进行中 -->
            <template v-if="item.status === 0 || item.status === 1">
              <span class="pending-hint">正在处理中...</span>
              <el-button link type="primary" size="small" @click="viewResult(item)">
                查看进度
              </el-button>
            </template>
          </div>
        </div>

        <!-- 失败原因 -->
        <div v-if="item.status === 3 && item.errorMsg" class="card-error">
          <FeatureIcon name="error" size="xs" style="flex-shrink: 0;" />
          <span>{{ item.errorMsg }}</span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-wrap">
      <el-pagination
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        :total="total"
        :layout="isMobileLayout ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { Clock } from "@element-plus/icons-vue";
import { getResumeHistory, extractFileName, clearResumeHistory, deleteResumeHistory, retryResumeTask } from "@/api/resume";
import { ElMessage, ElMessageBox } from "element-plus";
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import ResumeEmpty from "@/components/empty/ResumeEmpty.vue";
import { prefetchUserRoute } from "@/router/routeLoaders";

defineOptions({ name: 'HistoryView' })

const router = useRouter();

// 页面状态
const loading = ref(true);
const error = ref("");
const historyList = ref([]);
const isMobileLayout = ref(false);
const retryingTaskId = ref(null);

const updateLayout = () => {
  isMobileLayout.value = window.innerWidth < 768;
};

// 分页（默认每页 5 条）
const pageNum = ref(1);
const pageSize = ref(5);
const total = ref(0);
const totalPages = ref(0);
const hasNextPage = ref(false);
const hasPreviousPage = ref(false);

// 状态映射
const statusMap = {
  0: { text: "排队中", type: "warning" },
  1: { text: "解析中", type: "primary" },
  2: { text: "已完成", type: "success" },
  3: { text: "失败", type: "danger" },
};

const getStatusText = (status) => statusMap[status]?.text || "未知";
const getStatusType = (status) => statusMap[status]?.type || "info";
const getFileName = (row) => extractFileName(row.fileUrl);

// 解析模式标签
const getParseModeLabel = (mode) => {
  const map = { TEXT: "文本解析", MULTIMODAL: "多模态", OCR: "OCR", MIXED: "混合解析" };
  return map[mode] || mode;
};

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

// 获取历史
const fetchHistory = async () => {
  loading.value = true;
  error.value = "";
  try {
    const res = await getResumeHistory({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    if (res.data) {
      if (Array.isArray(res.data)) {
        historyList.value = res.data;
        total.value = Number(res.data.length) || 0;
      } else if (res.data.list && Array.isArray(res.data.list)) {
        historyList.value = res.data.list;
        total.value = Number(res.data.total) || 0;
        pageNum.value = Number(res.data.pageNum) || 1;
        pageSize.value = Number(res.data.pageSize) || 5;
        totalPages.value = Number(res.data.totalPages) || 0;
        hasNextPage.value = res.data.hasNextPage || false;
        hasPreviousPage.value = res.data.hasPreviousPage || false;
      } else {
        historyList.value = [];
        total.value = 0;
      }
    } else {
      historyList.value = [];
      total.value = 0;
    }
  } catch (err) {
    console.error("获取历史记录失败:", err);
    error.value = err.message || "获取历史记录失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};

const handleSizeChange = (val) => {
  pageSize.value = val;
  pageNum.value = 1;
  fetchHistory();
};

const handleCurrentChange = (val) => {
  pageNum.value = val;
  fetchHistory();
};

const viewResult = (row) => {
  prefetchUserRoute("/resume/result")?.catch(() => {});
  router.push(`/resume/result/${row.taskId}`);
};

const goToUpload = () => {
  router.push("/resume/upload");
};

// 删除单条
const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${getFileName(item)}」的诊断记录？`,
      "删除确认",
      { confirmButtonText: "删除", cancelButtonText: "取消", type: "warning" }
    );
    await deleteResumeHistory(item.taskId);
    historyList.value = historyList.value.filter(
      (r) => r.taskId !== item.taskId
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

// 重试失败任务
const handleRetry = async (item) => {
  retryingTaskId.value = item.taskId;
  try {
    const res = await retryResumeTask(item.taskId);
    const newTaskId = res.data;
    if (!newTaskId) {
      ElMessage.error("重试响应异常，请稍后重试");
      return;
    }
    ElMessage.success("重试任务已提交");
    prefetchUserRoute("/resume/result")?.catch(() => {});
    router.push(`/resume/result/${newTaskId}`);
  } catch (err) {
    ElMessage.error(err?.message || "重试失败，请重新上传");
  } finally {
    retryingTaskId.value = null;
  }
};

// 清空全部
const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm(
      "确定清空所有诊断记录？此操作不可恢复。",
      "清空确认",
      { confirmButtonText: "清空", cancelButtonText: "取消", type: "warning" }
    );
    await clearResumeHistory();
    historyList.value = [];
    total.value = 0;
    ElMessage.success("已清空全部记录");
  } catch {
    // 用户取消
  }
};

onMounted(() => {
  updateLayout();
  window.addEventListener("resize", updateLayout);
  fetchHistory();
});

onUnmounted(() => {
  window.removeEventListener("resize", updateLayout);
});
</script>

<style scoped>
.resume-history-view {
  min-height: 100%;
}

/* ── 页头 ── */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
  gap: 16px;
}
.page-title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title);
  letter-spacing: -0.01em;
}
.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
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

/* ── 居中状态 ── */
.centered-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
}

/* ── 错误态 ── */
.error-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  max-width: 460px;
  width: 100%;
  box-shadow: var(--shadow-card);
}
.error-icon-ring {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--tag-bg-danger, rgba(245, 108, 108, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.error-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.error-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-title);
}
.error-desc {
  font-size: 13px;
  color: var(--text-muted);
}

/* ── 空态 ── */
.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title);
  margin: 20px 0 6px;
}
.empty-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.6;
}

/* ── 卡片列表 ── */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── 骨架卡片 ── */
.skeleton-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: skeletonFadeIn 0.4s ease both;
}
.skeleton-card:nth-child(1) { animation-delay: 0s; }
.skeleton-card:nth-child(2) { animation-delay: 0.08s; }
.skeleton-card:nth-child(3) { animation-delay: 0.16s; }
@keyframes skeletonFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
.skeleton-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.skeleton-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid var(--orange-light-bg, rgba(255, 140, 66, 0.06));
}

/* ── 诊断卡片（核心） ── */
.diag-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: border-color 0.2s ease, box-shadow 0.25s ease, transform 0.25s ease;
  box-shadow: var(--shadow-card);
}
.diag-card:hover {
  border-color: var(--orange-main);
  box-shadow: 0 6px 24px rgba(255, 140, 66, 0.10);
  transform: translateY(-2px);
}

/* 进行中卡片的微弱脉冲 */
.diag-card--status-0,
.diag-card--status-1 {
  animation: cardPulse 3s ease-in-out infinite;
}
@keyframes cardPulse {
  0%, 100% { border-color: var(--border-card); }
  50% { border-color: var(--orange-border); }
}

@media (prefers-reduced-motion: reduce) {
  .diag-card--status-0,
  .diag-card--status-1 { animation: none; }
  .diag-card { transition: none; }
}

/* ── 卡片上部 ── */
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.file-block {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

/* 文件图标 - 按状态变色，无背景 */
.file-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--orange-main);
}
.file-icon--0,
.file-icon--1 {
  color: var(--orange-main);
}
.file-icon--2 {
  color: var(--color-success);
}
.file-icon--3 {
  color: var(--color-danger);
}

.file-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

/* 文件名行：圆点 + 文件名 */
.file-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

/* 状态小圆点 */
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--orange-main);
}
.status-dot--0,
.status-dot--1 {
  background: var(--orange-main);
}
.status-dot--2 {
  background: var(--color-success);
}
.status-dot--3 {
  background: var(--color-danger);
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.parse-hint {
  font-size: 12px;
  color: var(--text-muted);
}

/* 删除按钮 */
.card-delete {
  flex-shrink: 0;
  color: transparent;
  transition: color 0.15s ease;
}
.diag-card:hover .card-delete {
  color: var(--text-muted);
}
.card-delete:hover {
  color: var(--color-danger) !important;
}

/* ── 卡片下部 ── */
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--orange-light-bg, rgba(255, 140, 66, 0.06));
}

.time-block {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--text-muted);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.pending-hint {
  font-size: 12px;
  color: var(--orange-main);
  animation: textPulse 2s ease-in-out infinite;
}
@keyframes textPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 失败原因 */
.card-error {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 10px 14px;
  background: rgba(245, 108, 108, 0.06);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-danger);
  margin-top: -8px;
  overflow: hidden;
  word-break: break-all;
}

.card-error span {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 分页 ── */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 28px 0 8px;
}

/* ── 响应式：平板 ── */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 20px;
  }
  .page-title { font-size: 20px; }

  .card-list { gap: 14px; }

  .diag-card { padding: 20px 22px; }

  /* 移动端始终显示删除 */
  .card-delete { color: var(--text-muted); opacity: 0.5; }
  .card-delete:hover { opacity: 1; }
}

/* ── 响应式：手机 ── */
@media (max-width: 480px) {
  .page-title { font-size: 18px; }
  .page-desc { font-size: 13px; }

  .card-list { gap: 12px; }

  .diag-card {
    padding: 16px 18px;
    gap: 16px;
  }

  .file-icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
  }
  .file-name {
    font-size: 15px;
  }

  .card-foot {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .card-actions {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>
