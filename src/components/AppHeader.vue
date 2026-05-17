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
                <div class="panel-item-icon" :class="`type-${item.type}`">
                  <svg v-if="item.type === 'resume'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                  </svg>
                  <svg v-else-if="item.type === 'polish'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                  </svg>
                  <svg v-else-if="item.type === 'interview'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="9" cy="7" r="4" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="12" y1="8" x2="12" y2="12" />
                    <line x1="12" y1="16" x2="12.01" y2="16" />
                  </svg>
                </div>
                <div class="panel-item-content">
                  <div class="panel-item-title">{{ item.title }}</div>
                  <div class="panel-item-text">{{ item.content }}</div>
                  <div class="panel-item-time">{{ formatNotifTime(item.createTime) }}</div>
                </div>
                <div v-if="item.readStatus === 0" class="panel-item-dot"></div>
              </div>
            </div>

            <div class="panel-footer" @click="goToNotificationPage">
              查看全部消息
            </div>
          </div>
        </el-popover>

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
              <el-dropdown-item command="nickname">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M17 3a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2-2V7a2 2 0 0 1 2-2 2 2 0 0 1-2-2V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2-2V7a2 2 0 0 1 2-2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1 2 2V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2" transform="scale(0.7) translate(4, 4)"/>
                </svg>
                修改昵称
              </el-dropdown-item>
              <el-dropdown-item command="password">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item command="securityQuestion">
                <svg
                  class="dropdown-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
                修改安全问题
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
          v-if="isLoggedIn"
          to="/notifications"
          class="mobile-nav-link"
          @click="drawerVisible = false"
        >
          消息通知
          <span v-if="unreadCount > 0" class="mobile-unread-badge">{{ unreadCount }}</span>
        </router-link
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
      title="修改昵称"
      width="440px"
      :close-on-click-modal="false"
      class="nickname-dialog"
      :show-close="true"
      :append-to-body="true"
    >
      <div class="nickname-modal-content">
        <div class="nickname-icon-wrapper">
          <svg class="nickname-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        </div>
        <div class="nickname-header">
          <h3 class="nickname-title">设置你的专属昵称</h3>
          <p class="nickname-desc">昵称将用于个人中心、会员中心等展示</p>
        </div>
        <div class="nickname-current">
          <span class="current-label">当前昵称</span>
          <span class="current-value">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未设置' }}</span>
        </div>
        <div class="nickname-input-wrapper">
          <el-input
            v-model="nicknameForm.nickname"
            placeholder="请输入新昵称"
            maxlength="12"
            show-word-limit
            clearable
            size="large"
            class="nickname-input"
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M17 3a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2-2V7a2 2 0 0 1 2-2 2 2 0 0 1-2-2V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 1-2-2V7a2 2 0 0 1 2-2 2 2 0 0 1 2 2v2a2 2 0 0 1-2 2"/>
              </svg>
            </template>
          </el-input>
          <div class="input-tips">2-12个字符，可使用中文、字母、数字</div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="nicknameDialogVisible = false">取消</el-button>
          <el-button
            size="large"
            type="primary"
            @click="saveNickname"
            :disabled="!nicknameForm.nickname || nicknameForm.nickname.trim().length < 2"
            class="save-btn"
          >
            保存修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="440px"
      :close-on-click-modal="false"
      :show-close="true"
      :append-to-body="true"
      @closed="resetPasswordForm"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
        size="large"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码（6-100位）"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="passwordDialogVisible = false">取消</el-button>
          <el-button
            size="large"
            type="primary"
            @click="handlePasswordSave"
            :loading="passwordSaving"
            class="save-btn"
          >
            确认修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 修改安全问题弹窗 -->
    <el-dialog
      v-model="securityDialogVisible"
      title="修改安全问题"
      width="440px"
      :close-on-click-modal="false"
      :show-close="true"
      :append-to-body="true"
      @closed="resetSecurityForm"
    >
      <el-form
        ref="securityFormRef"
        :model="securityForm"
        :rules="securityRules"
        label-position="top"
        size="large"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="securityForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码验证身份"
          />
        </el-form-item>
        <el-form-item label="安全问题" prop="securityQuestion">
          <el-select
            v-model="securityForm.securityQuestion"
            placeholder="请选择或输入安全问题"
            filterable
            allow-create
            style="width: 100%"
          >
            <el-option
              v-for="q in securityQuestionOptions"
              :key="q"
              :label="q"
              :value="q"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="安全答案" prop="securityAnswer">
          <el-input
            v-model="securityForm.securityAnswer"
            placeholder="请输入安全问题答案"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="securityDialogVisible = false">取消</el-button>
          <el-button
            size="large"
            type="primary"
            @click="handleSecuritySave"
            :loading="securitySaving"
            class="save-btn"
          >
            确认修改
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
import { updateNickname, updatePassword, updateSecurityQuestion } from "@/api/auth";
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, connectNotificationStream } from "@/api/notification";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const themeStore = useThemeStore();

