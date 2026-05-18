<template>
  <div class="settings-view">
    <header class="settings-header">
      <div>
        <h1>设置中心</h1>
        <p>管理账号、安全、显示和本机通知偏好。</p>
      </div>
    </header>

    <div class="settings-layout">
      <aside class="settings-nav" aria-label="设置分组">
        <button
          v-for="section in sections"
          :key="section.key"
          type="button"
          class="settings-nav-item"
          :class="{ active: activeSection === section.key }"
          @click="activeSection = section.key"
        >
          <el-icon><component :is="section.icon" /></el-icon>
          <span>{{ section.label }}</span>
        </button>
      </aside>

      <main class="settings-content">
        <section v-show="activeSection === 'profile'" class="settings-panel" aria-labelledby="profile-title">
          <div class="panel-heading">
            <div>
              <h2 id="profile-title">账号资料</h2>
              <p>查看当前账号信息、订阅状态和会员有效期。</p>
            </div>
          </div>

          <div class="profile-summary">
            <img src="@/assets/user.png" alt="用户头像" class="profile-avatar" />
            <div class="profile-main">
              <div class="profile-name-row">
                <div class="profile-name">{{ displayName }}</div>
              </div>
              <div class="profile-meta">{{ userInfo?.username || '--' }}</div>
            </div>
            <el-tag :type="roleTagType" effect="plain">{{ roleText }}</el-tag>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span>账号状态</span>
              <strong>{{ statusText }}</strong>
            </div>
            <div class="info-item">
              <span>当前身份</span>
              <strong>{{ roleText }}</strong>
            </div>
            <div class="info-item">
              <span>订阅套餐</span>
              <strong>{{ membershipPlanText }}</strong>
            </div>
            <div class="info-item">
              <span>会员到期时间</span>
              <strong>{{ profileVipExpireTimeText }}</strong>
            </div>
          </div>
        </section>

        <section v-show="activeSection === 'security'" class="settings-panel" aria-labelledby="security-title">
          <div class="panel-heading">
            <div>
              <h2 id="security-title">账号安全</h2>
              <p>修改登录密码和找回密码使用的安全问题。</p>
            </div>
          </div>

          <div class="security-mode-tabs" role="tablist" aria-label="账号安全操作类型">
            <button
              type="button"
              class="security-mode-tab"
              :class="{ active: securityMode === 'password' }"
              role="tab"
              :aria-selected="securityMode === 'password'"
              @click="handleSecurityModeChange('password')"
            >
              修改密码
            </button>
            <button
              type="button"
              class="security-mode-tab"
              :class="{ active: securityMode === 'securityQuestion' }"
              role="tab"
              :aria-selected="securityMode === 'securityQuestion'"
              @click="handleSecurityModeChange('securityQuestion')"
            >
              修改安全问题
            </button>
          </div>

          <Transition name="security-panel" mode="out-in">
            <el-form
              v-if="securityMode === 'password'"
              key="password"
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
              class="settings-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
              </el-form-item>
              <el-button type="primary" :loading="passwordSaving" @click="handlePasswordSave">
                保存密码
              </el-button>
            </el-form>

            <el-form
              v-else
              key="securityQuestion"
              ref="securityFormRef"
              :model="securityForm"
              :rules="securityRules"
              label-position="top"
              class="settings-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="securityForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <el-form-item label="安全问题" prop="securityQuestion">
                <el-select v-model="securityForm.securityQuestion" filterable allow-create default-first-option>
                  <el-option
                    v-for="item in securityQuestionOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="安全答案" prop="securityAnswer">
                <el-input v-model="securityForm.securityAnswer" maxlength="100" show-word-limit />
              </el-form-item>
              <el-button type="primary" :loading="securitySaving" @click="handleSecuritySave">
                保存安全问题
              </el-button>
            </el-form>
          </Transition>
        </section>

        <section v-show="activeSection === 'appearance'" class="settings-panel" aria-labelledby="appearance-title">
          <div class="panel-heading">
            <div>
              <h2 id="appearance-title">外观偏好</h2>
              <p>选择当前浏览器使用的显示模式，偏好会自动保存在本机。</p>
            </div>
            <div class="appearance-status">
              <el-tag effect="plain">当前：{{ resolvedThemeText }}</el-tag>
              <span>已保存到当前浏览器</span>
            </div>
          </div>

          <div class="appearance-options" role="radiogroup" aria-label="外观模式">
            <button
              v-for="option in themeOptions"
              :key="option.value"
              type="button"
              class="appearance-option"
              :class="{ active: themeChoice === option.value }"
              role="radio"
              :aria-checked="themeChoice === option.value"
              @click="handleThemeChange(option.value)"
            >
              <span class="appearance-preview" :class="option.previewClass">
                <span></span>
                <span></span>
                <span></span>
              </span>
              <strong>{{ option.label }}</strong>
              <em>{{ option.description }}</em>
            </button>
          </div>
        </section>

        <section v-show="activeSection === 'notification'" class="settings-panel" aria-labelledby="notification-title">
          <div class="panel-heading">
            <div>
              <h2 id="notification-title">通知偏好</h2>
              <p>仅影响当前浏览器的显示偏好。</p>
            </div>
          </div>

          <div class="preference-list">
            <div class="preference-row">
              <div>
                <strong>顶部实时通知提醒</strong>
                <span>关闭后不建立实时通知连接，也不显示顶部通知铃铛。</span>
              </div>
              <el-switch
                v-model="notificationForm.notificationRealtimeEnabled"
                aria-label="顶部实时通知提醒"
                @change="handleNotificationPreferenceSave"
              />
            </div>
            <div class="preference-row">
              <div>
                <strong>进入通知中心默认只看未读</strong>
                <span>打开通知中心时自动带入未读筛选。</span>
              </div>
              <el-switch
                v-model="notificationForm.notificationDefaultUnreadOnly"
                aria-label="进入通知中心默认只看未读"
                @change="handleNotificationPreferenceSave"
              />
            </div>
            <div class="preference-row stacked">
              <div>
                <strong>通知中心默认类型</strong>
                <span>进入通知中心时自动选择对应类型。</span>
              </div>
              <el-select
                v-model="notificationForm.notificationDefaultType"
                class="notification-type-select"
                @change="handleNotificationPreferenceSave"
              >
                <el-option label="全部类型" value="" />
                <el-option label="简历诊断" value="resume" />
                <el-option label="AI 润色" value="polish" />
                <el-option label="模拟面试" value="interview" />
                <el-option label="额度提醒" value="quota" />
                <el-option label="系统通知" value="system" />
                <el-option label="活动公告" value="activity" />
                <el-option label="版本公告" value="update" />
                <el-option label="维护公告" value="maintenance" />
              </el-select>
            </div>
          </div>
        </section>

        <section v-show="activeSection === 'onboarding'" class="settings-panel" aria-labelledby="onboarding-title">
          <div class="panel-heading">
            <div>
              <h2 id="onboarding-title">新手引导</h2>
              <p>需要重新熟悉功能入口时，可以再次查看引导。</p>
            </div>
          </div>

          <el-button type="primary" plain @click="showOnboarding = true">
            重新查看新手引导
          </el-button>
        </section>

        <section v-show="activeSection === 'membership'" class="settings-panel" aria-labelledby="membership-title">
          <div class="panel-heading">
            <div>
              <h2 id="membership-title">会员与额度</h2>
              <p>查看当前身份、到期时间和可用额度。</p>
            </div>
            <el-button type="primary" plain @click="router.push('/membership')">
              查看会员中心
            </el-button>
          </div>

          <div class="info-grid quota-grid">
            <div class="info-item">
              <span>当前身份</span>
              <strong>{{ roleText }}</strong>
            </div>
            <div class="info-item">
              <span>VIP 到期时间</span>
              <strong>{{ vipExpireTimeText }}</strong>
            </div>
            <div class="info-item">
              <span>简历诊断额度</span>
              <strong>{{ userInfo?.resumeQuota ?? 0 }}</strong>
            </div>
            <div class="info-item">
              <span>模拟面试额度</span>
              <strong>{{ userInfo?.interviewQuota ?? 0 }}</strong>
            </div>
            <div class="info-item">
              <span>VIP 今日简历额度</span>
              <strong>{{ userInfo?.vipDailyResumeQuota ?? 0 }}</strong>
            </div>
            <div class="info-item">
              <span>VIP 今日面试额度</span>
              <strong>{{ userInfo?.vipDailyInterviewQuota ?? 0 }}</strong>
            </div>
          </div>
        </section>
      </main>
    </div>

    <OnboardingGuide v-model:visible="showOnboarding" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Brush, Memo, Lock, Star, User } from '@element-plus/icons-vue'
