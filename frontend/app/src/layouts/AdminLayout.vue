<template>
  <div class="admin-layout">
    <header class="admin-header">
      <img :src="logoUrl" alt="logo" class="header-logo" />
      <div class="admin-header-title">offerCat 管理端</div>
      <div class="header-spacer"></div>
      <div class="admin-user-info">
        <el-icon><User /></el-icon>
        <span class="admin-user-name">
          {{ adminStore.adminInfo?.nickname || adminStore.adminInfo?.username || "管理员" }}
        </span>
      </div>
      <el-button text type="danger" @click="handleLogout" class="logout-btn">
        <el-icon><SwitchButton /></el-icon>
        退出
      </el-button>
    </header>

    <div class="admin-body">
      <aside class="admin-sidebar">
        <nav class="admin-nav">
          <section
            v-for="group in navGroups"
            :key="group.groupKey"
            class="nav-group"
          >
            <h3 class="nav-group-title">{{ group.groupLabel }}</h3>
            <RouterLink
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              class="admin-nav-item"
              :class="{ active: isNavActive(item.path) }"
              @mouseenter="prefetchAdminNavigationRoute(item.path)"
              @focus="prefetchAdminNavigationRoute(item.path)"
              @touchstart.passive="prefetchAdminNavigationRoute(item.path)"
            >
              <el-icon class="nav-icon">
                <component :is="item.icon" />
              </el-icon>
              <span class="nav-label">{{ item.label }}</span>
            </RouterLink>
          </section>
        </nav>
      </aside>

      <main class="admin-content">
        <div v-if="showAdminRouteLoading" class="admin-route-loading-bar" aria-hidden="true"></div>
        <div
          v-if="showAdminRouteLoading"
          class="admin-route-loading-placeholder"
          role="status"
          aria-live="polite"
        >
          <div class="admin-route-loading-placeholder-card">
            <div class="admin-route-loading-placeholder-title">正在打开管理模块</div>
            <div class="admin-route-loading-placeholder-line primary"></div>
            <div class="admin-route-loading-placeholder-line"></div>
            <div class="admin-route-loading-placeholder-line short"></div>
          </div>
        </div>
        <RouterView v-slot="{ Component }">
          <Transition name="admin-page-fade" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Bell,
  ChatLineRound,
  Coin,
  Collection,
  DataAnalysis,
  Document,
  Flag,
  List,
  Odometer,
  Setting,
  SwitchButton,
  Trophy,
  User,
  UserFilled,
} from "@element-plus/icons-vue";
import { useAdminUserStore } from "@/stores/adminUser";
import logoUrl from "@/assets/logo.png";
import { showAdminError, showAdminSuccess } from "@/utils/adminFeedback";
import { prefetchAdminRoute, warmupHighFrequencyAdminRoutes } from "@/router/routeLoaders";

const route = useRoute();
const router = useRouter();
const adminStore = useAdminUserStore();
const showAdminRouteLoading = ref(false);
let adminRouteLoadingTimer = null;
let adminRouteLoadingHideTimer = null;
let removeAdminRouteBeforeGuard = null;
let removeAdminRouteAfterGuard = null;
let removeAdminRouteErrorGuard = null;
let adminWarmupHandle = null;

// 管理端分组导航：统一信息架构层次，降低模块扩展后的认知成本。
const navGroups = computed(() => [
  {
    groupKey: "insight",
    groupLabel: "看板与监控",
    items: [
      { path: "/admin/dashboard", label: "数据看板", icon: DataAnalysis },
      { path: "/admin/monitor", label: "监控总览", icon: Odometer }
    ]
  },
  {
    groupKey: "operation",
    groupLabel: "运营管理",
    items: [
      { path: "/admin/users", label: "用户权益", icon: UserFilled },
      { path: "/admin/community", label: "社区审核", icon: Flag },
      { path: "/admin/audit-logs", label: "审计日志", icon: List },
      { path: "/admin/notifications", label: "通知公告", icon: Bell },
      { path: "/admin/feedback", label: "问题反馈", icon: ChatLineRound },
      { path: "/admin/version-logs", label: "版本日志", icon: Collection }
    ]
  },
  {
    groupKey: "billing",
    groupLabel: "商业管理",
    items: [
      { path: "/admin/membership/plans", label: "会员套餐", icon: Coin },
      { path: "/admin/membership/orders", label: "订单管理", icon: Document }
    ]
  },
  {
    groupKey: "config",
    groupLabel: "配置中心",
    items: [
      { path: "/admin/job-roles", label: "岗位配置", icon: Document },
      { path: "/admin/prompts", label: "Prompt 管理", icon: ChatLineRound },
      { path: "/admin/ai-engines", label: "AI 引擎", icon: Setting },
      { path: "/admin/growth-config", label: "成长配置", icon: Trophy }
    ]
  }
]);

