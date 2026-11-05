import { Link, NavLink } from 'react-router-dom'
import { ROUTES } from '../../../routes'
import { isAdmin } from '../../auth/roles'
import { useAuth } from '../../auth/useAuth'
import { BrandMark } from '../icons/Icons'

/** 반투명 상단 바. 모바일에서는 메뉴를 숨기고 하단 탭 바(TabBar)가 대신한다 */
export function AppHeader() {
  const { status, member, logout } = useAuth()

  return (
    <header className="nav">
      <div className="nav__inner">
        <Link to={ROUTES.SCR01_HOME} className="nav__brand">
          <span className="nav__brand-mark">
            <BrandMark />
          </span>
          fin-broker
        </Link>
        <nav className="nav__links" aria-label="주 메뉴">
          <NavLink to={ROUTES.SCR02_POSTS} className="nav__link">
            커뮤니티
          </NavLink>
          <NavLink to={ROUTES.SCR05_COMPANIES} className="nav__link">
            업체 조회
          </NavLink>
          <NavLink to={ROUTES.SCR06_SAFETY} className="nav__link">
            피해 예방
          </NavLink>
          {isAdmin(member) ? (
            <NavLink to={ROUTES.SCR08_OPS_REPORTS} className="nav__link">
              신고 처리
            </NavLink>
          ) : null}
        </nav>
        <div className="nav__account">
          {status === 'authenticated' && member ? (
            <>
              <span className="pill pill--gray pill--plain">{member.roleType.description}</span>
              <button type="button" className="text-link nav__logout" onClick={() => void logout()}>
                로그아웃
              </button>
            </>
          ) : status === 'anonymous' ? (
            <Link to={ROUTES.SCR07_LOGIN} className="btn btn--primary btn--small">
              로그인
            </Link>
          ) : null}
        </div>
      </div>
    </header>
  )
}
