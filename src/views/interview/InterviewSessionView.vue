<template>
  <div class="interview-session-view">
    <!-- 顶部面试状态栏（sticky） -->
    <div class="session-status-bar">
      <div class="status-bar-left">
        <span class="interview-title"
          >模拟面试 · {{ sessionData?.jobRole || "加载中" }}</span
        >
        <span class="status-divider">|</span>
        <span
          class="difficulty-badge"
          :class="`difficulty-${sessionData?.difficulty || 1}`"
        >
          {{ difficultyText }}
        </span>
        <span class="mode-text">{{ modeText }}</span>
      </div>
      <div class="status-bar-right">
        <span class="status-indicator" :class="{ ended: isEnded }">
          <span class="status-dot"></span>
          {{ sessionStatusText }}
        </span>
        <el-button link @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <el-button
          v-if="isInProgress"
          type="danger"
          plain
          size="small"
          @click="endInterview"
          class="end-btn"
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

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="loading-content">
        <el-icon class="loading-icon" :size="48"><Loading /></el-icon>
        <div class="loading-text">加载面试会话...</div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-section">
      <el-result icon="error" title="加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="fetchSessionDetail">重试</el-button>
          <el-button @click="goBack">返回</el-button>
        </template>
      </el-result>
    </div>

    <!-- 会话内容区 -->
    <div v-else class="session-content">
      <!-- 对话主舞台 -->
      <div class="chat-stage">
        <div class="chat-container">
          <div class="chat-messages" ref="chatContainer" :key="sessionId">
            <template v-if="groupedChatLogs.length > 0">
              <template
                v-for="item in groupedChatLogs"
              >
                <!-- 日期分割线 -->
                <div
                  v-if="item.type === 'date-separator'"
                  class="date-separator"
                  :key="'date-' + item.date"
                >
                  <span class="date-separator-line"></span>
                  <span class="date-separator-text">{{
                    formatDateSeparator(item.date)
                  }}</span>
                  <span class="date-separator-line"></span>
                </div>

                <!-- AI 面试官消息 - 左对齐 -->
                <div
                  v-else-if="item.messageRole === 'assistant'"
                  class="message-row assistant-row"
                  :key="item.id || item.tempId || `msg-${item.createTime}`"
                >
                  <div class="message-avatar assistant-avatar">
                    <img
                      :src="assistantAvatar"
                      alt="AI面试官"
                      @error="handleImageError"
                    />
                  </div>
                  <div class="message-content">
                    <div class="message-bubble assistant-bubble">
                      <span
                        v-if="item.status === 'thinking'"
                        class="thinking-indicator"
                      >
                        <span class="thinking-text">思考中</span
                        ><span class="thinking-dots">...</span>
                      </span>
                      <span
                        v-else-if="item.status === 'error'"
                        class="error-text"
                        >回复失败，请重试</span
                      >
                      <span
                        v-else-if="item.status === 'streaming'"
                        class="streaming-text"
                        >{{ item.displayContent
                        }}<span class="typing-cursor">|</span></span
                      >
                      <span v-else class="done-text">{{
                        item.content || ""
                      }}</span>
                    </div>
                    <div class="message-meta assistant-meta">
                      <span class="role-tag">面试官</span>
                      <span class="time-tag">{{
                        formatTime(item.createTime)
                      }}</span>
                    </div>
                  </div>
                </div>

                <!-- 用户消息 - 右对齐 -->
                <div v-else class="message-row user-row" :key="'user-' + (item.id || item.tempId || item.createTime)">
                  <div class="message-avatar user-avatar">
                    <img
                      :src="userAvatar"
                      alt="用户"
                      @error="handleImageError"
                    />
                  </div>
                  <div class="message-content">
                    <div class="message-bubble user-bubble">
                      {{ item.content }}
                    </div>
                    <div class="message-meta user-meta">
                      <span class="time-tag">{{
                        formatTime(item.createTime)
                      }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </template>

            <!-- 空对话状态 -->
            <div v-else class="empty-chat">
              <div class="empty-icon-wrapper">
                <el-icon :size="56" color="#F3D8C7"><ChatDotSquare /></el-icon>
              </div>
              <p class="empty-title">等待开始</p>
              <p class="empty-desc">在下方输入你的回答，AI 面试官将进行追问</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="input-area" v-if="isInProgress">
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="输入你的回答，AI 面试官将进行追问..."
            resize="none"
            @keyup.enter.ctrl="sendMessage"
          />
          <div class="input-footer">
            <span class="input-hint">
              <kbd>Ctrl</kbd> + <kbd>Enter</kbd> 发送
            </span>
            <el-button
              type="primary"
              :loading="sending"
              :disabled="!inputMessage.trim()"
              @click="sendMessage"
              class="send-btn"
            >
              发送回答
            </el-button>
          </div>
        </div>
      </div>

      <!-- 面试结束提示 -->
      <div v-if="isEnded" class="ended-notice">
        <el-icon><CircleCheckFilled /></el-icon>
        <span>面试已结束，你可以查看或返回</span>
      </div>
    </div>

    <!-- 结束面试确认对话框 -->
    <el-dialog v-model="showEndDialog" title="结束面试" width="400px">
      <p>确定要结束本次面试吗？结束后将无法继续回答。</p>
      <template #footer>
        <el-button @click="showEndDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="ending"
          @click="confirmEndInterview"
        >
          确认结束
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  ArrowLeft,
  User,
  Service,
  ChatDotSquare,
  Loading,
  CircleCheckFilled,
} from "@element-plus/icons-vue";
import {
  getInterviewSession,
  sendInterviewMessage,
  endInterview as apiEndInterview,
  streamInterviewMessage,
} from "@/api/interview";
import { ElMessage, ElMessageBox } from "element-plus";
import { getToken } from "@/utils/auth";

