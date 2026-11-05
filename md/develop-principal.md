# 개발 규약 — fin-broker 코드 작성 규칙

> 목적: SOLID 같은 일반 원칙이 아니라, 이 프로젝트에서 코드를 쓸 때 **매번 같은 방식으로** 해야 하는 구체적인 규칙을 모은다.
> 새 규칙이 정해지면 이 문서에 먼저 추가하고, 기존 코드는 해당 영역을 건드릴 때 맞춘다.
> 관련 문서: [solid.md](./solid.md), [domain-design.md](./domain-design.md), [runtime-structure.md](./runtime-structure.md), [authentication-security.md](./authentication-security.md)

## 한눈에 보기

| 영역 | 규칙 | 근거 |
|---|---|---|
| enum | 모든 enum(중첩 enum 포함)은 `DescriptionCode` 를 구현하고 한글 `description` 을 가진다. 프론트에는 `{code, description}` 으로 내려간다 | 표시 문구를 서버 한 곳에서 관리 |
| 예외 | 도메인 규칙 위반 예외는 해당 도메인 패키지(`company.domain.exception` 등)에 둔다. 모두 unchecked | 의존 방향(domain 이 바깥을 모름), 자동 롤백 |
| 요구사항 추적 | 코드에 `FR-…`, `SCR-…` ID 를 주석·라우트 이름에 남긴다 | 요구사항 정의서·화면설계서를 개발 후 산출물로 뽑음 |
| 주석 | 필요한 곳에만 한두 줄. 날짜를 넣지 않는다. HTML 태그를 쓰지 않는다 | 날짜·경위는 git 기록과 md 문서가 맡는다 |
| 화면 문구 | "무조건 승인", "100% 가능", "무심사", "저금리 보장" 같은 단정 표현을 쓰지 않는다 | 대출 광고·권유로 오해받지 않게 (domain-design.md §12) |
| JWT | jjwt 0.13 + `jjwt-gson`. `jjwt-jackson` 은 쓰지 않는다 | Boot 4 의 Jackson 3(`tools.jackson`)와 Jackson 2 가 섞임 |
| 모듈 | 도메인·공통 yml 은 `common:core`, API 는 `server`, 배치는 `batch`. 앱 모듈끼리 의존하지 않는다 | server·batch 가 같은 도메인을 씀 |
| 비밀 값 | `src/main/resources` 에 두지 않는다. 로컬은 루트 `secrets/`, 운영은 환경변수 | resources 에 두면 jar·이미지에 들어감 |
| API 응답·오류 | 성공은 DTO 그대로, 오류는 ProblemDetail(RFC 9457) + `code`(`ErrorCode`). 시큐리티 401/403 도 같은 형식 | 표준 형식. 성공 여부는 HTTP 상태 하나로 |
| API 경로·문서 | 모든 API 는 `/api/v1/...`, 운영자는 `/api/v1/ops/...`. 컨트롤러에 `@Tag`, 메서드에 FR ID 로 시작하는 `@Operation` | 프록시·시큐리티가 `/api` 접두사 기준 |
| 작업 순서 | 새 영역은 뼈대(패키지·시그니처)를 먼저 잡고 구현을 채운다 | 구조 결정을 먼저 리뷰할 수 있음 |

---

## 1. enum: code + description

### 규칙

- 서버의 **모든 enum** 은 `com.marinbom2.finbroker.type.common.DescriptionCode` 를 구현한다.
- 한글 `description` 필드를 가진다. 화면에 그대로 나가는 문구이므로 단정 표현(한눈에 보기 "화면 문구")을 쓰지 않는다.
- `@JsonFormat(shape = Shape.OBJECT)` 를 붙여 JSON 으로 `{"code": "COMPANY", "description": "등록 업체"}` 형태로 직렬화한다.
- 클라이언트(React)는 `code` 로 분기·비교하고 `description` 으로 표시한다. 프론트에 한글 문구를 중복 정의하지 않는다.

### 서버 작성 형태

```java
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum RoleType implements DescriptionCode {

    USER("금융소비자"),
    COMPANY("등록 업체"),
    ADMIN("관리자"),
    SYSTEM("시스템 관리자");

    private final String description;

    @Override
    public String getDescription() {
        return this.description;
    }
}
```

