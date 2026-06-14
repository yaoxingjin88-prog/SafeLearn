import request from './request'
import type { ApiResponse } from '@/types'
import type { DeductionEventLogEntry, AiScoreReport } from '@/simulation/types/deduction.types'

export const deductionApi = {
  startSession(scenarioId: string) {
    return request.post<ApiResponse<{ sessionId: string; scenarioId: string; status: string }>>(
      '/deduction/sessions',
      { scenarioId },
    )
  },

  appendEvents(sessionId: string, events: DeductionEventLogEntry[]) {
    return request.post(`/deduction/sessions/${sessionId}/events`, { events })
  },

  finishSession(sessionId: string, summary?: Record<string, unknown>) {
    return request.post<ApiResponse<AiScoreReport>>(`/deduction/sessions/${sessionId}/finish`, summary ?? {})
  },

  abandonSession(sessionId: string) {
    return request.post(`/deduction/sessions/${sessionId}/abandon`)
  },

  getReplay(sessionId: string) {
    return request.get<ApiResponse<{ events: DeductionEventLogEntry[]; durationMs: number }>>(
      `/deduction/sessions/${sessionId}/replay`,
    )
  },

  getScore(sessionId: string) {
    return request.get<ApiResponse<AiScoreReport>>(`/deduction/sessions/${sessionId}/score`)
  },

  getMyAnalytics() {
    return request.get('/deduction/analytics/me')
  },

  getUserAnalytics(userId: string) {
    return request.get(`/deduction/analytics/user/${userId}`)
  },
}