import { updatePassword, updateSecurityQuestion } from '@/api/auth'
import { getMembershipPlans } from '@/api/membership'
import OnboardingGuide from '@/components/OnboardingGuide.vue'
import { useThemeStore } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { removeToken } from '@/utils/auth'
import { getSettingsPreferences, saveSettingsPreferences } from '@/utils/settingsPreferences'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const activeSection = ref('profile')
const showOnboarding = ref(false)
const securityMode = ref('password')
const passwordFormRef = ref(null)
const securityFormRef = ref(null)
const passwordSaving = ref(false)
const securitySaving = ref(false)

const sections = [
  { key: 'profile', label: '账号资料', icon: User },
  { key: 'security', label: '账号安全', icon: Lock },
  { key: 'appearance', label: '外观偏好', icon: Brush },
  { key: 'notification', label: '通知偏好', icon: Bell },
  { key: 'onboarding', label: '新手引导', icon: Memo },
  { key: 'membership', label: '会员与额度', icon: Star }
]

const themeOptions = [
  { value: 'system', label: '跟随系统', description: '随设备系统自动切换', previewClass: 'system' },
  { value: 'light', label: '亮色', description: '适合白天和明亮环境', previewClass: 'light' },
  { value: 'dark', label: '暗色', description: '适合夜间和低亮环境', previewClass: 'dark' }
]

