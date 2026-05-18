<template>
  <header class="app-header">
    <div class="header-left">
      <img src="@/assets/logo.jpg" class="logo-img" alt="Logo" />
      <span class="logo-text">智能模拟面试与简历诊断系统</span>
    </div>

    <nav class="header-nav desktop-nav">
      <!-- 首页始终显示 -->
      <router-link to="/" class="nav-link" :class="{ active: isHomeActive }">
        首页
      </router-link>

      <!-- 已登录才显示简历诊断 -->
      <router-link
        v-if="isLoggedIn"
        to="/resume/upload"
        class="nav-link"
        :class="{ active: isResumeActive }"
      >
        简历诊断
      </router-link>

      <!-- 已登录才显示模拟面试 -->
      <router-link
        v-if="isLoggedIn"
        to="/interview/entry"
        class="nav-link"
        :class="{ active: isInterviewActive }"
      >
        模拟面试
      </router-link>

      <!-- 已登录才显示模板库 -->
      <router-link
        v-if="isLoggedIn"
        to="/templates"
        class="nav-link"
        :class="{ active: isTemplateActive }"
      >
        模板库
      </router-link>

      <!-- 已登录才显示成长中心 -->
      <router-link
        v-if="isLoggedIn"
        to="/growth"
        class="nav-link"
        :class="{ active: isGrowthActive }"
      >
        成长中心
      </router-link>

      <!-- 已登录才显示 Offer 辅助 -->
      <router-link
        v-if="isLoggedIn"
        to="/offer"
        class="nav-link"
        :class="{ active: isOfferActive }"
      >
        Offer 辅助
      </router-link>

      <!-- 已登录才显示历史记录下拉菜单 -->
      <div v-if="isLoggedIn" class="history-dropdown-wrapper">
        <el-dropdown trigger="click" @command="handleHistoryCommand">
          <span
            class="nav-link history-trigger"
            :class="{ active: isHistoryActive }"
          >
            历史记录
            <svg
              class="dropdown-arrow"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="history-dropdown-menu">
              <el-dropdown-item
                command="resume"
                :class="{ active: isResumeHistoryActive }"
              >
                <svg
                  class="dropdown-icon"
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
                简历诊断历史
              </el-dropdown-item>
              <el-dropdown-item
                command="interview"
                :class="{ active: isInterviewHistoryActive }"
              >
                <svg
                  class="dropdown-icon"
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
                模拟面试历史
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>

    <!-- 小屏汉堡按钮 -->
    <button class="hamburger-btn" @click="drawerVisible = true">
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <line x1="3" y1="6" x2="21" y2="6" />
        <line x1="3" y1="12" x2="21" y2="12" />
        <line x1="3" y1="18" x2="21" y2="18" />
      </svg>
    </button>

    <div class="header-right">
      <!-- 主题切换按钮 -->
      <button class="theme-toggle" @click="themeStore.toggleTheme()" :aria-label="themeStore.resolvedTheme === 'dark' ? '切换为亮色模式' : '切换为暗色模式'">
        <!-- 亮色模式显示月亮图标 -->
        <svg v-if="themeStore.resolvedTheme === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
        <!-- 暗色模式显示太阳图标 -->
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="5" />
          <line x1="12" y1="1" x2="12" y2="3" />
          <line x1="12" y1="21" x2="12" y2="23" />
          <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
          <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
          <line x1="1" y1="12" x2="3" y2="12" />
          <line x1="21" y1="12" x2="23" y2="12" />
          <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
          <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
        </svg>
      </button>

      <!-- 已登录状态：显示通知铃铛和头像下拉菜单 -->
      <template v-if="isLoggedIn">
        <!-- 消息通知铃铛 -->
        <el-popover
          v-if="notificationRealtimeEnabled"
          v-model:visible="notificationPopoverVisible"
          placement="bottom-end"
          :width="360"
          trigger="click"
          :show-arrow="false"
          :offset="8"
          popper-class="notification-popover"
          @before-enter="handleNotificationOpen"
        >
          <template #reference>
            <div class="notification-bell">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </div>
          </template>

          <!-- 通知下拉面板 -->
          <div class="notification-panel">
            <div class="panel-header">
              <span class="panel-title">消息通知</span>
              <el-button
                v-if="unreadCount > 0"
                type="primary"
                link
                size="small"
                :loading="markAllReadLoading"
                @click="handleMarkAllRead"
              >
                全部已读
              </el-button>
            </div>

            <div v-if="notificationLoading" class="panel-loading">
              <span class="loading-spinner"></span>
              <span>加载中...</span>
            </div>

            <div v-else-if="notificationList.length === 0" class="panel-empty">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-bell-icon">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <p>暂无消息</p>
            </div>

            <div v-else class="panel-list">
              <div
                v-for="item in notificationList"
                :key="item.id"
                class="panel-item"
                :class="{ unread: item.readStatus === 0 }"
                @click="handleNotificationRead(item)"
              >
                <NotificationTypeIcon class="panel-item-icon" :type="item.type" size="sm" />
                <div class="panel-item-content">
                  <div class="panel-item-title-row">
                    <div class="panel-item-title">{{ item.title }}</div>
                    <el-tag :type="getNotificationTypeMeta(item.type).tagType" size="small" effect="plain">
                      {{ getNotificationTypeMeta(item.type).label }}
                    </el-tag>
                  </div>
                  <div class="panel-item-text">{{ item.content }}</div>
                  <div class="panel-item-time">{{ formatNotificationTime(item.createTime, { compact: true }) }}</div>
                </div>
                <div v-if="item.readStatus === 0" class="panel-item-dot"></div>
              </div>
            </div>

            <div class="panel-footer" @click="goToNotificationPage">
              查看全部消息
            </div>
          </div>
        </el-popover>

      <el-dialog
          v-model="announcementDialogVisible"
          class="announcement-dialog"
          :show-close="true"
          :append-to-body="true"
        >
          <template #header>
            <div class="announcement-dialog-header" v-if="selectedAnnouncement">
              <NotificationTypeIcon :type="selectedAnnouncement.type" size="sm" />
              <div class="announcement-dialog-title-block">
                <div class="announcement-dialog-title">{{ selectedAnnouncement.title }}</div>
                <div class="announcement-dialog-meta">
                  <el-tag :type="getNotificationTypeMeta(selectedAnnouncement.type).tagType" size="small" effect="plain">
                    {{ getNotificationTypeMeta(selectedAnnouncement.type).label }}
                  </el-tag>
                  <span>{{ formatNotificationTime(selectedAnnouncement.createTime) }}</span>
                </div>
              </div>
            </div>
          </template>
          <div class="announcement-dialog-content" v-if="selectedAnnouncement">
            {{ selectedAnnouncement.content }}
          </div>
        </el-dialog>

        <el-dropdown trigger="click" @command="handleCommand">
          <div class="avatar-wrapper avatar-sm">
            <div class="avatar-ring avatar-sm">
              <img src="@/assets/user.png" class="avatar-img avatar-sm" alt="用户头像" />
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="user-dropdown-menu">
              <!-- 用户信息区 -->
              <div class="user-info-header">
                <div class="user-info-avatar-wrapper">
                  <img src="@/assets/user.png" alt="用户头像" />
                </div>
                <div class="user-info-content">
                  <div class="user-info-name">
                    {{ username }}
                  </div>
                  <div class="user-info-role" :class="userRoleClass">
                    {{ userRoleText }}
                  </div>
                </div>
              </div>

              <!-- 个人中心入口 -->
              <el-dropdown-item command="nickname">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M12 20h9" />
                  <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                </svg>
                修改昵称
              </el-dropdown-item>
              <el-dropdown-item command="profile">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
                个人中心
              </el-dropdown-item>
              <!-- 退出登录 -->
              <!-- 会员中心入口：
                   页面已经存在，这里只是在头像下拉菜单中补入口。
                   放在“个人中心”下面、“退出登录”上面，符合账户相关操作的使用顺序。 -->
              <el-dropdown-item command="membership">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polygon
                    points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"
                  />
                </svg>
                会员中心
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <circle cx="12" cy="12" r="3" />
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06A1.65 1.65 0 0 0 15 19.4a1.65 1.65 0 0 0-1 .6l-.09.09a2 2 0 0 1-2.83-2.83l.09-.09A1.65 1.65 0 0 0 10.6 15a1.65 1.65 0 0 0-1.82-.33l-.11.05a2 2 0 0 1-2.6-2.6l.05-.11A1.65 1.65 0 0 0 4.6 10a1.65 1.65 0 0 0-.6-1l-.09-.09a2 2 0 0 1 2.83-2.83l.09.09A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-.6l.09-.09a2 2 0 0 1 2.83 2.83l-.09.09A1.65 1.65 0 0 0 13.4 9a1.65 1.65 0 0 0 1.82.33l.11-.05a2 2 0 0 1 2.6 2.6l-.05.11A1.65 1.65 0 0 0 19.4 15z" />
                </svg>
                设置中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" class="logout-item">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <!-- 未登录状态：显示登录/注册链接 -->
      <template v-else>
        <router-link to="/login" class="login-link">登录/注册</router-link>
      </template>
    </div>

    <!-- 移动端 Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="导航菜单"
      direction="rtl"
      size="280px"
      :with-header="true"
    >
      <nav class="mobile-nav">
        <router-link
          to="/"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >首页</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/resume/upload"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >简历诊断</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/interview/entry"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >模拟面试</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/templates"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >模板库</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/growth"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >成长中心</router-link>
        >
        <router-link
          v-if="isLoggedIn"
          to="/offer"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >Offer 辅助</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/resume/history"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >简历诊断历史</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/interview/history"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >模拟面试历史</router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/dashboard"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >个人中心</router-link
        >
        <router-link
          v-if="isLoggedIn && notificationRealtimeEnabled"
          to="/notifications"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          消息通知
          <span v-if="unreadCount > 0" class="mobile-unread-badge">{{ unreadCount }}</span>
        </router-link
        >
        <router-link
          v-if="isLoggedIn"
          to="/settings"
          class="mobile-nav-link"
          @click="drawerVisible = false"
          >设置中心</router-link
        >
        <!-- 移动端主题切换 -->
        <button class="mobile-nav-link theme-toggle-mobile" @click="themeStore.toggleTheme(); drawerVisible = false">
          <svg v-if="themeStore.resolvedTheme === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="theme-icon">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="theme-icon">
            <circle cx="12" cy="12" r="5" />
            <line x1="12" y1="1" x2="12" y2="3" />
            <line x1="12" y1="21" x2="12" y2="23" />
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
            <line x1="1" y1="12" x2="3" y2="12" />
            <line x1="21" y1="12" x2="23" y2="12" />
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
          </svg>
          {{ themeStore.resolvedTheme === 'dark' ? '切换亮色模式' : '切换暗色模式' }}
        </button>
      </nav>
    </el-drawer>

    <el-dialog
      v-model="nicknameDialogVisible"
      class="nickname-dialog"
      title="修改昵称"
      width="min(440px, calc(100vw - 24px))"
      :close-on-click-modal="false"
      :append-to-body="true"
      @closed="resetNicknameForm"
    >
      <el-form
        ref="nicknameFormRef"
        :model="nicknameForm"
        :rules="nicknameRules"
        label-position="top"
        class="nickname-form"
      >
        <div class="nickname-current">
          <div class="nickname-current-avatar">
            <img src="@/assets/user.png" alt="用户头像" />
          </div>
          <div class="nickname-current-text">
            <span>当前昵称</span>
            <strong>{{ username }}</strong>
          </div>
        </div>
        <el-form-item label="新昵称" prop="nickname">
          <el-input
            v-model="nicknameForm.nickname"
            maxlength="12"
            show-word-limit
            clearable
            autocomplete="nickname"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="nicknameDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="nicknameSaving" @click="handleNicknameSave">
            保存昵称
          </el-button>
        </div>
      </template>
    </el-dialog>

  </header>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useThemeStore } from "@/stores/theme";
