# fin-broker 도메인 설계 — 대출 정보 커뮤니티

> 상태: **초안**. 운영 주체는 **개인사업자(소프트웨어 판매업)** 이고, 대부업·대부중개업·대출모집 등록 없이 할 수 있는 범위로 설계한다 (§12). 대출나라 같은 대출 커뮤니티를 참고하되, 수익 구조는 가져오지 않고 신뢰(등록 확인·광고 분리·피해 예방)로 차별화한다.
> 구현된 것: 회원·인증(§5.1), 업체 조회·운영자 등록·대조(§5.2), 피해 예방 규칙과 검사 API(§6), 커뮤니티 글·답변과 수정·삭제(§5.4·§5.5), 신고·운영자 처리(§4.4·§5.6). 나머지는 리뷰 후 뼈대부터 채운다 (develop-principal.md §9).
> 관련 문서: [develop-principal.md](./develop-principal.md), [authentication-security.md](./authentication-security.md), [solid.md](./solid.md), [runtime-structure.md](./runtime-structure.md), [todo.md](./todo.md)
> 모르는 값은 `[ ]`로 비워 둔다. 법령에 기대는 내용은 `[확인]`을 붙인 초안이고, 원문(대부업법, 금융소비자보호법, 정보통신망법, 전기통신사업법, 전자상거래법, 개인정보보호법)과 전문가 확인 뒤 확정한다. 이 문서는 법률 자문이 아니다.

## 1. 한 줄 정의와 범위

**대출 받기 전에 업체가 등록된 곳인지 확인하고, 실제 후기를 나누고, 피해를 미리 막는 대출 정보 커뮤니티.**
플랫폼은 대출을 **중개하지 않고, 업체를 추천하지 않고, 고객을 업체에 넘기지 않는다.** 등록 정보 확인과 커뮤니티, 피해 예방 정보를 소프트웨어로 제공한다.

| 구분 | 1단계 (MVP) | 다음 단계 |
|---|---|---|
| 업체 조회 | 등록 정보(업체명·등록번호·등록기관·업체 유형) 조회, "등록 확인" 표시 | 등록 정보 정기 재확인(배치), 공공 조회 연동 |
| 커뮤니티 | 질문·후기·피해 사례·정보 글, 답변 | 이용 인증 후기, 검색, 태그 |
| 피해 예방 | 글·답변 작성 시 경고 규칙 (금리 20% 초과, 선입금 요구, 통장·체크카드 요구, 연락처 노출, 미등록 업체 언급) | LLM 으로 표현 검수 추가 |
| 업체 회원 | 등록 업체 담당자 인증 → `COMPANY` 권한, 업체 답변 표시 | 후기에 공식 답변 |
| 신고 | 글·답변 신고, 운영자 숨김, 불법 사금융 신고 창구 안내 | |
| 수익 | 일반 디스플레이 광고 (금융 카테고리 차단) | 업체용 비공개 도구 구독 `[확인]` |

**이 설계가 지키는 원칙**
- **고객을 업체에 연결하지 않는다.** 상담 신청을 받아 업체에 넘기거나, 업체가 고객 문의를 열람하는 기능을 만들지 않는다. 소비자가 연락하려면 업체의 공식 연락처를 직접 쓴다 (§12).
- **업체에게서 노출·연결의 대가를 받지 않는다.** 업체 목록 순서, 상단 노출, 배지는 돈으로 살 수 없다. 모든 등록 업체가 같은 형식으로 보인다.
- **추천하지 않는다.** "이 업체가 좋다", "당신에게 맞는 대출" 같은 맞춤 추천·비교 순위를 만들지 않는다. 사실(등록 여부, 후기)만 보여 준다.
- **작성자는 익명이다.** 화면에는 역할(익명 / 등록 업체)과 "글쓴이" 여부만 보인다. 이메일·회원 ID 를 응답에 내보내지 않는다. 대출 고민은 민감한 주제라 이름을 걸고 쓰기 어렵기 때문이다.
- 업체 담당자가 쓴 답변은 언제나 "등록 업체 답변"과 등록번호로 구분해 보인다. 답변에 연락처·금리 홍보를 쓰지 않는다 (§4.3).
- 소득·계좌·주민번호 같은 민감 정보는 **글에 쓰지 않도록 막는다** (피해 예방 규칙의 연락처·개인정보 노출).
- 화면 문구에 "무조건 승인", "100% 가능", "무심사", "저금리 보장" 같은 단정 표현을 쓰지 않는다.