`DescriptionCode` 의 `getCode()` 는 기본 구현이 `Enum.name()` 을 돌려주므로 enum 쪽에서 따로 구현하지 않는다.

### 클라이언트 타입 형태

```ts
// shared/types/common.ts
export interface CodeLabel<C extends string = string> {
  code: C
  description: string
}

// shared/api/member/memberService.ts
export type RoleCode = 'USER' | 'COMPANY' | 'ADMIN' | 'SYSTEM'

if (member.roleType.code === 'COMPANY') { ... }    // 비교는 code 로만
<span>{member.roleType.description}</span>        // 표시는 description 으로만
```

### 주의

- **요청(입력) 방향**: 클라이언트가 enum 값을 보낼 때는 `"COMPANY"` 문자열 하나만 보낸다. `@JsonFormat(shape = OBJECT)` 는 직렬화에만 영향을 준다.
- **JPA 저장**: `@Enumerated(EnumType.STRING)` 로 `code` 가 저장된다. `description` 은 DB 에 넣지 않는다.

---

## 2. 예외

### 위치

| 종류 | 패키지 | 예 |
|---|---|---|
| 도메인 규칙 위반 | 각 도메인의 `domain.exception` | `IllegalMembershipStateException` (예정) |
| 계층 공통 (조회 실패, 권한, 입력 형식) | core `exception.BusinessException` + `type.common.ErrorCode` | `throw new BusinessException(ErrorCode.NOT_FOUND_MEMBER)`. 예외 클래스를 늘리지 않고 ErrorCode 항목을 늘린다 |
| 외부 연동 실패 | 어댑터 패키지 안에서 잡아 포트 규약의 예외로 바꿔 던짐 | Spring AI 오류 → `LlmUnavailableException` (예정) |

### 형태

- 모두 `RuntimeException` 계열(unchecked). `@Transactional` 안에서 던지면 자동 롤백된다.
- 호출부가 **타입으로 분기**한다. 메시지 문자열을 파싱하지 않는다.
- API 계층의 변환 규칙은 예외 클래스 Javadoc 에 적고, `GlobalExceptionHandler` 에 핸들러를 더한다.
- 시큐리티 단계(인증·인가)에서 나는 401/403 은 컨트롤러까지 오지 않으므로 7절 규칙을 따른다.

---

## 3. 요구사항·화면 ID와 주석

- 요구사항 ID `FR-XX-NNN`, 화면 ID `SCR-NN` 은 domain-design.md §9 표를 기준으로 한다.
- 클래스·메서드 Javadoc, 컨트롤러 라우트 이름, 테스트 이름에 ID 를 남긴다.
- 컨트롤러는 `@Tag` 의 `description` 에 SCR ID, `@Operation` 의 `description` 맨 앞에 FR ID 를 둔다 (8절).
- ID 가 없는 기능을 구현하게 되면 domain-design.md 표에 먼저 행을 추가한다.

### 주석

- 코드만 봐서는 알 수 없는 이유·규약·주의점이 있는 곳에만 단다.
- 한두 줄로 짧게. 긴 설명은 md 문서에 두고 주석에는 문서 위치(`md/todo.md §2` 등)만 적는다.
- 날짜를 넣지 않는다. 언제·누가 바꿨는지는 git 이 기록한다.
- Javadoc·yml 주석에 HTML 태그(`<p>`, `<ul>`, `<li>`, `<pre>`)를 쓰지 않는다.

---

## 4. 의존성 결정 기록

