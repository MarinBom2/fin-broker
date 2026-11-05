# SOLID 원칙 — fin-broker 적용 가이드

> 목적: fin-broker 백엔드(Spring Boot 4.1 / Java 21 / JPA / MySQL)와 프론트엔드(React 19 / TypeScript)를 작성할 때
> 지켜야 할 객체지향 설계 원칙과, 이 프로젝트에서의 구체적인 적용 기준을 정리한다.
> 핵심 규칙은 다섯 줄이다: 불변식은 한 클래스에, 확장점은 인터페이스 하나 + 빈 하나, 호출부 규약은 인터페이스에 못박기, 실제로 호출되는 메서드만, 기술 타입은 경계를 넘지 않음.
> 관련 문서: [domain-design.md](./domain-design.md), [develop-principal.md](./develop-principal.md)

## 한눈에 보기

| 원칙 | 한 줄 정의 | 위반 신호 | fin-broker 에서의 적용 |
|---|---|---|---|
| **S** 단일 책임 (SRP) | 클래스는 변경 이유가 하나여야 한다 | 한 클래스가 HTTP, 상태 전이, DB, 외부 SDK 를 모두 다룸 | 함께 지켜야 하는 불변식(예: 중개사 확인 상태와 회원 권한, 파일 본문과 메타 행의 저장 순서·버전)은 한 클래스에 둔다. 단일 책임을 이유로 정합성 규칙을 쪼개지 않는다 |
| **O** 개방-폐쇄 (OCP) | 확장에는 열려 있고 수정에는 닫혀 있어야 한다 | 공급자·저장소가 늘 때마다 `switch`/`if` 를 고침 | 확장점은 인터페이스 하나 + 빈 하나. AI 추가는 `LlmProvider` 값 + `LlmClient` 구현체 + 조건부 빈 |
| **L** 리스코프 치환 (LSP) | 하위 타입은 상위 타입을 완전히 대체할 수 있어야 한다 | 구현체가 `UnsupportedOperationException` 을 던짐 | 구현체를 바꿔도 호출부 규약이 같게 인터페이스에 못박는다 (없는 파일 읽기는 예외가 아니라 empty 등) |
| **I** 인터페이스 분리 (ISP) | 사용하지 않는 메서드에 의존하게 만들지 않는다 | 거대한 인터페이스, 빈 구현 메서드 | 실제로 호출되는 메서드만. 나중에 쓸 것 같은 메서드를 미리 넣지 않는다 |
| **D** 의존 역전 (DIP) | 고수준 모듈은 저수준 구현이 아니라 추상에 의존한다 | Service 가 Spring AI·파일 시스템 타입을 직접 import | 기술 타입이 경계를 넘지 않는다. 파일 시스템은 `storage/local`, Spring AI 는 `llm/springai` 아래만 |

---

## S — 단일 책임 원칙 (Single Responsibility)

**정의**: 하나의 클래스(모듈)는 하나의 액터(변경을 요구하는 주체)에 대해서만 책임진다. "변경 이유가 하나"라는 뜻이다.

**fin-broker 에서의 계층 책임**

| 계층 | 책임 | 바뀌는 이유 |
|---|---|---|
| `api` (Controller) | HTTP 요청/응답 변환, 인증 주체(`@AuthenticationPrincipal Member`) 받기, 입력 형식 검증 | API 스펙이 바뀔 때 |
| `application` (Service) | 유스케이스 흐름 조립, 트랜잭션 경계 | 업무 흐름이 바뀔 때 |
| `domain` (Entity, VO, 포트) | 상태 전이, 불변식 (예: `BrokerProfile` 확인 상태) | 업무 규칙이 바뀔 때 |
| 어댑터 (`storage/local`, `llm/springai`) | 외부 기술 연동 | 저장소·SDK 가 바뀔 때 |

**불변식은 쪼개지 않는다**: "SRP 니까" 하고 함께 지켜야 하는 규칙을 두 클래스로 나누면 둘 중 하나만 바뀌는 틈이 생긴다.

**나쁜 예**: 중개사 승인과 회원 권한 변경이 서로 다른 서비스에 있다. 한쪽만 호출되면 `BROKER` 권한인데 확인되지 않은 회원이 생긴다.

```java
brokerProfileService.verify(profileId);       // status = VERIFIED
memberService.changeRole(memberId, BROKER);   // 호출을 빠뜨리거나 실패하면 불일치
```

**좋은 예**: 상태 전이 메서드 하나가 둘을 같이 바꾸고, 서비스 하나가 같은 트랜잭션에서 부른다.

