import { computed } from 'vue'
import { useRoute } from 'vue-router'

/** 用户端路由前缀：在 /user 下为 '/user'，管理端为空 */
export function useAppBase() {
  const route = useRoute()
  const prefix = computed(() => (route.path.startsWith('/user') ? '/user' : ''))

  function p(path: string) {
    const normalized = path.startsWith('/') ? path : `/${path}`
    return `${prefix.value}${normalized}`
  }

  return { prefix, p }
}
