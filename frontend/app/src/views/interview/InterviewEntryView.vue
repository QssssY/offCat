<template>
  <div class="interview-entry-view">
    <div class="page-header">
      <h1 class="page-title">模拟面试</h1>
      <p class="page-desc">配置面试参数，开始一场更贴近真实岗位场景的模拟面试</p>
    </div>

    <div class="ready-bar">
      <div class="ready-icon">
        <FeatureIcon name="microphone-on" size="md" />
      </div>
      <span class="ready-text">普通模拟面试可直接开始，开启岗位定向后会额外结合 JD 要求提问</span>
    </div>

    <div class="config-section">
      <div class="config-card">
        <div class="config-item">
          <div class="config-label">面试岗位</div>
          <div class="config-control">
            <el-select
              v-model="selectedJob"
              placeholder="请选择面试岗位"
              size="large"
              class="job-select"
              :disabled="creating"
              popper-class="job-select-popper"
              @change="onJobChange"
            >
              <template #empty>
                <div class="empty-options">暂无岗位数据</div>
              </template>
              <el-option
                v-for="job in jobOptions"
                :key="job.value"
                :label="job.label"
                :value="job.value"
              >
                <div class="job-option-content">
                  <span class="job-name">{{ job.label }}</span>
                  <span class="job-tag" :class="job.tagClassName">{{ job.tag }}</span>
                </div>
              </el-option>
            </el-select>
          </div>
        </div>

        <div class="config-item">
          <div class="config-label">难度级别</div>
          <div class="config-control pill-control">
            <div
              v-for="level in difficultyOptions"
              :key="level.value"
              class="pill-button"
              :class="{ active: selectedDifficulty === level.value }"
              @click="selectedDifficulty = level.value"
            >
              <span class="pill-label">{{ level.label }}</span>
              <span class="pill-hint">{{ level.hint }}</span>
            </div>
          </div>
        </div>

        <div class="config-item">
          <div class="config-label">面试模式</div>
          <div class="config-control pill-control">
            <div
              v-for="mode in modeOptions"
              :key="mode.value"
              class="pill-button"
              :class="{ active: selectedMode === mode.value }"
              @click="selectedMode = mode.value"
            >
              <span class="pill-label">{{ mode.label }}</span>
              <span class="pill-hint">{{ mode.hint }}</span>
            </div>
          </div>
        </div>

        <div class="config-item">
          <div class="config-label">反馈模式</div>
          <div class="config-control pill-control">
            <div
              v-for="fm in FEEDBACK_MODE_OPTIONS"
              :key="fm.value"
              class="pill-button"
              :class="{ active: selectedFeedbackMode === fm.value, disabled: isFeedbackModeDisabled(fm.value) }"
              @click="selectFeedbackMode(fm.value)"
            >
              <span class="pill-label">{{ fm.label }}</span>
              <span class="pill-hint">{{ getFeedbackModeHint(fm) }}</span>
            </div>
          </div>
        </div>

        <div class="config-item">
          <div class="config-label">交互方式</div>
          <div class="config-control pill-control">
            <button
              v-for="mode in INTERACTION_MODE_OPTIONS"
              :key="mode.value"
              type="button"
              class="pill-button"
              :class="{ active: selectedInteractionType === mode.value, disabled: mode.value === INTERACTION_TYPE_VOICE && !speechApiSupported }"
              :disabled="mode.value === INTERACTION_TYPE_VOICE && !speechApiSupported"
              @click="selectInteractionType(mode.value)"
            >
              <span class="pill-label">{{ mode.label }}</span>
              <span class="pill-hint">
                {{ mode.value === INTERACTION_TYPE_VOICE && !speechApiSupported ? '当前浏览器暂不支持' : mode.hint }}
              </span>
            </button>
          </div>
        </div>

        <div class="config-item">
          <div class="config-label">岗位定向模拟</div>
          <div class="job-target-card">
            <div class="job-target-header">
              <div>
                <div class="job-target-title">按目标岗位生成更贴近 JD 的问题</div>
                <div class="job-target-desc">
                  开启后会优先结合简历内容、岗位 JD 和最近一次 JD 对比结果生成问题，并在反馈中增加岗位匹配建议。
                </div>
              </div>
              <el-switch
                v-model="jobTargeted"
                :disabled="creating"
                inline-prompt
                active-text="开启"
                inactive-text="关闭"
              />
            </div>

            <div v-if="jobTargeted" class="job-target-body">
              <div v-if="resumeTaskId" class="resume-link-bar">
                <span class="resume-link-label">当前已关联简历任务：</span>
                <span class="resume-link-value">#{{ resumeTaskId }}</span>
              </div>

              <div class="source-select-row">
                <div class="source-option" :class="{ active: useLatestJobMatch }">
                  <div class="source-option-head">
                    <span class="source-option-title">优先复用最近一次 JD 对比结果</span>
                    <el-switch
                      v-model="useLatestJobMatch"
                      :disabled="creating || !hasLatestJobMatch"
                    />
                  </div>
                  <div class="source-option-desc">
                    <template v-if="hasLatestJobMatch">
                      已检测到最近一次 JD 对比结果，可直接复用其 JD、匹配关键词和优化建议。
                    </template>
                    <template v-else>
                      当前未检测到可复用的 JD 对比结果，仍可手动粘贴岗位 JD。
                    </template>
                  </div>
                </div>
              </div>

              <div v-if="hasLatestJobMatch" class="job-match-summary">
                <div class="summary-title">最近一次 JD 对比结果</div>
                <div class="summary-score">
                  匹配度：{{ latestJobMatchAnalysis?.matchScore ?? "--" }} 分
                </div>
                <div v-if="matchedKeywords.length" class="summary-tags">
                  <span class="summary-label">已匹配：</span>
                  <el-tag
                    v-for="keyword in matchedKeywords"
                    :key="'matched-' + keyword"
                    size="small"
                    type="success"
                  >
                    {{ keyword }}
                  </el-tag>
                </div>
                <div v-if="missingKeywords.length" class="summary-tags">
                  <span class="summary-label">缺失项：</span>
                  <el-tag
                    v-for="keyword in missingKeywords"
                    :key="'missing-' + keyword"
                    size="small"
                    type="warning"
                  >
                    {{ keyword }}
                  </el-tag>
                </div>
              </div>

              <div class="config-sub-item">
                <div class="config-sub-label">岗位 JD 文本</div>
                <el-input
                  v-model="jdText"
                  type="textarea"
                  :rows="8"
                  resize="none"
                  maxlength="6000"
                  show-word-limit
                  :disabled="creating"
                  placeholder="可直接粘贴目标岗位 JD；若留空且存在最近一次 JD 对比结果，系统会自动复用最近一次 JD。"
                />
              </div>

              <div class="job-target-tip">
                普通模拟面试不会强制依赖 JD。若你关闭岗位定向，系统仍按原有通用模拟面试流程运行。
              </div>
            </div>
          </div>
        </div>

        <div class="start-section">
          <el-button
            type="primary"
            size="large"
            :disabled="!selectedJob || creating"
            :loading="creating"
            :class="{ 'btn-creating': creating }"
            @click="handleStart"
          >
            <span v-if="creating" class="btn-text">正在创建面试...</span>
            <span v-else>{{ startButtonText }}</span>
          </el-button>
        </div>
      </div>
    </div>

    <div class="info-section">
      <div class="info-card">
        <h3 class="info-title">使用说明</h3>
        <div class="info-list">
          <div class="info-item">
            <div class="info-number">1</div>
            <div class="info-text">选择面试岗位和难度级别，系统会按对应技术栈和业务场景生成问题</div>
          </div>
          <div class="info-item">
            <div class="info-number">2</div>
            <div class="info-text">支持 5 种面试人设：普通、压力、大厂 HR、技术 Leader、外企，不同人设提问风格与考察侧重点不同</div>
          </div>
          <div class="info-item">
            <div class="info-number">3</div>
            <div class="info-text">反馈模式可选「面完复盘」统一分析或「每题反馈」即时点评</div>
          </div>
          <div class="info-item">
            <div class="info-number">4</div>
            <div class="info-text">开启岗位定向模拟后，会结合简历、岗位 JD 和最近一次 JD 对比结果生成针对性问题与匹配建议</div>
          </div>
          <div class="info-item">
            <div class="info-number">5</div>
            <div class="info-text">支持语音输入回答（输入框右侧麦克风按钮），外企面试官模式仅识别英文，其他模式支持中英混杂</div>
          </div>
          <div class="info-item">
            <div class="info-number">6</div>
            <div class="info-text">面试结束后生成综合评价报告，包含多维度评分、优势短板和改进建议</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { createInterviewSession, getInterviewJobRoles } from "@/api/interview";
