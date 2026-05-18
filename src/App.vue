<template>
  <component :is="layoutComponent">
    <router-view />
  </component>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
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
