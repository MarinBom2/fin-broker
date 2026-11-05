import type { ReactNode } from 'react'

/** 화면 머리: 작은 분류 + 큰 제목 + 한두 줄 설명 */
export function PageHead({ eyebrow, title, lede }: { eyebrow?: string; title: string; lede?: ReactNode }) {
  return (
    <header className="page-head">
      {eyebrow ? <p className="page-head__eyebrow">{eyebrow}</p> : null}
      <h1 className="page-head__title">{title}</h1>
      {lede ? <p className="page-head__lede">{lede}</p> : null}
    </header>
  )
}
