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

/** 운영자 화면의 업체 (서버 OpsCompanyResponse, SCR-08) */
export interface OpsCompany extends Company {
  /** UTC ISO 문자열 */
  createdAt: string
  /** 지금 상태에서 남길 수 있는 대조 결과 (버튼) */
  allowedResults: CodeLabel<RegistrationStatusCode>[]
  /** 등록 확인한 지 재확인 기간이 지났는지 */
  recheckDue: boolean
}

/** 서버 OpsCompanySummaryResponse. statuses 는 enum 순서로 모든 상태가 온다 */
export interface OpsCompanySummary {
  total: number
  statuses: { status: CodeLabel<RegistrationStatusCode>; count: number }[]
  recheckDue: number
  recheckAfterDays: number
}

export interface CompanyRegisterRequest {
  name: string
  companyType: CompanyTypeCode
  registrationNumber: string
  registrationAuthority: string
}

/** 업체 유형 선택지. 문구는 서버 description 과 같다 */
export const COMPANY_TYPES: { value: CompanyTypeCode; label: string }[] = [
  { value: 'LENDER', label: '대부업자' },
  { value: 'LOAN_BROKER', label: '대부중개업자' },
  { value: 'LOAN_AGENT', label: '대출모집인' },
]

export const companyApi = {
  /** 상호 일부 또는 등록번호로 검색. 상호 가나다순 (FR-CO-001) */
  search: (params: { keyword?: string; page?: number; size?: number }) =>
    apiRequest<PageResponse<Company>>('/api/v1/companies', { query: params }),
  /** 없으면 ApiError.code === 'NOT_FOUND_COMPANY' */
  get: (companyId: number) => apiRequest<Company>(`/api/v1/companies/${companyId}`),
}

/** 운영자 업체 관리 (FR-CO-002). 권한은 ADMIN·SYSTEM */
export const opsCompanyApi = {
  /** 새로 등록한 순. recheckDue 면 status 를 무시하고 재확인 필요 업체만 */
  list: (params: { status?: RegistrationStatusCode; recheckDue?: boolean; keyword?: string; page?: number; size?: number }) =>
    apiRequest<PageResponse<OpsCompany>>('/api/v1/ops/companies', { query: params }),
  summary: () => apiRequest<OpsCompanySummary>('/api/v1/ops/companies/summary'),
  /** 같은 유형·등록번호가 있으면 ApiError.code === 'ALREADY_EXIST' */
  register: (request: CompanyRegisterRequest) =>
    apiRequest<OpsCompany>('/api/v1/ops/companies', { method: 'POST', body: JSON.stringify(request) }),
  /** 허용되지 않는 전이면 COMPANY_STATE_CONFLICT */
  recordCheck: (companyId: number, result: RegistrationStatusCode) =>
    apiRequest<OpsCompany>(`/api/v1/ops/companies/${companyId}/registration-status`, {
      method: 'PATCH',
      body: JSON.stringify({ result }),
    }),
}
