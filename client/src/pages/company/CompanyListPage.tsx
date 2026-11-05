import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { companyPath, pageParam } from '../../routes'
import { companyApi } from '../../shared/api/company/companyService'
import type { Company } from '../../shared/api/company/companyService'
import { errorMessage } from '../../shared/core/apiClient'
import type { PageResponse } from '../../shared/types/common'
import { ChevronLeftIcon, ChevronRightIcon, InfoIcon, SearchIcon } from '../../shared/ui/icons/Icons'
import { PageHead } from '../../shared/ui/PageHead'
import { StatusPill } from '../../shared/ui/StatusPill'

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
    <>
      <PageHead
        eyebrow="업체 조회"
        title="등록된 업체인지 확인하세요."
        lede="상호 일부나 등록번호로 찾을 수 있어요. 목록은 상호 가나다순이고, 광고나 비용으로 순서가 바뀌지 않아요."
      />

      <form className="search" onSubmit={handleSubmit} role="search">
        <SearchIcon className="search__icon" size={18} />
        <input
          className="search__input"
          type="search"
          aria-label="상호 또는 등록번호"
          placeholder="상호 또는 등록번호"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
      </form>

      {error ? (
        <p className="error-text mt-m" role="alert">
          {error}
        </p>
      ) : null}

      {!result && !error ? <div className="skeleton skeleton--list mt-l" /> : null}

      {result ? (
        <div className="stack mt-l">
          <p className="group__label">
            {keyword ? `‘${keyword}’ 검색 결과 ` : '전체 '}
            {result.totalSize.toLocaleString('ko-KR')}곳
          </p>
          {result.content.length === 0 ? (
            <div className="callout callout--orange">
              <InfoIcon className="callout__icon" />
              찾는 업체가 없어요. 등록 정보에 없는 업체라면 거래 전에 등록 여부를 다시 확인해 주세요.
            </div>
          ) : (
            <ul className="group">
              {result.content.map((company) => (
                <li key={company.companyId}>
                  <Link to={companyPath(company.companyId)} className="row row--with-avatar">
                    <span className="avatar" aria-hidden="true">
                      {company.name.slice(0, 1)}
                    </span>
                    <span className="row__main">
                      <span className="row__title">{company.name}</span>
                      <span className="row__sub">
                        {company.companyType.description} · {company.registrationNumber}
                      </span>
                      <span className="only-narrow">
                        <StatusPill status={company.registrationStatus} />
                      </span>
                    </span>
                    <span className="only-wide">
                      <StatusPill status={company.registrationStatus} />
                    </span>
                    <ChevronRightIcon className="row__chevron" />
                  </Link>
                </li>
              ))}
            </ul>
          )}

          {result.totalPages > 1 ? (
            <nav className="pager" aria-label="페이지">
              <button type="button" className="btn btn--secondary btn--small" disabled={page <= 1} onClick={() => goToPage(page - 1)}>
                <ChevronLeftIcon size={12} /> 이전
              </button>
              <span>
                {result.pageNumber} / {result.totalPages}
              </span>
              <button type="button" className="btn btn--secondary btn--small" disabled={!result.hasNext} onClick={() => goToPage(page + 1)}>
                다음 <ChevronRightIcon size={12} />
              </button>
            </nav>
          ) : null}
        </div>
      ) : null}
    </>
  )
}
