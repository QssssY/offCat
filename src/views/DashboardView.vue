<template>
  <div class="dashboard-view">
    <!-- 顶部区域 -->
    <div class="top-section">
      <!-- 左侧：身份欢迎卡 -->
      <div class="identity-card">
        <div class="card-bg-decoration"></div>
        <div class="card-content">
          <div class="identity-left">
            <div class="avatar-wrapper avatar-lg">
              <div class="avatar-ring avatar-lg">
                <img src="@/assets/user.png" class="avatar-img avatar-lg" />
              </div>
            </div>
            <div class="user-info">
              <div class="welcome-text">欢迎回来</div>
              <div class="user-name">
                {{ userStore.userInfo?.username || "用户" }}
              </div>
              <div class="user-role-tag" :class="roleBadgeClass">
                <span class="role-dot"></span>
                {{ roleBadgeText }}
              </div>
            </div>
          </div>
          <div class="identity-right" v-if="isVipUser && !isAdmin">
            <div class="vip-badge">
              <svg
                class="vip-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polygon
                  points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
                />
              </svg>
              <span>会员有效期至</span>
            </div>
            <div class="vip-expire-time">{{ formatVipExpireTime }}</div>
          </div>
          <div class="identity-right" v-else>
            <div class="guest-badge">
              <svg
                class="guest-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <span>{{ isAdmin ? "管理员身份" : "普通用户" }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：权益配额卡 -->
      <div class="quota-card">
        <div class="quota-item resume">
          <div class="quota-icon-wrap">
            <svg
              class="quota-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
              />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
            </svg>
          </div>
          <div class="quota-info">
            <div class="quota-number">{{ resumeQuotaLeft }}</div>
            <div class="quota-label">{{ resumeQuotaLabel }}</div>
          </div>
        </div>
        <div class="quota-divider"></div>
        <div class="quota-item interview">
          <div class="quota-icon-wrap">
            <svg
              class="quota-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </div>
          <div class="quota-info">
            <div class="quota-number">{{ interviewQuotaLeft }}</div>
            <div class="quota-label">{{ interviewQuotaLabel }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据概览区 - 4张独立卡片 -->
    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon resume">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
            />
            <polyline points="14 2 14 8 20 8" />
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ resumeCountThisMonth }}</div>
          <div class="stat-label">本月诊断</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon interview">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ interviewCountThisMonth }}</div>
          <div class="stat-label">本月面试</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon resume-left">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ resumeQuotaLeft }}</div>
          <div class="stat-label">{{ resumeStatLabel }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon interview-left">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polygon
              points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
            />
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ interviewQuotaLeft }}</div>
          <div class="stat-label">{{ interviewStatLabel }}</div>
        </div>
      </div>
    </div>

    <!-- 最近记录区 -->
    <div class="records-section">
      <div class="record-column">
        <div class="column-header">
          <div class="header-left">
            <div class="header-icon resume">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                />
                <polyline points="14 2 14 8 20 8" />
              </svg>
            </div>
            <h3 class="column-title">最近简历诊断</h3>
          </div>
          <el-button
            link
            type="primary"
            class="view-all-btn"
            @click="viewAllResume"
          >
            查看全部
            <svg
              class="arrow-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </el-button>
        </div>
        <div class="record-list">
          <template v-if="recentResumeRecords.length > 0">
            <div
              v-for="record in recentResumeRecords"
              :key="record.taskId"
              class="record-item"
            >
              <div class="record-left">
                <div class="file-icon">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                    />
                    <polyline points="14 2 14 8 20 8" />
                  </svg>
                </div>
                <div class="record-info">
                  <div class="record-name">{{ record.fileName }}</div>
                  <div class="record-time">{{ record.time }}</div>
                </div>
              </div>
              <div class="record-status-badge" :class="record.statusClass">
                {{ record.statusText }}
              </div>
            </div>
          </template>
          <template v-else>
            <div class="empty-state">
              <svg
                class="empty-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <path
                  d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <div class="empty-text">暂无简历诊断记录</div>
              <el-button link type="primary" @click="startResumeDiagnosis"
                >上传简历</el-button
              >
            </div>
          </template>
        </div>
      </div>

      <div class="record-column">
        <div class="column-header">
          <div class="header-left">
            <div class="header-icon interview">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <h3 class="column-title">最近模拟面试</h3>
          </div>
          <el-button
            link
            type="primary"
            class="view-all-btn"
            @click="viewAllInterview"
          >
            查看全部
            <svg
              class="arrow-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </el-button>
        </div>
        <div class="record-list">
          <template v-if="recentInterviewRecords.length > 0">
            <div
              v-for="record in recentInterviewRecords"
              :key="record.sessionId"
              class="record-item"
            >
              <div class="record-left">
                <div class="interview-icon-wrap">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                  </svg>
                </div>
                <div class="record-info">
                  <div class="record-name">{{ record.jobRole }}</div>
                  <div class="record-time">{{ record.time }}</div>
                </div>
              </div>
              <div class="record-score-tag" v-if="record.score">
                <span class="score-value">{{ record.score }}</span>
                <span class="score-unit">分</span>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="empty-state">
              <svg
                class="empty-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
              <div class="empty-text">暂无模拟面试记录</div>
              <el-button link type="primary" @click="startInterview"
                >开始面试</el-button
              >
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { UserFilled, Document, ChatDotRound } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";
import { getResumeHistory, extractFileName } from "@/api/resume";
import { getInterviewHistory } from "@/api/interview";