---

## 2. 용어 사전

| 용어 | 코드 이름 | 뜻 |
|---|---|---|
| 금융소비자 | `RoleType.USER` | 가입한 일반 회원. 질문·후기·피해 사례를 쓴다 |
| 등록 업체 담당자 | `RoleType.COMPANY` | 등록 확인을 마친 업체에 소속이 확인된 회원. 업체 답변을 쓴다 |
| 업체 | `Company` | 대출 관련 등록 업체. 업체 정보는 등록 정보에서 온다 |
| 업체 유형 | `CompanyType` | 대부업자, 대부중개업자, 대출모집인 `[확인: 분류와 등록기관]` |
| 등록 확인 | `RegistrationStatus` | 등록 정보와 대조한 결과. 확인됨 / 확인 안 됨 / 등록 정보 없음 / 만료 |
| 업체 담당자 인증 | `CompanyMembership` | 회원이 그 업체 소속임을 운영자가 확인한 기록 |
| 글 | `Post` | 질문, 후기, 피해 사례, 정보 |
| 답변 | `Answer` | 글에 단 응답. 업체 답변은 따로 표시 |
| 경고 | `Warning` | 피해 예방 규칙이 글·답변에서 찾은 위험 신호 |
| 신고 | `Report` | 홍보성·사기 의심·개인정보 노출 글을 운영자에게 알림 |

---

## 3. 컨텍스트와 확장점

```
member ── 회원·인증 (구현 완료)
  │
company ── 업체 등록 정보, 담당자 인증 ──(승인 시 Member.role = COMPANY)
  │
community ── 글, 답변 ──(업체 답변 표시는 company 의 확인 상태를 본다)
  │
warning ── 피해 예방 규칙 ──(글·답변 저장 전에 돈다)
  │
moderation ── 신고, 숨김 ──(운영자)
  │
ops ── 운영자 집계 (/api/v1/ops/**)
```

광고는 서버 컨텍스트가 없다. 클라이언트의 고정 광고 슬롯(일반 광고 네트워크)뿐이다 (§12).

**확장점 (인터페이스 하나 + 빈 하나, solid.md O)**

| 포트 | 소유 | 1단계 구현 | 다음 구현 | 상태 |
|---|---|---|---|---|
| `WarningRule` | `warning.domain` | 규칙 클래스 다섯 개 (§6, `warning.rule`) | 규칙 추가는 `@Component` 하나 | 구현 |
| `LlmClient` | `warning.domain` | 없음 | `llm/springai` (Spring AI). 공급자 추가는 `LlmProvider` 값 + 구현체 + 조건부 빈 | 아직 코드 없음 |
| `RegistryLookup` | `company.domain` | 운영자 수동 입력 | 공공 조회 연동 `[확인: 공개 데이터·API 유무]` | 아직 코드 없음 |
| `FileStorage` | `company.domain` | 없음 | `storage/local` → S3. 담당자 재직 증빙 첨부 | 아직 코드 없음 |

포트는 실제로 호출하는 기능이 생길 때 만든다. 위 표는 자리를 정해 둔 것이다 (solid.md I).

---

## 4. 흐름과 상태

### 4.1 업체 등록 확인과 담당자 인증

```
Company.registrationStatus
  UNVERIFIED ──운영자 대조──> VERIFIED ──(재확인 실패·등록 취소)──> EXPIRED
       └──────대조 결과 없음──> NOT_FOUND

CompanyMembership.status  (회원 → 업체 담당자 신청)
  PENDING ──운영자 승인──> APPROVED ──(퇴사·업체 EXPIRED)──> REVOKED
     └──반려──> REJECTED
```

| 전이 | 누가 | 같이 바뀌는 것 (한 트랜잭션) |
|---|---|---|
| 업체 `VERIFIED` | ADMIN | `lastCheckedAt` |
| 업체 `EXPIRED` | ADMIN / 배치 재확인 | 그 업체의 `APPROVED` 담당자 전부 `REVOKED`, 각 회원 `roleType = USER` |
| 담당자 `APPROVED` | ADMIN | `Member.roleType = COMPANY` (업체가 `VERIFIED` 일 때만) |
| 담당자 `REVOKED` | ADMIN / 업체 만료 | `Member.roleType = USER` |

