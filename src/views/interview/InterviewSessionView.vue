<template>
  <div class="interview-session-view interview-session-shell">
    <div class="session-status-bar">
      <div class="status-bar-left">
        <FeatureIcon name="ai-interviewer" size="sm" class="title-icon" />
        <span class="interview-title">模拟面试 · {{ sessionData?.jobRole || "加载中" }}</span>
        <span class="status-divider">|</span>
        <span class="difficulty-badge" :class="`difficulty-${sessionData?.difficulty || 1}`">
          {{ difficultyText }}
        </span>
        <span class="mode-text">{{ modeText }}</span>
        <span class="feedback-mode-text">{{ feedbackModeText }}</span>
        <span class="interaction-mode-text">{{ interactionModeText }}</span>
      </div>
      <div class="status-bar-right">
        <span class="status-indicator" :class="{ ended: isEnded }">
          <span class="status-dot"></span>
          <span class="status-text">{{ sessionStatusText }}</span>
        </span>
        <el-button link class="back-btn" @click="goBack" aria-label="返回">
          <FeatureIcon name="back" size="xs" />
        </el-button>
        <!-- 结束按钮：桌面端显示文字，移动端切换为挂断图标 SVG -->
        <el-button
          v-if="isInProgress"
          type="danger"
          plain
          size="small"
          class="end-btn"
          aria-label="结束面试"
          @click="endInterview"
        >
          <FeatureIcon name="interview-end" size="xs" class="end-btn-svg" />
          <span class="end-btn-text">结束面试</span>
        </el-button>
        <el-button
          v-else-if="isEnded"
          type="primary"
          size="small"
          class="report-btn"
          @click="viewReport"
        >
          查看报告
        </el-button>
      </div>
    </div>

    <div v-if="sessionData?.jobTargeted" class="job-target-banner">
      <div class="job-target-banner-title">岗位定向模拟已开启</div>
      <div class="job-target-banner-desc">{{ jobTargetSummary }}</div>
    </div>

    <div v-if="loading" class="loading-section">
      <div class="loading-content">
        <FeatureIcon name="loading" size="lg" class="loading-icon" />
        <div class="loading-text">加载面试会话...</div>
      </div>
    </div>

    <div v-else-if="error" class="error-section">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="fetchSessionDetail">重试</el-button>
          <el-button @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>

    <div v-else class="session-content session-main-surface">
      <div
        class="chat-stage"
        :class="{
          'voice-chat-stage': isVoiceSession,
          'voice-call-collapsed-stage': isVoiceSession && voiceCallCollapsed,
        }"
      >
        <div class="chat-container conversation-surface">
          <div class="chat-messages" ref="chatContainer">
            <!-- 开场白加载中 -->
            <div v-if="openingPending" class="opening-pending">
              <div class="opening-loading">
                <FeatureIcon name="ai-loading" size="lg" class="loading-icon" />
                <div class="loading-text">AI 面试官正在准备中...</div>
                <div class="loading-hint">首次生成可能需要 1-2 分钟，请稍候</div>
              </div>
            </div>

            <template v-else-if="groupedChatLogs.length > 0">
              <!-- 原有聊天记录代码 -->
              <template v-for="(item, itemIndex) in groupedChatLogs" :key="item.id || item.tempId || item.date">
                <!-- 原有代码保持不变 -->
                <div v-if="item.type === 'date-separator'" class="date-separator">
                  <span class="date-separator-line"></span>
                  <span class="date-separator-text">{{ formatDateSeparator(item.date) }}</span>
                  <span class="date-separator-line"></span>
                </div>

                <div
                  v-else-if="item.messageRole === 'assistant'"
                  class="message-row message-entrance assistant-row"
                  :style="{ '--message-index': itemIndex }"
                >
                  <div class="message-avatar assistant-avatar">
                    <img :src="assistantAvatar" alt="AI面试官" @error="handleImageError" />
                  </div>
                  <div class="message-content">
                    <div
                      class="message-bubble assistant-bubble"
                      :class="{ 'thinking-bubble': item.status === 'thinking' }"
                    >
                      <span v-if="item.status === 'thinking'" class="thinking-indicator">
                        <span class="thinking-text" aria-label="面试官正在思考你的回答">
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 0" aria-hidden="true">面</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 1" aria-hidden="true">试</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 2" aria-hidden="true">官</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 3" aria-hidden="true">正</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 4" aria-hidden="true">在</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 5" aria-hidden="true">思</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 6" aria-hidden="true">考</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 7" aria-hidden="true">你</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 8" aria-hidden="true">的</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 9" aria-hidden="true">回</span>
                          <span class="thinking-motion-unit thinking-char" style="--thinking-unit-index: 10" aria-hidden="true">答</span>
                        </span>
                        <span class="thinking-dots" aria-hidden="true">
                          <span class="thinking-motion-unit thinking-dot" style="--thinking-unit-index: 11"></span>
                          <span class="thinking-motion-unit thinking-dot" style="--thinking-unit-index: 12"></span>
                          <span class="thinking-motion-unit thinking-dot" style="--thinking-unit-index: 13"></span>
                        </span>
                      </span>
                      <span v-else-if="item.status === 'error'" class="error-text">回复失败，请重试</span>
                      <span v-else-if="item.status === 'streaming'" class="streaming-text">
                        {{ getAssistantDisplay(item).mainContent }}<span class="typing-cursor">|</span>
                      </span>
                      <span v-else class="done-text">{{ getAssistantDisplay(item).mainContent }}</span>
                    </div>
                    <div
                      v-if="getAssistantDisplay(item).feedbackContent"
                      class="message-feedback-card"
                    >
                      <div class="feedback-card-title">上一题回答的反馈</div>
                      <div class="feedback-card-body">{{ getAssistantDisplay(item).feedbackContent }}</div>
                    </div>
                    <div class="message-meta assistant-meta">
                      <span class="role-tag message-role-pill">面试官</span>
                      <span class="time-tag">{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>

                <div
                  v-else
                  class="message-row message-entrance user-row"
                  :style="{ '--message-index': itemIndex }"
                >
                  <div class="message-avatar user-avatar">
                    <img :src="userAvatar" alt="用户" @error="handleImageError" />
                  </div>
                  <div class="message-content">
                    <div class="message-bubble user-bubble">{{ item.content }}</div>
                    <div class="message-meta user-meta">
                      <span class="role-tag message-role-pill">我</span>
                      <span class="time-tag">{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </template>

            <div v-else class="empty-chat">
              <div class="empty-icon-wrapper">
                <FeatureIcon name="ai-interviewer" size="lg" class="empty-stage-icon" />
              </div>
              <p class="empty-title">等待开始</p>
              <p class="empty-desc">在下方输入你的回答，AI 面试官会继续追问。</p>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="isVoiceSession && isInProgress && !voiceCallCollapsed"
        class="voice-call-overlay"
        role="dialog"
        aria-label="语音面试通话"
      >
        <div class="voice-call-window">
          <div class="voice-window-bar">
            <button class="voice-window-btn" type="button" aria-label="折叠到聊天界面" title="折叠到聊天界面" @click="collapseVoiceCall">
              <span aria-hidden="true">−</span>
            </button>
            <button class="voice-window-btn" type="button" aria-label="挂断语音通话" title="挂断语音通话" @click="handleEndVoiceCall">
              <span aria-hidden="true">×</span>
            </button>
          </div>

          <div class="voice-avatar-wrap">
            <img class="voice-avatar" :src="assistantAvatar" alt="AI面试官" @error="handleImageError" />
          </div>

          <div class="voice-wave" :class="{ active: voiceCall.isListening.value, speaking: voiceCall.isAiSpeaking.value }" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </div>

          <div class="voice-call-title">{{ voiceCallTitle }}</div>
          <div class="voice-call-desc">{{ voiceCallDescription }}</div>
          <div class="voice-call-time">{{ formatCallDuration(voiceCall.callDuration.value) }}</div>

          <div v-if="voiceCall.pendingMessage.value" class="voice-pending-text voice-pending-floating">
            {{ voiceCall.pendingMessage.value }}
          </div>

          <div class="voice-dock-actions">
            <button class="voice-icon-btn" type="button" aria-label="折叠到聊天界面" title="折叠到聊天界面" @click="collapseVoiceCall">
              <FeatureIcon name="collapse" size="sm" />
            </button>
            <button
              class="voice-icon-btn"
              type="button"
              :aria-label="voiceCall.isManualResumePending.value ? '继续收音' : voiceCall.isVoiceMode.value ? '切换静音' : '开始通话'"
              :title="voiceCall.isManualResumePending.value ? '继续收音' : voiceCall.isVoiceMode.value ? '切换静音' : '开始通话'"
              :disabled="!voiceFeatureSupported || replyLocked"
              @click="handleMicControl"
            >
              <FeatureIcon :name="voiceCall.isMuted.value ? 'microphone-off' : 'microphone-on'" size="sm" />
              <span v-if="voiceCall.isMuted.value" class="muted-slash" aria-hidden="true"></span>
            </button>
            <button
              v-if="voiceCall.isVoiceMode.value"
              class="voice-icon-btn voice-submit-btn"
              type="button"
              aria-label="停止收听并发送"
              title="停止收听并发送"
              :disabled="replyLocked || voiceCall.isAiSpeaking.value"
              @click="handleStopListeningAndSend"
            >
              <FeatureIcon name="success" size="sm" />
            </button>
            <button class="voice-icon-btn voice-hangup-btn" type="button" aria-label="挂断语音通话" title="挂断语音通话" @click="handleEndVoiceCall">
              <FeatureIcon name="interview-end" size="sm" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isInProgress && (!isVoiceSession || voiceCallCollapsed)" class="input-area">
      <div v-if="isVoiceSession" class="voice-call-panel voice-call-panel-collapsed">
        <div class="voice-call-status">
          <div class="voice-call-main">
            <span class="voice-state-dot" :class="{ active: voiceCall.isListening.value, speaking: voiceCall.isAiSpeaking.value }"></span>
            <div>
              <div class="voice-call-title">{{ voiceCallTitle }}</div>
              <div class="voice-call-desc">{{ voiceCallDescription }}</div>
            </div>
          </div>
          <div class="voice-call-time">{{ formatCallDuration(voiceCall.callDuration.value) }}</div>
        </div>
        <div v-if="voiceCall.pendingMessage.value" class="voice-pending-text">
          {{ voiceCall.pendingMessage.value }}
        </div>
        <div class="voice-call-actions">
          <el-button
            v-if="!voiceCall.isVoiceMode.value"
            type="primary"
            :disabled="!voiceFeatureSupported || replyLocked"
            @click="handleStartVoiceCall"
          >
            <FeatureIcon name="microphone-on" size="xs" class="button-feature-icon" />
            开始通话
          </el-button>
          <template v-else>
            <el-button
              :type="voiceCall.isMuted.value ? 'warning' : 'default'"
              plain
              @click="handleToggleMute"
            >
              <FeatureIcon :name="voiceCall.isMuted.value ? 'microphone-off' : 'microphone-on'" size="xs" class="button-feature-icon" />
              {{ voiceCall.isManualResumePending.value ? '继续收音' : voiceCall.isMuted.value ? '取消静音' : '静音' }}
            </el-button>
            <el-button
              plain
              :disabled="replyLocked || voiceCall.isAiSpeaking.value"
              @click="handleStopListeningAndSend"
            >
              <FeatureIcon name="success" size="xs" class="button-feature-icon" />
              停止收听并发送
            </el-button>
            <el-button plain @click="expandVoiceCall">
              <FeatureIcon name="fullscreen" size="xs" class="button-feature-icon" />
              展开
            </el-button>
            <el-button
              type="danger"
              plain
              @click="handleEndVoiceCall"
            >
              <FeatureIcon name="interview-end" size="xs" class="button-feature-icon" />
              挂断
            </el-button>
          </template>
        </div>
      </div>
      <div v-else class="input-container">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="输入你的回答，AI 面试官将继续追问..."
          resize="none"
          maxlength="2000"
          show-word-limit
          @keyup.enter.ctrl="sendMessage"
        />
        <div class="input-footer">
          <span class="input-hint"><kbd>Ctrl</kbd> + <kbd>Enter</kbd> 发送</span>
          <div class="input-actions">
            <el-button
              v-if="sttSupported"
              :class="['mic-btn', { 'is-recording': sttRecording }]"
              :type="sttRecording ? 'danger' : 'default'"
              :disabled="replyLocked"
              circle
              @click="sttToggle"
            >
              <FeatureIcon :name="sttRecording ? 'microphone-off' : 'microphone-on'" size="sm" />
            </el-button>
            <el-button
              type="primary"
              :loading="replyLocked"
              :disabled="replyLocked || !inputMessage.trim()"
              class="send-btn"
              @click="sendMessage"
            >
              {{ replyLocked ? 'AI 回复中...' : '发送回答' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isEnded" class="ended-notice">
      <FeatureIcon name="success" size="sm" />
      <span>面试已结束，你可以查看报告或返回历史记录。</span>
    </div>

    <el-dialog v-model="showEndDialog" title="结束面试" width="400px">
      <p>确认结束本次面试吗？结束后将无法继续回答。</p>
      <template #footer>
        <el-button @click="showEndDialog = false">取消</el-button>
        <el-button type="primary" :loading="ending" @click="confirmEndInterview">
          确认结束
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  getDifficultyLabel,
  getFeedbackModeLabel,
  getInteractionTypeLabel,
  getInterviewModeLabel,
  INTERACTION_TYPE_TEXT,
  INTERACTION_TYPE_VOICE,
} from '@/constants/interview'
import {
  endInterview as apiEndInterview,
  getInterviewSession,
  streamInterviewMessage,
} from "@/api/interview";
import { ElMessage } from "element-plus";
import { getToken } from "@/utils/auth";
import { getSettingsPreferences } from "@/utils/settingsPreferences";
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import { useSpeechToText } from "@/composables/useSpeechToText";
import { useTextToSpeech } from "@/composables/useTextToSpeech";
import { useVoiceCall } from "@/composables/useVoiceCall";

import assistantAvatarImg from "@/assets/assistant.png";
import userAvatarImg from "@/assets/user.png";

const router = useRouter();
const route = useRoute();
const settingsPreferences = getSettingsPreferences();

const sessionId = computed(() => route.params.sessionId);
const chatContainer = ref(null);

const loading = ref(true);
const error = ref("");
const sessionData = ref(null);
const inputMessage = ref("");
const sending = ref(false);
const ending = ref(false);
const showEndDialog = ref(false);
const openingPending = ref(false);
const replyLocked = ref(false);
const voiceCallCollapsed = ref(false);
const openingSpeechPlayed = ref(false);
const feedbackMode = computed(() => sessionData.value?.feedbackMode || "after_interview");
const feedbackModeText = computed(() => getFeedbackModeLabel(feedbackMode.value));
const interactionType = computed(() => sessionData.value?.interactionType ?? INTERACTION_TYPE_TEXT);
const isVoiceSession = computed(() => interactionType.value === INTERACTION_TYPE_VOICE);
const interactionModeText = computed(() => getInteractionTypeLabel(interactionType.value));
const voiceFeatureSupported = computed(() => voiceSttSupported.value && textToSpeech.isSupported.value);
const voiceCallTitle = computed(() => {
  if (!voiceFeatureSupported.value) return "当前浏览器不支持语音通话";
  if (!voiceCall.isVoiceMode.value) return "语音面试待开始";
  if (voiceCall.isMuted.value) return "已静音";
  if (voiceCall.isManualResumePending.value) return "等待继续收音";
  if (replyLocked.value || voiceCall.isAiSpeaking.value) return "AI 正在回复";
  if (voiceCall.isListening.value) return "正在聆听";
  return "通话准备中";
});
const voiceCallDescription = computed(() => {
  if (!voiceFeatureSupported.value) return "请切换到支持 Web Speech API 的 Chrome 浏览器，或返回创建文字面试。";
  if (!voiceCall.isVoiceMode.value) return "点击开始通话后再授权麦克风，页面刷新不会自动开麦。";
  if (voiceCall.isMuted.value) return "麦克风已关闭，取消静音后会继续收音。";
  if (voiceCall.isManualResumePending.value) return "已取消静音，再次点击麦克风后继续收音。";
  if (replyLocked.value || voiceCall.isAiSpeaking.value) return "AI 朗读时会暂停收音，避免播报内容被误识别。";
  if (!settingsPreferences.voiceAutoSubmitDelayMs) return "当前已关闭静音自动提交，请使用停止收听并发送。";
  return `说完后静音 ${settingsPreferences.voiceAutoSubmitDelayMs / 1000} 秒会自动发送本轮回答。`;
});
let openingPollingTimer = null;

const {
  isSupported: sttSupported,
  isRecording: sttRecording,
  finalTranscript: sttFinal,
  interimTranscript: sttInterim,
  error: sttError,
  language: sttLanguage,
  cancel: sttCancel,
  toggle: sttToggle,
} = useSpeechToText();

const {
  isSupported: voiceSttSupported,
  isRecording: voiceSttRecording,
  isVoiceActive: voiceSttVoiceActive,
  voiceActivityAt: voiceSttVoiceActivityAt,
  finalTranscript: voiceSttFinal,
  interimTranscript: voiceSttInterim,
  error: voiceSttError,
  language: voiceSttLanguage,
  start: voiceSttStart,
  stop: voiceSttStop,
  cancel: voiceSttCancel,
} = useSpeechToText();

const textToSpeech = useTextToSpeech({
  rate: settingsPreferences.voiceSpeakingRate,
  pitch: settingsPreferences.voicePitch,
  volume: settingsPreferences.voiceVolume,
  voicePreference: {
    type: settingsPreferences.voicePreferredType,
    name: settingsPreferences.voiceName,
    voiceURI: settingsPreferences.voiceURI,
    lang: settingsPreferences.voiceLang,
  },
  onEnd: () => {
    voiceCall.resumeListening();
  },
});

const voiceCall = useVoiceCall({
  speech: {
    isSupported: voiceSttSupported,
    isRecording: voiceSttRecording,
    isVoiceActive: voiceSttVoiceActive,
    voiceActivityAt: voiceSttVoiceActivityAt,
    finalTranscript: voiceSttFinal,
    interimTranscript: voiceSttInterim,
    error: voiceSttError,
    start: voiceSttStart,
    stop: voiceSttStop,
    cancel: voiceSttCancel,
  },
  textToSpeech,
  isReplying: replyLocked,
  silenceTimeoutMs: settingsPreferences.voiceAutoSubmitDelayMs,
  muteResumeMode: settingsPreferences.voiceMuteResumeMode,
  onSend: (content) => sendMessage(content),
});

const sttLang = computed(() =>
  settingsPreferences.voiceRecognitionLanguage === 'auto'
    ? (sessionData.value?.interviewMode === 'foreign_interviewer' ? 'en-US' : 'zh-CN')
    : settingsPreferences.voiceRecognitionLanguage
);

const sttDraftBase = ref('');
const syncingSttDraft = ref(false);

watch(sttLang, (lang) => {
  sttLanguage.value = lang;
  voiceSttLanguage.value = lang;
}, { immediate: true });

watch([sttFinal, sttInterim], () => {
  if (isVoiceSession.value && voiceCall.isVoiceMode.value) {
    return;
  }
  if (!sttRecording.value && !sttFinal.value && !sttInterim.value) {
    return;
  }

  syncingSttDraft.value = true;
  inputMessage.value = `${sttDraftBase.value}${sttFinal.value}${sttInterim.value}`;
});

watch(sttRecording, (recording) => {
  if (isVoiceSession.value && voiceCall.isVoiceMode.value) {
    return;
  }
  if (recording) {
    sttDraftBase.value = inputMessage.value;
    return;
  }

  sttDraftBase.value = '';
});

watch(inputMessage, (nextValue) => {
  if (isVoiceSession.value && voiceCall.isVoiceMode.value) {
    return;
  }
  if (syncingSttDraft.value) {
    syncingSttDraft.value = false;
    return;
  }

  if (!sttRecording.value) {
    return;
  }

  const expectedDraft = `${sttDraftBase.value}${sttFinal.value}${sttInterim.value}`;
  if (nextValue !== expectedDraft) {
    sttCancel();
  }
});

watch(sttError, (err) => {
  if (err) ElMessage.warning(err);
});

watch(voiceCall.error, (err) => {
  if (err) ElMessage.warning(err);
});

const assistantAvatar = assistantAvatarImg;
const userAvatar = userAvatarImg;

const isInProgress = computed(() => sessionData.value?.status === 0);
const isEnded = computed(() => sessionData.value?.status === 1);

const sessionStatusText = computed(() => {
  if (!sessionData.value) return "加载中";
  return sessionData.value.status === 0 ? "进行中" : "已结束";
});

const difficultyText = computed(() => {
  return getDifficultyLabel(sessionData.value?.difficulty, '初级')
});

const modeText = computed(() => {
  if (sessionData.value?.interviewModeDesc) {
    return sessionData.value.interviewModeDesc;
  }
  if (sessionData.value?.jobTargeted || sessionData.value?.interviewMode === "job_targeted") {
    return "岗位定向模拟";
  }
  return getInterviewModeLabel(sessionData.value?.interviewMode);
});

const jobTargetSummary = computed(() => {
  const context = sessionData.value?.jobTargetContext;
  if (!context) {
    return "系统会结合岗位要求生成更贴近目标岗位的问题与反馈建议。";
  }
  const sourceMap = {
    manual_jd: "当前使用的是你手动输入的岗位 JD。",
    manual_jd_with_job_match: "当前同时复用了手动 JD 和最近一次 JD 对比分析结果。",
    latest_job_match: "当前复用了最近一次 JD 对比分析结果。",
  };
  const matchedCount = Array.isArray(context.matchedKeywords) ? context.matchedKeywords.length : 0;
  const missingCount = Array.isArray(context.missingKeywords) ? context.missingKeywords.length : 0;
  return `${sourceMap[context.sourceType] || "当前已开启岗位定向模拟。"} 已带入 ${matchedCount} 个已匹配关键词和 ${missingCount} 个待补强关键词。`;
});

const chatLogs = computed(() => sessionData.value?.chatLogs || []);

const stripFollowUpPrefix = (content = "") => {
  return String(content || "").trim().replace(/^(追问|问题)[:：]\s*/u, "");
};

const trimPartialFeedbackStart = (content = "") => {
  const text = String(content || "");
  const fullStartIndex = text.search(/<FEEDBACK>/i);
  if (fullStartIndex !== -1) {
    return text.slice(0, fullStartIndex);
  }

  const upperText = text.toUpperCase();
  const feedbackTag = "<FEEDBACK>";
  const searchStart = Math.max(0, text.length - feedbackTag.length + 1);
  for (let index = searchStart; index < text.length; index += 1) {
    const tail = upperText.slice(index);
    if (feedbackTag.startsWith(tail)) {
      return text.slice(0, index);
    }
  }

  return text;
};

const parseAssistantFeedback = (content = "") => {
  const text = String(content || "");
  const match = text.match(/<FEEDBACK>\s*([\s\S]*?)\s*<\/FEEDBACK>/i);
  if (!match) {
    return {
      mainContent: stripFollowUpPrefix(trimPartialFeedbackStart(text)),
      feedbackContent: "",
    };
  }
  return {
    mainContent: stripFollowUpPrefix(text.replace(match[0], "")),
    feedbackContent: match[1].replace(/^本题反馈[:：]\s*/u, "").trim(),
  };
};

const getAssistantDisplay = (item) => {
  const source = item?.status === "streaming"
    ? item.displayContent || ""
    : item?.content || item?.displayContent || "";
  if (feedbackMode.value !== "immediate") {
    return {
      mainContent: source,
      feedbackContent: "",
    };
  }
  return parseAssistantFeedback(source);
};

const getOpeningSpeechContent = () => {
  // 只有尚未出现用户回答的首轮会话才播报开场白；继续历史面试时不重复朗读第一题。
  if (chatLogs.value.some((item) => item.messageRole === "user")) {
    return "";
  }
  const openingMessage = chatLogs.value.find((item) => item.messageRole === "assistant");
  return getAssistantDisplay(openingMessage).mainContent.trim();
};

const speakOpeningMessageOnce = () => {
  if (openingSpeechPlayed.value || !isVoiceSession.value || !voiceCall.isVoiceMode.value) {
    return;
  }
  const openingContent = getOpeningSpeechContent();
  if (!openingContent) {
    return;
  }

  // 语音通话首次启动时先播报已生成的开场白，避免用户进入通话后直接面对空白收音状态。
  openingSpeechPlayed.value = true;
  // 播报开场白前必须关闭语音识别，避免浏览器把扬声器里的开场白误识别成用户回答。
  voiceSttCancel();
  textToSpeech.speak(openingContent);
};

const groupedChatLogs = computed(() => {
  const logs = chatLogs.value;
  if (!logs.length) return [];

  const result = [];
  let currentDate = "";
  logs.forEach((msg) => {
    const msgDate = new Date(msg.createTime).toDateString();
    if (msgDate !== currentDate) {
      currentDate = msgDate;
      result.push({
        type: "date-separator",
        date: msg.createTime,
        id: `date-${msg.createTime}`,
      });
    }
    result.push(msg);
  });
  return result;
});

const normalizeChatLogs = (logs) => {
  return (Array.isArray(logs) ? logs : []).map((msg) => ({
    ...msg,
    displayContent: msg.content || "",
    pendingContent: "",
    status: msg.status || "done",
    rawContent: msg.content || "",
    streamFinished: true,
  }));
};

const fetchSessionDetail = async () => {
  if (!sessionId.value) {
    error.value = "会话 ID 不存在";
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";
  try {
    const res = await getInterviewSession(sessionId.value);
    const data = res.data || {};
    sessionData.value = {
      ...data,
      chatLogs: normalizeChatLogs(data.chatLogs),
    };
    
    // 处理开场白待生成状态
    if (data.openingPending) {
      openingPending.value = true;
      startOpeningPolling();
    } else {
      openingPending.value = false;
    }
    
    await nextTick();
    scrollToBottom();
  } catch (err) {
    error.value = err.message || "获取会话详情失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};

const OPENING_POLL_MAX_ROUNDS = 60; // 最多轮询 60 次，约 3 分钟超时
let openingPollRounds = 0;

const startOpeningPolling = () => {
  stopOpeningPolling();
  openingPollRounds = 0;
  const poll = async () => {
    openingPollRounds++;
    if (openingPollRounds > OPENING_POLL_MAX_ROUNDS) {
      openingPending.value = false;
      ElMessage.warning("开场白生成超时，请刷新页面重试");
      return;
    }
    try {
      const res = await getInterviewSession(sessionId.value);
      const data = res.data || {};
      if (!data.openingPending && data.chatLogs?.length > 0) {
        openingPending.value = false;
        sessionData.value = {
          ...data,
          chatLogs: normalizeChatLogs(data.chatLogs),
        };
        await nextTick();
        scrollToBottom();
        speakOpeningMessageOnce();
        return;
      }
    } catch (err) {
      // 后端开场白生成是异步过程，单次轮询失败不一定意味着永久错误，
      // 但完全静默会让排查变困难，至少要在控制台留痕方便定位。
      console.warn("开场白轮询失败，将在 3 秒后重试", err);
    }
    openingPollingTimer = setTimeout(poll, 3000);
  };
  openingPollingTimer = setTimeout(poll, 3000);
};

const stopOpeningPolling = () => {
  if (openingPollingTimer) {
    clearTimeout(openingPollingTimer);
    openingPollingTimer = null;
  }
};

const formatDateSeparator = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  if (Number.isNaN(date.getTime())) return "";
  const today = new Date().toDateString();
  const yesterday = new Date(Date.now() - 86400000).toDateString();
  if (date.toDateString() === today) return "今天";
  if (date.toDateString() === yesterday) return "昨天";
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月${pad(date.getDate())}日`;
};

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

const handleImageError = (event) => {
  event.target.style.display = "none";
};

const scrollToBottom = () => {
  nextTick(() => {
    const container = chatContainer.value;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
};

let typingTimer = null;
const TYPE_INTERVAL_MS = 35;

// 维护当前活跃 SSE 流的 AbortController：
// 路由切换、组件卸载、连发同一会话的下一条消息时，都需要中止旧流，
// 否则旧 reader 会继续向已被移除的 chatLogs 项写入字节，并占用 fetch 连接。
let activeStreamController = null;

const abortActiveStream = (reason = "stream-superseded") => {
  if (activeStreamController) {
    try {
      activeStreamController.abort(reason);
    } catch {
      // AbortController 在部分旧浏览器下不接受 reason 参数，吞掉以兼容
    }
    activeStreamController = null;
  }
};

const stopTypingMachine = () => {
  if (typingTimer) {
    clearInterval(typingTimer);
    typingTimer = null;
  }
};

const startTypingMachine = (tempMsgId) => {
  stopTypingMachine();
  typingTimer = setInterval(() => {
    const logs = sessionData.value?.chatLogs || [];
    const msgIndex = logs.findIndex((item) => item.id === tempMsgId);
    if (msgIndex === -1) {
      stopTypingMachine();
      return;
    }

    const msg = logs[msgIndex];
    if (msg.pendingContent.length > 0) {
      msg.displayContent += msg.pendingContent[0];
      msg.pendingContent = msg.pendingContent.substring(1);
      msg.status = "streaming";
      nextTick(() => scrollToBottom());
      return;
    }

    if (msg.streamFinished) {
      stopTypingMachine();
      if (msg.status !== "error") {
        msg.content = msg.rawContent;
        msg.status = "done";
      }
      replyLocked.value = false;
      sending.value = false;
    }
  }, TYPE_INTERVAL_MS);
};

const sendMessage = async (overrideContent = "") => {
  // Element Plus 点击事件会传入 PointerEvent，只有语音通话自动发送时才允许用字符串覆盖输入框内容。
  const messageSource = typeof overrideContent === "string" && overrideContent ? overrideContent : inputMessage.value;
  const content = messageSource.trim();
  if (!content || !sessionId.value || !sessionData.value || replyLocked.value) {
    return;
  }

  textToSpeech.stop();
  sttCancel();
  voiceSttCancel();
  replyLocked.value = true;
  sending.value = true;
  const tempMsgId = `temp-${Date.now()}`;
  const now = new Date().toISOString();
  const assistantMsg = {
    id: tempMsgId,
    messageRole: "assistant",
    content: "",
    displayContent: "",
    pendingContent: "",
    rawContent: "",
    status: "thinking",
    streamFinished: false,
    createTime: now,
  };

  sessionData.value.chatLogs = [
    ...(sessionData.value.chatLogs || []),
    { id: `user-${Date.now()}`, messageRole: "user", content, createTime: now },
    assistantMsg,
  ];
  inputMessage.value = "";
  await nextTick();
  scrollToBottom();

  // 网络差或 SSE 中断时直接失败并解锁，避免界面长时间停留在重连状态。
  let streamSucceeded = false;
  let receivedDone = false;
  startTypingMachine(tempMsgId);

  const applyStreamPayload = (payload) => {
    const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
    if (msgIndex === -1) return;
    const msg = sessionData.value.chatLogs[msgIndex];

    if (payload.type === "content") {
      const normalizedData = (payload.content || "").replace(/\r/g, "");
      if (normalizedData) {
        const previousRawContent = msg.rawContent;
        msg.status = "streaming";
        msg.rawContent += normalizedData;
        msg.pendingContent += normalizedData;
        if (isVoiceSession.value && voiceCall.isVoiceMode.value) {
          // 语音播报只读自然追问部分，避免把即时反馈标签和结构化反馈读出来。
          const speechChunk = previousRawContent.includes("<FEEDBACK>")
            ? ""
            : normalizedData.split("<FEEDBACK>")[0];
          textToSpeech.speakStreaming(speechChunk);
        }
      }
      return;
    }
    if (payload.type === "done") {
      receivedDone = true;
      msg.streamFinished = true;
      if (isVoiceSession.value && voiceCall.isVoiceMode.value) {
        textToSpeech.flushRemaining();
      }
      return;
    }
    if (payload.type === "error") {
      throw new Error(payload.message || "AI 回复失败");
    }
  };

  const consumeSseBuffer = (sseBuffer, applyFn) => {
    while (true) {
      const eventEndIndex = sseBuffer.indexOf("\n\n");
      if (eventEndIndex === -1) break;
      const eventBlock = sseBuffer.slice(0, eventEndIndex).replace(/\r/g, "");
      sseBuffer = sseBuffer.slice(eventEndIndex + 2);
      if (!eventBlock.trim()) continue;
      const dataLines = eventBlock
        .split("\n")
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.slice("data:".length).trim());
      if (!dataLines.length) continue;
      const jsonStr = dataLines.join("\n");
      let parsedPayload;
      try {
        parsedPayload = JSON.parse(jsonStr);
      } catch {
        continue;
      }
      applyFn(parsedPayload);
    }
    return sseBuffer;
  };

  const readSseStream = async (reader, decoder, applyFn) => {
    let sseBuffer = "";
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        sseBuffer += decoder.decode();
        consumeSseBuffer(sseBuffer, applyFn);
        break;
      }
      sseBuffer += decoder.decode(value, { stream: true });
      sseBuffer = consumeSseBuffer(sseBuffer, applyFn);
    }
  };

  try {
    const token = getToken();
    // 连发新消息时，先中止上一条仍在读取的 SSE 流，避免旧 reader 写入已被清理的 chatLogs 项。
    abortActiveStream("new-message");
    activeStreamController = new AbortController();
    const streamController = activeStreamController;
    const response = await streamInterviewMessage(
      sessionId.value,
      { sessionId: sessionId.value, content, feedbackMode: feedbackMode.value },
      token,
      { signal: streamController.signal }
    );

    if (!response.ok) {
      // 401 表示 token 失效，必须跳到登录页，否则用户停在面试页只看到通用错误 toast。
      if (response.status === 401) {
        ElMessage.error("登录已过期，请重新登录");
        router.push({ path: "/login", query: { redirect: router.currentRoute.value.fullPath } });
        const authError = new Error("登录已过期，请重新登录");
        authError.suppressToast = true;
        throw authError;
      }
      let errMsg = `请求失败 (${response.status})`;
      try {
        const errBody = await response.json();
        errMsg = errBody.message || errBody.msg || errMsg;
      } catch { /* 保持最小兜底 */ }
      throw new Error(errMsg);
    }
    if (!response.body) throw new Error("未获取到流式响应体");

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    await readSseStream(reader, decoder, applyStreamPayload);

    if (!receivedDone) {
      throw new Error("AI 回复流已中断，请重新发送");
    }

    // 只有当前 SSE 流完整结束才标记成功，中断时交给外层错误状态处理。
    streamSucceeded = true;
    const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
    if (msgIndex !== -1) {
      sessionData.value.chatLogs[msgIndex].streamFinished = true;
    }
    if (activeStreamController === streamController) {
      activeStreamController = null;
    }
  } catch (err) {
    // AbortError 是主动取消（路由切换、连发新消息、组件卸载），不展示错误 toast。
    if (err?.name === "AbortError") {
      const msgIndex = sessionData.value?.chatLogs?.findIndex((item) => item.id === tempMsgId);
      if (msgIndex !== undefined && msgIndex !== -1) {
        sessionData.value.chatLogs[msgIndex].streamFinished = true;
      }
      replyLocked.value = false;
      sending.value = false;
      return;
    }
    if (isVoiceSession.value && voiceCall.isVoiceMode.value) {
      voiceCall.endVoiceCall();
    }
    textToSpeech.stop();
    if (!err?.suppressToast) {
      ElMessage.error(err.message || "发送消息失败，请稍后重试");
    }
    const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
    if (msgIndex !== -1) {
      sessionData.value.chatLogs[msgIndex].streamFinished = true;
      if (!streamSucceeded) {
        sessionData.value.chatLogs[msgIndex].status = "error";
      }
    }
    replyLocked.value = false;
    sending.value = false;
  }
};

const formatCallDuration = (duration) => {
  const totalSeconds = Number(duration || 0);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
};

const handleStartVoiceCall = () => {
  if (!voiceCall.startVoiceCall()) {
    ElMessage.warning(voiceCall.error.value || "无法启动语音通话");
    return;
  }
  speakOpeningMessageOnce();
};

const handleEndVoiceCall = () => {
  voiceCall.endVoiceCall();
  voiceCallCollapsed.value = true;
  ElMessage.success("语音通话已挂断");
};

const handleToggleMute = () => {
  const muted = voiceCall.toggleMute();
  if (voiceCall.isManualResumePending.value) {
    ElMessage.success("已取消静音，再次点击麦克风后继续收音");
    return;
  }
  ElMessage.success(muted ? "已静音" : "已取消静音");
};

const handleStopListeningAndSend = async () => {
  // 手动停止用于跳过 3 秒静音等待；没有识别文本时给出明确提示，不重置静音状态。
  const sent = await voiceCall.stopListeningAndSend();
  if (!sent) {
    ElMessage.warning("当前还没有可发送的识别文本");
  }
};

const handleMicControl = () => {
  if (!voiceCall.isVoiceMode.value) {
    handleStartVoiceCall();
    return;
  }
  handleToggleMute();
};

const collapseVoiceCall = () => {
  voiceCallCollapsed.value = true;
};

const expandVoiceCall = () => {
  voiceCallCollapsed.value = false;
};

const endInterview = () => {
  if (voiceCall.isVoiceMode.value) {
    voiceCall.endVoiceCall();
  }
  showEndDialog.value = true;
};

const confirmEndInterview = async () => {
  if (!sessionId.value) return;
  ending.value = true;
  try {
    await apiEndInterview(sessionId.value);
    showEndDialog.value = false;
    ElMessage.success("面试已结束，报告生成中...");
    await fetchSessionDetail();
  } catch {
    // 拦截器已弹出错误提示
  } finally {
    ending.value = false;
  }
};

const viewReport = () => {
  if (!sessionId.value) return;
  router.push(`/interview/report/${sessionId.value}`);
};

const goBack = () => {
  router.back();
};

onMounted(() => {
  fetchSessionDetail();
});

onBeforeUnmount(() => {
  // 组件卸载时主动中止正在进行的 SSE 流，避免后台 reader 继续向已被销毁的状态写入。
  abortActiveStream("component-unmount");
  stopTypingMachine();
  stopOpeningPolling();
  voiceCall.endVoiceCall();
});
</script>

<style scoped>
.interview-session-view {
  height: 100vh;
  --interview-bg: #fff7ef;
  --interview-bg-soft: #fffaf5;
  --interview-surface: rgba(255, 255, 255, 0.94);
  --interview-surface-strong: rgba(255, 255, 255, 0.98);
  --interview-border: rgba(229, 177, 135, 0.32);
  --interview-border-strong: rgba(255, 140, 66, 0.34);
  --interview-shadow: 0 18px 48px rgba(117, 72, 38, 0.12);
  --interview-shadow-soft: 0 8px 24px rgba(117, 72, 38, 0.08);
  --interview-ink: var(--text-title);
  --interview-muted: var(--text-muted);
  --interview-accent: #ff8c42;
  --interview-accent-strong: #f97316;
  --interview-ease: cubic-bezier(0.22, 1, 0.36, 1);
  background:
    radial-gradient(circle at 14% 12%, rgba(255, 180, 92, 0.18), transparent 30%),
    linear-gradient(180deg, var(--interview-bg-soft) 0%, var(--interview-bg) 48%, var(--bg-page) 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: var(--interview-ink);
}

.session-status-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: var(--interview-surface-strong);
  border-bottom: 1px solid var(--interview-border);
  box-shadow: 0 8px 26px rgba(117, 72, 38, 0.07);
  animation: sessionSurfaceIn 420ms var(--interview-ease) both;
}

.status-bar-left,
.status-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.interview-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-icon,
.empty-stage-icon {
  flex-shrink: 0;
}

.status-divider {
  color: var(--border-card);
}

.difficulty-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.difficulty-badge.difficulty-1 {
  background: #e8f5e9;
  color: #4caf50;
}

.difficulty-badge.difficulty-2 {
  background: #fff3e0;
  color: #ff9800;
}

.difficulty-badge.difficulty-3 {
  background: #ffebee;
  color: #f44336;
}

.mode-text,
.feedback-mode-text,
.interaction-mode-text,
.status-indicator {
  font-size: 13px;
  color: var(--text-body);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 结束按钮图标：默认仅在移动端显示 */
.end-btn-svg {
  display: none;
  width: 16px;
  height: 16px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
}

.status-indicator.ended .status-dot {
  background: #909399;
}

.job-target-banner {
  position: sticky;
  top: 0;
  flex-shrink: 0;
  padding: 12px 24px;
  background: linear-gradient(135deg, #fff4ea 0%, #fffaf6 100%);
  border-bottom: 1px solid rgba(243, 216, 199, 0.6);
}

.job-target-banner-title {
  font-size: 13px;
  font-weight: 600;
  color: #b86127;
  margin-bottom: 4px;
}

.job-target-banner-desc {
  font-size: 13px;
  line-height: 1.7;
  color: #7b5b48;
}

.loading-section,
.error-section {
  flex:1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.opening-pending {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

.opening-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.opening-loading .loading-icon {
  color: var(--orange-main);
  animation: spin 1s linear infinite;
}

.opening-loading .loading-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-title);
}

.opening-loading .loading-hint {
  font-size: 13px;
  color: var(--text-muted);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-icon {
  color: var(--orange-main);
  animation: spin 1s linear infinite;
}


.loading-text {
  font-size: 14px;
  color: var(--text-body);
}

.session-content {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.session-main-surface::before,
.session-main-surface::after {
  content: "";
  position: absolute;
  pointer-events: none;
  border-radius: 999px;
}

.session-main-surface::before {
  width: 360px;
  height: 360px;
  right: -110px;
  top: 9%;
  background: radial-gradient(circle, rgba(255, 140, 66, 0.13), transparent 68%);
}

.session-main-surface::after {
  width: 240px;
  height: 240px;
  left: -96px;
  bottom: 12%;
  background: radial-gradient(circle, rgba(255, 210, 150, 0.16), transparent 70%);
}

.chat-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.voice-chat-stage .chat-container {
  padding-bottom: 40px;
}

.voice-call-collapsed-stage .chat-container {
  padding-bottom: 16px;
}

.voice-session-stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
  padding: 32px 24px;
}

.chat-container {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: clamp(16px, 3vw, 28px);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.conversation-surface {
  position: relative;
  z-index: 1;
  animation: sessionSurfaceIn 520ms var(--interview-ease) both;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px 16px 12px 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: 0;
  scroll-behavior: smooth;
  scroll-padding-top: 24px;
}

.message-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.message-entrance {
  animation: messageFloatIn 360ms var(--interview-ease) both;
  animation-delay: min(calc(var(--message-index) * 42ms), 220ms);
}

.user-row {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--interview-surface);
  border: 1px solid rgba(255, 140, 66, 0.16);
  box-shadow: var(--interview-shadow-soft);
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  max-width: min(76%, 720px);
  display: flex;
  flex-direction: column;
}

.assistant-row .message-content {
  align-items: flex-start;
}

.user-row .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 15px 18px;
  border-radius: 18px;
  font-size: 14.5px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-line;
  transition: transform 220ms var(--interview-ease), box-shadow 220ms var(--interview-ease), border-color 220ms var(--interview-ease);
}

.assistant-bubble {
  background: var(--interview-surface);
  border: 1px solid var(--interview-border);
  color: var(--interview-ink);
  border-top-left-radius: 6px;
  box-shadow: 0 12px 32px rgba(117, 72, 38, 0.08);
}

.assistant-row:hover .assistant-bubble,
.user-row:hover .user-bubble {
  transform: translateY(-2px);
}

.thinking-bubble {
  position: relative;
  min-width: 246px;
  overflow: hidden;
  border-color: rgba(255, 140, 66, 0.28);
  background: linear-gradient(135deg, rgba(255, 250, 246, 0.98) 0%, var(--bg-card) 100%);
  box-shadow: 0 8px 24px rgba(255, 140, 66, 0.1);
  animation: thinkingBubbleBreath 2.4s ease-in-out infinite;
}

.thinking-bubble::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
    110deg,
    transparent 0%,
    transparent 35%,
    rgba(255, 140, 66, 0.12) 48%,
    transparent 62%,
    transparent 100%
  );
  transform: translateX(-120%);
  animation: thinkingBubbleSweep 2.8s ease-in-out infinite;
}

.message-feedback-card {
  margin-top: 10px;
  padding: 12px 14px;
  max-width: 100%;
  border: 1px solid rgba(255, 140, 66, 0.22);
  border-radius: 8px;
  background: rgba(255, 248, 244, 0.96);
  color: var(--text-body);
  line-height: 1.65;
  box-shadow: 0 8px 22px rgba(255, 140, 66, 0.07);
}

.feedback-card-title {
  margin-bottom: 6px;
  color: var(--orange-main);
  font-size: 12px;
  font-weight: 700;
}

.feedback-card-body {
  font-size: 13px;
  white-space: pre-line;
  word-break: break-word;
}

.user-bubble {
  background: linear-gradient(135deg, var(--interview-accent) 0%, var(--interview-accent-strong) 100%);
  color: var(--bg-card);
  border-top-right-radius: 6px;
  box-shadow: 0 14px 30px rgba(255, 128, 48, 0.22);
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 11px;
  color: var(--text-muted);
}

.role-tag {
  color: var(--orange-main);
  font-weight: 500;
}

.message-role-pill {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 140, 66, 0.1);
  color: var(--interview-accent-strong);
  font-size: 11px;
  line-height: 1;
}

.thinking-indicator {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 24px;
  color: var(--text-muted);
  animation: thinkingContentIn 0.32s ease-out both;
}

.thinking-text {
  display: inline-flex;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.thinking-motion-unit {
  display: inline-block;
  animation: thinkingUnitWave 2.35s ease-in-out infinite;
  animation-delay: calc(var(--thinking-unit-index) * 0.055s);
}

.thinking-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: 28px;
  flex-shrink: 0;
}

.thinking-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--orange-main);
  opacity: 0.45;
}

@keyframes thinkingBubbleBreath {
  0%,
  100% {
    border-color: rgba(255, 140, 66, 0.22);
    box-shadow: 0 8px 24px rgba(255, 140, 66, 0.08);
  }
  50% {
    border-color: rgba(255, 140, 66, 0.42);
    box-shadow: 0 10px 28px rgba(255, 140, 66, 0.14);
  }
}

@keyframes thinkingBubbleSweep {
  0% {
    transform: translateX(-120%);
  }
  55%,
  100% {
    transform: translateX(120%);
  }
}

@keyframes thinkingContentIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes thinkingUnitWave {
  0%,
  32%,
  100% {
    color: var(--text-muted);
    opacity: 0.72;
    transform: translateY(0);
  }
  16% {
    color: var(--orange-main);
    opacity: 1;
    transform: translateY(-3px);
  }
}

.typing-cursor {
  display: inline-block;
  color: var(--orange-main);
  font-weight: bold;
  animation: blink 0.8s step-end infinite;
  margin-left: 1px;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

@keyframes sessionSurfaceIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes messageFloatIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes voiceCallEnter {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.error-text {
  color: var(--color-danger);
}

.streaming-text,
.done-text {
  display: inline;
  white-space: pre-line;
  word-break: break-word;
}

.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 8px 0;
}

.date-separator-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(243, 216, 199, 0.6), transparent);
}

.date-separator-text {
  font-size: 12px;
  color: var(--text-placeholder);
  background: var(--bg-page);
  padding: 4px 12px;
  border-radius: 12px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: var(--text-muted);
  text-align: center;
}

.empty-icon-wrapper {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fff8f3 0%, #fff0e6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-title);
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.input-area {
  flex-shrink: 0;
  padding: 12px 24px 18px;
  background: var(--bg-page);
}

.input-container {
  max-width: 900px;
  margin: 0 auto;
  background: var(--interview-surface-strong);
  border: 1px solid var(--interview-border);
  border-radius: 16px;
  box-shadow: var(--interview-shadow);
  padding: 16px;
  animation: sessionSurfaceIn 420ms var(--interview-ease) both;
}

.voice-call-panel {
  max-width: 820px;
  margin: 0 auto;
  padding: 14px 18px;
  border: 1px solid var(--interview-border);
  border-radius: 14px;
  background: var(--interview-surface-strong);
  box-shadow: var(--interview-shadow-soft);
  animation: sessionSurfaceIn 360ms var(--interview-ease) both;
}

.voice-call-overlay {
  position: absolute;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 50% 12%, rgba(255, 172, 86, 0.18), transparent 34%),
    var(--interview-bg);
  pointer-events: auto;
  padding: clamp(20px, 5vh, 56px) 24px;
}

.voice-call-window {
  position: relative;
  width: min(560px, calc(100vw - 48px));
  min-height: min(680px, calc(100dvh - 220px));
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
  padding: 18px 40px 36px;
  border: 1px solid rgba(243, 216, 199, 0.78);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 22px 54px rgba(53, 39, 28, 0.18);
  pointer-events: auto;
  animation: voiceCallEnter 460ms var(--interview-ease) both;
}

.voice-call-window::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 238px;
  background: rgba(255, 140, 66, 0.045);
  pointer-events: none;
}

.voice-window-bar {
  position: relative;
  z-index: 1;
  width: 100%;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.voice-window-btn {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-title);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  transition: transform 180ms var(--interview-ease), background-color 180ms var(--interview-ease);
}

.voice-window-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
}

.voice-avatar-wrap {
  position: relative;
  z-index: 1;
  width: clamp(224px, 22vw, 252px);
  height: clamp(224px, 22vw, 252px);
  margin-top: 34px;
  padding: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 34px rgba(80, 52, 36, 0.14);
}

.voice-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.voice-wave {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  margin-top: 32px;
}

.voice-wave span {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #3e3e3e;
}

.voice-wave.active span,
.voice-wave.speaking span {
  animation: voiceWave 1s ease-in-out infinite;
}

.voice-wave.active span:nth-child(2),
.voice-wave.speaking span:nth-child(2) {
  animation-delay: 0.12s;
}

.voice-wave.active span:nth-child(3),
.voice-wave.speaking span:nth-child(3) {
  animation-delay: 0.24s;
}

.voice-call-window .voice-call-title {
  position: relative;
  z-index: 1;
  margin-top: 10px;
  color: var(--text-title);
  font-size: 22px;
  font-weight: 600;
}

.voice-call-window .voice-call-desc {
  position: relative;
  z-index: 1;
  min-height: 48px;
  max-width: 380px;
  text-align: center;
  font-size: 15px;
  line-height: 1.7;
}

.voice-call-window .voice-call-time {
  position: relative;
  z-index: 1;
  margin-top: 14px;
  font-size: 18px;
  color: var(--text-title);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.voice-pending-floating {
  width: 100%;
  max-height: 74px;
  overflow: auto;
}

.voice-dock-actions {
  position: relative;
  z-index: 1;
  width: 100%;
  margin-top: auto;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
  padding-top: 36px;
}

.voice-icon-btn {
  position: relative;
  width: 56px;
  height: 56px;
  flex: 0 0 56px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(214, 200, 190, 0.75);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.86);
  color: #7a7a7a;
  font-size: 24px;
  box-shadow: 0 8px 22px rgba(53, 39, 28, 0.08);
  cursor: pointer;
  transition: transform 180ms var(--interview-ease), box-shadow 180ms var(--interview-ease), background-color 180ms var(--interview-ease), color 180ms var(--interview-ease);
}

.voice-icon-btn :deep(.feature-icon) {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.voice-icon-btn:hover {
  background: rgba(255, 248, 242, 0.96);
  color: var(--text-title);
  transform: translateY(-3px);
  box-shadow: 0 14px 28px rgba(53, 39, 28, 0.12);
}

.voice-icon-btn:active,
.voice-window-btn:active {
  transform: translateY(0) scale(0.96);
}

.voice-icon-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.voice-icon-btn .el-icon {
  font-size: 26px;
}

.button-feature-icon {
  margin-right: 4px;
}

.voice-hangup-btn {
  border-color: #ff5a54;
  background: #ff5a54;
  color: #fff;
  box-shadow: 0 12px 28px rgba(255, 90, 84, 0.28);
}

.voice-hangup-btn:hover {
  background: #ef4444;
  color: #fff;
}

.voice-submit-btn {
  border-color: rgba(103, 194, 58, 0.45);
  color: #4b8f29;
}

.voice-submit-btn:hover {
  background: rgba(103, 194, 58, 0.1);
  color: #3f7d22;
}

.voice-hangup-btn .el-icon {
  font-size: 28px;
  transform: rotate(135deg);
}

.muted-slash {
  position: absolute;
  width: 2px;
  height: 28px;
  border-radius: 999px;
  background: currentColor;
  transform: rotate(45deg);
}

.voice-call-panel-large {
  width: min(680px, 100%);
  margin: 0;
  padding: 28px 32px;
}

.voice-call-status {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.voice-call-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.voice-state-dot {
  width: 12px;
  height: 12px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #c7c7c7;
}

.voice-state-dot.active {
  background: #67c23a;
}

.voice-state-dot.speaking {
  background: #ff8c42;
}

.voice-call-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.voice-call-desc {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-muted);
}

.voice-call-time {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-title);
  font-variant-numeric: tabular-nums;
}

.voice-pending-text {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--orange-light-bg);
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.6;
}

.voice-call-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.voice-call-actions :deep(.el-button) {
  min-width: 106px;
  width: auto;
  height: 36px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  font-weight: 600;
  border-radius: 8px;
  font-size: 13px;
}

.voice-call-panel-collapsed {
  box-shadow: var(--interview-shadow);
}

.input-container :deep(.el-textarea__inner) {
  border: none;
  padding: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-title);
  background: transparent;
  box-shadow: none;
}

.input-container :deep(.el-textarea__inner::placeholder) {
  color: var(--text-placeholder);
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(243, 216, 199, 0.3);
}

.input-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
  white-space: nowrap;
}

.input-hint kbd {
  display: inline-block;
  padding: 2px 6px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-card);
  border-radius: 4px;
  font-size: 11px;
}

.send-btn {
  border-radius: 20px;
  padding: 8px 20px;
  transition: transform 180ms var(--interview-ease), box-shadow 180ms var(--interview-ease);
}

.send-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(255, 128, 48, 0.2);
}

.ended-notice {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  font-size: 14px;
  color: var(--text-muted);
  background: rgba(255, 255, 255, 0.92);
  border-top: 1px solid rgba(243, 216, 199, 0.3);
}

.ended-notice .el-icon {
  color: #67c23a;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mic-btn {
  transition: transform 180ms var(--interview-ease), box-shadow 180ms var(--interview-ease), background-color 180ms var(--interview-ease);
}

.mic-btn.is-recording {
  animation: mic-pulse 1.2s ease-in-out infinite;
  box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.6);
}

:global(html[data-theme="dark"] .mic-btn.is-recording) {
  box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4);
}

@keyframes mic-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.6);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(245, 108, 108, 0);
  }
}

@keyframes voiceWave {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.55;
  }
  50% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

@media (max-width: 1023px) {
  .session-status-bar {
    padding: 12px 16px;
  }

  .input-area {
    padding: 12px 16px 24px;
  }

  .chat-container {
    padding: 16px;
  }

  .voice-call-overlay {
    top: 0;
    padding: 16px;
  }
}

@media (max-width: 767px) {
  .chat-messages {
    padding-top: 18px;
  }

  /* 移动端导航栏：单行紧凑布局，所有元素水平排列以节省竖向空间 */
  .session-status-bar {
    padding: 10px 12px;
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }

  /* 左侧：标题 + 难度徽章，占据剩余空间并溢出截断 */
  .status-bar-left {
    flex: 1 1 auto;
    min-width: 0;
    gap: 6px;
    overflow: hidden;
  }

  /* 右侧：状态点 + 返回 + 结束按钮，紧凑分组 */
  .status-bar-right {
    flex: 0 0 auto;
    width: auto;
    gap: 4px;
  }

  .interview-title {
    flex: 0 1 auto;
    min-width: 0;
    max-width: 100%;
    font-size: 13px;
  }

  .difficulty-badge {
    flex: 0 0 auto;
    font-size: 11px;
    padding: 2px 8px;
  }

  /* 移动端隐藏次要信息以节省空间 */
  .status-divider,
  .mode-text,
  .feedback-mode-text,
  .interaction-mode-text {
    display: none;
  }

  /* 状态指示：仅保留圆点，隐藏文字 */
  .status-indicator {
    flex: 0 0 auto;
    gap: 0;
  }

  .status-indicator .status-text {
    display: none;
  }

  /* 返回按钮：32x32 紧凑图标 */
  .back-btn {
    flex: 0 0 auto;
    width: 32px;
    height: 32px;
    padding: 0;
    margin: 0;
  }

  /* 结束按钮：圆形图标按钮，仅显示挂断 SVG */
  .end-btn {
    flex: 0 0 auto;
    width: 32px;
    height: 32px;
    min-width: 0;
    padding: 0;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .end-btn .end-btn-text {
    display: none;
  }

  .end-btn .end-btn-svg {
    display: block;
    width: 16px;
    height: 16px;
  }

  /* 查看报告按钮：移动端缩小尺寸 */
  .report-btn {
    flex: 0 0 auto;
    height: 32px;
    padding: 0 10px;
    font-size: 12px;
  }

  .message-content {
    max-width: 84%;
  }

  .thinking-bubble {
    min-width: min(246px, calc(100vw - 112px));
  }

  .thinking-indicator {
    max-width: 100%;
    gap: 8px;
  }

  .thinking-text {
    flex-wrap: wrap;
    white-space: normal;
  }

  .chat-container {
    padding: 12px;
    gap: 20px;
  }

  .voice-chat-stage .chat-container,
  .voice-call-collapsed-stage .chat-container {
    padding-bottom: 12px;
  }

  .input-area {
    padding: 12px;
  }

  .ended-notice {
    padding: 12px;
  }

  .input-container {
    padding: 12px;
    border-radius: 12px;
  }

  .voice-call-panel {
    padding: 12px 14px;
  }

  .voice-call-overlay {
    top: 0;
    align-items: flex-end;
    padding: 0;
    background: var(--interview-bg);
  }

  .voice-call-window {
    width: 100%;
    min-height: 100%;
    height: 100%;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    overflow-y: auto;
    padding: calc(20px + env(safe-area-inset-top)) 24px calc(28px + env(safe-area-inset-bottom));
  }

  .voice-window-bar {
    display: none;
  }

  .voice-call-window::before {
    height: 232px;
  }

  .voice-avatar-wrap {
    width: min(224px, 62vw);
    height: min(224px, 62vw);
    margin-top: 8px;
  }

  .voice-wave {
    margin-top: 28px;
  }

  .voice-call-window .voice-call-title {
    font-size: 22px;
  }

  .voice-call-window .voice-call-desc {
    max-width: 320px;
  }

  .voice-dock-actions {
    justify-content: space-between;
    gap: 14px;
    padding: 32px max(8px, env(safe-area-inset-left)) 0 max(8px, env(safe-area-inset-right));
  }

  .voice-icon-btn {
    width: 56px;
    height: 56px;
    flex-basis: 56px;
  }

  .voice-icon-btn :deep(.feature-icon) {
    width: 30px;
    height: 30px;
  }

  .voice-session-stage {
    align-items: flex-end;
    padding: 16px 12px 24px;
  }

  .voice-call-panel-large {
    padding: 16px;
  }

  .voice-call-status {
    align-items: center;
    flex-direction: row;
  }

  .voice-call-actions :deep(.el-button) {
    width: 42px;
    min-width: 42px;
    height: 42px;
    padding: 0;
    border-radius: 50%;
    font-size: 0;
  }

  .voice-call-actions :deep(.el-button .el-icon) {
    margin-right: 0;
    font-size: 18px;
  }

  .voice-call-actions {
    justify-content: center;
    flex-wrap: nowrap;
    gap: 10px;
    margin-top: 12px;
  }

  @media (max-height: 720px) {
    .voice-avatar-wrap {
      width: 188px;
      height: 188px;
      margin-top: 14px;
    }

    .voice-wave {
      margin-top: 18px;
    }

    .voice-dock-actions {
      padding-top: 20px;
    }
  }

  .input-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .input-hint {
    display: none;
  }

  .send-btn {
    width: 100%;
  }
}

:global(html[data-theme="dark"] .interview-session-view) {
  --interview-bg: #16110e;
  --interview-bg-soft: #201713;
  --interview-surface: rgba(38, 29, 24, 0.92);
  --interview-surface-strong: rgba(34, 25, 21, 0.98);
  --interview-border: rgba(255, 173, 104, 0.2);
  --interview-border-strong: rgba(255, 173, 104, 0.34);
  --interview-shadow: 0 18px 48px rgba(0, 0, 0, 0.36);
  --interview-shadow-soft: 0 8px 26px rgba(0, 0, 0, 0.28);
  --interview-ink: #f7e9dc;
  --interview-muted: #c9a994;
  background:
    radial-gradient(circle at 12% 8%, rgba(255, 140, 66, 0.16), transparent 30%),
    linear-gradient(180deg, var(--interview-bg-soft) 0%, var(--interview-bg) 54%, var(--bg-page) 100%);
}

:global(html[data-theme="dark"] .session-status-bar) {
  background: var(--interview-surface-strong);
  border-bottom-color: var(--interview-border);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.25);
}

:global(html[data-theme="dark"] .difficulty-badge.difficulty-1) {
  background: rgba(76, 175, 80, 0.15);
  color: #81c784;
}

:global(html[data-theme="dark"] .difficulty-badge.difficulty-2) {
  background: rgba(255, 152, 0, 0.15);
  color: #ffb74d;
}

:global(html[data-theme="dark"] .difficulty-badge.difficulty-3) {
  background: rgba(244, 67, 54, 0.15);
  color: #ef9a9a;
}

:global(html[data-theme="dark"] .status-dot) {
  background: #81c784;
}

:global(html[data-theme="dark"] .status-indicator.ended .status-dot) {
  background: var(--text-muted);
}

:global(html[data-theme="dark"] .job-target-banner) {
  background: var(--interview-surface-strong);
  border-bottom: 1px solid var(--interview-border);
}

:global(html[data-theme="dark"] .job-target-banner-title) {
  color: #FFB877;
}

:global(html[data-theme="dark"] .job-target-banner-desc) {
  color: var(--text-muted);
}

:global(html[data-theme="dark"] .empty-icon-wrapper) {
  background: linear-gradient(135deg, var(--orange-light-bg) 0%, rgba(255, 140, 66, 0.06) 100%);
}

:global(html[data-theme="dark"] .assistant-bubble),
:global(html[data-theme="dark"] .input-container),
:global(html[data-theme="dark"] .voice-call-panel) {
  background: var(--interview-surface-strong);
  border-color: var(--interview-border);
}

:global(html[data-theme="dark"] .message-avatar) {
  background: var(--interview-surface);
  border-color: var(--interview-border);
}

:global(html[data-theme="dark"] .date-separator-text) {
  background: var(--interview-bg);
}

:global(html[data-theme="dark"] .ended-notice) {
  background: rgba(22, 17, 14, 0.86);
  border-top-color: var(--interview-border);
}

:global(html[data-theme="dark"] .ended-notice .el-icon) {
  color: var(--color-success);
}

:global(html[data-theme="dark"] .voice-call-window) {
  background: var(--interview-surface-strong);
  border-color: var(--interview-border);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.38);
}

:global(html[data-theme="dark"] .voice-call-overlay) {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 140, 66, 0.14), transparent 34%),
    var(--interview-bg);
}

:global(html[data-theme="dark"] .voice-call-window::before) {
  background: rgba(255, 140, 66, 0.1);
}

:global(html[data-theme="dark"] .voice-window-btn:hover),
:global(html[data-theme="dark"] .voice-icon-btn:hover) {
  background: rgba(255, 255, 255, 0.08);
}

:global(html[data-theme="dark"] .voice-icon-btn) {
  border-color: var(--interview-border);
  background: rgba(48, 48, 48, 0.92);
  color: #f7e9dc;
}

:global(html[data-theme="dark"] .voice-hangup-btn) {
  border-color: #ff5a54;
  background: #ff5a54;
}

:global(html[data-theme="dark"] .voice-wave span) {
  background: #d8d8d8;
}

:global(html[data-theme="dark"] .message-feedback-card) {
  background: rgba(255, 140, 66, 0.06);
  border-color: rgba(255, 184, 119, 0.2);
}

:global(html[data-theme="dark"] .thinking-bubble) {
  border-color: rgba(255, 184, 119, 0.22);
  background: linear-gradient(135deg, rgba(255, 140, 66, 0.08) 0%, var(--bg-card) 100%);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.22);
}

:global(html[data-theme="dark"] .thinking-bubble::after) {
  background: linear-gradient(
    110deg,
    transparent 0%,
    transparent 35%,
    rgba(255, 184, 119, 0.08) 48%,
    transparent 62%,
    transparent 100%
  );
}

:global(html[data-theme="dark"] .thinking-dot) {
  background: #ffb877;
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }

  .session-status-bar,
  .conversation-surface,
  .input-container,
  .voice-call-panel,
  .voice-call-window,
  .message-entrance {
    animation: none;
  }

  /* sessionSurfaceIn, messageFloatIn, voiceCallEnter */
  .thinking-bubble,
  .thinking-bubble::after,
  .thinking-indicator,
  .thinking-motion-unit,
  .thinking-char,
  .thinking-dot,
  .voice-wave span {
    animation: none;
  }

  .thinking-bubble::after {
    display: none;
  }

  .thinking-dot {
    opacity: 0.72;
    transform: none;
  }

  .thinking-char {
    color: var(--text-muted);
    opacity: 1;
    transform: none;
  }
}
</style>
