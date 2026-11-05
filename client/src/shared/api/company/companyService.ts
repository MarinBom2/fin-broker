import { apiRequest } from '../../core/apiClient'
import type { CodeLabel, PageResponse } from '../../types/common'

/** 서버 CompanyType */
export type CompanyTypeCode = 'LENDER' | 'LOAN_BROKER' | 'LOAN_AGENT'

/** 서버 RegistrationStatus */
export type RegistrationStatusCode = 'UNVERIFIED' | 'VERIFIED' | 'NOT_FOUND' | 'EXPIRED'

/** 서버 CompanyResponse. 등록 정보에서 온 사실만 있다 (금리·한도·홍보 문구 없음) */
export interface Company {
  companyId: number
  name: string
  companyType: CodeLabel<CompanyTypeCode>
  registrationNumber: string
  registrationAuthority: string
  registrationStatus: CodeLabel<RegistrationStatusCode>
  /** UTC ISO 문자열. 대조 전이면 null */
  lastCheckedAt: string | null
}

export const companyApi = {
  /** 상호 일부 또는 등록번호로 검색. 상호 가나다순 (FR-CO-001) */
  search: (params: { keyword?: string; page?: number; size?: number }) =>
    apiRequest<PageResponse<Company>>('/api/v1/companies', { query: params }),
  /** 없으면 ApiError.code === 'NOT_FOUND_COMPANY' */
  get: (companyId: number) => apiRequest<Company>(`/api/v1/companies/${companyId}`),
}