- 운영자 화면의 대조 버튼은 `RegistrationStatus.allowedResults()` 가 정한다 (전이 표와 같은 곳). 지금 상태와 같은 결과(예: `VERIFIED` → `VERIFIED`)는 "다시 대조했는데 그대로"이고 `lastCheckedAt` 만 새로 잡는다
- 재확인 필요: `VERIFIED` 인데 `lastCheckedAt` 에서 `company.recheck-after`(기본 90일)가 지난 업체 (`Company.needsRecheck`, 경계 포함). 지금은 운영자 화면 표시·걸러 보기만 하고, 자동 재확인은 배치(§14 4단계)

**불변식**: `Member.roleType == COMPANY` ⇔ 그 회원의 `CompanyMembership.status == APPROVED` 이고 그 업체가 `VERIFIED`. 권한을 바꾸는 곳은 `CompanyMembership` 의 전이 메서드와 이를 부르는 서비스 하나다 (solid.md S).

### 4.2 글 작성과 피해 예방 경고

```
작성 화면 ──(본문)──> POST /api/v1/warnings/check   미리 보기. 저장하지 않는다
                         └─> Warning 목록 (규칙, 근거 문장, 안내 문구)
저장 ──> 서버가 규칙을 다시 돌린다 (클라이언트 결과를 믿지 않는다)
           BLOCK 등급 있음  -> 400 POST_BLOCKED (예: 주민번호·계좌번호 노출)
           WARN 등급만      -> 저장 + 글에 경고 배지 (예: "선입금 요구 사례")
```

### 4.3 업체 답변 규칙

- `COMPANY` 회원의 답변에는 업체명과 등록번호가 자동으로 붙는다. 본인이 지울 수 없다.
- 업체 답변에는 전화번호·메신저 ID·링크를 쓸 수 없다 (연락처 노출 규칙이 `BLOCK`). 고객이 업체로 바로 넘어가는 통로를 만들지 않기 위해서다.
- 업체는 자기 업체에 대한 후기에만 공식 답변을 단다 (다음 단계). 다른 글에는 일반 정보 답변만.

### 4.4 신고

```
Post: PUBLISHED ──신고 누적 / 운영자──> HIDDEN ──운영자 복구──> PUBLISHED
                └──작성자 삭제──> DELETED (내용 비움, 행은 남김)
Report: OPEN ──운영자──> ACCEPTED(글 숨김) | DISMISSED
```

- **처리는 대상 단위다.** 운영자가 신고 하나를 처리하면 같은 대상의 접수 신고가 모두 같은 결과로 닫힌다. 운영자 목록도 대상 하나당 한 줄로 묶어 사유·메모·건수를 보여 준다.
- **신고 누적 자동 숨김:** 서로 다른 회원의 접수 신고가 `moderation.auto-hide-report-count`(기본 3)에 이르면 운영자 확인 전에 먼저 숨긴다. 운영자가 "문제없음"으로 처리하면 되살아난다.
- 한 회원은 같은 대상을 한 번만 신고한다 (유니크 제약). 내 글·답변은 신고할 수 없다.
- 작성자가 지운 글·답변은 행을 남기고 내용을 비운다. 숨김·삭제된 글은 사용자 API 에서 없는 글(404)로 다룬다.

권리 침해 신고(명예훼손 등)를 받으면 정보통신망법의 임시조치 절차를 따른다 `[확인: 절차와 기간]`.

---

## 5. 애그리거트

### 5.1 Member (구현 완료)

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | Long | |
| `email` | String(100) | 로그인 ID. 소문자·공백 제거로 정규화, 유니크 |
| `password` | String(100) | `{bcrypt}` 해시 |
| `roleType` | `RoleType` | `USER`, `COMPANY`, `ADMIN`, `SYSTEM` |
| `tokenVersion` | int | 로그아웃 시 +1. 이전 토큰 무효 (authentication-security.md §5) |

### 5.2 Company

