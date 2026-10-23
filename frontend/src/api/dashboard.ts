import request from './request'
import type { ApiResponse } from '@/types'

export interface UserNotificationItem {
  id: string
  type: string
  title: string
  content: string
  courseId?: string
  actionPath?: string
  time?: string
  read: boolean
}

export interface UserNotificationSummary {
  items: UserNotificationItem[]
  total: number
  unreadCount: number
  generatedAt: string
}

export interface StudyTrend {
  days: number
  labels: string[]
  hours: number[]
  totalHours: number
}

export interface AbilityDimension {
  name: string
  value: number
}

export interface AbilityRadar {
  dimensions: AbilityDimension[]
  hasDeductionData: boolean
  overallScore: number
}

export interface DashboardTask {
  id: string
  title: string
  type: string
  deadline: string
  status: string
  actionText: string
  link: string
  assigned?: boolean
}

export interface LearningPathStep {
  key: string
  title: string
  description: string
  link: string
  status: 'done' | 'current' | 'locked'
}

export interface DashboardOverview {
  displayName: string
  joinDays: number
  streakDays: number
  studyHours: number
  completedCourses: number
  completedChapters?: number
  avgScore: number
  pendingMandatory: number
  deductionCount: number
  deductionSuccessRate: number
  deductionAvgScore: number
  continueLearning?: {
    courseId: string
    chapterId?: string
    courseTitle: string
    chapterTitle: string
    progress: number
    remainingMinutes: number
  } | null
  mandatoryTasks?: DashboardTask[]
  assignedTasks?: DashboardTask[]
  suggestedTasks?: DashboardTask[]
  learningPath?: LearningPathStep[]
  recommendedCourses?: Array<{
    id: string
    title: string
    description?: string
    coverImage?: string
    tag?: string
    meta?: string
  }>
  showRecommendedCourses?: boolean
  recentDeductions?: Array<{
    sessionId: string
    scenarioName: string
    totalScore: number
    outcome: string
  }>
  announcement?: string
}

export interface LearningCalendarDay {
  date: string
  minutes: number
  events?: Array<{ type: string; title: string; minutes?: number; link?: string }>
}

export interface LearningHistoryItem {
  date: string
  courseTitle: string
  chapterTitle?: string
  minutes: number
  type: string
}

export const dashboardApi = {
  getOverview(): Promise<ApiResponse<DashboardOverview>> {
    return request.get('/dashboard/overview')
  },

  getLearningCalendar(year: number, month: number): Promise<ApiResponse<{
    year: number
    month: number
    days: LearningCalendarDay[]
    totalMinutes: number
  }>> {
    return request.get('/dashboard/learning-calendar', { params: { year, month } })
  },

  getLearningHistory(): Promise<ApiResponse<LearningHistoryItem[]>> {
    return request.get('/dashboard/learning-history')
  },
  getStats(): Promise<ApiResponse<{
    courseCount: number
    completedCount: number
    simulationCount: number
    trainingCount: number
    avgScore: number
  }>> {
    return request.get('/dashboard/stats')
  },

  getRecentCourses(): Promise<ApiResponse<Array<{ id: string; title: string; category: string; progress: number }>>> {
    return request.get('/dashboard/recent-courses')
  },

  getTrainingAlerts(): Promise<ApiResponse<Array<{
    id: string
    alertNo: string
    type: string
    level: string
    title: string
    description?: string
    status: string
    actionPath?: string
    occurredAt?: string
  }>>> {
    return request.get('/dashboard/training-alerts')
  },

  getStudyTrend(days = 7): Promise<ApiResponse<StudyTrend>> {
    return request.get('/dashboard/study-trend', { params: { days } })
  },

  getAbilityRadar(): Promise<ApiResponse<AbilityRadar>> {
    return request.get('/dashboard/ability-radar')
  },

  getNotificationSummary(): Promise<ApiResponse<UserNotificationSummary>> {
    return request.get('/dashboard/notifications/summary')
  },

  markNotificationRead(id: string): Promise<ApiResponse<UserNotificationItem>> {
    return request.put(`/dashboard/notifications/${encodeURIComponent(id)}/read`)
  },

  markAllNotificationsRead(): Promise<ApiResponse<{ success: boolean }>> {
    return request.put('/dashboard/notifications/read-all')
  },
}
