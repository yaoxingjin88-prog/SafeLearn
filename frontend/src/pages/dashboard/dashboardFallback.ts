import type { AdminDashboardData } from '@/api/admin'

/** 接口不可用时的演示数据，布局与首页设计稿一致 */
export function createFallbackDashboard(): AdminDashboardData {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const generatedAt = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`

  const labels: string[] = []
  const completedUsers: number[] = []
  const completionRates: number[] = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    labels.push(`${pad(d.getMonth() + 1)}-${pad(d.getDate())}`)
    const done = i === 0 ? 18 : 12 + (6 - i) * 2
    completedUsers.push(done)
    completionRates.push(Math.round(done * 100 / 1248 * 100) / 100)
  }

  return {
    stats: {
      traineeCount: 1248,
      completedTraining: 986,
      incompleteTraining: 262,
      completionRate: 79.01,
      examPassRate: 92.35,
      safetyAlerts: 8,
      traineeDelta: 18,
      completedDelta: 18,
      incompleteDelta: -6,
      passRateDelta: 2.3,
      alertsDelta: -2,
    },
    completionTrend: { labels, completedUsers, completionRates },
    categoryCompletion: [
      { code: 'basic', name: '储能基础知识', rate: 85, color: '#3b82f6' },
      { code: 'operation', name: '设备安全管理', rate: 78, color: '#10b981' },
      { code: 'fire', name: '消防安全', rate: 72, color: '#f59e0b' },
      { code: 'thermal', name: '热失控防控', rate: 68, color: '#ef4444' },
      { code: 'emergency', name: '应急处置', rate: 65, color: '#8b5cf6' },
      { code: 'bms', name: 'BMS 系统', rate: 58, color: '#06b6d4' },
    ],
    departments: ['运维部', '安全部', '培训部'],
    categoryCompletionByDepartment: {
      运维部: [
        { code: 'basic', name: '储能基础知识', rate: 88, color: '#3b82f6' },
        { code: 'operation', name: '设备安全管理', rate: 82, color: '#10b981' },
        { code: 'fire', name: '消防安全', rate: 75, color: '#f59e0b' },
        { code: 'thermal', name: '热失控防控', rate: 70, color: '#ef4444' },
      ],
      安全部: [
        { code: 'fire', name: '消防安全', rate: 92, color: '#f59e0b' },
        { code: 'emergency', name: '应急处置', rate: 86, color: '#8b5cf6' },
        { code: 'basic', name: '储能基础知识', rate: 80, color: '#3b82f6' },
        { code: 'thermal', name: '热失控防控', rate: 74, color: '#ef4444' },
      ],
      培训部: [
        { code: 'basic', name: '储能基础知识', rate: 76, color: '#3b82f6' },
        { code: 'bms', name: 'BMS 系统', rate: 68, color: '#06b6d4' },
        { code: 'operation', name: '设备安全管理', rate: 62, color: '#10b981' },
      ],
    },
    departmentOverview: {
      运维部: { traineeCount: 420, completedTraining: 356, completionRate: 84.76 },
      安全部: { traineeCount: 380, completedTraining: 312, completionRate: 82.11 },
      培训部: { traineeCount: 448, completedTraining: 318, completionRate: 70.98 },
    },
    trainingPlans: [
      { id: '1', name: '2024年度储能安全基础培训', department: '全部部门', learned: 890, target: 1120, rate: 79.5, status: 'in_progress' },
      { id: '2', name: '热失控应急处置专项培训', department: '运维部', learned: 156, target: 180, rate: 86.7, status: 'in_progress' },
      { id: '3', name: '消防安全操作规范培训', department: '安全部', learned: 98, target: 120, rate: 81.7, status: 'in_progress' },
      { id: '4', name: 'BMS 系统运维认证培训', department: '技术部', learned: 45, target: 80, rate: 56.3, status: 'in_progress' },
    ],
    alerts: [
      { title: '热失控预警', description: '3号储能舱温度异常升高，当前 45°C，超过预警阈值', level: 'danger', time: '2024-05-14 09:32' },
      { title: '培训进度滞后', description: '运维部 12 名学员未完成本月必修课程', level: 'warning', time: '2024-05-14 08:15' },
      { title: '证书即将到期', description: '5 名学员安全操作证书将在 30 天内到期', level: 'warning', time: '2024-05-13 16:40' },
      { title: '系统维护提醒', description: '计划于 5 月 20 日 02:00-04:00 进行系统维护', level: 'info', time: '2024-05-13 10:00' },
    ],
    announcements: [
      { title: '关于开展2024年度储能安全培训的通知', date: '2024-05-14', pinned: true },
      { title: '热失控应急处置专项培训将于下周启动', date: '2024-05-13', pinned: false },
      { title: '系统将于5月20日凌晨进行维护升级', date: '2024-05-12', pinned: false },
      { title: '新增3门储能安全必修课程已上线', date: '2024-05-10', pinned: false },
      { title: '2024年第二季度安全考核结果公示', date: '2024-05-08', pinned: false },
    ],
    calendarEvents: [
      { date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-05`, type: 'planned' },
      { date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-08`, type: 'in_progress' },
      { date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-12`, type: 'completed' },
      { date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-15`, type: 'planned' },
      { date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-18`, type: 'in_progress' },
      { date: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-22`, type: 'completed' },
    ],
    generatedAt,
  }
}
