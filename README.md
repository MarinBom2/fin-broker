# fin-broker

**금융상품 판매대리·중개 커뮤니티.** 금융소비자가 묻고, 등록 확인을 마친 판매대리·중개업자가 답하고, 원하면 상담으로 이어지는 서비스.

- 상태: 골격 단계. 모듈·인증·문서 형식까지 만들었고, 도메인(중개사 등록·커뮤니티·상담)은 [`md/domain-design.md`](./md/domain-design.md) 초안을 리뷰한 뒤 채운다
- 할 일: [`md/todo.md`](./md/todo.md)

## 구조

```
fin-broker/
├── common/core/  도메인·공통 설정(yml). server·batch 가 같이 쓰는 라이브러리 jar
├── server/       Spring Boot 4.1.2, Java 21  (API + React 빌드 결과물 서빙)
├── batch/        Spring Boot 배치
├── client/       Vite + React 19 + TypeScript 5.9 (SPA)
├── md/           설계 문서
├── http/         로컬 API 호출 모음
└── docker-compose.yml  로컬 MySQL
```

## 로컬 실행

```bash
docker compose up -d mysql
```

```bash
./gradlew :server:bootRun          # http://localhost:9090, Swagger: /swagger-ui.html
```

```bash
cd client && npm install && npm run dev    # http://localhost:9091 (/api 는 9090 으로 프록시)
```

Windows 는 `./gradlew` 대신 `.\gradlew.bat`. 실행 방식 세 가지와 Gradle 플래그는 [`md/runtime-structure.md`](./md/runtime-structure.md).

비밀 값(JWT 서명 키 등)은 저장소에 넣지 않는다. 로컬은 루트 `secrets/application-secret.yml`(gitignore), 운영은 환경변수 ([`md/develop-principal.md`](./md/develop-principal.md) §6).

## 테스트

```bash
./gradlew build -PskipClient
```

Testcontainers(MySQL) 테스트는 Docker 데몬이 없으면 건너뛴다.

## 문서

| 문서 | 내용 |
|---|---|
| [domain-design.md](./md/domain-design.md) | 도메인 모델·상태·API 초안, 규제 확인 사항 |
| [solid.md](./md/solid.md) | SOLID 적용 기준 |
| [develop-principal.md](./md/develop-principal.md) | 코드 작성 규칙 (enum, 예외, ProblemDetail, API 경로, 비밀 값) |
| [authentication-security.md](./md/authentication-security.md) | JWT·쿠키·필터 구조 |
| [runtime-structure.md](./md/runtime-structure.md) | 실행·빌드 구조 (한 오리진 원칙) |
| [todo.md](./md/todo.md) | 할 일 |

## 고지

커뮤니티의 글과 답변은 참고용 정보이며 특정 금융상품의 권유가 아니다. 금융상품 가입 전에는 상품설명서와 약관을 확인한다.
