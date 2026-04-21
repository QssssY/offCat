<template>
  <div class="auth-page">
    <div class="bg-decoration">
      <div class="deco-circle circle-1"></div>
      <div class="deco-circle circle-2"></div>
      <div class="deco-circle circle-3"></div>
      <div class="deco-circle circle-4"></div>
      <div class="deco-circle circle-5"></div>
      <div class="deco-circle circle-6"></div>
      <div class="deco-circle circle-7"></div>
      <div class="deco-circle circle-8"></div>
      <div class="deco-dots"></div>
    </div>

    <div class="auth-wrapper">
      <div class="auth-brand">
        <router-link to="/" class="brand-logo-link">
          <img :src="brandLogo" alt="Logo" class="brand-logo" />
          <div class="brand-text">
            <div class="brand-title">AI 面试 与 简历</div>
            <div class="brand-subtitle">智能求职辅助平台</div>
          </div>
        </router-link>

        <div class="brand-slogan">
          <h1 class="slogan-title">智能求职<br />从这里开始</h1>
          <p class="slogan-desc">AI 驱动的简历诊断与模拟面试平台<br />助你找到理想工作</p>
        </div>

        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>智能简历诊断</span>
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>AI 模拟面试</span>
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            <span>个性化建议</span>
          </div>
        </div>

        </div>

      <div class="auth-card">
        <div class="card-tabs">
          <button class="tab" :class="{ active: authMode === 'login' }" @click="switchMode('login')">登录</button>
          <button class="tab" :class="{ active: authMode === 'register' }" @click="switchMode('register')">注册</button>
        </div>

        <template v-if="authMode === 'login'">
          <div class="card-body">
            <h2 class="card-title">欢迎回来</h2>
            <p class="card-subtitle">使用账号登录</p>

            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="auth-form" size="large" @keyup.enter="handleLogin">
              <el-form-item prop="username">
                <el-input id="login-username" v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable />
              </el-form-item>

              <el-form-item prop="password">
                <el-input id="login-password" v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password clearable @keyup.enter="handleLogin" />
              </el-form-item>

              <el-form-item>
                <el-button class="btn-primary" :loading="loading" :disabled="loading" @click="handleLogin">{{ loading ? '登录中...' : '登 录' }}</el-button>
              </el-form-item>
            </el-form>

            <div class="card-footer">
              <span>还没有账号？</span>
              <el-button class="btn-link" @click="switchMode('register')">立即注册</el-button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="card-body">
            <h2 class="card-title">创建账号</h2>
            <p class="card-subtitle">注册享受更多服务</p>

            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="auth-form" size="large" @keyup.enter="handleRegister">
              <el-form-item prop="username">
                <el-input id="register-username" v-model="registerForm.username" placeholder="用户名（3-50个字符）" :prefix-icon="User" clearable />
              </el-form-item>

              <el-form-item prop="password">
                <el-input id="register-password" v-model="registerForm.password" type="password" placeholder="密码（6-100个字符）" :prefix-icon="Lock" show-password clearable />
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <el-input id="register-confirm" v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password clearable @keyup.enter="handleRegister" />
              </el-form-item>

              <el-form-item>
                <el-button class="btn-primary" :loading="loading" :disabled="loading" @click="handleRegister">{{ loading ? '注册中...' : '注 册' }}</el-button>
              </el-form-item>
            </el-form>

            <div class="card-footer">
              <span>已有账号？</span>
              <el-button class="btn-link" @click="switchMode('login')">立即登录</el-button>
            </div>
          </div>
        </template>

        <div v-if="errorMessage" class="alert-error">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ errorMessage }}</span>
        </div>

        <div v-if="registerSuccess" class="alert-success">
          <el-icon color="#67c23a"><CircleCheck /></el-icon>
          <span>{{ registerSuccess }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter, useRoute } from "vue-router";
import { User, Lock, WarningFilled, CircleCheck } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { register } from "@/api/auth";
import brandLogo from "@/assets/logo.jpg";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const authMode = ref("login");
const loading = ref(false);
const errorMessage = ref("");
const registerSuccess = ref("");

const loginFormRef = ref(null);
const registerFormRef = ref(null);

const loginForm = reactive({ username: "", password: "" });
const registerForm = reactive({ username: "", password: "", confirmPassword: "" });

const loginRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 50, message: "用户名长度为 3-50 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 100, message: "密码长度为 6-100 个字符", trigger: "blur" }
  ]
};

const registerRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 50, message: "用户名长度为 3-50 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 100, message: "密码长度为 6-100 个字符", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: "请再次输入密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ]
};

