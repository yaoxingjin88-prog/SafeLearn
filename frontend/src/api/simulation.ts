import request from './request'
import type { AccidentScenario, SimulationState, ApiResponse } from '@/types'

export const simulationApi = {
  getScenarios(): Promise<ApiResponse<AccidentScenario[]>> {
    return request.get('/simulation/scenarios')
  },

  getScenarioById(id: string): Promise<ApiResponse<AccidentScenario>> {
    return request.get(`/simulation/scenarios/${id}`)
  },

  startSimulation(scenarioId: string, customParams?: Record<string, any>): Promise<ApiResponse<{ simulationId: string }>> {
    return request.post('/simulation/start', { scenarioId, customParams })
  },

  pauseSimulation(id: string): Promise<ApiResponse<void>> {
    return request.post(`/simulation/${id}/pause`)
  },

  resumeSimulation(id: string): Promise<ApiResponse<void>> {
    return request.post(`/simulation/${id}/resume`)
  },

  resetSimulation(id: string): Promise<ApiResponse<void>> {
    return request.post(`/simulation/${id}/reset`)
  },

  getSimulationState(id: string): Promise<ApiResponse<SimulationState>> {
    return request.get(`/simulation/${id}/state`)
  },
}