import assistantAvatarImg from "@/assets/assistant.png";
import userAvatarImg from "@/assets/user.png";

const router = useRouter();
const route = useRoute();

const sessionId = computed(() => route.params.sessionId);

const loading = ref(true);
const error = ref("");
const sessionData = ref(null);
const inputMessage = ref("");
const sending = ref(false);
const ending = ref(false);
const showEndDialog = ref(false);
const streamingContent = ref("");
const isStreaming = ref(false);
const pendingAssistantMsgId = ref(null);

const assistantAvatar = assistantAvatarImg;
const userAvatar = userAvatarImg;

const modeFromQuery = computed(() => route.query.mode || "normal");

const isInProgress = computed(() => sessionData.value?.status === 0);
const isEnded = computed(() => sessionData.value?.status === 1);

const sessionStatusText = computed(() => {
  if (!sessionData.value) return "加载中";
  return sessionData.value.status === 0 ? "进行中" : "已结束";
});

const sessionStatusType = computed(() => {
  if (!sessionData.value) return "info";
  return sessionData.value.status === 0 ? "success" : "info";
});

const difficultyText = computed(() => {
  const map = { 1: "初级", 2: "中级", 3: "高级" };
  return map[sessionData.value?.difficulty] || "初级";
});

const difficultyType = computed(() => {
  const map = { 1: "success", 2: "warning", 3: "danger" };
  return map[sessionData.value?.difficulty] || "info";
});

const modeText = computed(() => {
  if (sessionData.value?.interviewModeDesc) {
    return sessionData.value.interviewModeDesc;
  }
  if (sessionData.value?.interviewMode === "stress") {
    return "压力面试";
  }
  if (sessionData.value?.interviewMode === "normal") {
    return "普通面试";
  }
  return "普通面试";
});

const chatLogs = computed(() => {
  return sessionData.value?.chatLogs || [];
});

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

