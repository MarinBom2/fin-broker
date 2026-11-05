import type { WarningItem } from '../api/warning/warningService'
import { BlockIcon, ExclamationIcon } from './icons/Icons'

/** 위험 신호 카드 목록. 피해 예방 검사, 글쓰기 거부(CONTENT_BLOCKED)에서 같이 쓴다 */
export function WarningList({ warnings }: { warnings: WarningItem[] }) {
  return (
    <div className="stack stack--tight">
      {warnings.map((warning, index) => {
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
    </div>
  )
}
