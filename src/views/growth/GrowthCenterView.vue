<template>
  <div class="growth-center">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <FeatureIcon name="growth-center" size="md" />
        </div>
        <div>
          <h1 class="page-title">个人成长中心</h1>
          <p class="page-desc">查看你的成长轨迹与个性化建议</p>
        </div>
      </div>
      <el-button
        circle
        size="small"
        :loading="loading"
        @click="fetchData"
        class="refresh-btn"
      >
        <FeatureIcon v-if="!loading" name="retry" size="xs" />
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <FeatureIcon name="loading" size="sm" class="loading-feature-icon" />
      <span>正在加载成长数据...</span>
    </div>

    <!-- 加载失败状态 -->
    <div v-else-if="loadError" class="error-state">
      <div class="error-icon-wrapper">
        <FeatureIcon name="error" size="lg" />
      </div>
      <h3 class="error-title">加载失败</h3>
      <p class="error-desc">获取成长数据时出现问题，请重试</p>
      <el-button type="primary" @click="fetchData">重新加载</el-button>
    </div>

    <!-- 全量无数据状态 -->
    <div v-else-if="isEmpty" class="empty-full-state">
      <div class="empty-icon-wrapper">
        <FeatureIcon name="empty-state" size="lg" />
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
            <FeatureIcon name="resume-score" size="sm" />
          </div>
          <div class="summary-value">{{ summary.latestResumeScore ?? '--' }}</div>
          <div class="summary-label">最近简历分</div>
        </div>
        <div class="summary-card">
          <div class="summary-icon interview-icon">
            <FeatureIcon name="interview-report" size="sm" />
          </div>
          <div class="summary-value">{{ summary.latestInterviewScore ?? '--' }}</div>
          <div class="summary-label">最近面试分</div>
        </div>
        <div class="summary-card">
          <div class="summary-icon count-icon">
            <FeatureIcon name="resume-analysis" size="sm" />
          </div>
          <div class="summary-value">{{ summary.resumeDiagnosisCount ?? 0 }}</div>
          <div class="summary-label">简历诊断次数</div>
        </div>
        <div class="summary-card">
          <div class="summary-icon count-icon">
            <FeatureIcon name="mock-interview" size="sm" />
          </div>
          <div class="summary-value">{{ summary.mockInterviewCount ?? 0 }}</div>
          <div class="summary-label">模拟面试次数</div>
        </div>
      </section>

      <!-- 管理端配置驱动的成长激励与里程碑 -->
      <section v-if="hasGrowthConfig" class="growth-config-section">
        <div v-if="encouragementMessages.length" class="growth-config-block">
          <h3 class="card-title">成长激励</h3>
          <div class="encouragement-list">
            <div
              v-for="(message, index) in encouragementMessages"
              :key="index"
              class="encouragement-item"
            >
              <span class="encouragement-dot"></span>
              <span>{{ message }}</span>
            </div>
          </div>
        </div>
        <div v-if="milestones.length" class="growth-config-block">
          <h3 class="card-title">成长里程碑</h3>
          <div class="milestone-list">
            <div
              v-for="milestone in milestones"
              :key="milestone.configKey"
              class="milestone-item"
            >
              <div class="milestone-marker"></div>
              <div class="milestone-content">
                <div class="milestone-title">{{ milestone.title }}</div>
                <div v-if="milestone.description" class="milestone-desc">{{ milestone.description }}</div>
              </div>
            </div>
          </div>
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
            <FeatureIcon name="resume-analysis" size="sm" />
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
            <FeatureIcon name="interview-report" size="sm" />
            <span>暂无数据，完成更多面试后将展示趋势</span>
          </div>
        </div>
      </section>

      <!-- 面试维度雷达区块 -->
      <section class="radar-section">
        <h3 class="card-title">面试维度雷达</h3>
        <div v-if="radarLoading" class="radar-loading">
          <div class="loading-spinner"></div>
          <span>加载维度数据...</span>
        </div>
        <template v-else-if="radarData && radarData.sessionCount > 0">
          <!-- 雷达图 + 维度详情面板 -->
          <div class="radar-top-row">
            <div class="radar-chart-col">
              <RadarChart
                :scores="radarScores"
                :labels="radarLabels"
                :keys="radarKeys"
              />
              <div class="radar-session-info">
                来源面试：{{ radarData.latestRadar?.createTime || '--' }}
              </div>
            </div>
            <div class="radar-panel-col">
              <RadarScorePanel
                :details="radarDetails"
                :dimensionConfig="interviewDimensionConfig"
              />
            </div>
          </div>

          <!-- 维度趋势折线图 -->
          <div v-if="trendLabels.length > 1" class="radar-trend-card">
            <h4 class="sub-title">维度趋势</h4>
            <LineChart
              :labels="trendLabels"
              :datasets="trendDatasets"
              :showLegend="true"
            />
          </div>

          <!-- 盲区提示 -->
          <div v-if="radarData.blindSpotTips?.length" class="blind-spot-list">
            <h4 class="sub-title">盲区提示</h4>
            <div
              v-for="tip in radarData.blindSpotTips"
              :key="tip.dimensionKey"
              class="blind-spot-card"
              :class="'blind-spot-card--' + tip.type"
            >
              <div class="blind-spot-header">
                <span class="blind-spot-dim">{{ tip.dimensionLabel }}</span>
                <span class="blind-spot-badge" :class="'badge--' + tip.type">
                  {{ tip.type === 'persistent_low' ? '持续低分' : '下滑趋势' }}
                </span>
                <span class="blind-spot-avg">均分 {{ Math.round(tip.averageScore) }}</span>
              </div>
              <div class="blind-spot-tip">{{ tip.tip }}</div>
              <div v-if="tip.suggestions?.length" class="blind-spot-suggestions">
                <div v-for="(sg, i) in tip.suggestions" :key="i" class="blind-spot-sg-item">
                  <span class="sg-bullet"></span>
                  <span>{{ sg }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="radar-empty">
          <FeatureIcon name="growth-radar" size="sm" />
          <span>暂无维度数据，完成面试后将展示雷达分析</span>
          <el-button link type="primary" @click="$router.push('/interview/entry')">去面试</el-button>
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
              <FeatureIcon name="resume-optimization" size="xs" />
              简历侧
            </div>
            <ul class="weakness-list">
              <li v-for="(w, i) in weaknessSummary.resumeWeaknesses" :key="i">{{ w }}</li>
            </ul>
          </div>
          <div v-if="weaknessSummary.jobMatchWeaknesses?.length" class="weakness-block">
            <div class="weakness-block-title">
              <FeatureIcon name="job-match-analysis" size="xs" />
              岗位匹配侧
            </div>
            <ul class="weakness-list">
              <li v-for="(w, i) in weaknessSummary.jobMatchWeaknesses" :key="i">{{ w }}</li>
            </ul>
          </div>
          <div v-if="weaknessSummary.interviewWeaknesses?.length" class="weakness-block">
            <div class="weakness-block-title">
              <FeatureIcon name="interview-feedback" size="xs" />
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
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import { getGrowthOverview, getInterviewRadar } from '@/api/growth'

defineOptions({
  name: 'GrowthCenterView'
})
import FeatureIcon from '@/components/common/FeatureIcon.vue'

const LineChart = defineAsyncComponent(() => import('@/components/resume/LineChart.vue'))
const RadarChart = defineAsyncComponent(() => import('@/components/resume/RadarChart.vue'))
const RadarScorePanel = defineAsyncComponent(() => import('@/components/resume/RadarScorePanel.vue'))

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
/** 管理端维护的成长中心运营配置 */
const growthConfig = computed(() => overviewData.value?.growthConfig || {})
/** 激励文案 */
const encouragementMessages = computed(() => growthConfig.value?.encouragementMessages || [])
/** 成长里程碑 */
const milestones = computed(() => growthConfig.value?.milestones || [])

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

/** 是否有管理端配置内容 */
const hasGrowthConfig = computed(() => encouragementMessages.value.length > 0 || milestones.value.length > 0)

/** 面试模式中文标签 */
const modeLabel = (mode) => {
  const map = { normal: '普通面试', stress: '压力面试', job_targeted: '岗位定向面试' }
  return map[mode] || mode || '普通面试'
}

// ==================== 面试维度雷达相关 ====================

/** 面试 6 维度配置 */
const interviewDimensionConfig = [
  { key: 'technicalDepth', label: '技术深度' },
  { key: 'projectExpression', label: '项目表达' },
  { key: 'communication', label: '沟通表达' },
  { key: 'problemSolving', label: '问题解决' },
  { key: 'pressureResistance', label: '抗压表现' },
  { key: 'jobMatch', label: '岗位匹配' },
]

/** 维度颜色方案 */
const dimensionColors = {
  technicalDepth: '#FF8C42',
  projectExpression: '#3ABAB4',
  communication: '#5B8DEF',
  problemSolving: '#E667AF',
  pressureResistance: '#F5A623',
  jobMatch: '#7B68EE',
}

/** 雷达数据 */
const radarData = ref(null)
/** 雷达加载状态 */
const radarLoading = ref(false)

/** 雷达图标签 */
const radarLabels = interviewDimensionConfig.map(d => d.label)
/** 雷达图 key */
const radarKeys = interviewDimensionConfig.map(d => d.key)

/** 雷达图 scores 对象（RadarChart 要求 { key: score } 或 { key: { score } }） */
const radarScores = computed(() => {
  const dims = radarData.value?.latestRadar?.dimensions
  if (!dims) return {}
  const result = {}
  for (const cfg of interviewDimensionConfig) {
    const d = dims[cfg.key]
    result[cfg.key] = d ? { score: d.score || 0 } : { score: 0 }
  }
  return result
})

/** RadarScorePanel 维度详情（兼容 plus/minus 字段名） */
const radarDetails = computed(() => {
  const dims = radarData.value?.latestRadar?.dimensions
  if (!dims) return {}
  const result = {}
  for (const cfg of interviewDimensionConfig) {
    const d = dims[cfg.key]
    result[cfg.key] = {
      score: d?.score || 0,
      strengths: d?.strengths || [],
      weaknesses: d?.weaknesses || [],
    }
  }
  return result
})

/** 维度趋势 X 轴标签（取第一个有数据的维度的日期列表） */
const trendLabels = computed(() => {
  const trends = radarData.value?.dimensionTrends
  if (!trends?.length) return []
  const firstWithData = trends.find(t => t.points?.length > 0)
  return firstWithData?.points?.map(p => p.date) || []
})

/** 维度趋势数据集（6 条折线） */
const trendDatasets = computed(() => {
  const trends = radarData.value?.dimensionTrends
  if (!trends?.length) return []
  return trends.map(t => ({
    label: t.dimensionLabel,
    data: t.points?.map(p => p.score) || [],
    borderColor: dimensionColors[t.dimensionKey] || '#999',
    backgroundColor: 'transparent',
  }))
})

/** 获取雷达数据 */
const fetchRadarData = async () => {
  radarLoading.value = true
  try {
    const res = await getInterviewRadar()
    radarData.value = res.data
  } catch {
    radarData.value = null
  } finally {
    radarLoading.value = false
  }
}

/** 获取成长中心数据 */
const fetchData = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await getGrowthOverview()
    overviewData.value = res.data
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
  // 雷达数据独立加载，不阻塞概览
  fetchRadarData()
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
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.08) 100%);
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

