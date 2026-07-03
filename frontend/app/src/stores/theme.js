import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const savedTheme = localStorage.getItem('theme')
  const savedFollowSystem = localStorage.getItem('followSystem')

  // 兼容旧版本：将 'system' 迁移为 followSystem + light
  const needsMigration = savedTheme === 'system'
  const manualTheme = ref(needsMigration ? 'light' : (savedTheme || 'light'))
  const followSystem = ref(needsMigration ? true : (savedFollowSystem === 'true'))

  if (needsMigration) {
    localStorage.setItem('theme', manualTheme.value)
    localStorage.setItem('followSystem', 'true')
  }

  const systemDark = ref(window.matchMedia('(prefers-color-scheme: dark)').matches)

  const resolvedTheme = computed(() => {
    if (followSystem.value) {
      return systemDark.value ? 'dark' : 'light'
    }
    return manualTheme.value
  })

  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const onSystemThemeChange = (e) => {
    systemDark.value = e.matches
  }
  mediaQuery.addEventListener('change', onSystemThemeChange)

  let transitionTimer = null
  const applyTheme = (resolved) => {
    // 添加过渡 class 让主题切换平滑，300ms 后自动移除避免常驻性能开销
    document.documentElement.classList.add('theme-transition')
    document.documentElement.setAttribute('data-theme', resolved)
    if (transitionTimer) clearTimeout(transitionTimer)
    transitionTimer = setTimeout(() => {
      document.documentElement.classList.remove('theme-transition')
      transitionTimer = null
    }, 300)
  }

  const toggleTheme = () => {
    if (followSystem.value) {
      followSystem.value = false
      manualTheme.value = systemDark.value ? 'light' : 'dark'
    } else {
      manualTheme.value = manualTheme.value === 'dark' ? 'light' : 'dark'
    }
    savePrefs()
    applyTheme(resolvedTheme.value)
  }

  const setTheme = (val) => {
    manualTheme.value = val
    followSystem.value = false
    savePrefs()
    applyTheme(resolvedTheme.value)
  }

  const setFollowSystem = (val) => {
    followSystem.value = val
    if (val) {
      manualTheme.value = resolvedTheme.value
    }
    savePrefs()
    applyTheme(resolvedTheme.value)
  }

  const savePrefs = () => {
    localStorage.setItem('theme', manualTheme.value)
    localStorage.setItem('followSystem', followSystem.value.toString())
  }

  applyTheme(resolvedTheme.value)

  return {
    manualTheme,
    resolvedTheme,
    followSystem,
    setTheme,
    setFollowSystem,
    toggleTheme
  }
})
