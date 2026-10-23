import request from './request'
import type { QARecord, ApiResponse, PaginatedData } from '@/types'

export const aiApi = {
  ask(question: string): Promise<ApiResponse<{ answer: string; sources: { id: string; title: string; relevance: number }[]; relatedQuestions: string[] }>> {
    return request.post('/ai/ask', { question })
  },

  getHistory(params?: { page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<QARecord>>> {
    return request.get('/ai/history', { params })
  },

  getDraft(): Promise<ApiResponse<{ messages: string; updatedAt?: string }>> {
    return request.get('/ai/draft')
  },

  saveDraft(messages: string): Promise<ApiResponse<{ success: boolean; updatedAt?: string }>> {
    return request.put('/ai/draft', { messages })
  },

  submitFeedback(recordId: string, rating: number): Promise<ApiResponse<void>> {
    return request.post('/ai/feedback', { recordId, rating })
  },
}