| 라이브러리 | 결정 | 이유 |
|---|---|---|
| jjwt 0.13.0 | `jjwt-api` + `jjwt-impl` + `jjwt-gson` | `jjwt-jackson` 이 Jackson 2 에 의존. Boot 4.1 은 Jackson 3 라 두 Jackson 이 classpath 에 공존하게 됨 |
| Testcontainers 2.x | `testcontainers-mysql`, `testcontainers-junit-jupiter` | Boot 4.1.2 가 버전을 관리. 클래스가 1.x 와 다름 (`org.testcontainers.mysql.MySQLContainer`) |
| springdoc-openapi 3.2.0 | `springdoc-openapi-starter-webmvc-ui`, server 에서 `implementation` | Boot 4 라인. 내부 swagger-core 가 Jackson 2 를 가져오지만 패키지가 달라 우리 응답(Jackson 3)과 충돌하지 않는다 |
| node-gradle 7.1.0 | client 모듈에서 `npmBuild` | 빌드 PC 에 Node 가 없어도 jar 를 만들 수 있게 |
| 뺀 것: `spring-boot-starter-session-jdbc`, `spring-boot-starter-hateoas` | 처음 생성된 단일 모듈에 있던 의존성 | 세션을 쓰지 않는 STATELESS JWT 구조, 응답은 DTO 그대로 (7절) |

새 의존성을 넣을 때 이 표에 행을 더한다. Spring AI, Redis, RabbitMQ 는 쓰는 기능이 생길 때 넣는다.

---

## 5. 모듈 구조

```
fin-broker (루트 build.gradle: 플러그인 버전 + Java 모듈 공통 설정만)
 +- common/core   java-library. 도메인(member ...), 공통 설정·yml. bootJar 끔
 +- server        Boot 실행 jar. API·보안·화면(client dist 포함). core 의존
 +- batch         Boot 실행 jar. 배치 작업. core 의존. server 는 의존하지 않는다
 +- client        Vite + React (node-gradle). server 의 bootJar 가 dist 를 static/ 으로 담는다
```

- 패키지 이름은 모듈과 무관하게 domain-design.md §11 을 따른다. 앱 클래스(`ServerApplication`, `BatchApplication`)는 `com.marinbom2.finbroker` 루트에 둬서 core 의 엔티티·설정까지 스캔한다.
- 설정 파일 `application*.yml` 은 core 에만 둔다. server·batch 에 같은 이름의 파일을 두면 클래스패스에서 하나만 읽혀 core 설정이 사라진다. 앱 이름처럼 모듈마다 다른 값은 main 클래스의 `setDefaultProperties` 로 넣는다.
- core 는 도메인이 드러내는 JPA·Jackson 어노테이션과 `spring-security-core` 만 `api` 로 내보낸다. 외부 SDK 는 `implementation` 으로 두어 server·batch 에서 import 할 수 없게 한다.
- 빌드는 루트에서 한다. 화면을 어디에 담는지와 `-PskipClient`·`-PwithClient` 플래그는 [runtime-structure.md](./runtime-structure.md) §3.

```bash
./gradlew build -PskipClient
```

### 포트 구현체 규칙

- 구현체는 포트 규약을 똑같이 지킨다. 규약은 포트 Javadoc 에 적고, 추상 규약 테스트 하나를 모든 구현체가 상속해 돌린다 (solid.md L).
- 구현체 선택은 설정 클래스 하나에서 `@ConditionalOnProperty` 로 한다.
- 저장 키에는 원본 파일명을 넣지 않는다. `prefix/uuid.확장자` 만 쓴다. 파일명에 개인정보가 들어 있을 수 있다.

### enum 칼럼 스키마

- `NoColumnCheckMySQLDialect` 가 `@Enumerated(STRING)` 칼럼을 varchar 로 만들고 값 목록 CHECK 제약을 만들지 않는다. Hibernate 기본은 MySQL 네이티브 `enum(...)` 이라, ddl-auto=update 에서 enum 값을 추가하면 새 값 저장이 막힌다.
- server 의 `EnumColumnSchemaTest` 가 실제 MySQL 스키마로 이를 검증한다.

---

## 6. 비밀 값

| 값 | 로컬 | 운영 |
|---|---|---|
| JWT 서명 키 | 루트 `secrets/application-secret.yml` (`jwt.secret`). 비우면 기동마다 임시 키 | `JWT_SECRET` (없으면 기동 실패) |
| DB 접속 | `application-local.yml` (docker-compose 값, 비밀 아님) | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| 개발용 백도어 이메일 | 루트 `secrets/application-secret.yml` (`security.login-bypass.emails`) | 쓰지 않음 (local 프로필에서만 동작) |

```yaml
# secrets/application-secret.yml (로컬 예시)
jwt:
  secret: local-only-secret-at-least-32-bytes-long
security:
  login-bypass:
    emails:
      - me@example.com
```

