# 그린포켓 API 명세서

| 항목 | 내용 |
|---|---|
| 문서 기준일 | 2026-09-09 (ver3.9 — 결정 C-1~C-36 반영) |
| 참가팀 | 돈워리, 비그린 (Don't worry, be green) |
| 기준 문서 | `docs/feature-spec/기능명세서.md` (115건) · `docs/database/schema.sql` (20테이블) |
| 대상 범위 | **P0 88건 + P1 25건**. P2 2건(D-3-06 · E-2-02)은 15절에 자리만 표기 |
| API 수 | **68개** (P0 50 · P1 18) |
| 인증 | 이메일·비밀번호 로그인 + JWT Access Token. Refresh Token은 HttpOnly 쿠키 (결정 C-17) |
| 서버 | Spring Boot · MySQL 8.4 · Base URL `/api/v1` |
| 스키마 기준 | `docs/database/schema.sql` — FK·UNIQUE·CHECK 포함본. **DB 적용은 `backend/src/main/resources/db/migration/`의 Flyway 마이그레이션으로 한다** |

> **우선순위 규칙** — 기능은 원칙적으로 엑셀, 데이터는 `schema.sql`이 기준입니다. JWT 기능 COM-13~16도 원본 XLSX·DDL과 동기화되어 있습니다.
> 이전 버전에서 열어 두었던 결정 13건은 2026-09-03에 전부 확정됐고 이 문서에 반영돼 있습니다. 무엇을 어떻게 정했는지는 **16절**을 보세요.

---

## 목차

| 절 | 내용 |
|---|---|
| 1 | 공통 규칙 (인증 · 응답 래퍼 · 타입 · 페이징 · 멱등성) |
| 2 | 공통 에러 코드 |
| 3 | 열거형(ENUM) 사전 |
| 4 | 공통·인증·데모 API (COM) |
| 5 | 프로필 API (A-1) |
| 6 | 고지서 API (A-2) |
| 7 | 진단 API (A-3) |
| 8 | 에코마일리지 연동 API (B-1) |
| 9 | 목표·미션 API (B-2 · B-3) |
| 10 | 진행 현황·전달 리포트 API (B-4) |
| 11 | 평가 결과·마일리지 API (B-5) |
| 12 | 혜택 API (C) |
| 13 | 포켓 API (D) |
| 14 | 마이·보관함·청년정책 API (E) |
| 15 | 매핑표 (화면 ↔ API · 기능 ID ↔ API · DB ↔ API) |
| 16 | 2026-09-03 결정 기록 |
| 17 | 2026-09-07 JWT 인증 결정 |
| 18 | 2026-09-08 청년정책 추천 결정 |

---

# 1. 공통 규칙

## 1.1 Base URL · 버전

```
https://{host}/api/v1
```

## 1.2 인증 — JWT Bearer

운영 사용자는 이메일·비밀번호로 로그인하고, 인증이 필요한 요청에 JWT Access Token을 붙입니다(결정 C-17).

```http
Authorization: Bearer eyJ...
```

| 규칙 | 내용 |
|---|---|
| Access Token | JWT HS256, 30분. `sub`에는 문자열 형태의 `app_user.id`만 저장 |
| Refresh Token | 14일, HttpOnly 쿠키 `refreshToken`. 서버에는 SHA-256 해시만 저장하고 재발급 때마다 회전 |
| 공개 경로 | `POST /auth/signup` · `/auth/login` · `/auth/refresh` · `/auth/logout`, `GET /meta/**` |
| 인증 실패 | 누락·형식/서명 오류는 `401 UNAUTHENTICATED`, 만료는 `401 ACCESS_TOKEN_EXPIRED`. FE는 재발급을 한 번 시도하고 실패하면 ONB-01로 이동 |
| 서버 동작 | JWT `sub` → `app_user.id` 해석. **요청 본문·경로에 `userId`를 받지 않습니다.** 도메인 컨트롤러에는 `@CurrentUserId`로 전달 |
| 로깅 | 비밀번호, Authorization 헤더, JWT 전체 문자열, Refresh Token 원문을 로그에 남기지 않습니다. 계좌번호 원문도 남기지 않습니다(COM-11 · 결정 A-6) |

### 개발·시연용 데모 인증

`X-Demo-Key`, `POST /users`, `POST /demo/reset`은 `dev`·`demo` 프로필에서만 호환용으로 허용합니다. 운영 프로필에서는 비활성화하며 Bearer 인증을 우회할 수 없습니다.

```http
X-Demo-Key: 9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41
```

## 1.3 공통 응답 래퍼

**성공**

```json
{
  "success": true,
  "data": { "...": "엔드포인트별 본문" },
  "error": null,
  "timestamp": "2026-09-03T18:30:00+09:00"
}
```

**실패**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BILL_DUPLICATED",
    "message": "2026년 8월 전기 고지서가 이미 등록돼 있어요.",
    "field": "items[0].utilityType",
    "details": { "billingMonth": "2026-08", "utilityType": "ELECTRICITY" }
  },
  "timestamp": "2026-09-03T18:30:00+09:00"
}
```

| 필드 | 규칙 |
|---|---|
| `success` | HTTP 2xx면 `true`, 그 외 `false` |
| `data` | 실패 시 항상 `null` |
| `error.message` | **화면에 그대로 띄울 한국어 문장.** 개발자용 문구를 쓰지 않습니다 |
| `error.field` | 입력 검증 실패일 때만. 폼 필드 경로(JSON Pointer 유사) |
| `error.details` | 재시도·분기에 필요한 값만. 없으면 생략 |

> 이 문서의 각 엔드포인트에서 보여주는 **Response 본문은 `data` 안에 들어가는 부분만** 적었습니다.

## 1.4 데이터 타입 규칙

| 종류 | 형식 | 예 | 비고 |
|---|---|---|---|
| 일시 | ISO-8601 + KST 오프셋 | `2026-09-03T18:30:00+09:00` | 서버 저장은 `DATETIME`/`TIMESTAMP` |
| 날짜 | `YYYY-MM-DD` | `2026-08-15` | |
| 월 | `YYYY-MM` (문자열) | `2026-08` | DB는 `DATE`로 그 달 1일 저장 |
| 금액 | 정수 (원) | `43200` | 절대 소수·문자열로 보내지 않음 |
| 마일리지 | 정수 (M) | `30000` | 1M = 1원 (COM-06) |
| 사용량 | 소수 3자리까지 | `1340.000`, `108.500` | `DECIMAL(12,3)` |
| 비율·감축률 | 소수 3자리 퍼센트 | `10.500` (= 10.5%) | `DECIMAL(7,3)`. **음수는 증가** |
| 신뢰도 | 0~1 소수 4자리 | `0.8231` | OCR `confidence` |
| 불리언 | `true` / `false` | | DB `TINYINT(1)` |
| ID | 정수 (`BIGINT`) | `12` | |
| 열거형 | 대문자 스네이크 | `ELECTRICITY` | 3절 사전 |

**표기는 서버가 하지 않습니다.** `43,200원` · `↓12% 줄었어요` 같은 문자열 조립은 FE 공통 포맷터 담당(COM-06). 서버는 숫자와 enum만 줍니다. 예외는 **출처 문구·산출 근거·미등록 사유**처럼 DB에 문장으로 들어 있는 값입니다.

## 1.5 페이징

목록 API 공통 쿼리 파라미터.

| 파라미터 | 기본 | 설명 |
|---|---|---|
| `page` | `0` | 0-base |
| `size` | `20` | 최대 100 |

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 37,
  "totalPages": 2,
  "hasNext": true
}
```

## 1.6 멱등성 — `Idempotency-Key`

돈이 움직이는 세 곳은 **중복 탭·재시도로 거래가 두 번 생기면 안 됩니다**(비즈니스 규칙 5).

| 엔드포인트 | 키 | DB 제약 |
|---|---|---|
| `POST /pocket/withdrawals` | `Idempotency-Key` 헤더 (필수) | `UNIQUE pocket_transaction(idempotency_key)` |
| `POST /pocket/conversions/{id}/complete` | `Idempotency-Key` 헤더 (필수) | `UNIQUE(user_id, source_type='ECO_ROUND', source_key=회차id)` |
| `POST /greenlife/settlements` | 본문 `yearMonth` 가 키 | `UNIQUE(user_id, source_type='GREENLIFE_MONTH', source_key='2026-08')` |

**같은 키로 다시 들어오면 새로 만들지 않고, 이전에 만든 거래를 `200 OK`로 그대로 돌려줍니다.** 409를 던지지 않습니다 — 화면에서 오류로 보이면 안 되니까요.

## 1.7 비동기 작업 (OCR · 연동)

OCR(A-2-04)과 에코마일리지 연동(B-1-02)은 **작업 생성 → 폴링** 두 단계입니다.

```
POST  /bills/ocr           202 Accepted  → { jobId, status: "PENDING", progress: 0 }
GET   /bills/ocr/{jobId}   200           → { status, progress, result | error }
```

| 규칙 | 내용 |
|---|---|
| 폴링 주기 | 권장 1초 |
| 타임아웃 | OCR 30초 · 에코 연동 20초(B-1-03). 초과 시 `status: "TIMEOUT"` + FE 재시도 버튼 |
| 부분 실패 | 에코 연동은 성공한 요금 데이터를 유지하고 실패 항목만 표시(B-1-03) |
| 처리 중 표시 | **`SUCCEEDED` 가 아닌 상태를 완료 화면으로 보여주지 않습니다**(비즈니스 규칙 11) |

## 1.8 이 API가 하지 않는 것

| 항목 | 이유 |
|---|---|
| 실제 외부 연동 | 에코마일리지·녹색생활실천은 시드 기반 모의다. 온통청년 청년정책 API 동기화만 실연동한다(결정 C-24) |
| 실제 이체 | 전환·출금은 원장에 거래만 남기고 돈은 움직이지 않음(결정 A-6) |
| 요금 계산 | 공식 요금표 기반 전기요금 계산 엔진 없음. 절감액은 `기준 요금 × 목표율` 비례(B-2-05) |
| 이미지 보관 | 고지서 원본은 저장하지 않고 인식 후 폐기(COM-11) |
| 푸시 알림 | MVP 제외 |

---

# 2. 공통 에러 코드

## 2.1 공통 (모든 API)

| code | HTTP | 화면 문구(예) |
|---|---|---|
| `INVALID_REQUEST` | 400 | 입력값을 다시 확인해 주세요. |
| `UNAUTHENTICATED` | 401 | 로그인이 필요해요. |
| `ACCESS_TOKEN_EXPIRED` | 401 | 로그인 시간이 만료됐어요. 다시 연결할게요. |
| `UNAUTHENTICATED_DEMO_KEY` | 401 | 데모 사용자를 찾을 수 없어요. 처음부터 시작해 주세요. (`dev`·`demo` 전용) |
| `NOT_FOUND` | 404 | 요청한 정보를 찾을 수 없어요. |
| `CONFLICT` | 409 | 이미 처리된 요청이에요. |
| `TOO_MANY_REQUESTS` | 429 | 잠시 후 다시 시도해 주세요. |
| `INTERNAL_ERROR` | 500 | 잠시 문제가 생겼어요. 다시 시도해 주세요. |
| `EXTERNAL_TIMEOUT` | 504 | 시간이 오래 걸리고 있어요. 다시 시도해 주세요. |

## 2.2 도메인별

| 도메인 | code | HTTP | 조건 | 기능 ID |
|---|---|---|---|---|
| 인증 | `EMAIL_INVALID` | 400 | 이메일 형식 오류 | COM-13 |
| | `PASSWORD_INVALID` | 400 | 비밀번호 8자 미만 또는 UTF-8 기준 72바이트 초과 | COM-13 |
| | `EMAIL_ALREADY_USED` | 409 | 정규화한 이메일 중복 | COM-13 |
| | `AUTH_CREDENTIALS_INVALID` | 401 | 미가입 이메일 또는 비밀번호 불일치 | COM-14 |
| | `REFRESH_TOKEN_INVALID` | 401 | Refresh Token 누락·형식 오류·폐기·재사용 | COM-15 |
| | `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token 만료 | COM-15 |
| 프로필 | `NAME_INVALID` | 400 | 공백 제거 후 1~20자 아님 / 특수문자만 | COM-01 |
| | `PROFILE_INCOMPLETE` | 409 | 마이 정책 추천 선택값 중 필수값 미입력 | A-1-05 |
| | `BIRTH_DATE_INVALID` | 400 | 미래 날짜 또는 지원하지 않는 날짜 범위 | A-1-01 |
| | `POLICY_INTEREST_LIMIT_EXCEEDED` | 400 | 레거시 관심 분야 입력 오류(신규 API에서는 사용하지 않음) | 레거시 |
| 고지서 | `IMAGE_TOO_LARGE` | 413 | 10MB 초과 | A-2-03 |
| | `IMAGE_UNSUPPORTED` | 415 | JPG·PNG 아님 | A-2-03 |
| | `OCR_JOB_NOT_FOUND` | 404 | 잘못된 jobId | A-2-04 |
| | `OCR_FAILED` | 422 | 인식 0건·지원하지 않는 양식 → 직접 입력 유도 | A-2-07 |
| | `BILL_DUPLICATED` | 409 | 같은 (월 × 항목) 이미 등록 — **항목 단위** | A-2-09 · 결정 A-3 |
| | `BILL_ITEM_EMPTY` | 400 | 내용 있는 항목 0개 | A-2-10 |
| | `BILL_USAGE_REQUIRED` | 400 | 사용량 누락·0 이하 | A-2-08 · A-2-09 |
| | `BILL_ELECTRICITY_REQUIRED` | 400 | 직접 입력에서 전기 미입력 | A-2-08 |
| 진단 | `DIAGNOSIS_MONTH_EMPTY` | 404 | 해당 월 고지서 없음 | A-3-09 |
| What-if | `ECO_NOT_SEOUL` | 403 | 에코마일리지 연동 주소 시도 ≠ 서울(11) | B-1-09 |
| | `ECO_NOT_LINKED` | 409 | `eco_link_status != LINKED` | B-1-01 |
| | `ECO_LINK_FAILED` | 502 | 모의 연동 실패 | B-1-02 |
| | `ECO_ROUND_NOT_FOUND` | 404 | 회차 없음 | B-1-07 |
| | `ECO_UTILITY_NOT_REGISTERED` | 409 | 미등록 요금에 목표 설정 시도 | B-2-06 |
| | `ECO_GOAL_REQUIRED` | 409 | 목표 미설정 상태에서 진행 API 호출 | B-2-01 |
| | `ECO_TIER_INVALID` | 400 | `tier` 가 5·10·15 아님 | B-2-03 |
| | `ECO_RESULT_NOT_CONFIRMED` | 409 | 확정 전 결과 조회 | B-5-02 |
| 혜택 | `GREENLIFE_NOT_PARTICIPATING` | 409 | 미참여 상태에서 현황 조회 | C-1-01 |
| | `GREENLIFE_ITEM_NOT_FOUND` | 404 | 없는 항목 | C-2-04 |
| | `GREENLIFE_SETTLEMENT_DUPLICATED` | 409 | 같은 월 정산 재요청 → **200으로 기존 거래 반환** | C-2-06 |
| 포켓 | `POCKET_ACCOUNT_REQUIRED` | 409 | 출금계좌 미등록 상태에서 출금 신청 | D-1-06 · D-3-02 |
| | `POCKET_ACCOUNT_NOT_FOUND` | 404 | 없는 계좌 | D-3-01 |
| | `POCKET_INSUFFICIENT_BALANCE` | 409 | 출금액 > 잔액 | D-3-02 |
| | `POCKET_AMOUNT_INVALID` | 400 | 0 이하·정수 아님 | D-3-02 |
| | `CONVERSION_NOT_AVAILABLE` | 409 | 전환 가능 마일리지 0 | D-2-01 |
| | `CONVERSION_ALREADY_DONE` | 409 | 같은 회차 전환 이력 존재 | D-2-02 |
| | `CONVERSION_DAILY_LIMIT` | 429 | 오늘 이미 전환함 (1일 1회) | D-2-02 |
| | `CONVERSION_NOT_RETURNED` | 409 | 외부 이동 기록 없이 완료 요청 | D-2-02 |
| 청년정책 | `YOUTH_POLICY_NOT_FOUND` | 404 | 없는 정책 ID | E-3-03 |
| | `YOUTH_POLICY_DATA_UNAVAILABLE` | 503 | 정상 동기화 데이터가 아직 없음 | E-3-01 · E-3-02 |

> **비교 데이터 부재는 에러가 아닙니다.** 1인 가구 기준이 없거나(A-3-03) 작년 값이 없으면(A-3-06) `200` + `available: false` + `unavailableReason` 으로 내려서 화면이 "비교 데이터 준비 중"을 띄우게 합니다. 임의 값을 만들지 않습니다(비즈니스 규칙 8).

---

# 3. 열거형(ENUM) 사전

**전부 `docs/database/schema.sql` 의 ENUM 정의와 1:1입니다.** 값을 추가하려면 DDL부터 고칩니다.

| 이름 | 값 | 쓰는 곳 |
|---|---|---|
| `UtilityType` | `ELECTRICITY` · `GAS` · `WATER` | 전 영역 |
| `UsageUnit` | `kWh` · `m3` | 전기 kWh, 가스·수도 m3 |
| `HousingType` | `ONE_ROOM` · `OFFICETEL` · `APARTMENT` · `MULTI_HOUSE` | 원룸·오피스텔·아파트·다세대 |
| `AreaBand` | `UNDER_10` · `FROM_10_TO_20` · `OVER_20` | 10평 이하·10~20평·20평 이상 |
| `CurrentStatus` | `EMPLOYED` · `SELF_EMPLOYED` · `UNEMPLOYED` · `FREELANCER` · `STUDENT` · `PREPARING_STARTUP` · `OTHER` | 정책 추천 현재 상태 |
| `AnnualIncomeBand` | `NO_INCOME` · `UNDER_24M` · `FROM_24M_TO_36M` · `FROM_36M_TO_50M` · `OVER_50M` · `UNKNOWN` | 연소득 구간, 원 단위 상세 금액은 받지 않음 |
| `EducationStatus` | `BELOW_HIGH_SCHOOL` · `HIGH_SCHOOL_STUDENT` · `HIGH_SCHOOL_EXPECTED_GRADUATION` · `HIGH_SCHOOL_GRADUATE` · `UNIVERSITY_STUDENT` · `UNIVERSITY_EXPECTED_GRADUATION` · `UNIVERSITY_GRADUATE` · `GRADUATE_SCHOOL` · `OTHER` | 정책 추천 학력 |
| `HouseholdStatus` | `ONE_PERSON` · `WITH_PARENTS` · `MARRIED` · `SINGLE_PARENT` · `OTHER` | 기존 DB 호환용. 신규 추천 조건에서는 사용하지 않음 |
| `PolicyInterestCategory` | `JOB` · `HOUSING` · `EDUCATION` · `WELFARE_CULTURE` · `PARTICIPATION_RIGHTS` | 추천 우선순위용 1~2개 |
| `PolicyApplicationStatus` | `OPEN` · `UPCOMING` · `CLOSED` · `UNKNOWN` | 정책 신청 상태 |
| `EcoLinkStatus` | `UNLINKED` · `LINKING` · `LINKED` · `FAILED` | WF-01 · WF-02 |
| `RecordSource` | `BILL` · `ECO_BASELINE` | 고지서 / 직전 2년 기준값 |
| `BillType` | `MANAGEMENT` · `ELECTRICITY` · `GAS` · `WATER` | 관리비 통합·개별 |
| `InputSource` | `OCR` · `MANUAL` · `ECO_LINK` | |
| `RecordStatus` | `CONFIRMED` · `REVIEW_REQUIRED` | 확인 필요 배지 |
| `RegionLevel` | `SIGUNGU` · `SIDO` | 범위 배지 |
| `RoundStatus` | `READY` · `GOAL_SET` · `IN_PROGRESS` · `CONFIRMED` · `CLOSED` | |
| `ApplicationStatus` | `NOT_APPLIED` · `APPLYING` · `APPLIED` · `FAILED` | 참여신청 배너 |
| `TargetTier` | `TIER_5` · `TIER_10` · `TIER_15` | 5~10% / 10~15% / 15%+ |
| `Difficulty` | `EASY` · `NORMAL` · `HARD` | 미션 난이도 |
| `SeasonTag` | `SPRING` · `SUMMER` · `AUTUMN` · `WINTER` | SET 컬럼 → 배열로 응답 |
| `RewardStatus` | `PENDING` · `PAID` | 적립 예정 / 지급 완료 |
| `TxDirection` | `CREDIT` · `DEBIT` | 입금 / 출금 |
| `TxType` | `ECO_MILEAGE` · `GREENLIFE` · `WITHDRAWAL` | |
| `TxStatus` | `REQUESTED` · `PROCESSING` · `COMPLETED` · `FAILED` | **잔액엔 `COMPLETED`만** |
| `TxSourceType` | `ECO_ROUND` · `GREENLIFE_MONTH` · `WITHDRAWAL` | 중복 방지 키 |

**API 전용 열거형** (DB에 없고 응답 분기용)

| 이름 | 값 | 설명 |
|---|---|---|
| `WhatIfScreen` | `WF_01_UNLINKED` · `WF_02_LINKING` · `WF_03_NO_GOAL` · `WF_06_IN_PROGRESS` · `WF_09_RESULT_READY` | `GET /eco/home` 이 FE에 알려주는 렌더 상태 |
| `JobStatus` | `PENDING` · `RUNNING` · `SUCCEEDED` · `PARTIAL` · `FAILED` · `TIMEOUT` | OCR·연동 비동기 작업 |
| `Tab` | `DIAGNOSIS` · `BENEFIT` · `WHATIF` · `POCKET` · `MYPAGE` | 하단 탭 5개 (COM-02) |
| `PolicyMatchStatus` | `ELIGIBLE` · `CHECK_REQUIRED` · `NOT_ELIGIBLE` | 추천 조건 판정. `ELIGIBLE`도 최종 자격 확정을 뜻하지 않음 |
| `BaselineCalculationBasis` | `WEIGHTED_MONTHLY_MICRODATA_AVERAGE` · `ANNUAL_ENERGY_SHARE_MONTHLY_EQUIVALENT` · `DAILY_USAGE_MONTH_EQUIVALENT` | 월별 마이크로데이터 가중평균 / 연간 집계 월평균 환산(하위 호환) / 일 사용량 월 일수 환산 |

---

# 4. 공통·인증·데모 API (COM)

## 4.1 데모 사용자 시작 (개발·시연 전용)

`POST /users` · **P0** · COM-01 · ONB-01

> `dev`·`demo` 프로필에서만 활성화합니다. 운영 회원가입은 4.5절을 사용합니다.

**Request** (헤더 불필요)

```json
{ "demoKey": "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41", "name": "김수현" }
```

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `demoKey` | string(50) | ✔ | FE 생성 UUID. 이미 있으면 기존 사용자 반환(재진입) |
| `name` | string(20) | ✔ | `trim` 후 1~20자. 공백·특수문자만이면 `NAME_INVALID` |

**Response 201**

```json
{
  "userId": 1,
  "name": "김수현",
  "onboardingCompleted": true,
  "nextScreen": "WF-01",
  "pocketAccountNo": "1005-1234-5678-90",
  "pocketHolder": "김수현",
  "createdAt": "2026-09-03T18:30:00+09:00"
}
```

- 그린포켓 계좌번호는 가입 시 서버가 `1005-####-####-##` 형식의 사용자별 고유 번호로 발급하고, 예금주는 입력 이름으로 저장합니다(D-1-01·결정 C-14). 위 번호는 응답 예시이며 별도 생성 API는 없습니다.
- 같은 `demoKey` 재요청 → `200 OK` + 기존 사용자 (재진입, 이름은 갱신하지 않음).