.loading-feature-icon {
  animation: spin 1s linear infinite;
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
  background: linear-gradient(135deg, var(--tag-bg-danger) 0%, rgba(245, 108, 108, 0.08) 100%);
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
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.08) 100%);
  color: var(--orange-deep);
}

.interview-icon {
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.06) 100%);
  color: var(--orange-main);
}

.count-icon {
  background: var(--icon-bg-success);
  color: var(--color-success);
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

/* 管理端配置驱动内容 */
.growth-config-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.growth-config-block {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
}

.encouragement-list,
.milestone-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.encouragement-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-body);
}

.encouragement-dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: var(--orange-main);
  box-shadow: 0 0 0 4px rgba(255, 140, 66, 0.1);
  flex-shrink: 0;
}

.milestone-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.milestone-marker {
  width: 12px;
  height: 12px;
  margin-top: 4px;
  border-radius: 50%;
  border: 2px solid var(--orange-main);
  background: var(--bg-card);
  flex-shrink: 0;
}

.milestone-content {
  min-width: 0;
}

.milestone-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
  line-height: 1.5;
}

.milestone-desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
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
  min-height: 340px;
}

.chart-card :deep(.line-chart-wrapper) {
  min-height: 260px;
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
  background: rgba(103, 194, 58, 0.15);
  color: var(--color-success);
}