const drawerVisible = ref(false);
/**
 * 修改昵称弹窗显示状态
 */
const nicknameDialogVisible = ref(false);
/**
 * 昵称表单数据
 */
const nicknameForm = ref({ nickname: "" });
const isLoggedIn = computed(() => userStore.isLoggedIn());

// ===== 修改密码相关状态 =====
const passwordDialogVisible = ref(false);
const passwordFormRef = ref(null);
const passwordSaving = ref(false);
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' });

// ===== 修改安全问题相关状态 =====
const securityDialogVisible = ref(false);
const securityFormRef = ref(null);
const securitySaving = ref(false);
const securityForm = ref({ oldPassword: '', securityQuestion: '', securityAnswer: '' });

/** 预设安全问题列表（与注册页一致） */
const securityQuestionOptions = [
  "你的第一只宠物叫什么名字？",
  "你的出生城市是哪里？",
  "你小学班主任叫什么名字？",
  "你最喜欢的电影是什么？",
  "你母亲的名字是什么？",
  "你的第一辆车是什么品牌？",
  "你高中学校的名称是什么？",
  "你最好的朋友叫什么名字？",
];

/** 确认密码校验器 */
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应为6-100个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

/** 安全问题表单校验规则 */
const securityRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  securityQuestion: [{ required: true, message: '请选择或输入安全问题', trigger: 'change' }],
  securityAnswer: [
    { required: true, message: '请输入安全答案', trigger: 'blur' },
    { max: 100, message: '安全答案长度不能超过100个字符', trigger: 'blur' }
  ]
};

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
/** 轮询定时器（SSE 断线降级方案） */
let notificationTimer = null;
/** SSE 连接控制器 */
let sseController = null;

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
  if (item.readStatus === 0) {
    // 乐观更新 UI
    item.readStatus = 1;
    item.readTime = new Date().toISOString();
    unreadCount.value = Math.max(0, unreadCount.value - 1);
    // 发送已读请求（不阻塞导航，失败时回滚 UI）
    markAsRead(item.id).catch((e) => {
      console.error("标记已读失败，回滚状态", e);
      item.readStatus = 0;
      item.readTime = null;
      unreadCount.value += 1;
    });
  }
  // 关闭面板并跳转
  notificationPopoverVisible.value = false;
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
    notificationList.value.forEach((item) => {
      item.readStatus = 1;
      item.readTime = new Date().toISOString();
    });
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

/**
 * 格式化通知时间
 */
