import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminLogin, getAdminCurrentUser } from '@/api/admin/auth'
import {
  clearAdminSession,
  setAdminRole,
  setAdminSession
} from '@/utils/adminAuth'

export const useAdminUserStore = defineStore('adminUser', () => {
  // 管理端用户信息（仅用于管理端页面展示和权限判断）
  const adminInfo = ref(null)
  const loading = ref(false)

  /**
   * 清空管理端用户状态。
   */
  const clearAdminInfo = () => {
    adminInfo.value = null
  }

  /**
   * 拉取管理端当前用户信息并校验管理员角色。
   * 角色约定：role === 9 才允许进入管理端。
   */
  const fetchAdminInfo = async () => {
    const res = await getAdminCurrentUser()
    const user = res?.data || null

    if (!user || user.role !== 9) {
      clearAdminSession()
      clearAdminInfo()
      throw new Error('当前账号不是管理员，无法进入管理端')
    }

    adminInfo.value = user
    setAdminRole(user.role)
    return user
  }

  /**
   * 管理端登录流程：
   * 1. 调用登录接口拿 token
   * 2. 先存会话
   * 3. 拉取 /api/auth/me 并强校验管理员角色
   */
  const doAdminLogin = async (loginData) => {
    loading.value = true
    try {
      const loginRes = await adminLogin(loginData)
      const token = loginRes?.data?.token
      const tokenType = loginRes?.data?.tokenType || 'Bearer'

      if (!token) {
        throw new Error('登录成功但未返回有效 token')
      }

      // 先写入临时会话，便于后续 /me 调用携带认证头。
      setAdminSession(token, tokenType, 0)
      await fetchAdminInfo()
      return loginRes
    } catch (error) {
      clearAdminSession()
      clearAdminInfo()
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 管理端退出登录。
   */
  const doAdminLogout = () => {
    clearAdminSession()
    clearAdminInfo()
  }

  return {
    adminInfo,
    loading,
    clearAdminInfo,
    fetchAdminInfo,
    doAdminLogin,
    doAdminLogout
  }
})
