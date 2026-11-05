import type { MemberProfile } from '../api/member/memberService'

/** 운영자 권한인지 (서버 SecurityConfig 의 /api/v1/ops/** 와 같은 규칙: ADMIN, SYSTEM) */
export function isAdmin(member: MemberProfile | null | undefined) {
  const code = member?.roleType.code
  return code === 'ADMIN' || code === 'SYSTEM'
}