/**
 * 导航高亮判断。
 * 作用：进入子模块后侧栏菜单保持正确高亮。
 * @param {string} navPath
 * @returns {boolean}
 */
const isNavActive = (navPath) => route.path.startsWith(navPath);

const isAdminContentNavigation = (to, from) => (
  to.fullPath !== from.fullPath
  && to.path.startsWith("/admin/")
  && from.path.startsWith("/admin/")
  && to.path !== "/admin/login"
);

const startAdminRouteLoadingFeedback = () => {
  window.clearTimeout(adminRouteLoadingTimer);
  window.clearTimeout(adminRouteLoadingHideTimer);
  showAdminRouteLoading.value = false;
  adminRouteLoadingTimer = window.setTimeout(() => {
    showAdminRouteLoading.value = true;
  }, 120);
};

const stopAdminRouteLoadingFeedback = () => {
  window.clearTimeout(adminRouteLoadingTimer);
  adminRouteLoadingHideTimer = window.setTimeout(() => {
    showAdminRouteLoading.value = false;
  }, 180);
};

/**
 * 管理端导航预取：只在 hover/focus/touch 明确表达意图时加载目标页面 chunk。
 * 这样可以缩短点击后的白屏空窗，又避免登录后台时一次性预热所有管理页。
 * @param {string} path
 */
const prefetchAdminNavigationRoute = (path) => {
  prefetchAdminRoute(path)?.catch(() => {});
};

/**
 * 页面刷新后如果 Pinia 内存态丢失，主动补拉管理员信息。
 * 作用：确保头部用户名与权限状态不依赖单页内存。
 */
onMounted(async () => {
  removeAdminRouteBeforeGuard = router.beforeEach((to, from) => {
    if (isAdminContentNavigation(to, from)) {
      startAdminRouteLoadingFeedback();
    }
  });
  removeAdminRouteAfterGuard = router.afterEach(async () => {
    await nextTick();
    stopAdminRouteLoadingFeedback();
  });
  removeAdminRouteErrorGuard = router.onError(() => {
    stopAdminRouteLoadingFeedback();
  });

  // 后台壳加载完成后在浏览器空闲期预热高频模块，减少首次点击时的冷加载白屏。
  adminWarmupHandle = warmupHighFrequencyAdminRoutes();

  if (adminStore.adminInfo) return;
  try {
    await adminStore.fetchAdminInfo();
  } catch (error) {
    adminStore.doAdminLogout();
    showAdminError(error?.message || "管理员登录态已失效，请重新登录");
    router.push("/admin/login");
  }
});

/**
 * 管理端退出逻辑。
 * 说明：仅清理管理端会话，不影响用户端 token。
 */
const handleLogout = () => {
  adminStore.doAdminLogout();
  // 统一反馈：显式告知管理员当前会话已退出，避免误判为跳转异常。
  showAdminSuccess("已退出管理端");
  router.push("/admin/login");
};

onBeforeUnmount(() => {
  removeAdminRouteBeforeGuard?.();
  removeAdminRouteAfterGuard?.();
  removeAdminRouteErrorGuard?.();
  window.clearTimeout(adminRouteLoadingTimer);
  window.clearTimeout(adminRouteLoadingHideTimer);
  if (adminWarmupHandle !== null) {
    // 同一 handle 可能来自 requestIdleCallback 或 setTimeout，卸载时两类调度都清理一次。
    if (typeof window.cancelIdleCallback === "function") {
      window.cancelIdleCallback(adminWarmupHandle);
    }
    window.clearTimeout(adminWarmupHandle);
  }
});
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

