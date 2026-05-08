<template>
  <div class="main-layout">
    <AppHeader />
    <main class="layout-main">
      <section class="layout-content">
        <router-view />
      </section>
    </main>
    <AppFooter />
    <!-- 新手引导弹窗：登录用户首次进入时展示 -->
    <OnboardingGuide v-model:visible="showGuide" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import OnboardingGuide from '@/components/OnboardingGuide.vue'
import { getOnboardingStatus } from '@/api/onboarding'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 控制新手引导弹窗的显示/隐藏
const showGuide = ref(false)

onMounted(async () => {
  // 检查登录状态：同时验证 token 和 userInfo，兼容 fetchUserInfo 尚未完成的时序
  const token = localStorage.getItem('token')
  if (!token && !userStore.isLoggedIn()) return

  try {
    const res = await getOnboardingStatus()
    if (res.data?.showGuide) {
      showGuide.value = true
    }
  } catch (err) {
    // 引导状态查询失败时静默处理，不阻塞页面正常使用
    console.warn('[Onboarding] 查询引导状态失败:', err)
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
  overflow: hidden;
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
