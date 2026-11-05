import type { Company } from '../api/company/companyService'

/** 등록 확인 상태. 확인된 업체만 초록, 대조 전은 회색, 나머지는 주황 */
export function StatusPill({ status }: { status: Company['registrationStatus'] }) {
  const tone = status.code === 'VERIFIED' ? 'pill--green' : status.code === 'UNVERIFIED' ? 'pill--gray' : 'pill--orange'
  return <span className={`pill ${tone}`}>{status.description}</span>
}