const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '用户')
const isVipUser = computed(() => userStore.isVip())
const isAdmin = computed(() => userInfo.value?.role === 9)
const membershipPlans = ref([])

const roleText = computed(() => {
  if (isAdmin.value) return '管理员'
  if (isVipUser.value) return '会员用户'
  return '普通用户'
})

const roleTagType = computed(() => {
  if (isAdmin.value) return 'warning'
  if (isVipUser.value) return 'success'
  return 'info'
})

const statusText = computed(() => {
  if (userInfo.value?.status === 0) return '已禁用'
  if (userInfo.value?.status === 1) return '正常'
  return '--'
})

const vipExpireTimeText = computed(() => {
  if (!userInfo.value?.vipExpireTime) return '--'
  const date = new Date(userInfo.value.vipExpireTime)
  if (Number.isNaN(date.getTime())) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
})

const profileVipExpireTimeText = computed(() => {
  // 账号资料区需要补齐第四项信息；非会员不展示空日期，避免误解为数据缺失。
  if (!isVipUser.value) return '未开通会员'
  return vipExpireTimeText.value === '--' ? '会员有效期未设置' : vipExpireTimeText.value
})

const getPlanNameCn = (planName) => {
  const nameMap = {
    'Monthly VIP': '月度会员',
    'Quarterly VIP': '季度会员',
    'Yearly VIP': '年度会员'
  }
  return nameMap[planName] || planName
}

const membershipPlanText = computed(() => {
  if (!isVipUser.value) return '未开通会员'

  const currentPlanCode = userInfo.value?.membershipPlanCode || ''
  const matchedPlan = membershipPlans.value.find((plan) => plan.planCode === currentPlanCode)
  // 用户侧只展示套餐名称，避免暴露内部套餐编码。
  if (matchedPlan?.planName) return getPlanNameCn(matchedPlan.planName)
  return '会员套餐'
})

const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const securityForm = ref({ oldPassword: '', securityQuestion: '', securityAnswer: '' })
const notificationForm = ref(getSettingsPreferences())