```java
// broker.domain
public void verify(Member member, Long adminId, Instant now) {
    if (status != BrokerStatus.PENDING) throw new IllegalBrokerStateException(id, status, "승인");
    this.status = BrokerStatus.VERIFIED;
    this.verifiedAt = now;
    this.verifiedBy = adminId;
    member.changeRole(RoleType.BROKER);       // 불변식: BROKER ⇔ VERIFIED
}
```

인증 코드에서의 예: 액세스 토큰 쿠키의 속성(HttpOnly, SameSite, Max-Age)은 발급·삭제·읽기가 같아야 하므로 `AccessTokenCookie` 한 클래스에 있다. 시큐리티 오류 JSON 은 `SecurityErrorResponder` 한 곳에서만 쓴다.

**체크 질문**: "이 클래스가 바뀌어야 하는 이유를 한 문장으로 말할 수 있는가?" 두 문장이 나오면 쪼갠다. 단, 그 두 문장이 **같은 불변식**을 말하면 쪼개지 않는다.

---

## O — 개방-폐쇄 원칙 (Open-Closed)

**정의**: 새 기능을 추가할 때 기존 코드를 수정하지 않고 새 코드를 추가하는 것으로 끝나야 한다.

**fin-broker 의 확장점** (domain-design.md §3)
- AI 공급자: `LlmProvider` enum 값 + `LlmClient` 구현체 + 조건부 빈
- 파일 저장소: `FileStorage` 구현체 (`storage/local` → S3)
- 인증 방식: 토큰 타입 + `AuthenticationProvider` 한 쌍 (`MemberAuthenticationToken` + `MemberAuthenticationProvider` 처럼)

**나쁜 예**: 공급자가 늘 때마다 분기가 자란다.

```java
public ReviewResult review(String text) {
    return switch (properties.provider()) {
        case OPENAI -> openAiReview(text);
        case ANTHROPIC -> anthropicReview(text);   // 추가할 때마다 이 메서드 수정
    };
}
```

**좋은 예**: 공급자 값 하나에 빈 하나. 설정이 고른다.

```java
public enum LlmProvider implements DescriptionCode { SPRING_AI_OPENAI("OpenAI"), SPRING_AI_ANTHROPIC("Anthropic"); ... }

@Configuration
class LlmConfig {
    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "SPRING_AI_ANTHROPIC")
    LlmClient anthropicLlmClient(ChatClient.Builder builder) { return new SpringAiLlmClient(builder.build()); }
}
```

**주의**: 변경 지점이 실제로 두 개 이상 생겼을 때 추상화한다. domain-design.md §3 표에 자리를 정해 둔 포트 외의 "미래를 위한" 인터페이스는 만들지 않는다.

---

## L — 리스코프 치환 원칙 (Liskov Substitution)

**정의**: `S` 가 `T` 의 하위 타입이면, `T` 를 쓰는 코드는 `S` 로 바꿔도 올바르게 동작해야 한다.
구현체는 인터페이스가 약속한 **사전 조건을 강화하지 않고, 사후 조건을 약화하지 않는다.**

**위반 신호**
- 구현체 메서드가 `UnsupportedOperationException` 을 던진다.
- 호출하는 쪽이 `instanceof` 로 구현체를 구분한다.
- 같은 상황에서 구현체마다 다른 결과(한쪽은 예외, 한쪽은 null)를 낸다.

**나쁜 예**: 로컬 구현은 없는 파일에 `Optional.empty()`, S3 구현은 `NoSuchKeyException`. 호출부가 구현체를 알아야 한다.

**좋은 예**: 규약을 Javadoc 에 못박고 모든 구현체가 지킨다. 규약 테스트 하나를 구현체 테스트가 상속한다.

```java
public interface FileStorage {
    /** 규약: 저장 키를 돌려준다. 키에 원본 파일명을 넣지 않는다 (prefix/uuid.확장자) */
    String store(StoredFile file);

    /** 규약: 없는 키는 예외가 아니라 Optional.empty(). 저장소 장애만 FileStorageException */
    Optional<StoredFile> read(String key);
}

abstract class FileStorageContractTest { ... }   // LocalFileStorageTest, S3FileStorageTest 가 상속
```

인증 코드에서의 예: `UserDetailsService` 규약대로 없는 회원은 `UsernameNotFoundException` 이고, `MemberAuthenticationProvider` 는 이를 비밀번호 불일치와 같은 `BadCredentialsException` 으로 바꿔 호출부(`ProviderManager`)에 같은 의미를 전달한다.

---

## I — 인터페이스 분리 원칙 (Interface Segregation)

**정의**: 클라이언트는 자신이 사용하지 않는 메서드에 의존하도록 강요받아서는 안 된다. "실제로 호출되는 메서드만 넣는다"가 이것이다.

**나쁜 예**: LLM 포트에 SDK 기능을 전부 옮겨 놓았다. 실제로 쓰는 것은 하나다.