| 필드 | 타입 | 비고 |
|---|---|---|
| `id` | Long | |
| `name` | String | 등록 상호 |
| `companyType` | `CompanyType` | `LENDER`(대부업자), `LOAN_BROKER`(대부중개업자), `LOAN_AGENT`(대출모집인) `[확인]` |
| `registrationNumber` | String | 등록번호 |
| `registrationAuthority` | String | 등록기관 (시·도 / 금융위원회 등) `[확인]` |
| `registrationStatus` | `RegistrationStatus` | §4.1 |
| `lastCheckedAt`, `checkedBy` | Instant, Long | 마지막 대조 시각·운영자 |

업체 정보는 등록 정보에서 온 사실만 담는다. 금리·한도·상품 소개 칸은 두지 않는다 (홍보 통로가 되지 않게).

### 5.3 CompanyMembership

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `memberId`, `companyId` | Long | 회원당 승인된 소속은 하나 |
| `status` | `MembershipStatus` | §4.1 |
| `decidedAt`, `decidedBy` | Instant, Long | |

### 5.4 Post

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `authorId` | Long | |
| `authorRole` | `RoleType` | **작성 시점 스냅샷.** 담당자 인증이 풀려도 당시 "업체 작성"이었다는 사실은 남긴다 |
| `postType` | `PostType` | `QUESTION`(질문), `REVIEW`(후기), `SCAM_CASE`(피해 사례), `INFO`(정보) |
| `loanType` | `LoanType` | `CREDIT`(신용), `SECURED`(담보), `POLICY`(정책서민금융), `BUSINESS`(사업자), `OTHER` `[확인: 분류]` |
| `companyId` | Long, nullable | 후기·피해 사례가 가리키는 업체 |
| `title`, `body` | String | |
| `warnings` | Set<`WarningType`> | 저장 시 서버 규칙 결과 (WARN 등급만) |
| `status` | `PostStatus` | §4.4 |
| `answerCount` | int | 답변 추가·삭제와 같은 트랜잭션에서 바꾼다 |
| `version` | Long | `@Version`. 답변 수 갱신 lost update 방지 |

### 5.5 Answer

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `postId`, `authorId` | Long | |
| `authorRole` | `RoleType` | 작성 시점 스냅샷 |
| `companyId` | Long, nullable | `COMPANY` 답변이면 소속 업체 (등록번호 표시용) |
| `body`, `status`, `warnings` | | 글과 같다 |

### 5.6 Report

| 필드 | 타입 | 비고 |
|---|---|---|
| `id`, `reporterId` | Long | |
| `targetType`, `targetId` | `ReportTarget`, Long | 글 / 답변 |
| `reason` | `ReportReason` | 홍보성, 사기 의심, 개인정보 노출, 욕설, 권리 침해, 기타 |
| `status` | `ReportStatus` | §4.4 |

모든 enum 은 `DescriptionCode` 를 구현한다 (develop-principal.md §1).

---

## 6. 피해 예방 규칙 (`WarningRule`)

규칙 하나 = 클래스 하나. 규칙을 더해도 기존 코드를 고치지 않는다 (`List<WarningRule>` 주입).

```java
public interface WarningRule {
    /** 규약: 위험 신호가 없으면 빈 목록. 예외를 던지지 않는다 (solid.md L) */
    List<Warning> inspect(WarningTarget target);
}
```

| 규칙 | `WarningType` | 등급 | 찾는 것 | 안내 문구 (초안) |
|---|---|---|---|---|
| `InterestCapRule` | `OVER_INTEREST_CAP` | WARN | 연 20% 를 넘는 금리 표기 (`연 30%`, `일 1%` 환산) | "법정 최고금리(연 20%)를 넘는 금리는 불법입니다" `[확인: 최고금리]` |
| `UpfrontPaymentRule` | `UPFRONT_PAYMENT` | WARN | 수수료·보증금·작업비 선입금 요구 표현 | "대출 전에 돈을 요구하면 사기를 의심하세요" |
| `AccountHandoverRule` | `ACCOUNT_HANDOVER` | WARN | 통장·체크카드·OTP 전달 요구 표현 | "통장·카드를 넘기면 범죄에 이용될 수 있습니다" |
| `ContactExposureRule` | `CONTACT_EXPOSURE` | BLOCK | 전화번호·메신저 ID·주민번호·계좌번호 | "연락처·개인정보는 글에 쓸 수 없습니다" |
| `UnregisteredCompanyRule` | `UNREGISTERED_COMPANY` | WARN | 글이 가리키는 업체가 `VERIFIED` 가 아님 | "등록 정보를 확인하지 못한 업체입니다" |

