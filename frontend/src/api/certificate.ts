import request from './request'
import type { ApiResponse } from '@/types'

export interface UserCertificate {
  id: string
  courseId: string
  courseTitle: string
  title: string
  certificateNo: string
  certLevel: string
  issuedAt: string
  userName: string
  company: string
}

export const certificateApi = {
  mine() {
    return request.get<ApiResponse<UserCertificate[]>>('/certificates/mine')
  },
  detail(id: string) {
    return request.get<ApiResponse<UserCertificate>>(`/certificates/${id}`)
  },
}