const router = useRouter();
const userStore = useUserStore();

// 从 store 获取额度数据
const resumeQuotaLeft = computed(() => {
  return userStore.userInfo?.resumeQuota ?? 0;
});

const interviewQuotaLeft = computed(() => {
  return userStore.userInfo?.interviewQuota ?? 0;
});

// 用户角色判定
const isAdmin = computed(() => userStore.userInfo?.role === 9);
const isVipUser = computed(() => {
  const role = userStore.userInfo?.role;
  const vipExpireTime = userStore.userInfo?.vipExpireTime;
  if (role !== 1) return false;
  if (!vipExpireTime) return false;
  return new Date(vipExpireTime) > new Date();
});
const isNormalUser = computed(() => !isAdmin.value && !isVipUser.value);

/**
 * 作用：统一首页额度卡和统计卡的标签口径。
 * 之前这里直接写“剩余”，会继续把当前实现表达成“购买套餐后累计剩余次数”，
 * 但当前项目后端已经改成：
 * 1. 普通用户看总免费次数剩余；
 * 2. VIP 用户看后端基于 daily_resume_used / daily_interview_used 计算出的今日剩余。
 * 因此前端必须把 VIP 标成“今日剩余”，把普通用户标成“免费剩余”。
 */
const resumeQuotaLabel = computed(() => {
  return isVipUser.value ? "今日剩余简历诊断" : "免费简历诊断剩余";
});

const interviewQuotaLabel = computed(() => {
  return isVipUser.value ? "今日剩余模拟面试" : "免费模拟面试剩余";
});

const resumeStatLabel = computed(() => {
  return isVipUser.value ? "今日简历剩余" : "免费简历剩余";
});

const interviewStatLabel = computed(() => {
  return isVipUser.value ? "今日面试剩余" : "免费面试剩余";
});

// 角色徽章文本
const roleBadgeText = computed(() => {
  if (isAdmin.value) return "管理员";
  if (isVipUser.value) return "会员";
  return "普通用户";
});

// 角色徽章样式类
const roleBadgeClass = computed(() => {
  if (isAdmin.value) return "badge-admin";
  if (isVipUser.value) return "badge-vip";
  return "badge-normal";
});

// VIP 过期时间格式化
const formatVipExpireTime = computed(() => {
  const vipExpireTime = userStore.userInfo?.vipExpireTime;
  if (!vipExpireTime) return "--";
  const date = new Date(vipExpireTime);
  return date.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
});

// 分离数据源：用于统计和展示
const allResumeHistoryForStats = ref([]);
const allInterviewHistoryForStats = ref([]);
const allResumeHistoryForDisplay = ref([]);
const allInterviewHistoryForDisplay = ref([]);

/**
 * 从 API 响应中提取列表数据
 */
