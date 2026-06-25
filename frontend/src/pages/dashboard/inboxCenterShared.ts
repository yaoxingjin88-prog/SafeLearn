export const NOTIFICATION_LEVEL_OPTIONS = [
  { label: '全部等级', value: 'all' },
  { label: '高危', value: 'danger' },
  { label: '中危', value: 'warning' },
  { label: '提醒', value: 'info' },
] as const

export const NOTIFICATION_TYPE_OPTIONS = [
  { label: '全部类型', value: 'all' },
  { label: '考试异常', value: 'exam' },
  { label: '培训进度', value: 'progress' },
  { label: '应急训练', value: 'training' },
  { label: '证书到期', value: 'certificate' },
] as const

export const MESSAGE_TYPE_OPTIONS = [
  { label: '全部类型', value: 'all' },
  { label: '平台公告', value: 'announcement' },
  { label: '课程上线', value: 'course' },
  { label: '考试发布', value: 'exam' },
  { label: '系统通知', value: 'system' },
] as const

export function notificationLevelLabel(level: 'danger' | 'warning' | 'info') {
  return { danger: '高危', warning: '中危', info: '提醒' }[level]
}

export function notificationTypeLabel(type?: string) {
  const map: Record<string, string> = {
    exam: '考试',
    progress: '培训进度',
    training: '应急训练',
    certificate: '证书',
  }
  return type ? map[type] || type : '—'
}

export function messageTypeLabel(type?: string) {
  const map: Record<string, string> = {
    announcement: '平台公告',
    course: '课程上线',
    exam: '考试发布',
    system: '系统通知',
  }
  return type ? map[type] || type : '—'
}

export function inboxActionLabel(type?: string) {
  const map: Record<string, string> = {
    exam: '查看考试',
    progress: '学习监控',
    training: '学习监控',
    certificate: '组织与部门',
    course: '查看课程',
    announcement: '返回首页',
    system: '返回首页',
  }
  return type ? map[type] || '查看详情' : '查看详情'
}

export function navigateInboxAction(router: { push: (target: string | object) => void }, path: string) {
  const [pathname, search] = path.split('?')
  router.push(search ? { path: pathname, query: Object.fromEntries(new URLSearchParams(search)) } : path)
}
