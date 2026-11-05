import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { pageParam } from '../../routes'
import { opsCompanyApi } from '../../shared/api/company/companyService'
import type { OpsCompany, OpsCompanySummary, RegistrationStatusCode } from '../../shared/api/company/companyService'
import { errorMessage } from '../../shared/core/apiClient'
import type { PageResponse } from '../../shared/types/common'
import { CheckCircleIcon, ChevronLeftIcon, ChevronRightIcon, SearchIcon } from '../../shared/ui/icons/Icons'
import { ConfirmDialog } from '../../shared/ui/Modal'
import { PageHead } from '../../shared/ui/PageHead'
import { StatusPill } from '../../shared/ui/StatusPill'
import { formatRelative } from '../../shared/utils/datetime'
import { CompanyRegisterSheet } from './CompanyRegisterSheet'
import { OpsNav } from './OpsNav'

const PAGE_SIZE = 20

/** 현황 칸이자 걸러 보기. RECHECK 는 등록 확인한 지 기간이 지난 업체 */
type Filter = 'ALL' | RegistrationStatusCode | 'RECHECK'

const TILES: { filter: Filter; label: string; tone?: 'todo' }[] = [
  { filter: 'ALL', label: '전체' },
  { filter: 'UNVERIFIED', label: '확인 전', tone: 'todo' },
  { filter: 'RECHECK', label: '재확인 필요', tone: 'todo' },
  { filter: 'VERIFIED', label: '등록 확인' },
  { filter: 'NOT_FOUND', label: '정보 없음' },
  { filter: 'EXPIRED', label: '등록 만료' },
]

const isFilter = (value: string | null): value is Filter => TILES.some((tile) => tile.filter === value)

function tileCount(summary: OpsCompanySummary, filter: Filter) {
  if (filter === 'ALL') return summary.total
  if (filter === 'RECHECK') return summary.recheckDue
  return summary.statuses.find((s) => s.status.code === filter)?.count ?? 0
}

/** 대조 결과 버튼 문구. 지금 상태와 같은 결과는 "다시 대조했는데 그대로"라는 뜻이다 */
function resultLabel(current: RegistrationStatusCode, result: RegistrationStatusCode) {
  if (result === 'VERIFIED') return current === 'VERIFIED' ? '확인 유지' : '등록 확인'
  if (result === 'NOT_FOUND') return current === 'NOT_FOUND' ? '정보 없음 유지' : '정보 없음'
  if (result === 'EXPIRED') return current === 'EXPIRED' ? '만료 유지' : '만료'
  return result
}

function resultClass(current: RegistrationStatusCode, result: RegistrationStatusCode) {
  if (result === current) return 'btn btn--secondary btn--small'
  if (result === 'VERIFIED') return 'btn btn--primary btn--small'
  if (result === 'EXPIRED') return 'btn btn--danger btn--small'
  return 'btn btn--secondary btn--small'
}

