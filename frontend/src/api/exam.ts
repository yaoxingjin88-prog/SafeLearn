import request from './request'
import type { ApiResponse, PaginatedData } from '@/types'

export interface UserExamItem {
  id: string
  sourceType: 'paper' | 'chapter' | 'comprehensive'
  title: string
  description?: string
  examType?: string
  courseId?: string
  courseTitle?: string
  chapterId?: string
  chapterTitle?: string
  timeLimit?: number
  passScore?: number
  totalScore?: number
  questionCount?: number
  status?: string
  attemptCount?: number
  usedAttempts?: number
  maxAttempts?: number
  examPassed?: boolean
  examStatus?: 'available' | 'in_progress' | 'passed' | 'exhausted'
  validityText?: string
  bestScore?: number
  link?: string
  createdAt?: string
}

export interface ExamCenterStats {
  pendingExams: number
  inProgressExams: number
  completedExams: number
  averageScore: number
  percentileRank: number
}

export interface ExamListResponse extends PaginatedData<UserExamItem> {
  stats?: ExamCenterStats
}

export const examApi = {
  getList(params?: { page?: number; pageSize?: number; tab?: string }): Promise<ApiResponse<ExamListResponse>> {
    return request.get('/exams', { params })
  },

  getPaperStatus(paperId: string): Promise<ApiResponse<{
    id: string
    title: string
    examType?: string
    timeLimit?: number
    passScore?: number
    totalScore?: number
    questionCount?: number
    examPassed?: boolean
    attemptCount?: number
    bestScore?: number
  }>> {
    return request.get(`/exams/papers/${paperId}/status`)
  },

  startPaperExam(paperId: string): Promise<ApiResponse<{
    attemptId: string
    quiz: {
      id: string
      title: string
      passScore?: number
      timeLimit?: number
      questions: Array<{
        id: string
        type: string
        question: string
        options?: Array<{ id: string; text: string }>
      }>
    }
  }>> {
    return request.post(`/exams/papers/${paperId}/start`)
  },

  submitPaperExam(attemptId: string, answers: Record<string, string>): Promise<ApiResponse<Record<string, unknown>>> {
    return request.post(`/exams/papers/attempts/${attemptId}/submit`, { answers })
  },
}
