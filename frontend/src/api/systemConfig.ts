import request from './request'
import type { ApiResponse } from '@/types'

/** 用户端公开配置：{ "ai.relatedQuestions": [...], "learning.masteryThreshold": 60, ... } */
export const systemConfigApi = {
  getPublic(): Promise<ApiResponse<Record<string, unknown>>> {
    return request.get('/system-configs/public')
  },
}
