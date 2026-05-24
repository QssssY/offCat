<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="naiveThemeOverrides">
    <component :is="layoutComponent">
      <router-view />
    </component>
  </n-config-provider>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { darkTheme, NConfigProvider } from 'naive-ui'
import MainLayout from '@/layouts/MainLayout.vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { getToken, removeToken } from '@/utils/auth'

const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

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
    Skeleton: {
      color: isDark ? 'rgba(255, 140, 66, 0.12)' : '#F3D8C7',
      colorEnd: isDark ? 'rgba(255, 176, 122, 0.18)' : '#FFF3E8'
    }
  }
})

onMounted(async () => {
  const token = getToken()

  if (!token) return

  try {
    await userStore.fetchUserInfo()
  } catch (err) {
    removeToken()
    userStore.clearUserInfo()
  }
})
</script>

<style>
</style>
