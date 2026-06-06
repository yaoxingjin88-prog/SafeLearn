import request from './request'
import type { AccidentCase, ApiResponse, PaginatedData } from '@/types'

export const caseApi = {
  getList(params?: { type?: string; page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<AccidentCase>>> {
    return request.get('/cases', { params })
  },

  getById(id: string): Promise<ApiResponse<AccidentCase>> {
    return request.get(`/cases/${id}`)
  },
}
