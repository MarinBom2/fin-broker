import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { safeNext } from '../../routes'
import { authApi } from '../../shared/api/auth/authService'
import { useAuth } from '../../shared/auth/useAuth'
import { ApiError, errorMessage } from '../../shared/core/apiClient'
import { BrandMark, InfoIcon } from '../../shared/ui/icons/Icons'

/** SCR-07 로그인·가입 (FR-AU-001, FR-AU-002). 칸 두 개를 한 덩어리로 묶은 입력 */
export default function LoginPage() {
  const [search, setSearch] = useSearchParams()
  const navigate = useNavigate()
  const { login, expiredReason } = useAuth()

  const isSignup = search.get('mode') === 'signup'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      if (isSignup) {
        await authApi.signup({ email, password })
      }
      await login({ email, password })
      navigate(safeNext(search.get('next')), { replace: true })
    } catch (e) {
      const fieldMessage = e instanceof ApiError ? e.fieldErrors[0]?.message : undefined
      setError(fieldMessage ?? errorMessage(e, '요청을 처리하지 못했습니다.'))
    } finally {
      setSubmitting(false)
    }
  }

  function toggleMode() {
    const next = new URLSearchParams(search)
    if (isSignup) next.delete('mode')
    else next.set('mode', 'signup')
    setSearch(next, { replace: true })
    setError(null)
  }

  return (
    <section className="auth">
      <span className="auth__mark" aria-hidden="true">
        <BrandMark size={30} />
      </span>
      <h1 className="auth__title">{isSignup ? 'fin-broker 가입' : 'fin-broker 로그인'}</h1>
      <p className="auth__lede">
        {isSignup ? '이메일과 비밀번호만 있으면 바로 시작할 수 있어요.' : '업체 조회와 문자 검사는 로그인 후 쓸 수 있어요.'}
      </p>

      {expiredReason ? (
        <div className="callout callout--orange">
          <InfoIcon className="callout__icon" />
          {expiredReason}
        </div>
      ) : null}

      <form className="stack" onSubmit={handleSubmit}>
        <div className="field-group">
          <label className="field-group__item">
            <span className="field-group__label">이메일</span>
            <input
              className="field-group__input"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label className="field-group__item">
            <span className="field-group__label">비밀번호</span>
            <input
              className="field-group__input"
              type="password"
              autoComplete={isSignup ? 'new-password' : 'current-password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
        </div>
        {isSignup ? <p className="caption">비밀번호는 6~20자, 영문 대소문자·숫자·특수문자 중 두 가지 이상</p> : null}
        {error ? (
          <p className="error-text" role="alert">
            {error}
          </p>
        ) : null}
        <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
          {submitting ? '잠시만요…' : isSignup ? '가입하고 시작하기' : '로그인'}
        </button>
      </form>

      <p className="auth__switch">
        {isSignup ? '이미 계정이 있나요?' : '처음이신가요?'}{' '}
        <button type="button" className="text-link" onClick={toggleMode}>
          {isSignup ? '로그인' : '가입하기'}
        </button>
      </p>
    </section>
  )
}
