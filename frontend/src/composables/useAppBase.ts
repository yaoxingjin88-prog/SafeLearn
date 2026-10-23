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

  /** 将任务/外链路径规范为学员端可跳转地址（兼容带或不带 /user 前缀） */
  function resolveUserPath(link?: string | null) {
    if (!link) return p('/dashboard')
    const trimmed = link.trim()
    if (!trimmed) return p('/dashboard')
    if (trimmed.startsWith('/user/')) return trimmed
    if (trimmed.startsWith('/user')) return trimmed
    return p(trimmed.startsWith('/') ? trimmed : `/${trimmed}`)
  }

  return { prefix, p, resolveUserPath }
}
