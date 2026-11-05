# fin-broker 도메인 설계 — 금융상품 판매대리·중개 커뮤니티

> 상태: **초안**. 서비스 방향(금융상품 판매업 + 중개 커뮤니티)을 도메인 모델·상태·데이터·API 로 구체화한다. 구현된 것은 회원·인증(§5.1)뿐이고, 나머지는 리뷰 후 뼈대부터 채운다 (develop-principal.md §9).
> 관련 문서: [develop-principal.md](./develop-principal.md), [authentication-security.md](./authentication-security.md), [solid.md](./solid.md), [runtime-structure.md](./runtime-structure.md), [todo.md](./todo.md)
> 모르는 값은 `[ ]`로 비워 둔다. 법령·감독 규정에 기대는 내용은 `[확인]`을 붙인 초안이고, 원문(금융소비자보호법·시행령·감독규정, 금융위 유권해석)을 확인한 뒤 확정한다. 이 문서는 법률 자문이 아니다.

## 1. 한 줄 정의와 범위

**금융소비자가 금융상품에 대해 묻고, 등록된 판매대리·중개업자가 답하고, 원하면 상담으로 이어지는 커뮤니티.**
플랫폼은 상품을 직접 팔지 않는다. 질문·답변·상담 연결의 장을 만들고, 답하는 사람이 **실제로 등록된 중개업자인지**를 확인해 보여 주는 것이 핵심 가치다.

| 구분 | 1단계 (MVP) | 다음 단계 |
|---|---|---|
| 커뮤니티 | 상품 유형별 질문·후기·정보 글, 답변·댓글 | 검색, 추천 답변, 태그 |
| 중개사 | 등록번호 제출 → 운영자 확인 → `BROKER` 권한, 프로필에 "등록 확인" 표시 | 등록 정보 정기 재확인(배치), 공시 조회 연동 |
| 상담 | 소비자 → 중개사 상담 요청, 수락·거절·종료 | 상담 메시지, 일정, 후기 |
| 상품 | 상품 유형 코드만 (예금성·대출성·투자성·보장성) | 상품 카탈로그, 비교 (중개 등록 범위 확인 뒤) |
| 운영 | 신고 처리, 중개사 등록 승인, 글 숨김 | 광고성·부당권유 표현 자동 검수 (LLM) |

**이 설계가 지키는 원칙**
- 플랫폼은 **판매·권유 주체가 아니다.** 특정 상품을 골라 주거나 가입으로 바로 이어 주는 기능은 중개 등록 범위를 확인한 뒤에 만든다 (§12). `[확인]`
- 답변자가 중개사인지는 **등록 확인을 마친 경우에만** 표시한다. 자기 소개 문구로 표시하지 않는다.
- 중개사가 쓰는 글·답변은 언제나 "판매대리·중개업자 작성"으로 구분해 보인다. 광고와 정보를 소비자가 구분할 수 있어야 한다. `[확인]`
- 소득·자산·부채 같은 민감한 금융 정보는 **커뮤니티 글에 쓰지 않도록 안내**하고, 상담 요청에서도 최소 항목만 받는다.
- 화면 문구에 "원금 보장", "확정 수익", "무조건 승인", "최저 금리 보장" 같은 단정 표현을 쓰지 않는다 (§12 금지 표현).

---

## 2. 용어 사전

| 용어 | 코드 이름 | 뜻 |
|---|---|---|
| 금융소비자 | `RoleType.USER` | 가입한 일반 회원. 질문·후기를 쓰고 상담을 요청한다 |
| 판매대리·중개업자 (중개사) | `RoleType.BROKER` | 금융상품직접판매업자를 대리하거나 계약 체결을 중개하는 자. 이 서비스에서는 등록 확인을 마친 회원 `[확인]` |
| 직접판매업자 | `affiliatedCompany` | 상품을 만드는 금융회사(은행·보험사·카드사 등). 중개사의 소속·위탁 회사 |
| 금융상품 유형 | `ProductType` | 금융소비자보호법의 4분류: 예금성, 대출성, 투자성, 보장성 `[확인]` |
| 중개사 등록 확인 | `BrokerVerification` | 등록번호·등록 기관·소속을 운영자가 확인한 기록 |
| 글 | `Post` | 커뮤니티 게시물. 질문·후기·정보 |
| 답변 | `Answer` | 글에 단 응답. 중개사 답변은 따로 표시 |
| 상담 요청 | `Consultation` | 소비자가 특정 중개사에게 보내는 1:1 상담 요청 |
| 신고 | `Report` | 광고성·부당권유·개인정보 노출 글을 운영자에게 알림 |

