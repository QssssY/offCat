<template>
  <header class="app-header">
    <div class="header-left">
      <img src="@/assets/logo.jpg" class="logo-img" />
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
      <!-- 已登录状态：显示头像和下拉菜单 -->
      <template v-if="isLoggedIn">
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="avatar-wrapper avatar-sm">
            <div class="avatar-ring avatar-sm">
              <img src="@/assets/user.png" class="avatar-img avatar-sm" />
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu class="user-dropdown-menu">
              <!-- 用户信息区 -->
              <div class="user-info-header">
                <div class="user-info-avatar-wrapper">
                  <img src="@/assets/user.png" />
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
      </nav>
    </el-drawer>
  </header>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useUserStore } from "@/stores/user";
import { ElMessage } from "element-plus";
import { removeToken } from "@/utils/auth";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const drawerVisible = ref(false);
const isLoggedIn = computed(() => userStore.isLoggedIn());
const username = computed(() => userStore.userInfo?.username || "用户");

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

// 处理头像下拉菜单命令
const handleCommand = (command) => {
  if (command === "profile") {
    router.push("/dashboard");
  } else if (command === "membership") {
    // 点击“会员中心”后跳转到已经存在的 /membership 路由。
    router.push("/membership");
  } else if (command === "logout") {
    // 原有退出登录逻辑不能被破坏：
    // 这里仍然保持“清 token -> 清 Pinia 用户信息 -> 返回首页”的顺序，
    // 这样头部和页面登录态才能立即响应式更新。
    localStorage.removeItem("token");
    removeToken();
    userStore.clearUserInfo();
    ElMessage.success("已退出登录");
    router.push("/");
  }
};
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background-color: #ffffff;
  border-bottom: 1px solid #ff8c42;
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
  color: #333333;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  padding: 8px 16px;
  font-size: 14px;
  color: #666666;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link:hover {
  color: #ff8c42;
}

.nav-link.active {
  color: #ff8c42;
  border-bottom-color: #ff8c42;
}

.header-right {
  display: flex;
  align-items: center;
}

.login-link {
  font-size: 14px;
  color: #ff8c42;
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
  background-color: #fff8f3;
}

.hamburger-btn svg {
  width: 22px;
  height: 22px;
  color: #333;
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
  color: #555;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.15s;
}

.mobile-nav-link:hover {
  background-color: #fff8f3;
  color: #ff8c42;
}

.mobile-nav-link.router-link-active {
  background-color: #fff8f3;
  color: #ff8c42;
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
</style>