- 숫자 판정(금리)은 규칙이 한다. LLM 은 나중에 "표현이 사기 수법과 비슷한지" 같은 의미 판단에만 쓴다.
- 경고는 판단이 아니라 **위험 신호 안내**다. "이 업체는 불법입니다"라고 단정하지 않는다.

---

## 7. 외부 연동 계약 (자리만)

| 연동 | 포트 | 입력 | 출력 | 규약 |
|---|---|---|---|---|
| 표현 검수 | `LlmClient` | 글 본문 (개인정보 마스킹 후) | 위험 신호 목록 + 근거 문장 | WARN 까지만. BLOCK·숨김은 규칙과 운영자가 한다 |
| 등록 조회 | `RegistryLookup` | 등록번호 또는 상호 | 등록 정보 / 없음 | 없으면 예외가 아니라 empty |
| 파일 저장 | `FileStorage` | 재직 증빙 | 저장 키 | 없는 파일 읽기는 예외가 아니라 empty (solid.md L) |

---

## 8. 데이터 모델

| 테이블 | 주요 칼럼 | 제약 |
|---|---|---|
| `member` | id, email, password, role_type, token_version, created_at, modified_at | uk(email) |
| `company` | id, name, company_type, registration_number, registration_authority, registration_status, last_checked_at, checked_by | uk(company_type, registration_number) |
| `company_membership` | id, member_id, company_id, status, decided_at, decided_by | idx(member_id, status) |
| `post` | id, author_id, author_role, post_type, loan_type, company_id, title, body, status, answer_count, version | idx(post_type, status, created_at), idx(company_id) |
| `post_warning` | post_id, warning_type | pk(둘) |
| `answer` | id, post_id, author_id, author_role, company_id, body, status | idx(post_id, created_at) |
| `report` | id, reporter_id, target_type, target_id, reason, status | uk(reporter_id, target_type, target_id) |

enum 칼럼은 varchar (`NoColumnCheckMySQLDialect`, develop-principal.md §5).

---

## 9. 화면과 요구사항 ID

### 9.1 화면 (SCR)

| ID | 화면 | 경로 (client `routes.ts`) | 상태 |
|---|---|---|---|
| SCR-01 | 홈 (피해 예방 안내, 최근 글) | `/` | 뼈대 |
| SCR-02 | 게시판 목록 (글 유형·대출 유형별) | `/posts?postType=&loanType=` | 구현 |
| SCR-03 | 글 상세 + 답변 | `/posts/:postId` | 구현 |
| SCR-04 | 글쓰기 (경고 미리 보기) | `/posts/new` | 구현 |
| SCR-05 | 업체 조회 (등록 정보 + 후기) | `/companies`, `/companies/:companyId` | 구현 (후기 제외) |
| SCR-06 | 피해 예방 안내 (사기 수법, 신고 창구, 받은 문자 검사) | `/safety` | 구현 |
| SCR-07 | 로그인·가입 | `/login` | 구현 |
| SCR-08 | 운영자 (업체 대조, 담당자 승인, 신고) | `/ops/reports`, `/ops/companies` | 신고 처리, 업체 관리(현황·걸러 보기·등록·대조·재확인 표시) 구현. 담당자 승인 화면 전 |
| SCR-09 | 업체 담당자 인증 신청 | `/company/apply` | |

### 9.2 요구사항 (FR)

| ID | 기능 | 상태 |
|---|---|---|
| FR-AU-001 | 이메일·비밀번호 가입 | 구현 |
| FR-AU-002 | 로그인·로그아웃·내 정보 (JWT HttpOnly 쿠키) | 구현 |
| FR-AU-003 | 역할 기반 인가 (`USER`, `COMPANY`, `ADMIN`, `SYSTEM`) | 구현 (운영자 경로) |
| FR-CO-001 | 업체 조회 (등록 정보, 확인 상태, 관련 후기) | 구현 (후기 제외) |
| FR-CO-002 | 운영자 업체 목록·현황·등록·대조·만료 처리, 재확인 필요 표시 | 구현 (SCR-08 업체 관리) |
| FR-CO-003 | 업체 담당자 인증 신청·승인·해제 | |
| FR-CM-001 | 글 작성·수정·삭제 | 구현 |
| FR-CM-002 | 글 목록·상세 | 구현 |
| FR-CM-003 | 답변 작성 (업체 답변 구분 표시) | 구현 |
| FR-WN-001 | 피해 예방 경고 미리 보기 | 구현 |
| FR-WN-002 | 저장 시 경고 판정 (BLOCK 거부, WARN 배지) | 구현 (400 `CONTENT_BLOCKED` + `warnings`) |
| FR-MD-001 | 글·답변 신고 | 구현 (신고 누적 자동 숨김 포함) |
| FR-MD-002 | 운영자 신고 처리·숨김 | 구현 (대상 단위 처리) |