---

## 3. 컨텍스트와 확장점

```
member ── 회원·인증 (구현 완료)
  │
broker ── 중개사 프로필, 등록 확인 ──(승인 시 Member.role = BROKER)
  │
community ── 글, 답변 ──(중개사 답변 표시는 broker 의 확인 상태를 본다)
  │
consultation ── 상담 요청 (소비자 → 확인된 중개사)
  │
moderation ── 신고, 숨김 ──(운영자)
  │
ops ── 운영자 집계 (/api/v1/ops/**)
```

**확장점 (인터페이스 하나 + 빈 하나, solid.md O)**

| 포트 | 소유 | 1단계 구현 | 다음 구현 | 상태 |
|---|---|---|---|---|
| `FileStorage` | `broker.domain` | `storage/local` (로컬 디스크) | S3 | 아직 코드 없음. 등록증 사본 첨부가 필요해질 때 |
| `LlmClient` | `moderation.domain` | `llm/springai` (Spring AI) | 공급자 추가는 `LlmProvider` 값 + 구현체 + 조건부 빈 | 아직 코드 없음. 광고성·부당권유 표현 검수 |
| `BrokerRegistryClient` | `broker.domain` | 없음 (운영자 수동 확인) | 등록 정보 조회 연동 `[확인: 공개 API 유무]` | 아직 코드 없음 |

포트는 실제로 호출하는 기능이 생길 때 만든다. 위 표는 자리를 정해 둔 것이다 (solid.md I).

---

## 4. 흐름과 상태

### 4.1 중개사 등록 확인

```
BROKER 신청 (USER 회원)
  PENDING ──운영자 승인──> VERIFIED ──(정지)──> SUSPENDED
     │                       │
     └──운영자 반려──> REJECTED └──(재확인 실패)──> SUSPENDED
```

| 전이 | 누가 | 같이 바뀌는 것 (한 트랜잭션, acid 원칙) |
|---|---|---|
| 신청 → `PENDING` | 본인 (USER) | 없음 |
| `PENDING` → `VERIFIED` | ADMIN | `Member.roleType = BROKER`, `verifiedAt` |
| `PENDING` → `REJECTED` | ADMIN | 반려 사유 |
| `VERIFIED` → `SUSPENDED` | ADMIN / 배치 재확인 | `Member.roleType = USER`, 그 회원의 열린 상담은 `CLOSED` |

**불변식**: `Member.roleType == BROKER` ⇔ 그 회원의 `BrokerProfile.status == VERIFIED`. 둘을 따로 바꾸는 코드를 만들지 않는다. 바꾸는 곳은 `BrokerProfile` 의 상태 전이 메서드 + 이를 부르는 서비스 하나다 (solid.md S).

### 4.2 상담 요청

```
REQUESTED ──중개사 수락──> ACCEPTED ──종료──> CLOSED
    │
    ├──중개사 거절──> DECLINED
    └──소비자 취소 / [ ]일 무응답(배치)──> CANCELED
```

- 상담 요청은 `VERIFIED` 중개사에게만 보낼 수 있다.
- 소비자가 상담에 넘기는 정보는 요청 시 고른 항목만이다. 동의 시각을 남긴다 `[확인: 개인정보 제3자 제공 동의 형식]`

### 4.3 글과 신고

```
Post: PUBLISHED ──신고 누적 / 운영자──> HIDDEN ──운영자 복구──> PUBLISHED
                └──작성자 삭제──> DELETED (내용 비움, 행은 남김)
Report: OPEN ──운영자──> ACCEPTED(글 숨김) | DISMISSED
```

---

## 5. 애그리거트