import { getResumeTask } from "@/api/resume";
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import { prefetchInterviewSessionRoute } from "@/router/routeLoaders";
import {
  FEEDBACK_MODE_OPTIONS,
  INTERACTION_MODE_OPTIONS,
  INTERACTION_TYPE_TEXT,
  INTERACTION_TYPE_VOICE,
  INTERVIEW_MODE_OPTIONS,
  STRING_TO_DIFFICULTY,
} from "@/constants/interview";
import { getSettingsPreferences } from "@/utils/settingsPreferences";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const jobOptions = ref([]);
const selectedJob = ref("");
const selectedRoleCode = ref("");
const selectedDifficulty = ref("primary");
const selectedMode = ref("normal");
const selectedFeedbackMode = ref("after_interview");
const selectedInteractionType = ref(INTERACTION_TYPE_TEXT);
const creating = ref(false);

// 岗位定向状态统一收敛在入口页，便于清楚区分普通模拟与岗位定向模拟。
const jobTargeted = ref(false);
const resumeTaskId = ref(route.query.resumeTaskId ? String(route.query.resumeTaskId) : "");
const jdText = ref("");
const useLatestJobMatch = ref(true);
const latestJobMatchAnalysis = ref(null);
const latestJobMatchRecordId = ref("");

const difficultyOptions = [
  { label: "初级", value: "primary", hint: "入门基础" },
  { label: "中级", value: "intermediate", hint: "项目实战" },
  { label: "高级", value: "advanced", hint: "深度能力" },
];

