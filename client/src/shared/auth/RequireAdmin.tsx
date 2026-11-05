import type { ReactNode } from 'react'
import { isAdmin } from './roles'
import { RequireAuth } from './RequireAuth'
import { useAuth } from './useAuth'

/** 운영자 화면 (SCR-08). 로그인은 RequireAuth 가, 권한은 서버가 최종 판정한다. 화면은 안내만 한다 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  return (
    <RequireAuth>
      <AdminOnly>{children}</AdminOnly>
    </RequireAuth>
  )
}

function AdminOnly({ children }: { children: ReactNode }) {
  const { member } = useAuth()
  if (!isAdmin(member)) {
    return (
      <p className="error-text" role="alert">
        운영자만 볼 수 있는 화면이에요.
      </p>
    )
  }
  return children
}