.keyword-tag.missing {
  background: var(--tag-bg-danger);
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
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.08) 100%);
  color: var(--orange-main);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 面试维度雷达区块 */
.radar-section {
  background: var(--bg-card);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
  margin-bottom: 24px;
}

.radar-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  gap: 12px;
  color: var(--text-muted);
  font-size: 13px;
}

.radar-top-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.radar-chart-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 360px;
}

.radar-chart-col :deep(.radar-chart-wrapper) {
  min-height: 320px;
}

.radar-session-info {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: 8px;
}

.radar-panel-col {
  min-width: 0;
  min-height: 320px;
}

.radar-trend-card {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 340px;
}

.radar-trend-card :deep(.line-chart-wrapper) {
  min-height: 260px;
}

.sub-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
  align-self: flex-start;
}

.radar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
  color: var(--text-placeholder);
  font-size: 13px;
}

.radar-empty svg {
  width: 36px;
  height: 36px;
  color: var(--border-card);
}

/* 盲区提示 */
.blind-spot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.blind-spot-card {
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid;
}

.blind-spot-card--persistent_low {
  background: rgba(245, 108, 108, 0.04);
  border-color: rgba(245, 108, 108, 0.2);
}

.blind-spot-card--declining_trend {
  background: rgba(245, 166, 35, 0.04);
  border-color: rgba(245, 166, 35, 0.2);
}

.blind-spot-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.blind-spot-dim {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
}

.blind-spot-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.badge--persistent_low {
  background: rgba(245, 108, 108, 0.12);
  color: var(--color-danger);
}

.badge--declining_trend {
  background: rgba(245, 166, 35, 0.12);
  color: #b67e1a;
}

.blind-spot-avg {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
}

.blind-spot-tip {
  font-size: 13px;
  color: var(--text-body);
  line-height: 1.6;
  margin-bottom: 10px;
}

.blind-spot-suggestions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.blind-spot-sg-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: var(--text-body);
  line-height: 1.5;
}

.sg-bullet {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--orange-main);
  flex-shrink: 0;
  margin-top: 5px;
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
  .growth-config-section {
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
  .radar-top-row {
    grid-template-columns: 1fr;
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
