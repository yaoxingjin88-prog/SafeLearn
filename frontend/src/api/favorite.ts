import request from './request'
import type { ApiResponse } from '@/types'

export interface FavoriteItem {
  id: string
  targetType: string
  targetId: string
  title?: string
  category?: string
  coverImage?: string
  type?: string
  severity?: string
  createdAt?: string
}

export const favoriteApi = {
  list(targetType?: string) {
    return request.get<ApiResponse<FavoriteItem[]>>('/favorites', { params: { targetType } })
  },
  ids(targetType: string) {
    return request.get<ApiResponse<string[]>>('/favorites/ids', { params: { targetType } })
  },
  check(targetType: string, targetId: string) {
    return request.get<ApiResponse<{ favorited: boolean }>>('/favorites/check', { params: { targetType, targetId } })
  },
  toggle(targetType: string, targetId: string) {
    return request.post<ApiResponse<{ favorited: boolean }>>('/favorites/toggle', { targetType, targetId })
  },
}