const extractListFromResponse = (res) => {
  if (!res) return [];
  if (Array.isArray(res.data)) {
    return res.data;
  }
  if (res.data && typeof res.data === "object") {
    const listFields = ["list", "records", "rows", "data"];
    for (const field of listFields) {
      if (Array.isArray(res.data[field])) {
        return res.data[field];
      }
    }
  }
  if (res && typeof res === "object") {
    const listFields = ["list", "records", "rows", "data"];
    for (const field of listFields) {
      if (Array.isArray(res[field])) {
        return res[field];
      }
    }
  }
  return [];
};

/**
 * 判断给定时间是否为本月
 */
const isCurrentMonth = (timeValue) => {
  if (!timeValue) return false;
  const now = new Date();
  const currentMonth = now.getMonth();
  const currentYear = now.getFullYear();
  const itemDate = new Date(timeValue);
  if (isNaN(itemDate.getTime())) return false;
  return (
    itemDate.getMonth() === currentMonth &&
    itemDate.getFullYear() === currentYear
  );
};

/**
 * 从记录项中提取时间字段值
 */
const extractTimeFromRecord = (item) => {
  if (!item) return null;
  return (
    item.createTime ||
    item.createdAt ||
    item.startTime ||
    item.created_time ||
    item.updateTime ||
    item.updatedAt ||
    null
  );
};

// 本月诊断统计
const resumeCountThisMonth = computed(() => {
  return allResumeHistoryForStats.value.filter((item) => {
    const timeValue = extractTimeFromRecord(item);
    return isCurrentMonth(timeValue);
  }).length;
});

// 本月面试统计
const interviewCountThisMonth = computed(() => {
  return allInterviewHistoryForStats.value.filter((item) => {
    const timeValue = extractTimeFromRecord(item);
    return isCurrentMonth(timeValue);
  }).length;
});

// 状态映射 - 橙色主题
const statusMap = {
  0: { text: "排队中", class: "status-pending" },
  1: { text: "解析中", class: "status-processing" },
  2: { text: "已完成", class: "status-success" },
  3: { text: "失败", class: "status-failed" },
};

const getStatusText = (status) => {
  return statusMap[status]?.text || "未知";
};

const getStatusClass = (status) => {
  return statusMap[status]?.class || "";
};

// 时间格式化
const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// 最近记录
const recentResumeRecords = computed(() => {
  return allResumeHistoryForDisplay.value.slice(0, 5).map((item) => ({
    taskId: item.taskId,
    fileName: extractFileName(item.fileUrl),
    time: formatTime(extractTimeFromRecord(item)),
    statusText: getStatusText(item.status),
    statusClass: getStatusClass(item.status),
  }));
});

const recentInterviewRecords = computed(() => {
  return allInterviewHistoryForDisplay.value.slice(0, 5).map((item) => ({
    sessionId: item.sessionId,
    jobRole: jobRoleMap[item.jobRole] || item.jobRole || "未知岗位",
    time: formatTime(extractTimeFromRecord(item)),
    score: item.comprehensiveScore ?? item.score,
    status: item.status,
    difficulty: item.difficulty,
    mode: item.interviewMode || item.mode || "normal",
  }));
});

// 岗位名称映射
const jobRoleMap = {
  frontend: "前端开发工程师",
  backend: "后端开发工程师",
  java: "Java开发工程师",
  product: "产品经理",
  algorithm: "算法工程师",
  operation: "运营",
  marketing: "市场/销售",
};

// 获取简历诊断历史记录
const fetchResumeHistory = async () => {
  try {
    const resStats = await getResumeHistory({ pageNum: 1, pageSize: 1000 });
    allResumeHistoryForStats.value = extractListFromResponse(resStats);
    const resDisplay = await getResumeHistory({ pageNum: 1, pageSize: 10 });
    allResumeHistoryForDisplay.value = extractListFromResponse(resDisplay);
    const sortByTime = (a, b) => {
      const timeA = new Date(extractTimeFromRecord(a)).getTime();
      const timeB = new Date(extractTimeFromRecord(b)).getTime();
      return timeB - timeA;
    };
    allResumeHistoryForStats.value.sort(sortByTime);
    allResumeHistoryForDisplay.value.sort(sortByTime);
  } catch (err) {
    if (import.meta.env.DEV) {
      console.error("[首页] 获取简历诊断历史失败:", err);
    }
  }
};

