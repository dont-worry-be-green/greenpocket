# JWT 회원 인증 설계

| 항목 | 내용 |
|---|---|
| 문서 기준일 | 2026-09-09 |
| 상태 | JWT 인증 구현 완료 · 본인인증 정보 저장·에코 연동 우선 라우팅 반영 |
| 관련 기능 | COM-13 회원가입 · COM-14 로그인 · COM-15 토큰 재발급 · COM-16 로그아웃 |
| 관련 API | `POST /api/v1/auth/signup` · `/login` · `/refresh` · `/logout` |

이 문서는 GreenPocket의 이메일·비밀번호 회원 인증과 JWT 운용 규칙을 정의합니다. API의 요청·응답 계약은 `docs/api/api-spec.md`, 기능 완료 조건은 `docs/feature-spec/기능명세서.md`가 기준입니다.

## 1. 확정 결정

| 항목 | 결정 |
|---|---|
| 로그인 방식 | 이메일 + 비밀번호 |
| Access Token | JWT, 유효기간 30분 |
| Refresh Token | 추측 불가능한 난수 토큰, 유효기간 14일 |
| Access 전달 | 인증 응답 JSON의 `accessToken`, 이후 `Authorization: Bearer {accessToken}` |
| Refresh 전달 | `HttpOnly` 쿠키 `refreshToken` |
| Refresh 저장 | 원문을 저장하지 않고 SHA-256 해시만 DB에 저장 |
| 재발급 | 사용할 때마다 기존 Refresh Token을 폐기하고 새 토큰으로 교체(rotation) |
| 로그아웃 | 현재 Refresh Token을 폐기하고 쿠키 삭제. Access Token 블랙리스트는 MVP에서 운영하지 않음 |
| 비밀번호 | BCrypt 해시 저장, 원문 저장·응답·로그 금지, 8자 이상·UTF-8 기준 72바이트 이하 |
| JWT 식별자 | `sub`에는 `app_user.id`만 저장. 이메일·이름 등 개인정보는 넣지 않음 |
| 기존 API | URL·요청 DTO는 유지하고, 공통 인증 계층이 해석한 `userId`를 `@CurrentUserId`로 전달 |
| 데모 키 | `dev`·`demo` 프로필에서만 호환용으로 허용. 운영 프로필에서는 비활성화 |
| 본인인증 | 외부 인증 API는 연동하지 않고 인증 성공으로 간주. 이름·생년월일·성별·휴대전화번호를 모두 필수 저장 |
| 가입 후 이동 | 별도 온보딩 없이 에코마일리지 미연동 화면 `WF-01`로 이동 |

## 2. 범위

### MVP 포함

- 이메일 회원가입 및 중복 확인
- 이메일·비밀번호 로그인
- Access Token 검증과 현재 사용자 주입
- Refresh Token 회전 방식 재발급
- 로그아웃 및 Refresh Token 폐기
- Swagger Bearer 인증 입력
- 기존 `X-Demo-Key` 흐름의 개발·시연 전용 격리

### MVP 제외

- 이메일 인증 메일
- 비밀번호 찾기·재설정
- 소셜 로그인
- 관리자·역할 기반 권한 관리
- 다중 인증(MFA)
- 회원 탈퇴
- Access Token 블랙리스트

## 3. 인증 흐름

### 회원가입

1. 이메일을 `trim`하고 소문자로 정규화한다.
2. 이메일 형식, 비밀번호 8자 이상·UTF-8 기준 72바이트 이하, 이름 1~20자, 생년월일, 성별, 휴대전화번호를 검증한다.
3. `app_user`와 `auth_account`를 하나의 트랜잭션에서 생성한다.
4. 그린포켓 계좌번호를 기존 규칙대로 발급한다.
5. Access Token과 Refresh Token을 발급한다.
6. Access Token은 응답 본문으로, Refresh Token은 HttpOnly 쿠키로 전달한다.
7. 별도 온보딩 없이 `nextScreen`은 에코마일리지 미연동 화면 `WF-01`이다.

### 로그인

