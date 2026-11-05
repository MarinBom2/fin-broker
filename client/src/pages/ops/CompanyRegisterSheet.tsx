import { useState } from 'react'
import type { FormEvent } from 'react'
import { COMPANY_TYPES, opsCompanyApi } from '../../shared/api/company/companyService'
import type { CompanyTypeCode, OpsCompany } from '../../shared/api/company/companyService'
import { errorMessage } from '../../shared/core/apiClient'
import { Modal } from '../../shared/ui/Modal'
import { SegmentedControl } from '../../shared/ui/SegmentedControl'

/**
 * SCR-08 업체 등록 시트 (FR-CO-002). 등록 정보에서 확인한 사실 네 가지만 받는다 (금리·한도·홍보 칸 없음).
 * 열 때마다 새로 그리도록 호출부가 key 를 바꿔 준다.
 */
export function CompanyRegisterSheet({
  open,
  onClose,
  onRegistered,
}: {
  open: boolean
  onClose: () => void
  onRegistered: (company: OpsCompany) => void
}) {
  const [name, setName] = useState('')
  const [companyType, setCompanyType] = useState<CompanyTypeCode>('LENDER')
  const [registrationNumber, setRegistrationNumber] = useState('')
  const [registrationAuthority, setRegistrationAuthority] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const ready = name.trim() && registrationNumber.trim() && registrationAuthority.trim()

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!ready) return
    setSending(true)
    setError(null)
    try {
      onRegistered(await opsCompanyApi.register({ name, companyType, registrationNumber, registrationAuthority }))
    } catch (e) {
      setError(errorMessage(e, '등록하지 못했습니다.'))
    } finally {
      setSending(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} labelledBy="company-register-title" variant="sheet">
      <form className="stack" onSubmit={handleSubmit}>
        <header className="sheet__head">
          <h2 id="company-register-title" className="sheet__title">
            업체 등록
          </h2>
          <p className="caption">등록 정보에 적힌 그대로 넣어 주세요. 등록하면 ‘확인 전’으로 시작하고, 대조한 뒤에 결과를 남겨요.</p>
        </header>

        <div className="form-field">
          <span className="form-field__label">업체 유형</span>
          <SegmentedControl label="업체 유형" value={companyType} options={COMPANY_TYPES} onChange={setCompanyType} />
        </div>

        <div className="field-group">
          <label className="field-group__item">
            <span className="field-group__label">등록 상호</span>
            <input
              className="field-group__input"
              maxLength={100}
              autoComplete="off"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </label>
          <label className="field-group__item">
            <span className="field-group__label">등록번호</span>
            <input
              className="field-group__input"
              maxLength={50}
              autoComplete="off"
              value={registrationNumber}
              onChange={(e) => setRegistrationNumber(e.target.value)}
            />
          </label>
          <label className="field-group__item">
            <span className="field-group__label">등록기관 (예: 서울특별시, 금융위원회)</span>
            <input
              className="field-group__input"
              maxLength={50}
              autoComplete="off"
              value={registrationAuthority}
              onChange={(e) => setRegistrationAuthority(e.target.value)}
            />
          </label>
        </div>

        {error ? (
          <p className="error-text" role="alert">
            {error}
          </p>
        ) : null}
        <div className="sheet__actions">
          <button type="button" className="btn btn--secondary" onClick={onClose}>
            취소
          </button>
          <button type="submit" className="btn btn--primary" disabled={!ready || sending}>
            {sending ? '등록하는 중…' : '등록'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