const modeOptions = INTERVIEW_MODE_OPTIONS;

const difficultyMap = STRING_TO_DIFFICULTY;
const speechApiSupported = typeof window !== 'undefined'
  && Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
  && 'speechSynthesis' in window;

const hasLatestJobMatch = computed(() => Boolean(latestJobMatchAnalysis.value));
const matchedKeywords = computed(() => latestJobMatchAnalysis.value?.matchedKeywords || []);
const missingKeywords = computed(() => latestJobMatchAnalysis.value?.missingKeywords || []);
const startButtonText = computed(() => {
  // 开始按钮按当前模式动态回显，避免用户选择人设后仍显示“普通模拟面试”。
  const modeLabel = modeOptions.find((item) => item.value === selectedMode.value)?.label || "模拟面试";
  return jobTargeted.value ? `开始岗位定向 · ${modeLabel}` : `开始${modeLabel}`;
});

// 标签样式模板映射（与管理端 AdminJobRoleView.vue 保持一致）
const tagStyleTemplateOptions = [
  { value: 'default', className: 'tag-style-default' },
  { value: 'orange-highlight', className: 'tag-style-orange' },
  { value: 'blue-info', className: 'tag-style-blue' },
  { value: 'green-success', className: 'tag-style-green' },
  { value: 'red-alert', className: 'tag-style-red' },
  { value: 'purple-feature', className: 'tag-style-purple' },
  { value: 'gray-muted', className: 'tag-style-gray' },
  { value: 'outline', className: 'tag-style-outline' },
  { value: 'pill', className: 'tag-style-pill' },
  { value: 'pink-rose', className: 'tag-style-pink' },
]

// 历史值兼容映射（与管理端保持一致）
const legacyTagStyleAliasMap = {
  hot: 'orange-highlight',
  common: 'default',
  info: 'blue-info',
  success: 'green-success',
  warning: 'orange-highlight',
  danger: 'red-alert',
}

// 解析标签样式类名：直接匹配 → 别名匹配 → 默认值
const resolveTagClassName = (tagType) => {
  const value = String(tagType || '').trim()
  const directHit = tagStyleTemplateOptions.find((item) => item.value === value)
  if (directHit) return directHit.className
  const alias = legacyTagStyleAliasMap[value]
  if (alias) {
    const aliasHit = tagStyleTemplateOptions.find((item) => item.value === alias)
    if (aliasHit) return aliasHit.className
  }
  return 'tag-style-default'
}