const themeChoice = ref(themeStore.followSystem ? 'system' : themeStore.manualTheme)
const resolvedThemeText = computed(() => themeStore.resolvedTheme === 'dark' ? '暗色' : '亮色')

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度应为 6-100 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const securityRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  securityQuestion: [{ required: true, message: '请选择或输入安全问题', trigger: 'change' }],
  securityAnswer: [
    { required: true, message: '请输入安全答案', trigger: 'blur' },
    { max: 100, message: '安全答案长度不能超过 100 个字符', trigger: 'blur' }
  ]
}

const securityQuestionOptions = [
  '你的第一只宠物叫什么名字？',
  '你的出生城市是哪里？',
  '你小学班主任叫什么名字？',
  '你最喜欢的电影是什么？',
  '你母亲的名字是什么？',
  '你的第一辆车是什么品牌？',
  '你高中学校的名称是什么？',
  '你最好的朋友叫什么名字？'
]

const resetPasswordForm = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordFormRef.value?.resetFields()
}

const resetSecurityForm = () => {
  securityForm.value = { oldPassword: '', securityQuestion: '', securityAnswer: '' }
  securityFormRef.value?.resetFields()
}

const handleSecurityModeChange = (value) => {
  if (securityMode.value === value) return
  securityMode.value = value
  // 切换安全操作时清空未展示表单，防止两个高风险表单的输入状态互相干扰。
  if (value === 'password') {
    resetSecurityForm()
    return
  }
  resetPasswordForm()
}

const handlePasswordSave = async () => {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordSaving.value = true
  try {
    await updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    removeToken()
    userStore.clearUserInfo()
    router.push('/login')
  } finally {
    passwordSaving.value = false
  }
}

const handleSecuritySave = async () => {
  if (!securityFormRef.value) return
  try {
    await securityFormRef.value.validate()
  } catch {
    return
  }

  securitySaving.value = true
  try {
    await updateSecurityQuestion({
      oldPassword: securityForm.value.oldPassword,
      securityQuestion: securityForm.value.securityQuestion,
      securityAnswer: securityForm.value.securityAnswer
    })
    securityForm.value = { oldPassword: '', securityQuestion: '', securityAnswer: '' }
    securityFormRef.value?.resetFields()
    ElMessage.success('安全问题已保存')
  } finally {
    securitySaving.value = false
  }
}

const handleThemeChange = (value) => {
  themeChoice.value = value
  if (value === 'system') {
    themeStore.setFollowSystem(true)
    return
  }
  themeStore.setTheme(value)
}

const handleNotificationPreferenceSave = () => {
  notificationForm.value = saveSettingsPreferences(notificationForm.value)
}

onMounted(async () => {
  const tasks = []
  if (!userStore.userInfo) tasks.push(userStore.fetchUserInfo())
  tasks.push(
    getMembershipPlans().then((res) => {
      membershipPlans.value = Array.isArray(res?.data) ? res.data : []
    }).catch(() => {
      // 套餐列表失败时不回退显示内部编码，只保留用户可理解的兜底文案。
      membershipPlans.value = []
    })
  )
  await Promise.all(tasks)
})
</script>

<style scoped>
.settings-view {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.settings-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.settings-header h1 {
  margin: 0;
  color: var(--text-title);
  font-size: 24px;
  line-height: 1.3;
}

.settings-header p,
.panel-heading p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.6;
}

.settings-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  align-items: flex-start;
}

.settings-nav {
  position: sticky;
  top: 84px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-card);
}

.settings-nav-item {
  width: 100%;
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-body);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.settings-nav-item:hover,
.settings-nav-item.active {
  background: var(--orange-light-bg);
  color: var(--orange-deep);
}

.settings-content {
  min-width: 0;
}

.settings-panel {
  padding: 24px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-card);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.panel-heading h2 {
  margin: 0;
  color: var(--text-title);
}

.panel-heading h2 {
  font-size: 20px;
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  margin-bottom: 18px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
}

