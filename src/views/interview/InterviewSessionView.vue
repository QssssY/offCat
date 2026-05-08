<template>
  <div class="interview-session-view">
    <div class="session-status-bar">
      <div class="status-bar-left">
        <span class="interview-title">模拟面试 · {{ sessionData?.jobRole || "加载中" }}</span>
        <span class="status-divider">|</span>
        <span class="difficulty-badge" :class="`difficulty-${sessionData?.difficulty || 1}`">
          {{ difficultyText }}
        </span>
        <span class="mode-text">{{ modeText }}</span>
      </div>
      <div class="status-bar-right">
        <span class="status-indicator" :class="{ ended: isEnded }">
          <span class="status-dot"></span>
          {{ sessionStatusText }}
        </span>
        <el-button link class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <el-button
          v-if="isInProgress"
          type="danger"
          plain
          size="small"
          class="end-btn"
          @click="endInterview"
        >
          结束面试
        </el-button>
        <el-button
          v-else-if="isEnded"
          type="primary"
          size="small"
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
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
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

    <div v-else class="session-content">
      <div class="chat-stage">
        <div class="chat-container">
          <div class="chat-messages" ref="chatContainer">
            <!-- 开场白加载中 -->
            <div v-if="openingPending" class="opening-pending">
              <div class="opening-loading">
                <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
                <div class="loading-text">AI 面试官正在准备中...</div>
                <div class="loading-hint">首次生成可能需要 1-2 分钟，请稍候</div>
              </div>
            </div>

            <template v-else-if="groupedChatLogs.length > 0">
              <!-- 原有聊天记录代码 -->
              <template v-for="item in groupedChatLogs" :key="item.id || item.tempId || item.date">
                <!-- 原有代码保持不变 -->
                <div v-if="item.type === 'date-separator'" class="date-separator">
                  <span class="date-separator-line"></span>
                  <span class="date-separator-text">{{ formatDateSeparator(item.date) }}</span>
                  <span class="date-separator-line"></span>
                </div>

                <div v-else-if="item.messageRole === 'assistant'" class="message-row assistant-row">
                  <div class="message-avatar assistant-avatar">
                    <img :src="assistantAvatar" alt="AI面试官" @error="handleImageError" />
                  </div>
                  <div class="message-content">
                    <div class="message-bubble assistant-bubble">
                      <span v-if="item.status === 'thinking'" class="thinking-indicator">
                        <span class="thinking-text">思考中</span><span class="thinking-dots">...</span>
                      </span>
                      <span v-else-if="item.status === 'error'" class="error-text">回复失败，请重试</span>
                      <span v-else-if="item.status === 'streaming'" class="streaming-text">
                        {{ item.displayContent }}<span class="typing-cursor">|</span>
                      </span>
                      <span v-else class="done-text">{{ item.content || "" }}</span>
                    </div>
                    <div class="message-meta assistant-meta">
                      <span class="role-tag">面试官</span>
                      <span class="time-tag">{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>

                <div v-else class="message-row user-row">
                  <div class="message-avatar user-avatar">
                    <img :src="userAvatar" alt="用户" @error="handleImageError" />
                  </div>
                  <div class="message-content">
                    <div class="message-bubble user-bubble">{{ item.content }}</div>
                    <div class="message-meta user-meta">
                      <span class="time-tag">{{ formatTime(item.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </template>

            <div v-else class="empty-chat">
              <div class="empty-icon-wrapper">
                <el-icon :size="56" color="#F3D8C7"><ChatDotSquare /></el-icon>
              </div>
              <p class="empty-title">等待开始</p>
              <p class="empty-desc">在下方输入你的回答，AI 面试官会继续追问。</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isInProgress" class="input-area">
      <div class="input-container">
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
          <el-button
            type="primary"
            :loading="sending"
            :disabled="!inputMessage.trim()"
            class="send-btn"
            @click="sendMessage"
          >
            发送回答
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="isEnded" class="ended-notice">
      <el-icon><CircleCheckFilled /></el-icon>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowLeft,
  ChatDotSquare,
  Loading,
  CircleCheckFilled,
} from "@element-plus/icons-vue";
import {
  endInterview as apiEndInterview,
  getInterviewSession,
  streamInterviewMessage,
} from "@/api/interview";
import { ElMessage } from "element-plus";
import { getToken } from "@/utils/auth";

import assistantAvatarImg from "@/assets/assistant.png";
import userAvatarImg from "@/assets/user.png";

const router = useRouter();
const route = useRoute();

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
let openingPollingTimer = null;

const assistantAvatar = assistantAvatarImg;
const userAvatar = userAvatarImg;

const isInProgress = computed(() => sessionData.value?.status === 0);
const isEnded = computed(() => sessionData.value?.status === 1);

const sessionStatusText = computed(() => {
  if (!sessionData.value) return "加载中";
  return sessionData.value.status === 0 ? "进行中" : "已结束";
});

const difficultyText = computed(() => {
  const map = { 1: "初级", 2: "中级", 3: "高级" };
  return map[sessionData.value?.difficulty] || "初级";
});

const modeText = computed(() => {
  if (sessionData.value?.interviewModeDesc) {
    return sessionData.value.interviewModeDesc;
  }
  if (sessionData.value?.jobTargeted || sessionData.value?.interviewMode === "job_targeted") {
    return "岗位定向模拟";
  }
  return sessionData.value?.interviewMode === "stress" ? "压力面试" : "普通面试";
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
        return;
      }
    } catch (err) {
      console.warn("轮询开场白状态失败:", err.message);
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
    }
  }, TYPE_INTERVAL_MS);
};

