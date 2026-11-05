import { ApiError, apiRequest } from '../../core/apiClient'
import type { CodeLabel, PageResponse } from '../../types/common'
import type { RegistrationStatusCode } from '../company/companyService'
import type { RoleCode } from '../member/memberService'
import type { WarningItem, WarningTypeInfo } from '../warning/warningService'

/** 서버 PostType */
export type PostTypeCode = 'QUESTION' | 'REVIEW' | 'SCAM_CASE' | 'INFO'
/** 서버 LoanType */
export type LoanTypeCode = 'CREDIT' | 'SECURED' | 'POLICY' | 'BUSINESS' | 'OTHER'

/** 화면 선택지. 문구는 서버 description 과 같다 (목록·상세에서는 서버 값을 그대로 쓴다) */
export const POST_TYPES: { code: PostTypeCode; label: string }[] = [
  { code: 'QUESTION', label: '질문' },
  { code: 'REVIEW', label: '후기' },
  { code: 'SCAM_CASE', label: '피해 사례' },
  { code: 'INFO', label: '정보' },
]

export const LOAN_TYPES: { code: LoanTypeCode; label: string }[] = [
  { code: 'CREDIT', label: '신용대출' },
  { code: 'SECURED', label: '담보대출' },
  { code: 'POLICY', label: '정책서민금융' },
  { code: 'BUSINESS', label: '사업자대출' },
  { code: 'OTHER', label: '기타' },
]

/** 서버 CompanyRef */
export interface CompanyRef {
  companyId: number
  name: string
  registrationStatus: CodeLabel<RegistrationStatusCode>
}

/** 서버 PostSummaryResponse. 작성자는 역할로만 온다 (익명) */
export interface PostSummary {
  postId: number
  postType: CodeLabel<PostTypeCode>
  loanType: CodeLabel<LoanTypeCode>
  title: string
  excerpt: string
  authorRole: CodeLabel<RoleCode>
  company: CompanyRef | null
  warnings: WarningTypeInfo[]
  answerCount: number
  createdAt: string
}

/** 서버 AnswerResponse */
export interface Answer {
  answerId: number
  authorRole: CodeLabel<RoleCode>
  byPostAuthor: boolean
  mine: boolean
  body: string
  warnings: WarningTypeInfo[]
  createdAt: string
  modifiedAt: string
  /** 내가 신고한 답변인지 */
  reportedByMe: boolean
}

/** 서버 PostDetailResponse */
export interface PostDetail {
  postId: number
  postType: CodeLabel<PostTypeCode>
  loanType: CodeLabel<LoanTypeCode>
  title: string
  body: string
  authorRole: CodeLabel<RoleCode>
  mine: boolean
  company: CompanyRef | null
  warnings: WarningTypeInfo[]
  createdAt: string
  modifiedAt: string
  /** 내가 신고한 글인지 */
  reportedByMe: boolean
  answers: Answer[]
}

export interface PostWriteRequest {
  postType: PostTypeCode
  loanType: LoanTypeCode
  companyId: number | null
  title: string
  body: string
}

export const communityApi = {
  /** 최신순 (FR-CM-002) */
  list: (params: { postType?: PostTypeCode; loanType?: LoanTypeCode; page?: number; size?: number }) =>
    apiRequest<PageResponse<PostSummary>>('/api/v1/posts', { query: params }),
  /** 없거나 숨겨진 글은 ApiError.code === 'NOT_FOUND_POST' */
  get: (postId: number) => apiRequest<PostDetail>(`/api/v1/posts/${postId}`),
  /** 연락처 등이 있으면 ApiError.code === 'CONTENT_BLOCKED' (blockedWarnings 로 꺼낸다) */
  write: (request: PostWriteRequest) =>
    apiRequest<PostDetail>('/api/v1/posts', { method: 'POST', body: JSON.stringify(request) }),
  answer: (postId: number, body: string) =>
    apiRequest<Answer>(`/api/v1/posts/${postId}/answers`, { method: 'POST', body: JSON.stringify({ body }) }),
  /** 작성자만. 아니면 ApiError.code === 'FORBIDDEN' */
  edit: (postId: number, request: PostWriteRequest) =>
    apiRequest<PostDetail>(`/api/v1/posts/${postId}`, { method: 'PATCH', body: JSON.stringify(request) }),
  remove: (postId: number) => apiRequest<void>(`/api/v1/posts/${postId}`, { method: 'DELETE' }),
  editAnswer: (postId: number, answerId: number, body: string) =>
    apiRequest<Answer>(`/api/v1/posts/${postId}/answers/${answerId}`, { method: 'PATCH', body: JSON.stringify({ body }) }),
  removeAnswer: (postId: number, answerId: number) =>
    apiRequest<void>(`/api/v1/posts/${postId}/answers/${answerId}`, { method: 'DELETE' }),
}

/** 고친 적 있는지 (저장 시각과 수정 시각이 1초 넘게 다르면) */
export function isEdited(item: { createdAt: string; modifiedAt: string }) {
  return new Date(item.modifiedAt).getTime() - new Date(item.createdAt).getTime() > 1000
}

/** CONTENT_BLOCKED 응답에 실린 신호 목록. 다른 오류면 null */
export function blockedWarnings(error: unknown): WarningItem[] | null {
  if (error instanceof ApiError && error.code === 'CONTENT_BLOCKED' && Array.isArray(error.problem?.warnings)) {
    return error.problem.warnings as WarningItem[]
  }
  return null
}
