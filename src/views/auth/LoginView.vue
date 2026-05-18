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
        <router-link to="/" class="back-home">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回首页</span>
        </router-link>

        <!-- 登录/注册标签栏（忘记密码时隐藏） -->
        <div v-if="authMode !== 'forgot'" class="card-tabs">
          <button class="tab" :class="{ active: authMode === 'login' }" @click="switchMode('login')">登录</button>
          <button class="tab" :class="{ active: authMode === 'register' }" @click="switchMode('register')">注册</button>
        </div>

        <!-- 登录表单 -->
        <template v-if="authMode === 'login'">
          <div class="card-body">
            <h2 class="card-title">欢迎回来</h2>
            <p class="card-subtitle">使用账号登录</p>

            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="auth-form" size="large" @keyup.enter="handleLogin">
              <el-form-item prop="username">
                <el-input id="login-username" v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable />
              </el-form-item>

              <el-form-item prop="password">
                <el-input id="login-password" v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password clearable />
              </el-form-item>

              <div class="forgot-row">
                <el-button class="btn-link btn-forgot" @click="switchMode('forgot')">忘记密码？</el-button>
              </div>

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

        <!-- 注册表单 -->
        <template v-else-if="authMode === 'register'">
          <div class="card-body">
            <h2 class="card-title">创建账号</h2>
            <p class="card-subtitle">注册享受更多服务</p>

            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="auth-form" size="large" @keyup.enter="handleRegister">
              <el-form-item prop="username">
                <el-input id="register-username" v-model="registerForm.username" placeholder="用户名（3-50个字符）" :prefix-icon="User" clearable />
              </el-form-item>

              <el-form-item prop="password">
                <el-input id="register-password" v-model="registerForm.password" type="password" placeholder="密码（6-100个字符）" :prefix-icon="Lock" show-password clearable />
                <!-- 密码强度指示器 -->
                <div v-if="registerForm.password" class="password-strength" role="meter" :aria-valuenow="registerStrengthScore" aria-valuemin="0" aria-valuemax="5" :aria-label="`密码强度：${registerStrengthLabel}`">
                  <div class="strength-bars">
                    <div class="strength-bar" :class="registerStrengthClass(1)"></div>
                    <div class="strength-bar" :class="registerStrengthClass(2)"></div>
                    <div class="strength-bar" :class="registerStrengthClass(3)"></div>
                  </div>
                  <span class="strength-text" :class="registerStrengthLevel">{{ registerStrengthLabel }}</span>
                </div>
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <el-input id="register-confirm" v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password clearable />
              </el-form-item>

              <el-form-item prop="securityQuestion">
                <el-select
                  v-model="registerForm.securityQuestion"
                  placeholder="选择安全问题（忘记密码时使用）"
                  filterable
                  allow-create
                  default-first-option
                  style="width: 100%"
                >
                  <el-option v-for="q in presetSecurityQuestions" :key="q" :label="q" :value="q" />
                </el-select>
              </el-form-item>

              <el-form-item prop="securityAnswer">
                <el-input v-model="registerForm.securityAnswer" placeholder="安全问题答案" :prefix-icon="Key" clearable @keyup.enter="handleRegister" />
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

        <!-- 忘记密码流程 -->
        <template v-if="authMode === 'forgot'">
          <div class="card-body">
            <h2 class="card-title">找回密码</h2>
            <p class="card-subtitle">{{ forgotStep === 1 ? '请输入您的用户名' : '请回答安全问题并设置新密码' }}</p>

            <!-- Step 1: 输入用户名 -->
            <el-form v-if="forgotStep === 1" ref="forgotStep1FormRef" :model="forgotForm" :rules="forgotStep1Rules" class="auth-form" size="large" @keyup.enter="handleFetchQuestion">
              <el-form-item prop="username">
                <el-input v-model="forgotForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
              </el-form-item>

              <el-form-item>
                <el-button class="btn-primary" :loading="forgotLoading" :disabled="forgotLoading" @click="handleFetchQuestion">{{ forgotLoading ? '查询中...' : '下一步' }}</el-button>
              </el-form-item>
            </el-form>

            <!-- Step 2: 回答安全问题 + 设置新密码 -->
            <el-form v-else ref="forgotStep2FormRef" :model="forgotForm" :rules="forgotStep2Rules" class="auth-form" size="large">
              <div class="security-question-display">
                <span class="question-label">安全问题：</span>
                <span class="question-text">{{ forgotForm.securityQuestion }}</span>
              </div>

              <el-form-item prop="securityAnswer">
                <el-input v-model="forgotForm.securityAnswer" placeholder="请输入安全问题答案" :prefix-icon="Key" clearable />
              </el-form-item>

              <el-form-item prop="newPassword">
                <el-input v-model="forgotForm.newPassword" type="password" placeholder="新密码（6-100个字符）" :prefix-icon="Lock" show-password clearable />
                <!-- 忘记密码流程的密码强度指示器 -->
                <div v-if="forgotForm.newPassword" class="password-strength" role="meter" :aria-valuenow="forgotStrengthScore" aria-valuemin="0" aria-valuemax="5" :aria-label="`密码强度：${forgotStrengthLabel}`">
                  <div class="strength-bars">
                    <div class="strength-bar" :class="forgotStrengthClass(1)"></div>
                    <div class="strength-bar" :class="forgotStrengthClass(2)"></div>
                    <div class="strength-bar" :class="forgotStrengthClass(3)"></div>
                  </div>
                  <span class="strength-text" :class="forgotStrengthLevel">{{ forgotStrengthLabel }}</span>
                </div>
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="确认新密码" :prefix-icon="Lock" show-password clearable @keyup.enter="handleResetPassword" />
              </el-form-item>

              <el-form-item>
                <el-button class="btn-primary" :loading="forgotLoading" :disabled="forgotLoading" @click="handleResetPassword">{{ forgotLoading ? '重置中...' : '重置密码' }}</el-button>
              </el-form-item>
            </el-form>

            <div class="card-footer">
              <span>想起密码了？</span>
              <el-button class="btn-link" @click="switchMode('login')">返回登录</el-button>
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
import { ref, reactive, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { User, Lock, Key, ArrowLeft, WarningFilled, CircleCheck } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { register, getSecurityQuestion, resetPasswordBySecurity } from "@/api/auth";
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
const registerForm = reactive({ username: "", password: "", confirmPassword: "", securityQuestion: "", securityAnswer: "" });

// 密码强度计算：0-5 分，分 3 档
const calcPasswordScore = (pwd) => {
  if (!pwd) return 0;
  let score = 0;
  if (pwd.length >= 6) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[a-z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  return score;
};

// 密码强度通用逻辑：根据 score 返回 level / label / 各格样式
const usePasswordStrength = (pwdRef) => {
  const score = computed(() => calcPasswordScore(pwdRef.value));
  const level = computed(() => {
    if (score.value <= 1) return "weak";
    if (score.value <= 3) return "medium";
    return "strong";
  });
  const label = computed(() => {
    if (score.value <= 1) return "弱";
    if (score.value <= 3) return "中";
    return "强";
  });
  const barClass = (bar) => {
    if (bar === 1) return score.value >= 1 ? `active-${level.value}` : "";
    if (bar === 2) return score.value >= 3 ? `active-${level.value}` : "";
    if (bar === 3) return score.value >= 4 ? `active-${level.value}` : "";
    return "";
  };
  return { score, level, label, barClass };
};

// 注册密码强度
const { score: registerStrengthScore, level: registerStrengthLevel, label: registerStrengthLabel, barClass: registerStrengthClass } = usePasswordStrength(computed(() => registerForm.password));

// 忘记密码强度
const { score: forgotStrengthScore, level: forgotStrengthLevel, label: forgotStrengthLabel, barClass: forgotStrengthClass } = usePasswordStrength(computed(() => forgotForm.newPassword));

// 预设安全问题列表
const presetSecurityQuestions = [
  "你的第一只宠物叫什么名字？",
  "你的出生城市是哪里？",
  "你小学班主任叫什么名字？",
  "你最喜欢的电影是什么？",
  "你母亲的名字是什么？",
  "你的第一辆车是什么品牌？",
  "你高中学校的名称是什么？",
  "你最好的朋友叫什么名字？"
];

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
    { min: 6, max: 100, message: "密码长度为 6-100 个字符", trigger: "blur" },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/, message: "密码必须包含字母和数字", trigger: "blur" }
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
  ],
  securityQuestion: [
    { required: true, message: "请选择或输入安全问题", trigger: "change" },
    { max: 50, message: "安全问题长度不能超过50个字符", trigger: "change" }
  ],
  securityAnswer: [
    { required: true, message: "请输入安全问题答案", trigger: "blur" },
    { max: 100, message: "答案长度不能超过100个字符", trigger: "blur" }
  ]
};