import { ElMessage } from "element-plus";
import { removeToken } from "@/utils/auth";
import { updateNickname } from "@/api/auth";
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, connectNotificationStream } from "@/api/notification";
import NotificationTypeIcon from "@/components/notification/NotificationTypeIcon.vue";
import { formatNotificationTime, getNotificationTypeMeta, isAdminAnnouncementType } from "@/utils/notificationMeta";
import { getSettingsPreferences, SETTINGS_PREFERENCES_UPDATED_EVENT } from "@/utils/settingsPreferences";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const themeStore = useThemeStore();
const settingsPreferences = ref(getSettingsPreferences());

const drawerVisible = ref(false);
const nicknameDialogVisible = ref(false);
const nicknameFormRef = ref(null);
const nicknameSaving = ref(false);
const nicknameForm = ref({ nickname: "" });
const isLoggedIn = computed(() => userStore.isLoggedIn());
const notificationRealtimeEnabled = computed(() => settingsPreferences.value.notificationRealtimeEnabled !== false);

// ===== 消息通知相关状态 =====
/** 未读通知数量 */
const unreadCount = ref(0);
/** 通知列表（最近10条） */
const notificationList = ref([]);
/** 通知面板是否展开 */
const notificationPopoverVisible = ref(false);
/** 通知加载状态 */
const notificationLoading = ref(false);
/** 全部已读加载状态 */
const markAllReadLoading = ref(false);
/** 公告详情弹窗状态 */
const announcementDialogVisible = ref(false);
const selectedAnnouncement = ref(null);
/** 轮询定时器（SSE 断线降级方案） */
let notificationTimer = null;
/** SSE 连接控制器 */
let sseController = null;