// 获取模拟面试历史记录
const fetchInterviewHistory = async () => {
  try {
    const resStats = await getInterviewHistory({ pageNum: 1, pageSize: 1000 });
    allInterviewHistoryForStats.value = extractListFromResponse(resStats);
    const resDisplay = await getInterviewHistory({ pageNum: 1, pageSize: 10 });
    allInterviewHistoryForDisplay.value = extractListFromResponse(resDisplay);
    const sortByTime = (a, b) => {
      const timeA = new Date(extractTimeFromRecord(a)).getTime();
      const timeB = new Date(extractTimeFromRecord(b)).getTime();
      return timeB - timeA;
    };
    allInterviewHistoryForStats.value.sort(sortByTime);
    allInterviewHistoryForDisplay.value.sort(sortByTime);
  } catch (err) {
    if (import.meta.env.DEV) {
      console.error("[首页] 获取模拟面试历史失败:", err);
    }
  }
};

// 页面加载时获取用户信息和历史记录
onMounted(() => {
  if (userStore.isLoggedIn() && !userStore.userInfo) {
    userStore.fetchUserInfo();
  }
  fetchResumeHistory();
  fetchInterviewHistory();
});

const startResumeDiagnosis = () => {
  router.push("/resume/upload");
};

const startInterview = () => {
  router.push("/interview/entry");
};

// 查看全部简历诊断
const viewAllResume = () => {
  router.push("/resume/history");
};

const viewAllInterview = () => {
  router.push("/interview/history");
};
</script>

<style scoped>
.dashboard-view {
  min-height: 100%;
  padding: 0;
}

.top-section {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
  margin-bottom: 24px;
}

.identity-card {
  position: relative;
  background: linear-gradient(135deg, #ff9a5c 0%, #ff8c42 40%, #e67a35 100%);
  border-radius: 20px;
  padding: 28px 32px;
  color: #ffffff;
  overflow: hidden;
}

.card-bg-decoration {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.card-bg-decoration::after {
  content: "";
  position: absolute;
  top: 20px;
  left: 20px;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.card-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.identity-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.welcome-text {
  font-size: 13px;
  opacity: 0.85;
  font-weight: 400;
}

.user-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.user-role-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  margin-top: 6px;
  width: fit-content;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.identity-right {
  text-align: right;
}

.vip-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 6px;
}

.vip-icon {
  width: 14px;
  height: 14px;
}

.vip-expire-time {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.guest-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  opacity: 0.85;
}

.guest-icon {
  width: 16px;
  height: 16px;
}

.quota-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 20px rgba(255, 140, 66, 0.08);
  border: 1px solid rgba(243, 216, 199, 0.5);
}

.quota-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.quota-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff3e8 0%, #fff8f3 100%);
}

