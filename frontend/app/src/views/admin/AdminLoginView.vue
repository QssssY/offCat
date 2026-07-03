<template>
  <div class="admin-login-page">
    <div class="login-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
      <div class="decoration-circle circle-4"></div>
    </div>
    <div class="admin-login-card">
      <div class="login-header">
        <img :src="logoUrl" alt="logo" class="login-logo" />
        <div>
          <div class="login-title">管理端</div>
          <div class="login-subtitle">offerCat 管理系统</div>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            id="username"
            v-model="formData.username"
            placeholder="请输入管理员用户名"
            clearable
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            id="password"
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
            prefix-icon="Lock"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="login-btn"
          :loading="adminStore.loading"
          @click="handleLogin"
        >
          <span class="btn-content">登录管理端</span>
          <span class="btn-arrow">
            <el-icon><Right /></el-icon>
          </span>
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Right } from '@element-plus/icons-vue'
import logoUrl from '@/assets/logo.png'
import { useAdminUserStore } from '@/stores/adminUser'
import { showAdminError, showAdminSuccess } from '@/utils/adminFeedback'
import { prefetchAdminShellRoute } from '@/router/routeLoaders'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminUserStore()

const formRef = ref(null)
const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  try {
    await adminStore.doAdminLogin(formData)
    showAdminSuccess('管理端登录成功')
    const redirect = typeof route.query.redirect === 'string'
      && route.query.redirect.startsWith('/admin')
      && !route.query.redirect.startsWith('//')
      ? route.query.redirect
      : '/admin/dashboard'
    await prefetchAdminShellRoute().catch((prefetchError) => {
      console.debug('管理端首屏预取失败', prefetchError)
    })
    router.push(redirect)
  } catch (error) {
    showAdminError(error?.message || '管理端登录失败')
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #faf8f5 0%, #f0ebe4 50%, #e8dfd4 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.login-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
  animation: float 20s ease-in-out infinite;
}

.circle-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, rgba(230, 126, 34, 0.25) 0%, rgba(245, 158, 66, 0.1) 100%);
  top: -150px;
  right: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, rgba(211, 84, 0, 0.15) 0%, rgba(230, 126, 34, 0.08) 100%);
  bottom: -100px;
  left: -100px;
  animation-delay: -5s;
}

.circle-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, rgba(230, 126, 34, 0.2) 0%, transparent 70%);
  top: 40%;
  left: 15%;
  animation-delay: -10s;
}

.circle-4 {
  width: 180px;
  height: 180px;
  background: linear-gradient(135deg, rgba(245, 166, 35, 0.18) 0%, transparent 60%);
  top: 20%;
  right: 20%;
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(20px, -20px) rotate(5deg);
  }
  50% {
    transform: translate(-10px, 20px) rotate(-3deg);
  }
  75% {
    transform: translate(-20px, -10px) rotate(2deg);
  }
}

.admin-login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px 36px;
  box-shadow:
    0 25px 50px -12px rgba(143, 69, 27, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.5),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.5);
  position: relative;
  z-index: 1;
  animation: card-enter 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 36px;
  padding-bottom: 28px;
  border-bottom: 1px solid rgba(217, 196, 170, 0.3);
}

.login-logo {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  object-fit: contain;
  box-shadow: 0 4px 16px rgba(143, 69, 27, 0.12);
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #8f451b 0%, #d35400 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.login-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #a08060;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #8f451b;
  font-size: 14px;
  padding-bottom: 6px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 14px;
  padding: 8px 14px;
  box-shadow: 0 0 0 1px rgba(217, 196, 170, 0.4);
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.25s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(230, 126, 34, 0.4);
  background: rgba(255, 255, 255, 0.95);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(230, 126, 34, 0.2), 0 0 0 1px #e67e22;
  background: var(--bg-card);
}

.login-form :deep(.el-input__inner) {
  color: #5a4030;
  font-size: 15px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #c4a888;
}

.login-form :deep(.el-input__prefix) {
  color: #c4a888;
}

.login-btn {
  width: 100%;
  margin-top: 24px;
  height: 52px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  box-shadow: 0 6px 20px rgba(230, 126, 34, 0.3);
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  overflow: hidden;
  position: relative;
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.35s ease;
}

.login-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(230, 126, 34, 0.4);
}

.login-btn:hover::before {
  opacity: 1;
}

.login-btn:active {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(230, 126, 34, 0.3);
}

.btn-content {
  position: relative;
  z-index: 1;
}

.btn-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: transform 0.3s ease;
}

.login-btn:hover .btn-arrow {
  transform: translateX(4px);
}

.login-btn :deep(.el-icon) {
  vertical-align: middle;
}
</style>
