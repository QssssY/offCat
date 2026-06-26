<template>
  <header
    class="app-header motion-app-header"
  >
    <div class="header-left">
      <router-link to="/" class="brand-mark motion-brand-mark">
        <span class="logo-box">
          <OptimizedImage :sources="optimizedImages.logo" img-class="logo-img" alt="Logo" loading="eager" fetch-priority="high" />
        </span>
        <h1 class="brand-text">offerCat</h1>
      </router-link>
    </div>

    <nav class="header-nav desktop-nav motion-desktop-nav">
      <!-- 首页始终显示 -->
      <router-link to="/" class="nav-link" :class="{ active: isHomeActive }">
        <FeatureIcon name="home-dashboard" size="xs" class="nav-feature-icon" critical />
        首页
      </router-link>

      <!-- 已登录才显示简历诊断 -->
      <router-link
        v-if="isLoggedIn"
        to="/resume/upload"
        class="nav-link"
        :class="{ active: isResumeActive }"
        @mouseenter="prefetchNavigationRoute('/resume/upload')"
        @focus="prefetchNavigationRoute('/resume/upload')"
        @touchstart.passive="prefetchNavigationRoute('/resume/upload')"
      >
        <FeatureIcon name="resume-upload" size="xs" class="nav-feature-icon" critical />
        简历诊断
      </router-link>

      <!-- 已登录才显示模拟面试 -->
      <router-link
        v-if="isLoggedIn"
        to="/interview/entry"
        class="nav-link"
        :class="{ active: isInterviewActive }"
        @mouseenter="prefetchNavigationRoute('/interview/entry')"
        @focus="prefetchNavigationRoute('/interview/entry')"
        @touchstart.passive="prefetchNavigationRoute('/interview/entry')"
      >
        <FeatureIcon name="mock-interview" size="xs" class="nav-feature-icon" critical />
        模拟面试
      </router-link>

      <!-- 已登录才显示模板库 -->
      <router-link
        v-if="isLoggedIn"
        to="/templates"
        class="nav-link"
        :class="{ active: isTemplateActive }"
        @mouseenter="prefetchNavigationRoute('/templates')"
        @focus="prefetchNavigationRoute('/templates')"
        @touchstart.passive="prefetchNavigationRoute('/templates')"
      >
        <FeatureIcon name="template-library" size="xs" class="nav-feature-icon" critical />
        模板库
      </router-link>

      <!-- 已登录才显示社区 -->
      <router-link
        v-if="isLoggedIn"
        to="/community"
        class="nav-link"
        :class="{ active: isCommunityActive }"
        @mouseenter="prefetchNavigationRoute('/community')"
        @focus="prefetchNavigationRoute('/community')"
        @touchstart.passive="prefetchNavigationRoute('/community')"
      >
        <FeatureIcon name="community-hub" size="xs" class="nav-feature-icon" critical />
        社区
      </router-link>

      <!-- 已登录才显示成长中心 -->
      <router-link
        v-if="isLoggedIn"
        to="/growth"
        class="nav-link"
        :class="{ active: isGrowthActive }"
        @mouseenter="prefetchNavigationRoute('/growth')"
        @focus="prefetchNavigationRoute('/growth')"
        @touchstart.passive="prefetchNavigationRoute('/growth')"
      >
        <FeatureIcon name="growth-center" size="xs" class="nav-feature-icon" critical />
        成长中心
      </router-link>

      <!-- 已登录才显示 Offer 辅助 -->
      <router-link
        v-if="isLoggedIn"
        to="/offer"
        class="nav-link"
        :class="{ active: isOfferActive }"
        @mouseenter="prefetchNavigationRoute('/offer')"
        @focus="prefetchNavigationRoute('/offer')"
        @touchstart.passive="prefetchNavigationRoute('/offer')"
      >
        <FeatureIcon name="offer-assistant" size="xs" class="nav-feature-icon" critical />
        Offer 辅助
      </router-link>

      <!-- 已登录才显示历史记录下拉菜单 -->
      <div v-if="isLoggedIn" class="history-dropdown-wrapper">
        <el-dropdown trigger="click" @command="handleHistoryCommand">
          <span
            class="nav-link history-trigger"
            :class="{ active: isHistoryActive }"
            @mousedown.prevent
          >
            <FeatureIcon name="history-records" size="xs" class="nav-feature-icon" critical />
            历史记录
            <FeatureIcon name="expand" size="xs" class="dropdown-arrow" />
          </span>
          <template #dropdown>
            <el-dropdown-menu class="history-dropdown-menu">
              <el-dropdown-item
                command="resume"
                :class="{ active: isResumeHistoryActive }"
                @mousedown.prevent
              >
                <FeatureIcon name="resume-score" size="xs" class="dropdown-icon feature-dropdown-icon" />
                简历诊断历史
              </el-dropdown-item>
              <el-dropdown-item
                command="interview"
                :class="{ active: isInterviewHistoryActive }"
                @mousedown.prevent
              >
                <FeatureIcon name="interview-replay" size="xs" class="dropdown-icon feature-dropdown-icon" />
                模拟面试历史
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>

    <!-- 小屏汉堡按钮 -->
    <button class="hamburger-btn motion-hamburger-btn" @click="drawerVisible = true">
      <FeatureIcon name="menu" size="sm" critical />
    </button>

    <div class="header-right">
      <!-- 主题切换按钮 -->
      <el-tooltip :content="themeStore.resolvedTheme === 'dark' ? '切换亮色模式' : '切换暗色模式'" placement="bottom" :show-after="300">
        <button class="theme-toggle" @click="themeStore.toggleTheme()" :aria-label="themeStore.resolvedTheme === 'dark' ? '切换为亮色模式' : '切换为暗色模式'">
          <FeatureIcon :name="themeStore.resolvedTheme === 'light' ? 'dark-mode' : 'light-mode'" size="sm" critical />
        </button>
      </el-tooltip>

      <!-- 已登录状态：显示通知铃铛和头像下拉菜单 -->
      <template v-if="isLoggedIn">
        <!-- 消息通知铃铛 -->
        <template v-if="notificationRealtimeEnabled">
          <el-popover
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
              <button ref="bellRef" type="button" class="notification-bell" aria-label="消息通知">
                <FeatureIcon name="notification-center" size="sm" critical />
                <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </button>
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
              <FeatureIcon name="notification-center" size="lg" class="empty-bell-icon" />
              <p>暂无消息</p>
            </div>

            <div v-else class="panel-list">
              <button
                v-for="item in notificationList"
                :key="item.id"
                type="button"
                class="panel-item"
                :class="{ unread: item.readStatus === 0 }"
                @click="handleNotificationRead(item)"
              >
                <NotificationTypeIcon class="panel-item-icon" :type="item.type" size="sm" halo />
                <span class="panel-item-content">
                  <span class="panel-item-title-row">
                    <span class="panel-item-title">{{ item.title }}</span>
                    <el-tag :type="getNotificationTypeMeta(item.type).tagType" size="small" effect="plain">
                      {{ getNotificationTypeMeta(item.type).label }}
                    </el-tag>
                  </span>
                  <span class="panel-item-text">{{ item.content }}</span>
                  <span class="panel-item-time">{{ formatNotificationTime(item.createTime, { compact: true }) }}</span>
                </span>
                <span v-if="item.readStatus === 0" class="panel-item-dot"></span>
              </button>
            </div>

            <button type="button" class="panel-footer" @click="goToNotificationPage">
              查看全部消息
            </button>
          </div>
          </el-popover>
          <!-- 虚拟触发 tooltip，popover 打开时禁用，避免提示和通知面板叠在一起。 -->
          <el-tooltip content="消息通知" placement="bottom" :show-after="300" :disabled="notificationPopoverVisible" virtual-triggering :virtual-ref="bellRef" />
        </template>

        <!-- 设置中心齿轮按钮：桌面端保持独立入口，避免回退到头像下拉菜单中。 -->
        <el-tooltip content="设置中心" placement="bottom" :show-after="300">
          <router-link
            to="/settings"
            class="header-icon-btn"
            :class="{ active: isSettingsActive }"
            aria-label="设置中心"
            @mouseenter="prefetchNavigationRoute('/settings')"
            @focus="prefetchNavigationRoute('/settings')"
            @touchstart.passive="prefetchNavigationRoute('/settings')"
          >
            <FeatureIcon name="settings" size="sm" critical />
          </router-link>
        </el-tooltip>

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

        <el-dropdown trigger="click" @command="handleCommand" @visible-change="handleUserDropdownVisibleChange">
          <div class="avatar-wrapper avatar-sm">
            <div class="avatar-ring avatar-sm">
              <OptimizedImage :sources="optimizedImages.userAvatar" img-class="avatar-img avatar-sm" alt="用户头像" />
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="user-dropdown-menu">
              <!-- 用户信息区 -->
              <div class="user-info-header">
                <div class="user-info-avatar-wrapper">
                  <OptimizedImage :sources="optimizedImages.userAvatar" alt="用户头像" />
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
                <FeatureIcon name="user-profile" size="xs" class="dropdown-icon feature-dropdown-icon" />
                修改昵称
              </el-dropdown-item>
              <el-dropdown-item command="profile">
                <FeatureIcon name="user-profile" size="xs" class="dropdown-icon feature-dropdown-icon" />
                个人中心
              </el-dropdown-item>
              <!-- 退出登录 -->
              <!-- 会员中心入口：
                   页面已经存在，这里只是在头像下拉菜单中补入口。
                   放在“个人中心”下面、“退出登录”上面，符合账户相关操作的使用顺序。 -->
              <el-dropdown-item command="membership">
                <FeatureIcon name="membership-center" size="xs" class="dropdown-icon feature-dropdown-icon" />
                会员中心
              </el-dropdown-item>
              <el-dropdown-item
                command="activity"
                @mouseenter="prefetchNavigationRoute('/community/my')"
                @focus="prefetchNavigationRoute('/community/my')"
                @touchstart.passive="prefetchNavigationRoute('/community/my')"
              >
                <FeatureIcon name="community-activity" size="xs" class="dropdown-icon feature-dropdown-icon" />
                个人动态中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" class="logout-item">
                <FeatureIcon name="back" size="xs" class="dropdown-icon feature-dropdown-icon" />
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
      :append-to-body="true"
    >
      <nav class="mobile-nav motion-mobile-nav">
        <router-link
          to="/"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="home-dashboard" size="sm" />
          首页
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/resume/upload"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/resume/upload')"
          @focus="prefetchNavigationRoute('/resume/upload')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="resume-upload" size="sm" />
          简历诊断
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/interview/entry"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/interview/entry')"
          @focus="prefetchNavigationRoute('/interview/entry')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="mock-interview" size="sm" />
          模拟面试
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/templates"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/templates')"
          @focus="prefetchNavigationRoute('/templates')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="template-library" size="sm" />
          模板库
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/community"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/community')"
          @focus="prefetchNavigationRoute('/community')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="community-hub" size="sm" />
          社区
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/growth"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/growth')"
          @focus="prefetchNavigationRoute('/growth')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="growth-center" size="sm" />
          成长中心
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/offer"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/offer')"
          @focus="prefetchNavigationRoute('/offer')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="offer-assistant" size="sm" />
          Offer 辅助
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/resume/history"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="history-records" size="sm" />
          简历诊断历史
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/interview/history"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="interview-replay" size="sm" />
          模拟面试历史
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/dashboard"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="user-profile" size="sm" />
          个人中心
        </router-link>
        <router-link
          v-if="isLoggedIn && notificationRealtimeEnabled"
          to="/notifications"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="notification-center" size="sm" />
          消息通知
          <span v-if="unreadCount > 0" class="mobile-unread-badge">{{ unreadCount }}</span>
        </router-link>
        <router-link
          v-if="isLoggedIn"
          to="/settings"
          class="mobile-nav-link"
          @touchstart.passive="prefetchNavigationRoute('/settings')"
          @focus="prefetchNavigationRoute('/settings')"
          @click="drawerVisible = false"
        >
          <FeatureIcon name="settings" size="sm" />
          设置中心
        </router-link>
        <!-- 移动端主题切换 -->
        <button class="mobile-nav-link theme-toggle-mobile" @click="themeStore.toggleTheme(); drawerVisible = false">
          <FeatureIcon
            :name="themeStore.resolvedTheme === 'light' ? 'dark-mode' : 'light-mode'"
            size="sm"
            class="theme-icon"
          />
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
            <OptimizedImage :sources="optimizedImages.userAvatar" alt="用户头像" />
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
import FeatureIcon from "@/components/common/FeatureIcon.vue";
import OptimizedImage from "@/components/common/OptimizedImage.vue";
import NotificationTypeIcon from "@/components/notification/NotificationTypeIcon.vue";
import { formatNotificationTime, getNotificationTypeMeta, isAdminAnnouncementType } from "@/utils/notificationMeta";
import { optimizedImages } from "@/utils/optimizedImages";
import { prefetchUserRoute, warmupHighFrequencyUserRoutes } from "@/router/routeLoaders";
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