1. 정규화한 이메일로 `auth_account`를 찾는다.
2. BCrypt로 비밀번호를 비교한다.
3. 이메일 존재 여부를 노출하지 않도록 두 실패 모두 `AUTH_CREDENTIALS_INVALID`로 응답한다.
4. 다른 기기·브라우저의 활성 Refresh Token은 유지하고, 새 로그인 세션의 토큰을 별도 행으로 발급한다.
5. 에코 연동 상태에 따라 `entryScreen`을 `WF-01`(미연동·실패), `WF-02`(연동 중), `WF-06`(연동 완료)으로 반환한다.

### Access Token 재발급

1. 브라우저가 `refreshToken` 쿠키를 자동 전송한다.
2. 서버는 토큰 해시, 만료, 폐기 여부와 사용자를 검증한다.
3. 유효하면 기존 토큰을 폐기하고 Access Token과 새 Refresh Token을 발급한다.
4. 이미 폐기된 토큰의 재사용이 감지되면 해당 사용자의 활성 Refresh Token을 모두 폐기한다.

### 로그아웃

1. 전달된 Refresh Token이 있으면 DB에서 폐기한다.
2. `refreshToken` 쿠키를 즉시 만료시킨다.
3. 응답은 `204 No Content`다. 이미 만료·폐기된 토큰이어도 로그아웃은 멱등하게 처리한다.

## 4. 토큰 규격

### Access Token

- 서명: HMAC SHA-256(HS256)
- 필수 Claim: `sub`(문자열 userId), `iat`, `exp`, `jti`
- 유효기간: 1,800초
- 서버 시계 오차 허용은 최대 30초
- 비밀번호, 이메일, 이름, 주소, 계좌번호를 Claim에 넣지 않는다.

### Refresh Token 쿠키

| 속성 | 개발 | 운영 |
|---|---|---|
| 이름 | `refreshToken` | `refreshToken` |
| `HttpOnly` | `true` | `true` |
| `Secure` | 로컬 HTTP에서는 `false` 허용 | `true` |
| `SameSite` | `Lax` | `Lax` |
| `Path` | `/api/v1/auth` | `/api/v1/auth` |
| `Max-Age` | 1,209,600초 | 1,209,600초 |

FE는 Refresh Token을 읽거나 로컬 스토리지에 저장하지 않습니다. Access Token도 영구 저장하지 않고 메모리에 보관하며, 새로고침 시 `/auth/refresh`로 세션을 복구합니다.

## 5. API 요약

| Method | URL | 인증 | 성공 | 기능 |
|---|---|---|---|---|
| POST | `/api/v1/auth/signup` | 불필요 | 201 | 계정·사용자 생성, 토큰 발급 |
| POST | `/api/v1/auth/login` | 불필요 | 200 | 자격 증명 확인, 토큰 발급 |
| POST | `/api/v1/auth/refresh` | Refresh 쿠키 | 200 | 토큰 회전, 새 Access 발급 |
| POST | `/api/v1/auth/logout` | Refresh 쿠키 | 204 | Refresh 폐기, 쿠키 삭제 |
| GET | `/api/v1/users/me` | Bearer Access | 200 | 로그인 사용자 부트스트랩 |

상세 요청·응답과 오류 코드는 `docs/api/api-spec.md` 4.5~4.8절을 따릅니다.

## 6. 적용 스키마

`docs/database/schema.sql`, `V3__add_jwt_auth.sql`, `V5__add_signup_identity_information.sql`에 아래 구조를 반영했습니다.

### `app_user` 변경

- `demo_key`를 NULL 허용으로 변경한다.
- 기존 데모 사용자는 값을 유지한다.
- 일반 회원가입 사용자는 `demo_key = NULL`이다.
- `birth_date`, `gender`, `phone_number`에 본인인증 정보를 저장한다.
- `phone_number`는 숫자만 정규화해 저장하고 UNIQUE로 중복 가입을 막는다.
- 별도 온보딩이 없으므로 신규 가입자의 `onboarding_completed`는 생성 시 `1`이다.

### `auth_account` 추가

