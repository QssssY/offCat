<template>
  <div class="interview-report-view">
    <div class="page-back">
      <el-button link @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回历史记录
      </el-button>
    </div>

    <div v-if="loading" class="loading-section">
      <div class="loading-content">
        <div class="loading-ring"></div>
        <div class="loading-text">正在加载评估报告...</div>
      </div>
    </div>

    <div v-else-if="error" class="error-section">
      <div class="error-card">
        <div class="error-title">加载失败</div>
        <div class="error-desc">{{ error }}</div>
        <div class="error-actions">
          <el-button type="primary" @click="fetchSessionDetail">重试</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="!isEnded" class="empty-section">
      <div class="empty-card">
        <div class="empty-title">面试尚未结束</div>
        <div class="empty-desc">当前会话结束后，才会生成结构化面试反馈报告。</div>
        <div class="empty-actions">
          <el-button type="primary" @click="goToSession">返回会话</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="isReportGenerating" class="generating-section">
      <div class="generating-card">
        <div class="generating-spinner"></div>
        <div class="generating-title">报告生成中</div>
        <div class="generating-desc">{{ generatingDesc }}</div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
        </div>
        <div class="progress-text">页面会自动刷新，也可以手动刷新</div>
        <div class="generating-actions">
          <el-button type="primary" size="small" :loading="refreshingReport" @click="refreshReportNow">
            立即刷新
          </el-button>
          <el-button size="small" @click="goToSession">查看会话</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="hasReport" class="report-content">
      <div class="hero-section">
        <div class="hero-left">
          <div class="job-name">{{ sessionData?.jobRole || "-" }}</div>
          <div class="score-display">
            <span class="score-number">{{ displayScoreValue ?? "--" }}</span>
            <span class="score-unit">分</span>
          </div>
          <div class="hero-meta-row">
            <el-tag effect="plain">{{ difficultyDesc || difficultyFallback }}</el-tag>
            <el-tag effect="plain">{{ interviewModeDesc || modeFallback }}</el-tag>
            <el-tag v-if="sessionData?.jobTargeted" type="warning" effect="plain">岗位定向</el-tag>
          </div>
        </div>
        <div class="hero-right">
          <div class="summary-title">AI 评估总结</div>
          <p class="summary-text">{{ parsedReport?.summary || "暂无总结" }}</p>
        </div>
      </div>

      <div v-if="sessionData?.jobTargeted && jobTargetFeedback" class="section-card">
        <div class="section-header">
          <h3 class="section-title">岗位相关反馈</h3>
        </div>
        <div class="section-body job-feedback-body">
          <div class="job-feedback-item">
            <div class="job-feedback-label">岗位匹配表现</div>
            <div class="job-feedback-value">{{ jobTargetFeedback.jobMatchPerformance || "暂无" }}</div>
          </div>
          <div class="job-feedback-item" v-if="jobTargetFeedback.strengths?.length">
            <div class="job-feedback-label">优势表现</div>
            <div class="tag-list">
              <el-tag v-for="item in jobTargetFeedback.strengths" :key="`job-strength-${item}`" type="success" effect="plain">
                {{ item }}
              </el-tag>
            </div>
          </div>
          <div class="job-feedback-item" v-if="jobTargetFeedback.weaknesses?.length">
            <div class="job-feedback-label">不足表现</div>
            <div class="tag-list">
              <el-tag v-for="item in jobTargetFeedback.weaknesses" :key="`job-weakness-${item}`" type="warning" effect="plain">
                {{ item }}
              </el-tag>
            </div>
          </div>
          <div class="job-feedback-item" v-if="jobTargetFeedback.improvementSuggestions?.length">
            <div class="job-feedback-label">改进建议</div>
            <ul class="simple-list">
              <li v-for="item in jobTargetFeedback.improvementSuggestions" :key="`job-suggestion-${item}`">{{ item }}</li>
            </ul>
          </div>
        </div>
      </div>

      <div v-if="reportStrengths.length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">优势亮点</h3>
        </div>
        <div class="section-body">
          <ul class="simple-list">
            <li v-for="item in reportStrengths" :key="`strength-${item}`">{{ item }}</li>
          </ul>
        </div>
      </div>

      <div v-if="reportWeaknesses.length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">不足表现</h3>
        </div>
        <div class="section-body">
          <ul class="simple-list">
            <li v-for="item in reportWeaknesses" :key="`weakness-${item}`">{{ item }}</li>
          </ul>
        </div>
      </div>

      <div v-if="reportSuggestions.length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">改进建议</h3>
        </div>
        <div class="section-body">
          <ul class="simple-list">
            <li v-for="item in reportSuggestions" :key="`suggestion-${item}`">{{ item }}</li>
          </ul>
        </div>
      </div>

      <div v-if="dimensionCards.length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">维度评分</h3>
        </div>
        <div class="section-body dimension-grid">
          <div v-for="item in dimensionCards" :key="item.key" class="dimension-card">
            <div class="dimension-label">{{ item.label }}</div>
            <div class="dimension-score">{{ item.score }}</div>
            <div class="dimension-comment">{{ item.comment || "暂无补充说明" }}</div>
          </div>
        </div>
      </div>

      <div v-if="reportQuestionPerformance.length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">逐题表现</h3>
        </div>
        <div class="section-body">
          <div v-for="(item, index) in reportQuestionPerformance" :key="`question-${index}`" class="question-card">
            <div class="question-title">Q{{ index + 1 }} · {{ item.question || "未记录问题" }}</div>
            <div class="question-answer">{{ item.answer || "未记录回答" }}</div>
            <div class="question-footer">
              <span class="question-score">得分：{{ item.score ?? "--" }}</span>
              <div class="tag-list">
                <el-tag
                  v-for="tag in item.knowledgeTags || []"
                  :key="`question-tag-${index}-${tag}`"
                  size="small"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
            <div v-if="item.comment" class="question-comment">{{ item.comment }}</div>
          </div>
        </div>
      </div>

      <div class="action-section">
        <div class="action-group">
          <el-button @click="goBack" class="action-btn secondary">返回历史</el-button>
          <el-button @click="goToSession" class="action-btn secondary">查看会话</el-button>
          <el-button type="primary" class="action-btn primary" @click="goToEntry">再来一次</el-button>
        </div>
      </div>
    </div>

    <div v-else class="empty-section">
      <div class="empty-card">
        <div class="empty-title">暂无评估报告</div>
        <div class="empty-desc">系统仍在生成评估结果，请稍后刷新查看。</div>
        <el-button type="primary" :loading="refreshingReport" @click="refreshReportNow">立即刷新</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getInterviewSession } from "@/api/interview";