### 5.1 Member (구현 완료)

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | Long | |
| `email` | String(100) | 로그인 ID. 소문자·공백 제거로 정규화, 유니크 |
| `password` | String(100) | `{bcrypt}` 해시 |
| `roleType` | `RoleType` | `USER`, `BROKER`, `ADMIN`, `SYSTEM` |
| `tokenVersion` | int | 로그아웃 시 +1. 이전 토큰 무효 (authentication-security.md §5) |

### 5.2 BrokerProfile

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | Long | |
| `memberId` | Long | 회원당 하나 (유니크) |
| `registrationType` | `BrokerRegistrationType` | 보험설계사·대리점, 대출모집인, 투자권유대행인 등 `[확인: 분류]` |
| `registrationNumber` | String | 등록번호. 화면에는 일부만 보인다 |
| `affiliatedCompany` | String | 소속·위탁 직접판매업자 |
| `productTypes` | Set<`ProductType`> | 취급 상품 유형 |
| `status` | `BrokerStatus` | §4.1 |
| `verifiedAt`, `verifiedBy` | Instant, Long | 승인 시각·운영자 |
| `rejectReason` | String | 반려 시 |

### 5.3 Post

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | Long | |
| `authorId` | Long | |
| `authorRole` | `RoleType` | **작성 시점 스냅샷.** 중개사가 정지돼도 당시 "중개사 작성"이었다는 사실은 남긴다 |
| `productType` | `ProductType` | 게시판 구분 |
| `postType` | `PostType` | `QUESTION`(질문), `REVIEW`(후기), `INFO`(정보) |
| `title`, `body` | String | |
| `status` | `PostStatus` | §4.3 |
| `answerCount` | int | 목록 표시용. 답변 추가·삭제와 같은 트랜잭션에서 바꾼다 |
| `version` | Long | `@Version`. 답변 수 갱신 lost update 방지 |

### 5.4 Answer

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `postId`, `authorId` | Long | |
| `authorRole` | `RoleType` | 작성 시점 스냅샷. `BROKER` 면 "판매대리·중개업자 답변" 표시 |
| `body` | String | |
| `status` | `PostStatus` | 글과 같은 상태값 |

### 5.5 Consultation

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `consumerId`, `brokerId` | Long | `brokerId` 는 `VERIFIED` 중개사 |
| `productType` | `ProductType` | |
| `message` | String | 소비자가 쓴 첫 메시지 |
| `contactConsentAt` | Instant | 연락처 제공 동의 시각 |
| `status` | `ConsultationStatus` | §4.2 |
| `requestedAt`, `respondedAt`, `closedAt` | Instant | |

### 5.6 Report

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `reporterId` | Long | |
| `targetType`, `targetId` | `ReportTarget`, Long | 글 / 답변 |
| `reason` | `ReportReason` | 광고성, 부당권유, 개인정보 노출, 욕설, 기타 |
| `status` | `ReportStatus` | §4.3 |

모든 enum 은 `DescriptionCode` 를 구현한다 (develop-principal.md §1).

---

## 6. 상품 유형 기준

| `ProductType` | 설명 (화면 문구) | 예 | 비고 |
|---|---|---|---|
| `DEPOSIT` | 예금성 | 예금, 적금 | `[확인]` |
| `LOAN` | 대출성 | 신용대출, 주택담보대출, 카드론 | 대출모집인 등록 확인 대상 `[확인]` |
| `INVESTMENT` | 투자성 | 펀드, 신탁 | 투자권유대행인 `[확인]` |
| `INSURANCE` | 보장성 | 보험 | 보험설계사·대리점 `[확인]` |

---

## 7. 외부 연동 계약 (자리만)

| 연동 | 포트 | 입력 | 출력 | 규약 |
|---|---|---|---|---|
| 글 검수 | `LlmClient` | 글 본문 (개인정보 마스킹 후) | 표현 유형 목록 + 근거 문장 | 판정은 "검토 필요" 표시까지만. 숨김은 운영자가 한다 |
| 파일 저장 | `FileStorage` | 등록증 사본 | 저장 키 | 없는 파일 읽기는 예외가 아니라 empty (solid.md L) |