/**
 * 同步读取本机设置偏好。
 * 设置中心保存后会派发自定义事件，这里需要即时刷新顶部通知行为。
 */
const syncSettingsPreferences = (nextPreferences) => {
  settingsPreferences.value = nextPreferences || getSettingsPreferences();
};

const handleSettingsPreferencesUpdated = (event) => {
  syncSettingsPreferences(event.detail);
};

const stopNotificationRealtime = () => {
  if (sseController) {
    sseController.abort();
    sseController = null;
  }
  if (notificationTimer) {
    clearInterval(notificationTimer);
    notificationTimer = null;
  }
  notificationPopoverVisible.value = false;
  unreadCount.value = 0;
  notificationList.value = [];
};

/**
 * 获取未读通知数量
 */
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount();
    if (res.code === 200) {
      unreadCount.value = Number(res.data.unreadCount) || 0;
    }
  } catch (e) {
    console.error("获取未读数量失败", e);
  }
};

/**
 * 获取最近通知列表（面板展开时调用）
 */
const fetchNotificationList = async () => {
  notificationLoading.value = true;
  try {
    const res = await getNotifications({ pageNum: 1, size: 10 });
    if (res.code === 200) {
      notificationList.value = res.data.records || [];
      unreadCount.value = Number(res.data.unreadCount) || 0;
    }
  } catch (e) {
    console.error("获取通知列表失败", e);
  } finally {
    notificationLoading.value = false;
  }
};

