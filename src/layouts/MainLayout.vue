<template>
  <div class="main-layout">
    <AppHeader />
    <main class="layout-main">
      <section class="layout-content">
        <n-message-provider>
          <router-view />
        </n-message-provider>
      </section>
    </main>
    <AppFooter />
    <!-- 新手引导弹窗：登录用户首次进入时展示 -->
    <OnboardingGuide v-model:visible="showGuide" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NMessageProvider } from 'naive-ui'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import OnboardingGuide from '@/components/OnboardingGuide.vue'
import { getOnboardingStatus } from '@/api/onboarding'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

const userStore = useUserStore()
const showGuide = ref(false)

onMounted(async () => {
  // 统一通过 auth 工具读取 token，避免页面继续依赖旧 localStorage 键名。
  const token = getToken()
  if (!token && !userStore.isLoggedIn()) return

  try {
    const res = await getOnboardingStatus()
    if (res.data?.showGuide) {
      showGuide.value = true
    }
  } catch (err) {
    // 引导状态查询失败时静默处理，不阻塞页面正常使用。
  }
})
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: var(--bg-page, #fff8f3);
  display: flex;
  flex-direction: column;
}

.layout-main {
  padding-top: 60px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.layout-content {
  flex: 1;
  min-height: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .layout-content {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .layout-content {
    padding: 12px;
  }
}
</style>
