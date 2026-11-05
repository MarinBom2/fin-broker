import type { Company } from '../api/company/companyService'

/** 등록 확인 상태 배지. 확인된 업체만 초록, 나머지는 주의 색 */
export function RegistrationBadge({ status }: { status: Company['registrationStatus'] }) {
  const tone = status.code === 'VERIFIED' ? 'badge--ok' : 'badge--warn'
  return <span className={`badge ${tone}`}>{status.description}</span>
}