const updatePanelNotificationReadState = (id, readStatus, readTime) => {
  notificationList.value = notificationList.value.map((item) =>
    item.id === id ? { ...item, readStatus, readTime } : item
  );
};

const markPanelNotificationReadOptimistically = (item) => {
  if (item.readStatus !== 0) return;

  const readTime = new Date().toISOString();
  updatePanelNotificationReadState(item.id, 1, readTime);
  unreadCount.value = Math.max(0, unreadCount.value - 1);

  markAsRead(item.id).catch((e) => {
    console.error("标记已读失败，回滚状态", e);
    updatePanelNotificationReadState(item.id, 0, item.readTime || null);
    unreadCount.value += 1;
  });
};

/**
 * 打开通知面板
 */
const handleNotificationOpen = () => {
  notificationPopoverVisible.value = true;
  fetchNotificationList();
};

/**
 * 单条通知标记已读
 */
const handleNotificationRead = async (item) => {
  markPanelNotificationReadOptimistically(item);
  notificationPopoverVisible.value = false;

  if (isAdminAnnouncementType(item.type) && item.broadcastId) {
    selectedAnnouncement.value = item;
    announcementDialogVisible.value = true;
    return;
  }

  if (item.bizType === "resume_diagnosis" && item.bizId) {
    router.push(`/resume/result/${item.bizId}`);
  } else if (item.bizType === "resume_polish" && item.bizId) {
    router.push(`/resume/result/${item.bizId}`);
  } else if (item.bizType === "mock_interview" && item.bizId) {
    router.push(`/interview/report/${item.bizId}`);
  }
};

