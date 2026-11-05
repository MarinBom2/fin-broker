# 실행·빌드 구조 — 한 오리진 원칙

> 목적: 화면(React)과 API(Spring)를 **어떻게 띄우고, 어떻게 묶고, 어떻게 배포하는지**를 한곳에 정리한다. 로컬 개발은 편하게(HMR), 운영은 단순하게(jar 하나), 그리고 둘의 동작 조건은 같게 만드는 것이 목표다.
> 코드 안의 구조(도메인, 포트, 모듈)가 아니라 **실행·배포 아키텍처**, 즉 프로세스와 포트와 빌드 산출물이 어떻게 연결되는지에 대한 결정이다.
> 관련 문서: [develop-principal.md](./develop-principal.md) §5, [authentication-security.md](./authentication-security.md) §6

## 한눈에 보기

| 패턴 | fin-broker 에서 | 얻는 것 |
|---|---|---|
| **같은 오리진 서빙** (Same-Origin Serving) | 브라우저는 화면과 API 를 늘 한 주소(스킴+호스트+포트)에서 받는다 | CORS 설정이 필요 없다. HttpOnly 인증 쿠키가 그대로 동작한다 |
| **단일 배포 단위** (SPA 를 백엔드 jar 에 번들) | 빌드된 화면(client/dist)이 server.jar 의 `static/` 안에 들어간다 | 배포물이 jar 하나. 화면과 API 버전이 어긋날 일이 없다 |
| **개발 서버 프록시** (Dev Server Proxy) | 로컬에서는 Vite(9091)가 화면을 주고, `/api` 만 서버(9090)로 넘긴다 | HMR 로 즉시 확인하면서도 브라우저 입장에서는 운영과 같은 한 오리진 |

핵심은 세 실행 방식 모두 **브라우저가 보기에 오리진이 하나**라는 점이다. 그래서 로컬에서 되는 것은 운영에서도 된다.

---

## 1. 세 가지 실행 방식

| 방식 | 언제 | 브라우저 주소 | 화면을 주는 곳 | API 를 주는 곳 |
|---|---|---|---|---|
| A. 로컬 개발 (기본) | 화면·API 개발할 때 | `localhost:9091` | Vite dev 서버 (HMR) | Vite 가 9090 으로 프록시 |
| B. 로컬 통합 확인 | 배포 전에 jar 처럼 확인할 때 | `localhost:9090` | Spring (`static/`) | Spring |
| C. 운영 | 실제 서비스 | `[도메인]` | Spring (jar 안 `static/`) | Spring |

### A. 로컬 개발

```
브라우저 --> localhost:9091 (Vite dev 서버)
               |- /, /assets, /src ...   Vite 가 직접 응답 (HMR)
               +- /api/**   ------------> localhost:9090 (Spring, local 프로필)
```

```bash
# 터미널 1: DB
docker compose up -d mysql
```

```bash
# 터미널 2: 서버 (npm 빌드가 돌지 않는다)
./gradlew :server:bootRun
```

```bash
# 터미널 3: 화면
cd client && npm install && npm run dev
```

Windows 는 `./gradlew` 대신 `.\gradlew.bat`.

### B. 로컬 통합 확인

```bash
./gradlew :server:bootRun -PwithClient
```

`client` 를 npm 으로 빌드해 `static/` 에 넣고 9090 에서 화면과 API 를 같이 준다. dev 서버(A)는 끄고 돌린다 (client/build.gradle 주의).

### C. 운영

```bash
./gradlew :server:bootJar        # client dist 포함
java -jar server/build/libs/server.jar --spring.profiles.active=prod
```

필요한 환경변수는 `application-prod.yml` 머리 주석.

---

## 2. 포트

| 포트 | 무엇 | 어디서 정하나 |
|---|---|---|
| 9090 | Spring (local) | `application-local.yml` `server.port` |
| 9091 | Vite dev 서버 | `client/vite.config.ts` (`strictPort`) |
| 3306 | MySQL (docker-compose) | `docker-compose.yml` |
| 8080 | Spring (prod 기본값) | 정해지면 `application-prod.yml` |

---

## 3. Gradle 플래그

| 명령 | 화면 포함 | 언제 |
|---|---|---|
| `./gradlew :server:bootJar` | 포함 (`:client:npmBuild` 가 먼저 돈다) | 배포 jar |
| `./gradlew build -PskipClient` | 빼고 | 서버만 빨리 빌드·테스트 |
| `./gradlew :server:bootRun` | 빼고 | 평소 로컬 (A) |
| `./gradlew :server:bootRun -PwithClient` | 포함 | 로컬 통합 확인 (B) |

- `:client:npmBuild` 는 node-gradle 플러그인이 Node 22 를 직접 받아 `npm install` → `npm run build`(tsc + vite) 를 돈다. 빌드 PC 에 Node 가 없어도 된다.
- 소스가 그대로면 `npmBuild` 는 Gradle 이 건너뛴다 (`inputs`/`outputs`).

---

## 4. 같은 오리진이라서 생기는 이점

### CORS 가 필요 없다

브라우저는 화면을 받은 오리진과 API 오리진이 같으면 CORS 검사를 하지 않는다. 서버에 CORS 설정 빈이 없다.

### 인증 쿠키가 운영과 똑같이 동작한다

`finbroker_at` 쿠키는 `HttpOnly`, `SameSite=Lax`, host-only 다. 다른 오리진(예: 화면 5173, API 8080)이면 쿠키가 실리지 않거나 CORS credentials 설정이 필요해진다. 프록시 덕분에 로컬도 한 오리진이라 운영과 같은 쿠키 설정을 쓴다 (local 은 http 라 `Secure=false` 만 다르다).

### 화면과 API 버전이 늘 같다

같은 jar 에서 나가므로 화면이 모르는 API 가 먼저 배포되는 일이 없다.

---

## 5. 프론트가 지킬 것

- API 는 상대 경로(`/api/v1/...`)로만 부른다. 절대 주소를 하드코딩하지 않는다 (`apiRequest` 가 `VITE_API_BASE_URL` 이 없으면 같은 오리진).
- 모든 호출은 `shared/core/apiClient.ts` 의 `apiRequest` 를 거친다. `credentials: 'include'` 가 여기서 붙는다.
- 토큰을 스크립트로 다루지 않는다. 로그인 상태는 `GET /api/v1/me` 로 서버에 묻는다 (`AuthProvider`).
- 화면 경로는 `/api` 로 시작하지 않는다.

---

## 6. 주의할 점

- **SPA 새로고침 404**: B·C 방식에서 `/login` 같은 화면 경로를 새로고침하면 Spring 이 정적 파일을 찾다 404 를 낸다. 화면 경로를 `index.html` 로 넘기는 포워딩이 아직 없다 (todo.md §3).
- **배포 직후 청크 오류**: 열려 있던 화면이 이전 해시의 청크를 못 받으면 `vite:preloadError` 에서 새로고침한다 (`main.tsx`).
- **dev 서버와 npmBuild 동시 실행 금지**: 윈도우에서 node_modules 가 반쯤 깨질 수 있다 (client/build.gradle).

---

## 체크리스트

- [ ] 새 API 경로가 `/api/` 로 시작하는가
- [ ] 프론트가 절대 주소나 다른 포트를 직접 부르지 않는가
- [ ] 서버에 CORS 설정을 추가하지 않았는가 (필요하면 이 문서부터 고친다)
- [ ] 새 포트를 쓰면 2절 표에 적었는가
