<template>
  <div class="main-layout">
    <AppHeader />
    <main class="layout-main">
      <section class="layout-content">
        <n-message-provider>
          <div v-if="showRouteLoading" class="route-loading-bar" aria-hidden="true"></div>
          <div
            v-if="showRouteLoading && isRouteLoadingPlaceholderRoute"
            class="route-loading-placeholder"
            role="status"
            aria-live="polite"
          >
            <div class="route-loading-placeholder-card">
              <div class="route-loading-placeholder-title">{{ routeLoadingTargetText }}</div>
              <div class="route-loading-placeholder-line primary"></div>
              <div class="route-loading-placeholder-line"></div>
              <div class="route-loading-placeholder-line short"></div>
            </div>
          </div>
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
import { computed, nextTick, ref, onMounted, onUnmounted } from 'vue'
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
const loadingRoutePath = ref('')
const keepAliveViews = [
  'TemplateLibraryView',
  'CommunityView',
  'GrowthCenterView',
  'DashboardView',
  'SettingsView',
  'MembershipView',
  'InterviewHistoryView',
  'HistoryView'
]
let routeLoadingTimer = null
let routeLoadingHideTimer = null
let removeRouteBeforeGuard = null
let removeRouteAfterGuard = null
let removeRouteErrorGuard = null

const isRouteLoadingPlaceholderRoute = computed(() => (
  loadingRoutePath.value.startsWith('/resume/upload')
  || loadingRoutePath.value.startsWith('/resume/result')
  || loadingRoutePath.value.startsWith('/interview/report')
  || loadingRoutePath.value.startsWith('/settings')
  || loadingRoutePath.value.startsWith('/community/my')
))

const routeLoadingTargetText = computed(() => {
  if (loadingRoutePath.value.startsWith('/settings')) return '正在打开设置中心'
  if (loadingRoutePath.value.startsWith('/community/my')) return '正在打开个人动态中心'
  if (loadingRoutePath.value.startsWith('/interview/report')) return '正在打开面试报告'
  if (loadingRoutePath.value.startsWith('/resume/result')) return '正在打开诊断结果'
  if (loadingRoutePath.value.startsWith('/resume/upload')) return '正在打开简历诊断'
  return '正在加载页面'
})

function startRouteLoadingFeedback(targetPath = '') {
  window.clearTimeout(routeLoadingTimer)
  window.clearTimeout(routeLoadingHideTimer)
  loadingRoutePath.value = targetPath
  showRouteLoading.value = false
  routeLoadingTimer = window.setTimeout(() => {
    showRouteLoading.value = true
  }, 120)
}

function stopRouteLoadingFeedback() {
  window.clearTimeout(routeLoadingTimer)
  routeLoadingHideTimer = window.setTimeout(() => {
    showRouteLoading.value = false
    loadingRoutePath.value = ''
  }, 180)
}

onMounted(async () => {
  removeRouteBeforeGuard = router.beforeEach((to, from) => {
    if (to.fullPath !== from.fullPath) {
      startRouteLoadingFeedback(to.path)
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
/* 与 AppHeader 高度保持一致的 CSS 变量，子页面可引用 --header-height 计算偏移 */
.main-layout {
  --header-height: 82px;
  min-height: 100vh;
  background-color: var(--bg-page, #fff8f3);
  display: flex;
  flex-direction: column;
}

.layout-main {
  padding-top: var(--header-height);
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

.route-loading-placeholder {
  width: 100%;
  margin: 18px 0;
}

.route-loading-placeholder-card {
  width: min(720px, 100%);
  min-height: 156px;
  padding: 24px;
  border: 1px solid var(--border-card);
  border-radius: 12px;
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
}

.route-loading-placeholder-title {
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title);
}

.route-loading-placeholder-line {
  height: 12px;
  margin-top: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255, 140, 66, 0.12), rgba(255, 176, 122, 0.22));
}

.route-loading-placeholder-line.primary {
  width: 82%;
}

.route-loading-placeholder-line:not(.primary):not(.short) {
  width: 64%;
}

.route-loading-placeholder-line.short {
  width: 42%;
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

/* 移动端适配：同步 AppHeader 在各断点的高度 */
@media (max-width: 767px) {
  .main-layout {
    --header-height: 64px;
  }
  .layout-content {
    padding: 16px;
  }

  .route-loading-placeholder-card {
    min-height: 132px;
    padding: 18px;
  }
}

@media (max-width: 480px) {
  .main-layout {
    --header-height: 60px;
  }
  .layout-content {
    padding: 12px;
  }
}
</style>