```java
public interface LlmClient {
    ReviewResult review(ReviewRequest req);
    String chat(List<Message> history);
    float[] embed(String text);
    Stream<String> stream(String prompt);
}
```

**좋은 예**: 호출부가 쓰는 메서드만 둔다. 필요가 생기면 **별도 포트**를 추가한다.

```java
public interface LlmClient {
    ReviewResult review(ReviewRequest req);
}
```

- 리포지토리에도 쓰는 쿼리만 둔다. `MemberRepository` 는 `findByEmail`, `existsByEmail` 두 개뿐이다.
- 운영자 집계 조회는 `ops` 패키지의 별도 조회 리포지토리로 분리한다.
- 인증 골격을 옮길 때도 이 원칙을 적용했다: 호출부가 없는 `AccessUserInfo`, `RoleType.isAdmin()` 은 옮기지 않았다 (authentication-security.md §10).

---

## D — 의존 역전 원칙 (Dependency Inversion)

**정의**: 고수준 모듈(업무 규칙)은 저수준 모듈(DB, 외부 API)에 의존하지 않는다. 둘 다 추상(인터페이스)에 의존하고, **인터페이스는 고수준 쪽이 소유한다.**

**패키지 구조와 의존 방향** ([domain-design.md §11](./domain-design.md))

```
com.marinbom2.finbroker
 +- member / broker / community / consultation / moderation
 |   +- domain        # 엔티티 + 포트: FileStorage, LlmClient
 |   +- application
 |   +- api
 +- storage/local     # FileStorage 구현. java.nio.file 타입은 여기서만
 +- llm/springai      # LlmClient 구현. Spring AI 타입(ChatClient 등)은 여기서만
 +- security          # Spring Security·jjwt 타입은 여기서만 (server 모듈)

의존 방향:  api -> application -> domain <- (storage, llm)
```

- `*.domain`·`*.application` 은 `storage.local`, `llm.springai` 패키지와 SDK 를 import 하지 않는다.
- **기술 타입이 경계를 넘지 않는다.** Spring AI 응답 타입은 `llm/springai` 안에서 `ReviewResult` 로 바뀐다. jjwt 의 `Claims` 는 `security.jwt` 안에서 `AccessTokenClaims` 로 바뀐다.
- 모듈로도 막는다: 외부 SDK 는 core 에서 `implementation` 으로 두어 server·batch 가 import 할 수 없게 한다 (develop-principal.md §5).
- 예외 한 가지: `Member` 는 인증 주체라 `UserDetails` 를 구현한다. spring-security-core 만 쓰고 웹 시큐리티 타입은 쓰지 않는다.

**주입 규칙**
- 생성자 주입만 사용한다. 필드 주입(`@Autowired` 필드)과 세터 주입은 금지한다.
- Lombok `@RequiredArgsConstructor` + `private final` 필드 조합을 표준으로 한다.
- 순환 의존이 생기면 설계를 다시 본다. `@Lazy` 로 덮지 않는다.

---

## 프론트엔드(React)에서의 SOLID

| 원칙 | React 적용 |
|---|---|
| SRP | 컴포넌트는 "그리기"만, 데이터 로딩은 커스텀 훅으로, HTTP 는 `shared/api/` 모듈로 분리 |
| OCP | 글 유형(`postType`)별 카드는 매핑 테이블로 추가, 분기문 추가 지양 |
| LSP | 같은 props 타입을 받는 컴포넌트끼리는 교체 가능해야 함 |
| ISP | 컴포넌트가 필요한 필드만 props 로 받는다. 거대한 객체 통째로 넘기기 지양 |
| DIP | 컴포넌트가 `fetch` 를 직접 호출하지 않고 `shared/api/*Service.ts` 를 통해 호출 (`apiRequest` 가 유일한 수신부) |

---

## 코드 리뷰 체크리스트

- [ ] 이 클래스가 바뀌는 이유를 한 문장으로 말할 수 있는가? 함께 지켜야 할 불변식을 쪼개 놓지 않았는가? (SRP)
- [ ] 공급자·저장소를 추가할 때 기존 클래스를 수정하지 않아도 되는가? (OCP)
- [ ] 인터페이스 구현체 중 "지원 안 함" 예외를 던지거나 규약과 다른 결과를 내는 메서드가 있는가? (LSP)
- [ ] 인터페이스에 실제로 호출되지 않는 메서드가 있는가? (ISP)
- [ ] `domain`/`application` 이 Spring AI·파일 시스템·jjwt 타입을 import 하는가? (DIP, 경계)
- [ ] 생성자 주입을 쓰는가? 필드 주입이 남아 있는가?
- [ ] 추상화가 domain-design.md §3 의 확장점 외에 "미래를 위해" 만들어진 것은 아닌가? (과적용 경계)
