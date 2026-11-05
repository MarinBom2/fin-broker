import { NavLink } from 'react-router-dom'
import { ROUTES } from '../../routes'

/** SCR-08 운영 화면 전환 (신고 처리 / 업체 관리). 모바일에는 상단 메뉴가 없어 이 탭으로 오간다 */
export function OpsNav() {
  const className = ({ isActive }: { isActive: boolean }) =>
    isActive ? 'segmented__item segmented__item--active' : 'segmented__item'
  return (
    <nav className="segmented ops-nav" aria-label="운영 메뉴">
      <NavLink to={ROUTES.SCR08_OPS_REPORTS} className={className}>
        신고 처리
      </NavLink>
      <NavLink to={ROUTES.SCR08_OPS_COMPANIES} className={className}>
        업체 관리
      </NavLink>
    </nav>
  )
}