const route = useRoute();
const router = useRouter();
const sessionId = computed(() => route.params.sessionId);

const loading = ref(true);
const error = ref("");
const sessionData = ref(null);
const reportPolling = ref(false);
const refreshingReport = ref(false);
const reportPollRounds = ref(0);
const REPORT_POLL_INTERVAL_MS = 3000;
const REPORT_POLL_MAX_ROUNDS = 120;
let reportPollingTimer = null;

const isEnded = computed(() => sessionData.value?.status === 1);

const parsedReport = computed(() => {
  const report = sessionData.value?.evaluationReport;
  if (!report) return null;
  if (typeof report === "string") {
    let trimmed = report.trim();
    if (trimmed.startsWith("```json")) trimmed = trimmed.slice(7);
    else if (trimmed.startsWith("```")) trimmed = trimmed.slice(3);
    const lastBacktick = trimmed.lastIndexOf("```");
    if (lastBacktick > 0) trimmed = trimmed.slice(0, lastBacktick);
    trimmed = trimmed.trim();
    if (!trimmed) return null;
    try {
      return JSON.parse(trimmed);
    } catch {
      return null;
    }
  }
  return report;
});

const hasReport = computed(() => parsedReport.value !== null);
const shouldPollReport = computed(() => isEnded.value && !hasReport.value);
const isReportGenerating = computed(() => isEnded.value && !hasReport.value && reportPolling.value);

const generatingDesc = computed(() => {
  if (reportPollRounds.value <= 3) {
    return "AI 正在整理你的面试表现与岗位匹配反馈。";
  }
  if (reportPollRounds.value <= 10) {
    return "报告仍在生成中，请再等待几秒。";
  }
  return "报告生成时间稍长，页面会继续自动刷新。";
});