const formatNotifTime = (time) => {
  if (!time) return "";
  const date = new Date(time);
  const now = new Date();
  const diff = now - date;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${month}-${day}`;
};
const username = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || "用户");

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

// 处理头像下拉菜单命令
const handleCommand = (command) => {
  if (command === "profile") {
    router.push("/dashboard");
  } else if (command === "membership") {
    router.push("/membership");
  } else if (command === "nickname") {
    // 打开弹窗时填充当前昵称
    nicknameForm.value.nickname = userStore.userInfo?.nickname || "";
    nicknameDialogVisible.value = true;
  } else if (command === "password") {
    passwordDialogVisible.value = true;
  } else if (command === "securityQuestion") {
    securityDialogVisible.value = true;
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

/**
 * 保存修改后的昵称
 * 1. 校验昵称长度（2-12字符）
 * 2. 调用后端接口更新昵称
 * 3. 成功后刷新用户信息
 */
const saveNickname = async () => {
  const trimmed = nicknameForm.value.nickname?.trim() || "";
  if (trimmed.length < 2 || trimmed.length > 12) {
    ElMessage.warning("昵称长度需为2-12个字符");
    return;
  }
  try {
    await updateNickname({ nickname: trimmed });
    ElMessage.success("昵称修改成功");
    nicknameDialogVisible.value = false;
    userStore.fetchUserInfo();
  } catch {
    // 拦截器已弹出错误提示
  }
};

/**
 * 重置密码表单
 */
const resetPasswordForm = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
  passwordFormRef.value?.resetFields();
};

/**
 * 保存密码修改
 * 1. 表单校验
 * 2. 调用后端接口修改密码
 * 3. 成功后清除登录态，跳转登录页
 */
const handlePasswordSave = async () => {
  if (!passwordFormRef.value) return;
  try {
    await passwordFormRef.value.validate();
  } catch {
    return;
  }
  passwordSaving.value = true;
  try {
    await updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    });
    ElMessage.success("密码修改成功，请重新登录");
    passwordDialogVisible.value = false;
    // 清除登录态，跳转登录页
    localStorage.removeItem("token");
    removeToken();
    userStore.clearUserInfo();
    router.push("/login");
  } catch {
    // 拦截器已弹出错误提示，此处不再重复
  } finally {
    passwordSaving.value = false;
  }
};

/**
 * 重置安全问题表单
 */
const resetSecurityForm = () => {
  securityForm.value = { oldPassword: '', securityQuestion: '', securityAnswer: '' };
  securityFormRef.value?.resetFields();
};

/**
 * 保存安全问题修改
 * 1. 表单校验
 * 2. 调用后端接口修改安全问题
 * 3. 成功后关闭弹窗
 */
const handleSecuritySave = async () => {
  if (!securityFormRef.value) return;
  try {
    await securityFormRef.value.validate();
  } catch {
    return;
  }
  securitySaving.value = true;
  try {
    await updateSecurityQuestion({
      oldPassword: securityForm.value.oldPassword,
      securityQuestion: securityForm.value.securityQuestion,
      securityAnswer: securityForm.value.securityAnswer
    });
    ElMessage.success("安全问题修改成功");
    securityDialogVisible.value = false;
  } catch {
    // 拦截器已弹出错误提示
  } finally {
    securitySaving.value = false;
  }
};

// 监听登录状态变化，启动或停止通知推送
watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
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
          notificationList.value.unshift(data.notification);
          // 保持列表不超过 10 条
          if (notificationList.value.length > 10) {
            notificationList.value.pop();
          }
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
    // 断开 SSE 连接
    if (sseController) {
      sseController.abort();
      sseController = null;
    }
    if (notificationTimer) {
      clearInterval(notificationTimer);
      notificationTimer = null;
    }
    unreadCount.value = 0;
    notificationList.value = [];
  }
}, { immediate: true });

onUnmounted(() => {
  if (sseController) {
    sseController.abort();
    sseController = null;
  }
  if (notificationTimer) {
    clearInterval(notificationTimer);
    notificationTimer = null;
  }
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

/* 修改昵称弹窗样式 - 全新美学设计 */
.nickname-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.nickname-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.nickname-dialog :deep(.el-dialog__headerbtn) {
  top: 16px;
  right: 16px;
  z-index: 10;
}

.nickname-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-muted);
  transition: all 0.2s;
}

.nickname-dialog :deep(.el-dialog__headerbtn .el-dialog__close:hover) {
  color: var(--text-title);
  transform: rotate(90deg);
}

.nickname-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.nickname-dialog :deep(.el-dialog__footer) {
  padding: 0 24px 24px;
  border-top: none;
}

/* 弹窗主体内容 */
.nickname-modal-content {
  padding: 32px 24px 16px;
  text-align: center;
}

/* 图标包装 */
.nickname-icon-wrapper {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, var(--bg-page) 0%, var(--orange-light-bg) 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: icon-pulse 2s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.nickname-icon {
  width: 32px;
  height: 32px;
  color: var(--orange-main);
}

/* 标题区域 */
.nickname-header {
  margin-bottom: 24px;
}

.nickname-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-title);
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}

.nickname-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

/* 当前昵称展示 */
.nickname-current {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, var(--bg-elevated) 0%, var(--bg-card-hover) 100%);
  border-radius: 12px;
  margin-bottom: 20px;
}

.current-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.current-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--orange-main);
}

/* 输入框包装 */
.nickname-input-wrapper {
  text-align: left;
}

.nickname-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 16px;
  box-shadow: none;
  border: 2px solid var(--border-input);
  transition: all 0.25s ease;
}

.nickname-input :deep(.el-input__wrapper):hover {
  border-color: var(--border-divider);
}

.nickname-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--orange-main);
  box-shadow: 0 0 0 3px rgba(255, 140, 66, 0.15);
}

.nickname-input :deep(.el-input__inner) {
  font-size: 15px;
  color: var(--text-title);
}

.nickname-input :deep(.el-input__inner::placeholder) {
  color: var(--text-placeholder);
}

.nickname-input :deep(.el-input__prefix) {
  left: 12px;
}

.input-icon {
  width: 18px;
  height: 18px;
  color: var(--text-placeholder);
}

.input-tips {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
  text-align: center;
}

/* 底部按钮 */
.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.dialog-footer .el-button {
  padding: 12px 32px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.dialog-footer .el-button:not(.el-button--primary) {
  background: var(--bg-elevated);
  border: none;
  color: var(--text-body);
}

.dialog-footer .el-button:not(.el-button--primary):hover {
  background: var(--border-divider);
  color: var(--text-title);
}

.dialog-footer .el-button--primary {
  background: linear-gradient(135deg, #ff8c42 0%, #ff6b2b 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(255, 140, 66, 0.3);
}

.dialog-footer .el-button--primary:hover {
  background: linear-gradient(135deg, #ff7a2e 0%, #e55a1f 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 140, 66, 0.4);
}

.dialog-footer .el-button--primary:active {
  transform: translateY(0);
}

.dialog-footer .el-button--primary:disabled {
  background: #ccc;
  box-shadow: none;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 480px) {
  .nickname-dialog :deep(.el-dialog) {
    width: 90% !important;
    max-width: 360px;
  }

  .nickname-modal-content {
    padding: 24px 16px 12px;
  }

  .nickname-icon-wrapper {
    width: 56px;
    height: 56px;
  }

  .nickname-title {
    font-size: 18px;
  }

  .dialog-footer .el-button {
    padding: 10px 24px;
    flex: 1;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  padding: 10px 24px;
  border-radius: 8px;
}

.dialog-footer .el-button--primary {
  background: #ff8c42;
  border-color: #ff8c42;
}

.dialog-footer .el-button--primary:hover {
  background: #ff7a2e;
  border-color: #ff7a2e;
}

.dialog-footer .el-button--primary:disabled {
  background: var(--text-placeholder);
  border-color: var(--text-placeholder);
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

.panel-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-title);
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-item-text {
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
