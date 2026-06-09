import request from '@/api/request'
import type { ApiResponse } from '@/types'

export interface TdSessionDto {
  sessionId: string
  scenarioId: string
  status: string
}

export function listScenarios() {
  return request.get('/timeline-deduction/scenarios') as Promise<ApiResponse<Record<string, unknown>[]>>
}

export function startSession(scenarioCode: string) {
  return request.post('/timeline-deduction/sessions', { scenarioCode }) as Promise<ApiResponse<TdSessionDto>>
}

export function recordDecision(sessionId: string, body: Record<string, unknown>) {
  return request.post(`/timeline-deduction/sessions/${sessionId}/decisions`, body)
}

export function finishSession(sessionId: string, report: Record<string, unknown>) {
  return request.post(`/timeline-deduction/sessions/${sessionId}/finish`, report)
}

export function listSessions() {
  return request.get('/timeline-deduction/sessions') as Promise<ApiResponse<Record<string, unknown>[]>>
}

export function getSessionScore(sessionId: string) {
  return request.get(`/timeline-deduction/sessions/${sessionId}/score`) as Promise<ApiResponse<Record<string, unknown>>>
}