| 컬럼 | 규칙 |
|---|---|
| `id` | BIGINT PK AUTO_INCREMENT |
| `user_id` | BIGINT NOT NULL, `app_user.id` FK, UNIQUE |
| `email` | VARCHAR(255) NOT NULL, UNIQUE, 정규화된 소문자 저장 |
| `password_hash` | VARCHAR(255) NOT NULL, BCrypt |
| `last_login_at` | DATETIME NULL |
| `created_at`, `updated_at` | 공통 감사 시각 |

### `auth_refresh_token` 추가

| 컬럼 | 규칙 |
|---|---|
| `id` | BIGINT PK AUTO_INCREMENT |
| `user_id` | BIGINT NOT NULL, `app_user.id` FK |
| `token_hash` | CHAR(64) NOT NULL, UNIQUE, SHA-256 hex |
| `expires_at` | DATETIME NOT NULL |
| `revoked_at` | DATETIME NULL |
| `created_at`, `updated_at` | 공통 감사 시각 |

사용자 삭제 시 두 인증 테이블도 `ON DELETE CASCADE`로 정리합니다.

## 7. 환경변수

| 이름 | 예시가 아닌 규칙 |
|---|---|
| `JWT_SECRET_BASE64` | 최소 32바이트 난수를 Base64로 인코딩. 실제 값은 저장소·문서·로그에 기록하지 않음 |
| `JWT_ACCESS_EXPIRATION_SECONDS` | 기본 `1800` |
| `JWT_REFRESH_EXPIRATION_SECONDS` | 기본 `1209600` |
| `JWT_REFRESH_COOKIE_SECURE` | 로컬 HTTP는 `false`, 운영 HTTPS는 반드시 `true` |
| `DEMO_AUTH_ENABLED` | 기본 `false`; 개발·시연 환경에서만 `true` 허용 |

실제 Secret은 `.env.example`이 아니라 실행 환경 또는 IntelliJ Run Configuration에 넣습니다. `.env.example`에는 변수명과 안전한 설명만 둡니다.

## 8. 보안·로깅 규칙

- 비밀번호 원문, Authorization 헤더, JWT 전체 문자열, Refresh Token 원문을 로그에 남기지 않는다.
- 로그인 실패 응답으로 가입 여부를 구분할 수 없게 한다.
- 토큰 파싱 실패는 내부 예외 내용을 노출하지 않고 공통 401 응답으로 변환한다.
- Access Token이 만료됐을 때만 FE가 재발급을 한 번 시도한다. 재발급 실패 시 세션을 비우고 ONB-01로 이동한다.
- 동시 401 요청은 재발급 요청 하나만 실행하고 나머지는 그 결과를 기다리는 single-flight 방식으로 처리한다.
- CORS에서 자격 증명 사용 시 허용 Origin을 명시하며 `*`를 쓰지 않는다.

## 9. 구현 순서와 완료 조건

1. `[완료]` Markdown 명세와 `schema.sql` 동기화
2. `[완료]` Flyway `V3__add_jwt_auth.sql` 작성
3. `[완료]` 인증 Repository·Service 구현
4. `[완료]` JWT 발급·검증과 `@CurrentUserId` 연동
5. `[완료]` 인증 Controller 및 Swagger Bearer 설정
6. `[대기]` FE 인증 Store·API·401 single-flight 재발급 처리
7. `[완료]` 백엔드 단위·회귀 테스트와 로컬 DB 마이그레이션·Swagger 스모크 테스트

### 백엔드 검증 결과 (2026-09-07)

- Flyway V2·V3 적용 후 스키마 버전 3 확인
- 회원가입 `201` → Bearer `GET /users/me` `200` 확인
- Refresh Token 회전 재발급 `200` 및 새 Access Token 호출 `200` 확인
- 로그아웃 `204` 후 재발급 `401 REFRESH_TOKEN_INVALID` 확인
- 로그인 `200`, 잘못된 비밀번호 `401 AUTH_CREDENTIALS_INVALID` 확인
- 중복 이메일 `409 EMAIL_ALREADY_USED` 확인

완료 조건은 회원가입 → 프로필 입력 → 기존 기능 호출 → Access 만료 후 자동 재발급 → 로그아웃 → 보호 API 401 흐름이 자동 테스트와 Swagger에서 모두 확인되는 것입니다.