const prefetchNavigationRoute = (path) => {
  prefetchUserRoute(path)?.catch((error) => {
    console.debug("导航路由预取失败", path, error);
  });
};

const handleUserDropdownVisibleChange = (visible) => {
  if (visible) {
    prefetchNavigationRoute("/community/my");
  }
};

// ===== 消息通知相关状态 =====
/** 未读通知数量 */
const unreadCount = ref(0);
/** 通知列表（最近10条） */
const notificationList = ref([]);
/** 通知面板是否展开 */
const notificationPopoverVisible = ref(false);
/** 铃铛 DOM 引用，供虚拟触发 tooltip 使用 */
const bellRef = ref(null);
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

// 社区激活状态
const isCommunityActive = computed(() => route.path.startsWith("/community"));

// 成长中心激活状态
const isGrowthActive = computed(() => route.path === "/growth");

// Offer 辅助激活状态
const isOfferActive = computed(() => route.path.startsWith("/offer"));

// 设置中心激活状态
const isSettingsActive = computed(() => route.path === "/settings");

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
  } else if (command === "activity") {
    router.push("/community/my");
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

  if (isLoggedIn.value) {
    warmupHighFrequencyUserRoutes();
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
  height: var(--header-height, 82px);
  background: var(--bg-header);
  border-bottom: 1px solid rgba(255, 140, 66, 0.14);
  box-shadow: 0 10px 28px rgba(112, 62, 20, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 1000;
  --header-motion-ease: cubic-bezier(0.22, 1, 0.36, 1);
  --header-dropdown-icon-size: 28px;
}

.header-left {
  display: flex;
  align-items: center;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  padding: 6px 16px 6px 8px;
  border-radius: 999px;
  transition:
    background-color 220ms var(--header-motion-ease),
    box-shadow 220ms var(--header-motion-ease),
    transform 220ms var(--header-motion-ease);
  -webkit-tap-highlight-color: transparent;
}

.brand-mark:hover {
  background-color: var(--orange-light-bg);
  box-shadow: 0 10px 26px rgba(255, 140, 66, 0.12);
  transform: translateY(-1px);
}

.logo-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: clamp(44px, 5vw, 70px);
  height: clamp(44px, 5vw, 70px);
  flex-shrink: 0;
}

.logo-img {
  height: 100%;
  width: 100%;
  object-fit: contain;
}

.logo-box :deep(.logo-img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-text {
  font-size: clamp(14px, 1.56vw, 30px);
  font-weight: 800;
  color: var(--text-title);
  letter-spacing: -0.3px;
  text-shadow: 0 0.5px 0 rgba(0, 0, 0, 0.08);
  overflow: hidden;
  white-space: nowrap;
  margin: 0;
  margin-left: -12px;
  line-height: 1;
  transition: font-size 0.3s ease;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.nav-link {
  position: relative;
  isolation: isolate;
  padding: 9px 14px;
  font-size: 14px;
  color: var(--text-body);
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 999px;
  transition:
    color 180ms var(--header-motion-ease),
    border-color 180ms var(--header-motion-ease),
    box-shadow 220ms var(--header-motion-ease),
    transform 220ms var(--header-motion-ease);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  -webkit-tap-highlight-color: transparent;
}

.nav-link::before {
  content: "";
  position: absolute;
  inset: 2px;
  z-index: -1;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(255, 245, 236, 0.96), rgba(255, 255, 255, 0.82));
  box-shadow: 0 10px 24px rgba(255, 140, 66, 0.1);
  opacity: 0;
  transform: scale(0.94);
  transition:
    opacity 220ms var(--header-motion-ease),
    transform 220ms var(--header-motion-ease);
}

.nav-feature-icon {
  margin-right: 1px;
  transition:
    opacity 180ms var(--header-motion-ease),
    transform 220ms var(--header-motion-ease);
}

.nav-link :deep(.feature-icon.size-xs) {
  width: 24px;
  height: 24px;
}

.nav-link:hover {
  color: var(--orange-main);
  border-color: rgba(255, 140, 66, 0.18);
  transform: translateY(-1px);
}

.nav-link:hover::before,
.nav-link.active::before {
  opacity: 1;
  transform: scale(1);
}

.nav-link:hover .nav-feature-icon,
.nav-link.active .nav-feature-icon {
  opacity: 1;
  transform: translateY(-1px) scale(1.08);
}

.nav-link.active {
  color: var(--orange-main);
  border-color: rgba(255, 140, 66, 0.22);
  box-shadow: 0 12px 26px rgba(255, 140, 66, 0.1);
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.login-link {
  font-size: 14px;
  color: var(--orange-main);
  text-decoration: none;
  border: 1px solid rgba(255, 140, 66, 0.18);
  border-radius: 999px;
  padding: 9px 16px;
  background: linear-gradient(135deg, rgba(255, 246, 238, 0.98), rgba(255, 255, 255, 0.78));
  box-shadow: 0 12px 24px rgba(255, 140, 66, 0.12);
  transition:
    box-shadow 220ms var(--header-motion-ease),
    transform 220ms var(--header-motion-ease);
}

.login-link:hover {
  box-shadow: 0 16px 30px rgba(255, 140, 66, 0.18);
  transform: translateY(-1px);
}

/* 历史记录下拉菜单 */
.history-dropdown-wrapper {
  position: relative;
}

.history-trigger,
.history-trigger :deep(*) {
  cursor: pointer;
}

.history-trigger {
  user-select: none;
}

.dropdown-arrow {
  width: 20px;
  height: 20px;
  transition: transform 220ms var(--header-motion-ease);
}

.feature-dropdown-icon {
  margin-right: 8px;
}

.history-dropdown-wrapper:hover .dropdown-arrow {
  transform: rotate(180deg);
}

/* 小屏汉堡按钮 */
.hamburger-btn {
  display: none;
  position: relative;
  z-index: 2;
  flex: 0 0 46px;
  background: linear-gradient(135deg, rgba(255, 246, 238, 0.96), rgba(255, 255, 255, 0.8));
  border: 1px solid rgba(255, 140, 66, 0.16);
  cursor: pointer;
  width: 46px;
  height: 46px;
  padding: 0;
  border-radius: 999px;
  box-shadow: 0 12px 24px rgba(255, 140, 66, 0.12);
  transition:
    box-shadow 220ms var(--header-motion-ease),
    transform 160ms var(--header-motion-ease),
    border-color 220ms var(--header-motion-ease);
  -webkit-tap-highlight-color: transparent;
}

.hamburger-btn:hover {
  border-color: rgba(255, 140, 66, 0.28);
  box-shadow: 0 16px 30px rgba(255, 140, 66, 0.18);
  transform: translateY(-1px);
}

.hamburger-btn:active {
  transform: translateY(0) scale(0.96);
}

.hamburger-btn :deep(.feature-icon) {
  width: 30px;
  height: 30px;
}

/* 移动端导航 */
.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.mobile-nav-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  font-size: 15px;
  color: var(--text-body);
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 16px;
  transition:
    background-color 180ms var(--header-motion-ease),
    border-color 180ms var(--header-motion-ease),
    color 180ms var(--header-motion-ease),
    transform 180ms var(--header-motion-ease);
  -webkit-tap-highlight-color: transparent;
}

.mobile-nav-link:hover {
  background-color: var(--orange-light-bg);
  border-color: rgba(255, 140, 66, 0.16);
  color: var(--orange-main);
  transform: translateX(4px);
}

.mobile-nav-link.router-link-active {
  background-color: var(--orange-light-bg);
  border-color: rgba(255, 140, 66, 0.2);
  color: var(--orange-main);
  font-weight: 700;
}

.mobile-nav-link :deep(.feature-icon) {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  transition: transform 180ms var(--header-motion-ease);
}

.mobile-nav-link:hover :deep(.feature-icon) {
  transform: scale(1.08);
}

.motion-desktop-nav > .nav-link,
.motion-desktop-nav > .history-dropdown-wrapper {
  opacity: 1;
}

.motion-mobile-nav .mobile-nav-link {
  opacity: 1;
}

/* 响应式断点 */
/* 中屏及以下：≤1279px 使用汉堡菜单 */
@media (max-width: 1279px) {
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
}

/* ===== 消息通知铃铛 ===== */
.notification-bell {
  appearance: none;
  padding: 0;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  cursor: pointer;
  border: 1px solid transparent;
  background: transparent;
  color: inherit;
  font: inherit;
  transition:
    background-color 180ms var(--header-motion-ease),
    border-color 180ms var(--header-motion-ease),
    transform 160ms var(--header-motion-ease);
  margin-right: 4px;
  -webkit-tap-highlight-color: transparent;
}

.notification-bell:hover {
  background-color: var(--orange-light-bg);
  border-color: rgba(255, 140, 66, 0.14);
  transform: translateY(-1px);
}

.notification-bell:active {
  transform: translateY(0) scale(0.94);
}

.notification-bell :deep(.feature-icon) {
  width: 30px;
  height: 30px;
  transition: transform 180ms var(--header-motion-ease);
}

.notification-bell:hover :deep(.feature-icon) {
  transform: scale(1.08);
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
  appearance: none;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 160ms var(--header-motion-ease),
    box-shadow 180ms var(--header-motion-ease),
    transform 180ms var(--header-motion-ease);
  position: relative;
}

.panel-item:hover {
  background-color: var(--bg-elevated);
  box-shadow: inset 3px 0 0 rgba(255, 140, 66, 0.14);
  transform: translateX(2px);
}

.panel-item.unread {
  background-color: var(--bg-card-hover);
}

.panel-item-icon {
  width: var(--header-dropdown-icon-size);
  height: var(--header-dropdown-icon-size);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition:
    filter 180ms var(--header-motion-ease),
    transform 180ms var(--header-motion-ease);
}

.panel-item-icon svg {
  width: var(--header-dropdown-icon-size);
  height: var(--header-dropdown-icon-size);
}

.panel-item:hover .panel-item-icon {
  filter: drop-shadow(0 8px 14px rgba(255, 140, 66, 0.16));
  transform: translateY(-1px) scale(1.04);
}

.panel-item-content {
  display: block;
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
  display: block;
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
  display: block;
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
  appearance: none;
  display: block;
  width: 100%;
  text-align: center;
  padding: 10px 16px;
  font: inherit;
  font-size: 13px;
  color: var(--orange-main);
  border: 0;
  border-top: 1px solid var(--border-divider);
  background: transparent;
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
  width: 42px;
  height: 42px;
  border-radius: 50%;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  transition:
    background-color 180ms var(--header-motion-ease),
    border-color 180ms var(--header-motion-ease),
    color 180ms var(--header-motion-ease),
    transform 160ms var(--header-motion-ease);
  margin-right: 4px;
  color: var(--text-body);
  -webkit-tap-highlight-color: transparent;
}

.theme-toggle:hover {
  background-color: var(--orange-light-bg);
  border-color: rgba(255, 140, 66, 0.14);
  color: var(--orange-main);
  transform: translateY(-1px);
}

.theme-toggle:active {
  transform: translateY(0) scale(0.92);
}

.theme-toggle :deep(.feature-icon) {
  width: 30px;
  height: 30px;
  transition: transform 180ms var(--header-motion-ease);
}

.theme-toggle :deep(.feature-icon img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.theme-toggle:hover :deep(.feature-icon) {
  transform: rotate(-8deg) scale(1.06);
}

/* 设置中心图标按钮 */
.header-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  text-decoration: none;
  transition:
    background-color 180ms var(--header-motion-ease),
    border-color 180ms var(--header-motion-ease),
    color 180ms var(--header-motion-ease),
    transform 160ms var(--header-motion-ease);
  color: var(--text-body);
  margin-right: 12px;
  -webkit-tap-highlight-color: transparent;
}

.header-icon-btn:hover {
  background-color: var(--orange-light-bg);
  border-color: rgba(255, 140, 66, 0.14);
  color: var(--orange-main);
  transform: translateY(-1px);
}

.header-icon-btn.active {
  background-color: var(--orange-light-bg);
  border-color: rgba(255, 140, 66, 0.18);
}

.header-icon-btn.active :deep(.feature-icon) {
  color: var(--text-title);
  transform: scale(1.06);
}

.header-icon-btn:active {
  transform: translateY(0) scale(0.92);
}

.header-icon-btn :deep(.feature-icon) {
  width: 30px;
  height: 30px;
  transition: transform 180ms var(--header-motion-ease);
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
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}

@media (prefers-reduced-motion: reduce) {
  .brand-mark,
  .nav-link,
  .nav-link::before,
  .nav-feature-icon,
  .dropdown-arrow,
  .hamburger-btn,
  .mobile-nav-link,
  .mobile-nav-link :deep(.feature-icon),
  .notification-bell,
  .notification-bell :deep(.feature-icon),
  .panel-item,
  .panel-item-icon,
  .theme-toggle,
  .theme-toggle :deep(.feature-icon),
  .header-icon-btn,
  .header-icon-btn :deep(.feature-icon),
  .login-link {
    transition-duration: 0.01ms;
  }

  .brand-mark:hover,
  .nav-link:hover,
  .nav-link:hover .nav-feature-icon,
  .nav-link.active .nav-feature-icon,
  .hamburger-btn:hover,
  .hamburger-btn:active,
  .mobile-nav-link:hover,
  .mobile-nav-link:hover :deep(.feature-icon),
  .notification-bell:hover,
  .notification-bell:active,
  .notification-bell:hover :deep(.feature-icon),
  .panel-item:hover,
  .panel-item:hover .panel-item-icon,
  .theme-toggle:hover,
  .theme-toggle:active,
  .theme-toggle:hover :deep(.feature-icon),
  .header-icon-btn:hover,
  .header-icon-btn:active,
  .header-icon-btn.active :deep(.feature-icon),
  .login-link:hover {
    transform: none;
  }
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
  .app-header {
    height: var(--header-height, 64px);
    padding: 0 12px;
  }
  .brand-text {
    display: none;
  }
  .brand-mark {
    padding: 4px 10px 4px 2px;
    gap: 6px;
  }

  .header-right {
    gap: 0;
  }

  .theme-toggle,
  .header-icon-btn,
  .notification-bell {
    width: 40px;
    height: 40px;
  }

  .theme-toggle :deep(.feature-icon),
  .header-icon-btn :deep(.feature-icon),
  .notification-bell :deep(.feature-icon) {
    width: 28px;
    height: 28px;
  }

  .hamburger-btn {
    width: 44px;
    height: 44px;
    flex-basis: 44px;
  }

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
  .app-header {
    height: var(--header-height, 60px);
    padding: 0 10px;
  }
  .brand-mark {
    padding: 3px 8px 3px 2px;
    gap: 5px;
  }

  .header-icon-btn {
    display: none;
  }

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
