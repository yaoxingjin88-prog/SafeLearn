import request from './request'
import type { ApiResponse, Course, Chapter } from '@/types'

export const adminApi = {
  getCourses(): Promise<ApiResponse<Course[]>> {
    return request.get('/admin/courses')
  },

  getCourseById(id: string): Promise<ApiResponse<Course & { chapters: Chapter[] }>> {
    return request.get(`/admin/courses/${id}`)
  },

  createCourse(data: Partial<Course>): Promise<ApiResponse<Course>> {
    return request.post('/admin/courses', data)
  },

  updateCourse(id: string, data: Partial<Course>): Promise<ApiResponse<Course>> {
    return request.put(`/admin/courses/${id}`, data)
  },

  deleteCourse(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/courses/${id}`)
  },

  getChapters(courseId: string): Promise<ApiResponse<Chapter[]>> {
    return request.get(`/admin/courses/${courseId}/chapters`)
  },

  createChapter(courseId: string, data: Partial<Chapter>): Promise<ApiResponse<Chapter>> {
    return request.post(`/admin/courses/${courseId}/chapters`, data)
  },

  updateChapter(courseId: string, chapterId: string, data: Partial<Chapter>): Promise<ApiResponse<Chapter>> {
    return request.put(`/admin/courses/${courseId}/chapters/${chapterId}`, data)
  },

  deleteChapter(courseId: string, chapterId: string): Promise<ApiResponse<void>> {
    return request.delete(`/admin/courses/${courseId}/chapters/${chapterId}`)
  },
}
