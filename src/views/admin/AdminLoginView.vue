<template>
  <div class="admin-login-page">
    <div class="login-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
    </div>
    <div class="admin-login-card">
      <div class="login-header">
        <img :src="logoUrl" alt="logo" class="login-logo" />
        <div>
          <div class="login-title">管理端</div>
          <div class="login-subtitle">AI 面试与简历系统</div>
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
          登录管理端
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logoUrl from '@/assets/logo.jpg'
import { useAdminUserStore } from '@/stores/adminUser'
import { showAdminError, showAdminSuccess } from '@/utils/adminFeedback'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminUserStore()

const formRef = ref(null)
const formData = reactive({
  username: '',
  password: ''
})

// 管理端登录表单校验，保持最小可用规则。
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
      ? route.query.redirect
      : '/admin/dashboard'
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
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.login-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, rgba(230, 126, 34, 0.1) 0%, rgba(211, 84, 0, 0.05) 100%);
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, rgba(230, 126, 34, 0.08) 0%, rgba(211, 84, 0, 0.03) 100%);
  bottom: -50px;
  left: -50px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, rgba(230, 126, 34, 0.06) 0%, transparent 100%);
  top: 50%;
  left: 20%;
}

.admin-login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 20px;
  padding: 36px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(230, 126, 34, 0.1);
  position: relative;
  z-index: 1;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.login-logo {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  object-fit: contain;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: #2c3e50;
}

.login-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #7f8c8d;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #34495e;
  font-size: 14px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 6px 12px;
  box-shadow: 0 0 0 1px #e0e0e0;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #e67e22;
}

.login-btn {
  width: 100%;
  margin-top: 16px;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(230, 126, 34, 0.35);
}
</style>
