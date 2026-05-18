import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { login, getCurrentUser } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const loading = ref(false)

  const clearUserInfo = () => {
    userInfo.value = null
  }

  const isLoggedIn = () => !!userInfo.value

  const isVip = () => {
    const role = userInfo.value?.role
    const vipExpireTime = userInfo.value?.vipExpireTime

    if (!vipExpireTime) return false

    return role === 1 && new Date(vipExpireTime) > new Date()
  }

  const fetchUserInfo = async () => {
    const token = getToken()

    if (!token) return

    try {
      const res = await getCurrentUser()
      userInfo.value = res.data
      return res
    } catch (err) {
      clearUserInfo()
      throw err
    }
  }

  const doLogin = async (loginData) => {
    loading.value = true

    try {
      const res = await login(loginData)
      const { token: tokenStr, tokenType } = res.data

      setToken(tokenStr, tokenType)
      await fetchUserInfo()

      return res
    } finally {
      loading.value = false
    }
  }

  const doLogout = () => {
    removeToken()
    clearUserInfo()
  }

  return {
    userInfo,
    loading,
    fetchUserInfo,
    clearUserInfo,
    isLoggedIn,
    isVip,
    doLogin,
    doLogout
  }
})
