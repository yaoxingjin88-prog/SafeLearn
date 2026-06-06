import request from './request'
import type { Course, Chapter, UserProgress, ApiResponse, PaginatedData } from '@/types'

export const courseApi = {
  getList(params?: { category?: string; page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<Course>>> {
    return request.get('/courses', { params })
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
  }): Promise<ApiResponse<void>> {
    return request.post('/progress', data)
  },

  getProgress(courseId: string): Promise<ApiResponse<UserProgress[]>> {
    return request.get(`/progress/${courseId}`)
  },

  getCourseProgress(courseId: string): Promise<ApiResponse<UserProgress[]>> {
    return request.get(`/progress/${courseId}`)
  },
}
