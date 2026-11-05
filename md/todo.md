# 할 일과 진행 방법

> 기준: domain-design.md(설계) §14 일정 매핑.
> 항목마다 "어떻게"에 접근 방법과 끝났다고 볼 기준을 적는다. 끝나면 `[x]`, 범위를 자르면 이유를 남긴다.

## 0. 순서

| 단계 | 할 일 | 관련 절 |
|---|---|---|
| 0 | 모듈 골격, 인증, 문서 형식 | 1 |
| 1 | 서비스 범위 결정 (domain-design §13 미결 질문) | 2 |
| 2 | 중개사 등록 신청·확인 | 4 |
| 3 | 커뮤니티 글·답변·신고 | 5 |
| 4 | 상담 요청 | 6 |
| 5 | 배포 | 7 |

---

## 1. 골격 (완료)

- [x] Gradle 멀티모듈 `server` / `batch` / `common:core` / `client`
  - 끝 기준: `./gradlew build -PskipClient` 통과, `client` 에서 `npm run build`·`npm run lint` 통과
- [x] 인증: JWT HttpOnly 쿠키, 로그인 필터, ProblemDetail 오류 (authentication-security.md)
  - 끝 기준: `AuthFlowTest`(Testcontainers MySQL) 포함 전 테스트 통과
- [x] `RoleType.BROKER` 추가
- [x] 문서: solid, develop-principal, authentication-security, runtime-structure, domain-design, todo

## 2. 범위 결정 (사용자)

- [ ] domain-design §13 미결 질문에 답한다 (비로그인 열람, 수익 모델, 등록 확인 자료, 1단계 상품 유형)
- [ ] 중개 해당 여부·광고 규제 원문 확인 (domain-design §12 `[확인]`)
  - 어떻게: 금융소비자보호법·시행령·감독규정 원문, 금융위 유권해석·보도자료. 확인한 내용과 출처를 domain-design §12 에 적고 `[확인]` 을 지운다

## 3. 공통 기반

- [ ] SPA 포워딩: `/api` 가 아닌 화면 경로 새로고침을 `index.html` 로 (runtime-structure §6)
  - 끝 기준: `-PwithClient` 로 띄워 `/login` 새로고침이 화면을 연다. `/api/없는경로` 는 계속 404 ProblemDetail
- [ ] 스키마 관리 도구 결정 (Flyway 등). 정하면 prod `ddl-auto: validate`
- [ ] 페이지 응답 DTO (`PageResponse`) — 첫 목록 API 를 만들 때
- [ ] CI (GitHub Actions): `./gradlew build -PskipClient` + `npm run build`·`lint`

## 4. 중개사 등록 (`broker`, domain-design §4.1·§5.2)

- [ ] 뼈대: `BrokerProfile`, `BrokerStatus`, `BrokerRegistrationType`, `ProductType`, 상태 전이 메서드 시그니처
  - 어떻게: 불변식 `BROKER ⇔ VERIFIED` 를 `BrokerProfile` 전이 메서드 한 곳에서 지킨다 (solid.md S)
- [ ] 신청 API (FR-BR-001), 운영자 승인·반려·정지 API (FR-BR-002)
- [ ] 도메인 테스트: 전이 표의 모든 행, 잘못된 전이 예외
- [ ] 등록증 첨부가 필요하면 `FileStorage` 포트 + `storage/local` (규약 테스트 포함)

## 5. 커뮤니티 (`community`, `moderation`)

- [ ] 글 작성·목록·상세 (FR-CM-001·002), 답변 (FR-CM-003)
  - 끝 기준: 중개사 답변이 `authorRole` 스냅샷으로 구분된다. 정지된 중개사의 이전 답변도 그대로 표시된다
- [ ] 신고·숨김 (FR-MD-001·002)
- [ ] 화면: SCR-02~04
- [ ] 글 작성 화면 개인정보 안내 문구

## 6. 상담 (`consultation`)

- [ ] 요청·수락·거절·종료 (FR-CS-001·002)
- [ ] 무응답 자동 취소 배치 (`batch`)
- [ ] 화면: SCR-06

## 7. 배포

- [ ] 대상 인프라 결정 (AWS EC2 / ECS 등), 도메인, HTTPS
- [ ] 운영 환경변수 (`application-prod.yml` 머리 주석)
- [ ] 헬스체크 `/actuator/health` 를 로드밸런서에 연결