const progressPercent = computed(() => Math.min(Math.round((reportPollRounds.value / REPORT_POLL_MAX_ROUNDS) * 100), 95));

const displayScoreValue = computed(() => {
  const score = sessionData.value?.comprehensiveScore;
  return score === null || score === undefined ? null : Number(score);
});

const difficultyDesc = computed(() => sessionData.value?.difficultyDesc || "");
const interviewModeDesc = computed(() => sessionData.value?.interviewModeDesc || "");

const difficultyFallback = computed(() => {
  const map = { 1: "初级", 2: "中级", 3: "高级" };
  return map[sessionData.value?.difficulty] || "--";
});

const modeFallback = computed(() => {
  return sessionData.value?.interviewMode === "stress" ? "压力面试" : "普通面试";
});

const reportStrengths = computed(() => parsedReport.value?.strengths || []);
const reportWeaknesses = computed(() => [
  ...(parsedReport.value?.weaknesses || []),
  ...(parsedReport.value?.missingCompetencies || []),
]);
const reportSuggestions = computed(() => [
  ...(parsedReport.value?.improvementSuggestions || []),
  ...(parsedReport.value?.suggestions || []),
]);
const reportQuestionPerformance = computed(() => parsedReport.value?.questionPerformance || []);

const jobTargetFeedback = computed(() => sessionData.value?.jobTargetContext?.jobTargetedFeedback || null);

const dimensionCards = computed(() => {
  const report = parsedReport.value;
  if (!report) return [];
  const source = [
    { key: "jobMatch", label: "岗位匹配", value: report.jobMatch },
    { key: "technicalDepth", label: "技术深度", value: report.technicalDepth },
    { key: "communication", label: "沟通表达", value: report.communication },
    { key: "problemSolving", label: "问题解决", value: report.problemSolving },
    { key: "pressureResistance", label: "抗压表现", value: report.pressureResistance },
  ];
  return source
    .filter((item) => item.value && (item.value.score !== null && item.value.score !== undefined))
    .map((item) => ({
      key: item.key,
      label: item.label,
      score: item.value.score,
      comment: item.value.comment,
    }));
});

const fetchSessionDetail = async (options = {}) => {
  const { showLoading = true, silentError = false } = options;
  if (!sessionId.value) {
    if (!silentError) {
      error.value = "会话 ID 不存在";
    }
    loading.value = false;
    return;
  }

  if (showLoading) loading.value = true;
  if (!silentError) error.value = "";
  try {
    const res = await getInterviewSession(sessionId.value);
    sessionData.value = res.data;
  } catch (err) {
    if (!silentError) {
      error.value = err.message || "获取评估报告失败，请稍后重试";
    }
  } finally {
    if (showLoading) loading.value = false;
  }
};

const startReportPolling = () => {
  if (reportPollingTimer || !sessionId.value) return;
  reportPolling.value = true;
  reportPollRounds.value = 0;
  reportPollingTimer = setInterval(async () => {
    if (refreshingReport.value) return;
    refreshingReport.value = true;
    try {
      await fetchSessionDetail({ showLoading: false, silentError: true });
      reportPollRounds.value += 1;
      if (hasReport.value) {
        stopReportPolling();
        ElMessage.success("评估报告已生成");
        return;
      }
      if (reportPollRounds.value >= REPORT_POLL_MAX_ROUNDS) {
        stopReportPolling();
      }
    } finally {
      refreshingReport.value = false;
    }
  }, REPORT_POLL_INTERVAL_MS);
};

const stopReportPolling = () => {
  if (reportPollingTimer) {
    clearInterval(reportPollingTimer);
    reportPollingTimer = null;
  }
  reportPolling.value = false;
  reportPollRounds.value = 0;
};

const refreshReportNow = async () => {
  if (!sessionId.value) return;
  refreshingReport.value = true;
  try {
    await fetchSessionDetail({ showLoading: false, silentError: true });
  } finally {
    refreshingReport.value = false;
  }
};

const goBack = () => router.push("/interview/history");
const goToSession = () => sessionId.value && router.push(`/interview/session/${sessionId.value}`);
const goToEntry = () => router.push("/interview/entry");