const fetchJobOptions = async () => {
  try {
    const res = await getInterviewJobRoles();
    const rawList = Array.isArray(res.data) ? res.data : [];
    jobOptions.value = rawList.map((item) => ({
      label: item.roleName,
      value: item.roleName,
      roleCode: item.roleCode,
      tag: item.interviewTag || "常规",
      tagType: item.tagType || "normal",
      tagClassName: resolveTagClassName(item.tagType),
    }));
  } catch {
    jobOptions.value = [];
    // 拦截器已弹出错误提示
  }
};

/**
 * 从简历任务详情中自动带出最近一次 JD 对比结果。
 * 这里只做最小增量复用，不新增额外历史管理页面。
 */
const fetchResumeTaskDetail = async () => {
  if (!resumeTaskId.value) {
    return;
  }

  try {
    const res = await getResumeTask(resumeTaskId.value);
    const taskData = res.data || {};
    latestJobMatchAnalysis.value = taskData.latestJobMatchAnalysis || null;
    latestJobMatchRecordId.value = taskData.latestJobMatchAnalysis?.analysisId
      ? String(taskData.latestJobMatchAnalysis.analysisId)
      : "";

    if (hasLatestJobMatch.value) {
      jobTargeted.value = true;
    }
  } catch (err) {
    ElMessage.warning(err.message || "获取简历任务详情失败，岗位定向将仅支持手动输入 JD");
  }
};

const onJobChange = (jobName) => {
  const job = jobOptions.value.find((item) => item.label === jobName);
  selectedRoleCode.value = job?.roleCode || "";
};

const isFeedbackModeDisabled = (feedbackMode) => {
  return selectedInteractionType.value === INTERACTION_TYPE_VOICE && feedbackMode === "immediate";
};

const getFeedbackModeHint = (feedbackModeOption) => {
  if (isFeedbackModeDisabled(feedbackModeOption.value)) {
    return "语音面试暂不支持即时反馈";
  }
  return feedbackModeOption.hint;
};

const selectFeedbackMode = (feedbackMode) => {
  // 语音面试会过滤结构化即时反馈用于播报，避免用户进入“每题反馈不可见”的组合。
  if (selectedInteractionType.value === INTERACTION_TYPE_VOICE && feedbackMode === "immediate") {
    ElMessage.warning("语音面试暂不支持每题反馈，请使用面完复盘");
    selectedFeedbackMode.value = "after_interview";
    return;
  }
  selectedFeedbackMode.value = feedbackMode;
};

const selectInteractionType = (interactionType) => {
  // 语音通话依赖浏览器同时支持语音识别和语音合成，不支持时保持文字面试。
  if (interactionType === INTERACTION_TYPE_VOICE && !speechApiSupported) {
    ElMessage.warning("当前浏览器不支持语音识别或语音播报，请使用文字面试");
    selectedInteractionType.value = INTERACTION_TYPE_TEXT;
    return;
  }
  selectedInteractionType.value = interactionType;
  if (interactionType === INTERACTION_TYPE_VOICE && selectedFeedbackMode.value === "immediate") {
    ElMessage.warning("语音面试暂不支持每题反馈，已切换为面完复盘");
    selectedFeedbackMode.value = "after_interview";
  }
};

const applyMatchedJob = (matched) => {
  selectedJob.value = matched?.label || "";
  selectedRoleCode.value = matched?.roleCode || "";
};

const findJobOption = (jobValue) => {
  if (!jobValue || jobOptions.value.length === 0) {
    return null;
  }
  return jobOptions.value.find(
    (opt) => opt.roleCode === jobValue || opt.label === jobValue
  ) || null;
};

