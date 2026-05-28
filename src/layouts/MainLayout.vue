<template>
  <div class="main-layout">
    <AppHeader />
    <main class="layout-main">
      <section class="layout-content">
        <n-message-provider>
          <div v-if="showRouteLoading" class="route-loading-bar" aria-hidden="true"></div>
          <router-view v-slot="{ Component, route }">
            <KeepAlive :include="keepAliveViews">
              <component
                v-if="route.meta.keepAlive"
                :is="Component"
                :key="route.name"
              />
            </KeepAlive>
            <Transition name="page-fade" mode="out-in">
              <div
                v-if="!route.meta.keepAlive"
                class="page-fade-route"
                :key="route.fullPath"
              >
                <component :is="Component" />
              </div>
            </Transition>
          </router-view>
        </n-message-provider>
      </section>
    </main>
    <AppFooter />
    <!-- 新手引导弹窗：登录用户首次进入时展示 -->
    <OnboardingGuide v-model:visible="showGuide" />
    <!-- 全局VIP升级弹窗：非会员使用付费功能时触发 -->
    <GlobalVipUpgradeModal />
  </div>
</template>

<script setup>
import { nextTick, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { NMessageProvider } from 'naive-ui'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import OnboardingGuide from '@/components/OnboardingGuide.vue'
import GlobalVipUpgradeModal from '@/components/common/GlobalVipUpgradeModal.vue'
import { getOnboardingStatus } from '@/api/onboarding'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

const userStore = useUserStore()
const router = useRouter()
const showGuide = ref(false)
const showRouteLoading = ref(false)
const keepAliveViews = ['TemplateLibraryView', 'CommunityView', 'GrowthCenterView']
let routeLoadingTimer = null
let routeLoadingHideTimer = null
let removeRouteBeforeGuard = null
let removeRouteAfterGuard = null
let removeRouteErrorGuard = null

function startRouteLoadingFeedback() {
  window.clearTimeout(routeLoadingTimer)
  window.clearTimeout(routeLoadingHideTimer)
  showRouteLoading.value = false
  routeLoadingTimer = window.setTimeout(() => {
    showRouteLoading.value = true
  }, 120)
}

function stopRouteLoadingFeedback() {
  window.clearTimeout(routeLoadingTimer)
  routeLoadingHideTimer = window.setTimeout(() => {
    showRouteLoading.value = false
  }, 180)
}

onMounted(async () => {
  removeRouteBeforeGuard = router.beforeEach((to, from) => {
    if (to.fullPath !== from.fullPath) {
      startRouteLoadingFeedback()
    }
  })
  removeRouteAfterGuard = router.afterEach(async () => {
    await nextTick()
    stopRouteLoadingFeedback()
  })
  removeRouteErrorGuard = router.onError(() => {
    stopRouteLoadingFeedback()
  })

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

onUnmounted(() => {
  removeRouteBeforeGuard?.()
  removeRouteAfterGuard?.()
  removeRouteErrorGuard?.()
  window.clearTimeout(routeLoadingTimer)
  window.clearTimeout(routeLoadingHideTimer)
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
  padding-top: 82px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.layout-content {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 24px;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
}

.route-loading-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 2px;
  width: 100%;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 140, 66, 0.12);
}

.route-loading-bar::before {
  content: "";
  display: block;
  width: 38%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(255, 140, 66, 0.1), var(--orange-main), rgba(255, 176, 122, 0.25));
  animation: route-loading-sweep 0.9s ease-in-out infinite;
}

.page-fade-route {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.page-fade-enter-active {
  transition:
    opacity 0.3s cubic-bezier(0.25, 1, 0.5, 1),
    transform 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.page-fade-leave-active {
  transition:
    opacity 0.2s cubic-bezier(0.7, 0, 0.84, 0),
    transform 0.2s cubic-bezier(0.7, 0, 0.84, 0);
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes route-loading-sweep {
  from {
    transform: translateX(-110%);
  }
  to {
    transform: translateX(280%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .page-fade-enter-active,
  .page-fade-leave-active {
    transition-duration: 0.01ms;
  }

  .page-fade-enter-from,
  .page-fade-leave-to {
    opacity: 1;
    transform: none;
  }

  .route-loading-bar::before {
    animation: none;
    width: 100%;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .layout-main {
    padding-top: 64px;
  }
  .layout-content {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .layout-main {
    padding-top: 60px;
  }
  .layout-content {
    padding: 12px;
  }
}
</style>
