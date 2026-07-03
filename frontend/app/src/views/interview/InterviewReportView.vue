<template>
  <div class="interview-report-view">
    <div class="page-back">
      <el-button link @click="goBack" class="back-btn">
        <FeatureIcon name="back" size="xs" />
        返回历史记录
      </el-button>
    </div>

    <div v-if="loading" class="loading-section">
      <AiLoadingState
        title="正在加载评估报告..."
        :stages="interviewStages"
        :currentStageIndex="0"
        :showElapsedTime="true"
      />
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
        <div class="empty-desc">
          当前会话结束后，才会生成结构化面试反馈报告。
        </div>
        <div class="empty-actions">
          <el-button type="primary" @click="goToSession">返回会话</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="isReportGenerating" class="generating-section">
      <AiLoadingState
        title="报告生成中"
        :stages="interviewStages"
        :currentStageIndex="reportStageIndex"
        :messages="interviewLoadingMessages"
        :showElapsedTime="true"
        :showRefreshButton="true"
        :refreshLoading="refreshingReport"
        @refresh="refreshReportNow"
      >
        <template #actions>
          <el-button size="small" @click="goToSession">查看会话</el-button>
        </template>
      </AiLoadingState>
    </div>

    <div v-else-if="hasReport" class="report-content interview-report-shell">
      <div class="hero-section report-hero-shell">
        <div class="hero-left report-score-panel">
          <div class="job-name">{{ sessionData?.jobRole || "-" }}</div>
          <div class="score-display">
            <span class="score-number">{{ displayScoreValue ?? "--" }}</span>
            <span class="score-unit">分</span>
            <el-tag v-if="reportLevel" :type="levelTagType" effect="dark" size="large" class="level-badge">{{ levelLabel }}</el-tag>
          </div>
          <div class="hero-meta-row">
            <el-tag effect="plain">{{
              difficultyDesc || difficultyFallback
            }}</el-tag>
            <el-tag effect="plain">{{
              interviewModeDesc || modeFallback
            }}</el-tag>
            <el-tag
              v-if="sessionData?.jobTargeted"
              type="warning"
              effect="plain"
              >岗位定向</el-tag
            >
          </div>
        </div>
        <div class="hero-right report-summary-panel">
          <div class="summary-title">AI 评估总结</div>
          <p class="summary-text">{{ parsedReport?.summary || "暂无总结" }}</p>
        </div>
      </div>

      <div
        v-if="reportImmediateActions.length"
        class="section-card priority-section"
      >
        <div class="section-header">
          <h3 class="section-title">3 条立即能做的事</h3>
        </div>
        <div class="section-body action-plan-list report-priority-grid">
          <div
            v-for="(item, index) in reportImmediateActions"
            :key="`immediate-action-${index}-${item}`"
            class="action-plan-item"
          >
            <div class="action-plan-index">{{ index + 1 }}</div>
            <div class="action-plan-text">{{ item }}</div>
          </div>
        </div>
      </div>

      <div class="report-section-grid report-diagnosis-stack">
      <div class="section-card grade-ref-card">
        <div class="section-header">
          <h3 class="section-title">评分参考</h3>
        </div>
        <div class="section-body">
          <div class="grade-table">
            <div v-for="g in gradeScale" :key="g.level" class="grade-row" :class="{ active: reportLevel === g.level }">
              <span class="grade-level" :class="'grade-' + g.level.toLowerCase()">{{ g.level }}</span>
              <span class="grade-range">{{ g.range }}</span>
              <span class="grade-label">{{ g.label }}</span>
              <span class="grade-hire">{{ g.hire }}</span>
              <span v-if="reportLevel === g.level" class="grade-marker">你在此</span>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="sessionData?.jobTargeted && jobTargetFeedback"
        class="section-card"
      >
        <div class="section-header">
          <h3 class="section-title">岗位相关反馈</h3>
        </div>
        <div class="section-body job-feedback-body">
          <div class="job-feedback-item">
            <div class="job-feedback-label">岗位匹配表现</div>
            <div class="job-feedback-value">
              {{ jobTargetFeedback.jobMatchPerformance || "暂无" }}
            </div>
          </div>
          <div
            class="job-feedback-item"
            v-if="jobTargetStrengths.length"
          >
            <div class="job-feedback-label">优势表现</div>
            <div class="tag-list">
              <el-tag
                v-for="item in jobTargetStrengths"
                :key="`job-strength-${item}`"
                type="success"
                effect="plain"
              >
                {{ item }}
              </el-tag>
            </div>
          </div>
          <div
            class="job-feedback-item"
            v-if="jobTargetWeaknesses.length"
          >
            <div class="job-feedback-label">不足表现</div>
            <div class="tag-list">
              <el-tag
                v-for="item in jobTargetWeaknesses"
                :key="`job-weakness-${item}`"
                type="warning"
                effect="plain"
              >
                {{ item }}
              </el-tag>
            </div>
          </div>
          <div
            class="job-feedback-item"
            v-if="jobTargetSuggestions.length"
          >
            <div class="job-feedback-label">改进建议</div>
            <ul class="simple-list">
              <li
                v-for="item in jobTargetSuggestions"
                :key="`job-suggestion-${item}`"
              >
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div v-if="replayRounds.length" class="section-card">
        <button
          type="button"
          class="section-header section-toggle"
          @click="replayExpanded = !replayExpanded"
        >
          <span class="section-title-wrap">
            <h3 class="section-title">面试历史回放</h3>
            <span class="section-count">{{ replayRounds.length }} 轮</span>
          </span>
          <span class="section-toggle-text">{{
            replayExpanded ? "收起" : "展开"
          }}</span>
        </button>
        <div v-if="replayExpanded" class="section-body replay-timeline">
          <div
            v-for="item in replayRounds"
            :key="`replay-round-${item.roundNo}-${item.answerMessageId}`"
            class="replay-round"
          >
            <div class="replay-round-marker">{{ item.roundNo }}</div>
            <div class="replay-round-content">
              <button
                type="button"
                class="replay-round-toggle"
                @click="toggleReplayRound(replayRoundKey(item))"
              >
                <span>第 {{ item.roundNo }} 轮对话</span>
                <span class="replay-round-toggle-text">
                  {{
                    isReplayRoundExpanded(replayRoundKey(item))
                      ? "收起"
                      : "展开"
                  }}
                </span>
              </button>
              <div
                v-if="isReplayRoundExpanded(replayRoundKey(item))"
                class="replay-round-body"
              >
                <div class="replay-block question">
                  <div class="replay-label">面试官</div>
                  <div class="replay-text">
                    {{
                      getReplayQuestionDisplay(item).mainContent || "未记录问题"
                    }}
                  </div>
                </div>
                <div class="replay-block answer">
                  <div class="replay-label">你的回答</div>
                  <div class="replay-text">
                    {{ item.answerContent || "未记录回答" }}
                  </div>
                  <div v-if="item.answerTime" class="replay-time">
                    {{ formatReplayTime(item.answerTime) }}
                  </div>
                </div>
                <div v-if="item.feedbackContent" class="replay-block feedback">
                  <div class="replay-label">AI 追问</div>
                  <div class="replay-text">
                    {{ getReplayFeedbackDisplay(item).mainContent }}
                  </div>
                  <div
                    v-if="getReplayFeedbackDisplay(item).feedbackContent"
                    class="replay-feedback-card"
                  >
                    <div class="replay-feedback-title">上一题回答的反馈</div>
                    <div class="replay-feedback-text">
                      {{ getReplayFeedbackDisplay(item).feedbackContent }}
                    </div>
                  </div>
                  <div v-if="item.feedbackTime" class="replay-time">
                    {{ formatReplayTime(item.feedbackTime) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="reportRoundReviews.length" class="section-card">
        <button
          type="button"
          class="section-header section-toggle"
          @click="roundReviewsExpanded = !roundReviewsExpanded"
        >
          <span class="section-title-wrap">
            <h3 class="section-title">逐轮复盘</h3>
            <span class="section-count"
              >{{ reportRoundReviews.length }} 条</span
            >
          </span>
          <span class="section-toggle-text">{{
            roundReviewsExpanded ? "收起" : "展开"
          }}</span>
        </button>
        <div v-if="roundReviewsExpanded" class="section-body round-review-list">
          <div
            v-for="(item, index) in reportRoundReviews"
            :key="`round-review-${index}`"
            class="round-review-item"
          >
            <div class="round-review-head">
              <span class="round-review-title"
                >Q{{ item.roundNo || index + 1 }}</span
              >
              <el-tag v-if="item.score != null" size="small" effect="plain"
                >{{ item.score }}分</el-tag
              >
            </div>
            <div class="round-review-question">
              <span class="round-review-speaker">面试官：</span>
              <span>{{ item.question || "未记录问题" }}</span>
            </div>
            <div class="round-review-answer">
              <span class="round-review-speaker candidate">求职者：</span>
              <span>{{ item.answer || "未记录回答" }}</span>
            </div>
            <div v-if="item.replayAnalysis" class="round-review-block">
              <span class="round-review-label">复盘</span>
              <span>{{ item.replayAnalysis }}</span>
            </div>
            <div v-if="item.missedFollowUp" class="round-review-block warning">
              <span class="round-review-label">追问失分</span>
              <span>{{ item.missedFollowUp }}</span>
            </div>
            <div v-if="item.nextPractice" class="round-review-block">
              <span class="round-review-label">下次练法</span>
              <span>{{ item.nextPractice }}</span>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="
          reportFollowUpLossPoints.length || reportCommonLossPatterns.length
        "
        class="section-card"
      >
        <div class="section-header">
          <h3 class="section-title">失分模式</h3>
        </div>
        <div class="section-body loss-pattern-grid">
          <div
            v-if="reportFollowUpLossPoints.length"
            class="loss-pattern-column"
          >
            <div class="loss-pattern-title">追问没接住的点</div>
            <ul class="simple-list">
              <li
                v-for="item in reportFollowUpLossPoints"
                :key="`follow-up-loss-${item}`"
              >
                {{ item }}
              </li>
            </ul>
          </div>
          <div
            v-if="reportCommonLossPatterns.length"
            class="loss-pattern-column"
          >
            <div class="loss-pattern-title">常见失分模式</div>
            <ul class="simple-list">
              <li
                v-for="item in reportCommonLossPatterns"
                :key="`common-loss-${item}`"
              >
                {{ item }}
              </li>
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
            <li v-for="item in reportStrengths" :key="`strength-${item}`">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>

      <div v-if="reportWeaknesses.length" class="section-card weakness-list">
        <div class="section-header">
          <h3 class="section-title">不足表现</h3>
        </div>
        <div class="section-body">
          <ul class="simple-list">
            <li v-for="item in reportWeaknesses" :key="`weakness-${item}`">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>

      <div v-if="reportSuggestions.length" class="section-card suggestion-list">
        <div class="section-header">
          <h3 class="section-title">改进建议</h3>
        </div>
        <div class="section-body">
          <ul class="simple-list">
            <li v-for="item in reportSuggestions" :key="`suggestion-${item}`">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>

      <div v-if="dimensionCards.length" class="section-card dimension-detail-section">
        <div class="section-header">
          <h3 class="section-title">维度详情</h3>
        </div>
        <div class="section-body dimension-grid">
          <div
            v-for="item in dimensionCards"
            :key="item.key"
            class="dimension-card"
          >
            <div class="dimension-label">{{ item.label }}</div>
            <div class="dimension-score">{{ item.score }}</div>
            <div class="dimension-comment">
              {{ item.comment || "暂无补充说明" }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="Object.keys(radarScores).length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">维度评分</h3>
        </div>
        <div class="section-body radar-layout">
          <div class="radar-chart-area">
            <RadarChart
              :labels="radarLabels"
              :keys="radarKeys"
              :scores="radarScores"
            />
          </div>
          <div class="radar-panel-area">
            <RadarScorePanel
              :details="radarScores"
              :dimension-config="radarDimensionConfig"
            />
          </div>
        </div>
      </div>

      <div v-if="reportQuestionPerformance.length" class="section-card">
        <div class="section-header">
          <h3 class="section-title">逐题表现</h3>
        </div>
        <div class="section-body">
          <el-collapse v-model="activeQuestions">
            <el-collapse-item
              v-for="(item, index) in reportQuestionPerformance"
              :key="`question-${index}`"
              :name="String(index)"
            >
              <template #title>
                <div class="collapse-title">
                  <div class="collapse-title-left">
                    <span
                      class="collapse-question"
                      :title="`Q${index + 1} · ${
                        item.question || '未记录问题'
                      }`"
                      >Q{{ index + 1 }} ·
                      {{ item.question || "未记录问题" }}</span
                    >
                  </div>
                  <div v-if="item.score != null" class="collapse-title-right">
                    <el-tag
                      size="small"
                      type="warning"
                      effect="plain"
                      class="collapse-score"
                      >{{ item.score }}分</el-tag
                    >
                  </div>
                </div>
              </template>
              <div class="question-answer">
                {{ item.answer || "未记录回答" }}
              </div>
              <div class="question-footer">
                <span class="question-score"
                  >得分：{{ item.score ?? "--" }}</span
                >
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
              <div v-if="item.comment" class="question-comment">
                {{ item.comment }}
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
      </div>

      <div class="action-section">
        <div class="action-group">
          <el-button @click="goBack" class="action-btn secondary">返回历史</el-button>
          <el-button @click="goToSession" class="action-btn secondary">查看会话</el-button>
          <el-button class="action-btn share-btn" @click="showShareDialog = true">
            <FeatureIcon name="share" size="xs" class="share-icon" />
            分享到社区
          </el-button>
          <el-button type="primary" class="action-btn primary" @click="goToEntry">再来一次</el-button>
        </div>
      </div>

      <ShareReportDialog
        v-model:visible="showShareDialog"
        :session-data="sessionData"
      />
    </div>

    <div v-else class="empty-section">
      <div class="empty-card">
        <div class="empty-title">暂无评估报告</div>
        <div class="empty-desc">系统仍在生成评估结果，请稍后刷新查看。</div>
        <el-button
          type="primary"
          :loading="refreshingReport"
          @click="refreshReportNow"
          >立即刷新</el-button
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  getDifficultyLabel,
  getInterviewModeLabel,
  DIFFICULTY_KEY_MAP,
} from "@/constants/interview";
import { ElMessage } from "element-plus";
import { getInterviewSession, getInterviewSessionStatus } from "@/api/interview";
import RadarChart from "@/components/resume/RadarChart.vue";
import RadarScorePanel from "@/components/resume/RadarScorePanel.vue";
import AiLoadingState from "@/components/common/AiLoadingState.vue";
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import { completeOnboardingTask } from "@/api/onboarding";
import ShareReportDialog from "@/components/community/ShareReportDialog.vue";

const route = useRoute();
const router = useRouter();
const sessionId = computed(() => route.params.sessionId);

const loading = ref(true);
const error = ref("");
const sessionData = ref(null);
const reportPolling = ref(false);
const refreshingReport = ref(false);
const reportPollRounds = ref(0);
const replayExpanded = ref(false);
const roundReviewsExpanded = ref(false);
const collapsedReplayRounds = ref([]);
const REPORT_POLL_FAST_INTERVAL_MS = 3000;
const REPORT_POLL_NORMAL_INTERVAL_MS = 6000;
const REPORT_POLL_FAST_ROUNDS = 6;
const REPORT_POLL_MAX_ROUNDS = 120;
let reportPollingTimer = null;

const showShareDialog = ref(false);

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
const isReportGenerating = computed(
  () => isEnded.value && !hasReport.value && reportPolling.value
);

// ---- AiLoadingState 相关 ----
/** 面试报告阶段定义 */
const interviewStages = [
  { key: "organizing", label: "整理面试记录" },
  { key: "evaluating", label: "AI 评估中" },
  { key: "generating", label: "生成评估报告" },
];

/** 当前阶段索引：基于轮询轮次 */
const reportStageIndex = computed(() => {
  if (reportPollRounds.value <= 3) return 0;
  if (reportPollRounds.value <= 10) return 1;
  return 2;
});

/** 轮播鼓励文案 */
const interviewLoadingMessages = [
  "正在整理你的面试问答记录...",
  "AI 正在评估你的回答质量...",
  "正在生成各维度评分...",
  "正在撰写个性化反馈建议...",
  "报告马上就绪...",
];

const displayScoreValue = computed(() => {
  const score = sessionData.value?.comprehensiveScore;
  return score === null || score === undefined ? null : Number(score);
});

const difficultyDesc = computed(() => sessionData.value?.difficultyDesc || "");
const interviewModeDesc = computed(
  () => sessionData.value?.interviewModeDesc || ""
);

const difficultyFallback = computed(() => {
  return getDifficultyLabel(sessionData.value?.difficulty, "--");
});

const modeFallback = computed(() => {
  if (
    sessionData.value?.jobTargeted ||
    sessionData.value?.interviewMode === "job_targeted"
  ) {
    return "岗位定向模拟";
  }
  return getInterviewModeLabel(sessionData.value?.interviewMode);
});

const reportLevel = computed(() => parsedReport.value?.level || "");

const LEVEL_MAP = {
  S: { label: "远超预期", tagType: "success", hire: "强烈推荐" },
  A: { label: "优秀", tagType: "primary", hire: "强烈推荐" },
  B: { label: "达标", tagType: "warning", hire: "推荐" },
  C: { label: "勉强", tagType: "info", hire: "待定" },
  D: { label: "淘汰", tagType: "danger", hire: "不推荐" },
};

const levelLabel = computed(() => LEVEL_MAP[reportLevel.value]?.label || "");
const levelTagType = computed(() => LEVEL_MAP[reportLevel.value]?.tagType || "");

const gradeScale = [
  { level: "S", range: "90-100", label: "远超预期", hire: "强烈推荐" },
  { level: "A", range: "80-89", label: "优秀", hire: "强烈推荐" },
  { level: "B", range: "70-79", label: "达标", hire: "推荐" },
  { level: "C", range: "60-69", label: "勉强", hire: "待定" },
  { level: "D", range: "<60", label: "淘汰", hire: "不推荐" },
];

const uniqueTextList = (items = []) => {
  const seen = new Set();
  return items
    .map((item) => String(item || "").trim())
    .filter((item) => {
      if (!item || seen.has(item)) {
        return false;
      }
      seen.add(item);
      return true;
    });
};

const uniqueReportItems = (items = [], getKey) => {
  const seen = new Set();
  return items.filter((item) => {
    const key = getKey(item);
    if (!key || seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

const reportStrengths = computed(() =>
  uniqueTextList(parsedReport.value?.strengths || [])
);
const reportWeaknesses = computed(() =>
  uniqueTextList([
    ...(parsedReport.value?.weaknesses || []),
    ...(parsedReport.value?.missingCompetencies || []),
  ])
);
const reportSuggestions = computed(() =>
  uniqueTextList([
    ...(parsedReport.value?.improvementSuggestions || []),
    ...(parsedReport.value?.suggestions || []),
  ])
);
const reportQuestionPerformance = computed(
  () => parsedReport.value?.questionPerformance || []
);
const reportRoundReviews = computed(() =>
  uniqueReportItems(parsedReport.value?.roundReviews || [], (item) =>
    [
      item?.roundNo,
      item?.question,
      item?.answer,
      item?.replayAnalysis,
      item?.missedFollowUp,
      item?.nextPractice,
    ]
      .map((part) => String(part || "").trim())
      .join("|")
  )
);
const reportFollowUpLossPoints = computed(() =>
  uniqueTextList(parsedReport.value?.followUpLossPoints || [])
);
const reportCommonLossPatterns = computed(() =>
  uniqueTextList(parsedReport.value?.commonLossPatterns || [])
);
const reportImmediateActions = computed(() =>
  uniqueTextList(parsedReport.value?.immediateActions || []).slice(0, 3)
);
const replayRounds = computed(() => sessionData.value?.replayRounds || []);

const stripFollowUpPrefix = (content = "") => {
  return String(content || "")
    .trim()
    .replace(/^(追问|问题)[:：]\s*/u, "");
};

const parseReplayFeedback = (content = "") => {
  const text = String(content || "");
  const match = text.match(/<FEEDBACK>\s*([\s\S]*?)\s*<\/FEEDBACK>/i);
  if (!match) {
    return {
      mainContent: stripFollowUpPrefix(text),
      feedbackContent: "",
    };
  }

  return {
    mainContent: stripFollowUpPrefix(text.replace(match[0], "")),
    feedbackContent: match[1].replace(/^本题反馈[:：]\s*/u, "").trim(),
  };
};

const getReplayQuestionDisplay = (item) => {
  return parseReplayFeedback(item?.questionContent || "");
};

const getReplayFeedbackDisplay = (item) => {
  return parseReplayFeedback(item?.feedbackContent || "");
};

const replayRoundKey = (item) => {
  return `${item?.roundNo || "unknown"}-${
    item?.answerMessageId || item?.answerTime || "round"
  }`;
};

const isReplayRoundExpanded = (key) =>
  !collapsedReplayRounds.value.includes(key);

const toggleReplayRound = (key) => {
  // 每个序号代表一轮回放消息：问题、回答、追问作为整体折叠，状态只保存在当前页面。
  collapsedReplayRounds.value = collapsedReplayRounds.value.includes(key)
    ? collapsedReplayRounds.value.filter((item) => item !== key)
    : [...collapsedReplayRounds.value, key];
};

// 逐题表现折叠面板：题目数 <= 3 时全部展开，> 3 时只展开最后 3 题
const activeQuestions = ref([]);
watch(
  reportQuestionPerformance,
  (list) => {
    const count = list.length;
    if (count <= 3) {
      activeQuestions.value = list.map((_, i) => String(i));
    } else {
      activeQuestions.value = Array.from({ length: 3 }, (_, i) =>
        String(count - 3 + i)
      );
    }
  },
  { immediate: true }
);

const jobTargetFeedback = computed(
  () => sessionData.value?.jobTargetContext?.jobTargetedFeedback || null
);
const jobTargetStrengths = computed(() =>
  uniqueTextList(jobTargetFeedback.value?.strengths || [])
);
const jobTargetWeaknesses = computed(() =>
  uniqueTextList(jobTargetFeedback.value?.weaknesses || [])
);
const jobTargetSuggestions = computed(() =>
  uniqueTextList(jobTargetFeedback.value?.improvementSuggestions || [])
);

const dimensionCards = computed(() => {
  const report = parsedReport.value;
  if (!report) return [];
  const source = [
    { key: "jobMatch", label: "岗位匹配", value: report.jobMatch },
    { key: "technicalDepth", label: "技术深度", value: report.technicalDepth },
    {
      key: "projectExpression",
      label: "项目表达",
      value: report.projectExpression,
    },
    { key: "communication", label: "沟通表达", value: report.communication },
    { key: "problemSolving", label: "问题解决", value: report.problemSolving },
    {
      key: "pressureResistance",
      label: "抗压表现",
      value: report.pressureResistance,
    },
  ];
  return source
    .filter(
      (item) =>
        item.value &&
        item.value.score !== null &&
        item.value.score !== undefined
    )
    .map((item) => ({
      key: item.key,
      label: item.label,
      score: item.value.score,
      comment: item.value.comment,
    }));
});

// 面试五维配置
const interviewDimensionConfig = [
  { key: "technicalDepth", label: "技术深度" },
  { key: "projectExpression", label: "项目表达" },
  { key: "communication", label: "沟通表达" },
  { key: "problemSolving", label: "问题解决" },
  { key: "pressureResistance", label: "抗压表现" },
  { key: "jobMatch", label: "岗位匹配" },
];

const radarLabels = computed(() =>
  interviewDimensionConfig.map((d) => d.label)
);
const radarKeys = computed(() => interviewDimensionConfig.map((d) => d.key));
const radarScores = computed(() => {
  const report = parsedReport.value;
  if (!report) return {};
  const result = {};
  for (const dim of interviewDimensionConfig) {
    const val = report[dim.key];
    if (val && val.score != null) {
      result[dim.key] = {
        score: val.score,
        comment: val.comment || "",
        plus: val.strengths || [],
        minus: val.weaknesses || [],
      };
    }
  }
  return result;
});
const radarDimensionConfig = computed(() =>
  interviewDimensionConfig.map((d) => ({ key: d.key, label: d.label }))
);

const fetchSessionDetail = async (options = {}) => {
  const { showLoading = true, silentError = false } = options;
  if (!sessionId.value) {
    if (!silentError) {
      error.value = "会话 ID 不存在";
    }
    loading.value = false;
    return false;
  }

  if (showLoading) loading.value = true;
  if (!silentError) error.value = "";
  try {
    const res = await getInterviewSession(sessionId.value);
    sessionData.value = res.data;
    return true;
  } catch (err) {
    if (!silentError) {
      error.value = err.message || "获取评估报告失败，请稍后重试";
    }
    return false;
  } finally {
    if (showLoading) loading.value = false;
  }
};

const startReportPolling = () => {
  if (reportPolling.value || reportPollingTimer || !sessionId.value) return;
  reportPolling.value = true;
  reportPollRounds.value = 0;
  scheduleNextReportPoll();
};

const getNextReportPollDelay = () =>
  reportPollRounds.value < REPORT_POLL_FAST_ROUNDS
    ? REPORT_POLL_FAST_INTERVAL_MS
    : REPORT_POLL_NORMAL_INTERVAL_MS;

const scheduleNextReportPoll = () => {
  if (!reportPolling.value || reportPollingTimer || !sessionId.value) return;
  reportPollingTimer = setTimeout(runReportPoll, getNextReportPollDelay());
};

const runReportPoll = async () => {
  reportPollingTimer = null;
  if (!reportPolling.value || !sessionId.value || hasReport.value) {
    stopReportPolling();
    return;
  }
  if (refreshingReport.value) {
    scheduleNextReportPoll();
    return;
  }

  refreshingReport.value = true;
  try {
    const res = await getInterviewSessionStatus(sessionId.value);
    const statusData = res.data || {};
    sessionData.value = {
      ...(sessionData.value || {}),
      status: statusData.status ?? sessionData.value?.status,
      statusDesc: statusData.statusDesc || sessionData.value?.statusDesc,
      comprehensiveScore: statusData.comprehensiveScore ?? sessionData.value?.comprehensiveScore,
      openingPending: Boolean(statusData.openingPending),
      updateTime: statusData.updateTime || sessionData.value?.updateTime,
    };
    reportPollRounds.value += 1;
    if (statusData.reportReady) {
      const detailLoaded = await fetchSessionDetail({ showLoading: false, silentError: true });
      // 只有完整报告详情成功回填后才停止轮询；轻量状态先到但详情请求短暂失败时继续等待下一轮。
      if (detailLoaded && hasReport.value) {
        stopReportPolling();
        ElMessage.success("评估报告已生成");
      }
      return;
    }
    if (reportPollRounds.value >= REPORT_POLL_MAX_ROUNDS) {
      stopReportPolling();
    }
  } finally {
    refreshingReport.value = false;
    scheduleNextReportPoll();
  }
};
const stopReportPolling = () => {
  if (reportPollingTimer) {
    clearTimeout(reportPollingTimer);
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

const formatReplayTime = (timeStr) => {
  // 回放时间只用于辅助定位，不参与业务判断，解析失败时保持空展示。
  if (!timeStr) return "";
  const date = new Date(timeStr);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

const goBack = () => router.push("/interview/history");
const goToSession = () =>
  sessionId.value && router.push(`/interview/session/${sessionId.value}`);
const goToEntry = () => {
  // 保留上次面试配置，方便用户快速再来一次
  router.push({
    path: "/interview/entry",
    query: {
      jobRole: sessionData.value?.jobRole || undefined,
      difficulty:
        DIFFICULTY_KEY_MAP[sessionData.value?.difficulty] || undefined,
      mode: sessionData.value?.interviewMode || undefined,
      jobTargeted: sessionData.value?.jobTargeted ? "1" : undefined,
    },
  });
};

onMounted(() => {
  fetchSessionDetail();
});

// 报告首次加载成功时，静默上报新手任务完成。
const hasReportedOnboarding = ref(false);
watch(hasReport, (val) => {
  if (val && !hasReportedOnboarding.value) {
    hasReportedOnboarding.value = true;
    completeOnboardingTask("interview_completed").catch(() => {});
  }
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
  --report-bg: #fff7ef;
  --report-bg-deep: #f7efe6;
  --report-surface: rgba(255, 255, 255, 0.88);
  --report-surface-solid: #ffffff;
  --report-surface-soft: #fff6ed;
  --report-border: rgba(214, 142, 88, 0.2);
  --report-border-strong: rgba(255, 140, 66, 0.38);
  --report-text: #2f261f;
  --report-text-soft: #755b47;
  --report-text-muted: #987966;
  --report-accent: var(--orange-main, #ff8c42);
  --report-accent-deep: #d9661e;
  --report-green: #2f8a62;
  --report-shadow: 0 22px 60px rgba(132, 73, 30, 0.12);
  --report-shadow-soft: 0 12px 34px rgba(132, 73, 30, 0.08);
  --report-ease: cubic-bezier(0.22, 1, 0.36, 1);
  min-height: 100%;
  background:
    radial-gradient(circle at 12% 0%, rgba(255, 183, 115, 0.28), transparent 32%),
    linear-gradient(135deg, var(--report-bg) 0%, var(--report-bg-deep) 48%, #fffaf5 100%);
  color: var(--report-text);
  padding: clamp(16px, 3vw, 32px);
}

.page-back {
  margin-bottom: 16px;
}

.back-btn {
  color: var(--text-muted, #909399);
}

.loading-section,
.error-section,
.empty-section,
.generating-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
}

.error-card,
.empty-card {
  background: var(--bg-card, #ffffff);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  max-width: 480px;
  border: 1px solid var(--border-card, rgba(243, 216, 199, 0.5));
}

.error-desc,
.empty-desc {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-body, #666666);
}

.error-title,
.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
  margin-bottom: 10px;
}

.error-actions,
.empty-actions,
.action-group {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: clamp(16px, 2vw, 24px);
  max-width: 1180px;
  margin: 0 auto;
  animation: reportSurfaceIn 520ms var(--report-ease) both;
}

.hero-section,
.section-card {
  background: var(--report-surface);
  border-radius: 22px;
  border: 1px solid var(--report-border);
  box-shadow: var(--report-shadow-soft);
  backdrop-filter: blur(18px);
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.35fr);
  gap: clamp(18px, 3vw, 32px);
  padding: clamp(22px, 4vw, 40px);
  overflow: hidden;
  position: relative;
  box-shadow: var(--report-shadow);
}

.hero-section::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 16% 12%, rgba(255, 140, 66, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.72), transparent 56%);
}

.report-score-panel,
.report-summary-panel {
  position: relative;
  min-width: 0;
  animation: reportSectionIn 620ms var(--report-ease) both;
}

.report-summary-panel {
  animation-delay: 110ms;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(18px, 3vw, 28px);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.76), rgba(255, 247, 239, 0.72));
  border: 1px solid rgba(255, 140, 66, 0.18);
}

.job-name {
  font-size: clamp(18px, 2.2vw, 24px);
  font-weight: 600;
  color: var(--report-text);
  margin-bottom: 16px;
  line-height: 1.35;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score-number {
  font-size: clamp(56px, 8vw, 92px);
  font-weight: 800;
  color: var(--report-accent);
  line-height: 1;
  letter-spacing: 0;
}

.score-unit {
  font-size: 18px;
  color: var(--report-accent-deep);
}

.hero-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.summary-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--report-accent-deep);
  margin-bottom: 12px;
}

.summary-text {
  margin: 0;
  font-size: clamp(14px, 1.5vw, 16px);
  line-height: 1.8;
  color: var(--report-text);
}

.level-badge {
  margin-left: 10px;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 1px;
}

.grade-ref-card {
  overflow: visible;
}

.grade-table {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.grade-row {
  display: grid;
  grid-template-columns: 40px 72px 1fr 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--report-surface-soft);
  border: 1px solid transparent;
  transition:
    transform 180ms var(--report-ease),
    border-color 180ms var(--report-ease),
    background-color 180ms var(--report-ease),
    box-shadow 180ms var(--report-ease);
}

.grade-row.active {
  border-color: var(--report-accent);
  background: #fff1e4;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.12);
}

.grade-level {
  font-size: 18px;
  font-weight: 800;
  text-align: center;
}

.grade-s { color: #67c23a; }
.grade-a { color: #409eff; }
.grade-b { color: #e6a23c; }
.grade-c { color: #909399; }
.grade-d { color: #f56c6c; }

.grade-range {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
}

.grade-label {
  font-size: 13px;
  color: var(--text-body, #666);
}

.grade-hire {
  font-size: 12px;
  color: var(--text-muted, #909399);
}

.grade-marker {
  font-size: 12px;
  font-weight: 700;
  color: var(--orange-main, #ff8c42);
  background: rgba(255, 140, 66, 0.12);
  padding: 2px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

.section-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--report-border);
  background: linear-gradient(
    135deg,
    rgba(255, 246, 237, 0.82) 0%,
    rgba(255, 255, 255, 0.7) 100%
  );
}

.section-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 0;
  cursor: pointer;
  text-align: left;
  transition:
    transform 180ms var(--report-ease),
    border-color 180ms var(--report-ease),
    background 180ms var(--report-ease);
}

.section-toggle:hover {
  background: linear-gradient(
    135deg,
    rgba(255, 140, 66, 0.1) 0%,
    var(--bg-card, #fff) 100%
  );
  transform: translateY(-1px);
}

.section-title-wrap {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
}

.section-count {
  flex: none;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 140, 66, 0.12);
  color: var(--orange-main, #ff8c42);
  font-size: 12px;
  font-weight: 600;
}

.section-toggle-text {
  flex: none;
  color: var(--orange-main, #ff8c42);
  font-size: 13px;
  font-weight: 600;
}

.section-body {
  padding: 20px;
}

.priority-section {
  border-color: var(--report-border-strong);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 244, 232, 0.86)),
    var(--report-surface-solid);
}

.action-plan-list {
  display: grid;
  gap: 14px;
}

.report-priority-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.action-plan-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 140, 66, 0.16);
  animation: reportSectionIn 520ms var(--report-ease) both;
  transition:
    transform 180ms var(--report-ease),
    border-color 180ms var(--report-ease),
    box-shadow 180ms var(--report-ease);
}

.action-plan-item:nth-child(2) {
  animation-delay: 80ms;
}

.action-plan-item:nth-child(3) {
  animation-delay: 160ms;
}

.action-plan-item:hover {
  transform: translateY(-3px);
  border-color: var(--report-border-strong);
  box-shadow: 0 14px 34px rgba(132, 73, 30, 0.12);
}

.action-plan-index {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--report-accent), #ffb067);
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
}

.action-plan-text {
  min-width: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--report-text);
  overflow-wrap: anywhere;
}

.report-section-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: clamp(14px, 2vw, 20px);
  align-items: stretch;
}

.report-diagnosis-stack {
  align-items: start;
}

.report-section-grid > .section-card {
  animation: reportSectionIn 520ms var(--report-ease) both;
}

.report-section-grid > .section-card:nth-child(3n + 2) {
  animation-delay: 70ms;
}

.report-section-grid > .section-card:nth-child(3n) {
  animation-delay: 140ms;
}

.job-feedback-body {
  display: grid;
  gap: 16px;
}

.radar-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

.radar-chart-area {
  display: flex;
  justify-content: center;
  align-items: center;
}

.radar-panel-area {
  display: flex;
  flex-direction: column;
}

.job-feedback-item,
.question-card {
  background: var(--report-surface-soft);
  border: 1px solid var(--report-border);
  border-radius: 12px;
  padding: 14px 16px;
}

.dimension-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr);
}

.dimension-card {
  display: grid;
  grid-template-columns: minmax(110px, 0.28fr) 72px minmax(0, 1fr);
  align-items: start;
  gap: 14px;
}

.round-review-list {
  display: grid;
  gap: 14px;
  animation: reportExpandIn 260ms var(--report-ease) both;
}

.replay-timeline {
  display: grid;
  gap: 16px;
  animation: reportExpandIn 260ms var(--report-ease) both;
}

.replay-round {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
}

.replay-round-marker {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff2e8;
  color: var(--orange-main, #ff8c42);
  font-size: 13px;
  font-weight: 700;
  border: 1px solid rgba(255, 140, 66, 0.24);
}

.replay-round-content {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.replay-round-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 140, 66, 0.22);
  border-radius: 10px;
  background: #fffaf4;
  color: #8a5b39;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
  transition:
    transform 180ms var(--report-ease),
    border-color 180ms var(--report-ease),
    box-shadow 180ms var(--report-ease);
}

.replay-round-toggle:hover {
  transform: translateY(-1px);
  border-color: var(--report-border-strong);
  box-shadow: 0 10px 24px rgba(132, 73, 30, 0.08);
}

.replay-round-toggle-text {
  flex: none;
  color: var(--orange-main, #ff8c42);
  font-size: 12px;
  font-weight: 700;
}

.replay-round-body {
  display: grid;
  gap: 10px;
  min-width: 0;
  animation: reportExpandIn 220ms var(--report-ease) both;
}

.replay-block {
  border: 1px solid var(--border-card, rgba(243, 216, 199, 0.35));
  border-radius: 10px;
  padding: 12px 14px;
  background: var(--bg-card, #ffffff);
}

.replay-block.answer {
  border-color: rgba(46, 125, 90, 0.18);
  background: #f7fbf8;
}

.replay-block.feedback {
  border-color: rgba(255, 140, 66, 0.28);
  background: #fffaf4;
}

.replay-label {
  font-size: 12px;
  font-weight: 700;
  color: #8a5b39;
}

.replay-block.answer .replay-label {
  color: #2e7d5a;
}

.replay-block.feedback .replay-label {
  color: var(--orange-main, #ff8c42);
}

.replay-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-title, #2f2f2f);
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.replay-feedback-card {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 140, 66, 0.22);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 249, 243, 0.95), rgba(255, 255, 255, 0.86));
}

.replay-feedback-title {
  margin-bottom: 6px;
  color: var(--orange-main, #ff8c42);
  font-size: 12px;
  font-weight: 700;
}

.replay-feedback-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-body, #666666);
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.replay-time {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted, #909399);
}

.round-review-item {
  padding: 14px 16px;
  border: 1px solid var(--report-border);
  border-radius: 14px;
  background: var(--report-surface-soft);
  transition:
    transform 180ms var(--report-ease),
    border-color 180ms var(--report-ease),
    box-shadow 180ms var(--report-ease);
}

.round-review-item:last-child {
  padding-bottom: 14px;
}

.round-review-item:hover {
  transform: translateY(-1px);
  border-color: var(--report-border-strong);
  box-shadow: 0 12px 28px rgba(132, 73, 30, 0.08);
}

.round-review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.round-review-title,
.loss-pattern-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--orange-main, #ff8c42);
}

.round-review-question,
.round-review-answer {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-title, #2f2f2f);
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.round-review-answer {
  margin-top: 6px;
  color: var(--text-body, #666666);
}

.round-review-speaker {
  display: inline-block;
  min-width: 56px;
  color: #8a5b39;
  font-weight: 700;
}

.round-review-speaker.candidate {
  color: #2e7d5a;
}

.round-review-block {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
  margin-top: 10px;
  padding: 12px 14px;
  border: 1px solid var(--report-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.48);
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-title, #2f2f2f);
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.round-review-block.warning {
  color: var(--report-text);
  background: rgba(255, 140, 66, 0.1);
}

.round-review-label {
  font-weight: 600;
  color: #8a5b39;
}

.loss-pattern-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.loss-pattern-column {
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--report-border);
  border-radius: 14px;
  background: var(--report-surface-soft);
}

.loss-pattern-title {
  margin-bottom: 10px;
}

.dimension-card {
  background: var(--report-surface-soft);
  border: 1px solid var(--report-border);
  border-radius: 14px;
  padding: 14px 16px;
}

.dimension-label {
  font-size: 13px;
  font-weight: 600;
  color: #8a5b39;
  margin-bottom: 8px;
}

.dimension-score {
  font-size: 24px;
  font-weight: 700;
  color: var(--orange-main, #ff8c42);
  margin-bottom: 0;
}

.dimension-comment {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-title, #2f2f2f);
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.job-feedback-label {
  font-size: 13px;
  font-weight: 600;
  color: #8a5b39;
  margin-bottom: 8px;
}

.job-feedback-value,
.question-answer,
.question-comment {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-title, #2f2f2f);
}

.simple-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-title, #2f2f2f);
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

/* ---- 折叠面板标题：Grid 双列，左侧文本省略，右侧得分固定 ---- */
:deep(.el-collapse-item__header) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 8px;
  min-width: 0;
  overflow: hidden;
  background: transparent;
  transition:
    color 180ms var(--report-ease),
    background-color 180ms var(--report-ease);
}

:deep(.el-collapse-item__arrow) {
  grid-column: 2;
  flex: none;
  margin-left: 0;
}

.collapse-title {
  grid-column: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 12px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.collapse-title-left {
  min-width: 0;
  overflow: hidden;
}

.collapse-question {
  display: block;
  min-width: 0;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
}

.collapse-title-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: max-content;
}

.collapse-score {
  flex: none;
  white-space: nowrap;
}

/* ---- 防溢出：外层容器约束 ---- */
.report-content,
.section-card,
.section-body,
:deep(.el-collapse),
:deep(.el-collapse-item),
:deep(.el-collapse-item__wrap),
:deep(.el-collapse-item__content) {
  min-width: 0;
  max-width: 100%;
  animation: reportExpandIn 240ms var(--report-ease) both;
}

.section-card {
  overflow: hidden;
}

/* 展开内容长文本自然换行 */
.question-answer,
.question-comment {
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: pre-line;
}

.question-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
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
  color: var(--orange-main, #ff8c42);
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
  transition:
    transform 180ms var(--report-ease),
    box-shadow 180ms var(--report-ease),
    border-color 180ms var(--report-ease),
    background-color 180ms var(--report-ease);
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(132, 73, 30, 0.1);
}

.action-btn:active,
.section-toggle:active,
.replay-round-toggle:active {
  transform: scale(0.99);
}

.action-btn.primary {
  background: linear-gradient(
    135deg,
    var(--orange-main, #ff8c42) 0%,
    #ff7a30 100%
  );
  border: none;
  color: var(--bg-card, #ffffff);
}

.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-color: var(--orange-main, #ff8c42);
  color: var(--orange-main, #ff8c42);
}

.share-btn:hover {
  background: var(--orange-light-bg, #fff8f3);
  border-color: var(--orange-main, #ff8c42);
  color: var(--orange-main, #ff8c42);
}

.share-icon {
  width: 16px;
  height: 16px;
}

@keyframes reportSurfaceIn {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes reportSectionIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes reportExpandIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .hero-section {
    grid-template-columns: 1fr;
  }

  .report-section-grid,
  .radar-layout {
    grid-template-columns: 1fr;
  }

  .report-priority-grid {
    grid-template-columns: 1fr;
  }

  .loss-pattern-grid {
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
    flex-wrap: wrap;
    align-items: stretch;
  }

  .action-btn {
    min-width: min(100%, 180px);
  }

  .grade-row {
    grid-template-columns: 40px 72px minmax(0, 1fr);
  }

  .grade-hire,
  .grade-marker {
    grid-column: 2 / -1;
  }

  .action-plan-item {
    grid-template-columns: 36px minmax(0, 1fr);
    padding: 14px;
  }

  .action-plan-index {
    width: 36px;
    height: 36px;
  }

  .round-review-block {
    grid-template-columns: 1fr;
    gap: 2px;
  }

  .dimension-card {
    grid-template-columns: minmax(0, 1fr);
    gap: 6px;
  }

  .replay-round {
    grid-template-columns: 1fr;
  }

  .replay-round-marker {
    width: 28px;
    height: 28px;
  }
}

@media (max-width: 480px) {
  .interview-report-view {
    padding: 12px;
  }

  .hero-section,
  .section-body {
    padding: 16px;
  }

  .section-header {
    padding: 14px 16px;
  }

  .score-number {
    font-size: 48px;
  }

  .collapse-title {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }

  .collapse-title-right {
    justify-content: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .report-content,
  .report-score-panel,
  .report-summary-panel,
  .report-section-grid > .section-card,
  .replay-timeline,
  .replay-round-body,
  .round-review-list,
  :deep(.el-collapse-item__content),
  .action-plan-item {
    animation-duration: 1ms;
    animation-delay: 0ms;
  }

  .section-toggle,
  .replay-round-toggle,
  .action-plan-item,
  .action-btn,
  .round-review-item,
  .grade-row {
    transition-duration: 1ms;
  }

  .section-toggle:hover,
  .replay-round-toggle:hover,
  .action-plan-item:hover,
  .action-btn:hover,
  .round-review-item:hover {
    transform: none;
  }
}

:global(html[data-theme="dark"] .interview-report-view) {
  --report-bg: #17110d;
  --report-bg-deep: #24170f;
  --report-surface: rgba(37, 28, 22, 0.9);
  --report-surface-solid: #251c16;
  --report-surface-soft: rgba(255, 140, 66, 0.08);
  --report-border: rgba(255, 178, 115, 0.18);
  --report-border-strong: rgba(255, 178, 115, 0.36);
  --report-text: #fff3e9;
  --report-text-soft: #e0c1aa;
  --report-text-muted: #b8957c;
  --report-accent: #ffad6b;
  --report-accent-deep: #ffc18d;
  --report-green: #74d6aa;
  --report-shadow: 0 22px 60px rgba(0, 0, 0, 0.32);
  --report-shadow-soft: 0 12px 34px rgba(0, 0, 0, 0.24);
}

:global(html[data-theme="dark"] .report-summary-panel),
:global(html[data-theme="dark"] .priority-section),
:global(html[data-theme="dark"] .action-plan-item) {
  background: rgba(37, 28, 22, 0.78);
}

:global(html[data-theme="dark"] .section-header) {
  background: linear-gradient(135deg, rgba(255, 140, 66, 0.12), rgba(37, 28, 22, 0.84));
}

:global(html[data-theme="dark"] .job-name),
:global(html[data-theme="dark"] .summary-text),
:global(html[data-theme="dark"] .grade-range),
:global(html[data-theme="dark"] .action-plan-text),
:global(html[data-theme="dark"] .section-title),
:global(html[data-theme="dark"] .replay-text),
:global(html[data-theme="dark"] .round-review-question),
:global(html[data-theme="dark"] .round-review-block),
:global(html[data-theme="dark"] .dimension-comment),
:global(html[data-theme="dark"] .job-feedback-value),
:global(html[data-theme="dark"] .question-answer),
:global(html[data-theme="dark"] .question-comment),
:global(html[data-theme="dark"] .simple-list),
:global(html[data-theme="dark"] .collapse-question) {
  color: var(--report-text);
}

:global(html[data-theme="dark"] .grade-label),
:global(html[data-theme="dark"] .grade-hire),
:global(html[data-theme="dark"] .replay-feedback-text),
:global(html[data-theme="dark"] .round-review-answer),
:global(html[data-theme="dark"] .replay-time) {
  color: var(--report-text-soft);
}

:global(html[data-theme="dark"] .grade-row),
:global(html[data-theme="dark"] .dimension-card),
:global(html[data-theme="dark"] .loss-pattern-column),
:global(html[data-theme="dark"] .round-review-item),
:global(html[data-theme="dark"] .round-review-block),
:global(html[data-theme="dark"] .job-feedback-item),
:global(html[data-theme="dark"] .question-card),
:global(html[data-theme="dark"] .replay-block),
:global(html[data-theme="dark"] .replay-feedback-card) {
  background: var(--report-surface-soft);
  border-color: var(--report-border);
}

:global(html[data-theme="dark"]) :deep(.el-collapse),
:global(html[data-theme="dark"]) :deep(.el-collapse-item),
:global(html[data-theme="dark"]) :deep(.el-collapse-item__wrap),
:global(html[data-theme="dark"]) :deep(.el-collapse-item__header),
:global(html[data-theme="dark"]) :deep(.el-collapse-item__content) {
  background: transparent;
  border-color: var(--report-border);
  color: var(--report-text);
}

:global(html[data-theme="dark"] .action-btn.secondary),
:global(html[data-theme="dark"] .share-btn) {
  background: rgba(37, 28, 22, 0.72);
  color: var(--report-text);
  border-color: var(--report-border);
}

/* ===== 暗色模式适配 ===== */
:global(html[data-theme="dark"] .grade-row.active) {
  background: rgba(255, 140, 66, 0.1);
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

:global(html[data-theme="dark"] .grade-marker) {
  background: rgba(255, 140, 66, 0.22);
}

/* ===== 面试历史回放 暗色适配 ===== */
:global(html[data-theme="dark"] .replay-round-marker) {
  background: rgba(255, 140, 66, 0.12);
  border-color: rgba(255, 140, 66, 0.35);
}

:global(html[data-theme="dark"] .replay-round-toggle) {
  background: rgba(255, 140, 66, 0.06);
  color: #e0b090;
  border-color: rgba(255, 140, 66, 0.2);
}

:global(html[data-theme="dark"] .replay-block) {
  background: var(--bg-card);
}

:global(html[data-theme="dark"] .replay-block.answer) {
  background: rgba(46, 125, 90, 0.08);
  border-color: rgba(46, 125, 90, 0.2);
}

:global(html[data-theme="dark"] .replay-block.feedback) {
  background: rgba(255, 140, 66, 0.06);
  border-color: rgba(255, 140, 66, 0.2);
}

:global(html[data-theme="dark"] .replay-label) {
  color: #d0a07a;
}

:global(html[data-theme="dark"] .replay-block.answer .replay-label) {
  color: #5db892;
}

:global(html[data-theme="dark"] .replay-feedback-card) {
  background: var(--bg-card);
}

:global(html[data-theme="dark"] .round-review-speaker) {
  color: #d0a07a;
}

:global(html[data-theme="dark"] .round-review-speaker.candidate) {
  color: #5db892;
}

:global(html[data-theme="dark"] .round-review-label) {
  color: #d0a07a;
}

:global(html[data-theme="dark"] .round-review-block.warning) {
  color: #e8a040;
  background: rgba(255, 140, 66, 0.1);
}
</style>
