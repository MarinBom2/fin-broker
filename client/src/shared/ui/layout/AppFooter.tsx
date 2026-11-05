/** 바닥글. 서비스 성격(중개·추천하지 않음)과 참고용 고지를 모든 화면에 둔다 (md/domain-design.md §12) */
export function AppFooter() {
  return (
    <footer className="footer">
      <div className="footer__inner">
        <p>
          fin-broker 는 대출을 중개하거나 특정 업체·상품을 추천하지 않습니다. 업체 정보와 검사 결과는 참고용이며, 거래 전에
          등록 여부와 조건을 직접 확인해 주세요.
        </p>
        <p>피해 상담·신고: 금융감독원 불법사금융 신고센터 1332</p>
      </div>
    </footer>
  )
}