/**
 * 全部标记已读
 */
const handleMarkAllRead = async () => {
  markAllReadLoading.value = true;
  try {
    await markAllAsRead();
    unreadCount.value = 0;
    const readTime = new Date().toISOString();
    notificationList.value = notificationList.value.map((item) => (
      item.readStatus === 0 ? { ...item, readStatus: 1, readTime } : item
    ));
    ElMessage.success("已全部标记为已读");
  } catch {
    // 拦截器已弹出错误提示
  } finally {
    markAllReadLoading.value = false;
  }
};

/**
 * 跳转到通知完整页面
 */
const goToNotificationPage = () => {
  notificationPopoverVisible.value = false;
  router.push("/notifications");
};

const username = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || "用户");

const nicknameRules = {
  nickname: [
    { required: true, message: "请输入昵称", trigger: "blur" },
    { min: 2, max: 12, message: "昵称长度应为 2-12 个字符", trigger: "blur" }
  ]
};

// 用户角色判定
const isAdmin = computed(() => userStore.userInfo?.role === 9);
const isVipUser = computed(() => userStore.isVip());

// 角色徽章文本
const userRoleText = computed(() => {
  if (isAdmin.value) return "管理员";
  if (isVipUser.value) return "会员";
  return "普通用户";
});

// 角色徽章样式类
const userRoleClass = computed(() => {
  if (isAdmin.value) return "role-admin";
  if (isVipUser.value) return "role-vip";
  return "role-normal";
});

// 首页激活状态
const isHomeActive = computed(() => {
  return route.path === "/";
});

// 简历诊断激活状态
const isResumeActive = computed(() => {
  const path = route.path;
  return path.startsWith("/resume") && !path.startsWith("/resume/history");
});

// 模拟面试激活状态
const isInterviewActive = computed(() => {
  const path = route.path;
  return (
    path.startsWith("/interview") && !path.startsWith("/interview/history")
  );
});

// 模板库激活状态
const isTemplateActive = computed(() => route.path.startsWith("/templates"));

// 成长中心激活状态
const isGrowthActive = computed(() => route.path === "/growth");

// Offer 辅助激活状态
const isOfferActive = computed(() => route.path.startsWith("/offer"));

// 历史记录父级激活状态
const isHistoryActive = computed(() => {
  const path = route.path;
  return path === "/resume/history" || path === "/interview/history";
});

// 简历诊断历史激活状态
const isResumeHistoryActive = computed(() => {
  return route.path === "/resume/history";
});

// 模拟面试历史激活状态
const isInterviewHistoryActive = computed(() => {
  return route.path === "/interview/history";
});

// 处理历史记录下拉菜单命令
const handleHistoryCommand = (command) => {
  if (command === "resume") {
    router.push("/resume/history");
  } else if (command === "interview") {
    router.push("/interview/history");
  }
};

const openNicknameDialog = () => {
  nicknameForm.value.nickname = userStore.userInfo?.nickname || "";
  nicknameDialogVisible.value = true;
};

const resetNicknameForm = () => {
  nicknameForm.value.nickname = userStore.userInfo?.nickname || "";
  nicknameFormRef.value?.resetFields();
};