**Errors** `NAME_INVALID(400)`

---

## 4.2 앱 부트스트랩 (내 상태)

`GET /users/me` · **P0** · COM-01 · COM-02 · COM-12 · COM-13~15 · 전 화면

앱 진입 시 **한 번 호출해서 어느 화면으로 보낼지 결정**합니다.

운영에서는 Bearer Access Token이 필요합니다. `dev`·`demo` 프로필에서는 기존 `X-Demo-Key`도 사용할 수 있습니다.

**Response 200**

```json
{
  "userId": 1,
  "name": "김수현",
  "onboardingCompleted": true,
  "ecoLinkStatus": "LINKED",
  "ecoLinkedAt": "2026-09-01T09:00:00+09:00",
  "greenlifeParticipating": true,
  "greenlifeLinkedAt": "2026-09-01T09:12:00+09:00",
  "hasBill": true,
  "currentRoundId": 7,
  "entryScreen": "WF-06"
}
```

| 필드 | 설명 |
|---|---|
| `entryScreen` | 에코 연동 상태 기준 진입 화면. `UNLINKED/FAILED` → `WF-01`, `LINKING` → `WF-02`, `LINKED` → `WF-06` |
| `hasBill` | 등록 고지서 1건 이상 여부. 진단 빈 상태(A-3-04) 분기용 |

> 별도 온보딩은 없습니다. **마지막 방문 탭 복원도 만들지 않으며**, 에코 연동 상태에 따라 What-if 화면으로 들어갑니다(결정 C-1·C-26).

---

## 4.3 행정구역 목록

`GET /meta/regions` · **P0** · 레거시 호환

| 쿼리 | 필수 | 설명 |
|---|---|---|
| `sidoCode` | | 없으면 시도 목록, 있으면 그 시도의 시군구 목록 |

MVP 서비스 지역은 서울특별시로 한정합니다(결정 C-15). 신규 가입·정책 추천에서는 지역을 직접 입력받지 않으며, 이 API는 기존 화면 호환용입니다. 정책 추천 지역은 에코마일리지 연동 주소만 사용합니다.

**Response 200**

```json
{
  "level": "SIGUNGU",
  "items": [
    { "code": "11620", "name": "관악구", "sidoCode": "11", "hasRegionAverage": true }
  ]
}
```

