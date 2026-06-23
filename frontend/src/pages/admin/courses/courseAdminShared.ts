import type { CourseCategory } from '@/types'

export const DEPARTMENT_OPTIONS = ['全部部门', '运维部', '安全部', '培训部', '技术部', '全体员工']

export const STATUS_OPTIONS = [
  { label: '全部状态', value: 'all' },
  { label: '已发布', value: 'published' },
  { label: '草稿', value: 'draft' },
]

export function categoryTagType(code: string, categories: CourseCategory[]) {
  const tag = categories.find(c => c.code === code)?.tagType
  if (tag === 'success' || tag === 'warning' || tag === 'danger' || tag === 'info') return tag
  return ''
}

export function categoryLabel(code: string, categories: CourseCategory[]) {
  return categories.find(c => c.code === code)?.name || code
}

export function statusLabel(status?: string) {
  return status === 'published' ? '已发布' : status === 'draft' ? '草稿' : status || '-'
}

export function statusClass(status?: string) {
  return status === 'published' ? 'is-published' : 'is-draft'
}

export function formatDuration(minutes?: number) {
  if (!minutes) return '-'
  return `${minutes} 分钟`
}

export function coverUrl(url?: string) {
  return url || '/images/default-course.svg'
}
