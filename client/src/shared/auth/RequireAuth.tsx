import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { loginPath } from '../../routes'
import { useAuth } from './useAuth'

/** 로그인이 필요한 화면. 세션 확인 중에는 자리표시만 그리고, 비로그인이면 로그인으로 보낸다 (돌아올 경로 포함) */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'checking') {
    return <div className="skeleton skeleton--list" aria-busy="true" />
  }
  if (status === 'anonymous') {
    return <Navigate to={loginPath(location.pathname + location.search)} replace />
  }
  return children
}
