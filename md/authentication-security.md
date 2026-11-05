# 인증과 시큐리티 — Spring Security 7 (Spring Boot 4) 구조

> 목적: fin-broker 의 로그인·인증·인가가 **어디서 어떤 순서로** 일어나는지, Boot 기본 설정과 우리가 등록한 빈이 어떻게 맞물리는지 정리한다.
> 기준 버전: Spring Boot 4.1.2, Spring Security 7. 아래 필터 순서는 `AuthFlowTest.filterChainOrder` 가 실행 중인 컨텍스트에서 꺼낸 값이다.
> 바탕: 계약봄(gyeyakbom)의 회원 인증 구조. 로그인 필터·프로바이더·핸들러·JWT 필터·쿠키 전담 클래스 구성을 그대로 따르고, 이 서비스에 맞게 줄이고 바꿨다 (10절).
> 관련 문서: [develop-principal.md](./develop-principal.md) §7·§8, [runtime-structure.md](./runtime-structure.md), [domain-design.md](./domain-design.md) §5.1

## 한눈에 보기

| 항목 | 결정 |
|---|---|
| 로그인 | `POST /api/v1/auth/login` JSON `{email, password}`. 컨트롤러가 아니라 **시큐리티 필터**(`MemberAuthenticationAPIFilter`)가 처리 |
| 인증 주체 | `Member` 엔티티가 `UserDetails` 를 구현해 그대로 principal 이 된다 |
| 토큰 | JWT(HS256), **HttpOnly 쿠키** `finbroker_at`, 유효 12시간. claim 은 회원 ID 와 토큰 버전뿐 |
| 로그아웃 | `Member.tokenVersion` 을 1 올려 그 전에 발급한 토큰을 전부 무효화 |
| 권한 | 토큰이 아니라 요청마다 DB 의 현재 값. `USER`(금융소비자), `COMPANY`(등록 업체 담당자), `ADMIN`, `SYSTEM` (ROLE_ 접두사 없음) |
| 세션 | 없음 (STATELESS) |
| CSRF | 토큰 끔. SameSite=Lax 쿠키 + 같은 오리진 + CORS 없음으로 막는다 (6절) |
| 오류 | 401·403·로그인 실패 모두 ProblemDetail + `code` (`SecurityErrorResponder`) |
| 시큐리티가 맡는 경로 | `/api/**`만. 화면, `/actuator/health`, Swagger 는 체인을 타지 않는다 |

---

## 1. Boot 3 → Boot 4 에서 달라진 점

Boot 3 은 Spring Security 6, Boot 4 는 Spring Security 7 이다. Boot 3 시절 코드를 옮길 때 걸리는 곳은 아래다.

| 항목 | Boot 3 (Security 6) | Boot 4 (Security 7) |
|---|---|---|
| 설정 체이닝 | `.and()`로 이어 쓰기 (6.1부터 deprecated) | **제거**. 람다 DSL 만 (`http.csrf(c -> c.disable())`) |
| 인가 설정 | `authorizeRequests()` (deprecated) | **제거**. `authorizeHttpRequests()`만 |
| 경로 매처 | `AntPathRequestMatcher`, `MvcRequestMatcher` | **제거**. `PathPatternRequestMatcher`만 |
| JSON | `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2) | `tools.jackson.databind.json.JsonMapper` (Jackson 3) |
| `UserDetails` | 일부 default 메서드 | `isAccountNonExpired` 등이 default(true). `getAuthorities`·`getPassword`·`getUsername`만 구현하면 된다 |
| jjwt + Jackson | `jjwt-jackson` | `jjwt-jackson`은 Jackson 2 를 끌고 온다. fin-broker 는 `jjwt-gson` (develop-principal §4) |

그대로인 것: `AbstractAuthenticationProcessingFilter`, `OncePerRequestFilter`, `ProviderManager`, `AuthenticationProvider`, `UserDetailsService`, `SecurityContextHolder`.

---

## 2. 요청이 시큐리티에 들어가는 길

```
Tomcat (서블릿 컨테이너)
  |  서블릿 필터 목록
  +- DelegatingFilterProxy  "springSecurityFilterChain"
  |     |  스프링 빈이 아닌 서블릿 필터. 이름이 같은 빈을 컨텍스트에서 찾아 위임만 한다
  |     v
  |   FilterChainProxy
  |     +- apiSecurityFilterChain  securityMatcher("/api/**")   <- 우리가 등록한 유일한 체인
  |     |     DisableEncodeUrlFilter > WebAsyncManagerIntegrationFilter > SecurityContextHolderFilter
  |     |     > HeaderWriterFilter > JwtAuthenticationFilter > MemberAuthenticationAPIFilter
  |     |     > SecurityContextHolderAwareRequestFilter > AnonymousAuthenticationFilter
  |     |     > SessionManagementFilter > ExceptionTranslationFilter > AuthorizationFilter
  |     +- (맞는 체인이 없으면 시큐리티 필터를 하나도 거치지 않고 통과)
  v