- `secrets/` 는 gitignore 이고, server·batch 의 `bootRun` 클래스패스에만 붙는다. jar 에는 들어가지 않는다.
- 테스트·CI 에는 키가 없다. 테스트는 `@SpringBootTest(properties = "jwt.secret=...")` 로 넣는다.
- 새 비밀 값이 생기면 이 표에 행을 추가하고 `application-prod.yml` 머리 주석의 환경변수 목록도 고친다.

---

## 7. API 응답과 오류 (ProblemDetail)

공통 응답 봉투(`ApiResponse`)를 쓰지 않는다. **성공은 DTO 그대로, 오류는 RFC 9457 Problem Details** 로 응답한다.

### 형식

| 경우 | HTTP 상태 | 본문 |
|---|---|---|
| 성공 | 2xx | DTO 그대로. 본문이 없으면 204 |
| 오류 | 4xx·5xx | `application/problem+json`. `ProblemDetail` + 확장 필드 `code` |

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "로그인이 필요합니다.",
  "instance": "/api/v1/me",
  "code": "UNAUTHORIZED"
}
```

- `detail` 은 화면에 그대로 나가는 문장이다. 내부 정보(ID, 예외 원문, 쿼리, 스택)를 넣지 않는다.
- `code` 는 서버 `ErrorCode` 이름이다. 클라이언트는 문구가 아니라 이 값으로 분기한다.
- 입력값 검증 실패는 `errors: [{field, message}]` 를 함께 보낸다.

### 서버 구성

| 클래스 | 모듈·패키지 | 역할 |
|---|---|---|
| `ErrorCode` | core `type.common` | 오류 목록. HTTP 상태, `code`(이름), 기본 문구(`description`) |
| `BusinessException` | core `exception` | 애플리케이션 계층 공통 예외 |
| `ProblemDetails` | server `exception` | `ErrorCode` 로 `ProblemDetail` 을 만드는 **유일한 곳** |
| `GlobalExceptionHandler` | server `exception` | 컨트롤러 단계 오류 전부. `ResponseEntityExceptionHandler` 를 상속해 Spring MVC 예외도 같은 형식에 `code` 와 한글 문구를 입힌다 |
| `SecurityErrorResponder` | server `security` | 시큐리티 필터 단계의 401/403/로그인 실패 |

| 예외 | ErrorCode | 비고 |
|---|---|---|
| `BusinessException` | 담긴 값 | 메시지가 `detail` 이 된다 |
| `@Valid` 실패 | `INVALID_REQUEST_DATA` (400) | `errors` 추가 |
| Spring MVC 예외 | 상태별 기본값 (`ErrorCode.defaultFor`) | Spring 의 영문 `detail` 은 로그로만 |
| 그 밖의 예외 | `INTERNAL_ERROR` (500) | 스택은 로그로만 |

### 시큐리티 단계

```
Tomcat -> 필터 체인 (FilterChainProxy)        <- 401/403 이 여기서 난다
       -> DispatcherServlet -> 컨트롤러
       -> @RestControllerAdvice               <- 위 오류는 여기까지 오지 않는다