const applyStoredInterviewPreferences = () => {
  const preferences = getSettingsPreferences();

  if (difficultyOptions.some((item) => item.value === preferences.defaultInterviewDifficulty)) {
    selectedDifficulty.value = preferences.defaultInterviewDifficulty;
  }
  if (modeOptions.some((item) => item.value === preferences.defaultInterviewMode)) {
    selectedMode.value = preferences.defaultInterviewMode;
  }
  if (FEEDBACK_MODE_OPTIONS.some((item) => item.value === preferences.defaultFeedbackMode)) {
    selectedFeedbackMode.value = preferences.defaultFeedbackMode;
  }
  if (preferences.defaultInterviewInteractionType === INTERACTION_TYPE_VOICE && speechApiSupported) {
    selectedInteractionType.value = INTERACTION_TYPE_VOICE;
    if (selectedFeedbackMode.value === "immediate") {
      selectedFeedbackMode.value = "after_interview";
    }
  } else {
    selectedInteractionType.value = INTERACTION_TYPE_TEXT;
  }

  // 默认岗位必须仍存在于当前启用岗位列表，避免本机旧缓存把入口页带到不可用配置。
  const matchedJob = findJobOption(preferences.defaultInterviewJobRoleCode)
    || findJobOption(preferences.defaultInterviewJobRole);
  if (matchedJob) {
    applyMatchedJob(matchedJob);
  }
};

const applyRouteQueryPreferences = () => {
  const q = route.query;
  if (q.difficulty && ["primary", "intermediate", "advanced"].includes(q.difficulty)) {
    selectedDifficulty.value = q.difficulty;
  }
  if (q.mode && modeOptions.some((item) => item.value === q.mode)) {
    selectedMode.value = q.mode;
  }
  if (q.feedbackMode && FEEDBACK_MODE_OPTIONS.some((item) => item.value === q.feedbackMode)) {
    selectedFeedbackMode.value = q.feedbackMode;
  }
  if (q.jobTargeted === "1") {
    jobTargeted.value = true;
  }
  if (q.jobRole) {
    // 路由参数来自“再来一次”等显式动作，优先级高于本机默认岗位；匹配失败时不回落默认岗位。
    applyMatchedJob(null);
    applyMatchedJob(findJobOption(q.jobRole));
  }
};

const buildCreatePayload = () => {
  const normalizedFeedbackMode = selectedInteractionType.value === INTERACTION_TYPE_VOICE
    ? "after_interview"
    : selectedFeedbackMode.value;
  const payload = {
    jobRole: selectedJob.value,
    jobRoleCode: selectedRoleCode.value,
    difficulty: difficultyMap[selectedDifficulty.value],
    interviewMode: selectedMode.value,
    feedbackMode: normalizedFeedbackMode,
    interactionType: selectedInteractionType.value,
    // 普通模拟面试也允许携带关联简历任务，便于后端优先复用明确的简历上下文。
    resumeTaskId: resumeTaskId.value || undefined,
  };

  if (!jobTargeted.value) {
    return payload;
  }

  return {
    ...payload,
    jobTargeted: true,
    jdText: jdText.value.trim() || undefined,
    useLatestJobMatch: useLatestJobMatch.value,
    jobMatchRecordId: latestJobMatchRecordId.value || undefined,
  };
};

const handleStart = async () => {
  if (!selectedJob.value) {
    ElMessage.warning("请选择面试岗位");
    return;
  }

  creating.value = true;
  const sessionRoutePrefetchPromise = prefetchInterviewSessionRoute()?.catch((error) => {
    console.debug("面试会话页预取失败", error);
  });
  try {
    const res = await createInterviewSession(buildCreatePayload());
    const data = res?.data || res || {};
    const sessionId = data.sessionId || data.id;
    if (!sessionId) {
      throw new Error("创建会话失败，未获取到会话 ID");
    }
    // 等接口返回时已经并行拉取页面 chunk，这里只等待剩余部分，避免跳转后白屏。
    if (sessionRoutePrefetchPromise) {
      await sessionRoutePrefetchPromise;
    }
    router.push(`/interview/session/${sessionId}`);
  } catch (err) {
    // skipDefaultErrorHandler 跳过了拦截器的 401 处理，需手动兜底
    if (err.response?.status === 401) {
      ElMessage.error("登录已过期，请重新登录");
      router.push({ path: "/login", query: { redirect: router.currentRoute.value.fullPath } });
      return;
    }
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      ElMessage.error("创建会话超时，请稍后刷新页面查看历史记录");
    } else {
      ElMessage.error(err.message || "创建面试会话失败，请稍后重试");
    }
  } finally {
    creating.value = false;
  }
};

onMounted(async () => {
  await fetchJobOptions();
  await fetchResumeTaskDetail();

  applyStoredInterviewPreferences();
  // 从面试报告页"再来一次"时，路由参数必须覆盖本机默认面试偏好。
  applyRouteQueryPreferences();
});
</script>