- `hasRegionAverage` — 기존 지역 평균 비교와의 응답 호환을 위해 유지하는 필드입니다. 결정 C-29 이후 진단 기준선 선택에는 사용하지 않습니다.
- 시군구 코드는 한전 API `cityCd` 와 겸용(`app_user.sigungu_code` COMMENT).
- 코드·명칭의 출처는 행정안전부 [행정표준코드관리시스템](https://www.code.go.kr/stdcode/regCodeL.do)과 [행정표준코드 API](https://www.data.go.kr/data/15077871/openapi.do)이며, 외부 장애에 영향을 받지 않도록 실행 중에는 `backend/src/main/resources/data/seoul-regions.json`을 사용합니다.

---

## 4.4 데모 초기화

`POST /demo/reset` · **P0** · COM-10 · MY-01

> `dev`·`demo` 프로필에서만 활성화합니다. 운영 프로필에서는 노출하지 않습니다.

**Request**

```json
{ "demoKey": "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41" }
```

**Response 200**

```json
{ "resetAt": "2026-09-03T18:31:00+09:00", "nextScreen": "ONB-01" }
```

- 서버 동작: `DELETE FROM app_user WHERE id = :uid` **한 줄.** FK CASCADE로 사용자 데이터 8개 테이블이 전부 정리되고 마스터(`mission_catalog` · `greenlife_item` · `region_utility_snapshot`)만 남습니다.
- 스키마 기준(`docs/database/schema.sql`)에 FK 16개를 복원해 두었고, 위 동작을 MariaDB에서 실제로 확인했습니다(결정 4·9).
- 유효한 UUID v4를 다시 요청해 대상 사용자가 이미 없어도 초기화 완료 상태이므로 `200`을 반환합니다. UUID v4 형식이 아니면 `400 INVALID_REQUEST`입니다.

---

## 4.5 이메일 회원가입

`POST /auth/signup` · **P0** · COM-13 · ONB-01

**Request** (인증 불필요)

```json
{
  "email": "user@example.com",
  "password": "green1234",
  "name": "김수현",
  "birthDate": "1998-03-15",
  "gender": "FEMALE",
  "phoneNumber": "01091740339"
}
```

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| `email` | string(255) | ✔ | `trim` 후 소문자 저장, 표준 이메일 형식, UNIQUE |
| `password` | string | ✔ | 8자 이상·UTF-8 기준 72바이트 이하. 원문은 저장·응답·로그 금지 |
| `name` | string(20) | ✔ | `trim` 후 1~20자. 공백·특수문자만이면 `NAME_INVALID` |
| `birthDate` | date | ✔ | `YYYY-MM-DD`, 미래 날짜 불가. 본인인증 성공값으로 간주 |
| `gender` | enum | ✔ | `MALE` 또는 `FEMALE` |
| `phoneNumber` | string | ✔ | 국내 휴대전화번호. 구분기호 제거 후 숫자 10~11자리 저장, UNIQUE |

**Response 201**

```json
{
  "userId": 1,
  "email": "user@example.com",
  "name": "김수현",
  "onboardingCompleted": true,
  "nextScreen": "WF-01",
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

```http
Set-Cookie: refreshToken=<opaque>; HttpOnly; SameSite=Lax; Path=/api/v1/auth; Max-Age=1209600
```

- 외부 본인인증 API는 붙이지 않고 이름·생년월일·성별·휴대전화번호를 모두 확인된 값으로 간주합니다. `app_user`와 `auth_account`는 한 트랜잭션에서 생성합니다.
- 그린포켓 계좌번호와 예금주는 4.1절과 같은 규칙으로 생성합니다.
- 운영 HTTPS에서는 Refresh 쿠키에 `Secure`를 반드시 붙입니다.

**Errors** `EMAIL_INVALID(400)` · `PASSWORD_INVALID(400)` · `NAME_INVALID(400)` · `BIRTH_DATE_INVALID(400)` · `GENDER_REQUIRED(400)` · `PHONE_NUMBER_INVALID(400)` · `EMAIL_ALREADY_USED(409)` · `PHONE_NUMBER_ALREADY_USED(409)`

---

## 4.6 이메일 로그인

`POST /auth/login` · **P0** · COM-14 · ONB-01

**Request** (인증 불필요)

```json
{ "email": "user@example.com", "password": "green1234" }
```

**Response 200**

```json
{
  "userId": 1,
  "name": "김수현",
  "onboardingCompleted": true,
  "entryScreen": "WF-06",
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

응답 시 4.5절과 같은 속성의 `refreshToken` 쿠키를 설정합니다. `entryScreen`은 에코 연동 상태에 따라 `WF-01`, `WF-02`, `WF-06` 중 하나입니다.

가입 여부 노출을 막기 위해 미가입 이메일과 비밀번호 불일치는 모두 아래 오류로 응답합니다.

**Errors** `EMAIL_INVALID(400)` · `AUTH_CREDENTIALS_INVALID(401)`

---

## 4.7 Access Token 재발급

`POST /auth/refresh` · **P0** · COM-15 · 전 화면

**Request**

- 본문 없음
- 브라우저가 HttpOnly `refreshToken` 쿠키를 전송

**Response 200**

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

- 성공하면 기존 Refresh Token을 폐기하고 새 토큰을 발급해 쿠키를 교체합니다.
- 폐기된 토큰의 재사용이 감지되면 해당 사용자의 활성 Refresh Token을 모두 폐기합니다.
- FE는 동시 401 요청을 하나의 재발급 요청으로 합치고, 성공 후 원 요청을 최대 한 번 재시도합니다.

**Errors** `REFRESH_TOKEN_INVALID(401)` · `REFRESH_TOKEN_EXPIRED(401)`

---

## 4.8 로그아웃

`POST /auth/logout` · **P0** · COM-16 · MY-01

**Request**

- 본문 없음
- Refresh Token 쿠키가 있으면 함께 전송

**Response 204 No Content**

- 현재 Refresh Token을 폐기하고 동일 이름·경로의 쿠키를 `Max-Age=0`으로 삭제합니다.
- 이미 만료·폐기됐거나 쿠키가 없어도 멱등하게 204를 반환합니다.
- FE는 메모리의 Access Token과 사용자 상태를 즉시 지우고 ONB-01로 이동합니다.

---

# 5. 프로필·정책 추천 조건 API (A-1 · E-3)

## 5.1 프로필 조회

`GET /profile` · **P0** · A-1-06 · A-1-07 · MY-01 · AN-07

**Response 200**

```json
{
  "name": "김수현",
  "birthDate": "1998-03-15",
  "gender": "FEMALE",
  "phoneNumber": "01091740339",
  "currentStatus": "EMPLOYED",
  "annualIncomeBand": "FROM_24M_TO_36M",
  "educationStatus": "UNIVERSITY_GRADUATE",
  "interestCategories": ["JOB", "HOUSING"],
  "ecoAddress": {
    "label": "서울특별시 관악구",
    "sidoCode": "11",
    "sigunguCode": "11620",
    "registeredAt": "2026-03"
  },
  "policyProfileCompleted": true
}
```

이름·생년월일·성별·휴대전화번호는 가입 시 본인인증 정보이며 이 API에서는 조회만 합니다. `ecoAddress`는 미연동이면 `null`이고 앱 안에서는 수정하지 않습니다.

---

## 5.2 정책 추천 조건 조회

`GET /profile/policy-preferences` · **P0** · E-3-04 · MY-02

**Response 200**

```json
{
  "birthDate": "1998-03-15",
  "currentStatus": "FREELANCER",
  "annualIncomeBand": "UNDER_24M",
  "educationStatus": "UNIVERSITY_GRADUATE",
  "interestCategories": ["JOB", "HOUSING"],
  "ecoAddress": { "label": "서울특별시 관악구", "sidoCode": "11", "sigunguCode": "11620" },
  "birthDateEditable": false,
  "regionEditable": false,
  "completed": true
}
```

생년월일은 가입 정보, 지역은 에코 연동 정보이므로 읽기 전용입니다. 아직 선택 정보를 저장하지 않았다면 선택 필드는 `null` 또는 빈 배열이고 `completed`는 `false`입니다. 저장된 값은 사용자가 다시 저장할 때까지 유지됩니다.

## 5.3 정책 추천 조건 저장

`PUT /profile/policy-preferences` · **P0** · E-3-04 · MY-02

```json
{
  "currentStatus": "FREELANCER",
  "annualIncomeBand": "UNDER_24M",
  "educationStatus": "UNIVERSITY_GRADUATE",
  "interestCategories": ["JOB", "HOUSING"]
}
```

현재 상태·연소득 구간·학력은 각각 필수이고 관심 분야는 중복 없이 1~2개가 필수입니다. 사용자가 MY-02에서 **저장하고 추천받기**를 눌렀을 때만 호출하며, 기존 저장값은 이 요청이 성공하기 전까지 유지됩니다. 생년월일·성별·휴대전화번호·지역·가구·혼인 상태·주거 형태·평수는 요청받지 않습니다. 정책 상세(MY-06)에서는 조건을 수정하지 않습니다.

**Response 200**

```json
{ "policyProfileCompleted": true, "recommendationsUpdated": true }
```

**Errors** `PROFILE_INCOMPLETE(409)`

- 결정 C-29 이후 1인 가구 진단 기준선은 지역에 따라 달라지지 않습니다.
- 화면 MY-01의 "에코마일리지 주소 · 2026-03 등록"은 **프로필 주소가 아니라 누리집 등록 주소**입니다(결정 C-8). 앱에서는 직접 수정하지 않으며 다음 에코마일리지 연동 때 갱신됩니다.

---

# 6. 고지서 API (A-2)

> **저장 단위** — 고지서 묶음(`upload_batch_id`)과 수정 이력은 관리하지 않기로 했습니다(결정 3). 고지서 한 장은 **(사용자 × BILL × 청구월 × 에너지원) 레코드 여러 건**으로 남고, 조회·수정·삭제는 전부 **레코드 단위**입니다.

## 6.1 등록 대상 월 조회

`GET /bills/target-month` · **P0** · A-2-01 · AN-01 · AN-02

**Response 200**

```json
{
  "targetYearMonth": "2026-08",
  "lastRegisteredMonth": "2026-07",
  "alreadyRegistered": false,
  "registeredUtilitiesInTarget": [],
  "nextScreen": "AN-02"
}
```

- `targetYearMonth` = **아직 등록하지 않은 가장 최근 고지 월** (현재 달이 아님, A-2-01 팀원 규칙 채택).
- 이미 다 등록됐으면 `alreadyRegistered: true` · `nextScreen: "AN-07"`.

---

## 6.2 고지서 OCR 요청

`POST /bills/ocr` · **P0** · A-2-03 · A-2-04 · AN-02 → AN-03

`Content-Type: multipart/form-data`

| 파트 | 타입 | 규칙 |
|---|---|---|
| `image` | file | JPG·PNG **1장, 10MB 이하** |
| `billingMonthHint` | string `YYYY-MM` | 선택. 대상 월 힌트 |

CLOVA Template OCR의 고정 데모 템플릿은 관리비 통합(43341)·개별 전기(43345)·개별 수도(43347)·개별 도시가스(43348) 4종입니다. OCR 응답의 `matchedTemplate.id`를 `billType`으로 변환하며, 등록되지 않은 템플릿은 인식 실패로 처리합니다.

**Response 202**

```json
{ "jobId": "ocr_01J8ZK3", "status": "PENDING", "progress": 0, "pollAfterMs": 1000 }
```

**Errors** `IMAGE_TOO_LARGE(413)` · `IMAGE_UNSUPPORTED(415)`

> 원본 이미지는 인식 직후 폐기하고 저장하지 않습니다(COM-11). DB에 이미지·OCR 작업 테이블이 없는 이유입니다.

---

## 6.3 OCR 진행·결과 조회

`GET /bills/ocr/{jobId}` · **P0** · A-2-04 · A-2-05 · A-2-07 · COM-08 · AN-03 → AN-04

**Response 200 — 진행 중**

```json
{ "jobId": "ocr_01J8ZK3", "status": "RUNNING", "progress": 62 }
```

**Response 200 — 완료**

```json
{
  "jobId": "ocr_01J8ZK3",
  "status": "SUCCEEDED",
  "progress": 100,
  "billType": "MANAGEMENT",
  "billingMonth": "2026-08",
  "partialRecognition": true,
  "items": [
    { "utilityType": "ELECTRICITY", "hasData": true,  "billingMonth": "2026-08", "amount": 43200, "usage": 210.000, "usageUnit": "kWh", "confidence": 0.9412, "recordStatus": "CONFIRMED" },
    { "utilityType": "WATER",       "hasData": true,  "billingMonth": "2026-08", "amount": 8900,  "usage": 10.000,  "usageUnit": "m3",  "confidence": 0.6120, "recordStatus": "REVIEW_REQUIRED" },
    { "utilityType": "GAS",         "hasData": false, "billingMonth": null, "amount": null, "usage": null, "usageUnit": "m3", "confidence": null, "recordStatus": null }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `items` | 관리비 통합은 전기·수도·가스 3건 고정이며, 인식 안 된 항목은 `hasData: false`입니다. 개별 전기·수도·도시가스는 해당 항목 1건만 반환합니다. |
| `confidence` | `< 0.7` 이면 서버가 `recordStatus: "REVIEW_REQUIRED"` 로 내려 "확인 필요" 배지 (A-2-05) |
| `partialRecognition` | 일부 항목만 인식 → 부분 인식 경고 배지 |

**Response 200 — 실패** (A-2-07)

```json
{ "jobId": "ocr_01J8ZK3", "status": "FAILED", "errorCode": "OCR_FAILED",
  "message": "사진에서 값을 읽지 못했어요. 지원하지 않는 양식이거나 글자가 흐릴 수 있어요.",
  "fallbackScreen": "AN-05" }
```

**인식값은 저장되지 않습니다.** 사용자가 AN-06에서 확정한 값만 `POST /bills` 로 들어갑니다.

---

## 6.4 중복 사전 확인

`GET /bills/duplicate-check` · **P0** · A-2-09 · 결정 A-3 · AN-05

| 쿼리 | 예 |
|---|---|
| `billingMonth` | `2026-08` |
| `utilityTypes` | `ELECTRICITY,WATER` (콤마 구분) |

**Response 200**

```json
{
  "billingMonth": "2026-08",
  "results": [
    { "utilityType": "ELECTRICITY", "duplicated": true,  "existingRecordId": 41 },
    { "utilityType": "WATER",       "duplicated": false, "existingRecordId": null }
  ]
}
```

관리비 통합 고지서와 개별 고지서가 같은 달에 들어와도 **겹치는 항목만** 막습니다(결정 A-3). 저장 시 서버가 다시 검증하므로 이 API는 폼 실시간 안내용입니다.

---

## 6.5 고지서 저장

`POST /bills` · **P0** · A-2-08 · A-2-09 · A-2-10 · A-2-11 · AN-06

```json
{
  "billingMonth": "2026-08",
  "billType": "MANAGEMENT",
  "inputSource": "OCR",
  "items": [
    { "utilityType": "ELECTRICITY", "amount": 43200, "usage": 210.0, "usageUnit": "kWh", "confidence": 0.9412 },
    { "utilityType": "WATER",       "amount": 8900,  "usage": 10.0,  "usageUnit": "m3",  "confidence": 0.6120 }
  ]
}
```

| 검증 | 규칙 | 에러 |
|---|---|---|
| 청구 월 | `YYYY-MM` | `INVALID_REQUEST` |
| 금액 | 정수 ≥ 0 | `INVALID_REQUEST` |
| 사용량 | 소수 ≥ 0, **필수** (B-4 월 감축률 계산에 필요) | `BILL_USAGE_REQUIRED` |
| 항목 수 | 1개 이상 | `BILL_ITEM_EMPTY` |
| 전기 | `inputSource=MANUAL` 이면 전기 필수 | `BILL_ELECTRICITY_REQUIRED` |
| 중복 | `(user, BILL, month, utility)` | `BILL_DUPLICATED` |

**"내용 없음" 항목은 아예 보내지 않습니다.** 행을 만들지 않아 합계·비교에서 자동 제외됩니다.

**Response 201**

```json
{
  "billingMonth": "2026-08",
  "records": [
    { "recordId": 51, "utilityType": "ELECTRICITY", "amount": 43200, "usage": 210.0, "recordStatus": "CONFIRMED" },
    { "recordId": 52, "utilityType": "WATER", "amount": 8900, "usage": 10.0, "recordStatus": "REVIEW_REQUIRED" }
  ],
  "totalAmount": 52100,
  "recalculated": { "diagnosisMonth": "2026-08", "monthlyReportUpdated": true, "roundId": 7 },
  "nextScreen": "AN-07"
}
```

저장 즉시 **진단(A-3)과 What-if 월 리포트(B-4-02)를 재계산**합니다(A-3-10 "등록·수정·삭제 즉시 갱신").

---

## 6.6 고지서 보관함 목록

`GET /bills` · **P1** · A-2-12 · MY-03

| 쿼리 | 값 | 설명 |
|---|---|---|
| `utility` | `ELECTRICITY`\|`GAS`\|`WATER` | 없으면 전체 탭 |
| `year` | `2026` | 연도 필터 |
| `page` · `size` | | 1.5절 |

**Response 200**

```json
{
  "content": [
    { "recordId": 51, "billingMonth": "2026-08", "utilityType": "ELECTRICITY", "billType": "MANAGEMENT",
      "amount": 43200, "usage": 210.0, "usageUnit": "kWh",
      "inputSource": "OCR", "recordStatus": "CONFIRMED", "registeredAt": "2026-09-01T10:22:00+09:00" }
  ],
  "page": 0, "size": 20, "totalElements": 14, "totalPages": 1, "hasNext": false,
  "counts": { "ALL": 14, "ELECTRICITY": 5, "WATER": 5, "GAS": 4 }
}
```

최신 월 우선 정렬. `counts` 는 탭 배지 — "필터 적용 시 건수가 실제 데이터와 일치한다"(A-2-12 완료 조건).

---

## 6.7 고지서 상세

`GET /bills/{recordId}` · **P1** · A-2-13 · AN-08

**Response 200**

```json
{
  "recordId": 51, "billingMonth": "2026-08", "utilityType": "ELECTRICITY", "billType": "MANAGEMENT",
  "amount": 43200, "usage": 210.0, "usageUnit": "kWh",
  "inputSource": "OCR", "confidence": 0.9412, "recordStatus": "CONFIRMED",
  "registeredAt": "2026-09-01T10:22:00+09:00", "updatedAt": "2026-09-01T10:22:00+09:00",
  "siblings": [ { "recordId": 52, "utilityType": "WATER", "amount": 8900 } ]
}
```

`siblings` = 같은 청구 월의 다른 에너지원. 묶음 ID가 없으므로 **청구 월로 묶습니다**(결정 3).

---

## 6.8 고지서 수정

`PUT /bills/{recordId}` · **P1** · A-2-13 · AN-08 (AN-05 폼 재사용)

```json
{ "amount": 41800, "usage": 203.0 }
```

**Response 200**

```json
{
  "recordId": 51, "amount": 41800, "usage": 203.0, "updatedAt": "2026-09-03T18:40:00+09:00",
  "recalculated": { "diagnosisMonth": "2026-08", "monthlyReportUpdated": true }
}
```

> **수정 이력은 남기지 않기로 했습니다**(결정 3). `updatedAt` 만 갱신하고, 무엇이 어떻게 바뀌었는지는 보관하지 않습니다.

## 6.9 고지서 삭제

`DELETE /bills/{recordId}` · **P1** · A-2-13

**Response 200**

```json
{ "deletedRecordId": 51, "recalculated": { "diagnosisMonth": "2026-08", "monthlyReportUpdated": true } }
```

삭제 확인 다이얼로그는 FE. 삭제 후 해당 월 진단·월 리포트를 재계산합니다.

---

# 7. 진단 API (A-3)

## 7.1 등록된 청구 월 목록

`GET /diagnosis/months` · **P1** · A-3-09 · AN-07 헤더 드롭다운

**Response 200**

```json
{
  "months": [
    { "yearMonth": "2026-08", "registered": true, "utilities": ["ELECTRICITY","WATER","GAS"], "totalAmount": 64500 },
    { "yearMonth": "2026-07", "registered": true, "utilities": ["ELECTRICITY","WATER","GAS"], "totalAmount": 62600 }
  ],
  "defaultMonth": "2026-08"
}
```

미등록 월은 목록에 넣지 않습니다. FE가 드롭다운을 등록 월로만 채우고, 없는 달은 등록 안내로 연결합니다(A-3-09).

---

## 7.2 진단 결과 (AN-07 한 번에)

`GET /diagnosis` · **P0** · A-3-04 · A-3-05 · A-3-06 · A-3-07 · A-3-08 · AN-01 · AN-07

| 쿼리 | 필수 | 설명 |
|---|---|---|
| `month` | | 없으면 최신 등록 월 |

**Response 200 — 빈 상태** (A-3-04)

```json
{ "empty": true, "targetYearMonth": "2026-08", "screen": "AN-01" }
```

**Response 200 — 정상**

```json
{
  "empty": false,
  "screen": "AN-07",
  "yearMonth": "2026-08",
  "profileSummary": "서울 관악구 · 아파트 20평 이상",

  "summary": {
    "currentTotal": 64500,
    "previousYearTotal": 62600,
    "diffLastYearTotal": 1900,
    "hasPreviousYear": true,
    "items": [
      { "utilityType": "ELECTRICITY", "amount": 43200, "usage": 210.0, "usageUnit": "kWh" },
      { "utilityType": "GAS",         "amount": 12400, "usage": 14.0,  "usageUnit": "m3"  },
      { "utilityType": "WATER",       "amount": 8900,  "usage": 10.0,  "usageUnit": "m3"  }
    ]
  },

  "lastYearComparison": {
    "available": true,
    "unavailableReason": null,
    "totalDiff": 1900,
    "items": [
      { "utilityType": "ELECTRICITY", "lastYearAmount": 40100, "thisYearAmount": 43200, "diff": 3100 },
      { "utilityType": "GAS",         "lastYearAmount": 14200, "thisYearAmount": 12400, "diff": -1800 },
      { "utilityType": "WATER",       "lastYearAmount": 8300,  "thisYearAmount": 8900,  "diff": 600 }
    ]
  },

  "singleHouseholdComparison": {
    "comparisonLabel": "1인 가구 평균 사용량",
    "tabs": [
      {
        "utilityType": "ELECTRICITY",
        "available": true,
        "unavailableReason": null,
        "myUsage": 210.000,
        "averageUsage": 257.617,
        "differenceUsage": -47.617,
        "differenceRate": -18.484,
        "usageUnit": "kWh",
        "comparisonLabel": "전국 1인 가구",
        "sourceName": "에너지경제연구원 2023년 기준 14차 가구에너지패널조사 마이크로데이터",
        "referencePeriod": "2023",
        "calculationBasis": "WEIGHTED_MONTHLY_MICRODATA_AVERAGE",
        "note": "유효한 가구 횡단가중치가 있는 전국 1인 가구 1,221가구의 월별 전기 사용량 추정평균입니다.",
        "series": [
          { "yearMonth": "2026-03", "myUsage": 195.000, "averageUsage": 189.658 },
          { "yearMonth": "2026-04", "myUsage": null,    "averageUsage": 184.783 },
          { "yearMonth": "2026-05", "myUsage": 188.000, "averageUsage": 179.013 },
          { "yearMonth": "2026-06", "myUsage": 190.000, "averageUsage": 186.260 },
          { "yearMonth": "2026-07", "myUsage": 205.000, "averageUsage": 228.449 },
          { "yearMonth": "2026-08", "myUsage": 210.000, "averageUsage": 257.617 }
        ]
      },
      {
        "utilityType": "GAS",
        "available": true,
        "myUsage": 14.000,
        "averageUsage": 16.781,
        "differenceUsage": -2.781,
        "differenceRate": -16.572,
        "usageUnit": "m3",
        "comparisonLabel": "전국 1인 도시가스 사용 가구",
        "referencePeriod": "2023",
        "calculationBasis": "WEIGHTED_MONTHLY_MICRODATA_AVERAGE",
        "series": [
          { "yearMonth": "2026-03", "myUsage": 55.000, "averageUsage": 68.353 },
          { "yearMonth": "2026-04", "myUsage": null,    "averageUsage": 47.285 },
          { "yearMonth": "2026-05", "myUsage": 30.000, "averageUsage": 34.182 },
          { "yearMonth": "2026-06", "myUsage": 20.000, "averageUsage": 23.414 },
          { "yearMonth": "2026-07", "myUsage": 17.000, "averageUsage": 18.890 },
          { "yearMonth": "2026-08", "myUsage": 14.000, "averageUsage": 16.781 }
        ]
      },
      {
        "utilityType": "WATER",
        "available": true,
        "myUsage": 10.000,
        "averageUsage": 13.578,
        "differenceUsage": -3.578,
        "differenceRate": -26.351,
        "usageUnit": "m3",
        "comparisonLabel": "서울 아파트 1인 가구",
        "referencePeriod": "2018",
        "calculationBasis": "DAILY_USAGE_MONTH_EQUIVALENT",
        "series": [
          { "yearMonth": "2026-03", "myUsage": 10.000, "averageUsage": 13.578 },
          { "yearMonth": "2026-04", "myUsage": null,    "averageUsage": 13.140 },
          { "yearMonth": "2026-05", "myUsage": 9.000,  "averageUsage": 13.578 },
          { "yearMonth": "2026-06", "myUsage": 9.000,  "averageUsage": 13.140 },
          { "yearMonth": "2026-07", "myUsage": 10.000, "averageUsage": 13.578 },
          { "yearMonth": "2026-08", "myUsage": 10.000, "averageUsage": 13.578 }
        ]
      }
    ]
  },

  "whatIfLink": { "roundId": 7, "goalSet": true }
}
```

| 규칙 | 내용 |
|---|---|
| `available: false` | 해당 에너지원의 1인 가구 기준이 없으면 **임의 값을 만들지 않고** 이 플래그로 "비교 데이터 준비 중"을 띄움 (A-3-03 · 비즈니스 규칙 8) |
| `unavailableReason` | 현재 `NO_BASELINE` |
| `differenceUsage` · `differenceRate` | **양수 = 평균 초과, 음수 = 평균 미만.** 부호 그대로 내려주고 표기는 FE (A-3-08) |
| `calculationBasis` | `WEIGHTED_MONTHLY_MICRODATA_AVERAGE`는 월별 원자료의 가구 횡단가중 평균, `ANNUAL_ENERGY_SHARE_MONTHLY_EQUIVALENT`는 하위 호환용 연간 집계 월평균 환산, `DAILY_USAGE_MONTH_EQUIVALENT`는 일 사용량 × 조회 월 일수 |
| `series` | 선택 월을 포함한 최근 6개월을 과거순으로 제공. `myUsage`는 해당 월 고지서가 없으면 `null`이며 서버가 보간하지 않음 |
| `comparisonLabel` · `referencePeriod` · `note` | 비교군·기준 연도·자료 한계를 숨기지 않고 화면에 함께 표시 (결정 C-29·C-35) |
| `hasPreviousYear: false` | 작년 값 없음 → 배지·카드 숨김. 에코마일리지 연동 유도 (A-3-05 · A-3-06) |

**Errors** `DIAGNOSIS_MONTH_EMPTY(404)` — 등록 안 된 월을 지정했을 때

---

## 7.3 1인 가구 사용량 기준선 단건 조회 (내부·검증용)

`GET /diagnosis/baseline` · **P0** · A-3-02 · A-3-03

| 쿼리 | 예 |
|---|---|
| `month` | `2026-07` |
| `utility` | `ELECTRICITY` |

- 지역·주거형태 파라미터는 받지 않습니다. 전기·도시가스는 KESIS 2023년 기준 14차 가구에너지패널조사 마이크로데이터의 전국 1인 가구 월별 가중평균, 수도는 서울 아파트 1인 가구 조사값을 사용합니다.
- 전기·도시가스는 `month`의 월 번호에 맞는 월별 평균을, 수도는 실제 일수에 따른 월 환산값을 반환합니다.
- 런타임 외부 API를 호출하지 않고 `resources/data/single-household-utility-baselines.json`을 읽습니다.

**Response 200**

```json
{
  "found": true,
  "targetYearMonth": "2026-07",
  "utilityType": "ELECTRICITY",
  "comparisonLabel": "전국 1인 가구",
  "averageUsage": 228.449,
  "usageUnit": "kWh",
  "sourceName": "에너지경제연구원 2023년 기준 14차 가구에너지패널조사 마이크로데이터",
  "referencePeriod": "2023",
  "calculationBasis": "WEIGHTED_MONTHLY_MICRODATA_AVERAGE",
  "note": "유효한 가구 횡단가중치가 있는 전국 1인 가구 1,221가구의 월별 전기 사용량 추정평균입니다."
}
```

해당 에너지원 기준이 없으면 `found: false` — A-3-03 "임의 값을 만들지 않는다". 이때 요청 맥락인 `targetYearMonth` · `utilityType` 은 유지하고 기준선 값은 `null` 로 반환합니다.
7.2가 이 로직을 내부에서 쓰므로 FE는 보통 호출하지 않습니다. **시드·출처 검증용**으로 남깁니다.

---

# 8. 에코마일리지 연동 API (B-1)

## 8.1 연동 상태 조회

`GET /eco/status` · **P0** · COM-12 · B-1-01 · B-1-04 · B-1-09 · WF-01

엔드포인트 이름은 팀원본 그대로 씁니다(결정 A-5).

**Response 200**

```json
{
  "linkStatus": "LINKED",
  "linkedAt": "2026-09-01T09:00:00+09:00",
  "seoulResident": true,
  "linkable": true,
  "blockReason": null,
  "registeredUtilities": [
    { "utilityType": "ELECTRICITY", "registered": true,  "unregisteredReason": null },
    { "utilityType": "GAS",         "registered": true,  "unregisteredReason": null },
    { "utilityType": "WATER",       "registered": false, "unregisteredReason": "세대 명의 계약이 없어 사용량을 불러올 수 없어요" }
  ],
  "eligibleForRound": true,
  "ecoAddress": {
    "label": "서울 관악구",
    "sidoCode": "11", "sigunguCode": "11620",
    "registeredAt": "2026-03",
    "matchesProfile": true
  },
  "externalUrl": "https://ecomileage.seoul.go.kr"
}
```

| 필드 | 설명 |
|---|---|
| `linkable` | 서울 거주 + 미연동일 때 `true`. 서울 밖이면 `false` + `blockReason: "NOT_SEOUL"` → 버튼 비활성 (B-1-09) |
| `registeredUtilities` | **항상 3건 고정.** 세그먼트 3개 고정 규칙(B-2-02) |
| `eligibleForRound` | 전기 등록 && 등록 요금 ≥ 2개 (B-1-04). `false` 면 "평가 대상 아님" 안내 |
| `unregisteredReason` | `eco_round_utility.unregistered_reason` 문장 그대로 (WF-05에 노출) |
| `ecoAddress` | 에코마일리지 누리집 등록 주소를 연동 때 받아 저장한 값. 정책 추천·지역 진단의 단일 지역 기준이며 미연동이면 `null` |

---

## 8.2 연동 시작 (모의)

`POST /eco/link` · **P0** · B-1-02 · COM-05 · WF-01 → WF-02

외부 누리집으로 이동했다가 **복귀한 뒤** 호출합니다. 외부 이동 없이 호출해도 시드가 적용되지만, 화면 흐름은 COM-05를 따릅니다.

**Request** — 본문 없음

**Response 202**

```json
{ "linkJobId": "eco_01J8ZM7", "status": "RUNNING", "estimatedSeconds": 20 }
```

동시에 `app_user.eco_link_status = 'LINKING'` 으로 바뀝니다.

**Errors** `ECO_NOT_SEOUL(403)`

---

## 8.3 연동 진행 조회

`GET /eco/link/{linkJobId}` · **P0** · B-1-03 · COM-08 · WF-02

**Response 200 — 진행 중**

```json
{
  "linkJobId": "eco_01J8ZM7",
  "status": "RUNNING",
  "elapsedSeconds": 12,
  "utilityStatus": [
    { "utilityType": "ELECTRICITY", "status": "SUCCEEDED" },
    { "utilityType": "GAS",         "status": "SUCCEEDED" },
    { "utilityType": "WATER",       "status": "RUNNING"   }
  ]
}
```

**Response 200 — 완료**

```json
{
  "linkJobId": "eco_01J8ZM7",
  "status": "SUCCEEDED",
  "linkedAt": "2026-09-01T09:00:00+09:00",
  "roundId": 7,
  "registeredUtilities": ["ELECTRICITY","GAS","WATER"],
  "baselineMonthsLoaded": 24,
  "ecoAddress": { "label": "서울 관악구", "sidoCode": "11", "sigunguCode": "11620", "registeredAt": "2026-03" },
  "nextScreen": "WF-03"
}
```

| 규칙 | 내용 |
|---|---|
| 20초 초과 | `status: "TIMEOUT"` → 재시도 버튼 (B-1-03) |
| 부분 실패 | `status: "PARTIAL"` — 성공한 요금 데이터는 그대로 유지. 실패 항목만 재시도 |
| 저장 | 직전 2년 월별 사용량을 `utility_monthly_record(record_source='ECO_BASELINE')` 로, 회차·기준값을 `eco_round` · `eco_round_utility` 로 적재. 누리집 등록 주소는 `app_user.eco_*` 4컬럼에 저장(결정 8) |

**Errors** `ECO_LINK_FAILED(502)`

---

## 8.4 현재 평가 회차 (기준 사용량 · 비중)

`GET /eco/rounds/current` · **P0** · B-1-05 · B-1-06 · B-1-07 · B-2-01 · WF-03

**Response 200**

```json
{
  "roundId": 7,
  "periodStart": "2026-04",
  "periodEnd": "2026-09",
  "remainingMonths": 2,
  "roundStatus": "GOAL_SET",
  "applicationStatus": "NOT_APPLIED",
  "goalSet": true,
  "baselineQueriedAt": "2026-09-01T09:00:00+09:00",
  "baselineDescription": "2024·2025년 4~9월 평균",

  "baseline": {
    "totalAmount": 420600,
    "totalCarbonG": 831992.000,
    "items": [
      { "utilityType": "ELECTRICITY", "registered": true,  "amount": 268000, "usage": 1340.000, "usageUnit": "kWh", "carbonFactorG": 424.000, "shareRate": 64.000 },
      { "utilityType": "GAS",         "registered": true,  "amount": 96600,  "usage": 108.000,  "usageUnit": "m3",  "carbonFactorG": 2240.000, "shareRate": 23.000 },
      { "utilityType": "WATER",       "registered": true,  "amount": 56000,  "usage": 66.000,   "usageUnit": "m3",  "carbonFactorG": 332.000,  "shareRate": 13.000 }
    ],
    "largestShareUtility": "ELECTRICITY"
  },

  "nextScreen": "WF-06"
}
```

| 규칙 | 내용 |
|---|---|
| `remainingMonths` | **저장하지 않고 `periodEnd` − 현재 월로 계산.** 시연 중 날짜가 바뀌어도 어긋나지 않음 (DB 설계서 4.4) |
| `shareRate` | 기준 요금 비중. **합이 100** (B-1-06 완료 조건) |
| `carbonFactorG` | 회차 스냅샷값. 전기 424 · 수도 332 · 가스 2,240 (g/단위) |
| `goalSet` | `eco_round.goal_set_at != null`. `false` → WF-03, `true` → WF-06 |
| `items` | 미등록 요금도 행을 만들되 `registered: false`, 기준값 `null` (B-2-02 세그먼트 3개 고정) |
| 총액 0 | `baseline.totalAmount == 0` 이면 `shareRate` 전부 `null` → 비중 카드 숨김 (B-1-06 예외) |

**Errors** `ECO_NOT_LINKED(409)` · `ECO_ROUND_NOT_FOUND(404)`

---

## 8.5 평가 회차 목록

`GET /eco/rounds` · **P1** · B-5-04 · MY-04

**Response 200**

```json
{
  "content": [
    { "roundId": 7, "periodStart": "2026-04", "periodEnd": "2026-09", "roundStatus": "IN_PROGRESS", "finalRate": null,  "confirmedMileage": 0 },
    { "roundId": 6, "periodStart": "2025-10", "periodEnd": "2026-03", "roundStatus": "CLOSED",      "finalRate": 12.000, "confirmedMileage": 30000 }
  ]
}
```

이전 기간 목표·결과는 삭제하지 않습니다(B-5-04 완료 조건).

---

# 9. 목표·미션 API (B-2 · B-3)

## 9.1 목표 정하기 화면 데이터

`GET /eco/rounds/{roundId}/goal-form` · **P0** · B-2-02 · B-2-03 · B-2-06 · B-3-01 · B-3-03 · WF-04 · WF-05

기준값 + 구간 칩 + 미션 목록을 **한 번에** 내려 WF-04를 한 요청으로 그립니다.

마스터 카탈로그는 요금별 9~12개를 유지합니다. API는 프론트가 계절·목표 구간·추천 맥락에 따라 6~9개를 고를 수 있도록 전체 활성 미션과 `seasonTags`를 내려줍니다. 미션은 임대 주거의 청년이 직접 수행할 수 있는 습관 행동으로 한정하고, 난이도는 비용·설비 권한이 아니라 반복 강도를 뜻합니다(C-19).

**Response 200**

```json
{
  "roundId": 7,
  "periodStart": "2026-04", "periodEnd": "2026-09",
  "tiers": [
    { "tier": "TIER_5",  "label": "5~10%",   "targetRate": 5.000,  "mileage": 10000 },
    { "tier": "TIER_10", "label": "10~15%",  "targetRate": 10.000, "mileage": 30000 },
    { "tier": "TIER_15", "label": "15% 이상", "targetRate": 15.000, "mileage": 50000 }
  ],
  "segments": [
    {
      "utilityType": "ELECTRICITY",
      "registered": true,
      "unregisteredReason": null,
      "baselineAmount": 268000,
      "baselineUsage": 1340.000,
      "monthlyBaselineUsage": 223.333,
      "usageUnit": "kWh",
      "missionRateCap": 30.000,
      "selectedTier": "TIER_10",
      "missions": [
        {
          "missionId": 12, "missionCode": "AC_TEMP_26",
          "title": "냉방 온도 26℃로 맞추기",
          "description": "여름철 권장 실내 냉방온도예요",
          "difficulty": "EASY",
          "evidenceAmount": 7.000, "evidenceUnit": "kWh",
          "evidenceText": "1℃당 냉방 전력 7%",
          "calculationBasis": "실내온도를 1℃ 내리면 전력을 7% 더 씀 · 냉방을 여름 전기의 40%로 가정",
          "sourceOrg": "한국에너지공단",
          "deviceGroup": "냉방",
          "seasonTags": ["SUMMER"],
          "computedRate": 3.000,
          "capped": false,
          "selected": true
        },
        {
          "missionId": 13, "missionCode": "AC_HOUR_1",
          "title": "에어컨 하루 1시간 줄이기",
          "description": "켜 두는 시간만 줄여도 크게 달라져요",
          "difficulty": "NORMAL",
          "evidenceAmount": 40.000, "evidenceUnit": "kWh",
          "evidenceText": "월 40kWh · 4,880원",
          "calculationBasis": "15평형 2kW를 20일 기준 · 40kWh ÷ 우리 집 223kWh",
          "sourceOrg": "한국에너지공단",
          "deviceGroup": "냉방",
          "seasonTags": ["SUMMER"],
          "computedRate": 18.000,
          "capped": false,
          "selected": true
        }
      ]
    },
    {
      "utilityType": "WATER",
      "registered": false,
      "unregisteredReason": "세대 명의 계약이 없어 사용량을 불러올 수 없어요",
      "baselineAmount": null, "baselineUsage": null,
      "registerGuideUrl": "https://ecomileage.seoul.go.kr",
      "excludedFromCombine": true,
      "selectedTier": null,
      "missions": [ ]
    }
  ]
}
```

| 규칙 | 내용 |
|---|---|
| 미션 노출 | `evidence_amount` · `calculation_basis` · `source_org` **셋 다 있는 것만.** DB가 NOT NULL로 강제 (B-3-01) |
| 카탈로그·화면 수 | API는 요금별 활성 미션 9~12개를 `display_order` 순으로 반환. FE는 **선택 미션 유지 → 현재 계절 → 서로 다른 `deviceGroup` → 서버 반환 순서**로 6~9개를 고르고, 다시 고르기에서는 `recommended: true`를 먼저 노출 (C-19) |
| 난이도 | `EASY`·`NORMAL`·`HARD`는 한 달 동안 지속할 습관 변화 강도. 공사·설비 교체·전문업체 작업은 카탈로그에서 제외 (C-19) |
| `computedRate` | `evidenceAmount ÷ (baselineUsage ÷ 6) × 100`, 상한 전기 30% · 수도 20% 적용 (B-3-02 · 계산식 11) |
| `monthlyBaselineUsage` | `baselineUsage ÷ 6`. 환산 근거를 FE가 그대로 보여줄 수 있게 함 |
| `capped` | 상한이 걸려 잘렸으면 `true` → "한 미션 상한 30% 적용" 문구 |
| 미등록 요금 | 구간 칩 없음 · `excludedFromCombine: true`. 미션 목록은 그대로 노출하되 합계에 넣지 않음 (B-2-06) |
| `baselineUsage = 0` | `computedRate: null` — 환산하지 않음 (B-3-02 예외) |

---

## 9.2 목표 미리보기 (저장 전 실시간 계산)

`POST /eco/rounds/{roundId}/goal/preview` · **P0** · B-2-04 · B-2-05 · B-2-07 · B-3-04 · WF-04

구간 칩·미션 체크를 바꿀 때마다 호출합니다. **아무것도 저장하지 않습니다.**

**Request**

```json
{
  "targets": [
    { "utilityType": "ELECTRICITY", "tier": "TIER_10" },
    { "utilityType": "GAS",         "tier": "TIER_15" },
    { "utilityType": "WATER",       "tier": "TIER_5"  }
  ],
  "selectedMissionIds": [12, 13, 21]
}
```

**Response 200**

```json
{
  "utilities": [
    { "utilityType": "ELECTRICITY", "targetRate": 10.000, "baselineUsage": 1340.000, "targetUsage": 1206.000, "usageUnit": "kWh",
      "baselineAmount": 268000, "expectedSaving": 26800, "displayPrecision": 0 },
    { "utilityType": "GAS",         "targetRate": 15.000, "baselineUsage": 108.000,  "targetUsage": 91.800,  "usageUnit": "m3",
      "baselineAmount": 96600,  "expectedSaving": 14490, "displayPrecision": 1 },
    { "utilityType": "WATER",       "targetRate": 5.000,  "baselineUsage": 66.000,   "targetUsage": 62.700,  "usageUnit": "m3",
      "baselineAmount": 56000,  "expectedSaving": 2800,  "displayPrecision": 1 }
  ],

  "combined": {
    "baselineCarbonG": 831992.000,
    "targetCarbonG": 737792.400,
    "combinedRate": 11.322,
    "tier": "TIER_10",
    "tierLabel": "10~15%",
    "expectedMileage": 30000,
    "totalExpectedSaving": 44090,
    "baselineTotalAmount": 420600,
    "excludedUtilities": [],
    "nextTier": { "tier": "TIER_15", "gapPoint": 3.678, "mileage": 50000 }
  },

  "missions": {
    "combinedMissionRate": 18.000,
    "shortfallPoint": 0.000,
    "meetsTarget": true,
    "items": [
      { "missionId": 13, "computedRate": 18.000, "counted": true,  "exclusionReason": null },
      { "missionId": 12, "computedRate": 3.000,  "counted": false, "exclusionReason": "냉방 겹침 · 합계 제외" },
      { "missionId": 21, "computedRate": 5.000,  "counted": true,  "exclusionReason": null }
    ]
  },

  "carbonFactors": [
    { "utilityType": "ELECTRICITY", "factorG": 424.000, "unit": "kWh" },
    { "utilityType": "WATER",       "factorG": 332.000, "unit": "m3"  },
    { "utilityType": "GAS",         "factorG": 2240.000,"unit": "m3"  }
  ]
}
```

**계산 규칙** (기능명세 「비즈니스 규칙·계산식」 시트 3·4·5·6·7·11·12)

```
targetUsage_i        = baselineUsage_i × (1 − targetRate_i / 100)
expectedSaving_i     = baselineAmount_i × targetRate_i / 100
baselineCarbonG      = Σ (baselineUsage_i × carbonFactorG_i)     # 등록 요금만
targetCarbonG        = Σ (targetUsage_i   × carbonFactorG_i)     # 등록 요금만
combinedRate         = (baselineCarbonG − targetCarbonG) / baselineCarbonG × 100
tier                 = 5 ≤ R < 10 → 10,000M · 10 ≤ R < 15 → 30,000M · R ≥ 15 → 50,000M · R < 5 → 0
combinedMissionRate  = Σ_deviceGroup max(computedRate)           # 같은 기기 그룹은 최대 하나
```

| 규칙 | 내용 |
|---|---|
| `displayPrecision` | 전기 `0`(정수 kWh), 수도·가스 `1`(소수 첫째 자리) — B-2-04 표시 규칙 |
| `excludedUtilities` | 미등록 요금 목록. 있으면 FE가 "등록된 N·M만 평가에 들어가요" 표시 (B-2-06) |
| `nextTier.gapPoint` | 다음 구간까지 남은 %p |
| `counted: false` | 같은 `deviceGroup` 중 `computedRate` 최대값만 `true` (B-3-04) |
| 경계 | `combinedRate` 가 정확히 5·10·15 일 때 상위 구간 (B-2-07 완료 조건) |

**Errors** `ECO_TIER_INVALID(400)` · `ECO_UTILITY_NOT_REGISTERED(409)`

---

## 9.3 목표 저장 · 수정

`POST /eco/rounds/{roundId}/goal` (최초) · `PUT /eco/rounds/{roundId}/goal` (수정) · **P0** · B-2-08 · B-3-03 · WF-04 → WF-06

Request 본문은 9.2 미리보기와 **동일**합니다.

**Response 200** — 9.2 응답 + 저장 결과

```json
{
  "roundId": 7,
  "goalSetAt": "2026-04-02T21:10:00+09:00",
  "roundStatus": "GOAL_SET",
  "combinedTargetRate": 11.322,
  "expectedMileage": 30000,
  "expectedSavingAmount": 44090,
  "savedMissionCount": 3,
  "nextScreen": "WF-06"
}
```

- 저장 단위: **평가 기간당 1세트** (항목별 구간·목표율·목표 사용량 + 선택 미션).
- 서버가 `eco_round`(합산값) · `eco_round_utility`(항목별) · `user_mission`(선택 미션 + `computed_rate` · `is_counted` · `exclusion_reason`)에 나눠 저장합니다.
- 구간을 1개 이상 고르지 않으면 `409 ECO_TIER_INVALID`.

## 9.4 저장된 목표 조회

`GET /eco/rounds/{roundId}/goal` · **P0** · B-2-08 · B-4-06 · WF-06 목표 카드

**Response 200**

```json
{
  "roundId": 7,
  "goalSet": true,
  "goalSetAt": "2026-04-02T21:10:00+09:00",
  "combinedTargetRate": 11.322,
  "tier": "TIER_10",
  "expectedMileage": 30000,
  "expectedSavingAmount": 44090,
  "utilities": [
    { "utilityType": "ELECTRICITY", "targetTier": "TIER_10", "targetRate": 10.000, "baselineUsage": 1340.000, "targetUsage": 1206.000, "usageUnit": "kWh", "expectedSaving": 26800 },
    { "utilityType": "GAS",         "targetTier": "TIER_15", "targetRate": 15.000, "baselineUsage": 108.000,  "targetUsage": 91.800,  "usageUnit": "m3",  "expectedSaving": 14490 },
    { "utilityType": "WATER",       "targetTier": "TIER_5",  "targetRate": 5.000,  "baselineUsage": 66.000,   "targetUsage": 62.700,  "usageUnit": "m3",  "expectedSaving": 2800 }
  ],
  "missions": [
    { "missionId": 13, "title": "에어컨 하루 1시간 줄이기", "utilityType": "ELECTRICITY", "computedRate": 18.000, "counted": true, "exclusionReason": null }
  ]
}
```

`goalSet: false` 면 나머지 필드가 전부 `null` → WF-03 렌더.

---

## 9.5 오늘의 실천 조회

`GET /eco/rounds/{roundId}/missions/today` · **P1** · B-3-05 · WF-06

| 쿼리 | 기본 | 설명 |
|---|---|---|
| `date` | 오늘 | `YYYY-MM-DD` |

**Response 200**

```json
{
  "date": "2026-09-03",
  "season": "SUMMER",
  "completedCount": 3,
  "totalCount": 5,
  "missions": [
    { "missionId": 12, "title": "냉방 온도 26℃로 맞추기", "utilityType": "ELECTRICITY", "difficulty": "EASY", "completed": true  },
    { "missionId": 31, "title": "온수 온도 55℃ → 40℃로 낮추기", "utilityType": "GAS", "difficulty": "NORMAL", "completed": false }
  ],
  "emptyReason": null
}
```

- 목표 설정 때 고른 미션 중 **현재 계절 태그(`FIND_IN_SET`)에 맞는 것만** 노출 (B-3-05). 계절은 3~5월 봄 · 6~8월 여름 · 9~11월 가을 · 12~2월 겨울. FE 는 `season` 으로 「가을에 맞는 실천만 보여요」를 밝힌다.
- 계절 필터 후 0개면 `missions: []` + `emptyReason: "SEASON_FILTERED_EMPTY"` → "다시 고르기" 유도.

## 9.6 오늘의 실천 체크 저장

`PUT /eco/rounds/{roundId}/mission-logs/{date}` · **P1** · B-3-05 · WF-06

```json
{ "completedMissionIds": [12, 31, 44] }
```

**Response 200**

```json
{ "date": "2026-09-03", "completedCount": 3, "totalCount": 5 }
```

하루 한 행(`mission_daily_log`)을 **통째로 덮어씁니다.** 체크 토글이 한 번의 UPDATE로 끝나게 한 설계입니다.

---

# 10. 진행 현황·전달 리포트 API (B-4)

## 10.1 What-if 홈 (WF 화면 라우팅)

`GET /eco/home` · **P0** · B-4-01 · B-4-03 · B-4-04 · B-4-05 · B-4-06 · B-4-10 · B-5-01 · WF-01 · WF-02 · WF-03 · WF-06 · WF-09

**What-if 탭이 홈**이라 진입 시 이 하나만 부르면 어떤 화면을 그릴지 결정됩니다(COM-02).

**Response 200**

```json
{
  "screen": "WF_06_IN_PROGRESS",
  "roundId": 7,
  "header": { "periodStart": "2026-04", "periodEnd": "2026-09", "remainingMonths": 2, "remainingLabelMonths": [8, 9] },

  "progress": {
    "cumulativeRate": 9.000,
    "coveredMonths": ["2026-04","2026-05","2026-06","2026-07"],
    "currentTier": "TIER_5",
    "targetTier": "TIER_10",
    "tiers": [
      { "tier": "TIER_5",  "mileage": 10000, "state": "CURRENT" },
      { "tier": "TIER_10", "mileage": 30000, "state": "TARGET"  },
      { "tier": "TIER_15", "mileage": 50000, "state": "NONE"    }
    ],
    "gapToNextTierPoint": 1.000,
    "nextTierMileage": 30000
  },

  "latestReport": {
    "available": true,
    "reportMonth": "2026-07",
    "billRegisteredAt": "2026-08-03T00:00:00+09:00",
    "monthlyRate": 1.039,
    "targetRate": 10.000,
    "achieved": false
  },

  "application": { "status": "NOT_APPLIED", "showBanner": true, "externalUrl": "https://ecomileage.seoul.go.kr" },

  "goal": { "goalSet": true, "combinedTargetRate": 11.322, "tier": "TIER_10", "expectedMileage": 30000 },

  "todayMissions": { "completedCount": 3, "totalCount": 5 },

  "resultModal": null,

  "links": { "benefitTab": true, "pocketTab": true, "movingNotice": true }
}
```

**`screen` 값과 렌더 규칙**

| `screen` | 조건 | 화면 |
|---|---|---|
| `WF_01_UNLINKED` | `eco_link_status = UNLINKED` 또는 `FAILED` | WF-01 연동 전 |
| `WF_02_LINKING` | `LINKING` | WF-02 로딩 |
| `WF_03_NO_GOAL` | 연동됨 + `goal_set_at = null` | WF-03 목표 미설정 |
| `WF_06_IN_PROGRESS` | 목표 있음 | WF-06 메인 |
| `WF_09_RESULT_READY` | 직전 회차 `CONFIRMED` + `result_viewed_at = null` | WF-09 결산 모달 → 닫으면 아래 화면 |

`WF_09_RESULT_READY` 일 때 `resultModal` 이 채워집니다.

```json
"resultModal": {
  "roundId": 6,
  "periodStart": "2025-10", "periodEnd": "2026-03",
  "finalRate": 12.000, "tier": "TIER_10", "mileage": 30000,
  "confirmedAt": "2026-06-05T00:00:00+09:00"
}
```

- `latestReport.available: false` → 고지서 없음. 진단 탭 등록 유도 (B-4-04 예외).
- `application.showBanner` = 현재 회차 미신청 (B-4-05).
- `gapToNextTierPoint` = 다음 구간 하한 − 누적 감축률. FE 문구 "1%p만 더 줄이면 30,000M 구간이에요".

---

## 10.2 결산 모달 확인 처리

`POST /eco/rounds/{roundId}/result/view` · **P1** · B-5-01 · WF-09

**Response 204**

`eco_round.result_viewed_at` 을 채워 **다시 띄우지 않습니다.** 결과는 이후 리포트 보관함(E-2-01)에서 봅니다.

---

## 10.3 전달 리포트 상세

`GET /eco/monthly-report` · **P0** · B-4-02 · B-4-07 · B-4-08 · WF-07

| 쿼리 | 기본 | 설명 |
|---|---|---|
| `month` | 최신 등록 월 | `YYYY-MM` |

**Response 200**

```json
{
  "reportMonth": "2026-07",
  "roundId": 7,
  "billRegisteredAt": "2026-08-03T00:00:00+09:00",
  "baselineDescription": "2024·2025년 7월 평균",

  "result": {
    "monthlyRate": 1.039,
    "targetRate": 10.000,
    "achieved": false,
    "cumulativeRate": 9.000,
    "cumulativeMonths": ["2026-04","2026-05","2026-06","2026-07"]
  },

  "cause": {
    "byUtility": [
      { "utilityType": "ELECTRICITY", "baselineUsage": 265.000, "actualUsage": 270.000, "usageUnit": "kWh",
        "rate": -1.887, "achieved": false, "carbonSharePercent": 82.5, "expanded": true },
      { "utilityType": "GAS",   "baselineUsage": 9.000,  "actualUsage": 7.600, "usageUnit": "m3",
        "rate": 15.556, "achieved": true, "carbonSharePercent": 14.8, "expanded": false },
      { "utilityType": "WATER", "baselineUsage": 11.000, "actualUsage": 9.800, "usageUnit": "m3",
        "rate": 10.909, "achieved": true, "carbonSharePercent": 2.7,  "expanded": false }
    ],
    "largestCarbonUtility": "ELECTRICITY",
    "carbonFactors": [
      { "utilityType": "ELECTRICITY", "factorG": 424.000, "unit": "kWh" },
      { "utilityType": "WATER",       "factorG": 332.000, "unit": "m3"  },
      { "utilityType": "GAS",         "factorG": 2240.000,"unit": "m3"  }
    ]
  },

  "prescription": {
    "remainingMonths": 2,
    "remainingMonthLabels": [8, 9],
    "requiredRate": 11.981,
    "achievable": true,
    "requiredByUtility": [
      { "utilityType": "ELECTRICITY", "requiredRate": 11.374, "assumption": "도시가스 16%, 수도 11% 감축을 지금처럼 유지할 때" }
    ],
    "selectedMissionRate": 18.000,
    "adjustTargetUtility": "ELECTRICITY"
  },

  "monthlyRates": [
    { "yearMonth": "2026-04", "rate": 13.000, "achieved": true },
    { "yearMonth": "2026-05", "rate": 12.000, "achieved": true },
    { "yearMonth": "2026-06", "rate": 10.000, "achieved": true },
    { "yearMonth": "2026-07", "rate": 1.039,  "achieved": false }
  ]
}
```

**계산 규칙** (계산식 시트 8·9·10)

```
monthlyRate    = (E_base,m − E_m) / E_base,m × 100
                 E_base,m = 직전 2년 같은 달 평균 사용량 × 계수  (record_source='ECO_BASELINE')
                 E_m      = 진단 탭 고지서 사용량 × 계수        (record_source='BILL')
cumulativeRate = (Σ E_base,m − Σ E_m) / Σ E_base,m × 100        # 등록된 달만
requiredRate   = (targetRate × 6 − Σ monthlyRate) / remainingMonths
recoveryBurden = requiredRate / targetRate
requiredUtilityCarbonSaving
               = 전체 기준 탄소량 × requiredRate / 100
                 − Σ(다른 요금 기준 탄소량 × 해당 요금 현재 감축률 / 100)
requiredByUtility
               = requiredUtilityCarbonSaving / 해당 요금 기준 탄소량 × 100
```

| 규칙 | 내용 |
|---|---|
| **음수 = 증가** | `rate: -2.000` 은 "2% 늘었어요" (B-4-07) |
| `remainingMonths = 0` | `requiredRate: null` — **0 나눗셈 금지** (B-4-08 완료 조건) |
| `achievable: false` | 필요 감축률이 미션 합계 상한을 크게 넘음 → FE는 확정 표현("하면 돼요") 대신 가능성 문구 사용 (B-4-08 · 비즈니스 규칙 8) |
| `expanded` | 미달 항목은 펼치고 달성 항목은 접음 (B-4-07 ②) |
| `carbonSharePercent` | `baselineUsage × factor` 비중. "우리 집 온실가스의 83%가 전기예요" |
| 미등록 요금 | `byUtility` 에서 제외 |
| 데이터 없음 | `404 DIAGNOSIS_MONTH_EMPTY` 대신 `200` + `result: null` + `emptyReason: "NO_BILL"` |

---

## 10.4 실천 다시 고르기 (추천)

`GET /eco/rounds/{roundId}/mission-adjust` · **P1** · B-4-09 · WF-08

| 쿼리 | 필수 | 예 |
|---|---|---|
| `utility` | ✔ | `ELECTRICITY` |
| `month` | | 기준 리포트 월. 기본 최신 |

**Response 200**

아래 응답은 10.3의 정상 회복 예시와 다른 **독립된 하향 제안 시나리오**입니다. 남은 기간의 전기 필요 감축률이 33%까지 높아진 별도 회차·월을 가정합니다.

```json
{
  "roundId": 8,
  "utilityType": "ELECTRICITY",
  "reportMonth": "2026-08",
  "requiredRate": 33.000,
  "requiredAssumption": "도시가스 16%, 수도 11% 감축을 지금처럼 유지할 때예요",
  "carbonSharePercent": 83.0,

  "comparison": { "selectedExpectedRate": 18.000, "actualRate": -1.887 },

  "currentSelectedCount": 2,
  "missions": [
    { "missionId": 13, "title": "에어컨 하루 1시간 줄이기", "computedRate": 18.000, "difficulty": "NORMAL",
      "deviceGroup": "냉방", "evidenceText": "월 40kWh · 4,880원",
      "calculationBasis": "15평형 2kW를 20일 기준 · 40kWh ÷ 우리 집 223kWh", "sourceOrg": "한국에너지공단",
      "selected": true, "recommended": false, "capped": false },
    { "missionId": 14, "title": "에어컨 하루 2시간 줄이기", "computedRate": 30.000, "difficulty": "HARD",
      "deviceGroup": "냉방", "evidenceText": "월 80kWh · 1시간 실천의 2배",
      "calculationBasis": "공식 1시간 절감량 40kWh를 동일 조건에서 2시간으로 선형 환산", "sourceOrg": "한국에너지공단",
      "selected": false, "recommended": true, "capped": true },
    { "missionId": 16, "title": "안 쓰는 플러그 뽑기", "computedRate": 5.000, "difficulty": "EASY",
      "deviceGroup": "대기전력", "evidenceText": "가정 전력의 10% 이상",
      "calculationBasis": "대기전력이 가정·상업 전력사용량의 10%가 넘음 · 절반을 줄인다고 보수 적용", "sourceOrg": "한국에너지공단",
      "selected": false, "recommended": true, "capped": false }
  ],

  "preview": { "currentRate": 18.000, "withRecommendedRate": 35.000, "coversRequired": true },

  "tierDowngrade": { "suggest": true, "consecutiveMisses": 1,
    "message": "남은 기간에는 매달 33% 감축이 필요해 처음 목표보다 실천 부담이 커졌어요. 5~10% 구간으로 조정을 검토해 보세요" }
}
```

| 규칙 | 내용 |
|---|---|
| `recommended` | 이미 고른 `deviceGroup`에 더 높은 `computedRate` 미션이 있으면 **교체 추천을 우선**하고, 그래도 부족하면 겹치지 않는 다른 `deviceGroup` 미션을 추가 추천 (B-4-09) |
| `preview.withRecommendedRate` | 교체 추천은 기존 그룹 최댓값과의 **증가분만**, 새 그룹 추천은 전체 `computedRate`를 반영한 미션 합계 |
| 회복 부담 배수 | `requiredRate ÷ 현재 목표 구간 하한`. 낮출 구간이 있고 **1.5 이상이면** `tierDowngrade.suggest: true` (C-20) |
| 추천 미션 부족 | `preview.coversRequired: false`이고 낮출 구간이 있으면 미달 횟수와 관계없이 `tierDowngrade.suggest: true` (C-20) |
| `consecutiveMisses` | 선택 요금 단독 실적이 아닌 **회차 전체 합산 월 감축률**의 연속 미달 횟수. 응답 설명용이며 하향 여부를 직접 결정하지 않음 |
| 최저 구간 | 현재 목표가 `TIER_5`이면 더 낮은 지급 구간을 만들지 않고 `suggest: false` |
| 자동 변경 | 없음. **앱은 제안만 하고 사용자가 저장해야 바뀝니다** |

## 10.5 선택 미션 갱신

`PUT /eco/rounds/{roundId}/missions` · **P1** · B-4-09 · WF-08 → WF-06

```json
{ "selectedMissionIds": [13, 15, 16, 31, 44] }
```

**Response 200**

```json
{
  "roundId": 7,
  "combinedMissionRate": 28.000,
  "items": [
    { "missionId": 13, "computedRate": 18.000, "counted": true,  "exclusionReason": null },
    { "missionId": 15, "computedRate": 5.000,  "counted": false, "exclusionReason": "냉방 겹침 · 합계 제외" },
    { "missionId": 16, "computedRate": 5.000,  "counted": true,  "exclusionReason": null }
  ],
  "todayMissionsUpdated": true
}
```

목표 구간(`target_tier`)은 건드리지 않습니다. 미션만 교체합니다.

---

# 11. 평가 결과·마일리지 API (B-5)

## 11.1 평가 결과 상세

`GET /eco/rounds/{roundId}/result` · **P0** · B-5-02 · WF-10

**Response 200**

```json
{
  "roundId": 7,
  "periodStart": "2026-04", "periodEnd": "2026-09",
  "confirmedAt": "2026-12-05T00:00:00+09:00",
  "confirmedSource": "에코마일리지 누리집 기준",

  "finalRate": 12.499,
  "targetRate": 10.000,
  "achieved": true,
  "tier": "TIER_10",
  "tierLabel": "10~15% 구간",
  "confirmedMileage": 30000,

  "amount": { "baselineTotal": 420600, "actualTotal": 370100, "savedAmount": 50500, "savedIsPocketEligible": false },

  "utilityResults": [
    { "utilityType": "ELECTRICITY", "baselineUsage": 1340.000, "actualUsage": 1166.000, "usageUnit": "kWh", "finalRate": 13.000, "targetRate": 10.000, "achieved": true },
    { "utilityType": "GAS",         "baselineUsage": 108.000,  "actualUsage": 95.000,   "usageUnit": "m3",  "finalRate": 12.000, "targetRate": 15.000, "achieved": false },
    { "utilityType": "WATER",       "baselineUsage": 66.000,   "actualUsage": 62.700,   "usageUnit": "m3",  "finalRate": 5.000,  "targetRate": 5.000,  "achieved": true }
  ],

  "monthlyRates": [
    { "yearMonth": "2026-04", "rate": 8.000,  "achieved": false },
    { "yearMonth": "2026-05", "rate": 9.000,  "achieved": false },
    { "yearMonth": "2026-06", "rate": 11.000, "achieved": true },
    { "yearMonth": "2026-07", "rate": 12.000, "achieved": true },
    { "yearMonth": "2026-08", "rate": 12.000, "achieved": true },
    { "yearMonth": "2026-09", "rate": 17.000, "achieved": true }
  ],

  "mileageConverted": false,
  "nextRound": { "roundId": 8, "periodStart": "2026-10", "periodEnd": "2027-03", "goalSet": false }
}
```

| 규칙 | 내용 |
|---|---|
| `savedIsPocketEligible: false` | **"덜 낸 요금"은 성과 표시 전용, 포켓 적립 대상 아님** (비즈니스 규칙 3) |
| `achieved: false` | 빨간 X를 쓰지 않고 "목표 15% 줄이기"로 표시. "못 미쳐도 줄인 만큼은 합산에 들어가요" (B-5-02) |
| `mileageConverted` | `pocket_transaction(source_type='ECO_ROUND', source_key=roundId)` 존재 여부 |

**Errors** `ECO_RESULT_NOT_CONFIRMED(409)`

---

## 11.2 마일리지 적립 확정 화면

`GET /eco/rounds/{roundId}/settlement` · **P0** · B-5-03 · WF-11

```json
{
  "roundId": 7,
  "periodStart": "2026-04", "periodEnd": "2026-09",
  "confirmedMileage": 30000,
  "statusLabel": "확인",
  "cumulativeRate": 12.499,
  "tier": "TIER_10",
  "calculation": { "baselineAmount": 420600, "actualAmount": 370100, "savedAmount": 50500,
                   "note": "전기·도시가스·수도를 직전 2년 같은 기간(4~9월) 평균과 비교했어요" },
  "isCash": false,
  "convertible": true,
  "externalUrl": "https://ecomileage.seoul.go.kr",
  "otherUses": ["서울시 세금", "상품권", "관리비 납부"]
}
```

`isCash: false` — **아직 현금이 아닙니다.** 현금 전환은 13.5(`POST /pocket/conversions`)로 이어집니다.
"나중에 할래요"를 골라도 포켓 탭에서 전환할 수 있어야 합니다(B-5-03 완료 조건).

## 11.3 참여신청 (모의)

`POST /eco/rounds/{roundId}/application` · **P1** · B-4-05 · WF-06

외부 누리집 이동 → 복귀 시 호출. `application_status` 를 `APPLIED` 로 모의 전환합니다.

**Response 200**

```json
{ "roundId": 7, "applicationStatus": "APPLIED", "appliedAt": "2026-09-03T18:45:00+09:00", "showBanner": false }
```

---

# 12. 혜택 API (C) — 탄소중립포인트 녹색생활실천

## 12.1 참여·연동 상태 + 월 현황

`GET /greenlife/status` · **P0** · COM-12 · C-1-01 · C-1-02 · C-2-01 · C-2-02 · BN-01 · BN-02

미참여/참여 두 화면을 이 하나로 분기합니다.

| 쿼리 | 기본 | 설명 |
|---|---|---|
| `month` | 이번 달 | `YYYY-MM` |

**Response 200 — 미참여** (BN-01)

```json
{
  "participating": false,
  "screen": "BN-01",
  "linkedAt": null,
  "programInfo": {
    "name": "탄소중립포인트 녹색생활실천",
    "itemCount": 17,
    "annualLimit": 70000,
    "standardYear": 2026,
    "joinSteps": [
      "공식 누리집에서 회원가입해요",
      "참여기업 앱에서 실천 항목을 설정해요",
      "친환경 활동을 하면 포인트가 쌓여요"
    ],
    "externalUrl": "https://cpoint.or.kr"
  },
  "featuredItems": [
    { "itemId": 1, "name": "전자영수증", "unitPrice": 10, "rewardUnit": "건", "iconKey": "receipt" }
  ]
}
```

**Response 200 — 참여 중** (BN-02)

```json
{
  "participating": true,
  "screen": "BN-02",
  "linkedAt": "2026-09-01T09:12:00+09:00",
  "month": "2026-08",
  "monthSummary": { "activityCount": 44, "pendingAmount": 5540, "paidAmount": 3140, "paidMonth": "2026-07" },
  "annual": { "year": 2026, "paidAmount": 18600, "limitAmount": 70000, "progressPercent": 26.6, "limitReached": false },
  "delayNotice": "실적 반영까지 최소 3일~익월 말이 걸릴 수 있어요",
  "standardYear": 2026
}
```

| 규칙 | 내용 |
|---|---|
| `pendingAmount` | `reward_status='PENDING'` 합. **잔액에 반영하지 않음** (C-2-05) |
| `paidAmount` | `reward_status='PAID'` 합. 포켓 입금 대상 |
| `annual.paidAmount` | 저장하지 않고 올해 `PAID` 합계로 계산 (C-2-02) |
| `progressPercent` | 한도 도달 시 `100.0` 고정 |

---

## 12.2 연동 새로고침 (모의)

`POST /greenlife/link` · **P0** · C-1-02 · COM-05 · BN-01 → BN-02

외부 누리집 복귀 후 또는 새로고침 버튼에서 호출. 시드 실적을 적용하고 `greenlife_linked_at` 을 갱신합니다.

**Response 200**

```json
{ "participating": true, "linkedAt": "2026-09-03T18:50:00+09:00", "syncedActivityCount": 44, "screen": "BN-02" }
```

여전히 미참여면 `participating: false` + 안내 (C-1-01 예외 처리). 오류가 아니라 정상 응답입니다.

---

## 12.3 실천 항목 목록 (17개)

`GET /greenlife/items` · **P0** · C-1-03 · C-2-03 · BN-02

| 쿼리 | 기본 | 설명 |
|---|---|---|
| `month` | 이번 달 | 건수 집계 기준 월 |

**Response 200**

```json
{
  "month": "2026-08",
  "standardYear": 2026,
  "items": [
    { "itemId": 1, "itemCode": "E_RECEIPT", "name": "전자영수증", "unitPrice": 10, "rewardUnit": "건",
      "iconKey": "receipt", "displayOrder": 1,
      "monthCount": 24, "monthAmount": 240,
      "monthlyCapAmount": null, "annualCapAmount": null, "capReached": false },
    { "itemId": 2, "itemCode": "TUMBLER", "name": "텀블러·다회용컵", "unitPrice": 300, "rewardUnit": "개",
      "iconKey": "tumbler", "displayOrder": 2,
      "monthCount": 8, "monthAmount": 2400,
      "monthlyCapAmount": null, "annualCapAmount": null, "capReached": false },
    { "itemId": 7, "itemCode": "ECO_PRODUCT", "name": "친환경제품 구매", "unitPrice": 500, "rewardUnit": "건",
      "iconKey": "eco", "displayOrder": 7,
      "monthCount": 0, "monthAmount": 0,
      "monthlyCapAmount": null, "annualCapAmount": null, "capReached": false }
  ],
  "totalCount": 17,
  "collapsedAfter": 6
}
```

- **실적이 없어도 17개 전부 내려줍니다.** `monthCount: 0` → "아직 실천하지 않았어요" (C-2-03).
- 정렬은 `display_order` 고정.
- `collapsedAfter` — 6개 이후는 "17개 전체 보기"로 접는 FE 힌트.
- `monthlyCapAmount` · `annualCapAmount` 가 `null` 이면 **상한 값이 아직 확정되지 않은 항목**이고, FE는 상한을 표시하지 않습니다(결정 10 · 확인 필요 6번, 담당 아영).

---

## 12.4 실천항목 상세

`GET /greenlife/items/{itemId}` · **P1** · C-2-04 · BN-03

| 쿼리 | 기본 |
|---|---|
| `month` | 이번 달 |

**Response 200**

```json
{
  "itemId": 1, "itemCode": "E_RECEIPT", "name": "전자영수증",
  "unitPrice": 10, "rewardUnit": "건", "standardYear": 2026,
  "practiceSteps": [
    "매장에서 종이 대신 전자영수증을 선택해요",
    "카드사·매장 앱에서 전자영수증 발급을 켜 두면 자동으로 쌓여요",
    "누리집에 결제 카드를 등록하면 건수가 자동 집계돼요"
  ],
  "month": "2026-08",
  "validCount": 24,
  "pendingAmount": 240,
  "monthlyCapAmount": null,
  "capReached": false,
  "history": [
    { "activityId": 301, "occurredAt": "2026-08-28T13:20:00+09:00", "quantity": 1.000, "rewardAmount": 10, "rewardStatus": "PENDING" },
    { "activityId": 288, "occurredAt": "2026-07-30T09:05:00+09:00", "quantity": 1.000, "rewardAmount": 10, "rewardStatus": "PAID", "paidAt": "2026-08-10T00:00:00+09:00" }
  ],
  "externalUrl": "https://cpoint.or.kr",
  "syncedAt": "2026-09-01T09:12:00+09:00",
  "delayNotice": "실적 반영까지 최소 3일~익월 말이 걸릴 수 있어요"
}
```

`pendingAmount = validCount × unitPrice`, 항목 상한 적용 (계산식 13). 상세 건수는 목록 건수와 일치해야 합니다(C-2-04 완료 조건).

---

## 12.5 월 지급분 포켓 정산 (시스템)

`POST /greenlife/settlements` · **P0** · C-2-05 · C-2-06 · BN-02 → PK-05

월 단위 `PAID` 합계를 그린포켓 입금 거래로 만듭니다. 배치 또는 연동 직후 서버가 호출합니다.

**Request**

```json
{ "yearMonth": "2026-08" }
```

**Response 200**

```json
{
  "yearMonth": "2026-08",
  "paidTotal": 5540,
  "created": true,
  "transaction": {
    "transactionId": 88, "transactionCode": "GP-2609-0012",
    "direction": "CREDIT", "transactionType": "GREENLIFE",
    "amount": 5540, "transactionStatus": "COMPLETED",
    "label": "녹색생활실천 8월분", "completedAt": "2026-09-10T00:00:00+09:00"
  }
}
```

| 규칙 | 내용 |
|---|---|
| 중복 | `UNIQUE(user_id, source_type='GREENLIFE_MONTH', source_key='2026-08')`. 같은 사용자의 재호출 시 `created: false` + **기존 거래 그대로 반환** (200) |
| 대상 | `reward_status = 'PAID'` 만. `PENDING` 은 절대 포함하지 않음 (C-2-05 · 비즈니스 규칙 3) |
| 상태 전이 | `PENDING → PAID` **단방향.** 역전이 요청은 거부 |

---

# 13. 포켓 API (D)

## 13.1 포켓 메인

`GET /pocket` · **P0** · D-1-01 · D-1-02 · D-1-04 · D-1-06 · D-2-01 · PK-01 · PK-02

**Response 200**

```json
{
  "pocket": { "accountNo": "1005-1234-5678-90", "holder": "김수현" },
  "balance": 64000,
  "breakdown": { "ecoMileage": 40000, "greenlife": 24000 },
  "convertibleMileage": 30000,
  "convertibleSource": { "roundId": 7, "periodStart": "2026-04", "periodEnd": "2026-09" },
  "defaultAccount": {
    "accountId": 3, "bankCode": "088", "bankName": "신한은행",
    "accountNo": "110-123-456789", "holder": "김수현", "isDefault": true
  },
  "recentTransactions": [
    { "transactionId": 94, "transactionCode": "GP-2610-0006", "label": "그린포켓 출금",
      "direction": "DEBIT", "transactionType": "WITHDRAWAL", "amount": 10000,
      "transactionStatus": "COMPLETED", "completedAt": "2026-10-12T14:30:00+09:00" },
    { "transactionId": 91, "transactionCode": "GP-2610-0003", "label": "녹색생활실천 9월분",
      "direction": "CREDIT", "transactionType": "GREENLIFE", "amount": 3200,
      "transactionStatus": "COMPLETED", "completedAt": "2026-10-10T00:00:00+09:00" },
    { "transactionId": 88, "transactionCode": "GP-2609-0012", "label": "에코마일리지 2026 상반기",
      "direction": "CREDIT", "transactionType": "ECO_MILEAGE", "amount": 30000,
      "transactionStatus": "COMPLETED", "completedAt": "2026-09-06T21:24:00+09:00" },
    { "transactionId": 87, "transactionCode": "GP-2609-0011", "label": "그린포켓 출금",
      "direction": "DEBIT", "transactionType": "WITHDRAWAL", "amount": 12400,
      "transactionStatus": "COMPLETED", "completedAt": "2026-09-05T14:22:00+09:00" }
  ],
  "empty": { "noAccount": false, "noTransaction": false },
  "notices": [
    "마일리지 전환은 1일 1회만 가능해요",
    "전환 후 취소는 불가능하니 신중히 확인해 주세요",
    "실패한 거래는 잔액에 반영되지 않아요. 같은 수령 건은 한 번만 적립돼요"
  ]
}
```

| 규칙 | 내용 |
|---|---|
| `balance` | `SUM(CREDIT) − SUM(DEBIT)` **`COMPLETED` 만** (D-1-02) |
| 잔액 제외 | 예상 마일리지 · 적립 예정 포인트 · 미전환 마일리지 · "덜 낸 요금" (비즈니스 규칙 3) |
| `accountNo` | **전체 표시, 마스킹 없음** (결정 A-2). 단 서버 로그에는 남기지 않음 |
| `recentTransactions` | 방향·유형과 관계없이 최신 거래 4건. `COALESCE(completedAt, requestedAt)` 내림차순 (D-1-04·결정 C-16) |
| `empty.noAccount` | 계좌 미등록. **계좌가 없어도 적립 내역은 조회 가능** (D-1-06) |

---

## 13.2 잔액 단건 조회

`GET /pocket/balance` · **P0** · D-1-02

```json
{ "balance": 64000, "convertibleMileage": 30000, "calculatedAt": "2026-09-03T18:55:00+09:00" }
```

캐시 컬럼을 두지 않고 **원장에서 매번 계산**합니다(DB 설계서 1절 "원장이 진실").

## 13.3 전환 가능 마일리지

`GET /pocket/convertible-mileage` · **P0** · D-2-01 · PK-02

```json
{
  "convertibleMileage": 30000,
  "rounds": [ { "roundId": 7, "periodStart": "2026-04", "periodEnd": "2026-09", "confirmedMileage": 30000 } ],
  "convertible": true,
  "blockReason": null
}
```

= **확정(`CONFIRMED`)됐지만 `ECO_ROUND` 전환 거래가 없는 회차의 합.** 0이면 `convertible: false` → 버튼 비활성.

`blockReason`: `NO_MILEAGE` · `DAILY_LIMIT`(오늘 이미 전환) · `null`

---

## 13.4 거래 내역 전체

`GET /pocket/transactions` · **P0** · D-1-05 · PK-05

| 쿼리 | 값 |
|---|---|
| `direction` | `CREDIT`\|`DEBIT` (없으면 전체). 화면 탭은 전체=생략, 적립=`CREDIT`, 출금=`DEBIT` |
| `type` | `ECO_MILEAGE`\|`GREENLIFE`\|`WITHDRAWAL` |
| `page` · `size` | |

**Response 200**

```json
{
  "totalCreditAmount": 64000,
  "balance": 64000,
  "convertibleMileage": 30000,
  "groups": [
    {
      "yearMonth": "2026-10", "subtotal": 3200,
      "items": [
        { "transactionId": 91, "transactionCode": "GP-2610-0003", "label": "녹색생활실천 9월분",
          "direction": "CREDIT", "transactionType": "GREENLIFE", "amount": 3200,
          "transactionStatus": "COMPLETED", "completedAt": "2026-10-10T00:00:00+09:00", "sourceLabel": "자동 입금" }
      ]
    },
    {
      "yearMonth": "2026-04", "subtotal": 34600,
      "items": [
        { "transactionId": 60, "transactionCode": "GP-2604-0001", "label": "에코마일리지 2025 하반기",
          "direction": "CREDIT", "transactionType": "ECO_MILEAGE", "amount": 30000,
          "transactionStatus": "COMPLETED", "completedAt": "2026-04-15T00:00:00+09:00", "sourceLabel": "전환 신청 후 입금" }
      ]
    }
  ],
  "page": 0, "size": 20, "totalElements": 6, "totalPages": 1, "hasNext": false
}
```

월별 그룹 최신순입니다. `subtotal`은 해당 그룹의 완료 거래를 기준으로 입금은 양수, 출금은 음수로 합산하며 완료되지 않은 거래는 0으로 계산합니다. `totalCreditAmount`는 필터와 관계없이 전체 완료 입금 합계입니다(D-1-05·결정 C-16).

---

## 13.5 마일리지 현금 전환 시작

`POST /pocket/conversions` · **P0** · D-2-02 · COM-05 · WF-11 · PK-02

**Request**

```json
{ "roundId": 7, "agreed": true }
```

| 필드 | 규칙 |
|---|---|
| `agreed` | **명시적 동의 없이 전환하지 않습니다** (비즈니스 규칙 4). `false`면 400 |

**Response 201**

```json
{
  "conversionId": 120,
  "roundId": 7,
  "amount": 30000,
  "transactionStatus": "REQUESTED",
  "externalUrl": "https://ecomileage.seoul.go.kr/goods/apply.do",
  "requestedAt": "2026-09-03T18:58:00+09:00",
  "notice": "현금으로 바꿔야 그린포켓 계좌로 들어와요"
}
```

`pocket_transaction` 에 `REQUESTED` 거래를 만들고 **외부 누리집 URL을 돌려줍니다.** 이 상태는 잔액에 반영되지 않습니다.

**Errors** `CONVERSION_NOT_AVAILABLE(409)` · `CONVERSION_ALREADY_DONE(409)` · `CONVERSION_DAILY_LIMIT(429)`

## 13.6 전환 완료 처리 (복귀)

`POST /pocket/conversions/{conversionId}/complete` · **P0** · D-2-02 · D-2-03 · 결정 A-1

외부 누리집에서 **복귀했을 때** 호출. 참여신청 배너와 같은 패턴으로 자동 "전환 완료" 처리합니다.

**Headers** `Idempotency-Key: <UUID>` (필수)

**Response 200**

```json
{
  "conversionId": 120,
  "transactionStatus": "COMPLETED",
  "amount": 30000,
  "completedAt": "2026-09-03T19:01:00+09:00",
  "balanceAfter": 94000,
  "transaction": { "transactionId": 120, "transactionCode": "GP-2609-0021", "label": "에코마일리지 2026 상반기" }
}
```

| 규칙 | 내용 |
|---|---|
| 외부 이동 필수 | 13.5로 만든 `REQUESTED` 거래가 없으면 `409 CONVERSION_NOT_RETURNED`. **외부 이동 없이 거래가 생기지 않습니다** (D-2-02 완료 조건) |
| 회차당 1회 | `UNIQUE(user_id, source_type='ECO_ROUND', source_key=roundId)` |
| 1일 1회 | `requested_at` 오늘 날짜 조회로 앱 로직 차단 (DB 제약 아님) |
| 실패 | `transactionStatus: "FAILED"` + **잔액 변경 없음** + 재시도 가능 (D-2-03) |
| 재요청 | 같은 `Idempotency-Key` → 기존 거래 200 반환 |

---

## 13.7 출금 계좌 목록

`GET /pocket/accounts` · **P0** · D-3-01 · PK-06 · PK-07

```json
{
  "accounts": [
    { "accountId": 3, "bankCode": "088", "bankName": "신한은행", "accountNo": "110-123-456789",
      "holder": "김수현", "isDefault": true, "isActive": true, "verifiedAt": null }
  ]
}
```

`accountNo` 는 `account_no_encrypted` 를 복호화한 **평문 전체**입니다(결정 A-2). `verifiedAt` 은 MVP에서 항상 `null`(결정 A-6).

## 13.8 출금 계좌 등록

`POST /pocket/accounts` · **P0** · D-3-01 · PK-07

```json
{ "bankCode": "088", "bankName": "신한은행", "accountNo": "110-123-456789", "holder": "김수현", "isDefault": true }
```

**Response 201** — 13.7의 계좌 객체

실계좌 검증·본인 인증은 하지 않습니다(결정 A-6). 계좌번호 원문을 로그에 남기지 않습니다(COM-11).

## 13.9 출금 계좌 수정 / 기본 지정 / 삭제

| 메서드 | 경로 | 우선순위 | 설명 |
|---|---|---|---|
| `PUT` | `/pocket/accounts/{accountId}` | P0 | 은행·계좌번호·예금주 변경 |
| `PUT` | `/pocket/accounts/{accountId}/default` | P0 | 기본 계좌 지정. **사용자당 1건만** — 앱이 `default_slot`을 기본 계좌면 `user_id`, 아니면 `null`로 동기화하고 UNIQUE가 중복을 차단 |
| `DELETE` | `/pocket/accounts/{accountId}` | P1 | `is_active = 0` 소프트 삭제. 거래 이력이 계좌를 참조하므로 물리 삭제하지 않음 |

기본 계좌 지정 응답:

```json
{ "accountId": 3, "isDefault": true, "previousDefaultAccountId": 2 }
```

---

## 13.10 출금 신청

`POST /pocket/withdrawals` · **P0** · D-3-02 · D-3-03 · PK-03 → PK-04

**Headers** `Idempotency-Key: <UUID>` (필수)

**Request**

```json
{ "amount": 30000, "accountId": 3 }
```

| 검증 | 규칙 | 에러 |
|---|---|---|
| 금액 | 정수, `0 < amount ≤ balance` | `POCKET_AMOUNT_INVALID` · `POCKET_INSUFFICIENT_BALANCE` |
| 계좌 | 등록된 활성 계좌 | `POCKET_ACCOUNT_REQUIRED` · `POCKET_ACCOUNT_NOT_FOUND` |

**Response 201**

```json
{
  "transactionId": 130,
  "transactionCode": "GP-2609-0025",
  "direction": "DEBIT",
  "transactionType": "WITHDRAWAL",
  "amount": 30000,
  "transactionStatus": "COMPLETED",
  "requestedAt": "2026-09-03T19:10:00+09:00",
  "expectedDate": "2026-09-05",
  "balanceAfter": 34000,
  "accountSnapshot": { "bankName": "신한은행", "accountNo": "110-123-456789", "holder": "김수현" },
  "notice": "영업일 기준 1~2일 내에 입금될 예정이에요"
}
```

| 규칙 | 내용 |
|---|---|
| 멱등 | `UNIQUE(idempotency_key)`. 중복 탭이면 **1건만 생기고 같은 응답** (D-3-03) |
| `accountSnapshot` | 출금 당시 계좌 정보를 JSON으로 박제. 계좌가 나중에 바뀌어도 내역이 안 깨짐 |
| `expectedDate` | 신청일 + 영업일 1~2일 |
| 실패 | `transactionStatus: "FAILED"` 로 응답하고 **완료 화면(PK-04)을 띄우지 않습니다** (비즈니스 규칙 11) |
| 실제 이체 | 없음. 모의 처리 |

## 13.11 출금 내역

`GET /pocket/withdrawals` · **P1** · D-3-04 · PK-08

```json
{
  "content": [
    { "transactionId": 130, "transactionCode": "GP-2609-0025", "amount": 30000,
      "transactionStatus": "COMPLETED", "requestedAt": "2026-09-03T19:10:00+09:00",
      "expectedDate": "2026-09-05", "completedAt": "2026-09-05T10:00:00+09:00",
      "accountSnapshot": { "bankName": "신한은행", "accountNo": "110-123-456789", "holder": "김수현" },
      "failureReason": null, "retryable": false }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false
}
```

`FAILED` 는 `failureReason` + `retryable: true`, `PROCESSING` 은 취소 정책 안내를 FE가 붙입니다.

## 13.12 포켓 관리 화면

`GET /pocket/management` · **P1** · D-3-05 · PK-06

13.1의 `pocket` + 13.7의 `accounts` + 최근 출금 3건을 한 번에 묶은 조회용 API입니다.

```json
{
  "pocket": { "accountNo": "1005-1234-5678-90", "holder": "김수현", "balance": 64000 },
  "accounts": [ { "accountId": 3, "bankName": "신한은행", "accountNo": "110-123-456789", "isDefault": true } ],
  "recentWithdrawals": [ { "transactionId": 130, "amount": 30000, "transactionStatus": "COMPLETED", "requestedAt": "2026-09-03T19:10:00+09:00" } ]
}
```

> **포켓 이름은 "그린포켓"으로 고정**이고 변경 기능은 넣지 않습니다(결정 2). PK-06 시안의 이름 수정 UI는 빼주세요.

## 13.13 KB 금융상품 추천

`GET /pocket/recommended-product` · **P1** · D-4-01 · PK-01 · PK-02 → PK-09

포켓 홈 추천 배너와 상품 상세 화면에서 공통으로 사용하는 실제 판매 상품 정보를 조회합니다. 그린포켓은 금융상품 가입·저축·계좌 연결을 처리하지 않고 외부 상품 페이지 URL만 제공합니다.

**Response 200**

```json
{
  "productCode": "DP01000942",
  "name": "KB맑은하늘적금",
  "tagline": "맑은하늘 만들고 금리도 Up",
  "recommendation": {
    "badge": "그린포켓 추천",
    "title": "친환경 실천과 가장 잘 어울리는 적금",
    "description": "맑은하늘을 위한 생활 속 작은 실천에 우대금리를 제공해요."
  },
  "productType": "자유적립식",
  "monthlyDeposit": { "minimumAmount": 10000, "maximumAmount": 1000000 },
  "contractTermsMonths": [12, 24, 36],
  "preferentialMissions": ["종이통장 줄이기", "비대면 가입", "대중교통 이용", "미세먼지 퀴즈"],
  "informationBaseDate": "2026-08-26",
  "applicationUrl": "https://obank.kbstar.com/quics?cc=b061761:b061770&isNew=N&page=C020702&prcode=DP01000942",
  "notice": "금리와 우대 조건은 가입 시점에 KB국민은행에서 확인해 주세요."
}
```

| 규칙 | 내용 |
|---|---|
| 추천 범위 | 실제 판매 중인 KB맑은하늘적금 1개를 고정 추천. 개인별 금융상품 적합성 판단은 하지 않음 |
| 신청 동작 | `applicationUrl`로 외부 이동. 그린포켓 내부에서 가입 신청·자동저축·이체를 실행하지 않음 |
| 우대 조건 | `preferentialMissions`는 기존 KB 상품 조건 안내이며 그린포켓 미션 달성을 우대 조건으로 표현하지 않음 |
| 정보 기준일 | 상품 조건은 바뀔 수 있으므로 `informationBaseDate`와 `notice`를 함께 표시 |

**Errors** `UNAUTHENTICATED(401)` · 개발·시연 프로필의 Demo Key 오류는 `UNAUTHENTICATED_DEMO_KEY(401)`

---

# 14. 마이·보관함·청년정책 API (E)

## 14.1 마이 메인

`GET /mypage` · **P0** · E-1-01 · E-1-02 · MY-01

```json
{
  "profile": {
    "name": "김수현",
    "birthDate": "1998-03-15",
    "gender": "FEMALE",
    "phoneNumber": "01091740339"
  },
  "links": {
    "billArchive": { "count": 14, "screen": "MY-03" },
    "reportArchive": { "count": 9, "screen": "MY-04" }
  },
  "ecoAddress": {
    "label": "서울 관악구",
    "registeredAt": "2026-03",
    "notice": "주소를 바꾸려면 에코마일리지 누리집에서 변경한 뒤 다시 연동해 주세요"
  },
  "integration": {
    "ecoLinkStatus": "LINKED", "ecoLinkedAt": "2026-09-01T09:00:00+09:00",
    "greenlifeParticipating": true, "greenlifeLinkedAt": "2026-09-01T09:12:00+09:00",
    "registeredUtilities": ["ELECTRICITY","GAS","WATER"]
  },
  "pocketAccountNo": "1005-1234-5678-90",
  "youthPolicy": {
    "profileCompleted": true,
    "regionLinked": true,
    "recommendedCount": 12,
    "preview": [
      { "policyId": "20260908005400213380", "title": "청년 지원 정책", "category": "EDUCATION", "matchStatus": "CHECK_REQUIRED" }
    ],
    "lastSyncedAt": "2026-09-08T19:30:00+09:00"
  }
}
```

| 필드 | 설명 |
|---|---|
| `ecoAddress` | 에코마일리지 누리집에 등록된 주소입니다. 연동 때 받아 `app_user.eco_*`에 저장하며 미연동이면 `null` |
| `youthPolicy.regionLinked` | `false`면 전국 정책만 추천하며 에코마일리지 연동 CTA를 표시 |
| `youthPolicy.preview` | 마이 메인에 노출할 추천 정책 상위 최대 5개. 전체 결과는 14.3 사용 |

지역을 프로필 입력값으로 이중 관리하지 않습니다. 앱 내부 지역 수정 UI도 제공하지 않습니다(결정 C-25).

---

## 14.2 리포트 보관함

`GET /reports` · **P1** · E-2-01 · MY-04

| 쿼리 | 값 | 설명 |
|---|---|---|
| `type` | `MONTHLY_DIAGNOSIS` \| `ECO_MONTHLY` \| `ECO_RESULT` | 탭 |
| `year` | `2026` | 연도 필터 |
| `page` · `size` | | |

**Response 200**

```json
{
  "content": [
    { "reportId": "MONTHLY_DIAGNOSIS:2026-08", "type": "MONTHLY_DIAGNOSIS", "yearMonth": "2026-08",
      "title": "8월 생활비 진단", "createdAt": "2026-09-01T10:22:00+09:00",
      "targetScreen": "AN-07", "targetParams": { "month": "2026-08" }, "downloadable": false },
    { "reportId": "ECO_MONTHLY:2026-07", "type": "ECO_MONTHLY", "yearMonth": "2026-07",
      "title": "7월분 전달 리포트", "createdAt": "2026-08-03T00:00:00+09:00",
      "targetScreen": "WF-07", "targetParams": { "month": "2026-07" }, "downloadable": false },
    { "reportId": "ECO_RESULT:6", "type": "ECO_RESULT", "yearMonth": "2026-09",
      "title": "2026-04 ~ 09 평가 결과", "createdAt": "2026-12-05T00:00:00+09:00",
      "targetScreen": "WF-10", "targetParams": { "roundId": 6 }, "downloadable": false }
  ],
  "page": 0, "size": 20, "totalElements": 9, "totalPages": 1, "hasNext": false
}
```

| 규칙 | 내용 |
|---|---|
| 저장 테이블 없음 | `utility_monthly_record` + `eco_monthly_report` + `eco_round` 를 **UNION으로 유도**합니다 (DB 설계서 6절) |
| `reportId` | `타입:키` 형태의 합성 ID. 실제 PK가 아님 |
| MVP 범위 | **목록 + 대상 화면 라우팅만.** `downloadable` 은 항상 `false` (E-2-02는 P2) |
| 미생성 월 | 목록에 넣지 않음 (E-2-01) |

---

## 14.3 맞춤 청년정책 추천

`GET /policies/recommendations` · **P0** · E-3-01 · MY-01 · MY-05

쿼리 파라미터는 없습니다. 저장된 추천 조건에서 탈락하지 않은 정책을 최대 5개 반환하며, `ELIGIBLE`을 우선하고 `CHECK_REQUIRED`는 확인 사유를 함께 제공합니다.

**Response 200**

```json
{
  "content": [
    {
      "policyId": "20260908005400213380",
      "title": "경기도 대학혁신플랫폼 지원",
      "category": "EDUCATION",
      "subCategory": "미래역량강화",
      "supportSummary": "수요 맞춤형 교육 및 현장실습 지원",
      "applicationStatus": "OPEN",
      "applicationEndDate": "2027-02-28",
      "matchStatus": "ELIGIBLE",
      "matchScore": 100,
      "matchReasons": ["전국 대상 정책이에요", "지원 연령에 해당해요", "관심 분야와 일치해요"],
      "regionScope": "NATIONAL"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 5,
  "totalPages": 1,
  "hasNext": false,
  "region": { "linked": true, "label": "서울특별시 관악구", "appliedLevels": ["NATIONAL", "SIDO", "SIGUNGU"] },
  "lastSyncedAt": "2026-09-08T19:30:00+09:00"
}
```

- 맞춤 추천에는 구조화된 나이·지역·취업·연소득·학력 조건에서 탈락하지 않은 정책이 들어갑니다. `ELIGIBLE`을 먼저 반환하고, 세부 조건이 남은 `CHECK_REQUIRED`는 명시적으로 구분합니다.
- 조건 코드가 비어 있으면 `제한 없음`으로 간주하지 않고 `CHECK_REQUIRED`로 판정하며, 핵심 조건(취업·소득·학력)이 누락된 정책은 맞춤 추천에서 제외합니다.
- 직접대출·대출보증·공적보험처럼 재직기간·사업기간·신용·자산·보증 심사 등을 앱 입력값만으로 확정할 수 없는 정책은 상세에서 `CHECK_REQUIRED`로 판정하고 맞춤 추천에서는 제외합니다.
- 건강보험료·도시근로자 소득 기준 또는 자유 텍스트 추가 자격처럼 앱 입력값만으로 탈락을 확정할 수 없는 정책은 `CHECK_REQUIRED`와 확인 사유를 표시해 반환할 수 있습니다. `NOT_ELIGIBLE` 정책은 제외합니다.
- 선택한 관심 분야에 속한 적격 정책만 반환합니다. `matchScore`는 정렬용 보조값이며 실제 자격을 보증하지 않습니다.
- 응답은 최대 5개이고 추가 추천 페이지를 제공하지 않습니다. `ELIGIBLE`이 5개 미만이면 핵심 조건이 존재하고 명확히 탈락하지 않은 `CHECK_REQUIRED`로 보완합니다. 전체 활성 정책 탐색은 14.4 API를 사용합니다.
- 에코 미연동이면 `region.linked=false`, `appliedLevels=["NATIONAL"]`로 전국 정책만 반환합니다.

**Errors** `UNAUTHENTICATED(401)` · `PROFILE_INCOMPLETE(409)` · `YOUTH_POLICY_DATA_UNAVAILABLE(503)`

---

## 14.4 전체 청년정책 목록

`GET /policies` · **P0** · E-3-02 · MY-05

| 쿼리 | 값 | 설명 |
|---|---|---|
| `keyword` | string | 정책명·지원 내용 검색 |
| `category` | `PolicyInterestCategory` | 분야 |
| `regionCode` | 행정구역 코드 | 전국 정책은 항상 포함하지 않고, 지정 지역만 필터링 |
| `page` · `size` | 0 · 20 | size 1~100. MY-05 화면은 페이지당 6개로 요청 |

승인 완료·현재 신청 가능·개인 대상 제공 방식·분류 가능·지역 정보 존재·실제 신청 경로 존재 조건을 통과하고 팀 검증 카탈로그에 포함된 정책 60건만 조회합니다. MY-05는 페이지 번호로 이동하며, `category`는 목록 검색에만 적용되고 저장된 `interestCategories`를 변경하지 않습니다. Response의 페이징 구조와 카드 항목은 14.3과 같습니다. 사용자 조건이 완성돼 있으면 `matchStatus`·`matchReasons`를 포함하고, 아니면 해당 필드는 `null`입니다.

**Errors** `UNAUTHENTICATED(401)` · `YOUTH_POLICY_DATA_UNAVAILABLE(503)`

---

## 14.5 청년정책 상세

`GET /policies/{policyId}` · **P0** · E-3-03 · MY-06

**Response 200**

```json
{
  "policyId": "20260908005400213380",
  "title": "경기도 대학혁신플랫폼 지원",
  "category": "EDUCATION",
  "subCategory": "미래역량강화",
  "description": "정책 설명",
  "supportContent": "지원 내용",
  "application": {
    "status": "OPEN",
    "periodType": "LIMITED",
    "startDate": "2026-01-01",
    "endDate": "2027-02-28",
    "method": "기관 문의 또는 홈페이지 신청",
    "url": "https://example.go.kr"
  },
  "organizations": { "supervising": "경기도", "operating": "가천대학교" },
  "conditions": {
    "age": "제한 없음",
    "income": "별도 확인",
    "employment": "재직자·미취업자",
    "education": "대학 재학",
    "major": "제한 없음",
    "special": "세부 공고 확인"
  },
  "match": {
    "status": "CHECK_REQUIRED",
    "score": 60,
    "reasons": ["전국 대상 정책이에요", "세부 학력 조건은 직접 확인해 주세요"]
  },
  "referenceUrls": ["https://example.go.kr"],
  "source": "온통청년",
  "lastSyncedAt": "2026-09-08T19:30:00+09:00"
}
```

- `application.periodType=ALWAYS`이면 시작·종료일이 `null`이어도 현재 신청 가능한 상시 정책이며 화면에는 `상시 신청`으로 표시합니다.
- 공통코드가 명시적으로 제한 없음인 대상 조건만 `제한 없음`으로 내려줍니다. 조건 코드 누락은 `세부 … 조건 확인`으로 내려줍니다.
- 구조화된 연소득 최솟값·최댓값이 있으면 `연소득 3,500만원 이하`, `연소득 2,400만~3,500만원`처럼 구체적으로 내려줍니다.
- 직접대출·대출보증·공적보험 정책은 `match.status=CHECK_REQUIRED`이며, 취업·학업·보증 심사 조건을 상세에서 확인하도록 안내합니다.

외부 URL은 `http`·`https`만 허용하며 그 외 스킴은 응답에서 제외합니다.

**Errors** `YOUTH_POLICY_NOT_FOUND(404)`

---

# 15. 매핑표

## 15.1 API 68개 한눈에 보기

P1만 표시하고 나머지는 P0입니다. 뒤 숫자는 이 문서의 절 번호. 표 형태 목록은 노션 「API 기본 명세서」 DB에도 있습니다.

| 영역 | 엔드포인트 |
|---|---|
| 공통·인증 (8) | `POST /users` 4.1 (dev/demo) · `GET /users/me` 4.2 · `GET /meta/regions` 4.3 · `POST /demo/reset` 4.4 (dev/demo) · `POST /auth/signup` 4.5 · `POST /auth/login` 4.6 · `POST /auth/refresh` 4.7 · `POST /auth/logout` 4.8 |
| 프로필 (3) | `GET /profile` 5.1 · `GET /profile/policy-preferences` 5.2 · `PUT /profile/policy-preferences` 5.3 |
| 고지서 (9) | `GET /bills/target-month` 6.1 · `POST /bills/ocr` 6.2 · `GET /bills/ocr/{jobId}` 6.3 · `GET /bills/duplicate-check` 6.4 · `POST /bills` 6.5 · `GET /bills` 6.6 (P1) · `GET /bills/{recordId}` 6.7 (P1) · `PUT /bills/{recordId}` 6.8 (P1) · `DELETE /bills/{recordId}` 6.9 (P1) |
| 진단 (3) | `GET /diagnosis/months` 7.1 (P1) · `GET /diagnosis` 7.2 · `GET /diagnosis/baseline` 7.3 |
| 에코 연동 (5) | `GET /eco/status` 8.1 · `POST /eco/link` 8.2 · `GET /eco/link/{linkJobId}` 8.3 · `GET /eco/rounds/current` 8.4 · `GET /eco/rounds` 8.5 (P1) |
| 목표·미션 (7) | `GET /eco/rounds/{id}/goal-form` 9.1 · `POST .../goal/preview` 9.2 · `POST .../goal` 9.3 · `PUT .../goal` 9.3 · `GET .../goal` 9.4 · `GET .../missions/today` 9.5 (P1) · `PUT .../mission-logs/{date}` 9.6 (P1) |
| 진행·리포트 (5) | `GET /eco/home` 10.1 · `POST .../result/view` 10.2 (P1) · `GET /eco/monthly-report` 10.3 · `GET .../mission-adjust` 10.4 (P1) · `PUT .../missions` 10.5 (P1) |
| 평가 결과 (3) | `GET .../result` 11.1 · `GET .../settlement` 11.2 · `POST .../application` 11.3 (P1) |
| 혜택 (5) | `GET /greenlife/status` 12.1 · `POST /greenlife/link` 12.2 · `GET /greenlife/items` 12.3 · `GET /greenlife/items/{itemId}` 12.4 (P1) · `POST /greenlife/settlements` 12.5 |
| 포켓 (15) | `GET /pocket` 13.1 · `GET /pocket/balance` 13.2 · `GET /pocket/convertible-mileage` 13.3 · `GET /pocket/transactions` 13.4 · `POST /pocket/conversions` 13.5 · `POST .../conversions/{id}/complete` 13.6 · `GET /pocket/accounts` 13.7 · `POST /pocket/accounts` 13.8 · `PUT /pocket/accounts/{id}` 13.9 · `PUT .../{id}/default` 13.9 · `DELETE .../{id}` 13.9 (P1) · `POST /pocket/withdrawals` 13.10 · `GET /pocket/withdrawals` 13.11 (P1) · `GET /pocket/management` 13.12 (P1) · `GET /pocket/recommended-product` 13.13 (P1) |
| 마이·청년정책 (5) | `GET /mypage` 14.1 · `GET /reports` 14.2 (P1) · `GET /policies/recommendations` 14.3 · `GET /policies` 14.4 · `GET /policies/{policyId}` 14.5 |

각 엔드포인트 절 제목에 담당 기능 ID가 붙어 있습니다. 기능 ID로 역추적할 때는 문서에서 `A-2-11` 처럼 검색하세요.

## 15.2 API가 없는 기능 (FE 단독 · 비개발)

P0·P1 기능 중 아래 항목은 화면 동작·데이터 작업으로 별도 API가 없습니다. 나머지는 위 68개 API로 덮습니다.

| 기능 ID | 내용 | 왜 API가 없나 |
|---|---|---|
| COM-04 | 공통 UI 규칙 | 비개발(디자인). `design-system.md` · `tokens.css` |
| COM-06 | 금액·증감 표기 | FE 공통 포맷터. 서버는 숫자·enum만 준다(1.4절) |
| COM-07 | 예상·적립·입금 상태 표시 | FE 라벨. 서버는 `RewardStatus`·`TxStatus`·`isCash` 로 구분값만 |
| COM-09 | 시드 데이터 적재 | 비개발(데이터). 적재 스크립트는 BE |
| COM-11 | 개인정보·데모 안내 | 비개발(콘텐츠). 서버는 이미지 미저장·계좌 로그 금지로 준수 |
| A-1-02 · A-1-03 | 별도 온보딩 | 결정 C-26으로 제거. 가입 후 바로 에코 연동으로 이동 |
| A-2-02 | 입력 방식 선택 | FE 세그먼트. 두 경로 모두 `POST /bills` 로 수렴 |
| A-2-06 | 인식 내용 수정 | FE 3탭 폼. 저장은 `POST /bills` |
| A-2-10 | 등록 전 요약·확정 | FE 화면. 확정 전에는 저장하지 않음 |
| A-3-01 | 1인 가구 평균 사용량 기준선 데이터 | 비개발(데이터). `resources/data/single-household-utility-baselines.json` |
| B-3-01 | 실천 미션 데이터 | 비개발(데이터). 근거 3종 NOT NULL 로 DB가 품질 강제 |

## 15.3 화면 → API

| 화면 ID | 화면명 | 진입 시 호출 |
|---|---|---|
| ONB-01 | 회원가입·로그인 | `POST /auth/signup` · `POST /auth/login` (`dev`·`demo`는 `POST /users` 사용 가능) |
| ONB-02 | 사용하지 않음 | 별도 온보딩 제거 |
| ONB-03 | 사용하지 않음 | 정책 추천 조건은 마이에서 선택 입력 |
| AN-01 | 고지서 미등록 메인 | `GET /diagnosis` (`empty:true`) · `GET /bills/target-month` |
| AN-02 | 사진·직접 입력 선택 | `GET /bills/target-month` |
| AN-03 | OCR 분석 중 | `POST /bills/ocr` → `GET /bills/ocr/{jobId}` 폴링 |
| AN-04 | OCR 결과 확인 | `GET /bills/ocr/{jobId}` |
| AN-05 | 인식 내용 수정·직접 입력 | `GET /bills/duplicate-check` |
| AN-06 | 생활요금 최종 확인 | `POST /bills` |
| AN-07 | 생활비 분석 메인 | `GET /diagnosis?month=` · `GET /diagnosis/months` |
| AN-08 | 고지서 상세·수정 | `GET /bills/{id}` → `PUT`/`DELETE /bills/{id}` |
| BN-01 | 녹색생활실천 미참여 | `GET /greenlife/status` → `POST /greenlife/link` |
| BN-02 | 녹색생활실천 참여 메인 | `GET /greenlife/status` · `GET /greenlife/items` |
| BN-03 | 실천항목 상세 | `GET /greenlife/items/{itemId}` |
| WF-01 | 에코마일리지 미연동 | `GET /eco/home` (`WF_01_UNLINKED`) · `GET /eco/status` |
| WF-02 | 사용량 불러오는 중 | `POST /eco/link` → `GET /eco/link/{jobId}` 폴링 |
| WF-03 | 목표 미설정 메인 | `GET /eco/home` (`WF_03_NO_GOAL`) · `GET /eco/rounds/current` |
| WF-04 | 평가 기간 목표 정하기 | `GET /eco/rounds/{id}/goal-form` → `POST .../goal/preview` → `POST .../goal` |
| WF-05 | 미등록 요금 목표 상태 | `GET .../goal-form` (`registered:false` 세그먼트) |
| WF-06 | 목표 설정 후 메인 (홈) | `GET /eco/home` · `GET .../missions/today` |
| WF-07 | 전달 리포트 상세 | `GET /eco/monthly-report?month=` |
| WF-08 | 실천 다시 고르기 | `GET .../mission-adjust?utility=` → `PUT .../missions` |
| WF-09 | 평가 종료 팝업 | `GET /eco/home` (`resultModal`) → `POST .../result/view` |
| WF-10 | 평가 결과 상세 | `GET /eco/rounds/{id}/result` |
| WF-11 | 마일리지 적립·현금 전환 | `GET .../settlement` → `POST /pocket/conversions` → `.../complete` |
| PK-01 | 계좌 미등록 메인 | `GET /pocket` (`empty.noAccount:true`) · `GET /pocket/recommended-product` |
| PK-02 | 계좌 등록 메인 | `GET /pocket` · `GET /pocket/convertible-mileage` · `GET /pocket/recommended-product` |
| PK-03 | 출금 신청 | `GET /pocket/accounts` → `POST /pocket/withdrawals` |
| PK-04 | 출금 완료 | `POST /pocket/withdrawals` 응답 |
| PK-05 | 적립 내역 | `GET /pocket/transactions` |
| PK-06 | 그린포켓 관리 | `GET /pocket/management` |
| PK-07 | 출금계좌 등록·변경 | `GET/POST/PUT /pocket/accounts` |
| PK-08 | 출금 내역 | `GET /pocket/withdrawals` |
| PK-09 | KB맑은하늘적금 상세 | `GET /pocket/recommended-product` → `applicationUrl` 외부 이동 |
| MY-01 | 마이 메인 | `GET /mypage` · `GET /policies/recommendations` |
| MY-02 | 정책 추천 조건 설정 | `GET/PUT /profile/policy-preferences` |
| MY-03 | 고지서 보관함 | `GET /bills?utility=&year=` |
| MY-04 | 리포트 보관함 | `GET /reports?type=&year=` |
| MY-05 | 청년정책 전체 목록 | `GET /policies?keyword=&category=&regionCode=&page=&size=` |
| MY-06 | 청년정책 상세 | `GET /policies/{policyId}` |

## 15.4 DB 테이블 → API

| 테이블 | 읽는 API | 쓰는 API |
|---|---|---|
| `app_user` | `GET /users/me` · `/profile*` · `/mypage` · `/policies*` · `/eco/status` · `/greenlife/status` · `/pocket` | `POST /auth/signup` · `POST /users`(dev/demo) · `PUT /profile/policy-preferences` · `POST /eco/link`(연동 상태·등록 주소) · `POST /greenlife/link` · `POST /demo/reset`(dev/demo) |
| `auth_account` | `POST /auth/login` | `POST /auth/signup` · `POST /auth/login`(last_login_at) |
| `auth_refresh_token` | `POST /auth/refresh` · `/auth/logout` | `POST /auth/signup` · `/auth/login` · `/auth/refresh` · `/auth/logout` |
| `utility_monthly_record` | `GET /diagnosis` · `/bills` · `/eco/monthly-report` · `/reports` | `POST/PUT/DELETE /bills` · `POST /eco/link`(ECO_BASELINE) |
| `region_utility_snapshot` | `GET /meta/regions`의 하위 호환 `hasRegionAverage` | 시드만 (COM-09). 결정 C-29 이후 진단 API에서는 사용하지 않음 |
| `eco_round` | `GET /eco/rounds*` · `/eco/home` · `/pocket/convertible-mileage` | `POST /eco/link` · `POST/PUT .../goal` · `POST .../application` · `POST .../result/view` |
| `eco_round_utility` | `GET /eco/rounds/current` · `.../goal*` · `.../result` | `POST /eco/link` · `POST/PUT .../goal` |
| `eco_monthly_report` | `GET /eco/monthly-report` · `/eco/home` · `/reports` | `POST/PUT/DELETE /bills` 재계산 |
| `mission_catalog` | `GET .../goal-form` · `.../missions/today` · `.../mission-adjust` | 시드만 (B-3-01) |
| `user_mission` | `GET .../goal` · `.../missions/today` · `.../mission-adjust` | `POST/PUT .../goal` · `PUT .../missions` |
| `mission_daily_log` | `GET .../missions/today` | `PUT .../mission-logs/{date}` |
| `greenlife_item` | `GET /greenlife/items*` · `/greenlife/status` | 시드만 (C-1-03) |
| `greenlife_activity` | `GET /greenlife/status` · `/greenlife/items*` | `POST /greenlife/link` · `POST /greenlife/settlements`(PAID 전이) |
| `withdrawal_account` | `GET /pocket*` | `POST/PUT/DELETE /pocket/accounts` |
| `pocket_transaction` | `GET /pocket*` · `/eco/rounds/{id}/result` | `POST /greenlife/settlements` · `POST /pocket/conversions*` · `POST /pocket/withdrawals` |
| `user_policy_interest` | 신규 API에서 사용하지 않는 레거시 테이블 | 신규 쓰기 없음 |
| `youth_policy` | `GET /mypage` · `/policies*` | 온통청년 동기화 배치 |
| `youth_policy_region` | `GET /policies*` | 온통청년 동기화 배치 |
| `youth_policy_condition` | `GET /policies*` | 온통청년 동기화 배치 |
| `youth_policy_sync` | `GET /mypage` · `/policies*`(lastSyncedAt) | 온통청년 동기화 배치 |

---

# 16. 2026-09-03 결정 기록

이전 버전에서 「DB ↔ 명세 불일치」로 열어 두었던 13건을 팀이 전부 확정했습니다. **이 문서와 `docs/database/schema.sql` 은 아래 결정이 이미 반영된 상태**입니다.

## 16.1 스키마를 바꾼 결정 (2건)

| # | 결정 | 무엇을 했나 |
|---|---|---|
| **4** | FK·UNIQUE·CHECK·AUTO_INCREMENT를 **다시 붙인다** | `docs/database/schema.sql` 을 배포용 DDL로 새로 만들었습니다. 테이블 13 · **FK 16 · UNIQUE 16 · CHECK 9** · 전 테이블 AUTO_INCREMENT. `default_slot`은 MySQL 8.4의 생성 컬럼 기반 FK CASCADE 제한을 피하기 위해 일반 NULL 허용 컬럼으로 두고 앱이 값을 동기화하며 UNIQUE가 중복을 차단합니다. ERD Cloud export는 다이어그램 원본으로만 두고 저장소에는 두지 않습니다 |
| **8** | 에코마일리지에 **등록된 주소를 조회해서 쓴다** | `app_user` 에 `eco_sido_code` · `eco_sigungu_code` · `eco_address_label` · `eco_address_registered_at` 4컬럼 추가. `POST /eco/link` 때 받아 저장하고 `GET /eco/status` · `GET /mypage`가 내려줍니다. C-25 이후에는 정책 추천·지역 진단의 단일 지역 기준입니다 |

## 16.2 만들지 않기로 한 것 (6건)

| # | 결정 | 결과 |
|---|---|---|
| **1** | 마지막 방문 탭 복원 — **기능 자체 제외** | `PATCH /users/me/last-tab` 없음. C-26 이후 `GET /users/me`의 `entryScreen`은 에코 연동 상태에 따라 `WF-01`·`WF-02`·`WF-06` |
| **2** | 포켓 이름 — **"그린포켓" 고정** | `pocket_name` 컬럼·변경 API 없음. PK-06 시안의 이름 수정 UI는 제거 |
| **3** | 고지서 묶음·수정 이력 — **관리 안 함** | `upload_batch_id` · `revision_history` 없음. 조회·수정·삭제는 레코드 단위, 이력은 `updated_at` 뿐. A-2-13의 "수정 이력이 남고"는 미구현 |
| **5** | 출처 링크 — **기관명만 노출** | `source_url` 없음. `region_utility_snapshot.source_name` · `mission_catalog.source_org` 를 텍스트로 표시 |
| **6** | 미션 표시 스냅샷 — **항상 조인** | `user_mission.title_snapshot` 등 없음. 표시값은 매번 `mission_catalog` 조인. 시연 중 마스터를 바꾸지 않으면 문제없음 |
| **7** | 리포트 재계산 대상 추적 — **키로 찾는다** | `source_batch_id` 없음. 고지서 등록·수정·삭제 시 `(user_id, report_month)` 로 대상 리포트를 찾아 재계산 |

## 16.3 현행 유지 · 데이터 대기 (4건)

| # | 결정 | 결과 |
|---|---|---|
| **9** | 데모 초기화 CASCADE | 결정 4로 해결. `DELETE FROM app_user WHERE id = :uid` 한 줄로 사용자 데이터 8개 테이블이 비고 마스터 3개가 남는 것을 실측 확인 |
| **10** | 녹색생활 항목 상한 | 값이 확정될 때까지 `monthlyCapAmount` · `annualCapAmount` 를 `null` 로 내리고 FE는 상한을 표시하지 않음 (담당 아영) |
| **11** | 지역난방 | **미지원 확정.** `utility_type` 은 전기·가스·수도 3종 유지. 데모 페르소나가 도시가스 사용이라 시연에 지장 없음 |
| **12** | 수도·가스 지역 평균 | 2026-09-09 결정 C-29·C-35로 대체. 지역 평균 금액 비교를 폐기하고, 도시가스는 KESIS 전국 1인 가구 월별 마이크로데이터 가중평균, 수도는 서울 아파트 1인 가구 일 사용량 월환산값을 사용함 |

## 16.4 화면 문구를 고치기로 한 것 (1건)

| # | 결정 | 해야 할 일 |
|---|---|---|
| **13** | **화면 문구를 계산값으로 수정**한다 | 시안의 합산 감축률 **10.5%** · 최종 감축률 **12%** · 전기 탄소 비중 **83%** 는, 같은 문서의 기준값(1,340kWh · 108㎥ · 66㎥)과 계수(424 · 2,240 · 332)로 계산하면 **11.322% · 12.499% · 82.5%** 가 나옵니다. 이 명세서의 예시 JSON은 전부 **계산값**입니다. **FE는 WF-04 · WF-06 · WF-07 · WF-10 시안의 하드코딩 숫자를 계산값으로 바꿔주세요** |

지급 구간(10~15%)과 마일리지(30,000M)는 어느 쪽이든 같아서 시연 스토리는 그대로입니다. 계산은 DDL을 올린 MariaDB에서 실제 쿼리로 재확인했습니다.

```sql
SELECT ROUND((SUM(baseline_usage*carbon_factor_g) - SUM(target_usage*carbon_factor_g))
             / SUM(baseline_usage*carbon_factor_g) * 100, 3) AS combined_rate
FROM eco_round_utility WHERE eco_round_id = :rid AND is_registered = 1;
-- → 11.322  (기준 831,992 → 목표 737,792.4 gCO2e)
```

## 16.5 아직 답을 기다리는 값

스키마·API는 준비됐고 **값만** 채우면 됩니다 (기능명세서 「확인 필요 사항」 시트).

| 값 | 담당 | 들어갈 자리 |
|---|---|---|
| KESIS 1인 가구 전기·도시가스 월별 마이크로데이터 | 유현 | 2026-09-09 14차(2023년 기준) 원자료 확보·검증 완료. 가구 횡단가중치로 전기 1,221가구·도시가스 841가구의 월별 평균을 산출해 `single-household-utility-baselines.json`에 반영(결정 C-35) |
| 녹색생활실천 나머지 항목 단가·상한 | 아영 | `greenlife_item.unit_price` · `monthly_cap_amount` · `annual_cap_amount` |
| 실천 미션 출처 수치·산출 근거·기관 | — | `mission_catalog` (세 값 없으면 INSERT 실패) |
| 에코마일리지 시드(2024·2025년 4~9월) | 민철 | `utility_monthly_record(record_source='ECO_BASELINE')` |
| OCR 샘플·인식률 | 준수 | — |

# 17. 2026-09-07 JWT 인증 결정

기존 서비스 API의 URL과 요청·응답 DTO는 유지하고 사용자 식별 계층만 데모 키에서 JWT로 전환합니다. 상세 보안·쿠키·스키마 설계는 `docs/auth/jwt-auth.md`를 따릅니다.

| 항목 | 결정 |
|---|---|
| 기능 | COM-13 회원가입 · COM-14 로그인 · COM-15 재발급 · COM-16 로그아웃을 P0으로 추가 |
| Access | JWT HS256, 30분, `sub=userId`, Bearer 헤더 |
| Refresh | 14일 HttpOnly 쿠키, DB에는 SHA-256 해시만 저장, 재발급 시 회전 |
| 비밀번호 | BCrypt, 8자 이상·UTF-8 기준 72바이트 이하, 원문 저장·응답·로그 금지 |
| DB | `app_user.demo_key` NULL 허용 + `auth_account`·`auth_refresh_token` 추가. Flyway V3 적용 |
| 데모 | `X-Demo-Key`, `POST /users`, `POST /demo/reset`은 `dev`·`demo` 프로필에서만 유지 |
| 제외 | 이메일 인증, 비밀번호 재설정, 소셜 로그인, MFA, 역할 권한, Access 블랙리스트 |

> 원본 XLSX 동기화와 프론트엔드 구현은 별도 작업입니다. 백엔드는 인증 API·Bearer 공통 인증·Refresh 회전까지 구현했습니다.

# 18. 2026-09-09 청년정책 추천·가입 정보 결정

| 항목 | 결정 |
|---|---|
| 화면 | 하단 `마이페이지` 표기를 `마이`로 변경하고 기존 마이 기능 아래에 청년정책 추천을 추가 |
| 회원가입 | 이름·생년월일·성별·휴대전화번호를 본인인증 성공값으로 모두 필수 저장. 외부 본인인증 API는 사용하지 않음 |
| 온보딩 | ONB-02·03을 제거하고 가입 직후 `WF-01` 에코마일리지 연동 화면으로 이동 |
| 선택 정보 | 현재 상태·연소득 구간·학력·관심 분야 1~2개는 원하는 사용자만 마이에서 저장. 가구·혼인 상태·주거 형태·평수는 수집하지 않음 |
| 지역 | 에코마일리지 연동 주소가 단일 기준. 미연동은 전국 정책만 추천하고 연동 CTA 표시 |
| 데이터 | 온통청년 OPEN API 전체는 100건 단위 갱신 검증에만 사용. 팀이 검증한 고정 정책 ID 60건만 로컬 DB에 저장하고 사용자 조회 때 외부 API를 직접 호출하지 않음 |
| 활성 정책 | 검증된 60건이 모두 승인 완료·현재 신청 가능·개인 대상 제공 방식·분류 가능·지역 정보·실제 신청 경로 조건을 통과해야 교체. 미선정 정책은 삭제 |
| 장애 대응 | 외부 API 실패나 인증키 미설정 시 번들 검증 스냅샷 또는 마지막 정상 60건을 유지 |
| 판정 | 구조화된 생년월일·지역·취업·연소득·학력 조건을 명확히 충족한 `ELIGIBLE` 정책만 최대 5개 추천. 불확실하거나 추가 자격 확인이 필요한 정책은 맞춤 추천에서 제외 |
| 조건 수정 | 관심 분야는 맞춤 추천 필터로 반영. 추천 조건은 MY-02에서 `저장하고 추천받기`를 누를 때만 갱신하며, 정책 상세에서는 조건 수정·재추천을 제공하지 않음 |
| 보안 | `YOUTH_POLICY_API_KEY` 환경변수 사용. 인증키·응답 개인정보를 저장소나 로그에 남기지 않음 |
| 제외 | 신청 대행, 자격 확정, 온통청년 마이데이터 연동 |

# 부록 A. 시연 흐름 API 호출 순서

핵심 시연 흐름(개요 시트)을 그대로 API로 옮긴 것입니다. 발표 리허설·통합 테스트 체크리스트로 쓰세요.

```
 1. POST /auth/signup 또는 /auth/login            Access + Refresh 발급
 2. GET  /users/me              (Bearer)          → entryScreen: WF-01
 3. GET  /eco/home                               WF_01_UNLINKED
 4. POST /eco/link              → GET /eco/link/{id} 폴링   WF-02
 5. GET  /eco/rounds/current                     WF-03 기준 사용량·비중
 6. GET  /eco/rounds/7/goal-form                 WF-04
 7. POST /eco/rounds/7/goal/preview  (칩·미션 바꿀 때마다)
 8. POST /eco/rounds/7/goal                      목표 저장 → WF-06
 9. GET  /bills/target-month     → POST /bills/ocr → GET /bills/ocr/{id}   AN-02~04
10. GET  /bills/duplicate-check  → POST /bills                             AN-05~06
11. GET  /diagnosis?month=2026-08                AN-07
12. GET  /eco/home                               WF-06 (누적 갱신)
13. GET  /eco/monthly-report?month=2026-07       WF-07
14. GET  /eco/rounds/7/mission-adjust?utility=ELECTRICITY → PUT .../missions   WF-08
15. GET  /eco/rounds/7/result                    WF-10
16. GET  /eco/rounds/7/settlement                WF-11
17. POST /pocket/conversions     → POST /pocket/conversions/{id}/complete
18. GET  /greenlife/status       → POST /greenlife/link → GET /greenlife/items   BN-01~02
19. POST /greenlife/settlements                  월 지급분 → 포켓 입금
20. GET  /pocket                 → POST /pocket/withdrawals                PK-02~04
21. GET  /pocket/transactions                    PK-05
22. GET  /pocket/recommended-product             PK-01·02 → PK-09
23. GET  /mypage                 → PUT /profile/policy-preferences          MY-01·MY-02
24. GET  /policies/recommendations → GET /policies                         MY-01·MY-05
25. GET  /reports                                                        MY-04
26. POST /auth/logout                            Refresh 폐기

개발·시연 프로필에서는 1번을 `POST /users`, 마지막 로그아웃을 `POST /demo/reset`으로 대체할 수 있습니다.
```

# 부록 B. 검증 체크리스트 (완료 조건 → 테스트)

| 검증 | 기준 | 근거 |
|---|---|---|
| 회원 인증 | 가입 → Bearer 보호 API → Access 만료 후 회전 재발급 → 로그아웃 후 재발급 401 | COM-13~16 |
| 로그인 오류 | 미가입 이메일과 비밀번호 불일치가 모두 `AUTH_CREDENTIALS_INVALID` | COM-14 |
| 구간 경계 | `combinedRate` = 4.999 / 5.000 / 9.999 / 10.000 / 14.999 / 15.000 → 0 / 10,000 / 10,000 / 30,000 / 30,000 / 50,000 M | B-2-07 |
| 목표 사용량 | 1,340 × 0.9 = **1,206 kWh** | B-2-04 |
| 합산 감축률 | 전기 10%·가스 15%·수도 5% → **11.322%** (아래 ⚠︎) | 계산식 6 |
| 최종 감축률 | 1,340→1,166 / 108→95 / 66→62.7 → **12.499%** (아래 ⚠︎) | B-5-02 |
| 월 감축률 | 265→270 / 9→7.6 / 11→9.8 → **1.039%** (아래 ⚠︎) | B-4-02 |
| 줄어드는 요금 | 420,600 × 10.5% ≈ **44,090원** | B-2-07 |
| 미션 환산 | 40 ÷ (1,340 ÷ 6) × 100 = **18%** | B-3-02 |
| 기기 그룹 | 냉방 26℃(3%) + 에어컨 1시간(18%) 동시 선택 → 합계 **18%** (중복 가산 없음) | B-3-04 |
| 누적·월 | 시드 4~7월 → 누적 **9%**, 7월 **1%** | B-4-02 |
| 역산 | (10×6 − 9×4) ÷ 2 = **12%** / 남은 개월 0 → `null` (0 나눗셈 없음) | B-4-08 |
| 비중 합 | `shareRate` 합 = **100** | B-1-06 |
| 합계 일치 | 진단 `summary.currentTotal` = 항목 합 | A-3-05 |
| 차액 부호 | 양수·0·음수 경계 통과 | A-3-08 |
| 고지서 중복 | 같은 (월 × 항목) 재등록 → `409 BILL_DUPLICATED` | A-2-09 |
| 출금 멱등 | 같은 `Idempotency-Key` 2회 → 거래 **1건**, 응답 동일 | D-3-03 |
| 전환 중복 | 같은 회차 전환 2회 → `409 CONVERSION_ALREADY_DONE` | D-2-02 |
| 녹색생활 월 정산 | 같은 월 2회 → 거래 1건, `created:false` | C-2-06 |
| 잔액 | `SUM(CREDIT) − SUM(DEBIT)` (`COMPLETED`만) = 표시 잔액 | D-1-02 |
| 잔액 제외 | 예상 마일리지·`PENDING` 포인트·`savedAmount` 미포함 | 비즈니스 규칙 3 |
| 기본 계좌 | 두 계좌를 기본으로 지정 시도 → 1건만 유지 | D-3-01 |
| 실패 처리 | 전환·출금 `FAILED` → 잔액 불변, 완료 화면 미표시 | D-2-03 · 비즈니스 규칙 11 |
| 기준선 부재 | 1인 가구 사용량 기준 없음 → `available:false`, **임의 값 생성 금지** | A-3-03 |
| 결산 모달 | 닫은 뒤 재진입 → 다시 뜨지 않음 | B-5-01 |
| 데모 초기화 | 초기화 후 사용자 데이터 0건, 마스터 유지 | COM-10 |

> ⚠︎ 위 세 줄은 **시안 문구(10.5% · 12% · 83%)가 아니라 계산값**입니다. 2026-09-03 결정 13에 따라 시안 쪽을 계산값으로 맞추기로 했습니다(16.4절).

**DB 층은 이미 실측 확인했습니다.** 위 항목 중 고지서 중복 · 출금 멱등 · 전환 중복 · 녹색생활 월 정산 · 기본 계좌 · 음수 금액 · 데모 초기화 7가지는 `docs/database/schema.sql` 을 MariaDB 10.11에 올려 실제로 차단되는 것을 확인했고, 에러 코드는 그 파일 하단 주석에 있습니다. **애플리케이션 테스트는 "DB가 막았을 때 사용자에게 어떤 화면이 나가는가"에 집중하세요** — 특히 멱등 재요청은 409가 아니라 **200으로 기존 거래를 돌려주는지**가 핵심입니다(1.6절).