---

## 10. API 초안

| 메서드 | 경로 | 권한 | FR |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 없음 | FR-AU-001 |
| POST | `/api/v1/auth/login` | 없음 (필터) | FR-AU-002 |
| POST | `/api/v1/auth/logout` | 없음 | FR-AU-002 |
| GET | `/api/v1/me` | 로그인 | FR-AU-002 |
| GET | `/api/v1/companies?keyword=&page=&size=` (상호 일부 또는 등록번호, 상호순) | 로그인 `[확인: 비로그인 공개 여부]` | FR-CO-001 |
| GET | `/api/v1/companies/{companyId}` | 로그인 | FR-CO-001 |
| POST | `/api/v1/company-memberships` | USER | FR-CO-003 |
| GET | `/api/v1/posts?postType=&loanType=&companyId=&page=` | 로그인 | FR-CM-002 |
| POST | `/api/v1/posts` | 로그인 | FR-CM-001, FR-WN-002 |
| GET | `/api/v1/posts/{postId}` | 로그인 | FR-CM-002 |
| POST | `/api/v1/posts/{postId}/answers` | 로그인 | FR-CM-003, FR-WN-002 |
| POST | `/api/v1/warnings/check` | 로그인 | FR-WN-001 |
| PATCH·DELETE | `/api/v1/posts/{postId}` | 작성자 (아니면 403) | FR-CM-001 |
| PATCH·DELETE | `/api/v1/posts/{postId}/answers/{answerId}` | 작성자 (아니면 403) | FR-CM-003 |
| POST | `/api/v1/reports` | 로그인 | FR-MD-001 |
| GET | `/api/v1/ops/companies` (`?status=&recheckDue=&keyword=&page=&size=`, 새로 등록한 순) | ADMIN, SYSTEM | FR-CO-002 |
| GET | `/api/v1/ops/companies/summary` (상태별 수, 재확인 필요 수) | ADMIN, SYSTEM | FR-CO-002 |
| POST | `/api/v1/ops/companies` | ADMIN, SYSTEM | FR-CO-002 |
| PATCH | `/api/v1/ops/companies/{companyId}/registration-status` (`{result}`) | ADMIN, SYSTEM | FR-CO-002 |
| GET·PATCH | `/api/v1/ops/company-memberships/**` | ADMIN, SYSTEM | FR-CO-003 |
| GET | `/api/v1/ops/reports?status=` (대상 단위로 묶음) | ADMIN, SYSTEM | FR-MD-002 |
| PATCH | `/api/v1/ops/reports/{reportId}` (`{decision}`) | ADMIN, SYSTEM | FR-MD-002 |

없는 API (만들지 않는다): 상담 신청, 업체에 문의 전달, 업체 순위·추천, 업체 광고 등록.
비로그인 공개 경로가 생기면 `SecurityConfig` 의 허용 목록에 GET 경로를 더한다 (authentication-security.md §9).

---

## 11. 패키지 구조

```
com.marinbom2.finbroker
 +- config, exception, type.common, util, domain.base   (core 공통)
 +- member     domain(core) / application, api(server)   ← 구현
 +- company    domain(core) / application, api(server)
 +- community  domain(core) / application, api(server)
 +- warning    domain(core): WarningRule, 규칙 구현 / api(server): 미리 보기
 +- moderation domain(core) / application, api(server)
 +- ops        api(server)   집계 조회 전용
 +- security   (server)      ← 구현
 +- storage/local, llm/springai   어댑터 (core). 기술 타입은 이 안에서만
 +- batch      (batch 모듈)   업체 등록 정보 재확인

의존 방향: api -> application -> domain <- (storage, llm 어댑터)
```