<style scoped>
.interview-entry-view {
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title);
}

.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.ready-bar {
  --interview-ready-bg: linear-gradient(135deg, #fffaf6 0%, #fff6ee 100%);
  --interview-ready-border: rgba(255, 194, 153, 0.38);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: var(--interview-ready-bg);
  border: 1px solid var(--interview-ready-border);
  border-radius: 10px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(132, 75, 32, 0.045);
}

.ready-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 52px;
  background: linear-gradient(135deg, var(--orange-main) 0%, var(--orange-deep) 100%);
  color: var(--bg-card);
  box-shadow: 0 8px 18px rgba(255, 140, 66, 0.18);
}

.ready-icon :deep(.feature-icon) {
  margin: auto;
}

.ready-text {
  font-size: 14px;
  color: var(--text-body);
}

.config-section {
  margin-bottom: 24px;
}

.config-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.config-item {
  margin-bottom: 32px;
}

.config-item:last-of-type {
  margin-bottom: 36px;
}

.config-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 14px;
  letter-spacing: 0.5px;
}

.config-control {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.job-select {
  width: 360px;
}

.job-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  border: 1px solid var(--border-card);
  box-shadow: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  padding: 0 16px;
}

.job-select :deep(.el-input__wrapper:hover) {
  border-color: var(--orange-main);
}

.job-select :deep(.el-input__wrapper.is-focus) {
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.1);
}

.job-select-popper {
  width: 380px !important;
  padding: 8px 0;
}

.job-select-popper .el-select-dropdown__item {
  height: 52px;
  padding: 0 20px;
  display: flex;
  align-items: center;
}

.job-select-popper .el-select-dropdown__item.hover,
.job-select-popper .el-select-dropdown__item:hover {
  background-color: var(--orange-light-bg);
}

.job-option-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 0;
}

.job-name {
  font-size: 15px;
  color: var(--text-title);
  font-weight: 500;
}

.job-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
  flex-shrink: 0;
  margin-left: 16px;
}

/* 标签样式模板（与管理端保持一致） */
.tag-style-default {
  background-color: rgba(255, 140, 66, 0.12);
  color: var(--orange-deep);
}

.tag-style-orange {
  background-color: rgba(255, 140, 66, 0.15);
  color: var(--orange-deep);
}

.tag-style-blue {
  background-color: rgba(47, 125, 225, 0.12);
  color: #2f7de1;
}

.tag-style-green {
  background-color: rgba(48, 176, 111, 0.12);
  color: #2a9658;
}

.tag-style-red {
  background-color: rgba(224, 84, 84, 0.12);
  color: #d64545;
}

.tag-style-purple {
  background-color: rgba(123, 90, 217, 0.12);
  color: #6b4dc9;
}

.tag-style-gray {
  background-color: rgba(143, 153, 167, 0.15);
  color: #6b7280;
}

.tag-style-outline {
  background-color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(217, 180, 154, 0.6);
  color: #9a5c33;
}

.tag-style-pill {
  background-color: rgba(255, 140, 66, 0.12);
  color: #b35f2b;
  border-radius: 999px;
}

.tag-style-pink {
  background-color: rgba(236, 113, 147, 0.12);
  color: #d64575;
}

.empty-options {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}

.pill-control {
  gap: 16px;
}

.pill-button {
  min-width: 116px;
  height: 44px;
  padding: 0 20px;
  border: 1px solid var(--border-card);
  border-radius: 22px;
  background-color: var(--bg-card);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: background-color 0.25s ease, color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
  font-family: inherit;
}

.pill-button:hover {
  border-color: var(--orange-main);
  background-color: var(--orange-light-bg);
}

.pill-button.active {
  border-color: var(--orange-main);
  background-color: var(--orange-main);
  box-shadow: 0 4px 12px rgba(255, 140, 66, 0.3);
}

.pill-button.disabled,
.pill-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

.pill-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-body);
  transition: color 0.25s ease;
}

.pill-hint {
  font-size: 11px;
  color: var(--text-muted);
  transition: color 0.25s ease;
}

.pill-button:hover .pill-label {
  color: var(--orange-main);
}

