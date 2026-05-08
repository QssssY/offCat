<template>
  <div class="growth-center">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>
        <div>
          <h1 class="page-title">个人成长中心</h1>
          <p class="page-desc">查看你的成长轨迹与个性化建议</p>
        </div>
      </div>
      <el-button
        :icon="Refresh"
        circle
        size="small"
        :loading="loading"
        @click="fetchData"
        class="refresh-btn"
      />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>正在加载成长数据...</span>
    </div>

    <!-- 加载失败状态 -->
    <div v-else-if="loadError" class="error-state">
      <div class="error-icon-wrapper">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
      </div>
      <h3 class="error-title">加载失败</h3>
      <p class="error-desc">获取成长数据时出现问题，请重试</p>
      <el-button type="primary" @click="fetchData">重新加载</el-button>
    </div>

    <!-- 全量无数据状态 -->
    <div v-else-if="isEmpty" class="empty-full-state">
      <div class="empty-icon-wrapper">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
      </div>
      <h3 class="empty-title">还没有成长数据</h3>
      <p class="empty-desc">完成简历诊断或模拟面试后，这里将展示你的成长轨迹</p>
      <div class="empty-actions">
        <el-button type="primary" @click="$router.push('/resume/upload')">开始简历诊断</el-button>
        <el-button @click="$router.push('/interview/entry')">开始模拟面试</el-button>
      </div>
    </div>

    <!-- 有数据时展示 -->
    <template v-else>
      <!-- 1. 成长概览卡片 -->
      <section class="summary-section">
        <div class="summary-card">
          <div class="summary-icon resume-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
          </div>
          <div class="summary-value">{{ summary.latestResumeScore ?? '--' }}</div>
          <div class="summary-label">最近简历分</div>
        </div>
        <div class="summary-card">
          <div class="summary-icon interview-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
            </svg>
          </div>
          <div class="summary-value">{{ summary.latestInterviewScore ?? '--' }}</div>
          <div class="summary-label">最近面试分</div>
        </div>
        <div class="summary-card">
          <div class="summary-icon count-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
            </svg>
          </div>
          <div class="summary-value">{{ summary.resumeDiagnosisCount ?? 0 }}</div>
          <div class="summary-label">简历诊断次数</div>
        </div>
        <div class="summary-card">
          <div class="summary-icon count-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
          </div>
          <div class="summary-value">{{ summary.mockInterviewCount ?? 0 }}</div>
          <div class="summary-label">模拟面试次数</div>
        </div>
      </section>

      <!-- 2+3. 双折线图区域 -->
      <section class="charts-section">
        <div class="chart-card">
          <h3 class="card-title">简历诊断分数趋势</h3>
          <LineChart
            v-if="resumeScoreTrend.length > 0"
            :labels="resumeScoreTrend.map(i => i.date)"
            :datasets="[{ label: '简历分数', data: resumeScoreTrend.map(i => i.score), borderColor: '#FF8C42', backgroundColor: 'rgba(255, 140, 66, 0.1)' }]"
          />
          <div v-else class="chart-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
            <span>暂无数据，完成更多诊断后将展示趋势</span>
          </div>
        </div>
        <div class="chart-card">
          <h3 class="card-title">面试评分趋势</h3>
          <LineChart
            v-if="interviewScoreTrend.length > 0"
            :labels="interviewScoreTrend.map(i => i.date)"
            :datasets="[{ label: '面试分数', data: interviewScoreTrend.map(i => i.score), borderColor: '#E67A35', backgroundColor: 'rgba(230, 122, 53, 0.1)' }]"
          />
          <div v-else class="chart-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
            </svg>
            <span>暂无数据，完成更多面试后将展示趋势</span>
          </div>
        </div>
      </section>

      <!-- 4+5+6. 详情卡片区域 -->
      <section class="details-section">
        <!-- 最近JD匹配结果 -->
        <div class="detail-card">
          <h3 class="card-title">最近 JD 匹配结果</h3>
          <template v-if="latestJobMatch">
            <div class="match-score-row">
              <span class="match-score-value">{{ latestJobMatch.matchScore ?? '--' }}</span>
              <span class="match-score-unit">分</span>
            </div>
            <div v-if="latestJobMatch.matchedKeywords?.length" class="keyword-section">
              <span class="keyword-label">已匹配关键词</span>
              <div class="keyword-tags">
                <span v-for="kw in latestJobMatch.matchedKeywords.slice(0, 5)" :key="kw" class="keyword-tag matched">{{ kw }}</span>
              </div>
            </div>
            <div v-if="latestJobMatch.missingKeywords?.length" class="keyword-section">
              <span class="keyword-label">缺失关键词</span>
              <div class="keyword-tags">
                <span v-for="kw in latestJobMatch.missingKeywords.slice(0, 5)" :key="kw" class="keyword-tag missing">{{ kw }}</span>
              </div>
            </div>
            <div class="detail-time">{{ latestJobMatch.createTime }}</div>
          </template>
          <div v-else class="card-empty">
            <span>尚未进行 JD 匹配分析</span>
            <el-button link type="primary" @click="$router.push('/resume/upload')">去分析</el-button>
          </div>
        </div>

        <!-- 最近AI润色记录 -->
        <div class="detail-card">
          <h3 class="card-title">最近 AI 润色记录</h3>
          <template v-if="latestPolish">
            <div class="polish-source">来源：{{ latestPolish.sourceType || '简历诊断' }}</div>
            <div v-if="latestPolish.modificationNotes?.length" class="polish-notes">
              <div v-for="(note, idx) in latestPolish.modificationNotes.slice(0, 3)" :key="idx" class="polish-note-item">
                <span class="note-bullet"></span>
                <span>{{ note }}</span>
              </div>
            </div>
            <div class="detail-time">{{ latestPolish.createTime }}</div>
          </template>
          <div v-else class="card-empty">
            <span>尚未使用 AI 润色</span>
            <el-button link type="primary" @click="$router.push('/resume/upload')">去润色</el-button>
          </div>
        </div>

        <!-- 最近模拟面试反馈 -->
        <div class="detail-card">
          <h3 class="card-title">最近模拟面试反馈</h3>
          <template v-if="latestInterviewFeedback">
            <div class="feedback-header">
              <span class="feedback-job">{{ latestInterviewFeedback.jobRole || '未知岗位' }}</span>
              <span class="feedback-mode">{{ modeLabel(latestInterviewFeedback.interviewMode) }}</span>
            </div>
            <div class="feedback-score">
              <span class="feedback-score-value">{{ latestInterviewFeedback.comprehensiveScore ?? '--' }}</span>
              <span class="feedback-score-unit">分</span>
            </div>
            <div v-if="latestInterviewFeedback.evaluationReport" class="feedback-summary">
              {{ latestInterviewFeedback.evaluationReport }}
            </div>
            <div class="detail-time">{{ latestInterviewFeedback.createTime }}</div>
          </template>
          <div v-else class="card-empty">
            <span>暂无面试反馈</span>
            <el-button link type="primary" @click="$router.push('/interview/entry')">去面试</el-button>
          </div>
        </div>
      </section>

      <!-- 7. 短板与建议 -->
      <section v-if="hasWeakness" class="weakness-section">
        <h3 class="card-title">当前主要短板与建议</h3>
        <div class="weakness-grid">
          <div v-if="weaknessSummary.resumeWeaknesses?.length" class="weakness-block">
            <div class="weakness-block-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              简历侧
            </div>
            <ul class="weakness-list">
              <li v-for="(w, i) in weaknessSummary.resumeWeaknesses" :key="i">{{ w }}</li>
            </ul>
          </div>
          <div v-if="weaknessSummary.jobMatchWeaknesses?.length" class="weakness-block">
            <div class="weakness-block-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              岗位匹配侧
            </div>
            <ul class="weakness-list">
              <li v-for="(w, i) in weaknessSummary.jobMatchWeaknesses" :key="i">{{ w }}</li>
            </ul>
          </div>
          <div v-if="weaknessSummary.interviewWeaknesses?.length" class="weakness-block">
            <div class="weakness-block-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
              </svg>
              面试表现侧
            </div>
            <ul class="weakness-list">
              <li v-for="(w, i) in weaknessSummary.interviewWeaknesses" :key="i">{{ w }}</li>
            </ul>
          </div>
        </div>
        <div v-if="weaknessSummary.suggestions?.length" class="suggestions-block">
          <div class="suggestions-title">改进建议</div>
          <div class="suggestion-items">
            <div v-for="(s, i) in weaknessSummary.suggestions" :key="i" class="suggestion-item">
              <span class="suggestion-num">{{ i + 1 }}</span>
              <span>{{ s }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getGrowthOverview } from '@/api/growth'
