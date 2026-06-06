import request from './request'
import type { TrainingScenario, DecisionPoint, TrainingRecord, ApiResponse, PaginatedData } from '@/types'

export const trainingApi = {
  getScenarios(params?: { difficulty?: string; page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<TrainingScenario>>> {
    return request.get('/training/scenarios', { params })
  },

  getScenarioById(id: string): Promise<ApiResponse<TrainingScenario>> {
    return request.get(`/training/scenarios/${id}`)
  },

  startTraining(scenarioId: string): Promise<ApiResponse<{ recordId: string; firstDecisionPoint: DecisionPoint }>> {
    return request.post('/training/start', { scenarioId })
  },

  submitDecision(data: { recordId: string; decisionPointId: string; optionId: string; responseTime: number }): Promise<ApiResponse<{ score: number; consequence: string; nextDecisionPoint?: DecisionPoint }>> {
    return request.post('/training/decision', data)
  },

  getReport(recordId: string): Promise<ApiResponse<TrainingRecord>> {
    return request.get(`/training/records/${recordId}/report`)
  },

  getRecords(params?: { page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<TrainingRecord>>> {
    return request.get('/training/records', { params })
  },
}