onMounted(() => {
  fetchSessionDetail();
});

watch(
  shouldPollReport,
  (needPolling) => {
    if (needPolling) {
      startReportPolling();
    } else {
      stopReportPolling();
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  stopReportPolling();
});
</script>

<style scoped>
.interview-report-view {
  min-height: 100%;
  background: #f8f6f3;
  padding: 24px;
}

.page-back {
  margin-bottom: 16px;
}

.back-btn {
  color: #909399;
}

.loading-section,
.error-section,
.empty-section,
.generating-section {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
}

.loading-content,
.error-card,
.empty-card,
.generating-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  max-width: 480px;
  border: 1px solid rgba(243, 216, 199, 0.5);
}

.loading-ring,
.generating-spinner {
  width: 56px;
  height: 56px;
  margin: 0 auto 20px;
  border: 4px solid #f3d8c7;
  border-top-color: #ff8c42;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text,
.error-desc,
.empty-desc,
.generating-desc,
.progress-text {
  font-size: 14px;
  line-height: 1.7;
  color: #666666;
}

.error-title,
.empty-title,
.generating-title {
  font-size: 20px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 10px;
}

.error-actions,
.empty-actions,
.generating-actions,
.action-group {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.progress-bar {
  height: 6px;
  background: #f3d8c7;
  border-radius: 3px;
  overflow: hidden;
  margin: 20px 0 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff8c42 0%, #ffb380 100%);
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-section,
.section-card {
  background: #ffffff;
  border-radius: 18px;
  border: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.06);
}

.hero-section {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  padding: 28px 32px;
}

.job-name {
  font-size: 18px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 14px;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-number {
  font-size: 56px;
  font-weight: 700;
  color: #ff8c42;
  line-height: 1;
}

.score-unit {
  font-size: 18px;
  color: #ff8c42;
}

.hero-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: #ff8c42;
  margin-bottom: 10px;
}

.summary-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #2f2f2f;
}

.section-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(243, 216, 199, 0.3);
  background: linear-gradient(135deg, #fff8f3 0%, #fff 100%);
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.section-body {
  padding: 20px;
}

.job-feedback-body,
.dimension-grid {
  display: grid;
  gap: 16px;
}

.dimension-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.job-feedback-item,
.dimension-card,
.question-card {
  background: #fffaf7;
  border: 1px solid rgba(243, 216, 199, 0.35);
  border-radius: 12px;
  padding: 14px 16px;
}

.job-feedback-label,
.dimension-label {
  font-size: 13px;
  font-weight: 600;
  color: #8a5b39;
  margin-bottom: 8px;
}

.job-feedback-value,
.dimension-comment,
.question-answer,
.question-comment {
  font-size: 13px;
  line-height: 1.7;
  color: #2f2f2f;
}

.dimension-score {
  font-size: 28px;
  font-weight: 700;
  color: #ff8c42;
  margin-bottom: 6px;
}

.simple-list {
  margin: 0;
  padding-left: 18px;
  color: #2f2f2f;
}

.simple-list li {
  line-height: 1.8;
  margin-bottom: 6px;
}

.simple-list li:last-child {
  margin-bottom: 0;
}

.question-card {
  margin-bottom: 14px;
}

.question-card:last-child {
  margin-bottom: 0;
}

.question-title {
  font-size: 14px;
  font-weight: 600;
  color: #2f2f2f;
  margin-bottom: 10px;
}

.question-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.question-score {
  font-size: 13px;
  color: #ff8c42;
  font-weight: 600;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-section {
  padding: 8px 0 24px;
}

.action-btn {
  border-radius: 24px;
  padding: 10px 24px;
}

.action-btn.primary {
  background: linear-gradient(135deg, #ff8c42 0%, #ff7a30 100%);
  border: none;
  color: #ffffff;
}

@media (max-width: 900px) {
  .hero-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .interview-report-view {
    padding: 16px;
  }

  .loading-content,
  .error-card,
  .empty-card,
  .generating-card {
    padding: 28px 20px;
  }

  .hero-section {
    padding: 20px;
  }

  .score-number {
    font-size: 44px;
  }

  .action-group {
    flex-direction: column;
  }
}
</style>