import LineChart from '@/components/resume/LineChart.vue'

/** 加载状态 */
const loading = ref(true)
/** 加载错误状态 */
const loadError = ref(false)
/** 成长中心概览数据 */
const overviewData = ref(null)

/** 成长概览摘要（数值字段统一 Number 转换，避免 JacksonConfig 字符串序列化问题） */
const summary = computed(() => {
  const s = overviewData.value?.summary
  if (!s) return {}
  return {
    ...s,
    latestResumeScore: Number(s.latestResumeScore ?? 0),
    latestInterviewScore: Number(s.latestInterviewScore ?? 0),
    resumeDiagnosisCount: Number(s.resumeDiagnosisCount ?? 0),
    mockInterviewCount: Number(s.mockInterviewCount ?? 0)
  }
})
/** 简历分数趋势 */
const resumeScoreTrend = computed(() => overviewData.value?.resumeScoreTrend || [])
/** 面试评分趋势 */
const interviewScoreTrend = computed(() => overviewData.value?.interviewScoreTrend || [])
/** 最近JD匹配 */
const latestJobMatch = computed(() => overviewData.value?.latestJobMatch)
/** 最近AI润色 */
const latestPolish = computed(() => overviewData.value?.latestPolish)
/** 最近面试反馈 */
const latestInterviewFeedback = computed(() => overviewData.value?.latestInterviewFeedback)
/** 短板与建议 */
const weaknessSummary = computed(() => overviewData.value?.weaknessSummary || {})