:global(html[data-theme="dark"] .admin-layout) {
  --bg-page: #171D26;
  --bg-card: #202838;
  --bg-card-hover: #263044;
  --bg-input: #263044;
  --bg-header: #1B2230;
  --bg-elevated: #222B3A;
  --text-title: #F2F5FA;
  --text-body: #C5CDD8;
  --text-muted: #8994A6;
  --border-card: rgba(148, 163, 184, 0.22);
  --border-input: rgba(148, 163, 184, 0.28);
  --border-divider: rgba(148, 163, 184, 0.16);
  --shadow-card: 0 2px 12px rgba(0, 0, 0, 0.28);
  --shadow-hover: 0 4px 16px rgba(0, 0, 0, 0.36);
  /* 管理端局部接管 Element Plus 暗色变量，避免用户端暖棕主题扩散到运营后台组件。 */
  --el-bg-color: #171D26;
  --el-bg-color-overlay: #202838;
  --el-bg-color-page: #171D26;
  --el-text-color-primary: #F2F5FA;
  --el-text-color-regular: #C5CDD8;
  --el-text-color-secondary: #8994A6;
  --el-text-color-placeholder: #6F7A8C;
  --el-border-color: rgba(148, 163, 184, 0.22);
  --el-border-color-light: rgba(148, 163, 184, 0.16);
  --el-border-color-lighter: rgba(148, 163, 184, 0.1);
  --el-fill-color-blank: #202838;
  --el-fill-color-light: #263044;
}

.admin-header {
  height: 60px;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-divider);
  background: var(--bg-header);
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-logo {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: contain;
}

.admin-header-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-title);
}

.header-spacer {
  flex: 1;
}

.admin-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-elevated);
  border-radius: 8px;
  color: var(--text-body);
}

.admin-user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-title);
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.admin-body {
  display: flex;
  flex: 1;
}

.admin-sidebar {
  width: 200px;
  background: linear-gradient(180deg, #2c3e50 0%, #1a252f 100%);
  color: #fff;
  padding: 16px 12px;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 60px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-group-title {
  margin: 8px 10px 4px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.4px;
}

.admin-nav-item {
  color: rgba(255, 255, 255, 0.75);
  text-decoration: none;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.admin-nav-item.active {
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.35);
}

.nav-icon {
  font-size: 18px;
}

.nav-label {
  flex: 1;
}

.admin-content {
  flex: 1;
  position: relative;
  padding: 20px 24px;
  min-width: 0;
  overflow-x: hidden;
}

.admin-route-loading-bar {
  position: absolute;
  top: 0;
  left: 24px;
  right: 24px;
  z-index: 20;
  height: 2px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(230, 126, 34, 0.14);
}

.admin-route-loading-bar::before {
  content: "";
  display: block;
  width: 38%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(230, 126, 34, 0.08), #e67e22, rgba(243, 156, 18, 0.28));
  animation: admin-route-loading-sweep 0.9s ease-in-out infinite;
}

.admin-route-loading-placeholder {
  position: absolute;
  inset: 22px 24px auto;
  z-index: 15;
  pointer-events: none;
}

.admin-route-loading-placeholder-card {
  width: min(420px, 100%);
  padding: 16px;
  border: 1px solid var(--border-card);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.admin-route-loading-placeholder-title {
  margin-bottom: 14px;
  color: var(--text-title);
  font-size: 14px;
  font-weight: 600;
}

.admin-route-loading-placeholder-line {
  height: 10px;
  margin-top: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--bg-elevated), var(--bg-card-hover), var(--bg-elevated));
  background-size: 180% 100%;
  animation: admin-route-placeholder-pulse 1.1s ease-in-out infinite;
}

.admin-route-loading-placeholder-line.primary {
  width: 82%;
}

.admin-route-loading-placeholder-line.short {
  width: 46%;
}

.admin-page-fade-enter-active,
.admin-page-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.admin-page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.admin-page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@keyframes admin-route-loading-sweep {
  from {
    transform: translateX(-110%);
  }
  to {
    transform: translateX(280%);
  }
}

@keyframes admin-route-placeholder-pulse {
  from {
    background-position: 100% 0;
  }
  to {
    background-position: -80% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .admin-page-fade-enter-active,
  .admin-page-fade-leave-active {
    transition-duration: 0.01ms;
  }

  .admin-page-fade-enter-from,
  .admin-page-fade-leave-to {
    opacity: 1;
    transform: none;
  }

  .admin-route-loading-bar::before {
    animation: none;
    width: 100%;
  }

  .admin-route-loading-placeholder-line {
    animation: none;
  }
}


@media (max-width: 768px) {
  .admin-header {
    padding: 0 16px;
  }

  .admin-sidebar {
    width: 60px;
    padding: 12px 8px;
  }

  .nav-label {
    display: none;
  }

  .nav-group-title {
    display: none;
  }

  .admin-nav-item {
    justify-content: center;
    padding: 12px;
  }

  .admin-content {
    padding: 16px;
  }

  .admin-route-loading-bar {
    left: 16px;
    right: 16px;
  }

  .admin-route-loading-placeholder {
    inset: 18px 16px auto;
  }
}
</style>