```

| 규칙 | 이유 |
|---|---|
| 401·403·로그인 실패를 **한 빈** `SecurityErrorResponder` 가 구현한다 | 시큐리티 오류 JSON 을 쓰는 곳이 하나 |
| Boot 가 만든 `JsonMapper`(Jackson 3)를 **주입받는다** | 매퍼를 따로 만들면 컨트롤러 응답과 설정이 달라진다 |
| 권한 부족은 반드시 **403** | 클라이언트는 401 을 세션 만료로 보고 로그아웃시킨다 |
| JWT 필터는 응답을 **직접 쓰지 않는다** | 공개 경로는 잘못된 토큰이 붙어 와도 정상 동작한다 |
| 시큐리티 체인은 `/api/**` 만 맡는다 | 헬스체크가 401 이면 로드밸런서 헬스체크가 실패한다 |

### 클라이언트

| 파일 | 역할 |
|---|---|
| `shared/types/common.ts` | `ProblemDetail`, `FieldError`, `CodeLabel` 타입 (서버와 1:1) |
| `shared/core/apiClient.ts` 의 `apiRequest` | 모든 호출의 공통 수신부. 2xx 면 DTO 그대로, 아니면 `ApiError` 를 던진다. 401 이면 세션 만료를 알린다 |
| `ApiError` | `message`(= `detail`, 화면용), `status`, `code`(분기용), `fieldErrors`, 원본 `problem` |

### 테스트

- `GlobalExceptionHandlerTest`, `SecurityErrorResponderTest`, `AuthFlowTest` (authentication-security.md §11).

---

## 8. 컨트롤러와 API 경로 (Swagger)

### 경로

| 대상 | 경로 | 예 |
|---|---|---|
| 사용자 API | `/api/v1/{리소스}` | `/api/v1/posts`, `/api/v1/companies/{companyId}` |
| 인증 | `/api/v1/auth/...` | `/api/v1/auth/login`, `/api/v1/auth/logout` |
| 내 정보 | `/api/v1/me` | |
| 운영자 API (SCR-08) | `/api/v1/ops/...` | `/api/v1/ops/company-memberships` |

- 모든 API 는 `/api/` 로 시작한다. Vite 프록시와 시큐리티 규칙이 이 접두사를 기준으로 한다.
- 리소스는 복수 명사, 여러 단어면 kebab-case (`/company-memberships`). 동작은 HTTP 메서드로 나타낸다.
- 컨트롤러 클래스에 `@RequestMapping("/api/v1/...")` 를 두고, 메서드에는 나머지 경로만 쓴다.
- 성공은 DTO 그대로 (`201`·`204` 는 `@ResponseStatus` 로), 오류는 `BusinessException` 을 던져 ProblemDetail 로.

### Swagger 규칙

| 붙이는 곳 | 어노테이션 | 쓰는 법 |
|---|---|---|
| 컨트롤러 클래스 | `@Tag(name, description)` | `name` 은 화면 기준 한글 이름, `description` 에 SCR ID |
| 컨트롤러 메서드 | `@Operation(summary, description)` | `description` 은 **FR ID 로 시작** |
| 엔드포인트 고유 오류 | `@ApiResponse(responseCode, description, content)` | `description` 에 `ErrorCode` 이름. 공통 오류(401·403·500)는 적지 않는다 |
| 요청·응답 DTO 필드 | `@Schema(description, example)` | `example` 에 실제 개인정보·실제 등록번호를 쓰지 않는다 |

- 문서에는 `/api/**` 만 나온다 (`springdoc.paths-to-match`). 로컬은 `http://localhost:9090/swagger-ui.html`, 운영은 끈다.

### 뼈대

```java
@Tag(name = "커뮤니티", description = "글·답변 (SCR-02~04)")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "글 작성", description = "FR-CM-001. 상품 유형과 글 유형을 골라 글을 쓴다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@AuthenticationPrincipal Member member, @Valid @RequestBody PostCreateRequest request) {
        return PostResponse.from(postService.create(member.getId(), request.toCommand()));
    }
}
```

---

## 9. 작업 순서

1. 새 영역(업체 조회, 커뮤니티 등)은 **뼈대 먼저**: 패키지, 클래스 이름, 포트 시그니처, 상태 전이.
2. 뼈대를 리뷰한 뒤 구현을 채운다.
3. 단위 테스트는 인프라 없이 돌아가는 도메인 테스트를 먼저, 통합 테스트(Testcontainers)는 어댑터가 생긴 뒤.

---

## 코드 리뷰 체크리스트

- [ ] 새 enum 이 `DescriptionCode` 를 구현하고 `@JsonFormat(shape = OBJECT)` 가 붙었는가
- [ ] 화면 문구·`description` 에 단정 표현이 없는가
- [ ] 오류를 `BusinessException` + `ErrorCode` 로 던지는가 (응답 봉투를 만들지 않았는가)
- [ ] 컨트롤러 경로가 `/api/v1/` 로 시작하고 `@Operation` 이 FR ID 로 시작하는가
- [ ] 비밀 값이 `src/main/resources` 에 들어가지 않았는가
- [ ] server·batch 가 서로 의존하지 않는가
