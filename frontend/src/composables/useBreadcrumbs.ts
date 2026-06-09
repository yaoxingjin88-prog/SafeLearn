import { computed, type ComputedRef } from 'vue'
import { useRoute, type RouteLocationNormalizedLoaded } from 'vue-router'

export interface BreadcrumbItem {
  title: string
  path?: string
}

export interface BreadcrumbMenu {
  path: string
  title: string
  children?: { path: string; title: string }[]
}

function leafTitle(route: RouteLocationNormalizedLoaded): string {
  const matched = route.matched.filter(r => r.meta?.title)
  return (matched[matched.length - 1]?.meta?.title as string) || ''
}

export function buildBreadcrumbs(
  path: string,
  route: RouteLocationNormalizedLoaded,
  menus: BreadcrumbMenu[],
  topLevel: { path: string; title: string }[] = [],
): BreadcrumbItem[] {
  for (const top of topLevel) {
    if (path === top.path) {
      return [{ title: top.title }]
    }
  }

  for (const menu of menus) {
    if (menu.children?.length) {
      const sectionPath = menu.children[0]?.path

      for (const child of menu.children) {
        if (path === child.path) {
          return [
            { title: menu.title, path: sectionPath },
            { title: child.title },
          ]
        }
      }

      if (path.startsWith(`${menu.path}/`) || menu.children.some(c => path.startsWith(`${c.path}/`))) {
        const entry = menu.children.find(c => path.startsWith(c.path)) || menu.children[0]
        const title = leafTitle(route)
        if (title && title !== menu.title) {
          return [
            { title: menu.title, path: entry.path },
            { title },
          ]
        }
      }
    } else if (path === menu.path || path.startsWith(`${menu.path}/`)) {
      const title = leafTitle(route)
      return [{ title: title || menu.title }]
    }
  }

  const matched = route.matched.filter(r => r.meta?.title && !['/user', '/'].includes(r.path))
  if (matched.length) {
    return matched.map((r, index) => ({
      title: r.meta!.title as string,
      path: index < matched.length - 1 ? r.path : undefined,
    }))
  }

  return []
}

export function useBreadcrumbs(
  menus: ComputedRef<BreadcrumbMenu[]> | BreadcrumbMenu[],
  topLevel: { path: string; title: string }[] = [],
) {
  const route = useRoute()

  const breadcrumbs = computed(() => {
    const menuList = 'value' in menus ? menus.value : menus
    return buildBreadcrumbs(route.path, route, menuList, topLevel)
  })

  return { breadcrumbs }
}