.profile-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.profile-name {
  color: var(--text-title);
  font-size: 18px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.profile-meta {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.info-item {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
}

.info-item span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.info-item strong {
  display: block;
  margin-top: 8px;
  color: var(--text-title);
  font-size: 16px;
  overflow-wrap: anywhere;
}

.settings-form {
  max-width: 520px;
}

.security-mode-tabs {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-divider);
  overflow-x: auto;
}

.security-mode-tab {
  position: relative;
  flex: 0 0 auto;
  min-height: 40px;
  padding: 8px 18px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--text-body);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background-color 0.2s;
}

.security-mode-tab:hover,
.security-mode-tab.active {
  color: var(--orange-main);
}

.security-mode-tab.active {
  border-bottom-color: var(--orange-main);
  font-weight: 600;
}

.security-mode-tab:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.security-panel-enter-active,
.security-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.security-panel-enter-from,
.security-panel-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.appearance-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.4;
}

.appearance-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.appearance-option {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  background: var(--bg-page);
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s;
}

.appearance-option:hover,
.appearance-option.active {
  border-color: var(--orange-main);
  background: var(--bg-card);
}

.appearance-option.active {
  box-shadow: 0 0 0 2px rgba(255, 140, 66, 0.14);
}

.appearance-option:focus-visible {
  outline: 2px solid var(--orange-main);
  outline-offset: 2px;
}

.appearance-option strong {
  color: var(--text-title);
  font-size: 15px;
}

.appearance-option em {
  color: var(--text-muted);
  font-size: 13px;
  font-style: normal;
  line-height: 1.5;
}

.appearance-preview {
  width: 100%;
  height: 76px;
  display: grid;
  grid-template-columns: 28px 1fr;
  grid-template-rows: 18px 1fr;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--border-card);
}

.appearance-preview span:first-child {
  grid-row: 1 / 3;
  border-radius: 6px;
}

.appearance-preview span:nth-child(2),
.appearance-preview span:nth-child(3) {
  border-radius: 6px;
}

.appearance-preview.light {
  background: #f8fafc;
}

.appearance-preview.light span:first-child {
  background: #ffffff;
}

.appearance-preview.light span:nth-child(2) {
  background: #ff8c42;
}

.appearance-preview.light span:nth-child(3) {
  background: #e5e7eb;
}

.appearance-preview.dark {
  background: #111827;
  border-color: #374151;
}

.appearance-preview.dark span:first-child {
  background: #1f2937;
}

.appearance-preview.dark span:nth-child(2) {
  background: #ff8c42;
}

.appearance-preview.dark span:nth-child(3) {
  background: #374151;
}

.appearance-preview.system {
  background: linear-gradient(90deg, #f8fafc 0 50%, #111827 50% 100%);
}

.appearance-preview.system span:first-child {
  background: linear-gradient(90deg, #ffffff 0 50%, #1f2937 50% 100%);
}

.appearance-preview.system span:nth-child(2) {
  background: #ff8c42;
}

.appearance-preview.system span:nth-child(3) {
  background: linear-gradient(90deg, #e5e7eb 0 50%, #374151 50% 100%);
}

.preference-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-card);
  border-radius: 10px;
  overflow: hidden;
}

.preference-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-card);
}

.preference-row:last-child {
  border-bottom: 0;
}

.preference-row.stacked {
  align-items: flex-start;
}

.preference-row strong,
.preference-row span {
  display: block;
}

.preference-row strong {
  color: var(--text-title);
  font-size: 14px;
}

.preference-row span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.notification-type-select {
  width: 220px;
  flex-shrink: 0;
}

.quota-grid {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }

  .settings-nav {
    position: static;
    overflow-x: auto;
    flex-direction: row;
  }

  .settings-nav-item {
    flex: 0 0 auto;
    width: auto;
    white-space: nowrap;
  }

  .appearance-options {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .settings-panel {
    padding: 18px;
  }

  .panel-heading,
  .profile-summary,
  .profile-name-row,
  .preference-row,
  .preference-row.stacked {
    flex-direction: column;
    align-items: stretch;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .notification-type-select {
    width: 100%;
  }

  .appearance-status {
    align-items: flex-start;
  }

  .appearance-preview {
    height: 64px;
  }
}
</style>
