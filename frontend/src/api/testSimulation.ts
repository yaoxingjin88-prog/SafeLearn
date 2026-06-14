import request from '@/api/request'
import type { ApiResponse } from '@/types'

export interface TestSessionDto {
  sessionId: string
  scenarioId: string
  status: string
}

export function startSession(scenarioCode: string) {
  return request.post('/test-simulation/sessions', { scenarioCode }) as Promise<ApiResponse<TestSessionDto>>
}

export function recordDecision(sessionId: string, body: Record<string, unknown>) {
  return request.post(`/test-simulation/sessions/${sessionId}/decisions`, body)
}

export function abandonSession(sessionId: string) {
  return request.post(`/test-simulation/sessions/${sessionId}/abandon`)
}

export function finishSession(sessionId: string, report: Record<string, unknown>) {
  return request.post(`/test-simulation/sessions/${sessionId}/finish`, report)
}

export function listSessions() {
  return request.get('/test-simulation/sessions') as Promise<ApiResponse<Record<string, unknown>[]>>
}

export function getSessionScore(sessionId: string) {
  return request.get(`/test-simulation/sessions/${sessionId}/score`) as Promise<ApiResponse<Record<string, unknown>>>
}
