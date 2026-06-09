import request from "./request";
import type { AccidentCase, ApiResponse, PaginatedData } from "@/types";

export const caseApi = {
  getList(params?: {
    type?: string;
    page?: number;
    pageSize?: number;
  }): Promise<ApiResponse<PaginatedData<AccidentCase>>> {
    return request.get("/cases", { params });
  },

  getById(id: string): Promise<ApiResponse<AccidentCase>> {
    return request.get(`/cases/${id}`);
  },

  getProgressSummary(): Promise<
    ApiResponse<
      {
        caseId: string;
        completed: boolean;
        currentStep: number;
        totalSteps: number;
      }[]
    >
  > {
    return request.get("/cases/progress/summary");
  },

  getProgress(caseId: string): Promise<
    ApiResponse<{
      caseId: string;
      completed: boolean;
      currentStep: number;
      totalSteps: number;
      reflections: Record<string, string>;
    }>
  > {
    return request.get(`/cases/${caseId}/progress`);
  },

  saveProgress(
    caseId: string,
    data: {
      currentStep: number;
      totalSteps: number;
      reflections: Record<string, string>;
    },
  ) {
    return request.put(`/cases/${caseId}/progress`, data);
  },

  complete(
    caseId: string,
    data?: { totalSteps: number; reflections: Record<string, string> },
  ) {
    return request.post(`/cases/${caseId}/complete`, data);
  },

  resetProgress(caseId: string) {
    return request.delete(`/cases/${caseId}/progress`);
  },
};
