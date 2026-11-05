import { apiRequest } from '../../core/apiClient'
import type { CodeLabel } from '../../types/common'

/** 서버 WarningType. 등급과 안내 문구를 함께 담아 온다 */
export interface WarningTypeInfo extends CodeLabel<
  'CONTACT_EXPOSURE' | 'OVER_INTEREST_CAP' | 'UPFRONT_PAYMENT' | 'ACCOUNT_HANDOVER' | 'UNREGISTERED_COMPANY'
> {
  severity: CodeLabel<'BLOCK' | 'WARN'>
  guidance: string
}

export interface WarningItem {
  type: WarningTypeInfo
  /** 근거 조각. 개인정보는 서버가 가려서 보낸다 */
  evidence: string
}

/** 서버 WarningCheckResponse */
export interface WarningCheckResult {
  /** 글로 올릴 수 없는 내용(BLOCK)이 있는지 */
  blocked: boolean
  /** BLOCK 이 먼저 온다 */
  warnings: WarningItem[]
}

export interface WarningCheckRequest {
  title?: string
  body: string
  companyId?: number | null
}

export const warningApi = {
  /** 위험 신호 검사 (FR-WN-001). 저장하지 않는다 */
  check: (request: WarningCheckRequest) =>
    apiRequest<WarningCheckResult>('/api/v1/warnings/check', { method: 'POST', body: JSON.stringify(request) }),
}
