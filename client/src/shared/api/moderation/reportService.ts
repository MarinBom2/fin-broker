import { apiRequest } from '../../core/apiClient'
import type { CodeLabel, PageResponse } from '../../types/common'
import type { RoleCode } from '../member/memberService'

/** 서버 ReportTarget */
export type ReportTargetCode = 'POST' | 'ANSWER'
/** 서버 ReportReason */
export type ReportReasonCode = 'PROMOTION' | 'SCAM' | 'PRIVACY' | 'ABUSE' | 'RIGHTS' | 'OTHER'
/** 서버 ReportStatus */
export type ReportStatusCode = 'OPEN' | 'ACCEPTED' | 'DISMISSED'

/** 신고 시트 선택지. 문구는 서버 description 과 같다 */
export const REPORT_REASONS: { code: ReportReasonCode; label: string; hint: string }[] = [
  { code: 'PROMOTION', label: '홍보·광고', hint: '업체 홍보, 상품 광고, 연락 유도' },
  { code: 'SCAM', label: '사기 의심', hint: '선입금·통장 요구, 불법 금리 제안' },
  { code: 'PRIVACY', label: '개인정보 노출', hint: '연락처·주민번호·계좌번호 등' },
  { code: 'ABUSE', label: '욕설·비방', hint: '모욕, 혐오, 특정인 비난' },
  { code: 'RIGHTS', label: '권리 침해', hint: '명예훼손, 저작권 등' },
  { code: 'OTHER', label: '기타', hint: '그 밖의 문제' },
]

export const REPORT_DETAIL_MAX = 300

export interface ReportRequest {
  targetType: ReportTargetCode
  targetId: number
  reason: ReportReasonCode
  detail?: string
}

/** 서버 ReportResponse */
export interface ReportResult {
  reportId: number
  status: CodeLabel<ReportStatusCode>
  targetHidden: boolean
}

/** 서버 OpsReportResponse (운영자). 대상 하나에 대한 신고 묶음 */
export interface OpsReport {
  /** 처리에 쓰는 대표 신고 ID */
  reportId: number
  targetType: CodeLabel<ReportTargetCode>
  targetId: number
  postId: number
  /** 많이 고른 순 */
  reasons: CodeLabel<ReportReasonCode>[]
  details: string[]
  reportCount: number
  status: CodeLabel<ReportStatusCode>
  lastReportedAt: string
  decidedAt: string | null
  postTitle: string | null
  excerpt: string
  authorRole: CodeLabel<RoleCode> | null
  targetStatus: CodeLabel<'PUBLISHED' | 'HIDDEN' | 'DELETED'> | null
}

export const reportApi = {
  /** 내 글이면 SELF_REPORT, 두 번째면 ALREADY_REPORTED (FR-MD-001) */
  file: (request: ReportRequest) =>
    apiRequest<ReportResult>('/api/v1/reports', { method: 'POST', body: JSON.stringify(request) }),
  /** 운영자 목록 (FR-MD-002) */
  opsList: (params: { status: ReportStatusCode; page?: number; size?: number }) =>
    apiRequest<PageResponse<OpsReport>>('/api/v1/ops/reports', { query: params }),
  /** 같은 대상의 접수 신고 전부에 적용된다 */
  decide: (reportId: number, decision: 'ACCEPTED' | 'DISMISSED') =>
    apiRequest<OpsReport>(`/api/v1/ops/reports/${reportId}`, { method: 'PATCH', body: JSON.stringify({ decision }) }),
}
