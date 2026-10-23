import request from './request'
import type { ApiResponse } from '@/types'

export interface SimulationRecordItem {
  sessionId: string
  type: 'classic' | 'timeline' | 'test'
  scenarioName?: string
  scenarioId?: string
  scenarioCode?: string
  outcome?: string
  branch?: string
  totalScore?: number
  rating?: string
  instructorComment?: string
  dimensions?: unknown
  strengths?: string[]
  weaknesses?: string[]
  fiveWhy?: string[]
  recommendations?: unknown
  status?: string
  startedAt?: string
  finishedAt?: string
}

export const simulationApi = {
  getRecords(): Promise<ApiResponse<SimulationRecordItem[]>> {
    return request.get('/simulation/records')
  },
}