const handleNicknameSave = async () => {
  if (!nicknameFormRef.value) return;
  try {
    await nicknameFormRef.value.validate();
  } catch {
    return;
  }

  nicknameSaving.value = true;
  try {
    await updateNickname({ nickname: nicknameForm.value.nickname.trim() });
    // 昵称保存后刷新用户信息，头像菜单与页面标题可立即同步新昵称。
    if (typeof userStore.fetchUserInfo === "function") {
      await userStore.fetchUserInfo();
    }
    nicknameDialogVisible.value = false;
    ElMessage.success("昵称已保存");
  } finally {
    nicknameSaving.value = false;
  }
};

// 处理头像下拉菜单命令
const handleCommand = (command) => {
  if (command === "nickname") {
    openNicknameDialog();
  } else if (command === "profile") {
    router.push("/dashboard");
  } else if (command === "membership") {
    router.push("/membership");
  } else if (command === "settings") {
    router.push("/settings");
  } else if (command === "logout") {
    // 断开 SSE 连接
    if (sseController) {
      sseController.abort();
      sseController = null;
    }
    localStorage.removeItem("token");
    removeToken();
  userStore.clearUserInfo();
    ElMessage.success("已退出登录");
    router.push("/");
  }
};

// 监听登录状态变化，启动或停止通知推送
watch([isLoggedIn, notificationRealtimeEnabled], ([loggedIn, realtimeEnabled]) => {
  if (loggedIn && realtimeEnabled) {
    fetchUnreadCount();
    // 建立 SSE 实时推送连接
    sseController = connectNotificationStream({
      onNotification(data) {
        // 收到新通知：更新未读数，将新通知插入列表头部
        if (data.unreadCount !== undefined) {
          unreadCount.value = Number(data.unreadCount) || 0;
        }
        if (data.notification) {
          // 始终将新通知插入列表头部，确保打开面板时能看到
          notificationList.value = [data.notification, ...notificationList.value].slice(0, 10);
        }
      },
      onUnreadCount(data) {
        if (data.unreadCount !== undefined) {
          unreadCount.value = Number(data.unreadCount) || 0;
        }
      },
      onError() {
        // SSE 断线时降级为轮询（由外层统一管理定时器，此处仅记录日志）
      }
    });
    // 降级轮询：每 5 分钟同步一次（防止 SSE 丢失事件）
    notificationTimer = setInterval(fetchUnreadCount, 300000);
  } else {
    // 退出登录或关闭实时通知时，统一清理连接和本地展示状态。
    stopNotificationRealtime();
  }
}, { immediate: true });

onMounted(() => {
  if (typeof window !== "undefined") {
    window.addEventListener(SETTINGS_PREFERENCES_UPDATED_EVENT, handleSettingsPreferencesUpdated);
  }
});

onUnmounted(() => {
  if (typeof window !== "undefined") {
    window.removeEventListener(SETTINGS_PREFERENCES_UPDATED_EVENT, handleSettingsPreferencesUpdated);
  }
  stopNotificationRealtime();
});
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background-color: var(--bg-header);
  border-bottom: 1px solid var(--orange-main);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-img {
  height: 36px;
  border-radius: 6px;
  object-fit: contain;
}

.logo-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-title);
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  padding: 8px 16px;
  font-size: 14px;
  color: var(--text-body);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link:hover {
  color: var(--orange-main);
}

.nav-link.active {
  color: var(--orange-main);
  border-bottom-color: var(--orange-main);
}

.header-right {
  display: flex;
  align-items: center;
}

.login-link {
  font-size: 14px;
  color: var(--orange-main);
  text-decoration: none;
}

.login-link:hover {
  text-decoration: underline;
}

/* 历史记录下拉菜单 */
.history-dropdown-wrapper {
  position: relative;
}

.history-trigger {
  user-select: none;
}

.dropdown-arrow {
  width: 14px;
  height: 14px;
  transition: transform 0.2s;
}

.history-dropdown-wrapper:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* 小屏汉堡按钮 */
.hamburger-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.hamburger-btn:hover {
  background-color: var(--bg-page);
}

.hamburger-btn svg {
  width: 22px;
  height: 22px;
  color: var(--text-title);
  display: block;
}