/** 是否全量无数据 */
const isEmpty = computed(() => {
  if (!overviewData.value) return true
  const s = summary.value
  return (s?.resumeDiagnosisCount ?? 0) === 0 && (s?.mockInterviewCount ?? 0) === 0
})

/** 是否有短板数据 */
const hasWeakness = computed(() => {
  const w = weaknessSummary.value
  return (w?.resumeWeaknesses?.length > 0) ||
         (w?.jobMatchWeaknesses?.length > 0) ||
         (w?.interviewWeaknesses?.length > 0) ||
         (w?.suggestions?.length > 0)
})

/** 面试模式中文标签 */
const modeLabel = (mode) => {
  const map = { normal: '普通面试', stress: '压力面试', job_targeted: '岗位定向面试' }
  return map[mode] || mode || '普通面试'
}

/** 获取成长中心数据 */
const fetchData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await getGrowthOverview()
    overviewData.value = res.data
  } catch (err) {
    console.error('[成长中心] 获取数据失败:', err)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

/** 页面加载时获取数据 */
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.growth-center {
  min-height: 100%;
  padding: 0 0 40px;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.refresh-btn {
  flex-shrink: 0;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff3e8 0%, #ffe0c8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--orange-main);
  flex-shrink: 0;
}

.header-icon svg {
  width: 24px;
  height: 24px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-title);
}

.page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-muted);
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 16px;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 全量无数据状态 */
.empty-full-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  text-align: center;
}

