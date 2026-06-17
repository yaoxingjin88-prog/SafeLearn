import request from './request'
import type { ApiResponse } from '@/types'

export interface CertificateTemplate {
  code: string
  name: string
  badgeLabel: string
  titleSuffix: string
  validityMonths: number
  earlyRenewDays: number
  borderColor: string
  accentColor: string
  headerTitle: string
  bodyTemplate: string
  renewalRequirement: 'comprehensive_exam' | 'course_recomplete'
}

export interface CertificateRenewalStatus {
  certId: string
  status: string
  expiresAt?: string
  daysUntilExpiry?: number
  expiringSoon?: boolean
  eligible?: boolean
  requirementMet?: boolean
  reason?: string
  renewalRequirement?: string
  earlyRenewDays?: number
}

export interface UserCertificate {
  id: string
  courseId: string
  courseTitle: string
  title: string
  certificateNo: string
  certLevel: string
  templateCode: string
  template?: CertificateTemplate
  issuedAt: string
  expiresAt?: string
  status: 'active' | 'expired' | 'revoked'
  renewalCount: number
  lastRenewedAt?: string
  issueSource?: string
  userName: string
  company: string
  bodyText?: string
  daysUntilExpiry?: number
  renewal?: CertificateRenewalStatus
  renewed?: boolean
  message?: string
}

export const certificateApi = {
  templates() {
    return request.get<ApiResponse<CertificateTemplate[]>>('/certificates/templates')
  },
  mine() {
    return request.get<ApiResponse<UserCertificate[]>>('/certificates/mine')
  },
  detail(id: string) {
    return request.get<ApiResponse<UserCertificate>>(`/certificates/${id}`)
  },
  renewalStatus(id: string) {
    return request.get<ApiResponse<CertificateRenewalStatus>>(`/certificates/${id}/renewal`)
  },
  renew(id: string) {
    return request.post<ApiResponse<UserCertificate>>(`/certificates/${id}/renew`)
  },
}
