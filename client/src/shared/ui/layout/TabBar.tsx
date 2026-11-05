import { NavLink } from 'react-router-dom'
import { ROUTES } from '../../../routes'
import { BuildingIcon, HomeIcon, ShieldIcon } from '../icons/Icons'

/** 모바일 하단 탭 바 (734px 이하에서만 보인다, layout.css) */
export function TabBar() {
  return (
    <nav className="tabbar" aria-label="탭">
      <NavLink to={ROUTES.SCR01_HOME} end className="tabbar__item">
        <HomeIcon size={24} />
        홈
      </NavLink>
      <NavLink to={ROUTES.SCR05_COMPANIES} className="tabbar__item">
        <BuildingIcon size={24} />
        업체 조회
      </NavLink>
      <NavLink to={ROUTES.SCR06_SAFETY} className="tabbar__item">
        <ShieldIcon size={24} />
        피해 예방
      </NavLink>
    </nav>
  )
}
