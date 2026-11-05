import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { pageParam, postPath, ROUTES } from '../../routes'
import { communityApi, LOAN_TYPES, POST_TYPES } from '../../shared/api/community/communityService'
import type { LoanTypeCode, PostSummary, PostTypeCode } from '../../shared/api/community/communityService'
import { errorMessage } from '../../shared/core/apiClient'
import type { PageResponse } from '../../shared/types/common'
import { AuthorBadge } from '../../shared/ui/AuthorBadge'
import { ChatIcon, ChevronLeftIcon, ChevronRightIcon, ExclamationIcon, PencilIcon } from '../../shared/ui/icons/Icons'
import { PageHead } from '../../shared/ui/PageHead'
import { SegmentedControl } from '../../shared/ui/SegmentedControl'
import { formatRelative } from '../../shared/utils/datetime'

const PAGE_SIZE = 20
const ALL = 'ALL'

const isPostType = (value: string | null): value is PostTypeCode => POST_TYPES.some((t) => t.code === value)
const isLoanType = (value: string | null): value is LoanTypeCode => LOAN_TYPES.some((t) => t.code === value)

/** SCR-02 커뮤니티 목록 (FR-CM-002). 최신순, 글 유형·대출 유형으로 거른다 */
export default function PostListPage() {
  const [search, setSearch] = useSearchParams()
  const rawPostType = search.get('postType')
  const rawLoanType = search.get('loanType')
  const postType = isPostType(rawPostType) ? rawPostType : undefined
  const loanType = isLoanType(rawLoanType) ? rawLoanType : undefined
  const page = pageParam(search)

  const [result, setResult] = useState<PageResponse<PostSummary> | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    communityApi
      .list({ postType, loanType, page, size: PAGE_SIZE })
      .then((data) => {
        if (cancelled) return
        setResult(data)
        setError(null)
      })
      .catch((e) => {
        if (!cancelled) setError(errorMessage(e, '글 목록을 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [postType, loanType, page])

  function update(key: 'postType' | 'loanType' | 'page', value: string | null) {
    const next = new URLSearchParams(search)
    if (value === null || value === ALL) next.delete(key)
    else next.set(key, value)
    if (key !== 'page') next.delete('page')
    setSearch(next)
  }

  return (
    <>
      <div className="head-with-action">
        <PageHead
          eyebrow="커뮤니티"
          title="대출, 혼자 고민하지 마세요."
          lede="질문하고, 후기와 피해 사례를 나눠요. 모든 글은 익명이고, 연락처나 개인정보는 올릴 수 없어요."
        />
        <Link to={ROUTES.SCR04_POST_NEW} className="btn btn--primary head-with-action__button">
          <PencilIcon size={18} /> 글쓰기
        </Link>
      </div>

      <div className="filters">
        <SegmentedControl
          label="글 유형"
          value={postType ?? ALL}
          options={[{ value: ALL, label: '전체' }, ...POST_TYPES.map((t) => ({ value: t.code, label: t.label }))]}
          onChange={(value) => update('postType', value)}
        />
        <div className="chips" role="group" aria-label="대출 유형">
          {[{ code: ALL, label: '모든 대출' }, ...LOAN_TYPES].map((t) => {
            const active = (loanType ?? ALL) === t.code
            return (
              <button
                key={t.code}
                type="button"
                aria-pressed={active}
                className={active ? 'chip chip--active' : 'chip'}
                onClick={() => update('loanType', t.code)}
              >
                {t.label}
              </button>
            )
          })}
        </div>
      </div>

      {error ? (
        <p className="error-text mt-m" role="alert">
          {error}
        </p>
      ) : null}
      {!result && !error ? <div className="skeleton skeleton--list mt-l" /> : null}

      {result ? (
        result.content.length === 0 ? (
          <div className="empty mt-l">
            <ChatIcon size={40} />
            <p className="empty__title">아직 글이 없어요</p>
            <p className="caption">첫 질문이나 후기를 남겨 주세요.</p>
            <Link to={ROUTES.SCR04_POST_NEW} className="btn btn--secondary btn--small">
              글쓰기
            </Link>
          </div>
        ) : (
          <ul className="group mt-l">
            {result.content.map((post) => (
              <li key={post.postId}>
                <Link to={postPath(post.postId)} className="row post-row">
                  <span className="row__main">
                    <span className="post-row__tags">
                      <span className="pill pill--plain pill--blue">{post.postType.description}</span>
                      <span className="caption">{post.loanType.description}</span>
                      {post.warnings.length > 0 ? (
                        <span className="pill pill--plain pill--orange">
                          <ExclamationIcon size={12} /> 주의 신호 {post.warnings.length}
                        </span>
                      ) : null}
                    </span>
                    <span className="row__title post-row__title">{post.title}</span>
                    <span className="post-row__excerpt">{post.excerpt}</span>
                    <span className="post-row__meta">
                      <AuthorBadge role={post.authorRole} />
                      <span>{formatRelative(post.createdAt)}</span>
                      {post.company ? <span>· {post.company.name}</span> : null}
                      <span className="post-row__answers">
                        <ChatIcon size={14} /> {post.answerCount}
                      </span>
                    </span>
                  </span>
                  <ChevronRightIcon className="row__chevron" />
                </Link>
              </li>
            ))}
          </ul>
        )
      ) : null}

      {result && result.totalPages > 1 ? (
        <nav className="pager" aria-label="페이지">
          <button type="button" className="btn btn--secondary btn--small" disabled={page <= 1} onClick={() => update('page', String(page - 1))}>
            <ChevronLeftIcon size={12} /> 이전
          </button>
          <span>
            {result.pageNumber} / {result.totalPages}
          </span>
          <button type="button" className="btn btn--secondary btn--small" disabled={!result.hasNext} onClick={() => update('page', String(page + 1))}>
            다음 <ChevronRightIcon size={12} />
          </button>
        </nav>
      ) : null}
    </>
  )
}
