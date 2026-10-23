import request from './request'
import type { Course, Chapter, UserProgress, ApiResponse, PaginatedData, CourseReview, CourseReviewSummary } from '@/types'

export const courseApi = {
  getList(params?: { category?: string; keyword?: string; page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<Course>>> {
    return request.get('/courses', { params })
  },

  getRelated(id: string): Promise<ApiResponse<Course[]>> {
    return request.get(`/courses/${id}/related`)
  },

  getMyLearning(): Promise<ApiResponse<Array<{
    courseId: string
    courseTitle: string
    chapterId: string
    chapterTitle: string
    progress: number
    completed: boolean
    masteryLevel?: number
    lastAccessAt?: string
  }>>> {
    return request.get('/progress/my-learning')
  },

  getById(id: string): Promise<ApiResponse<Course>> {
    return request.get(`/courses/${id}`)
  },

  getChapter(courseId: string, chapterId: string): Promise<ApiResponse<Chapter>> {
    return request.get(`/courses/${courseId}/chapters/${chapterId}`)
  },

  updateProgress(data: {
    courseId: string
    chapterId: string
    progress: number
    completed?: boolean
    masteryLevel?: number
  }): Promise<ApiResponse<{ success: boolean; alreadyCompleted?: boolean; newCertificate?: Record<string, unknown> }>> {
    return request.post('/progress', data)
  },

  getProgress(courseId: string): Promise<ApiResponse<UserProgress[]>> {
    return request.get(`/progress/${courseId}`)
  },

  sendHeartbeat(data: {
    courseId: string
    chapterId: string
    elapsedSeconds: number
    progress?: number
  }): Promise<ApiResponse<{ success: boolean; studySeconds?: number; progress?: number }>> {
    return request.post('/progress/heartbeat', data)
  },

  getCourseProgress(courseId: string): Promise<ApiResponse<UserProgress[]>> {
    return request.get(`/progress/${courseId}`)
  },

  getReviews(courseId: string): Promise<ApiResponse<CourseReviewSummary>> {
    return request.get(`/courses/${courseId}/reviews`)
  },

  submitReview(courseId: string, data: { rating: number; content: string }): Promise<ApiResponse<CourseReview>> {
    return request.post(`/courses/${courseId}/reviews`, data)
  },
}
