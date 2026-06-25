export const ALERT_LEVEL_OPTIONS = [
  { label: '全部等级', value: 'all' },
  { label: '高危', value: 'danger' },
  { label: '中危', value: 'warning' },
  { label: '低危', value: 'info' },
] as const

export const ALERT_LEVEL_TAB_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '高危', value: 'danger' },
  { label: '中危', value: 'warning' },
  { label: '低危', value: 'info' },
] as const

export const ALERT_TYPE_OPTIONS = [
  { label: '全部类型', value: 'all' },
  { label: '考试不合格', value: 'exam_fail' },
  { label: '考试异常', value: 'exam_abnormal' },
  { label: '培训进度', value: 'progress_lag' },
  { label: '课程未完成', value: 'course_incomplete' },
  { label: '学习任务', value: 'task_overdue' },
  { label: '缺考预警', value: 'exam_absent' },
  { label: '复训提醒', value: 'retrain' },
  { label: '证书到期', value: 'certificate_expire' },
] as const

export const ALERT_STATUS_OPTIONS = [
  { label: '全部状态', value: 'all' },
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已闭环', value: 'closed' },
] as const

export function alertLevelLabel(level: 'danger' | 'warning' | 'info') {
  return { danger: '高危', warning: '中危', info: '低危' }[level]
}

export function alertTypeLabel(type?: string) {
  const map: Record<string, string> = {
    exam: '考试异常',
    exam_fail: '考试不合格',
    exam_abnormal: '考试异常',
    progress: '培训进度',
    progress_lag: '培训进度',
    course_incomplete: '课程未完成',
    task_overdue: '学习任务',
    exam_absent: '缺考预警',
    retrain: '复训提醒',
    certificate: '证书到期',
    certificate_expire: '证书到期',
    training: '应急训练',
    normal: '系统状态',
  }
  return type ? map[type] || type : '—'
}

export function alertStatusLabel(status?: string) {
  const map: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    closed: '已闭环',
  }
  return status ? map[status] || status : '—'
}

export function alertActionLabel(type?: string) {
  const map: Record<string, string> = {
    exam: '查看考试',
    exam_fail: '查看考试',
    exam_abnormal: '查看考试',
    progress: '学习监控',
    progress_lag: '学习监控',
    course_incomplete: '学习监控',
    task_overdue: '学习监控',
    training: '学习监控',
    certificate: '组织与部门',
    certificate_expire: '组织与部门',
    normal: '返回首页',
  }
  return type ? map[type] || '去处理' : '去处理'
}