/* 移动端导航 */
.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-nav-link {
  display: block;
  padding: 14px 16px;
  font-size: 15px;
  color: var(--text-body);
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.15s;
}

.mobile-nav-link:hover {
  background-color: var(--bg-page);
  color: var(--orange-main);
}

.mobile-nav-link.router-link-active {
  background-color: var(--bg-page);
  color: var(--orange-main);
  font-weight: 500;
}

/* 响应式断点 */
/* 中屏：1024px - 1279px */
@media (max-width: 1279px) {
  .logo-text {
    font-size: 14px;
  }
  .nav-link {
    padding: 8px 10px;
    font-size: 13px;
  }
  .header-left {
    gap: 10px;
  }
}

/* 小屏：≤1023px */
@media (max-width: 1023px) {
  .hamburger-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .desktop-nav {
    display: none;
  }
  .header-nav {
    flex: 1;
    justify-content: flex-end;
  }
  .header-left {
    flex-shrink: 1;
    min-width: 0;
  }
  .logo-text {
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* ===== 消息通知铃铛 ===== */
.notification-bell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-right: 8px;
}

.notification-bell:hover {
  background-color: var(--bg-page);
}

.notification-bell svg {
  width: 20px;
  height: 20px;
  color: var(--text-body);
  transition: color 0.2s;
}

.notification-bell:hover svg {
  color: var(--orange-main);
}

.bell-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: #ff4d4f;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* ===== 通知下拉面板 ===== */
.notification-panel {
  margin: -12px -16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-divider);
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-title);
}

.panel-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--text-muted);
  font-size: 13px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-divider);
  border-top-color: var(--orange-main);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}


.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.empty-bell-icon {
  width: 40px;
  height: 40px;
  color: var(--text-placeholder);
  margin-bottom: 8px;
}

.panel-empty p {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.panel-list {
  max-height: 360px;
  overflow-y: auto;
}

.panel-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
  position: relative;
}

.panel-item:hover {
  background-color: var(--bg-elevated);
}

.panel-item.unread {
  background-color: var(--bg-card-hover);
}

.panel-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.panel-item-icon svg {
  width: 16px;
  height: 16px;
}

.panel-item-icon.type-resume {
  background: rgba(255, 140, 66, 0.1);
  color: #ff8c42;
}

.panel-item-icon.type-polish {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.panel-item-icon.type-interview {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.panel-item-icon.type-quota {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.panel-item-icon.type-system {
  background: rgba(144, 147, 153, 0.1);
  color: #909399;
}

.panel-item-content {
  flex: 1;
  min-width: 0;
}

.panel-item-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  margin-bottom: 3px;
}

.panel-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.panel-item-text {
  font-size: 12px;
  color: var(--text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.45;
  margin-bottom: 2px;
}

.panel-item-time {
  font-size: 11px;
  color: var(--text-placeholder);
}

.panel-item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--orange-main);
  flex-shrink: 0;
  margin-top: 6px;
}

.panel-footer {
  text-align: center;
  padding: 10px 16px;
  font-size: 13px;
  color: var(--orange-main);
  border-top: 1px solid var(--border-divider);
  cursor: pointer;
  transition: background-color 0.15s;
}

.panel-footer:hover {
  background-color: var(--bg-page);
}

/* 移动端未读角标 */
.mobile-unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--color-danger);
  border-radius: 9px;
  margin-left: 6px;
}

/* ===== 主题切换按钮 ===== */
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  background: none;
  border: none;
  transition: background-color 0.2s, transform 0.15s;
  margin-right: 4px;
  color: var(--text-body);
  -webkit-tap-highlight-color: transparent;
}

.theme-toggle:hover {
  background-color: var(--bg-page);
  color: var(--orange-main);
}

.theme-toggle:active {
  transform: scale(0.88);
}

.theme-toggle svg {
  width: 20px;
  height: 20px;
  transition: color 0.2s;
}