// 忘记密码相关状态
const forgotStep = ref(1);
const forgotLoading = ref(false);
const forgotStep1FormRef = ref(null);
const forgotStep2FormRef = ref(null);
const forgotForm = reactive({
  username: "",
  securityQuestion: "",
  securityAnswer: "",
  newPassword: "",
  confirmPassword: ""
});

const forgotStep1Rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 50, message: "用户名长度为 3-50 个字符", trigger: "blur" }
  ]
};

const forgotStep2Rules = {
  securityAnswer: [
    { required: true, message: "请输入安全问题答案", trigger: "blur" }
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, max: 100, message: "密码长度为 6-100 个字符", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.newPassword) {
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
  // 重置忘记密码状态
  forgotStep.value = 1;
  forgotForm.username = "";
  forgotForm.securityQuestion = "";
  forgotForm.securityAnswer = "";
  forgotForm.newPassword = "";
  forgotForm.confirmPassword = "";
};

const resolveSafeRedirect = (redirect) => {
  if (typeof redirect !== "string") return "/";
  if (!redirect.startsWith("/") || redirect.startsWith("//")) return "/";
  return redirect;
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  const isValid = await loginFormRef.value.validate().catch(() => false);
  if (!isValid) return;

  loading.value = true;
  try {
    await userStore.doLogin(loginForm);
    ElMessage.success("登录成功");
    const redirect = resolveSafeRedirect(route.query.redirect);
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

// 忘记密码 Step 1：获取安全问题
const handleFetchQuestion = async () => {
  if (!forgotStep1FormRef.value) return;
  const isValid = await forgotStep1FormRef.value.validate().catch(() => false);
  if (!isValid) return;

  forgotLoading.value = true;
  errorMessage.value = "";
  try {
    const res = await getSecurityQuestion(forgotForm.username);
    forgotForm.securityQuestion = res.data.securityQuestion;
    forgotStep.value = 2;
  } catch (err) {
    errorMessage.value = err.message || "查询失败，请检查用户名";
  } finally {
    forgotLoading.value = false;
  }
};

// 忘记密码 Step 2：验证答案并重置密码
const handleResetPassword = async () => {
  if (!forgotStep2FormRef.value) return;
  const isValid = await forgotStep2FormRef.value.validate().catch(() => false);
  if (!isValid) return;

  forgotLoading.value = true;
  errorMessage.value = "";
  try {
    await resetPasswordBySecurity({
      username: forgotForm.username,
      securityAnswer: forgotForm.securityAnswer,
      newPassword: forgotForm.newPassword
    });
    ElMessage.success("密码重置成功，请登录");
    switchMode("login");
    loginForm.username = forgotForm.username;
  } catch (err) {
    errorMessage.value = err.message || "密码重置失败";
  } finally {
    forgotLoading.value = false;
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

.back-home {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted, #7f8c8d);
  text-decoration: none;
  margin-bottom: 16px;
  transition: color 0.2s;
}

.back-home:hover {
  color: var(--orange-main, #ff6700);
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
  background: var(--bg-card);
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
  background: var(--bg-card);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 103, 0, 0.1);
}

.card-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid var(--border-divider);
  padding-bottom: 16px;
}

.tab {
  padding: 8px 4px;
  border: none;
  background: none;
  font-size: 18px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab:hover {
  color: #666;
}

.tab.active {
  color: var(--text-title);
}

.tab.active::after {
  content: "";
  position: absolute;
  bottom: -17px;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--orange-main);
  border-radius: 2px;
}

.card-body {
  padding-top: 24px;
}

.card-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-title);
  margin: 0 0 8px;
}

.card-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0 0 32px;
}

.auth-form {
  margin-bottom: 24px;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--border-input) inset;
  padding: 4px 12px;
}

