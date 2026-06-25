import request from './request'
import type { ApiResponse } from '@/types'

export interface UserNotificationItem {
  id: string
  type: string
  title: string
  content: string
  courseId?: string
  actionPath?: string
  time?: string
  read: boolean
}

export interface UserNotificationSummary {
  items: UserNotificationItem[]
  total: number
  unreadCount: number
  generatedAt: string
}

export const dashboardApi = {
  getNotificationSummary(): Promise<ApiResponse<UserNotificationSummary>> {
    return request.get('/dashboard/notifications/summary')
  },

  markNotificationRead(id: string): Promise<ApiResponse<UserNotificationItem>> {
    return request.put(`/dashboard/notifications/${encodeURIComponent(id)}/read`)
  },

  markAllNotificationsRead(): Promise<ApiResponse<{ success: boolean }>> {
    return request.put('/dashboard/notifications/read-all')
  },
}