const formatDateSeparator = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  if (isNaN(date.getTime())) return "";
  const today = new Date().toDateString();
  const yesterday = new Date(Date.now() - 86400000).toDateString();
  if (date.toDateString() === today) return "今天";
  if (date.toDateString() === yesterday) return "昨天";
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}年${pad(date.getMonth() + 1)}月${pad(
    date.getDate()
  )}日`;
};

const handleImageError = (e) => {
  console.warn("头像加载失败:", e.target.src);
  e.target.style.display = "none";
  e.target.parentElement.classList.add("avatar-fallback");
};

const fetchSessionDetail = async () => {
  if (!sessionId.value) {
    error.value = "会话ID不存在";
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";

  try {
    const res = await getInterviewSession(sessionId.value);

    if (res.data?.chatLogs && Array.isArray(res.data.chatLogs)) {
      res.data.chatLogs = res.data.chatLogs.map((msg) => ({
        ...msg,
        displayContent: msg.content || "",
        pendingContent: "",
        status: msg.status || "done",
        rawContent: msg.content || "",
      }));
    }

    sessionData.value = res.data;
    loading.value = false;
  } catch (err) {
    console.error("获取会话详情失败:", err);
    error.value = err.message || "获取会话详情失败，请稍后重试";
    loading.value = false;
  }
};

let typingTimer = null;
const TYPE_INTERVAL_MS = 35;

const sendMessage = async () => {
  const content = inputMessage.value.trim();
  if (!content || !sessionId.value) return;

  if (typingTimer) {
    clearInterval(typingTimer);
    typingTimer = null;
  }

  sending.value = true;
  isStreaming.value = true;

  const tempMsgId = `temp-${Date.now()}`;
  const assistantMsg = {
    id: tempMsgId,
    messageRole: "assistant",
    content: "",
    displayContent: "",
    pendingContent: "",
    rawContent: "",
    status: "thinking",
    streamFinished: false,
    createTime: new Date().toISOString(),
  };

  sessionData.value.chatLogs = [
    ...(sessionData.value.chatLogs || []),
    {
      id: `user-${Date.now()}`,
      messageRole: "user",
      content: content,
      createTime: new Date().toISOString(),
    },
    assistantMsg,
  ];

  inputMessage.value = "";
  await nextTick();
  scrollToBottom();

  const startTypingMachine = () => {
    if (typingTimer) clearInterval(typingTimer);
    typingTimer = setInterval(() => {
      const logs = sessionData.value?.chatLogs || [];
      const msgIndex = logs.findIndex((m) => m.id === tempMsgId);
      if (msgIndex === -1) {
        clearInterval(typingTimer);
        typingTimer = null;
        return;
      }
      const msg = logs[msgIndex];
      if (msg.pendingContent.length > 0) {
        msg.displayContent += msg.pendingContent[0];
        msg.pendingContent = msg.pendingContent.substring(1);
        msg.status = "streaming";
        nextTick(() => scrollToBottom());
      } else if (msg.streamFinished && msg.pendingContent.length === 0) {
        clearInterval(typingTimer);
        typingTimer = null;
        if (msg.status !== "error") {
          msg.content = msg.rawContent;
          msg.status = "done";
        } else {
          msg.status = "error";
        }
      }
    }, TYPE_INTERVAL_MS);
  };

  let streamSucceeded = false;
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
      } catch {}
      throw new Error(errMsg);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const text = decoder.decode(value, { stream: true });
      const lines = text.split("\n");

      for (const line of lines) {
        if (line.startsWith("event:")) {
          continue;
        } else if (line.startsWith("data:")) {
          const jsonStr = line.substring("data:".length).trim();
          if (!jsonStr) continue;

          let payload;
          try {
            payload = JSON.parse(jsonStr);
          } catch (e) {
            console.warn("[stream] JSON解析失败:", jsonStr, e);
            continue;
          }

          if (payload.type === "content") {
            const data = payload.content || "";
            if (!data) continue;

            const msgIndex = sessionData.value.chatLogs.findIndex(
              (m) => m.id === tempMsgId
            );
            if (msgIndex !== -1) {
              const msg = sessionData.value.chatLogs[msgIndex];
              const normalizedData = data.replace(/\r/g, "");
              const visibleContent = normalizedData.replace(/[\s\u00A0]/g, "");
              if (visibleContent === "") {
                console.debug(
                  "[stream] 跳过纯空白chunk:",
                  JSON.stringify(normalizedData)
                );
              } else {
                if (msg.status === "thinking") {
                  msg.status = "streaming";
                  startTypingMachine();
                }
                msg.rawContent += normalizedData;
                msg.pendingContent += normalizedData;
              }
            }
            await nextTick();
            scrollToBottom();
          } else if (payload.type === "done") {
            console.debug("[stream] 收到done事件");
            const msgIndex = sessionData.value.chatLogs.findIndex(
              (m) => m.id === tempMsgId
            );
            if (msgIndex !== -1) {
              const msg = sessionData.value.chatLogs[msgIndex];
              msg.streamFinished = true;
            }
            break;
          } else if (payload.type === "error") {
            throw new Error(payload.message || "AI 回复失败");
          }
        }
      }
    }

    streamSucceeded = true;
    const msgIndex = sessionData.value.chatLogs.findIndex(
      (m) => m.id === tempMsgId
    );
    if (msgIndex !== -1) {
      const msg = sessionData.value.chatLogs[msgIndex];
      msg.streamFinished = true;
    }
  } catch (err) {
    console.error("流式消息失败:", err);
    ElMessage.error(err.message || "发送消息失败，请稍后重试");
    if (!streamSucceeded) {
      const msgIndex = sessionData.value.chatLogs.findIndex(
        (m) => m.id === tempMsgId
      );
      if (msgIndex !== -1) {
        const msg = sessionData.value.chatLogs[msgIndex];
        msg.status = "error";
        msg.streamFinished = true;
      }
      await nextTick();
    }
  } finally {
    sending.value = false;
    isStreaming.value = false;
    pendingAssistantMsgId.value = null;
    streamingContent.value = "";
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    const container = document.querySelector(".chat-messages");
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
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
    // 访问会话详情以刷新状态
    await fetchSessionDetail();
    // 提示用户报告会稍后生成
    ElMessage.info({
      message: "评价报告正在生成，请稍后在报告页查看",
      duration: 3000
    });
  } catch (err) {
    console.error("结束面试失败:", err);
    ElMessage.error(err.message || "结束面试失败，请稍后重试");
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

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  if (isNaN(date.getTime())) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
    date.getSeconds()
  )}`;
};

