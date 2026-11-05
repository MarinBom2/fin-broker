import { useState } from 'react'
import type { FormEvent } from 'react'
import { warningApi } from '../../shared/api/warning/warningService'
import type { WarningCheckResult } from '../../shared/api/warning/warningService'
import { ApiError, errorMessage } from '../../shared/core/apiClient'
import { BlockIcon, CheckCircleIcon, ExclamationIcon, PhoneIcon } from '../../shared/ui/icons/Icons'
import { PageHead } from '../../shared/ui/PageHead'

const MAX_LENGTH = 5000

/** 대출 사기에서 자주 보이는 수법. 서버 피해 예방 규칙(md/domain-design.md §6)과 같은 항목이다 */
const SIGNALS = [
  { title: '대출 전에 돈을 먼저 보내라고 한다', body: '수수료·보증금·작업비·전산비를 먼저 보내라는 요구는 대출 사기에서 자주 보여요.' },
  { title: '통장·체크카드·인증 수단을 달라고 한다', body: '넘긴 통장은 범죄에 쓰일 수 있고, 넘긴 사람도 처벌받을 수 있어요.' },
  { title: '연 20%를 넘는 금리를 제안한다', body: '법정 최고금리를 넘는 금리예요. 월 2%만 돼도 연 24%예요.' },
  { title: '"신용 작업"으로 등급을 올려 준다고 한다', body: '돈을 받고 신용등급을 올려 주는 방법은 없어요.' },
  { title: '등록 정보를 확인할 수 없는 업체다', body: '거래 전에 업체 조회에서 등록 여부를 확인하세요.' },
]

/** SCR-06 피해 예방: 받은 문자 검사 (FR-WN-001) + 대표 수법 + 신고 창구. 붙여 넣은 내용은 저장하지 않는다 */
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
    <>
      <PageHead
        eyebrow="피해 예방"
        title="받은 문자, 보내기 전에 검사하세요."
        lede="업체에게 받은 문자나 제안을 붙여 넣으면 위험 신호를 찾아 드려요. 붙여 넣은 내용은 저장하지 않아요."
      />

      <form className="card stack" onSubmit={handleSubmit}>
        <label htmlFor="check-body" className="visually-hidden">
          검사할 내용
        </label>
        <textarea
          id="check-body"
          className="textarea"
          rows={5}
          maxLength={MAX_LENGTH}
          required
          placeholder="예: 선입금 30만원 보내 주시면 바로 승인됩니다"
          value={body}
          onChange={(e) => setBody(e.target.value)}
        />
        <div className="cluster space-between">
          <span className="caption">
            {body.length.toLocaleString('ko-KR')} / {MAX_LENGTH.toLocaleString('ko-KR')}자
          </span>
          <button type="submit" className="btn btn--primary" disabled={checking || !body.trim()}>
            {checking ? '검사하는 중…' : '검사하기'}
          </button>
        </div>
        {error ? (
          <p className="error-text" role="alert">
            {error}
          </p>
        ) : null}
      </form>

      {result ? (
        <section className="stack mt-l" aria-live="polite">
          {result.warnings.length === 0 ? (
            <div className="callout callout--green">
              <CheckCircleIcon className="callout__icon" />
              <span>
                찾은 위험 신호가 없어요. 다만 모든 수법을 찾을 수는 없으니, 거래 전에 업체 등록 여부를 꼭 확인하세요.
              </span>
            </div>
          ) : (
            <>
              <p className="summary">
                <ExclamationIcon size={26} className="summary__icon" />
                위험 신호 {result.warnings.length}개를 찾았어요.
              </p>
              {result.warnings.map((warning, index) => {
                const blocked = warning.type.severity.code === 'BLOCK'
                return (
                  <article key={`${warning.type.code}-${index}`} className={`signal ${blocked ? 'signal--block' : 'signal--warn'}`}>
                    <span className="signal__icon">{blocked ? <BlockIcon /> : <ExclamationIcon />}</span>
                    <div className="signal__body">
                      <div className="signal__head">
                        <h3 className="signal__title">{warning.type.description}</h3>
                        <span className={`pill pill--plain ${blocked ? 'pill--red' : 'pill--gray'}`}>
                          {warning.type.severity.description}
                        </span>
                      </div>
                      <p className="signal__evidence">“{warning.evidence}”</p>
                      <p className="signal__guide">{warning.type.guidance}</p>
                    </div>
                  </article>
                )
              })}
            </>
          )}
        </section>
      ) : null}

      <h2 className="section-title">이런 요구를 받으면 멈추세요.</h2>
      <ul className="group">
        {SIGNALS.map((signal, index) => (
          <li key={signal.title} className="row row--with-step">
            <span className="step-number">{index + 1}</span>
            <span className="row__main">
              <span className="row__title">{signal.title}</span>
              <span className="row__sub">{signal.body}</span>
            </span>
          </li>
        ))}
      </ul>

      <div className="callout callout--blue mt-m">
        <PhoneIcon className="callout__icon" />
        <span>
          피해를 입었거나 의심되면 <strong>금융감독원 불법사금융 신고센터 1332</strong>(국번 없이)에 상담·신고하세요.
        </span>
      </div>
    </>
  )
}
