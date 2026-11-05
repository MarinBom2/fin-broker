import { Link } from 'react-router-dom'
import { ROUTES } from '../../../routes'
import { useAuth } from '../../auth/useAuth'

export function AppHeader() {
  const { status, member, logout } = useAuth()

  return (
    <header className="app-header">
      <Link to={ROUTES.SCR01_HOME} className="brand">
        fin-broker
      </Link>
      <nav className="app-header__nav">
        {status === 'authenticated' && member ? (
          <>
            <span className="badge">{member.roleType.description}</span>
            <span className="muted">{member.email}</span>
            <button type="button" className="button button--ghost" onClick={() => void logout()}>
              로그아웃
            </button>
          </>
        ) : status === 'anonymous' ? (
          <Link to={ROUTES.SCR07_LOGIN} className="button">
            로그인
          </Link>
        ) : null}
      </nav>
    </header>
  )
}