/* 加载失败状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  text-align: center;
}

.error-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.error-icon-wrapper svg {
  width: 40px;
  height: 40px;
  color: var(--color-danger);
}

.error-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title);
}

.error-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff3e8 0%, #ffe0c8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-icon-wrapper svg {
  width: 40px;
  height: 40px;
  color: var(--orange-main);
}

.empty-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-title);
}

.empty-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.empty-actions {
  display: flex;
  gap: 12px;
}

/* 成长概览卡片 */
.summary-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
  transition: all 0.2s ease;
}

.summary-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.1);
}

.summary-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-icon svg {
  width: 22px;
  height: 22px;
}

.resume-icon {
  background: linear-gradient(135deg, #fff3e8 0%, #ffe0c8 100%);
  color: var(--orange-deep);
}

.interview-icon {
  background: linear-gradient(135deg, #fff8f3 0%, #ffe8d6 100%);
  color: var(--orange-main);
}

.count-icon {
  background: linear-gradient(135deg, #f0f9eb 0%, #d8f0d0 100%);
  color: #67c23a;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-title);
  line-height: 1;
}

.summary-label {
  font-size: 13px;
  color: var(--text-muted);
}

/* 折线图区域 */
.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
  color: var(--text-placeholder);
  font-size: 13px;
}

.chart-empty svg {
  width: 36px;
  height: 36px;
  color: var(--border-card);
}

/* 通用卡片标题 */
.card-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  align-self: flex-start;
}

/* 详情卡片区域 */
.details-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.detail-card {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
  display: flex;
  flex-direction: column;
}

.card-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  gap: 12px;
  color: var(--text-placeholder);
  font-size: 13px;
}

/* JD匹配 */
.match-score-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 16px;
}

.match-score-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--orange-main);
}

.match-score-unit {
  font-size: 14px;
  color: var(--orange-deep);
}

.keyword-section {
  margin-bottom: 12px;
}

.keyword-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
  display: block;
}

.keyword-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
}

.keyword-tag.matched {
  background: #f0f9eb;
  color: #67c23a;
}

.keyword-tag.missing {
  background: #fef0f0;
  color: var(--color-danger);
}

/* 润色记录 */
.polish-source {
  font-size: 13px;
  color: var(--text-body);
  margin-bottom: 12px;
}

.polish-notes {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.polish-note-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.5;
}

.note-bullet {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--orange-main);
  flex-shrink: 0;
  margin-top: 6px;
}

/* 面试反馈 */
.feedback-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.feedback-job {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
}

.feedback-mode {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--orange-light-bg);
  color: var(--orange-deep);
}

.feedback-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 12px;
}

.feedback-score-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--orange-main);
}

.feedback-score-unit {
  font-size: 14px;
  color: var(--orange-deep);
}

.feedback-summary {
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: auto;
  padding-top: 8px;
}

/* 短板与建议 */
.weakness-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
}

.weakness-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.weakness-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 10px;
}

.weakness-block-title svg {
  width: 16px;
  height: 16px;
  color: var(--orange-main);
}

.weakness-list {
  margin: 0;
  padding: 0 0 0 20px;
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.8;
}

.suggestions-block {
  border-top: 1px solid var(--orange-light-bg);
  padding-top: 16px;
}

.suggestions-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 12px;
}

.suggestion-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.6;
}

.suggestion-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fff3e8 0%, #ffe0c8 100%);
  color: var(--orange-main);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 1279px) {
  .summary-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .details-section {
    grid-template-columns: 1fr;
  }
  .weakness-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1023px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
  .summary-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767px) {
  .summary-section {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .summary-card {
    padding: 16px;
  }
  .summary-value {
    font-size: 24px;
  }
  .page-title {
    font-size: 18px;
  }
}
</style>
