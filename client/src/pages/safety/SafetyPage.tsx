import { useState } from 'react'
import type { FormEvent } from 'react'
import { warningApi } from '../../shared/api/warning/warningService'
import type { WarningCheckResult } from '../../shared/api/warning/warningService'
import { ApiError, errorMessage } from '../../shared/core/apiClient'

const MAX_LENGTH = 5000

/** 대출 사기에서 자주 보이는 수법. 서버 피해 예방 규칙(md/domain-design.md §6)과 같은 항목이다 */
const SIGNALS = [
  { title: '대출 전에 돈을 먼저 보내라고 한다', body: '수수료·보증금·작업비·전산비를 먼저 보내라는 요구는 대출 사기에서 자주 보입니다.' },
  { title: '통장·체크카드·인증 수단을 달라고 한다', body: '넘긴 통장은 범죄에 쓰일 수 있고, 넘긴 사람도 처벌받을 수 있습니다.' },
  { title: '연 20%를 넘는 금리를 제안한다', body: '법정 최고금리를 넘는 금리는 불법입니다. 월 2%만 돼도 연 24%입니다.' },
  { title: '"신용 작업"으로 등급·한도를 올려 준다고 한다', body: '신용등급을 돈을 받고 올려 주는 방법은 없습니다.' },
  { title: '등록 정보를 확인할 수 없는 업체다', body: '거래 전에 업체 조회에서 등록 여부를 확인해 주세요.' },
]

/** SCR-06 피해 예방 안내 + 받은 문자 검사 (FR-WN-001). 붙여 넣은 내용은 저장하지 않는다 */
export default function SafetyPage() {
  const [body, setBody] = useState('')
  const [result, setResult] = useState<WarningCheckResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [checking, setChecking] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setChecking(true)
    setError(null)
    try {
      setResult(await warningApi.check({ body }))
    } catch (e) {
      const fieldMessage = e instanceof ApiError ? e.fieldErrors[0]?.message : undefined
      setError(fieldMessage ?? errorMessage(e, '검사하지 못했습니다.'))
      setResult(null)
    } finally {
      setChecking(false)
    }
  }

  return (
    <section className="stack">
      <h1>대출 피해 예방</h1>

      <div className="card card--wide stack">
        <h2>받은 문자·제안 검사해 보기</h2>
        <p className="muted">
          업체에게 받은 문자나 제안 내용을 붙여 넣으면 위험 신호를 찾아 드립니다. 붙여 넣은 내용은 저장하지 않습니다.
        </p>
        <form className="stack" onSubmit={handleSubmit}>
          <textarea
            aria-label="검사할 내용"
            rows={6}
            maxLength={MAX_LENGTH}
            required
            placeholder="예: 선입금 30만원 보내 주시면 바로 승인됩니다"
            value={body}
            onChange={(e) => setBody(e.target.value)}
          />
          <button type="submit" className="button" disabled={checking || !body.trim()}>
            {checking ? '검사하는 중…' : '검사하기'}
          </button>
        </form>

        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}

        {result ? (
          result.warnings.length === 0 ? (
            <p className="notice">
              찾은 위험 신호가 없습니다. 다만 모든 수법을 찾을 수는 없으니, 거래 전에 업체 등록 여부를 꼭 확인해 주세요.
            </p>
          ) : (
            <ul className="list" aria-live="polite">
              {result.warnings.map((warning, index) => (
                <li key={`${warning.type.code}-${index}`} className={`warning warning--${warning.type.severity.code.toLowerCase()}`}>
                  <strong>{warning.type.description}</strong>
                  <span className="muted">“{warning.evidence}”</span>
                  <span>{warning.type.guidance}</span>
                </li>
              ))}
            </ul>
          )
        ) : null}
      </div>

      <h2>이런 요구를 받으면 멈추세요</h2>
      <ul className="list">
        {SIGNALS.map((signal) => (
          <li key={signal.title} className="list-item list-item--static">
            <span className="list-item__title">{signal.title}</span>
            <span className="muted">{signal.body}</span>
          </li>
        ))}
      </ul>

      <p className="notice">
        피해를 입었거나 의심되면 금융감독원 불법사금융 신고센터(국번 없이 1332)에 상담·신고해 주세요.
      </p>
    </section>
  )
}
