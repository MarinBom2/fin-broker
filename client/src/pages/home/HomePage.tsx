import { Link } from 'react-router-dom'
import { ROUTES } from '../../routes'
import { BuildingIcon, ChatIcon, ChevronRightIcon, ShieldIcon } from '../../shared/ui/icons/Icons'

/** 멈춰야 할 신호. 서버 피해 예방 규칙(md/domain-design.md §6)과 같은 항목이다 */
const STOP_SIGNALS = [
  { title: '대출 전에 돈을 먼저 보내라고 한다', body: '수수료·보증금·작업비 선입금 요구' },
  { title: '통장·체크카드를 달라고 한다', body: '넘긴 사람도 처벌받을 수 있어요' },
  { title: '연 20%를 넘는 금리를 제안한다', body: '월 2%만 돼도 연 24%예요' },
]

/** SCR-01 홈. 서비스가 무엇인지 한 문장으로 말하고, 업체 조회·문자 검사로 보낸다 */
export default function HomePage() {
  return (
    <>
      <section className="hero">
        <p className="hero__eyebrow">대출 정보 커뮤니티</p>
        <h1 className="hero__title">
          대출 받기 전에,
          <br />
          업체부터 확인하세요.
        </h1>
        <p className="hero__lede">
          등록된 업체인지 확인하고, 받은 제안에 위험 신호가 있는지 바로 검사해 보세요. 대출을 중개하거나 업체를 추천하지
          않습니다.
        </p>
        <div className="cluster hero__actions">
          <Link to={ROUTES.SCR05_COMPANIES} className="btn btn--primary">
            업체 조회하기
          </Link>
          <Link to={ROUTES.SCR06_SAFETY} className="text-link">
            받은 문자 검사하기 <ChevronRightIcon />
          </Link>
        </div>

        <div className="hero__visual" aria-hidden="true">
          <div className="preview">
            <p className="preview__message">선입금 30만원 보내 주시면 월 3%로 바로 승인됩니다</p>
            <div className="preview__results">
              <span className="pill pill--orange">선입금 요구</span>
              <span className="pill pill--orange">법정 최고금리 초과 · 연 36%</span>
            </div>
          </div>
        </div>
      </section>

      <section className="home-section">
        <h2 className="home-section__title">
          거래 전에, 그리고 고민될 때. <span>혼자 판단하지 마세요.</span>
        </h2>
        <div className="grid-3">
          <Link to={ROUTES.SCR05_COMPANIES} className="card card--link">
            <span className="card__icon card__icon--blue">
              <BuildingIcon size={26} />
            </span>
            <h3 className="card__title">업체 조회</h3>
            <p className="card__body">상호나 등록번호로 등록 확인 상태를 봅니다. 순서는 가나다순이고, 돈으로 바뀌지 않아요.</p>
            <span className="card__more">
              조회하기 <ChevronRightIcon />
            </span>
          </Link>
          <Link to={ROUTES.SCR06_SAFETY} className="card card--link">
            <span className="card__icon card__icon--green">
              <ShieldIcon size={26} />
            </span>
            <h3 className="card__title">받은 문자 검사</h3>
            <p className="card__body">선입금·고금리·통장 요구 같은 위험 신호를 찾아 드려요. 붙여 넣은 내용은 저장하지 않아요.</p>
            <span className="card__more">
              검사하기 <ChevronRightIcon />
            </span>
          </Link>
          <Link to={ROUTES.SCR02_POSTS} className="card card--link">
            <span className="card__icon card__icon--orange">
              <ChatIcon size={26} />
            </span>
            <h3 className="card__title">커뮤니티</h3>
            <p className="card__body">질문하고 후기와 피해 사례를 나눠요. 모든 글은 익명이고, 연락처는 올릴 수 없어요.</p>
            <span className="card__more">
              둘러보기 <ChevronRightIcon />
            </span>
          </Link>
        </div>
      </section>

      <section className="home-section">
        <h2 className="home-section__title">
          이런 요구는 멈추세요. <span>대출 사기에서 자주 보여요.</span>
        </h2>
        <ul className="group">
          {STOP_SIGNALS.map((signal, index) => (
            <li key={signal.title}>
              <div className="row row--with-step">
                <span className="step-number">{index + 1}</span>
                <span className="row__main">
                  <span className="row__title">{signal.title}</span>
                  <span className="row__sub">{signal.body}</span>
                </span>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}
