import { Link, NavLink } from 'react-router-dom'
import { ROUTES } from '../../../routes'
import { useAuth } from '../../auth/useAuth'

export function AppHeader() {
  const { status, member, logout } = useAuth()

  return (
    <header className="app-header">
      <Link to={ROUTES.SCR01_HOME} className="brand">
        fin-broker
      </Link>
      <nav className="app-header__menu" aria-label="주 메뉴">
        <NavLink to={ROUTES.SCR05_COMPANIES}>업체 조회</NavLink>
        <NavLink to={ROUTES.SCR06_SAFETY}>피해 예방</NavLink>
      </nav>
      <div className="app-header__nav">
        {status === 'authenticated' && member ? (
          <>
            <span className="badge">{member.roleType.description}</span>
            <button type="button" className="button button--ghost" onClick={() => void logout()}>
              로그아웃
            </button>
          </>
        ) : status === 'anonymous' ? (
          <Link to={ROUTES.SCR07_LOGIN} className="button">
            로그인
          </Link>
        ) : null}
      </div>
    </header>
  )
}
