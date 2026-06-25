export const PERMISSION_PREFIX = 'perm:'

export function buildPermissionCode(module: string, action = 'view') {
  return `${PERMISSION_PREFIX}${module}:${action}`
}

export const ROUTE_PERMISSIONS: Record<string, { module: string; action?: string }> = {
  '/dashboard': { module: 'dashboard', action: 'view' },
  '/dashboard/reports': { module: 'report', action: 'view' },
  '/dashboard/alerts': { module: 'hazard', action: 'view' },
  '/dashboard/notifications': { module: 'dashboard', action: 'view' },
  '/dashboard/messages': { module: 'dashboard', action: 'view' },
  '/admin/learning/courses': { module: 'training', action: 'view' },
  '/admin/learning/monitoring': { module: 'training', action: 'view' },
  '/admin/learning/exams': { module: 'exam', action: 'view' },
  '/admin/learning/question-bank': { module: 'exam', action: 'view' },
  '/admin/learning/paper-assembly': { module: 'exam', action: 'view' },
  '/admin/org': { module: 'organization', action: 'view' },
  '/admin/users': { module: 'organization', action: 'view' },
  '/admin/roles': { module: 'permission', action: 'view' },
  '/admin/settings': { module: 'permission', action: 'view' },
}

export function resolveRoutePermission(path: string) {
  if (ROUTE_PERMISSIONS[path]) {
    return ROUTE_PERMISSIONS[path]
  }
  if (path.startsWith('/admin/learning/courses')) return { module: 'training', action: 'view' }
  if (path.startsWith('/admin/learning/question-bank')) return { module: 'exam', action: 'view' }
  if (path.startsWith('/admin/users/')) return { module: 'organization', action: 'view' }
  return null
}
