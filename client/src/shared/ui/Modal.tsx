import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'

/**
 * 화면 위에 뜨는 창. variant 가 alert 면 가운데 작은 창(확인), sheet 면 아래에서 올라오는 시트(데스크톱은 가운데).
 * Esc 와 바깥 누르기로 닫힌다. 열리면 창 안의 첫 입력 요소로 초점을 옮긴다.
 */
export function Modal({
  open,
  onClose,
  labelledBy,
  variant = 'alert',
  children,
}: {
  open: boolean
  onClose: () => void
  labelledBy: string
  variant?: 'alert' | 'sheet'
  children: ReactNode
}) {
  const panelRef = useRef<HTMLDivElement>(null)
  // 부모가 다시 그려져 onClose 가 새로 만들어져도 초점을 다시 옮기지 않도록, 열고 닫을 때만 아래 효과를 돌린다
  const onCloseRef = useRef(onClose)
  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (!open) return
    const previous = document.activeElement as HTMLElement | null
    const focusable = panelRef.current?.querySelector<HTMLElement>('button, [href], input, textarea, select')
    focusable?.focus()
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current()
    }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
      previous?.focus()
    }
  }, [open])

  if (!open) return null

  return (
    <div className="modal" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div ref={panelRef} className={`modal__panel modal__panel--${variant}`} role="dialog" aria-modal="true" aria-labelledby={labelledBy}>
        {children}
      </div>
    </div>
  )
}

/** 지우기처럼 되돌릴 수 없는 일을 하기 전의 확인 창 */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  busy = false,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  message: string
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}) {
  return (
    <Modal open={open} onClose={onCancel} labelledBy="confirm-title">
      <div className="alert">
        <h2 id="confirm-title" className="alert__title">
          {title}
        </h2>
        <p className="alert__message">{message}</p>
      </div>
      <div className="alert__actions">
        <button type="button" className="alert__button" onClick={onCancel}>
          취소
        </button>
        <button type="button" className="alert__button alert__button--destructive" disabled={busy} onClick={onConfirm}>
          {busy ? '잠시만요…' : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
