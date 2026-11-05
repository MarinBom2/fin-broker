import { useAuth } from '../../shared/auth/useAuth'

/** SCR-01 홈. 커뮤니티·상품·상담 화면이 생기면 여기서 진입한다 (md/domain-design.md §9.1) */
export default function HomePage() {
  const { member } = useAuth()

  return (
    <section className="stack">
      <h1>금융상품, 묻고 비교하고 상담받는 곳</h1>
      <p className="muted">
        등록된 판매대리·중개업자와 금융소비자가 함께 쓰는 커뮤니티입니다. 상품 정보와 답변은 참고용이며, 가입 전에는 상품설명서와
        약관을 꼭 확인해 주세요.
      </p>
      {member ? (
        <p>
          {member.email} 님은 <strong>{member.roleType.description}</strong> 권한으로 로그인했습니다.
        </p>
      ) : null}
    </section>
  )
}