.quota-item.resume .quota-icon-wrap {
  background: linear-gradient(135deg, #fff3e8 0%, #ffe8d6 100%);
}

.quota-item.interview .quota-icon-wrap {
  background: linear-gradient(135deg, #fff0e8 0%, #ffe0cc 100%);
}

.quota-icon {
  width: 24px;
  height: 24px;
  color: #ff8c42;
}

.quota-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quota-number {
  font-size: 28px;
  font-weight: 700;
  color: #2f2f2f;
  line-height: 1;
}

.quota-label {
  font-size: 13px;
  color: #909399;
}

.quota-divider {
  width: 1px;
  height: 52px;
  background: linear-gradient(
    180deg,
    transparent 0%,
    #f3d8c7 50%,
    transparent 100%
  );
  margin: 0 28px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 140, 66, 0.1);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 22px;
  height: 22px;
}

.stat-icon.resume {
  background: linear-gradient(135deg, #fff3e8 0%, #ffe0c8 100%);
  color: #e67a35;
}

.stat-icon.interview {
  background: linear-gradient(135deg, #fff8f3 0%, #ffe8d6 100%);
  color: #ff8c42;
}

.stat-icon.resume-left {
  background: linear-gradient(135deg, #f0f9eb 0%, #d8f0d0 100%);
  color: #67c23a;
}

.stat-icon.interview-left {
  background: linear-gradient(135deg, #fdf6ec 0%, #f8e8d0 100%);
  color: #e6a23c;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #2f2f2f;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.records-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.record-column {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 16px rgba(255, 140, 66, 0.06);
  border: 1px solid rgba(243, 216, 199, 0.4);
}

.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #fff8f3;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon svg {
  width: 18px;
  height: 18px;
}

.header-icon.resume {
  background: #fff3e8;
  color: #ff8c42;
}

.header-icon.interview {
  background: #fff8f3;
  color: #e67a35;
}

.column-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #2f2f2f;
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #ff8c42;
}

.arrow-icon {
  width: 14px;
  height: 14px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 10px;
  transition: all 0.15s ease;
  cursor: pointer;
}

.record-item:hover {
  background: #fff8f3;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.file-icon,
.interview-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #fff3e8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-icon svg,
.interview-icon-wrap svg {
  width: 18px;
  height: 18px;
  color: #ff8c42;
}

.record-info {
  flex: 1;
  min-width: 0;
}

.record-name {
  font-size: 14px;
  color: #2f2f2f;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-time {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.record-status-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
  flex-shrink: 0;
}

.record-status-badge.status-success {
  background: #fff3e8;
  color: #e67a35;
}

.record-status-badge.status-processing {
  background: #fdf6ec;
  color: #e6a23c;
}

.record-status-badge.status-pending {
  background: #f5f7fa;
  color: #909399;
}

.record-status-badge.status-failed {
  background: #fef0f0;
  color: #f56c6c;
}

.record-score-tag {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #fff3e8 0%, #ffe0c8 100%);
  border-radius: 6px;
  flex-shrink: 0;
}

.score-value {
  font-size: 15px;
  font-weight: 700;
  color: #e67a35;
}

.score-unit {
  font-size: 12px;
  color: #e67a35;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  gap: 8px;
}

.empty-icon {
  width: 40px;
  height: 40px;
  color: #f3d8c7;
  margin-bottom: 4px;
}

.empty-text {
  font-size: 13px;
  color: #909399;
}

/* ============================================
   响应式断点
   ============================================ */

/* 大屏：≥1280px - 默认样式 */

/* 中屏：1024px - 1279px */
@media (max-width: 1279px) {
  .top-section {
    gap: 16px;
  }
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .identity-card,
  .quota-card {
    padding: 20px 24px;
  }
  .quota-number {
    font-size: 24px;
  }
  .stat-value {
    font-size: 22px;
  }
  .user-name {
    font-size: 20px;
  }
  .quota-icon-wrap {
    width: 48px;
    height: 48px;
  }
  .quota-icon {
    width: 22px;
    height: 22px;
  }
}

/* 小屏：≤1023px - 平板 */
@media (max-width: 1023px) {
  .top-section {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .stats-section {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .records-section {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .card-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .identity-right {
    text-align: left;
  }
  .card-bg-decoration {
    display: none;
  }
  .quota-card {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
    padding: 20px 20px;
  }
  .quota-item {
    flex: none;
    width: 100%;
  }
  .quota-divider {
    display: none;
  }
  .stat-card {
    padding: 16px 18px;
  }
  .stat-icon {
    width: 42px;
    height: 42px;
  }
  .stat-icon svg {
    width: 20px;
    height: 20px;
  }
  .stat-value {
    font-size: 22px;
  }
  .identity-card {
    padding: 20px 20px;
  }
  .welcome-text {
    font-size: 12px;
  }
  .user-name {
    font-size: 18px;
  }
  .record-column {
    padding: 18px;
  }
}

/* 超小屏：≤767px - 手机 */
@media (max-width: 767px) {
  .dashboard-container {
    padding: 16px;
  }
  .stat-card {
    padding: 14px 16px;
    gap: 12px;
  }
  .stat-icon {
    width: 36px;
    height: 36px;
  }
  .stat-icon svg {
    width: 18px;
    height: 18px;
  }
  .stat-value {
    font-size: 20px;
  }
  .stat-label {
    font-size: 12px;
  }
  .identity-card {
    padding: 16px;
  }
  .quota-card {
    padding: 16px;
  }
  .quota-number {
    font-size: 22px;
  }
  .user-name {
    font-size: 16px;
  }
  .record-column {
    padding: 14px;
  }
}
</style>
