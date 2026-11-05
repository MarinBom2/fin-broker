import { Link } from 'react-router-dom'
import { ROUTES } from '../../routes'

/** SCR-01 홈. 업체 조회와 피해 예방으로 들어간다 (md/domain-design.md §9.1) */
export default function HomePage() {
  return (
    <section className="stack">
      <h1>대출 받기 전에, 업체부터 확인하세요</h1>
      <p className="muted">
        등록된 업체인지 확인하고, 받은 제안에 위험 신호가 있는지 검사하는 대출 정보 커뮤니티입니다. 이 서비스는 대출을 중개하거나
        업체를 추천하지 않습니다.
      </p>
      <div className="tiles">
        <Link to={ROUTES.SCR05_COMPANIES} className="tile">
          <strong>업체 조회</strong>
          <span className="muted">상호나 등록번호로 등록 확인 상태를 봅니다</span>
        </Link>
        <Link to={ROUTES.SCR06_SAFETY} className="tile">
          <strong>받은 문자 검사</strong>
          <span className="muted">선입금·고금리·통장 요구 같은 위험 신호를 찾습니다</span>
        </Link>
      </div>
      <p className="notice">
        대출 전에 돈·통장·체크카드를 요구하거나 연 20%를 넘는 금리를 제안하면 진행을 멈추고 신고해 주세요.
      </p>
    </section>
  )
}
