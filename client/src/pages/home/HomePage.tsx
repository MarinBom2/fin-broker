import { useAuth } from '../../shared/auth/useAuth'

/** SCR-01 홈. 커뮤니티·업체 조회·피해 예방 화면이 생기면 여기서 진입한다 (md/domain-design.md §9.1) */
export default function HomePage() {
  const { member } = useAuth()

  return (
    <section className="stack">
      <h1>대출 받기 전에, 업체부터 확인하세요</h1>
      <p className="muted">
        등록된 업체인지 확인하고, 실제 이용 후기를 나누고, 피해 사례를 미리 알아보는 대출 정보 커뮤니티입니다. 이 서비스는 대출을
        중개하거나 업체를 추천하지 않습니다.
      </p>
      <p className="notice">
        대출 전에 돈·통장·체크카드를 요구하거나 연 20%를 넘는 금리를 제안하면 진행을 멈추고 신고해 주세요.
      </p>
      {member ? (
        <p>
          {member.email} 님은 <strong>{member.roleType.description}</strong> 권한으로 로그인했습니다.
        </p>
      ) : null}
    </section>
  )
}
