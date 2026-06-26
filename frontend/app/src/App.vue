<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="naiveThemeOverrides">
    <div
      v-if="showGlobalRouteLoading"
      class="global-route-loading"
      role="status"
      aria-live="polite"
    >
      <div class="global-route-loading-panel">
        <div class="global-route-loading-title">{{ globalRouteLoadingText }}</div>
        <div class="global-route-loading-line primary"></div>
        <div class="global-route-loading-line"></div>
      </div>
    </div>
    <component :is="layoutComponent">
      <router-view />
    </component>
  </n-config-provider>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { darkTheme, NConfigProvider } from 'naive-ui'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { getToken, removeToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const showGlobalRouteLoading = ref(false)
const globalRouteLoadingPath = ref('')
let globalRouteLoadingTimer = null
let globalRouteLoadingHideTimer = null
let removeGlobalRouteBeforeGuard = null
let removeGlobalRouteAfterGuard = null
let removeGlobalRouteErrorGuard = null

const layoutComponent = computed(() => {
  if (route.meta.useLayout) {
    return MainLayout
  }

  return 'div'
})

const naiveTheme = computed(() => (
  themeStore.resolvedTheme === 'dark' ? darkTheme : null
))

const naiveThemeOverrides = computed(() => {
  const isDark = themeStore.resolvedTheme === 'dark'

  return {
    common: {
      primaryColor: '#FF8C42',
      primaryColorHover: '#FFB07A',
      primaryColorPressed: '#E67A35',
      primaryColorSuppl: '#FF8C42',
      borderRadius: '8px',
      bodyColor: isDark ? '#1F1511' : '#FFF8F3',
      cardColor: isDark ? '#2A1B14' : '#FFFFFF',
      modalColor: isDark ? '#2A1B14' : '#FFFFFF',
      popoverColor: isDark ? '#2A1B14' : '#FFFFFF',
      textColorBase: isDark ? '#FFF3E8' : '#2F2F2F',
      textColor1: isDark ? '#FFF3E8' : '#2F2F2F',
      textColor2: isDark ? '#F0D1BD' : '#555555',
      textColor3: isDark ? '#CAA189' : '#888888',
      borderColor: isDark ? 'rgba(255, 175, 108, 0.24)' : '#F3D8C7'
    },
    Button: {
      borderRadiusLarge: '999px',
      borderRadiusMedium: '999px',
      borderRadiusSmall: '999px'
    },
    Tag: {
      borderRadius: '999px'
    },
    Switch: {
      railColorActive: '#FF8C42',
      loadingColor: '#FF8C42'
    },
    Slider: {
      fillColor: '#FF8C42',
      fillColorHover: '#FF6B1A',
      handleColor: '#FF8C42',
      handleColorHover: '#FF6B1A'
    },
    Skeleton: {
      color: isDark ? 'rgba(255, 243, 232, 0.08)' : '#F5E8DF',
      colorEnd: isDark ? 'rgba(255, 176, 122, 0.14)' : '#FBF1EA'
    }
  }
})

const isGlobalRouteLoadingRoute = (targetPath = '') => (
  (targetPath.startsWith('/admin') && targetPath !== '/admin/login')
  || targetPath.startsWith('/interview/session')
  || targetPath.startsWith('/templates/editor')
)

const globalRouteLoadingText = computed(() => {
  if (globalRouteLoadingPath.value.startsWith('/admin')) return '正在打开管理端'
  if (globalRouteLoadingPath.value.startsWith('/interview/session')) return '正在打开面试会话'
  if (globalRouteLoadingPath.value.startsWith('/templates/editor')) return '正在打开模板编辑器'
  return '正在打开页面'
})

function startGlobalRouteLoadingFeedback(targetPath = '') {
  window.clearTimeout(globalRouteLoadingTimer)
  window.clearTimeout(globalRouteLoadingHideTimer)
  showGlobalRouteLoading.value = false
  globalRouteLoadingPath.value = ''

  if (!isGlobalRouteLoadingRoute(targetPath)) return

  globalRouteLoadingPath.value = targetPath
  // 无全局布局的冷路由先延迟再显示反馈，避免快速切换时闪一下。
  globalRouteLoadingTimer = window.setTimeout(() => {
    showGlobalRouteLoading.value = true
  }, 120)
}

function stopGlobalRouteLoadingFeedback() {
  window.clearTimeout(globalRouteLoadingTimer)
  globalRouteLoadingHideTimer = window.setTimeout(() => {
    showGlobalRouteLoading.value = false
    globalRouteLoadingPath.value = ''
  }, 180)
}

onMounted(async () => {
  removeGlobalRouteBeforeGuard = router.beforeEach((to, from) => {
    if (to.fullPath !== from.fullPath) {
      startGlobalRouteLoadingFeedback(to.path)
    }
  })
  removeGlobalRouteAfterGuard = router.afterEach(async () => {
    await nextTick()
    stopGlobalRouteLoadingFeedback()
  })
  removeGlobalRouteErrorGuard = router.onError(() => {
    stopGlobalRouteLoadingFeedback()
  })

  const token = getToken()

  if (!token) return

  try {
    await userStore.fetchUserInfo()
  } catch (err) {
    removeToken()
    userStore.clearUserInfo()
  }
})

onUnmounted(() => {
  removeGlobalRouteBeforeGuard?.()
  removeGlobalRouteAfterGuard?.()
  removeGlobalRouteErrorGuard?.()
  window.clearTimeout(globalRouteLoadingTimer)
  window.clearTimeout(globalRouteLoadingHideTimer)
})
</script>

<style>
.global-route-loading {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-page, #fff8f3);
}

.global-route-loading-panel {
  width: min(420px, 100%);
  padding: 24px;
  border: 1px solid var(--border-card, #f3d8c7);
  border-radius: 8px;
  background: var(--bg-card, #fff);
  box-shadow: 0 18px 42px rgba(132, 75, 32, 0.12);
}

.global-route-loading-title {
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-title, #2f2f2f);
}

.global-route-loading-line {
  height: 12px;
  margin-top: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255, 140, 66, 0.12), rgba(255, 176, 122, 0.24));
  background-size: 180% 100%;
  animation: global-route-loading-shimmer 1.2s ease-in-out infinite;
}

.global-route-loading-line.primary {
  width: 78%;
}

.global-route-loading-line:not(.primary) {
  width: 58%;
}

@keyframes global-route-loading-shimmer {
  from {
    background-position: 180% 0;
  }
  to {
    background-position: -80% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .global-route-loading-line {
    animation: none;
  }
}
</style>
