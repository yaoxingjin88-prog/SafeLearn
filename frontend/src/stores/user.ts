import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types'
import { buildPermissionCode } from '@/utils/permissions'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const role = computed(() => userInfo.value?.role || 'trainee')
  const permissions = computed(() => userInfo.value?.permissions ?? [])
  const permissionRoleName = computed(() => userInfo.value?.permissionRoleName || '')

  function hasPermission(module: string, action = 'view') {
    if (role.value !== 'admin') return false
    const code = buildPermissionCode(module, action)
    const list = permissions.value
    if (!list.length) return true
    return list.includes(code)
  }

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    if (res.data.user) {
      userInfo.value = res.data.user as UserInfo
    } else {
      await getUserInfo()
    }
    return res
  }

  async function getUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = res.data
    return res
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    role,
    permissions,
    permissionRoleName,
    hasPermission,
    login,
    getUserInfo,
    logout,
  }
})