onMounted(() => {
  fetchSessionDetail();
});
</script>

<style scoped>
.interview-session-view {
  height: 100vh;
  background: #f8f6f3;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ============================================
   顶部面试状态栏
   ============================================ */
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

.status-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.interview-title {
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.status-divider {
  color: #f3d8c7;
  font-weight: 300;
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

.mode-text {
  font-size: 13px;
  color: #666666;
}

.status-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666666;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  animation: pulse 2s ease-in-out infinite;
}

.status-indicator.ended .status-dot {
  background: #909399;
  animation: none;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.9);
  }
}

.back-btn {
  color: #666;
}

.end-btn {
  border-radius: 8px;
}

/* ============================================
   加载和错误状态
   ============================================ */
.loading-section,
.error-section {
  flex: 1;
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
  color: #ff8c42;
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
  color: #666;
}

/* ============================================
   会话内容区
   ============================================ */
.session-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* ============================================
   对话主舞台
   ============================================ */
.chat-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-container {
  flex: 1;
  max-width: 800px;
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
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 0;
}

/* ============================================
   消息行
   ============================================ */
.message-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.assistant-row {
  flex-direction: row;
}

.user-row {
  flex-direction: row-reverse;
}

/* ============================================
   消息头像
   ============================================ */
.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ============================================
   消息内容
   ============================================ */
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

