import { computed } from 'vue'
import { useUserStore } from '@/stores'
import { buildPermissionCode } from '@/utils/permissions'

export function usePermissions() {
  const userStore = useUserStore()

  const permissions = computed(() => userStore.userInfo?.permissions ?? [])

  function hasPermission(module: string, action = 'view') {
    if (userStore.role !== 'admin') return false
    const code = buildPermissionCode(module, action)
    const list = permissions.value
    if (!list.length) return true
    return list.includes(code)
  }

  function canAccessModule(module: string) {
    return hasPermission(module, 'view')
  }

  return {
    permissions,
    hasPermission,
    canAccessModule,
  }
}