/** SCR-08 운영자 업체 관리 (FR-CO-002). 등록 정보와 대조한 결과만 남긴다. 순위·노출을 바꾸는 기능은 없다 */
export default function OpsCompaniesPage() {
  const [search, setSearch] = useSearchParams()
  const rawFilter = search.get('filter')
  const filter: Filter = isFilter(rawFilter) ? rawFilter : 'ALL'
  const keyword = search.get('keyword') ?? ''
  const page = pageParam(search)

  const [input, setInput] = useState(keyword)
  const [result, setResult] = useState<PageResponse<OpsCompany> | null>(null)
  const [summary, setSummary] = useState<OpsCompanySummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [expiring, setExpiring] = useState<OpsCompany | null>(null)
  const [sheetKey, setSheetKey] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    const status = filter === 'ALL' || filter === 'RECHECK' ? undefined : filter
    opsCompanyApi
      .list({ status, recheckDue: filter === 'RECHECK' || undefined, keyword: keyword || undefined, page, size: PAGE_SIZE })
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
  }, [filter, keyword, page, reloadKey])

  useEffect(() => {
    let cancelled = false
    opsCompanyApi
      .summary()
      .then((data) => {
        if (!cancelled) setSummary(data)
      })
      .catch(() => {
        // 현황은 보조 정보라 실패해도 목록은 그대로 쓴다
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  function update(next: { filter?: Filter; keyword?: string; page?: number }) {
    const params = new URLSearchParams(search)
    if (next.filter !== undefined) {
      if (next.filter === 'ALL') params.delete('filter')
      else params.set('filter', next.filter)
    }
    if (next.keyword !== undefined) {
      if (next.keyword) params.set('keyword', next.keyword)
      else params.delete('keyword')
    }
    if (next.page && next.page > 1) params.set('page', String(next.page))
    else params.delete('page')
    setSearch(params)
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    update({ keyword: input.trim() })
  }

  async function record(company: OpsCompany, resultCode: RegistrationStatusCode) {
    setBusyId(company.companyId)
    try {
      const updated = await opsCompanyApi.recordCheck(company.companyId, resultCode)
      // 걸러 보는 중이어도 줄을 바로 빼지 않고 바뀐 상태를 보여 준다. 현황 숫자만 다시 읽는다
      setResult((prev) =>
        prev ? { ...prev, content: prev.content.map((c) => (c.companyId === updated.companyId ? updated : c)) } : prev,
      )
      opsCompanyApi.summary().then(setSummary, () => undefined)
      setError(null)
    } catch (e) {
      setError(errorMessage(e, '대조 결과를 남기지 못했습니다.'))
    } finally {
      setBusyId(null)
      setExpiring(null)
    }
  }

  function handleResult(company: OpsCompany, resultCode: RegistrationStatusCode) {
    if (resultCode === 'EXPIRED' && company.registrationStatus.code !== 'EXPIRED') {
      setExpiring(company)
      return
    }
    void record(company, resultCode)
  }

  const recheckDays = summary?.recheckAfterDays ?? 90

  return (
    <>
      <OpsNav />
      <div className="head-with-action">
        <PageHead
          eyebrow="운영"
          title="업체 관리"
          lede={`등록 정보와 대조한 결과만 남겨요. 금리·한도·홍보 문구는 받지 않아요. 확인한 지 ${recheckDays}일이 지나면 다시 대조해 주세요.`}
        />
        <button type="button" className="btn btn--primary head-with-action__button only-wide" onClick={() => setSheetKey(Date.now())}>
          업체 등록
        </button>
      </div>

      <div className="ops-tiles" role="radiogroup" aria-label="업체 현황">
        {TILES.map((tile) => {
          const count = summary ? tileCount(summary, tile.filter) : null
          const active = tile.filter === filter
          const classes = ['ops-tile']
          if (active) classes.push('ops-tile--active')
          if (tile.tone === 'todo' && count) classes.push('ops-tile--todo')
          return (
            <button
              key={tile.filter}
              type="button"
              role="radio"
              aria-checked={active}
              className={classes.join(' ')}
              onClick={() => update({ filter: tile.filter })}
            >
              <span className="ops-tile__count">{count === null ? '–' : count.toLocaleString('ko-KR')}</span>
              <span className="ops-tile__label">{tile.label}</span>
            </button>
          )
        })}
      </div>

      <div className="ops-toolbar mt-l">
        <form className="search ops-toolbar__search" onSubmit={handleSearch} role="search">
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
        <button type="button" className="btn btn--primary only-narrow" onClick={() => setSheetKey(Date.now())}>
          업체 등록
        </button>
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
            <CheckCircleIcon size={40} />
            <p className="empty__title">
              {keyword
                ? `‘${keyword}’에 맞는 업체가 없어요`
                : filter === 'UNVERIFIED' || filter === 'RECHECK'
                  ? '대조할 업체가 없어요'
                  : '아직 없어요'}
            </p>
          </div>
        ) : (
          <ul className="group mt-l">
            {result.content.map((company) => {
              const current = company.registrationStatus.code
              return (
                <li key={company.companyId} className="row row--with-avatar ops-company">
                  <span className="avatar" aria-hidden="true">
                    {company.name.slice(0, 1)}
                  </span>
                  <span className="row__main">
                    <span className="ops-company__title">
                      <span className="row__title">{company.name}</span>
                      <StatusPill status={company.registrationStatus} />
                      {company.recheckDue ? <span className="pill pill--plain pill--orange">재확인 필요</span> : null}
                    </span>
                    <span className="row__sub">
                      {company.companyType.description} · {company.registrationNumber} · {company.registrationAuthority}
                    </span>
                    <span className="ops-company__meta">
                      {company.lastCheckedAt
                        ? `마지막 대조 ${formatRelative(company.lastCheckedAt)}`
                        : `등록 ${formatRelative(company.createdAt)} · 아직 대조하지 않았어요`}
                    </span>
                  </span>
                  <span className="ops-company__actions" aria-label={`${company.name} 대조 결과`}>
                    {company.allowedResults.map((allowed) => (
                      <button
                        key={allowed.code}
                        type="button"
                        className={resultClass(current, allowed.code)}
                        disabled={busyId === company.companyId}
                        onClick={() => handleResult(company, allowed.code)}
                      >
                        {resultLabel(current, allowed.code)}
                      </button>
                    ))}
                  </span>
                </li>
              )
            })}
          </ul>
        )
      ) : null}

      {result && result.totalPages > 1 ? (
        <nav className="pager" aria-label="페이지">
          <button type="button" className="btn btn--secondary btn--small" disabled={page <= 1} onClick={() => update({ page: page - 1 })}>
            <ChevronLeftIcon size={12} /> 이전
          </button>
          <span>
            {result.pageNumber} / {result.totalPages}
          </span>
          <button type="button" className="btn btn--secondary btn--small" disabled={!result.hasNext} onClick={() => update({ page: page + 1 })}>
            다음 <ChevronRightIcon size={12} />
          </button>
        </nav>
      ) : null}

      <ConfirmDialog
        open={expiring !== null}
        title={`${expiring?.name ?? ''}을(를) 만료로 바꿀까요?`}
        message="업체 조회와 관련 글에 ‘등록 만료’로 보여요. 등록 정보에서 다시 확인되면 ‘등록 확인’으로 되돌릴 수 있어요."
        confirmLabel="만료로 바꾸기"
        busy={busyId !== null}
        onConfirm={() => expiring && void record(expiring, 'EXPIRED')}
        onCancel={() => setExpiring(null)}
      />

      {sheetKey !== null ? (
        <CompanyRegisterSheet
          key={sheetKey}
          open
          onClose={() => setSheetKey(null)}
          onRegistered={() => {
            setSheetKey(null)
            update({ filter: 'UNVERIFIED', keyword: '' })
            setInput('')
            setReloadKey((key) => key + 1)
          }}
        />
      ) : null}
    </>
  )
}
