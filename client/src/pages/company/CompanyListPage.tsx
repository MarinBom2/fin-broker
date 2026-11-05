import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { companyPath, pageParam } from '../../routes'
import { companyApi } from '../../shared/api/company/companyService'
import type { Company } from '../../shared/api/company/companyService'
import { errorMessage } from '../../shared/core/apiClient'
import type { PageResponse } from '../../shared/types/common'
import { RegistrationBadge } from '../../shared/ui/RegistrationBadge'

const PAGE_SIZE = 20

/** SCR-05 업체 조회 (FR-CO-001). 순서는 상호 가나다순이고, 돈으로 바뀌지 않는다 */
export default function CompanyListPage() {
  const [search, setSearch] = useSearchParams()
  const keyword = search.get('keyword') ?? ''
  const page = pageParam(search)

  const [input, setInput] = useState(keyword)
  const [result, setResult] = useState<PageResponse<Company> | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    companyApi
      .search({ keyword: keyword || undefined, page, size: PAGE_SIZE })
      .then((data) => {
        if (cancelled) return
        setResult(data)
        setError(null)
      })
      .catch((e) => {
        if (!cancelled) setError(errorMessage(e, '업체 목록을 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [keyword, page])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = new URLSearchParams()
    if (input.trim()) next.set('keyword', input.trim())
    setSearch(next)
  }

  function goToPage(nextPage: number) {
    const next = new URLSearchParams(search)
    next.set('page', String(nextPage))
    setSearch(next)
  }

  return (
    <section className="stack">
      <h1>업체 조회</h1>
      <p className="muted">
        대출 업체의 등록 정보와 확인 상태를 보여 드립니다. 목록 순서는 상호 가나다순이며, 광고나 비용으로 바뀌지 않습니다.
      </p>

      <form className="search-row" onSubmit={handleSubmit} role="search">
        <input
          type="search"
          aria-label="상호 또는 등록번호"
          placeholder="상호 일부 또는 등록번호"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
        <button type="submit" className="button">
          검색
        </button>
      </form>

      {error ? (
        <p className="error" role="alert">
          {error}
        </p>
      ) : null}

      {result ? (
        <>
          <p className="muted">{result.totalSize.toLocaleString('ko-KR')}곳</p>
          {result.content.length === 0 ? (
            <p className="notice">
              찾는 업체가 없습니다. 등록 정보에 없는 업체라면 거래 전에 금융감독원 등록 조회로 다시 확인해 주세요.
            </p>
          ) : (
            <ul className="list">
              {result.content.map((company) => (
                <li key={company.companyId}>
                  <Link to={companyPath(company.companyId)} className="list-item">
                    <span className="list-item__title">{company.name}</span>
                    <span className="muted">
                      {company.companyType.description} · {company.registrationNumber}
                    </span>
                    <RegistrationBadge status={company.registrationStatus} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
          <nav className="pager" aria-label="페이지">
            <button type="button" className="button button--ghost" disabled={page <= 1} onClick={() => goToPage(page - 1)}>
              이전
            </button>
            <span className="muted">
              {result.pageNumber} / {Math.max(result.totalPages, 1)}
            </span>
            <button type="button" className="button button--ghost" disabled={!result.hasNext} onClick={() => goToPage(page + 1)}>
              다음
            </button>
          </nav>
        </>
      ) : null}
    </section>
  )
}
