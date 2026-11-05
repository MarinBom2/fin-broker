import type { CodeLabel } from '../types/common'
import type { RoleCode } from '../api/member/memberService'

/** 작성자 표시. 익명이라 역할만 보여 준다. 등록 업체 담당자는 파란 배지 */
export function AuthorBadge({ role, isPostAuthor = false }: { role: CodeLabel<RoleCode>; isPostAuthor?: boolean }) {
  const company = role.code === 'COMPANY'
  return (
    <span className="author">
      <span className={company ? 'author__dot author__dot--company' : 'author__dot'} aria-hidden="true" />
      {company ? '등록 업체' : '익명'}
      {isPostAuthor ? <span className="pill pill--plain pill--blue">글쓴이</span> : null}
    </span>
  )
}