---

## 8. 데이터 모델

| 테이블 | 주요 칼럼 | 제약 |
|---|---|---|
| `member` | id, email, password, role_type, token_version, created_at, modified_at | uk(email) |
| `broker_profile` | id, member_id, registration_type, registration_number, affiliated_company, status, verified_at, verified_by | uk(member_id), uk(registration_type, registration_number) |
| `broker_product_type` | broker_profile_id, product_type | pk(둘) |
| `post` | id, author_id, author_role, product_type, post_type, title, body, status, answer_count, version | idx(product_type, status, created_at) |
| `answer` | id, post_id, author_id, author_role, body, status | idx(post_id, created_at) |
| `consultation` | id, consumer_id, broker_id, product_type, message, contact_consent_at, status | idx(broker_id, status) |
| `report` | id, reporter_id, target_type, target_id, reason, status | uk(reporter_id, target_type, target_id) |

enum 칼럼은 varchar (`NoColumnCheckMySQLDialect`, develop-principal.md §5).

---

## 9. 화면과 요구사항 ID

### 9.1 화면 (SCR)

| ID | 화면 | 경로 (client `routes.ts`) | 상태 |
|---|---|---|---|
| SCR-01 | 홈 | `/` | 뼈대 |
| SCR-02 | 게시판 목록 (상품 유형별) | `/posts?productType=` | |
| SCR-03 | 글 상세 + 답변 | `/posts/:postId` | |
| SCR-04 | 글쓰기 | `/posts/new` | |
| SCR-05 | 중개사 프로필 | `/brokers/:brokerId` | |
| SCR-06 | 상담 요청함 (소비자·중개사) | `/consultations` | |
| SCR-07 | 로그인·가입 | `/login` | 구현 |
| SCR-08 | 운영자 (등록 승인, 신고) | `/ops` | |
| SCR-09 | 중개사 등록 신청 | `/broker/apply` | |

### 9.2 요구사항 (FR)

| ID | 기능 | 상태 |
|---|---|---|
| FR-AU-001 | 이메일·비밀번호 가입 | 구현 |
| FR-AU-002 | 로그인·로그아웃·내 정보 (JWT HttpOnly 쿠키) | 구현 |
| FR-AU-003 | 역할 기반 인가 (`USER`, `BROKER`, `ADMIN`, `SYSTEM`) | 구현 (운영자 경로) |
| FR-BR-001 | 중개사 등록 신청 | |
| FR-BR-002 | 운영자 등록 확인 (승인·반려·정지) | |
| FR-BR-003 | 중개사 프로필 조회 ("등록 확인" 표시) | |
| FR-CM-001 | 글 작성·수정·삭제 | |
| FR-CM-002 | 상품 유형별 글 목록 | |
| FR-CM-003 | 답변 작성 (중개사 답변 구분 표시) | |
| FR-CS-001 | 상담 요청 (확인된 중개사에게만) | |
| FR-CS-002 | 상담 수락·거절·종료 | |
| FR-MD-001 | 글·답변 신고 | |
| FR-MD-002 | 운영자 신고 처리·숨김 | |

---

## 10. API 초안

| 메서드 | 경로 | 권한 | FR |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 없음 | FR-AU-001 |
| POST | `/api/v1/auth/login` | 없음 (필터) | FR-AU-002 |
| POST | `/api/v1/auth/logout` | 없음 | FR-AU-002 |
| GET | `/api/v1/me` | 로그인 | FR-AU-002 |
| POST | `/api/v1/broker-applications` | USER | FR-BR-001 |
| GET | `/api/v1/brokers/{brokerId}` | 로그인 `[확인: 비로그인 공개 여부]` | FR-BR-003 |
| GET | `/api/v1/posts?productType=&postType=&page=` | 로그인 `[확인]` | FR-CM-002 |
| POST | `/api/v1/posts` | 로그인 | FR-CM-001 |
| GET | `/api/v1/posts/{postId}` | 로그인 | FR-CM-002 |
| POST | `/api/v1/posts/{postId}/answers` | 로그인 | FR-CM-003 |
| POST | `/api/v1/consultations` | USER | FR-CS-001 |
| PATCH | `/api/v1/consultations/{id}` (`{status}`) | 상담의 중개사·소비자 | FR-CS-002 |
| POST | `/api/v1/reports` | 로그인 | FR-MD-001 |
| GET·PATCH | `/api/v1/ops/broker-applications/**` | ADMIN, SYSTEM | FR-BR-002 |
| GET·PATCH | `/api/v1/ops/reports/**` | ADMIN, SYSTEM | FR-MD-002 |