DispatcherServlet -> 컨트롤러 -> @RestControllerAdvice
```

1. 서블릿 컨테이너에 등록되는 것은 `DelegatingFilterProxy` 하나다. 실제 일은 컨텍스트 안의 `FilterChainProxy` 빈에게 넘긴다.
2. `FilterChainProxy` 가 요청마다 체인을 고른다. 우리 체인은 `/api/**`만 맡으므로 화면(`/`, `/assets/**`), `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs` 는 시큐리티를 전혀 거치지 않는다.
3. **필터 순서는 실제 값이다.** 순서가 바뀌면 `AuthFlowTest.filterChainOrder` 가 깨진다.

꺼 둔 필터: `CsrfFilter`(6절), `LogoutFilter`(로그아웃은 컨트롤러), `UsernamePasswordAuthenticationFilter`(폼 로그인), `BasicAuthenticationFilter`, `RequestCacheAwareFilter`(켜 두면 401 때 요청을 세션에 저장하려 한다).

---

## 3. 기본 빈과 우리가 등록한 빈

| Boot 기본 | 만드는 조건 | 우리가 등록한 빈 | 결과 |
|---|---|---|---|
| `defaultSecurityFilterChain` (모든 요청 인증 + 폼 로그인 + HTTP Basic) | `SecurityFilterChain` 빈이 없을 때 | `apiSecurityFilterChain` | 물러남. 체인이 우리 것 하나뿐 (테스트로 확인) |
| `InMemoryUserDetailsManager` + 로그에 찍히는 임시 비밀번호 | `AuthenticationManager`·`UserDetailsService` 등의 빈이 없을 때 | `memberAuthenticationManager`, `memberDetailsService` | 물러남. 테스트 로그에 "generated security password" 0건 |
| `PasswordEncoder` | 없음 | `passwordEncoder` (DelegatingPasswordEncoder, 해시 앞에 `{bcrypt}`) | 가입·로그인이 같은 인코더를 쓴다 |
| JSON 매퍼 | `JacksonAutoConfiguration` | (Boot 것을 주입받아 씀) | 로그인 필터·성공 핸들러·오류 응답이 컨트롤러와 같은 설정으로 JSON 을 읽고 쓴다 |

주의할 점:

- **필터를 `@Component`로 만들지 않는다.** Boot 는 `Filter` 타입 빈을 서블릿 필터로도 자동 등록한다. 빈이면 시큐리티 체인 안에서 한 번, 밖에서 한 번 더 돈다. 그래서 `SecurityConfig` 에서 `new` 로 만들어 체인에만 넣는다.
- **`SecurityFilterChain` 을 두 개 이상 만들 때**(예: 소셜 로그인)는 `securityMatcher` 가 겹치지 않게 한다.

---

## 4. 로그인: 인증 매니저와 프로바이더

```
POST /api/v1/auth/login {"email","password"}
  |
MemberAuthenticationAPIFilter.attemptAuthentication      (AbstractAuthenticationProcessingFilter)
  |  Content-Type 확인, JSON 읽기, 이메일 정규화(Member.normalizeEmail)
  |  -> MemberAuthenticationToken.unauthenticated(email, password)
  v
AuthenticationManager = ProviderManager                   (SecurityConfig.memberAuthenticationManager 빈)
  |  supports(토큰 타입)가 true 인 프로바이더에게 맡긴다
  v
MemberAuthenticationProvider.authenticate
  |  MemberDetailsService.loadUserByUsername(email) -> Member (UserDetails)
  |  비밀번호 비교 (local 백도어 목록이면 건너뜀, 8절)
  |  -> UsernamePasswordAuthenticationToken.authenticated(member, null, authorities)
  v
성공: MemberLoginSuccessHandler          실패: SecurityErrorResponder (AuthenticationFailureHandler)
  JWT 발급 -> HttpOnly 쿠키                  JSON 형식 오류 -> 400 INVALID_REQUEST_DATA
  본문 MemberResponse (200)                  계정·비밀번호 불일치 -> 401 LOGIN_FAILED
```

- **매니저를 직접 빈으로 만드는 이유**: 로그인 필터가 쓰는 매니저와 그 안의 프로바이더가 정확히 우리 것이어야 한다.
- **토큰 타입을 따로 두는 이유**: `ProviderManager` 가 `supports()` 로 이 토큰을 `MemberAuthenticationProvider` 에만 보낸다. 다른 인증 방식(예: 소셜 로그인, 업체 담당자 회사 메일 인증)을 추가하면 토큰 타입과 프로바이더를 하나씩 더하면 된다 (solid.md O).
- **계정 존재 여부를 숨긴다**: 없는 이메일과 비밀번호 불일치를 같은 `BadCredentialsException`(→ 401 `LOGIN_FAILED`)으로 낸다. 없는 이메일일 때도 해시 비교를 한 번 해서 응답 시간으로 구분되지 않게 한다.
- **로그인이 Swagger 에 보이게**: 필터라 springdoc 이 찾지 못하므로 `OpenApiConfig` 가 경로를 직접 적어 넣는다.

---

## 5. 요청마다 인증: JWT 필터와 토큰 무효화

```
JwtAuthenticationFilter (OncePerRequestFilter)
  토큰 꺼내기: 쿠키 finbroker_at 우선, 없으면 Authorization: Bearer
    없음        -> 아무것도 안 하고 다음 필터 (공개 경로는 그대로 통과, 보호 경로는 뒤에서 401)
    있음        -> JwtTokenProvider.parse: 서명·만료 검증
                   실패 -> 요청 속성에 INVALID_ACCESS_TOKEN 남기고 다음 필터
                   성공 -> DB 에서 회원 조회 + tokenVersion 비교
                           불일치·없음 -> INVALID_ACCESS_TOKEN 남기고 다음 필터
                           일치 -> SecurityContext 에 Member 를 principal 로 넣음
ExceptionTranslationFilter / AuthorizationFilter
  인증 없음 + 보호 경로 -> SecurityErrorResponder.commence -> 401 (남긴 이유가 있으면 그 code)
  권한 부족            -> SecurityErrorResponder.handle   -> 403 FORBIDDEN
```

| 결정 | 이유 |
|---|---|
| 필터는 응답을 쓰지 않는다 | 응답을 쓰는 곳을 `SecurityErrorResponder` 하나로 둔다. 공개 경로는 만료된 쿠키가 붙어 와도 정상 동작한다 |
| 토큰에는 회원 ID 와 토큰 버전만 | 서명만 하고 암호화하지 않으므로 노출돼도 되는 값만. 이메일·권한을 넣지 않는다 |
| 권한은 요청마다 DB 에서 | 업체 담당자 승인·해제(domain-design.md §4.1)가 같은 토큰으로 바로 반영된다 (`AuthFlowTest.opsAuthorization`). 대신 요청마다 회원을 기본키로 한 번 읽는다. 늘면 Redis 캐시를 둔다 |
| 로그아웃은 tokenVersion + 1 | 토큰 저장 테이블 없이 회원의 버전 숫자 하나로 폐기한다. 여러 기기 동시 로그인이 되고, 로그아웃하면 모든 기기에서 풀린다 |
| 운영에서 서명 키 필수 | `prod` 프로필에서 `jwt.secret` 이 비면 기동을 멈춘다 (`JwtConfig`). 임시 키로 뜨면 인스턴스마다 키가 달라 로그인이 풀린다 |

principal 로 들어가는 `Member` 는 트랜잭션 밖에서 읽은 준영속 객체다. 값을 바꿀 때는 ID 로 다시 읽어 서비스의 트랜잭션 안에서 바꾼다 (`MemberService`). 컨트롤러에서는 `@AuthenticationPrincipal Member member` 로 받는다.

---

## 6. CSRF와 쿠키

인증을 쿠키로 하므로 CSRF(다른 사이트가 사용자 브라우저로 우리 API 를 부르게 하는 공격)를 따져야 한다. CSRF 토큰은 끄고 아래 세 가지로 막는다.

| 장치 | 효과 |
|---|---|
| 인증 쿠키 `SameSite=Lax` | 다른 사이트에서 시작된 POST·PATCH·DELETE 요청에는 브라우저가 쿠키를 싣지 않는다. 쿠키 없이 오면 401 이다 |
| 상태를 바꾸는 GET 을 만들지 않는다 | Lax 는 다른 사이트에서 링크로 들어오는 GET 에는 쿠키를 싣는다. 그래서 GET 은 조회만 한다 |
| 같은 오리진 + CORS 설정 없음 | 화면과 API 가 한 오리진이라 CORS 를 열지 않는다 (runtime-structure.md §4) |

- 쿠키 속성: `HttpOnly`(스크립트에서 못 읽음), `Secure`(운영 true, local 은 http 라 false), `Path=/`, `Max-Age`=토큰 유효 기간, 도메인 지정 없음(host-only). 쿠키를 넣고 빼는 곳은 `AccessTokenCookie` 하나다.
- 프론트가 JWT 를 `localStorage` 에 두고 `Authorization` 헤더로 보내는 방식은 쓰지 않는다. XSS 한 번이면 토큰이 털린다. Bearer 헤더는 스크립트·Swagger 같은 비브라우저 호출용으로만 받는다.
- 다른 하위 도메인에서 쿠키를 공유하거나, 상태를 바꾸는 GET 이 필요해지면 `csrf.spa()` 를 켜고 클라이언트가 `X-XSRF-TOKEN` 헤더를 보내게 바꾼다.

---

## 7. 오류 응답

시큐리티 단계의 오류는 컨트롤러까지 가지 않으므로 `SecurityErrorResponder` 한 빈이 세 자리에 꽂혀 ProblemDetail 을 쓴다 (develop-principal §7).

| 자리 | 상황 | 응답 |
|---|---|---|
| `AuthenticationEntryPoint` | 인증 없이 보호 경로 | 401 `UNAUTHORIZED`, JWT 필터가 남긴 이유가 있으면 401 `INVALID_ACCESS_TOKEN` |
| `AccessDeniedHandler` | 권한 부족 | 403 `FORBIDDEN` (401 이면 클라이언트가 로그아웃시키므로 반드시 403) |
| `AuthenticationFailureHandler` | 로그인 실패 | 400 `INVALID_REQUEST_DATA`(형식) / 401 `LOGIN_FAILED`(계정·비밀번호) |

성공 응답(로그인 본문)은 `MemberLoginSuccessHandler` 가 쓴다. 두 곳 모두 Boot 의 `JsonMapper` 를 주입받아 쓴다.

---

## 8. 개발용 백도어와 권한 바꾸기

| 기능 | 설정 (기본값) | 켜지는 조건 |
|---|---|---|
| 비밀번호 없이 로그인 | `security.login-bypass.emails` (빈 목록) | **local 프로필**이면서 목록에 이메일이 있을 때. 다른 프로필이면 목록이 있어도 무시하고 경고 로그 |
| 내 권한 바꾸기 `PATCH /api/v1/me/role` | `security.role.self-change-allowed` (false, `application-local.yml` 에서 true) | 값이 true 일 때. 꺼진 환경에서는 403 `FORBIDDEN` |

- 백도어 이메일은 gitignore 된 `secrets/application-secret.yml` 에 둔다. 개인 이메일이 저장소에 남지 않는다.
- 권한 바꾸기는 업체(`COMPANY`)·운영자 화면을 로컬에서 확인하려는 것이다. 운영에서 `COMPANY` 권한은 업체 담당자 인증(domain-design.md §4.1)으로만 얻는다.
- 백도어가 켜지면 기동 로그와 로그인마다 경고를 남긴다.

---

## 9. 클래스 목록

| 클래스 | 모듈·패키지 | 역할 |
|---|---|---|
| `Member`, `RoleType` | core `member.domain` | 회원 엔티티 + `UserDetails`. 이메일 정규화, 토큰 버전 |
| `BaseEntity` | core `domain.base` | `created_at`, `modified_at` (JPA Auditing, `JpaAuditingConfig`) |
| `MemberRepository` | core `member.domain` | `findByEmail`, `existsByEmail` |
| `SecurityConfig` | server `security` | 필터 체인, 인증 매니저, `UserDetailsService`, `PasswordEncoder` |
| `MemberAuthenticationAPIFilter` | server `security.filter` | JSON 로그인 |
| `MemberAuthenticationToken` | server `security.token` | 로그인 요청 토큰 |
| `MemberAuthenticationProvider` | server `security.provider` | 비밀번호 검증, 백도어 |
| `MemberDetailsService` | server `security.details` | 이메일로 `Member` 조회 |
| `MemberLoginSuccessHandler` | server `security.handler` | JWT 쿠키 + 회원 정보 본문 |
| `JwtAuthenticationFilter` | server `security.filter` | 요청마다 토큰 확인 |
| `JwtTokenProvider`, `JwtProperties`, `JwtConfig`, `AccessTokenClaims` | server `security.jwt` | 발급·검증, 설정, 운영 키 확인 |
| `AccessTokenCookie` | server `security.cookie` | 쿠키 발급·삭제·읽기 |
| `SecurityErrorResponder` | server `security` | 401·403·로그인 실패 ProblemDetail |
| `MemberSecurityProperties` | server `security` | 백도어, 권한 바꾸기 설정 |
| `AuthController`, `MeController`, `MemberService` | server `member.api`, `member.application` | 가입, 로그아웃, 내 정보, 권한 바꾸기 |

API:

| 메서드 | 경로 | 인증 | FR |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 없음 | FR-AU-001 |
| POST | `/api/v1/auth/login` | 없음 (필터) | FR-AU-002 |
| POST | `/api/v1/auth/logout` | 없음 (있으면 토큰 무효화, 항상 쿠키 삭제) | FR-AU-002 |
| GET | `/api/v1/me` | 필요 | FR-AU-002 |
| PATCH | `/api/v1/me/role` | 필요 (local 설정에서만) | FR-AU-003 |
| * | `/api/v1/ops/**` | `ADMIN`, `SYSTEM` | FR-AU-003, SCR-08 |

새 공개 경로(예: 비로그인 글 읽기)가 생기면 `SecurityConfig.PERMIT_ALL_POST` 옆에 GET 허용 목록을 만들어 더한다. 업체 담당자 전용 API 가 생기면 `hasAuthority("COMPANY")` 경로를 더한다.

---

## 10. 계약봄과 달라진 점

| 항목 | 계약봄 | fin-broker | 이유 |
|---|---|---|---|
| 패키지 | `com.gyeyakbom` | `com.marinbom2.finbroker` | 저장소 기존 패키지 |
| 쿠키 이름 | `gyeyakbom_at` | `finbroker_at` | |
| 역할 | `USER`, `ADMIN`, `SYSTEM` | + `COMPANY`(등록 업체 담당자) | domain-design.md §2 |
| 가입 | 이메일 인증번호 → 가입, 동의 세 가지 | 이메일·비밀번호만 | 메일 서버·약관이 정해지면 추가 (todo.md §2) |
| 소셜 로그인 | 카카오·네이버 체인 (`security.oauth2`) | 옮기지 않음 | 필요해지면 두 번째 체인으로 추가 (3절 주의) |
| `MemberService.signUp` 인자 | API DTO | `email`, `password` 값 | application 이 api 패키지를 모르게 (solid.md D) |
| `AccessUserInfo`, `RoleType.isAdmin()` | 있음 | 옮기지 않음 | 호출부가 없다 (solid.md I) |
| `application-local.yml` | gitignore | 커밋 (비밀 값 없음, DB 는 docker-compose 값) | 받자마자 실행 가능하게. 비밀 값은 계속 `secrets/` |

---

## 11. 테스트

| 테스트 | 확인하는 것 |
|---|---|
| `MemberTest` (core) | 이메일 정규화, 기본 권한, 토큰 버전, `UserDetails` 구현 |
| `JwtTokenProviderTest` | 발급·검증 왕복, 만료, 위조·다른 키, 짧은 키 기동 실패, 빈 키 임시 동작 |
| `MemberAuthenticationProviderTest` | 성공, 계정 없음과 비밀번호 불일치가 같은 예외, 백도어, `supports` |
| `SecurityErrorResponderTest` | 401·403 ProblemDetail, JWT 필터가 남긴 이유 반영 |
| `GlobalExceptionHandlerTest` | 컨트롤러 단계 ProblemDetail, `@Valid` errors, Spring MVC 예외, 500 |
| `AuthFlowTest` (MySQL Testcontainers) | 가입 → 로그인 쿠키 → 내 정보, 가입 검증(409·400), 로그인 실패(401·400), 무인증·위조 401, 운영자 API 403(USER·COMPANY) → ADMIN 통과, 로그아웃 후 토큰 거절, 체인 밖 경로(헬스체크·API 문서), **실제 필터 순서** |

Testcontainers 테스트는 Docker 데몬이 없으면 건너뛴다 (`disabledWithoutDocker = true`).

## 12. 정하지 않은 것

- [ ] 토큰 갱신(`/api/v1/auth/refresh`): 지금은 12시간 토큰 하나. 만료되면 다시 로그인
- [ ] 로그인 실패 횟수 제한(계정 잠금·요청 속도 제한, Redis)
- [ ] 비밀번호 재설정, 탈퇴
- [ ] 가입 이메일 인증, 약관·개인정보 동의 기록
- [ ] 업체 담당자 재직 확인 수단 (회사 메일, 재직 증빙)
- [ ] 회원 조회 캐시: 사용자가 늘어 요청마다 회원 조회가 부담되면 Redis
