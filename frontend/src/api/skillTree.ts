import request from './request'
import type { ApiResponse, SkillTreeData } from '@/types'

export const skillTreeApi = {
  getTree(): Promise<ApiResponse<SkillTreeData>> {
    return request.get('/skill-tree')
  },
}