비로그인 공개 경로가 생기면 `SecurityConfig` 의 허용 목록에 GET 경로를 더한다 (authentication-security.md §9).

---

## 11. 패키지 구조

```
com.marinbom2.finbroker
 +- config, exception, type.common, util, domain.base   (core 공통)
 +- member     domain(core) / application, api(server)   ← 구현
 +- broker     domain(core) / application, api(server)
 +- community  domain(core) / application, api(server)
 +- consultation domain(core) / application, api(server)
 +- moderation domain(core) / application, api(server)
 +- ops        api(server)   집계 조회 전용
 +- security   (server)      ← 구현
 +- storage/local, llm/springai   어댑터 (core). 기술 타입은 이 안에서만
 +- batch      (batch 모듈)   상담 무응답 취소, 중개사 등록 재확인

의존 방향: api -> application -> domain <- (storage, llm 어댑터)
```

---

## 12. 비기능·규제·개인정보

| 항목 | 정한 것 | 확인할 것 |
|---|---|---|
| 중개 해당 여부 | 1단계는 질문·답변·상담 연결까지. 상품 추천·비교·가입 연결은 하지 않는다 | 상담 연결에 수수료를 받거나 특정 상품을 노출하면 플랫폼이 판매대리·중개업 등록 대상이 되는지 `[확인: 금융위 유권해석]` |
| 광고 | 중개사 글·답변에 작성자 구분 표시. 광고성 글은 신고 사유 | 중개사 글이 금융상품 광고에 해당할 때 필요한 표시·심의 `[확인: 금소법 광고 규제]` |
| 금지 표현 | 화면 문구·enum `description` 에 "원금 보장", "확정 수익", "무조건", "보장" 단정 표현을 쓰지 않는다 | 검수 규칙 목록 |
| 개인정보 | 글 작성 화면에 "소득·계좌·주민번호를 쓰지 마세요" 안내. 상담은 최소 항목 + 동의 시각 | 상담 정보 보관 기간 `[ ]` |
| 등록번호 | 화면에는 일부만, 원문은 운영자만 | 마스킹 규칙 `[ ]` |
| 인증·보안 | JWT HttpOnly 쿠키, SameSite=Lax, 권한은 요청마다 DB | authentication-security.md §12 |

---

## 13. 미결 질문

- [ ] 비로그인 사용자도 글을 읽을 수 있게 할지 (검색 유입 vs 개인정보 노출)
- [ ] 상담 연결로 수익을 낼지, 낸다면 어떤 방식인지 (§12 중개 해당 여부와 묶여 있다)
- [ ] 중개사 등록 확인을 어떤 자료로 할지 (등록증 사본, 공시 조회 화면, 소속 회사 확인)
- [ ] 1단계에서 다룰 상품 유형 (네 가지 전부 / 보장성·대출성 먼저)
- [ ] 금융상품자문업자도 받을지
- [ ] 실시간 상담 메시지에 RabbitMQ·Redis pub/sub 중 무엇을 쓸지 (상담 메시지 기능을 만들 때)

---

## 14. 일정 매핑

| 단계 | 범위 | 관련 절 |
|---|---|---|
| 0 | 모듈 골격, 인증 (완료) | §5.1, authentication-security.md |
| 1 | 중개사 등록 신청·확인, 프로필 | §4.1, §5.2 |
| 2 | 커뮤니티 글·답변, 신고 | §4.3, §5.3~§5.6 |
| 3 | 상담 요청 | §4.2, §5.5 |
| 4 | 운영자 화면, 배치 재확인 | §9, §11 |
