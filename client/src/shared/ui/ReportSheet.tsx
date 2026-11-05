import { useState } from 'react'
import type { FormEvent } from 'react'
import { REPORT_DETAIL_MAX, REPORT_REASONS, reportApi } from '../api/moderation/reportService'
import type { ReportReasonCode, ReportTargetCode } from '../api/moderation/reportService'
import { ApiError, errorMessage } from '../core/apiClient'
import { CheckCircleIcon, CheckIcon } from './icons/Icons'
import { Modal } from './Modal'

/**
 * 신고 시트 (FR-MD-001). 사유를 고르고 설명을 덧붙여 보낸다. 이미 신고했으면(ALREADY_REPORTED) 신고한 것으로 본다.
 * 열 때마다 새로 그리도록 호출부가 key 를 대상마다 다르게 준다.
 */
export function ReportSheet({
  open,
  target,
  onClose,
  onReported,
}: {
  open: boolean
  target: { type: ReportTargetCode; id: number }
  onClose: () => void
  onReported: () => void
}) {
  const [reason, setReason] = useState<ReportReasonCode | null>(null)
  const [detail, setDetail] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState<'filed' | 'hidden' | null>(null)

  const label = target.type === 'POST' ? '글' : '답변'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!reason) return
    setSending(true)
    setError(null)
    try {
      const result = await reportApi.file({ targetType: target.type, targetId: target.id, reason, detail: detail || undefined })
      setDone(result.targetHidden ? 'hidden' : 'filed')
      onReported()
    } catch (e) {
      if (e instanceof ApiError && e.code === 'ALREADY_REPORTED') {
        setDone('filed')
        onReported()
      } else {
        setError(errorMessage(e, '신고하지 못했습니다.'))
      }
    } finally {
      setSending(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} labelledBy="report-title" variant="sheet">
      {done ? (
        <div className="sheet__done">
          <CheckCircleIcon size={44} />
          <h2 id="report-title" className="sheet__title">
            신고했어요
          </h2>
          <p className="caption">
            {done === 'hidden'
              ? `여러 분이 신고해 이 ${label}은 먼저 숨겨졌어요. 운영자가 확인할게요.`
              : '운영자가 확인할게요. 신고한 분은 밝혀지지 않아요.'}
          </p>
          <button type="button" className="btn btn--primary btn--block" onClick={onClose}>
            확인
          </button>
        </div>
      ) : (
        <form className="stack" onSubmit={handleSubmit}>
          <header className="sheet__head">
            <h2 id="report-title" className="sheet__title">
              이 {label}을 신고할까요?
            </h2>
            <p className="caption">사유를 골라 주세요. 신고한 분은 밝혀지지 않아요.</p>
          </header>
          <ul className="group" role="radiogroup" aria-label="신고 사유">
            {REPORT_REASONS.map((option) => (
              <li key={option.code}>
                <button
                  type="button"
                  role="radio"
                  aria-checked={reason === option.code}
                  className="row row--button"
                  onClick={() => setReason(option.code)}
                >
                  <span className="row__main">
                    <span className="row__title">{option.label}</span>
                    <span className="row__sub">{option.hint}</span>
                  </span>
                  {reason === option.code ? <CheckIcon className="row__check" /> : null}
                </button>
              </li>
            ))}
          </ul>
          <label className="form-field">
            <span className="form-field__label">덧붙일 말 (선택)</span>
            <textarea
              className="textarea sheet__detail"
              rows={2}
              maxLength={REPORT_DETAIL_MAX}
              placeholder="운영자가 판단하는 데 도움이 되는 내용"
              value={detail}
              onChange={(e) => setDetail(e.target.value)}
            />
          </label>
          {error ? (
            <p className="error-text" role="alert">
              {error}
            </p>
          ) : null}
          <div className="sheet__actions">
            <button type="button" className="btn btn--secondary" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn btn--danger" disabled={!reason || sending}>
              {sending ? '보내는 중…' : '신고하기'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
