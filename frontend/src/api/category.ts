import request from './request'
import type { ApiResponse, CourseCategory } from '@/types'

export const categoryApi = {
  getList(): Promise<ApiResponse<CourseCategory[]>> {
    return request.get('/course-categories')
  },
}
