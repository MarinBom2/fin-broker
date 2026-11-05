# 할 일과 진행 방법

> 기준: domain-design.md(설계) §14 일정 매핑. 사업 범위는 개인사업자(소프트웨어 판매업)로 할 수 있는 것만 (domain-design.md §12).
> 항목마다 "어떻게"에 접근 방법과 끝났다고 볼 기준을 적는다. 끝나면 `[x]`, 범위를 자르면 이유를 남긴다.

## 0. 순서

| 단계 | 할 일 | 관련 절 |
|---|---|---|
| 0 | 모듈 골격, 인증, 문서 형식 | 1 |
| 1 | 사업 요건·규제 확인, 미결 질문 | 2 |
| 2 | 업체 조회, 피해 예방 규칙 | 4 |
| 3 | 커뮤니티 글·답변·신고 | 5 |
| 4 | 업체 담당자 인증, 업체 답변 | 6 |
| 5 | 배포, 일반 광고 슬롯 | 7 |

---

## 1. 골격 (완료)

- [x] Gradle 멀티모듈 `server` / `batch` / `common:core` / `client`
  - 끝 기준: `./gradlew build -PskipClient` 통과, `client` 에서 `npm run build`·`npm run lint` 통과
- [x] 인증: JWT HttpOnly 쿠키, 로그인 필터, ProblemDetail 오류 (authentication-security.md)
  - 끝 기준: `AuthFlowTest`(Testcontainers MySQL) 포함 전 테스트 통과
- [x] 역할 `COMPANY`(등록 업체 담당자). 처음 `BROKER` 로 넣었다가, 플랫폼이 중개하지 않는 방향(A안)으로 정해 이름을 바꿨다
- [x] 문서: solid, develop-principal, authentication-security, runtime-structure, domain-design, todo

## 2. 사업 요건·규제 확인 (사용자)

- [ ] domain-design §12.2 표: 사업자등록 업종 추가, 통신판매업 신고(구독 결제 시), 부가통신사업 신고 면제 여부, 개인정보처리방침
  - 어떻게: 홈택스·관할 세무서, 관할 구청(통신판매업), 과학기술정보통신부 안내. 확인한 내용과 출처를 §12.2 에 적고 `[확인]` 을 지운다
- [ ] domain-design §12.1 "하지 않는 것"이 실제로 등록 없이 피해야 할 범위와 맞는지 (대부업법·금융소비자보호법 원문, 필요하면 전문가)
- [ ] 법정 최고금리 값 확인 (`InterestCapRule` 기준값, domain-design §6)
- [ ] domain-design §13 미결 질문에 답한다
- [ ] 사용자에게 보이는 서비스 이름 (§12.2 서비스 이름)

## 3. 공통 기반

- [ ] SPA 포워딩: `/api` 가 아닌 화면 경로 새로고침을 `index.html` 로 (runtime-structure §6)
  - 끝 기준: `-PwithClient` 로 띄워 `/login` 새로고침이 화면을 연다. `/api/없는경로` 는 계속 404 ProblemDetail
- [ ] 스키마 관리 도구 결정 (Flyway 등). 정하면 prod `ddl-auto: validate`
- [x] 페이지 응답 DTO (`PageResponse`, 1부터) — 업체 조회에서 만들었다
- [ ] CI (GitHub Actions): `./gradlew build -PskipClient` + `npm run build`·`lint`

## 4. 업체 조회와 피해 예방 (`company`, `warning`, domain-design §4.1·§5.2·§6)

- [x] `Company`, `CompanyType`, `RegistrationStatus`, 운영자 등록·대조 API (FR-CO-002), 조회 API (FR-CO-001)
  - 끝 기준: 업체 정보에 금리·한도·홍보 칸이 없다. 목록 순서가 상호순·등록순처럼 돈과 무관하다 (`CompanyFlowTest`)
- [x] `WarningRule` 와 규칙 다섯 개, 검사 API (FR-WN-001)
  - 어떻게: 규칙마다 단위 테스트 (잡아야 할 문장 / 잡으면 안 되는 문장). 금리는 `연`·`월`·`주`·`일` 표기를 연 금리로 환산해 비교
  - [ ] 실제 사기 문자 사례를 모아 규칙 테스트 문장을 늘린다 (놓치는 표현, 잘못 잡는 표현)
- [x] 화면: SCR-05 업체 조회, SCR-06 피해 예방 안내 + 받은 문자 검사
- [ ] 운영자 업체 등록·대조 화면 (SCR-08). 지금은 API·`http/local.http` 로만 한다
- [ ] 업체 등록 정보 대조 출처 정하기 (domain-design §13)

## 5. 커뮤니티 (`community`, `moderation`)

- [ ] 글 작성·목록·상세 (FR-CM-001·002), 답변 (FR-CM-003)
  - 끝 기준: 저장 시 서버가 규칙을 다시 돈다. `BLOCK` 이면 거부, `WARN` 이면 배지
- [ ] 신고·숨김 (FR-MD-001·002), 권리 침해 신고 처리 절차
- [ ] 화면: SCR-02~04

## 6. 업체 담당자 (`company`, domain-design §4.1·§4.3·§5.3)

- [ ] `CompanyMembership` 신청·승인·해제 (FR-CO-003)
  - 어떻게: 불변식 `COMPANY ⇔ APPROVED + 업체 VERIFIED` 를 전이 메서드 한 곳에서 지킨다 (solid.md S)
- [ ] 업체 답변에 업체명·등록번호 자동 표시, 연락처 금지 (`ContactExposureRule` BLOCK)
  - 끝 기준: 담당자 인증이 풀려도 이전 답변의 `authorRole` 스냅샷은 그대로
- [ ] 업체 만료 시 담당자 일괄 해제 + 재확인 배치 (`batch`)
- [ ] 화면: SCR-09

## 7. 배포와 광고

- [ ] 대상 인프라 결정 (AWS EC2 / ECS 등), 도메인, HTTPS
- [ ] 운영 환경변수 (`application-prod.yml` 머리 주석)
- [ ] 헬스체크 `/actuator/health` 를 로드밸런서에 연결
- [ ] 일반 광고 슬롯: 광고 네트워크에서 금융·대출 카테고리 차단, "광고" 표시, 글 목록과 분리 (domain-design §12.3)
