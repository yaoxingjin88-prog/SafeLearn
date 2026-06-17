import request from './request'
import type { LoginForm, UserInfo, ApiResponse } from '@/types'

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export interface UpdateProfilePayload {
  email?: string
  phone?: string
  avatarUrl?: string
}

export const authApi = {
  login(data: LoginForm): Promise<ApiResponse<{ token: string; user: UserInfo }>> {
    return request.post('/auth/login', data)
  },

  register(data: { username: string; password: string; email: string; company?: string; department?: string }): Promise<ApiResponse<void>> {
    return request.post('/auth/register', data)
  },

  getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return request.get('/auth/user-info')
  },

  updateProfile(data: UpdateProfilePayload): Promise<ApiResponse<UserInfo>> {
    return request.put('/auth/profile', data)
  },

  changePassword(data: ChangePasswordPayload): Promise<ApiResponse<{ message: string }>> {
    return request.post('/auth/change-password', data)
  },

  forgotPassword(email: string): Promise<ApiResponse<{ message: string; devResetUrl?: string }>> {
    return request.post('/auth/forgot-password', { email })
  },

  resetPassword(token: string, newPassword: string): Promise<ApiResponse<{ message: string }>> {
    return request.post('/auth/reset-password', { token, newPassword })
  },

  refreshToken(): Promise<ApiResponse<{ token: string }>> {
    return request.post('/auth/refresh')
  },
}
