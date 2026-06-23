import request from './request'
import type { ApiResponse, Chapter, CourseCategory } from '@/types'

export const adminApi = {
  getDashboard(): Promise<ApiResponse<AdminDashboardData>> {
    return request.get('/admin/dashboard')
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
    title: string
    description: string
    level: 'danger' | 'warning' | 'info'
    time: string
  }>
  announcements: Array<{
    title: string
    date: string
    pinned: boolean
  }>
  calendarEvents: Array<{
    date: string
    type: 'planned' | 'in_progress' | 'completed'
  }>
  generatedAt: string
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
