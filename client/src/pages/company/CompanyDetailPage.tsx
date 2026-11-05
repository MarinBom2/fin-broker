import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ROUTES } from '../../routes'
import { companyApi } from '../../shared/api/company/companyService'
import type { Company } from '../../shared/api/company/companyService'
import { errorMessage } from '../../shared/core/apiClient'
import { CheckCircleIcon, ChevronLeftIcon, ExclamationIcon } from '../../shared/ui/icons/Icons'
import { StatusPill } from '../../shared/ui/StatusPill'
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

  const back = (
    <Link to={ROUTES.SCR05_COMPANIES} className="back-link">
      <ChevronLeftIcon /> 업체 조회
    </Link>
  )

  if (error) {
    return (
      <>
        {back}
        <p className="error-text" role="alert">
          {error}
        </p>
      </>
    )
  }
  if (!company) {
    return (
      <>
        {back}
        <div className="skeleton skeleton--detail" />
      </>
    )
  }

  const verified = company.registrationStatus.code === 'VERIFIED'

  return (
    <>
      {back}
      <header className="detail-head">
        <span className="avatar avatar--large" aria-hidden="true">
          {company.name.slice(0, 1)}
        </span>
        <div className="stack stack--tight">
          <h1 className="page-head__title">{company.name}</h1>
          <StatusPill status={company.registrationStatus} />
        </div>
      </header>

      <h2 className="section-title">등록 정보</h2>
      <ul className="group">
        <li className="row">
          <span className="row__label">업체 유형</span>
          <span className="row__value">{company.companyType.description}</span>
        </li>
        <li className="row">
          <span className="row__label">등록번호</span>
          <span className="row__value">{company.registrationNumber}</span>
        </li>
        <li className="row">
          <span className="row__label">등록기관</span>
          <span className="row__value">{company.registrationAuthority}</span>
        </li>
        <li className="row">
          <span className="row__label">마지막 확인</span>
          <span className="row__value">{formatKstDate(company.lastCheckedAt)}</span>
        </li>
      </ul>

      <div className={`callout mt-m ${verified ? 'callout--green' : 'callout--orange'}`}>
        {verified ? <CheckCircleIcon className="callout__icon" /> : <ExclamationIcon className="callout__icon" />}
        <span>
          {verified
            ? '등록 정보와 대조해 확인한 업체예요. 다만 등록 여부만으로 대출 조건이 적절한지는 알 수 없으니 계약 전에 금리와 비용을 꼭 확인하세요.'
            : '등록 정보를 확인하지 못한 업체예요. 거래 전에 등록 여부를 직접 확인해 주세요.'}
        </span>
      </div>
    </>
  )
}