/* 移动端主题切换 */
.theme-toggle-mobile {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;
  transition: transform 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.theme-toggle-mobile:active {
  transform: scale(0.96);
}

.theme-toggle-mobile .theme-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

</style>

<style>
/* 公告弹窗 — 全局样式，因 append-to-body 需处理 teleport 的元素 */
.announcement-dialog {
  --el-dialog-width: 560px;
}

.announcement-dialog .el-dialog {
  border-radius: 12px;
  background: var(--bg-card);
}

.announcement-dialog .el-dialog__header {
  padding: 22px 24px 14px;
  margin-right: 38px;
}

.announcement-dialog .el-dialog__body {
  padding: 0 24px 24px;
}

.announcement-dialog-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.announcement-dialog-title-block {
  min-width: 0;
}

.announcement-dialog-title {
  color: var(--text-title);
  font-size: 18px;
  line-height: 1.4;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.announcement-dialog-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.announcement-dialog-content {
  max-height: 52vh;
  overflow-y: auto;
  padding: 18px;
  border: 1px solid var(--border-card);
  border-radius: 8px;
  background: var(--bg-page);
  color: var(--text-body);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 767px) {
  .announcement-dialog {
    --el-dialog-width: calc(100vw - 32px);
  }

  .announcement-dialog-header {
    flex-direction: column;
    gap: 8px;
  }

  .announcement-dialog .el-dialog__header {
    padding: 16px 16px 10px;
    margin-right: 32px;
  }

  .announcement-dialog .el-dialog__body {
    padding: 0 16px 16px;
  }

  .announcement-dialog-title {
    font-size: 16px;
  }

  .announcement-dialog-content {
    padding: 12px;
    font-size: 13px;
    max-height: 45vh;
  }
}

@media (max-width: 480px) {
  .announcement-dialog {
    --el-dialog-width: 100vw;
  }

  .announcement-dialog .el-dialog {
    border-radius: 0;
  }

  .announcement-dialog .el-dialog__header {
    padding: 14px 14px 10px;
    margin-right: 24px;
  }

  .announcement-dialog-title {
    font-size: 15px;
  }

  .announcement-dialog-content {
    padding: 10px;
    font-size: 12px;
    max-height: 40vh;
  }
}
</style>

<style>
/* 昵称弹窗使用 append-to-body，需要全局样式覆盖 Element Plus teleport 后的结构。 */
.nickname-dialog {
  max-width: calc(100vw - 24px);
}

.nickname-dialog .el-dialog {
  border-radius: 12px;
  background: var(--bg-card);
}

.nickname-dialog .el-dialog__header {
  padding: 22px 24px 12px;
  margin-right: 40px;
  border-bottom: 1px solid var(--border-divider);
}

.nickname-dialog .el-dialog__title {
  color: var(--text-title);
  font-size: 18px;
  font-weight: 700;
}

.nickname-dialog .el-dialog__body {
  padding: 20px 24px 6px;
}

.nickname-dialog .el-dialog__footer {
  padding: 12px 24px 22px;
}

.nickname-form .el-form-item {
  margin-bottom: 0;
}

.nickname-current {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 18px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
}

.nickname-current-avatar {
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border-card);
  background: var(--bg-card);
}

.nickname-current-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.nickname-current-text {
  min-width: 0;
}

.nickname-current-text span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.4;
}

.nickname-current-text strong {
  display: block;
  margin-top: 4px;
  min-width: 0;
  color: var(--text-title);
  font-size: 15px;
  overflow-wrap: anywhere;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 480px) {
  .nickname-dialog.el-dialog {
    margin-top: 12vh;
    margin-bottom: 0;
  }

  .nickname-dialog .el-dialog__header {
    padding: 16px 16px 10px;
    margin-right: 28px;
  }

  .nickname-dialog .el-dialog__body {
    padding: 14px 16px 4px;
  }

  .nickname-dialog .el-dialog__footer {
    padding: 10px 16px 16px;
  }

  .nickname-current {
    align-items: flex-start;
    padding: 12px;
  }

  .nickname-current-avatar {
    width: 38px;
    height: 38px;
  }

  .dialog-footer {
    flex-direction: column-reverse;
    align-items: stretch;
    gap: 8px;
  }

  .dialog-footer .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
