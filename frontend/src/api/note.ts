import request from './request'
import type { ApiResponse } from '@/types'

export interface UserNote {
  id: string
  targetType: string
  targetId: string
  courseId?: string
  content: string
  createdAt?: string
  updatedAt?: string
}

export const noteApi = {
  list(targetType?: string, targetId?: string) {
    return request.get<ApiResponse<UserNote[]>>('/notes', { params: { targetType, targetId } })
  },
  save(data: { id?: string; targetType: string; targetId: string; courseId?: string; content: string }) {
    return request.post<ApiResponse<UserNote>>('/notes', data)
  },
  remove(id: string) {
    return request.delete<ApiResponse<void>>(`/notes/${id}`)
  },
}
