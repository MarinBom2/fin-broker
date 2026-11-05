import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { safeNext } from '../../routes'
import { authApi } from '../../shared/api/auth/authService'
import { useAuth } from '../../shared/auth/useAuth'
import { ApiError, errorMessage } from '../../shared/core/apiClient'

/** SCR-07 로그인·가입 (FR-AU-001, FR-AU-002) */
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
    <section className="card stack">
      <h1>{isSignup ? '가입' : '로그인'}</h1>
      {expiredReason ? <p className="notice">{expiredReason}</p> : null}
      <form className="stack" onSubmit={handleSubmit}>
        <label className="field">
          <span>이메일</span>
          <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <label className="field">
          <span>비밀번호</span>
          <input
            type="password"
            autoComplete={isSignup ? 'new-password' : 'current-password'}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {isSignup ? <small className="muted">6~20자, 영문 대소문자·숫자·특수문자 중 두 가지 이상</small> : null}
        </label>
        {error ? (
          <p className="error" role="alert">
            {error}
          </p>
        ) : null}
        <button type="submit" className="button" disabled={submitting}>
          {isSignup ? '가입하고 로그인' : '로그인'}
        </button>
      </form>
      <button type="button" className="button button--ghost" onClick={toggleMode}>
        {isSignup ? '이미 계정이 있어요' : '처음이에요, 가입할게요'}
      </button>
    </section>
  )
}