---

## 12. 사업 범위·규제·개인정보

### 12.1 하는 것과 하지 않는 것

| 하는 것 (SW·정보 서비스) | 하지 않는 것 (등록이 필요한 영역) `[확인]` |
|---|---|
| 등록 정보 조회·표시 | 대출 상담 신청을 받아 업체에 전달 (대부중개) |
| 커뮤니티 글·답변·후기 | 업체에게서 광고비·노출비·건당 수수료 받기 |
| 피해 예방 경고, 신고 안내 | 업체 순위·추천·맞춤 대출 비교 |
| 일반 디스플레이 광고 (금융 카테고리 차단) | 대출·대부 상품 광고 게재 |
| 업체용 **비공개** 도구 구독 (다음 단계) | 구독 여부에 따른 노출·배지 차이 |

대출나라 같은 사이트는 업체 광고와 고객 연결로 수익을 내는 대부중개 플랫폼이고, 그 구조에는 대부중개업 등록이 필요하다 `[확인]`. 이 서비스는 그 구조를 쓰지 않는다. 등록을 하게 되면 이 표의 오른쪽을 다시 검토한다.

### 12.2 운영 주체가 갖출 것 (개인사업자)

| 항목 | 내용 | 확인 |
|---|---|---|
| 사업자등록 업종 | 소프트웨어 판매업에 인터넷 정보 매개·광고 관련 업종 추가 | `[확인: 세무서·홈택스 업종 코드]` |
| 통신판매업 신고 | 온라인으로 구독 결제를 받을 때 | `[확인]` |
| 부가통신사업 신고 | 소규모 사업자 면제 여부 | `[확인]` |
| 개인정보 | 개인정보처리방침 게시, 보호책임자 지정, 광고 네트워크 쿠키 고지 | `[확인]` |
| 게시물 | 권리 침해 신고 시 임시조치 절차 | `[확인]` |
| 서비스 이름 | "broker"·"중개"가 등록 업체로 오인될 수 있는지. 사용자에게 보이는 이름은 따로 정한다 | `[확인]` |

### 12.3 그 밖의 원칙

| 항목 | 정한 것 |
|---|---|
| 광고 | 광고 슬롯은 글·답변 목록과 분리하고 "광고" 표시. 맞춤 광고에 글 내용을 쓰지 않는다 |
| 금지 표현 | 화면 문구·enum `description` 에 "무조건 승인", "100% 가능", "무심사", "저금리 보장" 같은 표현을 쓰지 않는다 |
| 개인정보 | 연락처·주민번호·계좌번호는 글에 쓸 수 없다 (`ContactExposureRule` BLOCK) |
| 등록번호 | 공개 등록 정보라 그대로 보인다 `[확인]` |
| 인증·보안 | JWT HttpOnly 쿠키, SameSite=Lax, 권한은 요청마다 DB (authentication-security.md) |

---

## 13. 미결 질문

- [ ] 비로그인 사용자도 업체 조회·글 읽기를 할 수 있게 할지 (검색 유입 vs 개인정보 노출)
- [ ] 업체 등록 정보를 어디서 대조할지 (공공 조회 화면 수동 대조 / 공공 데이터 연동)
- [ ] 업체 담당자 재직을 어떻게 확인할지 (회사 메일, 재직 증빙)
- [ ] 이용 인증 후기를 받을지, 받으면 무엇으로 인증할지 (개인정보를 받지 않는 방법)
- [ ] 업체용 비공개 도구 구독을 할지, 무엇을 담을지
- [ ] 사용자에게 보이는 서비스 이름

---

## 14. 일정 매핑

| 단계 | 범위 | 관련 절 |
|---|---|---|
| 0 | 모듈 골격, 인증 (완료) | §5.1, authentication-security.md |
| 1 | 업체 조회 (운영자 입력), 피해 예방 규칙 | §4.1, §5.2, §6 |
| 2 | 커뮤니티 글·답변, 신고 | §4.2~§4.4, §5.4~§5.6 |
| 3 | 업체 담당자 인증, 업체 답변 | §4.1, §4.3, §5.3 |
| 4 | 운영자 화면, 배치 재확인, 일반 광고 슬롯 | §9, §11, §12 |
