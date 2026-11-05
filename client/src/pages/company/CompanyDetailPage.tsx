import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ROUTES } from '../../routes'
import { companyApi } from '../../shared/api/company/companyService'
import type { Company } from '../../shared/api/company/companyService'
import { errorMessage } from '../../shared/core/apiClient'
import { RegistrationBadge } from '../../shared/ui/RegistrationBadge'
import { formatKstDate } from '../../shared/utils/datetime'

/** SCR-05 업체 상세 (FR-CO-001). 연락처·금리·상품 소개는 보여 주지 않는다 */
export default function CompanyDetailPage() {
  const { companyId } = useParams()
  const id = Number(companyId)
  const validId = Number.isInteger(id) && id > 0
  const [company, setCompany] = useState<Company | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const error = validId ? loadError : '업체 정보를 찾을 수 없습니다.'

  useEffect(() => {
    if (!validId) return
    let cancelled = false
    companyApi
      .get(id)
      .then((data) => {
        if (!cancelled) setCompany(data)
      })
      .catch((e) => {
        if (!cancelled) setLoadError(errorMessage(e, '업체 정보를 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [id, validId])

  if (error) {
    return (
      <section className="stack">
        <p className="error" role="alert">
          {error}
        </p>
        <Link to={ROUTES.SCR05_COMPANIES}>업체 조회로 돌아가기</Link>
      </section>
    )
  }
  if (!company) {
    return <div className="page-loading" aria-busy="true" />
  }

  const verified = company.registrationStatus.code === 'VERIFIED'

  return (
    <section className="stack">
      <Link to={ROUTES.SCR05_COMPANIES} className="muted">
        ← 업체 조회
      </Link>
      <h1>{company.name}</h1>
      <RegistrationBadge status={company.registrationStatus} />

      <dl className="facts">
        <dt>업체 유형</dt>
        <dd>{company.companyType.description}</dd>
        <dt>등록번호</dt>
        <dd>{company.registrationNumber}</dd>
        <dt>등록기관</dt>
        <dd>{company.registrationAuthority}</dd>
        <dt>마지막 확인</dt>
        <dd>{formatKstDate(company.lastCheckedAt)}</dd>
      </dl>

      <p className={verified ? 'muted' : 'notice'}>
        {verified
          ? '등록 정보와 대조해 확인한 업체입니다. 등록 여부는 업체의 대출 조건이나 안전을 보장하지 않습니다.'
          : '등록 정보를 확인하지 못한 업체입니다. 거래 전에 등록 여부를 직접 확인해 주세요.'}
      </p>
    </section>
  )
}
