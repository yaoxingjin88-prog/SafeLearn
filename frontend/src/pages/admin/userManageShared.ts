export const USER_ROLE_OPTIONS = [
  { label: '全部角色', value: 'all' },
  { label: '普通学员', value: 'trainee' },
  { label: '安全管理员', value: 'admin' },
] as const

export const USER_STATUS_OPTIONS = [
  { label: '全部状态', value: 'all' },
  { label: '正常', value: 'active' },
  { label: '停用', value: 'disabled' },
] as const

export const USER_CERT_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '已持证', value: 'certified' },
  { label: '已过期', value: 'expired' },
  { label: '未持证', value: 'none' },
] as const

export const DEFAULT_DEPARTMENTS = ['运行部', '运维部', '安全部', '技术部', '综合部']

export function roleLabel(role?: string) {
  if (role === 'admin') return '安全管理员'
  if (role === 'trainee') return '普通学员'
  return role || '—'
}

export function certStatusLabel(status?: string) {
  if (status === 'certified') return '已持证'
  if (status === 'expired') return '已过期'
  return '未持证'
}

export function formatLoginTime(v?: string) {
  if (!v) return '—'
  try {
    const d = new Date(v)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch {
    return v
  }
}