/* ============================================
   消息气泡
   ============================================ */
.message-bubble {
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.assistant-bubble {
  background: #ffffff;
  border: 1px solid rgba(243, 216, 199, 0.6);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  color: #2f2f2f;
  border-top-left-radius: 4px;
}

.user-bubble {
  background: linear-gradient(135deg, #ff8c42 0%, #ff7a30 100%);
  box-shadow: 0 4px 16px rgba(255, 140, 66, 0.25);
  color: #fff;
  border-top-right-radius: 4px;
}

/* ============================================
   消息元信息
   ============================================ */
.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 11px;
  color: #999;
}

.assistant-meta {
  margin-left: 4px;
}

.user-meta {
  margin-right: 4px;
}

.role-tag {
  font-weight: 500;
  color: #ff8c42;
}

.user-row .role-tag {
  color: #999;
}

/* ============================================
   思考中状态
   ============================================ */
.thinking-indicator {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #999;
  font-style: italic;
}

.thinking-text {
  font-size: 13px;
}

.thinking-dots {
  animation: thinkingPulse 1.2s ease-in-out infinite;
  font-size: 13px;
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

/* 打字机光标 */
.typing-cursor {
  display: inline-block;
  color: #ff8c42;
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

/* 错误状态 */
.error-text {
  color: #f56c6c;
  font-size: 13px;
}

/* 流式输出正文样式 */
.streaming-text,
.done-text {
  display: inline;
  margin: 0;
  padding: 0;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
  letter-spacing: normal;
  word-spacing: normal;
  white-space: pre-line;
  word-break: break-word;
}

/* ============================================
   日期分割线
   ============================================ */
.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
}

.date-separator-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(243, 216, 199, 0.6) 20%,
    rgba(243, 216, 199, 0.6) 80%,
    transparent
  );
}

.date-separator-text {
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
  padding: 4px 12px;
  background: #f8f6f3;
  border-radius: 12px;
}

/* ============================================
   空对话状态
   ============================================ */
.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 360px;
  color: #999;
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
  color: #2f2f2f;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* ============================================
   底部输入区
   ============================================ */
.input-area {
  flex-shrink: 0;
  padding: 16px 24px 24px;
  background: #f8f6f3;
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  background: #ffffff;
  border: 1px solid rgba(243, 216, 199, 0.6);
  border-radius: 16px;
  box-shadow: 0 -4px 24px rgba(255, 140, 66, 0.1),
    0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 16px;
}

.input-container :deep(.el-textarea__inner) {
  border: none;
  padding: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: #2f2f2f;
  background: transparent;
}

.input-container :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.input-container :deep(.el-textarea__inner::placeholder) {
  color: #c0c4cc;
  line-height: 1.8;
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
  color: #999;
}

.input-hint kbd {
  display: inline-block;
  padding: 2px 6px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  color: #666;
}

.send-btn {
  border-radius: 20px;
  padding: 8px 20px;
}

/* ============================================
   面试结束提示
   ============================================ */
.ended-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  font-size: 14px;
  color: #909399;
  background: rgba(255, 255, 255, 0.8);
  border-top: 1px solid rgba(243, 216, 199, 0.3);
}

.ended-notice .el-icon {
  color: #67c23a;
}

/* ============================================
   响应式
   ============================================ */
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
  .chat-container {
    padding: 12px;
  }

  .chat-messages {
    gap: 20px;
  }

  .message-avatar {
    width: 36px;
    height: 36px;
  }

  .message-bubble {
    padding: 12px 14px;
    font-size: 13px;
  }

  .status-bar-left {
    gap: 8px;
  }

  .interview-title {
    font-size: 14px;
  }

  .mode-text {
    display: none;
  }

  .status-indicator {
    font-size: 12px;
  }

  .input-container {
    padding: 12px;
    border-radius: 12px;
  }
}
</style>
