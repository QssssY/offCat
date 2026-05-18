<template>
  <div class="admin-layout">
    <header class="admin-header">
      <img :src="logoUrl" alt="logo" class="header-logo" />
      <div class="admin-header-title">智能简历助手管理端</div>
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
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Bell,
  ChatLineRound,
  Coin,
  Collection,
  DataAnalysis,
  Document,
  List,
  Odometer,
  Setting,
  SwitchButton,
  Trophy,
  User,
  UserFilled,
} from "@element-plus/icons-vue";
import { useAdminUserStore } from "@/stores/adminUser";
import logoUrl from "@/assets/logo.jpg";
import { showAdminError, showAdminSuccess } from "@/utils/adminFeedback";

const route = useRoute();
const router = useRouter();
const adminStore = useAdminUserStore();

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
      { path: "/admin/audit-logs", label: "审计日志", icon: List },
      { path: "/admin/notifications", label: "通知公告", icon: Bell },
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

/**
 * 页面刷新后如果 Pinia 内存态丢失，主动补拉管理员信息。
 * 作用：确保头部用户名与权限状态不依赖单页内存。
 */
onMounted(async () => {
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
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
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
  transition: all 0.2s ease;
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
  padding: 20px 24px;
  min-width: 0;
  overflow-x: hidden;
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
}
</style>