const sendMessage = async () => {
  const content = inputMessage.value.trim();
  if (!content || !sessionId.value || !sessionData.value) {
    return;
  }

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

  // SSE 流断线重连：最多重试 3 次，保留已累积的文本内容
  const MAX_RECONNECT = 3;
  let reconnectAttempt = 0;
  let streamSucceeded = false;

  startTypingMachine(tempMsgId);

  const applyStreamPayload = (payload) => {
    const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
    if (msgIndex === -1) return;
    const msg = sessionData.value.chatLogs[msgIndex];

    if (payload.type === "content") {
      const normalizedData = (payload.content || "").replace(/\r/g, "");
      if (normalizedData) {
        msg.status = "streaming";
        msg.rawContent += normalizedData;
        msg.pendingContent += normalizedData;
      }
      return;
    }
    if (payload.type === "done") {
      msg.streamFinished = true;
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
      try {
        applyFn(JSON.parse(jsonStr));
      } catch (parseError) {
        console.warn("[interview-stream] SSE parse failed:", jsonStr, parseError);
      }
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
    while (reconnectAttempt <= MAX_RECONNECT) {
      try {
        const token = getToken();
        const response = await streamInterviewMessage(
          sessionId.value,
          { sessionId: sessionId.value, content },
          token
        );

        if (!response.ok) {
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

        // 流正常结束
        streamSucceeded = true;
        const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
        if (msgIndex !== -1) {
          sessionData.value.chatLogs[msgIndex].streamFinished = true;
        }
        break; // 成功，退出重连循环
      } catch (streamErr) {
        reconnectAttempt++;
        const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
        const msg = msgIndex !== -1 ? sessionData.value.chatLogs[msgIndex] : null;

        // 如果已完成（done事件已收到）或已达最大重试次数，直接抛出
        if (msg?.streamFinished || reconnectAttempt > MAX_RECONNECT) {
          throw streamErr;
        }

        // 重连提示：在消息气泡旁显示状态，然后重置已累积的内容防止重复
        if (msg) {
          msg.status = "streaming";
          msg.displayContent = "⚠ 网络中断，正在重连...";
          // 清除旧的部分内容，重连后服务器会重新发送完整回复
          msg.rawContent = "";
          msg.pendingContent = "";
        }
        console.warn(`[interview-stream] SSE 断流，第 ${reconnectAttempt} 次重连...`);
        await new Promise((r) => setTimeout(r, 1000));
      }
    }
  } catch (err) {
    ElMessage.error(err.message || "发送消息失败，请稍后重试");
    const msgIndex = sessionData.value.chatLogs.findIndex((item) => item.id === tempMsgId);
    if (msgIndex !== -1) {
      sessionData.value.chatLogs[msgIndex].streamFinished = true;
      if (!streamSucceeded) {
        sessionData.value.chatLogs[msgIndex].status = "error";
      }
    }
  } finally {
    sending.value = false;
  }
};

const endInterview = () => {
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
  stopTypingMachine();
  stopOpeningPolling();
});
</script>

<style scoped>
.interview-session-view {
  height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(243, 216, 199, 0.5);
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
}

.status-bar-left,
.status-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.interview-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
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
.status-indicator {
  font-size: 13px;
  color: var(--text-body);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
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

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
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

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 14px;
  color: var(--text-body);
}

.session-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.chat-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-container {
  flex: 1;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 16px 8px 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 0;
}

.message-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.user-row {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--bg-card);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  max-width: 75%;
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
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-line;
}

.assistant-bubble {
  background: var(--bg-card);
  border: 1px solid rgba(243, 216, 199, 0.6);
  color: var(--text-title);
  border-top-left-radius: 4px;
}

.user-bubble {
  background: linear-gradient(135deg, #ff8c42 0%, #ff7a30 100%);
  color: var(--bg-card);
  border-top-right-radius: 4px;
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

.thinking-indicator {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--text-muted);
  font-style: italic;
}

.thinking-dots {
  animation: thinkingPulse 1.2s ease-in-out infinite;
}

@keyframes thinkingPulse {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
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
  padding: 16px 24px 24px;
  background: var(--bg-page);
}

.input-container {
  max-width: 900px;
  margin: 0 auto;
  background: var(--bg-card);
  border: 1px solid rgba(243, 216, 199, 0.6);
  border-radius: 16px;
  box-shadow: 0 -4px 24px rgba(255, 140, 66, 0.1), 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 16px;
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
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(243, 216, 199, 0.3);
}

.input-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.input-hint kbd {
  display: inline-block;
  padding: 2px 6px;
  background: var(--bg-elevated);
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 11px;
}

.send-btn {
  border-radius: 20px;
  padding: 8px 20px;
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
}

@media (max-width: 767px) {
  .session-status-bar {
    padding: 12px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .mode-text {
    display: none;
  }

  .message-content {
    max-width: 84%;
  }

  .chat-container {
    padding: 12px;
    gap: 20px;
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
}

/* ===== 暗色模式适配 ===== */
.difficulty-badge.difficulty-1 {
  background: rgba(76, 175, 80, 0.15);
  color: #81c784;
}

.difficulty-badge.difficulty-2 {
  background: rgba(255, 152, 0, 0.15);
  color: #ffb74d;
}

.difficulty-badge.difficulty-3 {
  background: rgba(244, 67, 54, 0.15);
  color: #ef9a9a;
}

.status-dot {
  background: #81c784;
}

.status-indicator.ended .status-dot {
  background: var(--text-muted);
}
</style>
