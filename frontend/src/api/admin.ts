import request from './request'
import type { ApiResponse, Chapter, CourseCategory } from '@/types'

export const adminApi = {
  getDashboard(): Promise<ApiResponse<AdminDashboardData>> {
    return request.get('/admin/dashboard')
  },

  getAnalytics(params?: AdminAnalyticsQuery): Promise<ApiResponse<AdminAnalyticsData>> {
    return request.get('/admin/analytics', { params })
  },

  getLearningReport(params?: AdminLearningReportQuery): Promise<ApiResponse<AdminLearningReportData>> {
    return request.get('/admin/learning-report', { params })
  },

  sendLearningReminder(data: AdminLearningReminderPayload): Promise<ApiResponse<AdminLearningReminderResult>> {
    return request.post('/admin/learning-report/remind', data)
  },

  listRoles(): Promise<ApiResponse<AdminRoleSummary[]>> {
    return request.get('/admin/roles')
  },

  getRoleModules(): Promise<ApiResponse<AdminRoleModuleMeta>> {
    return request.get('/admin/roles/modules')
  },

  getRole(id: string): Promise<ApiResponse<AdminRoleDetail>> {
    return request.get(`/admin/roles/${encodeURIComponent(id)}`)
  },

  createRole(data: AdminRolePayload): Promise<ApiResponse<AdminRoleDetail>> {
    return request.post('/admin/roles', data)
  },

  updateRole(id: string, data: Partial<AdminRolePayload>): Promise<ApiResponse<AdminRoleDetail>> {
    return request.put(`/admin/roles/${encodeURIComponent(id)}`, data)
  },

  publishRole(id: string): Promise<ApiResponse<AdminRoleDetail & { message?: string }>> {
    return request.post(`/admin/roles/${encodeURIComponent(id)}/publish`)
  },

  deleteRole(id: string): Promise<ApiResponse<{ success: boolean }>> {
    return request.delete(`/admin/roles/${encodeURIComponent(id)}`)
  },

  getAlertCenter(params?: AdminAlertSearchParams): Promise<ApiResponse<AdminAlertCenterPage>> {
    return request.get('/admin/alerts', { params })
  },

  getAlertStats(): Promise<ApiResponse<AdminAlertStats>> {
    return request.get('/admin/alerts/stats')
  },

  getAlertDetail(id: string): Promise<ApiResponse<AdminTrainingAlertItem>> {
    return request.get(`/admin/alerts/${encodeURIComponent(id)}`)
  },

  createAlert(data: Partial<AdminTrainingAlertItem>): Promise<ApiResponse<AdminTrainingAlertItem>> {
    return request.post('/admin/alerts', data)
  },

  updateAlert(id: string, data: Partial<AdminTrainingAlertItem>): Promise<ApiResponse<AdminTrainingAlertItem>> {
    return request.put(`/admin/alerts/${encodeURIComponent(id)}`, data)
  },

  updateAlertStatus(id: string, data: { status: string; responsiblePerson?: string; remark?: string }): Promise<ApiResponse<AdminTrainingAlertItem>> {
    return request.patch(`/admin/alerts/${encodeURIComponent(id)}/status`, data)
  },

  exportAlerts(params?: AdminAlertSearchParams): Promise<ApiResponse<AdminTrainingAlertItem[]>> {
    return request.get('/admin/alerts/export', { params })
  },

  getNotificationSummary(): Promise<ApiResponse<AdminInboxSummary<AdminNotificationItem>>> {
    return request.get('/admin/notifications/summary')
  },

  getMessageSummary(): Promise<ApiResponse<AdminInboxSummary<AdminMessageItem>>> {
    return request.get('/admin/messages/summary')
  },

  listNotifications(params?: { page?: number; pageSize?: number; unreadOnly?: boolean }): Promise<ApiResponse<AdminInboxPage<AdminNotificationItem>>> {
    return request.get('/admin/notifications', { params })
  },

  listMessages(params?: { page?: number; pageSize?: number; unreadOnly?: boolean }): Promise<ApiResponse<AdminInboxPage<AdminMessageItem>>> {
    return request.get('/admin/messages', { params })
  },

  markNotificationRead(id: string): Promise<ApiResponse<AdminNotificationItem>> {
    return request.put(`/admin/notifications/${encodeURIComponent(id)}/read`)
  },

  markAllNotificationsRead(): Promise<ApiResponse<{ success: boolean }>> {
    return request.put('/admin/notifications/read-all')
  },

  markMessageRead(id: string): Promise<ApiResponse<AdminMessageItem>> {
    return request.put(`/admin/messages/${encodeURIComponent(id)}/read`)
  },

  markAllMessagesRead(): Promise<ApiResponse<{ success: boolean }>> {
    return request.put('/admin/messages/read-all')
  },

  createMessage(data: AdminMessagePayload): Promise<ApiResponse<AdminMessageItem>> {
    return request.post('/admin/messages', data)
  },

  getMessage(id: string): Promise<ApiResponse<AdminMessageItem>> {
    return request.get(`/admin/messages/${encodeURIComponent(id)}`)
  },

  updateMessage(id: string, data: Partial<AdminMessagePayload>): Promise<ApiResponse<AdminMessageItem>> {
    return request.put(`/admin/messages/${encodeURIComponent(id)}`, data)
  },

  deleteMessage(id: string): Promise<ApiResponse<{ success: boolean }>> {
    return request.delete(`/admin/messages/${encodeURIComponent(id)}`)
  },

  getCourseCategories(): Promise<ApiResponse<CourseCategory[]>> {
    return request.get('/admin/course-categories')
  },

  createCourseCategory(data: Partial<CourseCategory>): Promise<ApiResponse<CourseCategory>> {
    return request.post('/admin/course-categories', data)
  },

  updateCourseCategory(id: string, data: Partial<CourseCategory>): Promise<ApiResponse<CourseCategory>> {
    return request.put(`/admin/course-categories/${id}`, data)
  },

  deleteCourseCategory(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/course-categories/${id}`)
  },

  getCourses(params?: AdminCourseQuery): Promise<ApiResponse<AdminCoursePage>> {
    return request.get('/admin/courses', { params })
  },

  getCourseById(id: string): Promise<ApiResponse<AdminCourseDetail>> {
    return request.get(`/admin/courses/${id}`)
  },

  createCourse(data: Partial<AdminCoursePayload>): Promise<ApiResponse<AdminCourseDetail>> {
    return request.post('/admin/courses', data)
  },

  updateCourse(id: string, data: Partial<AdminCoursePayload>): Promise<ApiResponse<AdminCourseDetail>> {
    return request.put(`/admin/courses/${id}`, data)
  },

  deleteCourse(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/courses/${id}`)
  },

  getChapters(courseId: string): Promise<ApiResponse<Chapter[]>> {
    return request.get(`/admin/courses/${courseId}/chapters`)
  },

  createChapter(courseId: string, data: Partial<AdminChapterPayload>): Promise<ApiResponse<Chapter>> {
    return request.post(`/admin/courses/${courseId}/chapters`, data)
  },

  updateChapter(courseId: string, chapterId: string, data: Partial<AdminChapterPayload>): Promise<ApiResponse<Chapter>> {
    return request.put(`/admin/courses/${courseId}/chapters/${chapterId}`, data)
  },

  deleteChapter(courseId: string, chapterId: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/courses/${courseId}/chapters/${chapterId}`)
  },

  getCourseMonitoring(courseId: string): Promise<ApiResponse<AdminCourseMonitoringSummary>> {
    return request.get(`/admin/courses/${courseId}/monitoring`)
  },

  getCourseLearners(courseId: string, params?: AdminCourseLearnerQuery): Promise<ApiResponse<AdminCourseLearnerPage>> {
    return request.get(`/admin/courses/${courseId}/learners`, { params })
  },

  getCourseLearnerDetail(courseId: string, userId: string): Promise<ApiResponse<AdminCourseLearnerDetail>> {
    return request.get(`/admin/courses/${courseId}/learners/${userId}`)
  },

  getExams(params?: AdminExamQuery): Promise<ApiResponse<AdminExamPage>> {
    return request.get('/admin/exams', { params })
  },

  getExamById(id: string): Promise<ApiResponse<AdminExamListItem>> {
    return request.get(`/admin/exams/${encodeURIComponent(id)}`)
  },

  getExamStats(id: string): Promise<ApiResponse<AdminExamStats>> {
    return request.get(`/admin/exams/${encodeURIComponent(id)}/stats`)
  },

  updateExam(id: string, data: Partial<AdminExamPayload>): Promise<ApiResponse<AdminExamListItem>> {
    return request.put(`/admin/exams/${encodeURIComponent(id)}`, data)
  },

  deleteExam(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/exams/${encodeURIComponent(id)}`)
  },

  getQuestionCategories(keyword?: string): Promise<ApiResponse<AdminQuestionCategoryNode[]>> {
    return request.get('/admin/question-categories', { params: { keyword } })
  },

  getQuestions(params?: AdminQuestionQuery): Promise<ApiResponse<AdminQuestionPage>> {
    return request.get('/admin/questions', { params })
  },

  getQuestionTypeCounts(params?: Omit<AdminQuestionQuery, 'page' | 'pageSize' | 'type'>): Promise<ApiResponse<Record<string, number>>> {
    return request.get('/admin/questions/type-counts', { params })
  },

  getQuestionTags(): Promise<ApiResponse<string[]>> {
    return request.get('/admin/questions/tags')
  },

  getQuestionById(id: string): Promise<ApiResponse<AdminQuestionDetail>> {
    return request.get(`/admin/questions/${id}`)
  },

  createQuestion(data: AdminQuestionPayload): Promise<ApiResponse<AdminQuestionDetail>> {
    return request.post('/admin/questions', data)
  },

  updateQuestion(id: string, data: Partial<AdminQuestionPayload>): Promise<ApiResponse<AdminQuestionDetail>> {
    return request.put(`/admin/questions/${id}`, data)
  },

  deleteQuestion(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/questions/${id}`)
  },

  batchOperateQuestions(data: { ids: string[]; action: string }): Promise<ApiResponse<{ success: boolean; count: number }>> {
    return request.post('/admin/questions/batch', data)
  },

  getPaperCategoryOptions(): Promise<ApiResponse<Array<{ id: string; name: string }>>> {
    return request.get('/admin/papers/category-options')
  },

  getPaperDefaultConfig(): Promise<ApiResponse<Record<string, unknown>>> {
    return request.get('/admin/papers/default-config')
  },

  generatePaper(data: { config: Record<string, unknown> }): Promise<ApiResponse<AdminPaperGenerateResult>> {
    return request.post('/admin/papers/generate', data)
  },

  getPaperById(id: string): Promise<ApiResponse<AdminPaperDetail>> {
    return request.get(`/admin/papers/${id}`)
  },

  savePaper(data: AdminPaperPayload): Promise<ApiResponse<AdminPaperDetail>> {
    return request.post('/admin/papers', data)
  },

  updatePaper(id: string, data: AdminPaperPayload): Promise<ApiResponse<AdminPaperDetail>> {
    return request.put(`/admin/papers/${id}`, data)
  },

  publishPaper(id: string): Promise<ApiResponse<AdminPaperDetail>> {
    return request.post(`/admin/papers/${id}/publish`)
  },

  publishPaperDirect(data: AdminPaperPayload): Promise<ApiResponse<AdminPaperDetail>> {
    return request.post('/admin/papers/publish', data)
  },

  getSystemConfigs(): Promise<ApiResponse<SystemConfigItem[]>> {
    return request.get('/admin/system-configs')
  },

  updateSystemConfig(id: string, data: { value: unknown }): Promise<ApiResponse<SystemConfigItem>> {
    return request.put(`/admin/system-configs/${id}`, data)
  },

  getUsers(params?: AdminUserQuery): Promise<ApiResponse<AdminUserPage>> {
    return request.get('/admin/users', { params })
  },

  getUserFilterOptions(): Promise<ApiResponse<AdminUserFilterOptions>> {
    return request.get('/admin/users/filter-options')
  },

  createUser(data: AdminUserPayload): Promise<ApiResponse<AdminUserListItem>> {
    return request.post('/admin/users', data)
  },

  updateUser(id: string, data: AdminUserPayload): Promise<ApiResponse<{ success: boolean }>> {
    return request.put(`/admin/users/${id}`, data)
  },

  updateUserStatus(id: string, enabled: boolean): Promise<ApiResponse<{ success: boolean; enabled: boolean }>> {
    return request.patch(`/admin/users/${id}/status`, { enabled })
  },

  batchOperateUsers(data: { ids: string[]; action: 'enable' | 'disable' | 'delete' }): Promise<ApiResponse<{ success: boolean; affected: number }>> {
    return request.post('/admin/users/batch', data)
  },

  deleteUser(id: string): Promise<ApiResponse<{ success: boolean }>> {
    return request.delete(`/admin/users/${id}`)
  },

  getUserDetail(id: string): Promise<ApiResponse<AdminUserDetail>> {
    return request.get(`/admin/users/${id}/detail`)
  },

  updateUserTags(id: string, tags: string[]): Promise<ApiResponse<{ success: boolean; tags: string[] }>> {
    return request.put(`/admin/users/${id}/tags`, { tags })
  },

  getOrgTree(keyword?: string): Promise<ApiResponse<AdminOrgTreeNode[]>> {
    return request.get('/admin/org/tree', { params: keyword ? { keyword } : undefined })
  },

  getDepartmentDetail(id: string): Promise<ApiResponse<AdminDepartmentDetail>> {
    return request.get(`/admin/departments/${id}`)
  },

  getDepartmentMembers(id: string, params?: AdminDepartmentMemberQuery): Promise<ApiResponse<AdminDepartmentMemberPage>> {
    return request.get(`/admin/departments/${id}/members`, { params })
  },

  getDepartmentPositions(id: string): Promise<ApiResponse<AdminDepartmentPosition[]>> {
    return request.get(`/admin/departments/${id}/positions`)
  },

  getDepartmentStats(id: string): Promise<ApiResponse<AdminDepartmentStatsDetail>> {
    return request.get(`/admin/departments/${id}/stats`)
  },

  createDepartment(data: AdminDepartmentPayload): Promise<ApiResponse<{ id: string; name: string }>> {
    return request.post('/admin/departments', data)
  },

  updateDepartment(id: string, data: AdminDepartmentPayload): Promise<ApiResponse<{ success: boolean }>> {
    return request.put(`/admin/departments/${id}`, data)
  },

  deleteDepartment(id: string): Promise<ApiResponse<{ success: boolean }>> {
    return request.delete(`/admin/departments/${id}`)
  },

  createDepartmentPosition(departmentId: string, data: AdminDepartmentPositionPayload): Promise<ApiResponse<AdminDepartmentPosition>> {
    return request.post(`/admin/departments/${departmentId}/positions`, data)
  },

  updateDepartmentPosition(positionId: string, data: AdminDepartmentPositionPayload): Promise<ApiResponse<AdminDepartmentPosition>> {
    return request.put(`/admin/departments/positions/${positionId}`, data)
  },

  deleteDepartmentPosition(positionId: string): Promise<ApiResponse<{ success: boolean }>> {
    return request.delete(`/admin/departments/positions/${positionId}`)
  },

  removeDepartmentMember(departmentId: string, userId: string): Promise<ApiResponse<{ success: boolean }>> {
    return request.delete(`/admin/departments/${departmentId}/members/${userId}`)
  },

  updateDepartmentMember(
    departmentId: string,
    userId: string,
    data: AdminDepartmentMemberPayload,
  ): Promise<ApiResponse<AdminDepartmentMember>> {
    return request.put(`/admin/departments/${departmentId}/members/${userId}`, data)
  },
}

export interface AdminDashboardData {
  stats: {
    traineeCount: number
    completedTraining: number
    incompleteTraining: number
    completionRate: number
    examPassRate: number
    safetyAlerts: number
    traineeDelta?: number
    completedDelta?: number
    incompleteDelta?: number
    passRateDelta?: number
    alertsDelta?: number
  }
  completionTrend: {
    labels: string[]
    completedUsers: number[]
    completionRates: number[]
  }
  categoryCompletion: Array<{
    code: string
    name: string
    rate: number
    color: string
  }>
  departments?: string[]
  categoryCompletionByDepartment?: Record<string, AdminDashboardData['categoryCompletion']>
  departmentOverview?: Record<string, {
    traineeCount: number
    completedTraining: number
    completionRate: number
  }>
  trainingPlans: Array<{
    id: string
    name: string
    department: string
    learned: number
    target: number
    rate: number
    status: 'completed' | 'in_progress' | 'not_started'
  }>
  alerts: Array<{
    id?: string
    type?: string
    title: string
    description: string
    level: 'danger' | 'warning' | 'info'
    time: string
    actionPath?: string
  }>
  announcements: Array<{
    id?: string
    title: string
    date: string
    pinned: boolean
    type?: string
    actionPath?: string
  }>
  calendarEvents: Array<{
    date: string
    type: 'planned' | 'in_progress' | 'completed'
  }>
  generatedAt: string
}

export interface AdminAnalyticsChartItem {
  name: string
  value: number
  color?: string
  learners?: number
  completed?: number
  fullName?: string
  total?: number
  samples?: number
}

export interface AdminAnalyticsData {
  summary?: {
    traineeCount: number
    completionRate: number
    examPassRate: number
    incompleteTraining: number
  }
  filters?: {
    department: string
    from: string
    to: string
  }
  departments?: string[]
  exportRows?: Array<{
    section: string
    name: string
    value: string | number
    learners?: number
    completed?: number
    samples?: number
  }>
  generatedAt?: string
  learningEffect: {
    courseCompletionRank: AdminAnalyticsChartItem[]
    quizPassRateTrend: { months: string[]; rates: number[] }
    masteryDistribution: AdminAnalyticsChartItem[]
  }
  simulationEffect: {
    scoreDistribution: AdminAnalyticsChartItem[]
    scenarioSuccessRate: AdminAnalyticsChartItem[]
    decisionRadar: { indicators: Array<{ name: string; max: number }>; values: number[] }
  }
  userActivity: {
    weeklyActiveTrend: { weeks: string[]; counts: number[] }
    studyDurationDistribution: AdminAnalyticsChartItem[]
    newUserTrend: { months: string[]; counts: number[] }
  }
  departmentCompare: {
    deptScoreRank: AdminAnalyticsChartItem[]
    deptActivityHeatmap: { departments: string[]; weeks: string[]; data: Array<[number, number, number]> }
  }
  aiUsage: {
    qaTrend: { months: string[]; counts: number[] }
    qaCategoryDistribution: AdminAnalyticsChartItem[]
  }
  certificates: {
    issueTrend: { months: string[]; counts: number[] }
    typeDistribution: AdminAnalyticsChartItem[]
  }
}

export interface AdminAnalyticsQuery {
  department?: string
  from?: string
  to?: string
}

export interface AdminLearningReportQuery {
  keyword?: string
  department?: string
  category?: string
  learningStatus?: string
  from?: string
  to?: string
  trendDays?: number
  page?: number
  pageSize?: number
}

export interface AdminLearningReportData {
  summary: {
    traineeCount: number
    traineeDelta: number
    completionRate: number
    completedCount: number
    avgStudyHours: number
    examPassRate: number
    passedCount: number
  }
  charts: {
    learningTrend: { labels: string[]; learners: number[]; completedCourses: number[] }
    courseCompletion: { items: AdminAnalyticsChartItem[]; overallRate: number }
    departmentCompletion: Array<{ name: string; rate: number }>
    examScoreDistribution: AdminAnalyticsChartItem[]
  }
  followUp: {
    items: AdminLearningReportFollowUpItem[]
    total: number
    page: number
    pageSize: number
    totalPages: number
  }
  departments: string[]
  categories: Array<{ code: string; name: string }>
  generatedAt: string
}

export interface AdminLearningReportFollowUpItem {
  userId: string
  username: string
  department: string
  courseId: string
  courseTitle: string
  progress: number
  studyHours: number
  studyDuration: string
  examScore?: number | null
  examLabel: string
  learningStatus: string
  warningStatus: string
}

export interface AdminLearningReminderPayload {
  userId: string
  courseId: string
  warningStatus?: string
  progress?: number
}

export interface AdminLearningReminderResult {
  id: string
  type: string
  title: string
  content: string
  courseId?: string
  actionPath?: string
  time?: string
  read: boolean
  duplicate?: boolean
  message?: string
}

export interface AdminRoleSummary {
  id: string
  code: string
  name: string
  roleType: 'system' | 'custom'
  status: 'draft' | 'published'
}

export interface AdminRoleModule {
  code: string
  name: string
  supportsApprove: boolean
  approveOnly?: boolean
}

export interface AdminRoleModuleMeta {
  modules: AdminRoleModule[]
  actions: Array<{ code: string; label: string }>
  dataScopes: Array<{ code: string; label: string }>
}

export interface AdminRoleDetail extends AdminRoleSummary {
  description?: string
  dataScope: string
  customDeptIds: string[]
  permissions: Record<string, Record<string, boolean>>
  createdAt?: string
  updatedAt?: string
  roleTypeLabel?: string
  statusLabel?: string
}

export interface AdminRolePayload {
  code?: string
  name: string
  description?: string
  dataScope?: string
  customDeptIds?: string[]
  permissions?: Record<string, Record<string, boolean>>
}

export interface AdminAlertStats {
  total: number
  totalDelta: number
  examFailCount: number
  examFailRate: number
  progressLagCount: number
  progressLagRate: number
  pendingCount: number
  pendingRate: number
  dangerCount: number
  warningCount: number
  infoCount: number
  generatedAt?: string
}

export interface AdminTrainingAlertItem {
  id: string
  alertNo?: string
  type: string
  level: 'danger' | 'warning' | 'info'
  title: string
  description: string
  department?: string
  responsiblePerson?: string
  traineeName?: string
  courseName?: string
  time?: string
  status: 'pending' | 'processing' | 'closed'
  actionPath?: string
  manual?: boolean
  remark?: string
}

export interface AdminAlertSearchParams {
  keyword?: string
  level?: string
  type?: string
  department?: string
  status?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  pageSize?: number
}

export interface AdminAlertCenterPage {
  items: AdminTrainingAlertItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  stats?: AdminAlertStats
  departments?: string[]
  generatedAt?: string
}

export interface AdminAlertCenter {
  items: AdminDashboardData['alerts']
  total: number
  dangerCount: number
  warningCount: number
  infoCount: number
  generatedAt?: string
}

export interface AdminInboxSummary<T> {
  items: T[]
  total: number
  unreadCount?: number
  generatedAt?: string
}

export interface AdminInboxPage<T> extends AdminInboxSummary<T> {
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminNotificationItem {
  id: string
  type: string
  title: string
  description: string
  level: 'danger' | 'warning' | 'info'
  time?: string
  actionPath?: string
  read?: boolean
  persisted?: boolean
}

export interface AdminMessageItem {
  id: string
  title: string
  date: string
  pinned: boolean
  type?: string
  body?: string
  actionPath?: string
  read?: boolean
  persisted?: boolean
}

export interface AdminMessagePayload {
  title: string
  body?: string
  type?: string
  pinned?: boolean
  actionPath?: string
}

export interface SystemConfigItem {
  id: string
  configKey: string
  configValue: string
  valueType: 'BOOLEAN' | 'INT' | 'STRING' | 'TEXT' | 'JSON_LIST' | 'JSON'
  category: string
  label: string
  description: string
  isPublic: boolean
  isSensitive: boolean
  editable: boolean
  sortOrder: number
}

export interface AdminCourseQuery {
  category?: string
  status?: string
  keyword?: string
  department?: string
  createdFrom?: string
  createdTo?: string
  page?: number
  pageSize?: number
}

export interface AdminCoursePage {
  items: AdminCourseListItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminCourseStats {
  learnerCount: number
  completedCount: number
  completionRate: number
  avgScore: number
  positiveRate: number
}

export interface AdminCourseListItem {
  id: string
  title: string
  description?: string
  subtitle?: string
  coverImage?: string
  category: string
  categoryName?: string
  totalDuration?: number
  status?: string
  instructor?: string
  targetAudience?: string
  chapterCount?: number
  learnerCount?: number
  completedCount?: number
  completionRate?: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminCourseDetail extends AdminCourseListItem {
  instructorTitle?: string
  objectives?: string[]
  tags?: string[]
  knowledgePoints?: string[]
  targetDepartments?: string[]
  targetRoles?: string[]
  chapters?: AdminChapterDetail[]
  stats?: AdminCourseStats
}

export interface AdminChapterDetail extends Chapter {
  summary?: string
  contentType?: string
  required?: boolean
  allowDownload?: boolean
  orderNum?: number
}

export interface AdminCoursePayload {
  title: string
  description?: string
  subtitle?: string
  coverImage?: string
  category: string
  status?: string
  totalDuration?: number
  instructor?: string
  instructorTitle?: string
  targetAudience?: string
  objectives?: string[]
  tags?: string[]
  knowledgePoints?: string[]
  targetDepartments?: string[]
  targetRoles?: string[]
}

export interface AdminChapterPayload {
  title: string
  content?: string
  videoUrl?: string
  summary?: string
  contentType?: string
  required?: boolean
  allowDownload?: boolean
  duration?: number
  orderNum?: number
  order?: number
  difficultyLevel?: number
  prerequisiteIds?: string[]
  scenarioId?: string
}

export interface AdminCourseMonitoringStats {
  expectedCount: number
  completedCount: number
  completionRate: number
  avgScore: number
  incompleteCount: number
  warningCount: number
  chapterCount: number
}

export interface AdminCourseMonitoringSummary {
  course: AdminCourseDetail & { publishedAt?: string }
  stats: AdminCourseMonitoringStats
  departments: string[]
}

export interface AdminCourseLearnerQuery {
  keyword?: string
  department?: string
  learningStatus?: 'all' | 'not_started' | 'in_progress' | 'completed'
  warningStatus?: 'all' | 'none' | 'not_started' | 'low_progress'
  page?: number
  pageSize?: number
}

export interface AdminCourseLearnerItem {
  userId: string
  username: string
  email: string
  department: string
  employeeNo: string
  avatarUrl?: string
  progress: number
  completedChapters: number
  totalChapters: number
  learningStatus: 'not_started' | 'in_progress' | 'completed'
  studySeconds: number
  studyDuration: string
  examScore: number
  certificateStatus: 'not_started' | 'not_obtained' | 'obtained'
  warningStatus: 'none' | 'not_started' | 'low_progress'
  lastAccessAt?: string
}

export interface AdminCourseLearnerPage {
  items: AdminCourseLearnerItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminCourseLearnerDetail {
  userId: string
  username: string
  email: string
  department?: string
  employeeNo: string
  courseTitle: string
  chapters: Array<{
    chapterId: string
    title: string
    order?: number
    duration?: number
    progress: number
    completed: boolean
    studySeconds?: number
    lastAccessAt?: string
    quizScore?: number
    quizPassed?: boolean
  }>
}

export interface AdminExamQuery {
  keyword?: string
  examType?: string
  department?: string
  status?: string
  createdFrom?: string
  createdTo?: string
  page?: number
  pageSize?: number
}

export interface AdminExamPage {
  items: AdminExamListItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminExamListItem {
  id: string
  sourceType: 'chapter' | 'comprehensive' | 'paper'
  title: string
  examType: 'formal' | 'mock'
  department: string
  questionCount: number
  timeLimit?: number | null
  totalScore: number
  passScore: number
  status: 'published' | 'pending' | 'draft' | 'ended'
  createdAt?: string
  courseId?: string
  chapterId?: string
  courseTitle?: string
  chapterTitle?: string
  attemptCount?: number
}

export interface AdminExamStats {
  examId: string
  title: string
  attemptCount: number
  passedCount: number
  passRate: number
}

export interface AdminExamPayload {
  title?: string
  examType?: string
  status?: string
  passScore?: number
  timeLimit?: number
  totalScore?: number
  questions?: string
}

export interface AdminQuestionCategoryNode {
  id: string
  name: string
  parentId?: string
  questionCount: number
  children?: AdminQuestionCategoryNode[]
}

export interface AdminQuestionQuery {
  categoryId?: string
  type?: string
  difficulty?: string
  status?: string
  tags?: string
  keyword?: string
  page?: number
  pageSize?: number
}

export interface AdminQuestionPage {
  items: AdminQuestionListItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminQuestionListItem {
  id: string
  content: string
  type: string
  difficulty: string
  status: string
  tags: string[]
  usageCount: number
  categoryId: string
  categoryName?: string
  updatedAt?: string
}

export interface AdminQuestionDetail extends AdminQuestionListItem {
  options?: unknown
  explanation?: string
  attachments?: AdminQuestionAttachments
  settings?: AdminQuestionSettings
  categoryPath?: string
  createdAt?: string
}

export interface AdminQuestionAttachmentFile {
  name: string
  size?: number
  url?: string
  type?: string
}

export interface AdminQuestionAttachments {
  files: AdminQuestionAttachmentFile[]
  images: AdminQuestionAttachmentFile[]
  norms: AdminQuestionAttachmentFile[]
}

export interface AdminQuestionSettings {
  allowComments: boolean
  allowReport: boolean
  showAnalysisInExam: boolean
}

export interface AdminQuestionPayload {
  categoryId?: string
  type?: string
  difficulty?: string
  status?: string
  content?: string
  options?: unknown
  explanation?: string
  tags?: string[]
  attachments?: AdminQuestionAttachments
  settings?: AdminQuestionSettings
}

export interface AdminPaperTypeRule {
  type: string
  label?: string
  count: number
  scorePerQuestion: number
  difficultyRatio: { easy: number; medium: number; hard: number }
}

export interface AdminPaperPreview {
  totalQuestions: number
  totalScore: number
  distribution: Array<{
    type: string
    typeLabel: string
    count: number
    scorePerQuestion: number
    subtotal: number
    percent: number
  }>
}

export interface AdminPaperGenerateResult extends AdminPaperPreview {
  questionIds: string[]
  questions: Array<{ id: string; type: string; question: string; score: number }>
}

export interface AdminPaperDetail {
  id: string
  title: string
  mode: string
  examType: string
  status: string
  timeLimit: number
  totalScore: number
  passScore: number
  department?: string
  config?: Record<string, unknown>
  questionIds?: string[]
  questions?: Array<{ id: string; type: string; question: string; score: number }>
  preview?: AdminPaperPreview
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AdminPaperPayload {
  id?: string
  title?: string
  mode?: string
  examType?: string
  status?: string
  timeLimit?: number
  totalScore?: number
  passScore?: number
  department?: string
  config?: Record<string, unknown>
  questionIds?: string[]
  questionsSnapshot?: unknown[]
}

export interface AdminUserQuery {
  keyword?: string
  department?: string
  role?: string
  status?: string
  certStatus?: string
  progressMin?: number
  progressMax?: number
  page?: number
  pageSize?: number
}

export interface AdminUserPage {
  items: AdminUserListItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminUserListItem {
  id: string
  username: string
  email?: string
  role: string
  permissionRoleId?: string | null
  company?: string
  department?: string
  position?: string
  phone?: string
  phoneMasked?: string
  avatarUrl?: string
  employeeNo?: string
  enabled: boolean
  accountStatus: 'active' | 'disabled'
  trainingCompletionRate: number
  certStatus: 'certified' | 'expired' | 'none'
  lastLoginAt?: string
  createdAt?: string
}

export interface AdminUserFilterOptions {
  departments: string[]
}

export interface AdminUserPayload {
  username?: string
  email?: string
  password?: string
  role?: string
  permissionRoleId?: string | null
  company?: string
  department?: string
  position?: string
  employeeNo?: string
  phone?: string
  avatarUrl?: string
  enabled?: boolean
}

export interface AdminUserDetail {
  profile: AdminUserDetailProfile
  stats: AdminUserDetailStats
  trainingArchive: AdminUserTrainingArchive
  examSummary: AdminUserExamSummary
  certificateReminders: AdminUserCertReminder[]
  recentCourses: AdminUserRecentCourse[]
  warnings: AdminUserWarning[]
}

export interface AdminUserDetailProfile {
  id: string
  username: string
  role: string
  enabled: boolean
  employeeNo?: string
  department?: string
  position?: string
  phone?: string
  phoneMasked?: string
  email?: string
  avatarUrl?: string
  entryDate?: string
  accountSource?: string
  tags: string[]
}

export interface AdminUserDetailStats {
  completedCourses: number
  avgScore: number
  certificateCount: number
  warningCount: number
}

export interface AdminUserTrainingArchive {
  progressRate: number
  completed: number
  inProgress: number
  notStarted: number
  expired: number
}

export interface AdminUserExamSummary {
  avgScore: number
  attemptCount: number
  passRate: number
  latestExam?: {
    title: string
    score: number
    date?: string
  }
}

export interface AdminUserCertReminder {
  id: string
  title: string
  expiresAt: string
  daysLeft: number
  urgency: 'danger' | 'warning' | 'normal'
}

export interface AdminUserRecentCourse {
  id: string
  title: string
  coverImage?: string
  progress: number
  completedAt?: string
}

export interface AdminUserWarning {
  type: string
  content: string
  time?: string
  status: 'pending' | 'processed'
}

export interface AdminOrgTreeNode {
  id: string
  name: string
  parentId?: string
  memberCount: number
  children?: AdminOrgTreeNode[]
}

export interface AdminDepartmentDetail {
  id: string
  name: string
  parentId?: string
  leaderName?: string
  leaderTitle?: string
  memberCount: number
  trainingCompletionRate: number
  highRiskPositionCount: number
  certExpiringCount: number
  newMembersThisMonth: number
  stats: AdminDepartmentStatsSummary
}

export interface AdminDepartmentStatsSummary {
  totalMembers: number
  trainingCompletionRate: number
  highRiskPositionCount: number
  certExpiringCount: number
  newMembersThisMonth: number
}

export interface AdminDepartmentMemberQuery {
  page?: number
  pageSize?: number
  keyword?: string
}

export interface AdminDepartmentMemberPage {
  items: AdminDepartmentMember[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AdminDepartmentMember {
  id: string
  username: string
  employeeNo?: string
  position?: string
  role: string
  phone?: string
  enabled?: boolean
  department?: string
  avatarUrl?: string
  trainingCompletionRate: number
  certStatus: string
  certStatusLabel: string
}

export interface AdminDepartmentMemberPayload {
  employeeNo?: string
  position?: string
  role?: string
  phone?: string
  enabled?: boolean
}

export interface AdminDepartmentPosition {
  id: string
  departmentId: string
  name: string
  highRisk: boolean
  sortOrder?: number
}

export interface AdminDepartmentPayload {
  name?: string
  parentId?: string
  leaderName?: string
  leaderTitle?: string
  sortOrder?: number
}

export interface AdminDepartmentPositionPayload {
  name?: string
  highRisk?: boolean
  sortOrder?: number
}

export interface AdminDepartmentStatsDetail extends AdminDepartmentStatsSummary {
  trainingBreakdown: {
    completed: number
    inProgress: number
    notStarted: number
  }
  roleDistribution: Record<string, number>
  certStatusDistribution: Record<string, number>
}