.pill-button.active .pill-label {
  color: var(--bg-card);
}

.pill-button.active .pill-hint {
  color: rgba(255, 255, 255, 0.85);
}

.job-target-card {
  border: 1px solid var(--border-card);
  border-radius: 14px;
  background: linear-gradient(180deg, #fffdfb 0%, #fff8f3 100%);
  overflow: hidden;
}

.job-target-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
}

.job-target-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 8px;
}

.job-target-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-body);
}

.job-target-body {
  border-top: 1px solid rgba(243, 216, 199, 0.7);
  padding: 20px 22px 22px;
}

.resume-link-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 13px;
}

.resume-link-label {
  color: var(--text-muted);
}

.resume-link-value {
  color: var(--orange-main);
  font-weight: 600;
}

.source-select-row {
  margin-bottom: 14px;
}

.source-option {
  border: 1px solid var(--border-card);
  background: var(--bg-card);
  border-radius: 12px;
  padding: 16px;
}

.source-option.active {
  border-color: #ffb27a;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.08);
}

.source-option-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.source-option-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
}

.source-option-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-body);
}

.job-match-summary {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 8px;
}

.summary-score {
  font-size: 13px;
  color: var(--orange-main);
  font-weight: 600;
  margin-bottom: 10px;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.summary-tags:last-child {
  margin-bottom: 0;
}

.summary-label {
  font-size: 12px;
  color: var(--text-muted);
}

.config-sub-item {
  margin-bottom: 16px;
}

.config-sub-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-title);
  margin-bottom: 10px;
}

.job-target-tip {
  font-size: 13px;
  line-height: 1.7;
  color: #8a5b39;
  background: rgba(255, 140, 66, 0.08);
  border-radius: 10px;
  padding: 12px 14px;
}

.start-section {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.start-section .el-button {
  padding: 14px 56px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff8c42 0%, #e67a35 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(255, 140, 66, 0.35);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.start-section .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.45);
}

.start-section .el-button:disabled {
  background: var(--border-card);
  border-color: var(--border-card);
  box-shadow: none;
  cursor: not-allowed;
}

.info-section {
  margin-bottom: 24px;
}

.info-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.info-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff8c42 0%, #ffb07a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--bg-card);
  flex-shrink: 0;
}

.info-text {
  font-size: 14px;
  color: var(--text-body);
}

@media (max-width: 768px) {
  .page-title {
    font-size: 20px;
  }

  .job-select {
    width: 100%;
  }

  .job-target-header,
  .source-option-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 18px;
  }

  .page-desc {
    font-size: 13px;
  }

  .config-card {
    padding: 20px 16px;
  }

  .pill-control {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .pill-button {
    min-width: 0;
    padding: 0 10px;
  }

  .start-section .el-button {
    width: 100%;
    padding: 14px 24px;
  }
}

/* ===== 暗色模式适配 ===== */
[data-theme="dark"] .job-target-card {
  background: var(--bg-elevated);
}

[data-theme="dark"] .ready-bar {
  --interview-ready-bg: linear-gradient(135deg, rgba(255, 176, 122, 0.11) 0%, rgba(255, 140, 66, 0.07) 100%);
  --interview-ready-border: rgba(255, 176, 122, 0.2);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18);
}

[data-theme="dark"] .job-target-body {
  border-top-color: var(--border-card);
}

[data-theme="dark"] .job-target-tip {
  color: #d0a07a;
  background: rgba(255, 140, 66, 0.06);
}

[data-theme="dark"] .job-match-summary {
  background: var(--bg-card);
}

[data-theme="dark"] .tag-style-blue {
  color: #6bb0f0;
}

[data-theme="dark"] .tag-style-green {
  color: #5cd487;
}

[data-theme="dark"] .tag-style-red {
  color: #f08080;
}

[data-theme="dark"] .tag-style-purple {
  color: #9d8be8;
}

[data-theme="dark"] .tag-style-gray {
  color: #9ca3af;
}

[data-theme="dark"] .tag-style-outline {
  background-color: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 140, 66, 0.3);
  color: #ffb07a;
}

[data-theme="dark"] .tag-style-pill {
  color: #ffb07a;
}

[data-theme="dark"] .tag-style-pink {
  color: #f08a9e;
}
</style>
