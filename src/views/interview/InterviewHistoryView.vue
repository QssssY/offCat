<template>
  <div class="interview-history-view">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">面试历史</h1>
      <p class="page-desc">查看您所有的模拟面试记录</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="loading-content">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <div class="loading-text">加载中...</div>
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
        <el-icon :size="64" color="#F3D8C7"><ChatDotRound /></el-icon>
        <div class="empty-title">暂无面试记录</div>
        <div class="empty-desc">您还没有进行过模拟面试，选择岗位开始您的第一次面试吧</div>
        <el-button type="primary" @click="goToEntry">去开始面试</el-button>
      </div>
    </div>

    <!-- 历史记录卡片列表 -->
    <div v-else class="history-list">
      <div
        v-for="item in historyList"
        :key="item.sessionId"
        class="history-card"
      >
        <!-- 卡片顶部：岗位 + 状态 -->
        <div class="card-header">
          <h3 class="job-title">{{ item.jobRole || '未知岗位' }}</h3>
          <el-tag :type="getStatusType(item)" size="small" class="status-tag">
            {{ getStatusText(item) }}
          </el-tag>
        </div>

        <!-- 卡片中部：信息区分2行展示 -->
        <div class="card-info">
          <!-- 第一行：难度 + 模式 -->
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
          <!-- 第二行：评分 + 消息数 -->
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">综合评分</span>
              <span class="info-value score" v-if="item.comprehensiveScore">
                {{ item.comprehensiveScore }}分
              </span>
              <span class="info-value" v-else>--</span>
            </div>
            <div class="info-item" v-if="item.messageCount">
              <span class="info-label">消息数</span>
              <span class="info-value">{{ item.messageCount }}</span>
            </div>
          </div>
        </div>

        <!-- 卡片底部：时间 + 操作 -->
        <div class="card-footer">
          <div class="time-info">
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="card-actions">
            <!-- 已结束会话：查看会话 + 查看报告 -->
            <template v-if="item.status === 1">
              <el-button link type="primary" size="small" @click="viewSession(item)">
                查看会话
              </el-button>
              <el-button type="primary" size="small" @click="viewReport(item)">
                查看报告
              </el-button>
            </template>
            <!-- 进行中会话：继续面试 + 查看会话 -->
            <template v-else>
              <el-button type="primary" size="small" @click="continueSession(item)">
                继续面试
              </el-button>
              <el-button link type="primary" size="small" @click="viewSession(item)">
                查看会话
              </el-button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页器 -->
    <div v-if="total > 0" class="pagination-section">
      <el-pagination
        :page-size="5"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, CircleClose, ChatDotRound } from '@element-plus/icons-vue'
import { getInterviewHistory } from '@/api/interview'

const router = useRouter()

// 状态
const loading = ref(true)
const error = ref('')
const historyList = ref([])

// 分页状态 - 每页固定5条
const pageNum = ref(1)
const pageSize = ref(5)
const total = ref(0)

// 难度映射：数字 -> 文字/颜色
const difficultyMap = {
  1: { text: '初级', type: 'success' },
  2: { text: '中级', type: 'warning' },
  3: { text: '高级', type: 'danger' }
}

// 面试模式映射
const interviewModeMap = {
  'normal': '普通面试',
  'stress': '压力面试'
}

// 状态映射
const statusMap = {
  0: { text: '进行中', type: 'warning' },
  1: { text: '已结束', type: 'success' }
}

// 新增：报告状态映射
// 修复目的：区分"已结束但报告未生成"和"已结束且报告已生成"
const reportStatusMap = {
  0: { text: '未生成', type: 'info' },
  1: { text: '已完成', type: 'success' }
}

// 获取难度显示文本
const getDifficultyText = (item) => {
  if (item.difficultyDesc) return item.difficultyDesc
  return difficultyMap[item.difficulty]?.text || '未知'
}

// 获取难度标签类型
const getDifficultyType = (item) => {
  if (item.difficultyDesc) {
    return difficultyMap[item.difficulty]?.type || 'info'
  }
  return difficultyMap[item.difficulty]?.type || 'info'
}

// 获取面试模式显示文本
const getModeText = (item) => {
  if (item.interviewModeDesc) return item.interviewModeDesc
  return interviewModeMap[item.interviewMode] || '普通面试'
}

