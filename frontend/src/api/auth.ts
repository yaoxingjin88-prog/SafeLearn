import request from './request'
import type { LoginForm, UserInfo, ApiResponse } from '@/types'

export const authApi = {
  login(data: LoginForm): Promise<ApiResponse<{ token: string }>> {
    return request.post('/auth/login', data)
  },

  register(data: { username: string; password: string; email: string; company?: string; department?: string }): Promise<ApiResponse<void>> {
    return request.post('/auth/register', data)
  },

  getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return request.get('/auth/user-info')
  },

  refreshToken(): Promise<ApiResponse<{ token: string }>> {
    return request.post('/auth/refresh')
  },
}
