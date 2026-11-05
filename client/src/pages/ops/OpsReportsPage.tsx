import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { pageParam, postPath } from '../../routes'
import { reportApi } from '../../shared/api/moderation/reportService'
import type { OpsReport, ReportStatusCode } from '../../shared/api/moderation/reportService'
import { errorMessage } from '../../shared/core/apiClient'
import type { PageResponse } from '../../shared/types/common'
import { AuthorBadge } from '../../shared/ui/AuthorBadge'
import { CheckCircleIcon, ChevronLeftIcon, ChevronRightIcon, FlagIcon } from '../../shared/ui/icons/Icons'
import { PageHead } from '../../shared/ui/PageHead'
import { SegmentedControl } from '../../shared/ui/SegmentedControl'
import { formatRelative } from '../../shared/utils/datetime'
import { OpsNav } from './OpsNav'

const PAGE_SIZE = 20
const STATUSES: { value: ReportStatusCode; label: string }[] = [
  { value: 'OPEN', label: '접수' },
  { value: 'ACCEPTED', label: '숨김 처리' },
  { value: 'DISMISSED', label: '문제없음' },
]

const isStatus = (value: string | null): value is ReportStatusCode => STATUSES.some((s) => s.value === value)

/** SCR-08 운영자 신고 처리 (FR-MD-002). 처리는 대상 단위라 같은 대상의 접수 신고가 함께 닫힌다 */
export default function OpsReportsPage() {
  const [search, setSearch] = useSearchParams()
  const rawStatus = search.get('status')
  const status: ReportStatusCode = isStatus(rawStatus) ? rawStatus : 'OPEN'
  const page = pageParam(search)

  const [result, setResult] = useState<PageResponse<OpsReport> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    reportApi
      .opsList({ status, page, size: PAGE_SIZE })
      .then((data) => {
        if (cancelled) return
        setResult(data)
        setError(null)
      })
      .catch((e) => {
        if (!cancelled) setError(errorMessage(e, '신고 목록을 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [status, page, reloadKey])

  async function decide(report: OpsReport, decision: 'ACCEPTED' | 'DISMISSED') {
    setBusyId(report.reportId)
    try {
      await reportApi.decide(report.reportId, decision)
      setReloadKey((key) => key + 1)
    } catch (e) {
      setError(errorMessage(e, '처리하지 못했습니다.'))
    } finally {
      setBusyId(null)
    }
  }

  function update(key: 'status' | 'page', value: string) {
    const next = new URLSearchParams(search)
    next.set(key, value)
    if (key !== 'page') next.delete('page')
    setSearch(next)
  }

  return (
    <>
      <OpsNav />
      <PageHead
        eyebrow="운영"
        title="신고 처리"
        lede="같은 대상의 신고는 한 번에 처리돼요. 숨기면 목록과 상세에서 사라지고, 문제없음이면 신고 누적으로 숨겨진 글도 다시 보여요."
      />

      <SegmentedControl label="처리 상태" value={status} options={STATUSES} onChange={(value) => update('status', value)} />

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
            <p className="empty__title">{status === 'OPEN' ? '처리할 신고가 없어요' : '아직 없어요'}</p>
          </div>
        ) : (
          <ul className="ops-list mt-l">
            {result.content.map((report) => (
              <li key={report.reportId} className="ops-card">
                <div className="post-row__tags">
                  <span className="pill pill--plain pill--blue">{report.targetType.description}</span>
                  {report.reasons.map((reason) => (
                    <span key={reason.code} className="pill pill--plain pill--orange">
                      <FlagIcon size={12} /> {reason.description}
                    </span>
                  ))}
                  <span className={report.reportCount > 1 ? 'pill pill--plain pill--red' : 'pill pill--plain pill--gray'}>
                    신고 {report.reportCount}건
                  </span>
                  {report.targetStatus ? (
                    <span className={report.targetStatus.code === 'PUBLISHED' ? 'pill pill--green' : 'pill pill--gray'}>
                      {report.targetStatus.description}
                    </span>
                  ) : null}
                </div>

                <p className="ops-card__title">{report.postTitle || '(삭제된 글)'}</p>
                <p className="ops-card__excerpt">{report.excerpt || '(내용 없음)'}</p>
                {report.details.length > 0 ? (
                  <ul className="ops-card__details" aria-label="신고자 메모">
                    {report.details.map((detail, index) => (
                      <li key={index}>“{detail}”</li>
                    ))}
                  </ul>
                ) : null}

                <div className="post__meta-row">
                  <div className="post-row__meta">
                    {report.authorRole ? <AuthorBadge role={report.authorRole} /> : null}
                    <span>최근 신고 {formatRelative(report.lastReportedAt)}</span>
                    {report.decidedAt ? <span>· 처리 {formatRelative(report.decidedAt)}</span> : null}
                    {report.targetStatus?.code === 'PUBLISHED' ? (
                      <Link to={postPath(report.postId)} className="action">
                        글 보기 <ChevronRightIcon size={12} />
                      </Link>
                    ) : null}
                  </div>
                  {report.status.code === 'OPEN' ? (
                    <div className="cluster">
                      <button
                        type="button"
                        className="btn btn--secondary btn--small"
                        disabled={busyId === report.reportId}
                        onClick={() => void decide(report, 'DISMISSED')}
                      >
                        문제없음
                      </button>
                      <button
                        type="button"
                        className="btn btn--danger btn--small"
                        disabled={busyId === report.reportId}
                        onClick={() => void decide(report, 'ACCEPTED')}
                      >
                        숨기기
                      </button>
                    </div>
                  ) : (
                    <span className="pill pill--gray pill--plain">{report.status.description}</span>
                  )}
                </div>
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
