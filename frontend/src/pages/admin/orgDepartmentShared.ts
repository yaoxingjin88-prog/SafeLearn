export function deptRoleLabel(role?: string) {
  if (role === 'admin') return '安全管理员'
  if (role === 'trainee') return '普通学员'
  return role || '—'
}

export function certStatusClass(status?: string) {
  if (status === 'valid') return 'cert-valid'
  if (status === 'expiring') return 'cert-expiring'
  if (status === 'expired') return 'cert-expired'
  return 'cert-none'
}

export const DEPT_TABS = [
  { key: 'members', label: '部门成员' },
  { key: 'positions', label: '岗位设置' },
  { key: 'stats', label: '部门统计' },
] as const

export type DeptTabKey = typeof DEPT_TABS[number]['key']