const switchMode = (mode) => {
  authMode.value = mode;
  errorMessage.value = "";
  registerSuccess.value = "";
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  const isValid = await loginFormRef.value.validate().catch(() => false);
  if (!isValid) return;

  loading.value = true;
  try {
    await userStore.doLogin(loginForm);
    ElMessage.success("登录成功");
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";
    router.push(redirect);
  } catch (err) {
    errorMessage.value = err.message || "登录失败，请检查用户名和密码";
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;
  const isValid = await registerFormRef.value.validate().catch(() => false);
  if (!isValid) return;

  loading.value = true;
  try {
    await register(registerForm);
    registerSuccess.value = "注册成功，三秒后自动跳转至登录";
    setTimeout(() => {
      switchMode("login");
      loginForm.username = registerForm.username;
    }, 2000);
  } catch (err) {
    errorMessage.value = err.message || "注册失败，请稍后重试";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px;
}

.bg-decoration {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
}

.circle-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, rgba(255, 103, 0, 0.1) 0%, rgba(255, 103, 0, 0.03) 100%);
  top: -150px;
  right: -100px;
}

.circle-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, rgba(255, 103, 0, 0.08) 0%, rgba(255, 103, 0, 0.02) 100%);
  bottom: -100px;
  left: -80px;
}

.circle-3 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.06) 0%, transparent 70%);
  top: 40%;
  left: 15%;
}

.circle-4 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.05) 0%, transparent 70%);
  top: 15%;
  left: 35%;
}

.circle-5 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.08) 0%, transparent 70%);
  bottom: 30%;
  right: 20%;
}

.circle-6 {
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.06) 0%, transparent 70%);
  top: 60%;
  right: 30%;
}

.circle-7 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.04) 0%, transparent 70%);
  bottom: 10%;
  left: 40%;
}

.circle-8 {
  width: 60px;
  height: 60px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.07) 0%, transparent 70%);
  top: 25%;
  right: 25%;
}

.deco-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 103, 0, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
  opacity: 0.3;
}

.auth-wrapper {
  display: flex;
  gap: 60px;
  max-width: 1100px;
  width: 100%;
  align-items: center;
  position: relative;
  z-index: 1;
}

.auth-brand {
  flex: 1;
}

.brand-logo-link {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  text-decoration: none;
  margin-bottom: 48px;
}

.brand-logo {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  object-fit: contain;
  background: #fff;
  padding: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.brand-text {
  text-align: left;
}

.brand-title {
  font-size: 22px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.brand-subtitle {
  font-size: 13px;
  color: #7f8c8d;
  margin-top: 2px;
}

.brand-slogan {
  margin-bottom: 40px;
}

.slogan-title {
  font-size: 48px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.2;
  margin: 0 0 20px;
  letter-spacing: -0.02em;
}

.slogan-desc {
  font-size: 15px;
  color: #5a6c7d;
  line-height: 1.8;
  margin: 0;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 15px;
  color: #4a5568;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e67e22;
}

.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 103, 0, 0.1);
}

.card-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.tab {
  padding: 8px 4px;
  border: none;
  background: none;
  font-size: 18px;
  font-weight: 500;
  color: #999;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab:hover {
  color: #666;
}

.tab.active {
  color: #333;
}

.tab.active::after {
  content: "";
  position: absolute;
  bottom: -17px;
  left: 0;
  right: 0;
  height: 3px;
  background: #ff6700;
  border-radius: 2px;
}

.card-body {
  padding-top: 24px;
}

.card-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.card-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0 0 32px;
}

.auth-form {
  margin-bottom: 24px;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  padding: 4px 12px;
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #ff6700 inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #ff6700 inset;
}

.btn-primary {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: #ff6700;
  border: none;
  color: #fff;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #e65c00;
}

.card-footer {
  text-align: center;
  font-size: 14px;
  color: #999;
}

.btn-link {
  border: none;
  color: #ff6700;
  padding: 0 4px;
  height: auto;
  font-size: 14px;
}

.btn-link:hover {
  color: #e65c00;
  background: none;
}

.alert-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fff5f5;
  border-radius: 8px;
  font-size: 14px;
  color: #ff4d4f;
  margin-top: 20px;
}

.alert-success {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f6ffed;
  border-radius: 8px;
  font-size: 14px;
  color: #52c41a;
  margin-top: 20px;
}

@media (max-width: 900px) {
  .auth-wrapper {
    flex-direction: column;
    gap: 48px;
  }

  .auth-brand {
    text-align: center;
  }

  .brand-logo-link {
    margin-bottom: 32px;
  }

  .brand-logo-link .brand-text {
    text-align: center;
  }

  .slogan-title {
    font-size: 32px;
  }

  .brand-features {
    align-items: center;
  }

  .auth-card {
    width: 100%;
    max-width: 400px;
    padding: 32px 24px;
  }
}
</style>