// 获取状态显示文本
// 修复：增加对报告生成的区分显示
const getStatusText = (item) => {
  // 如果后端返回了 reportStatus，使用它区分报告状态
  if (item.reportStatus !== undefined) {
    return reportStatusMap[item.reportStatus]?.text || '未知'
  }
  // 兼容旧字段
  if (item.statusDesc) return item.statusDesc
  return statusMap[item.status]?.text || '未知'
}

// 获取状态标签类型
// 修复：已结束但报告未生成时显示不同状态
const getStatusType = (item) => {
  // 如果后端返回了 reportStatus
  if (item.reportStatus !== undefined) {
    // 报告未生成 -> 报告生成中的颜色（warning）
    if (item.reportStatus === 0) {
      return item.status === 1 ? 'warning' : 'info'
    }
    // 报告已完成 -> 绿色
    return 'success'
  }
  // 兼容旧字段
  if (item.statusDesc) {
    return item.status === 0 ? 'warning' : 'success'
  }
  return statusMap[item.status]?.type || 'info'
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取历史记录
const fetchHistory = async () => {
  loading.value = true
  error.value = ''

  try {
    const res = await getInterviewHistory({ pageNum: pageNum.value, pageSize: pageSize.value })

    if (res.data) {
      if (Array.isArray(res.data)) {
        historyList.value = res.data
        total.value = res.data.length
      } else if (res.data.list && Array.isArray(res.data.list)) {
        historyList.value = res.data.list
        total.value = res.data.total || 0
        pageNum.value = res.data.pageNum || 1
        pageSize.value = res.data.pageSize || 5
      } else {
        historyList.value = []
        total.value = 0
      }
    } else {
      historyList.value = []
      total.value = 0
    }
  } catch (err) {
    console.error('获取面试历史失败:', err)
    error.value = err.message || '获取历史记录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

// 页码变化
const handleCurrentChange = (val) => {
  pageNum.value = val
  fetchHistory()
}

// 查看会话
const viewSession = (item) => {
  if (!item.sessionId) return
  router.push(`/interview/session/${item.sessionId}`)
}

// 继续面试
const continueSession = (item) => {
  if (!item.sessionId) return
  router.push(`/interview/session/${item.sessionId}`)
}

// 查看报告
const viewReport = (item) => {
  if (!item.sessionId) return
  router.push(`/interview/report/${item.sessionId}`)
}

// 跳转到面试入口
const goToEntry = () => {
  router.push('/interview/entry')
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.interview-history-view {
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
  color: #2F2F2F;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: #888888;
}

/* 加载状态 */
.loading-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-icon {
  color: #FF8C42;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: #888888;
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
  background-color: #FFFFFF;
  border: 1px solid #F3D8C7;
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
  color: #F56C6C;
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: #888888;
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
  font-size: 16px;
  font-weight: 500;
  color: #2F2F2F;
  margin: 16px 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: #888888;
  margin-bottom: 24px;
  max-width: 400px;
}

/* ============================================
   卡片列表布局
   ============================================ */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 32px;
}

/* 单个卡片 */
.history-card {
  background: #FFFFFF;
  border: 1px solid #F3D8C7;
  border-radius: 16px;
  padding: 28px 32px;
  transition: all 0.25s ease;
}

.history-card:hover {
  border-color: #FF8C42;
  box-shadow: 0 6px 24px rgba(255, 140, 66, 0.12);
  transform: translateY(-2px);
}

/* 卡片顶部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #FFF8F3;
}

.job-title {
  font-size: 20px;
  font-weight: 600;
  color: #2F2F2F;
  margin: 0;
}

.status-tag {
  font-weight: 500;
}

/* 信息区 */
.card-info {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #FFF8F3;
}

/* 信息行 */
.info-row {
  display: flex;
  gap: 48px;
  margin-bottom: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

/* 信息项 */
.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 120px;
}

.info-label {
  font-size: 13px;
  color: #999999;
  min-width: 50px;
}

.info-value {
  font-size: 14px;
  color: #555555;
}

.info-value.score {
  font-weight: 600;
  color: #FF8C42;
  font-size: 16px;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
}

.time-info {
  font-size: 13px;
  color: #999999;
}

.card-actions {
  display: flex;
  gap: 16px;
}

/* 分页器 */
.pagination-section {
  display: flex;
  justify-content: center;
  padding: 24px 0;
  margin-top: 16px;
}
</style>