.auth-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--orange-main) inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--orange-main) inset;
}

.btn-primary {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: var(--orange-main);
  border: none;
  color: var(--bg-card);
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: var(--orange-deep);
}

.card-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
}

.forgot-row {
  display: flex;
  justify-content: flex-end;
  margin: -8px 0 12px;
}

.btn-forgot {
  font-size: 13px;
  color: var(--text-muted);
}

.btn-forgot:hover {
  color: var(--orange-main);
}

.security-question-display {
  background: var(--bg-page, #f5f5f5);
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.6;
}

.question-label {
  color: var(--text-muted);
  margin-right: 4px;
}

.question-text {
  color: var(--text-title);
  font-weight: 500;
}

/* 注册表单中的 el-select 样式适配 */
.auth-form :deep(.el-select) {
  width: 100%;
}

.auth-form :deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--border-input) inset;
  padding: 4px 12px;
}

.auth-form :deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--orange-main) inset;
}

.btn-link {
  border: none;
  color: var(--orange-main);
  padding: 0 4px;
  height: auto;
  font-size: 14px;
}

.btn-link:hover {
  color: var(--orange-deep);
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

/* 密码强度指示器 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  width: 100%;
}

.strength-bars {
  display: flex;
  gap: 4px;
  flex: 1;
}

.strength-bar {
  height: 4px;
  flex: 1;
  border-radius: 2px;
  background: #e4e7ed;
  transition: background 0.3s ease;
}

.strength-bar.active-weak {
  background: #f56c6c;
}

.strength-bar.active-medium {
  background: #e6a23c;
}

.strength-bar.active-strong {
  background: #67c23a;
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
  min-width: 16px;
}

.strength-text.weak {
  color: #f56c6c;
}

.strength-text.medium {
  color: #e6a23c;
}

.strength-text.strong {
  color: #67c23a;
}
</style>
