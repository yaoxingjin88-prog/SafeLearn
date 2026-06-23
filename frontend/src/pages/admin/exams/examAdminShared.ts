export const EXAM_TYPE_OPTIONS = [
  { label: '全部类型', value: 'all' },
  { label: '正式考试', value: 'formal' },
  { label: '模拟考试', value: 'mock' },
]

export const EXAM_STATUS_OPTIONS = [
  { label: '全部状态', value: 'all' },
  { label: '已发布', value: 'published' },
  { label: '待发布', value: 'pending' },
  { label: '草稿', value: 'draft' },
  { label: '已结束', value: 'ended' },
]

export const DEPARTMENT_FILTER_OPTIONS = [
  '全部部门', '运维部', '安全部', '培训部', '技术部', '检修维护部', '消防与应急管理部', '技术管理部', '全体员工',
]

export function examTypeLabel(type?: string) {
  if (type === 'formal') return '正式考试'
  if (type === 'mock') return '模拟考试'
  return type || '-'
}

export function examTypeTagType(type?: string) {
  if (type === 'formal') return 'primary'
  if (type === 'mock') return ''
  return 'info'
}

export function examStatusLabel(status?: string) {
  const map: Record<string, string> = {
    published: '已发布',
    pending: '待发布',
    draft: '草稿',
    ended: '已结束',
  }
  return map[status || ''] || status || '-'
}

export function examStatusClass(status?: string) {
  return `exam-status-${status || 'draft'}`
}

export function formatExamDuration(minutes?: number | null) {
  if (!minutes) return '不限时'
  return `${minutes} 分钟`
}
