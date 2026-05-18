<template>
  <div class="resume-history-view">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">诊断历史</h1>
      <p class="page-desc">查看您所有的简历诊断记录</p>
    </div>

    <!-- 加载状态 - 骨架屏 -->
    <div v-if="loading" class="loading-section">
      <div class="skeleton-card">
        <el-skeleton :rows="5" animated :loading="true">
          <template #default>
            <div class="skeleton-item" v-for="i in 5" :key="i">
              <el-skeleton-item variant="rect" style="width: 200px; height: 16px;" />
              <el-skeleton-item variant="rect" style="width: 80px; height: 24px; margin-left: 16px;" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-section">
      <div class="error-card">
        <div class="error-icon">
          <el-icon :size="48" color="#f56c6c"><CircleClose /></el-icon>
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

    <!-- 空状态 -->
    <div v-else-if="total === 0" class="empty-section">
      <div class="empty-content">
        <ResumeEmpty :size="140" />
        <div class="empty-title">暂无诊断记录</div>
        <div class="empty-desc">
          上传您的第一份简历，开启AI智能诊断，发现简历优化方向
        </div>
        <el-button type="primary" @click="goToUpload">上传简历</el-button>
      </div>
    </div>

    <!-- 历史记录列表 -->
    <div v-else class="history-list">
      <div class="history-card">
        <el-table
          :data="historyList"
          stripe
          style="width: 100%"
          :header-cell-style="{
            background: '#FFF8F3',
            color: '#2F2F2F',
            fontWeight: 600,
          }"
        >
          <!-- 文件名 -->
          <el-table-column label="文件名" min-width="280">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon :size="16" color="#FF8C42"><Document /></el-icon>
                <span class="file-name" :title="getFileName(row)">{{
                  getFileName(row)
                }}</span>
              </div>
            </template>
          </el-table-column>

          <!-- 状态 -->
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 创建时间 -->
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              <span class="create-time">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                @click="viewResult(row)"
              >
                查看结果
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-section">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          :layout="isMobileLayout ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { Loading, CircleClose, Document } from "@element-plus/icons-vue";
import { getResumeHistory, extractFileName } from "@/api/resume";
import { ElMessage } from "element-plus";
import ResumeEmpty from "@/components/empty/ResumeEmpty.vue";

const router = useRouter();

// 状态
const loading = ref(true);
const error = ref("");
const historyList = ref([]);

// 响应式分页布局
const isMobileLayout = ref(false);

const updateLayout = () => {
  isMobileLayout.value = window.innerWidth < 768;
};

// 分页状态
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalPages = ref(0);
const hasNextPage = ref(false);
const hasPreviousPage = ref(false);

// 状态映射 - 橙色主题
const statusMap = {
  0: { text: "排队中", type: "warning" },
  1: { text: "解析中", type: "primary" },
  2: { text: "已完成", type: "success" },
  3: { text: "失败", type: "danger" },
};

const getStatusText = (status) => {
  return statusMap[status]?.text || "未知";
};

const getStatusType = (status) => {
  return statusMap[status]?.type || "info";
};

const getFileName = (row) => {
  return extractFileName(row.fileUrl);
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
        pageSize.value = Number(res.data.pageSize) || 10;
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
  router.push(`/resume/result/${row.taskId}`);
};

const goToUpload = () => {
  router.push("/resume/upload");
};

onMounted(() => {
  updateLayout();
  window.addEventListener('resize', updateLayout);
  fetchHistory();
});

onUnmounted(() => {
  window.removeEventListener('resize', updateLayout);
});
</script>

<style scoped>
.resume-history-view {
  min-height: 100%;
}

/* 页面标题区 */
.page-header {
  margin-bottom: 24px;
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

/* 加载状态 - 骨架屏 */
.loading-section {
  padding: 20px 0;
}

.skeleton-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--orange-border, #f3d8c7);
  border-radius: 12px;
  padding: 24px;
}

.skeleton-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--bg-elevated, #f5f5f5);
}

.skeleton-item:last-child {
  border-bottom: none;
}

/* 错误状态 */
.error-section {
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

.error-icon {
  flex-shrink: 0;
}

.error-content {
  flex: 1;
}

.error-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-danger);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: var(--text-muted, #888888);
  margin-bottom: 16px;
}

.error-actions {
  display: flex;
  gap: 12px;
}

/* 空状态 */
.empty-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
  margin: 20px 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted, #888888);
  margin-bottom: 24px;
  max-width: 400px;
  line-height: 1.6;
}

/* 历史记录列表 */
.history-list {
  margin-bottom: 24px;
}

.history-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--orange-border, #f3d8c7);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-size: 14px;
  color: var(--text-body, #555555);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

.create-time {
  font-size: 13px;
  color: var(--text-muted, #888888);
}

/* 分页器 */
.pagination-section {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .page-title {
    font-size: 20px;
  }
  .history-card {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .file-name {
    max-width: 160px;
  }
  .create-time {
    font-size: 12px;
  }
  .pagination-section {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    padding: 12px 0;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 18px;
  }
  .page-desc {
    font-size: 13px;
  }
  .file-name {
    max-width: 120px;
    font-size: 13px;
  }
}
</style>
