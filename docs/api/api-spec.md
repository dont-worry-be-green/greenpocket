# 그린포켓 API 명세서

| 항목 | 내용 |
|---|---|
| 문서 기준일 | 2026-09-03 (ver2 — 팀 결정 13건 반영, 16절) |
| 참가팀 | 돈워리, 비그린 (Don't worry, be green) |
| 기준 문서 | `docs/feature-spec/기능명세서.xlsx` (105건) · `docs/database/schema.sql` (13테이블) |
| 대상 범위 | **P0 78건 + P1 24건**. P2 3건(D-3-06 · D-4-01 · E-2-02)은 15절에 자리만 표기 |
| API 수 | **60개** (P0 43 · P1 17) |
| 인증 | 로그인 없음. `X-Demo-Key` 헤더로 데모 사용자 식별 (결정 A-4) |
| 서버 | Spring Boot · MySQL 8.4 · Base URL `/api/v1` |
| 배포 DDL | `docs/database/schema.sql` — FK·UNIQUE·CHECK 포함본. **배포에 쓰는 유일한 DDL** |

> **우선순위 규칙** — 두 기준 문서가 어긋나면 **기능은 엑셀, 데이터는 `schema.sql`**이 이깁니다.
> 이전 버전에서 열어 두었던 결정 13건은 2026-09-03에 전부 확정됐고 이 문서에 반영돼 있습니다. 무엇을 어떻게 정했는지는 **16절**을 보세요.

---

## 목차

| 절 | 내용 |
|---|---|
| 1 | 공통 규칙 (인증 · 응답 래퍼 · 타입 · 페이징 · 멱등성) |
| 2 | 공통 에러 코드 |
| 3 | 열거형(ENUM) 사전 |
| 4 | 공통·데모 API (COM) |
| 5 | 프로필 API (A-1) |
| 6 | 고지서 API (A-2) |
| 7 | 진단 API (A-3) |
| 8 | 에코마일리지 연동 API (B-1) |
| 9 | 목표·미션 API (B-2 · B-3) |
| 10 | 진행 현황·전달 리포트 API (B-4) |
| 11 | 평가 결과·마일리지 API (B-5) |
| 12 | 혜택 API (C) |
| 13 | 포켓 API (D) |
| 14 | 마이페이지·보관함 API (E) |
| 15 | 매핑표 (화면 ↔ API · 기능 ID ↔ API · DB ↔ API) |
| 16 | 2026-09-03 결정 기록 |

---

# 1. 공통 규칙

## 1.1 Base URL · 버전

```
https://{host}/api/v1
```

## 1.2 인증 — `X-Demo-Key`

로그인·회원가입이 없습니다(결정 A-4). 앱이 최초 실행 시 기기에서 UUID를 만들어 보관하고, **모든 요청에 헤더로 붙입니다.**

```http
X-Demo-Key: 9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41
```

| 규칙 | 내용 |
|---|---|
| 발급 | FE가 생성한 UUID v4. `POST /users` 로 서버에 등록되면 `app_user.demo_key`(UNIQUE)에 저장 |
| 예외 | `POST /users`, `GET /meta/**`, `POST /demo/reset` 은 헤더 없이도 호출 가능 |
| 미등록 키 | `401 UNAUTHENTICATED_DEMO_KEY` — FE는 온보딩(ONB-01)으로 보냄 |
| 서버 동작 | 헤더 → `app_user.id` 해석. **요청 본문·경로에 `userId`를 받지 않습니다.** 데모에서 남의 데이터를 건드릴 경로 자체를 없앱니다 |
| 로깅 | demo_key는 앞 8자만 로그. 계좌번호 원문은 어떤 로그에도 남기지 않습니다(COM-11 · 결정 A-6) |

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
| `POST /pocket/conversions/{id}/complete` | `Idempotency-Key` 헤더 (필수) | `UNIQUE(source_type='ECO_ROUND', source_key=회차id)` |
| `POST /greenlife/settlements` | 본문 `yearMonth` 가 키 | `UNIQUE(source_type='GREENLIFE_MONTH', source_key='2026-08')` |

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
| 실제 외부 연동 | 에코마일리지·녹색생활실천은 시드 기반 모의(결정 A-5). 엔드포인트 이름만 실제 연동과 같게 씀 |
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
| `UNAUTHENTICATED_DEMO_KEY` | 401 | 데모 사용자를 찾을 수 없어요. 처음부터 시작해 주세요. |
| `NOT_FOUND` | 404 | 요청한 정보를 찾을 수 없어요. |
| `CONFLICT` | 409 | 이미 처리된 요청이에요. |
| `TOO_MANY_REQUESTS` | 429 | 잠시 후 다시 시도해 주세요. |
| `INTERNAL_ERROR` | 500 | 잠시 문제가 생겼어요. 다시 시도해 주세요. |
| `EXTERNAL_TIMEOUT` | 504 | 시간이 오래 걸리고 있어요. 다시 시도해 주세요. |

## 2.2 도메인별

| 도메인 | code | HTTP | 조건 | 기능 ID |
|---|---|---|---|---|
| 프로필 | `NAME_INVALID` | 400 | 공백 제거 후 1~20자 아님 / 특수문자만 | COM-01 |
| | `PROFILE_INCOMPLETE` | 409 | 지역·주거형태·평수 중 미입력 | A-1-05 |
| | `REGION_NOT_FOUND` | 404 | 없는 행정구역 코드 | A-1-01 |
| 고지서 | `IMAGE_TOO_LARGE` | 413 | 10MB 초과 | A-2-03 |
| | `IMAGE_UNSUPPORTED` | 415 | JPG·PNG 아님 | A-2-03 |
| | `OCR_JOB_NOT_FOUND` | 404 | 잘못된 jobId | A-2-04 |
| | `OCR_FAILED` | 422 | 인식 0건·지원하지 않는 양식 → 직접 입력 유도 | A-2-07 |
| | `BILL_DUPLICATED` | 409 | 같은 (월 × 항목) 이미 등록 — **항목 단위** | A-2-09 · 결정 A-3 |
| | `BILL_ITEM_EMPTY` | 400 | 내용 있는 항목 0개 | A-2-10 |
| | `BILL_USAGE_REQUIRED` | 400 | 사용량 누락·0 이하 | A-2-08 · A-2-09 |
| | `BILL_ELECTRICITY_REQUIRED` | 400 | 직접 입력에서 전기 미입력 | A-2-08 |
| 진단 | `DIAGNOSIS_MONTH_EMPTY` | 404 | 해당 월 고지서 없음 | A-3-09 |
| What-if | `ECO_NOT_SEOUL` | 403 | 프로필 시도 ≠ 서울(11) | B-1-09 |
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

> **비교 데이터 부재는 에러가 아닙니다.** 지역 평균이 없거나(A-3-03) 작년 값이 없으면(A-3-06) `200` + `available: false` + `unavailableReason` 으로 내려서 화면이 "비교 데이터 준비 중"을 띄우게 합니다. 임의 값을 만들지 않습니다(비즈니스 규칙 8).

---

# 3. 열거형(ENUM) 사전

**전부 `docs/database/schema.sql` 의 ENUM 정의와 1:1입니다.** 값을 추가하려면 DDL부터 고칩니다.

| 이름 | 값 | 쓰는 곳 |
|---|---|---|
| `UtilityType` | `ELECTRICITY` · `GAS` · `WATER` | 전 영역 |
| `UsageUnit` | `kWh` · `m3` | 전기 kWh, 가스·수도 m3 |
| `HousingType` | `ONE_ROOM` · `OFFICETEL` · `APARTMENT` · `MULTI_HOUSE` | 원룸·오피스텔·아파트·다세대 |
| `AreaBand` | `UNDER_10` · `FROM_10_TO_20` · `OVER_20` | 10평 이하·10~20평·20평 이상 |
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

---

# 4. 공통·데모 API (COM)

## 4.1 데모 사용자 시작

`POST /users` · **P0** · COM-01 · ONB-01

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
  "onboardingCompleted": false,
  "nextScreen": "ONB-02",
  "pocketAccountNo": "1005-1234-5678-90",
  "pocketHolder": "김수현",
  "createdAt": "2026-09-03T18:30:00+09:00"
}
```

- 그린포켓 계좌번호·예금주는 **가입 시 서버가 발급**합니다(D-1-01). 별도 생성 API 없음.
- 같은 `demoKey` 재요청 → `200 OK` + 기존 사용자 (재진입, 이름은 갱신하지 않음).

**Errors** `NAME_INVALID(400)`

---

## 4.2 앱 부트스트랩 (내 상태)

`GET /users/me` · **P0** · COM-01 · COM-02 · COM-12 · 전 화면

앱 진입 시 **한 번 호출해서 어느 화면으로 보낼지 결정**합니다.

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
| `entryScreen` | 서버가 판단한 진입 화면. `onboardingCompleted=false` → `ONB-01`, 완료면 홈인 `WF-06` (COM-02) |
| `hasBill` | 등록 고지서 1건 이상 여부. 진단 빈 상태(A-3-04) 분기용 |

> **마지막 방문 탭 복원은 만들지 않습니다**(결정 1). 온보딩을 마쳤으면 항상 What-if 탭(홈)으로 들어갑니다.

---

## 4.3 행정구역 목록

`GET /meta/regions` · **P0** · A-1-01 · ONB-02

| 쿼리 | 필수 | 설명 |
|---|---|---|
| `sidoCode` | | 없으면 시도 목록, 있으면 그 시도의 시군구 목록 |

**Response 200**

```json
{
  "level": "SIGUNGU",
  "items": [
    { "code": "11620", "name": "관악구", "sidoCode": "11", "hasRegionAverage": true }
  ]
}
```

- `hasRegionAverage` — `region_utility_snapshot` 에 해당 지역 행이 있는지. FE가 "비교 자료가 없는 지역은 더 넓은 범위의 평균을 써요" 안내를 미리 띄우는 데 씁니다(A-1-01).
- 시군구 코드는 한전 API `cityCd` 와 겸용(`app_user.sigungu_code` COMMENT).

---

## 4.4 데모 초기화

`POST /demo/reset` · **P0** · COM-10 · MY-01

**Request**

```json
{ "demoKey": "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41" }
```

**Response 200**

```json
{ "resetAt": "2026-09-03T18:31:00+09:00", "nextScreen": "ONB-01" }
```

- 서버 동작: `DELETE FROM app_user WHERE id = :uid` **한 줄.** FK CASCADE로 사용자 데이터 8개 테이블이 전부 정리되고 마스터(`mission_catalog` · `greenlife_item` · `region_utility_snapshot`)만 남습니다.
- 배포 DDL(`docs/database/schema.sql`)에 FK 16개를 복원해 두었고, 위 동작을 MariaDB에서 실제로 확인했습니다(결정 4·9).

---

# 5. 프로필 API (A-1)

## 5.1 프로필 저장 (온보딩 완료)

`POST /profile` · **P0** · A-1-01 · A-1-02 · A-1-03 · A-1-05 · ONB-02

```json
{
  "sidoCode": "11",
  "sidoName": "서울특별시",
  "sigunguCode": "11620",
  "sigunguName": "관악구",
  "housingType": "APARTMENT",
  "areaBand": "OVER_20"
}
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| `sidoCode` · `sigunguCode` | ✔ | 시도 먼저, 시군구는 시도 선택 후 (A-1-01) |
| `housingType` | ✔ | `HousingType` |
| `areaBand` | ✔ | `AreaBand` |

**청년 조건(나이·소득·취업)은 받지 않습니다** — 결정 B-1로 ONB-03 화면 삭제.

**Response 200**

```json
{
  "onboardingCompleted": true,
  "profileSummary": "서울 관악구 · 아파트 20평 이상",
  "nextScreen": "WF-06",
  "seoulResident": true
}
```

- `seoulResident` = `sidoCode == "11"`. What-if 연동 가능 여부(B-1-09)를 FE가 바로 알 수 있게 함.

**Errors** `PROFILE_INCOMPLETE(409)` · `REGION_NOT_FOUND(404)`

---

## 5.2 프로필 조회

`GET /profile` · **P0** · A-1-06 · A-1-07 · MY-01 · AN-07

**Response 200**

```json
{
  "name": "김수현",
  "sidoCode": "11", "sidoName": "서울특별시",
  "sigunguCode": "11620", "sigunguName": "관악구",
  "housingType": "APARTMENT",
  "areaBand": "OVER_20",
  "profileSummary": "서울 관악구 · 아파트 20평 이상",
  "seoulResident": true,
  "onboardingCompleted": true
}
```

`profileSummary` 는 진단 헤더·마이페이지 카드가 같은 문자열을 쓰도록 서버가 조립합니다(A-1-07 "동일 값이 화면마다 다르게 표시되지 않는다").

---

## 5.3 프로필 수정

`PUT /profile` · **P0** · A-1-06 · MY-02

Request 본문은 5.1과 동일 + `name` 수정 가능.

```json
{
  "name": "김수현",
  "sidoCode": "11", "sigunguCode": "11620",
  "housingType": "ONE_ROOM", "areaBand": "UNDER_10",
  "confirmBaselineChange": true
}
```

| 필드 | 설명 |
|---|---|
| `confirmBaselineChange` | 진행 중 평가 회차가 있는데 **지역이 바뀌면** 필수. `false`/누락이면 `409 CONFLICT` + `details.warning` 으로 경고 문구 반환 → FE가 확인 다이얼로그를 띄우고 재요청 (A-1-06 예외 처리) |

**Response 200**

```json
{
  "profileSummary": "서울 관악구 · 원룸 10평 이하",
  "baselineRecalculated": true,
  "affectedRoundId": 7
}
```

- 지역이 바뀌면 서버가 진단 기준선(`region_utility_snapshot`)을 다시 조회하도록 캐시를 무효화합니다(A-1-06 완료 조건).
- 화면 MY-01의 "에코마일리지 주소 · 2026-03 등록" 은 **프로필 주소가 아니라 누리집 등록 주소**입니다(결정 8). `GET /mypage` 의 `ecoAddress` 를 쓰고, 여기서 프로필 주소를 바꿔도 그 값은 바뀌지 않습니다 — 다음 연동 때 갱신됩니다.

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
| `items` | **항상 전기·수도·가스 3건 고정.** 인식 안 된 항목은 `hasData: false` → AN-05의 "내용 없음" 탭 (A-2-06) |
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

관리비 통합 고지서와 개별 전기 고지서가 같은 달에 들어와도 **겹치는 항목만** 막습니다(결정 A-3). 저장 시 서버가 다시 검증하므로 이 API는 폼 실시간 안내용입니다.

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

  "regionComparison": {
    "regionLevel": "SIGUNGU",
    "regionLabel": "서울 관악구",
    "fallbackApplied": false,
    "sourceName": "한국전력공사 전력데이터 개방포털",
    "baseMonth": "2026-07",
    "extractedAt": "2026-08-28T00:00:00+09:00",
    "tabs": [
      {
        "utilityType": "ELECTRICITY",
        "available": true,
        "unavailableReason": null,
        "myAmount": 43200,
        "regionAvgAmount": 38900,
        "diffRegion": 4300,
        "series": [
          { "yearMonth": "2026-03", "mine": 39100, "regionAvg": 37200 },
          { "yearMonth": "2026-04", "mine": 40500, "regionAvg": 37800 },
          { "yearMonth": "2026-05", "mine": 41200, "regionAvg": 38100 },
          { "yearMonth": "2026-06", "mine": 42000, "regionAvg": 38400 },
          { "yearMonth": "2026-07", "mine": 40100, "regionAvg": 38600 },
          { "yearMonth": "2026-08", "mine": 43200, "regionAvg": 38900 }
        ]
      },
      { "utilityType": "GAS",   "available": false, "unavailableReason": "REGION_DATA_NOT_PUBLISHED", "myAmount": 12400 },
      { "utilityType": "WATER", "available": false, "unavailableReason": "REGION_DATA_NOT_PUBLISHED", "myAmount": 8900 }
    ]
  },

  "whatIfLink": { "roundId": 7, "goalSet": true }
}
```

| 규칙 | 내용 |
|---|---|
| `available: false` | 지역 평균 행이 없으면 **임의 값을 만들지 않고** 이 플래그로 "비교 데이터 준비 중"을 띄움 (A-3-03 · 비즈니스 규칙 8) |
| `unavailableReason` | `REGION_DATA_NOT_PUBLISHED`(수도·가스 미확보) · `SAMPLE_TOO_SMALL` · `NO_BASELINE` |
| `fallbackApplied` | 시군구 표본 부족 → 시도 평균 대체. `regionLabel` 이 `"서울"` 로 바뀌고 FE가 범위 배지 표시 (A-3-02) |
| `diffRegion` · `diff` | **양수 = 초과, 음수 = 미만.** 부호 그대로 내려주고 표기는 FE (A-3-08) |
| `hasPreviousYear: false` | 작년 값 없음 → 배지·카드 숨김. 에코마일리지 연동 유도 (A-3-05 · A-3-06) |
| `series` | 최근 6개월. 데이터 없는 달은 `null` (A-3-07) |

**Errors** `DIAGNOSIS_MONTH_EMPTY(404)` — 등록 안 된 월을 지정했을 때

---

## 7.3 지역 기준선 단건 조회 (내부·검증용)

`GET /diagnosis/baseline` · **P0** · A-3-02 · A-3-03

| 쿼리 | 예 |
|---|---|
| `sigunguCode` | `11620` |
| `month` | `2026-07` |
| `utility` | `ELECTRICITY` |

**Response 200**

```json
{
  "found": true,
  "regionLevel": "SIGUNGU",
  "sidoCode": "11", "sigunguCode": "11620",
  "baseMonth": "2026-07",
  "utilityType": "ELECTRICITY",
  "householdCount": 132840,
  "avgUsage": 289.400,
  "avgAmount": 38900,
  "sourceName": "한국전력공사 전력데이터 개방포털",
  "extractedAt": "2026-08-28T00:00:00+09:00"
}
```

`found: false` 면 시도 평균으로 한 번 더 조회하고, 그것도 없으면 `regionLevel: null` — A-3-03 "임의 값을 만들지 않는다".
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
| `ecoAddress` | **에코마일리지 누리집에 등록된 주소**를 연동 때 받아 저장한 값(결정 8). 프로필 주소와 다르면 `matchesProfile:false` → 이사 안내(B-1-08). 미연동이면 `null` |

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

- 목표 설정 때 고른 미션 중 **현재 계절 태그(`FIND_IN_SET`)에 맞는 것만** 노출 (B-3-05).
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
      { "utilityType": "ELECTRICITY", "requiredRate": 11.000, "assumption": "도시가스 16%, 수도 11% 감축을 지금처럼 유지할 때" }
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

```json
{
  "roundId": 7,
  "utilityType": "ELECTRICITY",
  "reportMonth": "2026-07",
  "requiredRate": 11.000,
  "requiredAssumption": "도시가스 16%, 수도 11% 감축을 지금처럼 유지할 때예요",
  "carbonSharePercent": 83.0,

  "comparison": { "selectedExpectedRate": 18.000, "actualRate": -1.887 },

  "currentSelectedCount": 2,
  "missions": [
    { "missionId": 13, "title": "에어컨 하루 1시간 줄이기", "computedRate": 18.000, "difficulty": "NORMAL",
      "deviceGroup": "냉방", "evidenceText": "월 40kWh · 4,880원",
      "calculationBasis": "15평형 2kW를 20일 기준 · 40kWh ÷ 우리 집 223kWh", "sourceOrg": "한국에너지공단",
      "selected": true, "recommended": false, "capped": false },
    { "missionId": 15, "title": "에어컨 필터 청소하기", "computedRate": 5.000, "difficulty": "EASY",
      "deviceGroup": "냉방", "evidenceText": "월 10.7kWh",
      "calculationBasis": "필터를 청소하지 않으면 소비전력이 3~5% 증가", "sourceOrg": "한국에너지공단",
      "selected": false, "recommended": true, "capped": false },
    { "missionId": 16, "title": "안 쓰는 플러그 뽑기", "computedRate": 5.000, "difficulty": "EASY",
      "deviceGroup": "대기전력", "evidenceText": "가정 전력의 10% 이상",
      "calculationBasis": "대기전력이 가정·상업 전력사용량의 10%가 넘음 · 절반을 줄인다고 보수 적용", "sourceOrg": "한국에너지공단",
      "selected": false, "recommended": true, "capped": false }
  ],

  "preview": { "currentRate": 18.000, "withRecommendedRate": 28.000, "coversRequired": true },

  "tierDowngrade": { "suggest": false, "consecutiveMisses": 1,
    "message": "한 달 미끄러진 것만으로 10~15% 구간을 포기하기엔 일러요" }
}
```

| 규칙 | 내용 |
|---|---|
| `recommended` | 부족분을 메울 수 있고 **이미 고른 미션과 `deviceGroup` 이 겹치지 않는** 것 (B-4-09) |
| `preview.withRecommendedRate` | 추천을 전부 반영했을 때의 미션 합계 (기기 그룹 규칙 적용 후) |
| `tierDowngrade.suggest` | **2회 연속 미달일 때만 `true`** (비즈니스 규칙 9) |
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
| 중복 | `UNIQUE(source_type='GREENLIFE_MONTH', source_key='2026-08')`. 재호출 시 `created: false` + **기존 거래 그대로 반환** (200) |
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
    { "transactionId": 91, "transactionCode": "GP-2610-0003", "label": "녹색생활실천 9월분",
      "direction": "CREDIT", "transactionType": "GREENLIFE", "amount": 3200,
      "transactionStatus": "COMPLETED", "completedAt": "2026-10-10T00:00:00+09:00" },
    { "transactionId": 88, "transactionCode": "GP-2609-0012", "label": "녹색생활실천 8월분",
      "direction": "CREDIT", "transactionType": "GREENLIFE", "amount": 5540,
      "transactionStatus": "COMPLETED", "completedAt": "2026-09-10T00:00:00+09:00" }
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
| `recentTransactions` | 최근 2건 (D-1-04) |
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

## 13.4 적립 내역 전체

`GET /pocket/transactions` · **P0** · D-1-05 · PK-05

| 쿼리 | 값 |
|---|---|
| `direction` | `CREDIT`\|`DEBIT` (없으면 전체) |
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

월별 그룹 최신순, `subtotal` 합이 `totalCreditAmount` 와 일치해야 합니다(D-1-05 완료 조건).

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
  "externalUrl": "https://ecomileage.seoul.go.kr/mileage/convert",
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
| 회차당 1회 | `UNIQUE(source_type='ECO_ROUND', source_key=roundId)` |
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
| `PUT` | `/pocket/accounts/{accountId}/default` | P0 | 기본 계좌 지정. **사용자당 1건만** — `default_slot` 생성 컬럼 + UNIQUE가 DB에서 강제 |
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

---

# 14. 마이페이지·보관함 API (E)

## 14.1 마이페이지 메인

`GET /mypage` · **P0** · E-1-01 · E-1-02 · MY-01

```json
{
  "profile": {
    "name": "김수현",
    "sidoName": "서울특별시", "sigunguName": "관악구",
    "housingType": "ONE_ROOM", "areaBand": "UNDER_10",
    "profileSummary": "서울 관악구 · 원룸 · 10평 이하"
  },
  "links": {
    "billArchive": { "count": 14, "screen": "MY-03" },
    "reportArchive": { "count": 9, "screen": "MY-04" }
  },
  "ecoAddress": {
    "label": "서울 관악구",
    "registeredAt": "2026-03",
    "matchesProfile": true,
    "notice": "이사했다면 꼭 바꿔주세요. 바꾸지 않으면 지금 살지 않는 집의 사용량과 비교돼요"
  },
  "integration": {
    "ecoLinkStatus": "LINKED", "ecoLinkedAt": "2026-09-01T09:00:00+09:00",
    "greenlifeParticipating": true, "greenlifeLinkedAt": "2026-09-01T09:12:00+09:00",
    "registeredUtilities": ["ELECTRICITY","GAS","WATER"]
  },
  "pocketAccountNo": "1005-1234-5678-90"
}
```

| 필드 | 설명 |
|---|---|
| `ecoAddress` | **프로필 주소가 아니라 에코마일리지 누리집에 등록된 주소**입니다(결정 8). 연동 때 받아 `app_user.eco_*` 에 저장한 값을 그대로 보여줍니다. 미연동이면 `null` |
| `matchesProfile` | 프로필 주소와 시군구 코드가 같은지. `false` 면 이사 안내를 띄웁니다(B-1-08) |

**나이·소득 구간·취업 상태는 응답에 없습니다** — 결정 B-1로 제거(E-1-01).

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

# 15. 매핑표

## 15.1 API 60개 한눈에 보기

P1만 표시하고 나머지는 P0입니다. 뒤 숫자는 이 문서의 절 번호. 표 형태 목록은 노션 「API 기본 명세서」 DB에도 있습니다.

| 영역 | 엔드포인트 |
|---|---|
| 공통 (4) | `POST /users` 4.1 · `GET /users/me` 4.2 · `GET /meta/regions` 4.3 · `POST /demo/reset` 4.4 |
| 프로필 (3) | `POST /profile` 5.1 · `GET /profile` 5.2 · `PUT /profile` 5.3 |
| 고지서 (9) | `GET /bills/target-month` 6.1 · `POST /bills/ocr` 6.2 · `GET /bills/ocr/{jobId}` 6.3 · `GET /bills/duplicate-check` 6.4 · `POST /bills` 6.5 · `GET /bills` 6.6 (P1) · `GET /bills/{recordId}` 6.7 (P1) · `PUT /bills/{recordId}` 6.8 (P1) · `DELETE /bills/{recordId}` 6.9 (P1) |
| 진단 (3) | `GET /diagnosis/months` 7.1 (P1) · `GET /diagnosis` 7.2 · `GET /diagnosis/baseline` 7.3 |
| 에코 연동 (5) | `GET /eco/status` 8.1 · `POST /eco/link` 8.2 · `GET /eco/link/{linkJobId}` 8.3 · `GET /eco/rounds/current` 8.4 · `GET /eco/rounds` 8.5 (P1) |
| 목표·미션 (7) | `GET /eco/rounds/{id}/goal-form` 9.1 · `POST .../goal/preview` 9.2 · `POST .../goal` 9.3 · `PUT .../goal` 9.3 · `GET .../goal` 9.4 · `GET .../missions/today` 9.5 (P1) · `PUT .../mission-logs/{date}` 9.6 (P1) |
| 진행·리포트 (5) | `GET /eco/home` 10.1 · `POST .../result/view` 10.2 (P1) · `GET /eco/monthly-report` 10.3 · `GET .../mission-adjust` 10.4 (P1) · `PUT .../missions` 10.5 (P1) |
| 평가 결과 (3) | `GET .../result` 11.1 · `GET .../settlement` 11.2 · `POST .../application` 11.3 (P1) |
| 혜택 (5) | `GET /greenlife/status` 12.1 · `POST /greenlife/link` 12.2 · `GET /greenlife/items` 12.3 · `GET /greenlife/items/{itemId}` 12.4 (P1) · `POST /greenlife/settlements` 12.5 |
| 포켓 (14) | `GET /pocket` 13.1 · `GET /pocket/balance` 13.2 · `GET /pocket/convertible-mileage` 13.3 · `GET /pocket/transactions` 13.4 · `POST /pocket/conversions` 13.5 · `POST .../conversions/{id}/complete` 13.6 · `GET /pocket/accounts` 13.7 · `POST /pocket/accounts` 13.8 · `PUT /pocket/accounts/{id}` 13.9 · `PUT .../{id}/default` 13.9 · `DELETE .../{id}` 13.9 (P1) · `POST /pocket/withdrawals` 13.10 · `GET /pocket/withdrawals` 13.11 (P1) · `GET /pocket/management` 13.12 (P1) |
| 마이페이지 (2) | `GET /mypage` 14.1 · `GET /reports` 14.2 (P1) |

각 엔드포인트 절 제목에 담당 기능 ID가 붙어 있습니다. 기능 ID로 역추적할 때는 문서에서 `A-2-11` 처럼 검색하세요.

## 15.2 API가 없는 기능 (FE 단독 · 비개발)

P0·P1 102건 중 아래 12건은 서버 호출이 없습니다. 나머지 90건은 위 60개 API로 덮습니다.

| 기능 ID | 내용 | 왜 API가 없나 |
|---|---|---|
| COM-04 | 공통 UI 규칙 | 비개발(디자인). `design-system.md` · `tokens.css` |
| COM-06 | 금액·증감 표기 | FE 공통 포맷터. 서버는 숫자·enum만 준다(1.4절) |
| COM-07 | 예상·적립·입금 상태 표시 | FE 라벨. 서버는 `RewardStatus`·`TxStatus`·`isCash` 로 구분값만 |
| COM-09 | 시드 데이터 적재 | 비개발(데이터). 적재 스크립트는 BE |
| COM-11 | 개인정보·데모 안내 | 비개발(콘텐츠). 서버는 이미지 미저장·계좌 로그 금지로 준수 |
| A-1-02 · A-1-03 | 주거 형태·평수 선택 | 칩·라디오 UI. 값은 `POST /profile` 에 실려 나감 |
| A-2-02 | 입력 방식 선택 | FE 세그먼트. 두 경로 모두 `POST /bills` 로 수렴 |
| A-2-06 | 인식 내용 수정 | FE 3탭 폼. 저장은 `POST /bills` |
| A-2-10 | 등록 전 요약·확정 | FE 화면. 확정 전에는 저장하지 않음 |
| A-3-01 | 지역 평균 기준선 데이터 | 비개발(데이터). `region_utility_snapshot` 시드 |
| B-3-01 | 실천 미션 데이터 | 비개발(데이터). 근거 3종 NOT NULL 로 DB가 품질 강제 |
| C-1-03 | 실천 항목 데이터 | 비개발(데이터). `greenlife_item` 17건 시드 |

## 15.3 화면 → API

| 화면 ID | 화면명 | 진입 시 호출 |
|---|---|---|
| ONB-01 | 시작·이름 등록 | `POST /users` |
| ONB-02 | 주거 프로필 | `GET /meta/regions` → `POST /profile` |
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
| PK-01 | 계좌 미등록 메인 | `GET /pocket` (`empty.noAccount:true`) |
| PK-02 | 계좌 등록 메인 | `GET /pocket` · `GET /pocket/convertible-mileage` |
| PK-03 | 출금 신청 | `GET /pocket/accounts` → `POST /pocket/withdrawals` |
| PK-04 | 출금 완료 | `POST /pocket/withdrawals` 응답 |
| PK-05 | 적립 내역 | `GET /pocket/transactions` |
| PK-06 | 그린포켓 관리 | `GET /pocket/management` |
| PK-07 | 출금계좌 등록·변경 | `GET/POST/PUT /pocket/accounts` |
| PK-08 | 출금 내역 | `GET /pocket/withdrawals` |
| MY-01 | 마이페이지 메인 | `GET /mypage` |
| MY-02 | 기본 정보 수정 | `GET /profile` → `PUT /profile` |
| MY-03 | 고지서 보관함 | `GET /bills?utility=&year=` |
| MY-04 | 리포트 보관함 | `GET /reports?type=&year=` |

## 15.4 DB 테이블 → API

| 테이블 | 읽는 API | 쓰는 API |
|---|---|---|
| `app_user` | `GET /users/me` · `/profile` · `/mypage` · `/eco/status` · `/greenlife/status` · `/pocket` | `POST /users` · `POST/PUT /profile` · `POST /eco/link`(연동 상태·등록 주소) · `POST /greenlife/link` · `POST /demo/reset` |
| `utility_monthly_record` | `GET /diagnosis` · `/bills` · `/eco/monthly-report` · `/reports` | `POST/PUT/DELETE /bills` · `POST /eco/link`(ECO_BASELINE) |
| `region_utility_snapshot` | `GET /diagnosis` · `/diagnosis/baseline` · `/meta/regions` | 시드만 (COM-09) |
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

---

# 16. 2026-09-03 결정 기록

이전 버전에서 「DB ↔ 명세 불일치」로 열어 두었던 13건을 팀이 전부 확정했습니다. **이 문서와 `docs/database/schema.sql` 은 아래 결정이 이미 반영된 상태**입니다.

## 16.1 스키마를 바꾼 결정 (2건)

| # | 결정 | 무엇을 했나 |
|---|---|---|
| **4** | FK·UNIQUE·CHECK·AUTO_INCREMENT를 **다시 붙인다** | `docs/database/schema.sql` 을 배포용 DDL로 새로 만들었습니다. 테이블 13 · **FK 16 · UNIQUE 16 · CHECK 9** · 전 테이블 AUTO_INCREMENT · `default_slot` 생성 컬럼 복구. MariaDB에 올려 14개 규칙이 실제로 차단되는지 확인했고 결과를 DDL 하단 주석에 남겼습니다. ERD Cloud export는 다이어그램 원본으로만 두고 저장소에는 두지 않습니다 |
| **8** | 에코마일리지에 **등록된 주소를 조회해서 쓴다** | `app_user` 에 `eco_sido_code` · `eco_sigungu_code` · `eco_address_label` · `eco_address_registered_at` 4컬럼 추가. `POST /eco/link` 때 받아 저장하고, `GET /eco/status` · `GET /mypage` 가 `ecoAddress` 로 내려줍니다. 프로필 주소와 시군구가 다르면 `matchesProfile:false` → 이사 안내(B-1-08) |

## 16.2 만들지 않기로 한 것 (6건)

| # | 결정 | 결과 |
|---|---|---|
| **1** | 마지막 방문 탭 복원 — **기능 자체 제외** | `PATCH /users/me/last-tab` 삭제(API 61→60). `GET /users/me` 의 `entryScreen` 은 온보딩 미완료면 `ONB-01`, 완료면 항상 홈 `WF-06` |
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
| **12** | 수도·가스 지역 평균 | 데이터 미확보, 추후 확보 예정. 그때까지 `available:false` + `unavailableReason: "REGION_DATA_NOT_PUBLISHED"` (7.2절). 데이터가 들어오면 API 변경 없이 `true` 로 바뀜 |

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
| 한전 가구평균 API 인증키 · 최신 제공 월 | 유현 | `region_utility_snapshot` |
| 녹색생활실천 나머지 항목 단가·상한 | 아영 | `greenlife_item.unit_price` · `monthly_cap_amount` · `annual_cap_amount` |
| 실천 미션 출처 수치·산출 근거·기관 | — | `mission_catalog` (세 값 없으면 INSERT 실패) |
| 에코마일리지 시드(2024·2025년 4~9월) | 민철 | `utility_monthly_record(record_source='ECO_BASELINE')` |
| OCR 샘플·인식률 | 준수 | — |

# 부록 A. 시연 흐름 API 호출 순서

핵심 시연 흐름(개요 시트)을 그대로 API로 옮긴 것입니다. 발표 리허설·통합 테스트 체크리스트로 쓰세요.

```
 1. POST /users                                  이름 입력
 2. GET  /meta/regions          → POST /profile  주거 프로필
 3. GET  /users/me                               → entryScreen: WF-06(여기선 WF-01)
 4. GET  /eco/home                               WF_01_UNLINKED
 5. POST /eco/link              → GET /eco/link/{id} 폴링   WF-02
 6. GET  /eco/rounds/current                     WF-03 기준 사용량·비중
 7. GET  /eco/rounds/7/goal-form                 WF-04
 8. POST /eco/rounds/7/goal/preview  (칩·미션 바꿀 때마다)
 9. POST /eco/rounds/7/goal                      목표 저장 → WF-06
10. GET  /bills/target-month     → POST /bills/ocr → GET /bills/ocr/{id}   AN-02~04
11. GET  /bills/duplicate-check  → POST /bills                             AN-05~06
12. GET  /diagnosis?month=2026-08                AN-07
13. GET  /eco/home                               WF-06 (누적 갱신)
14. GET  /eco/monthly-report?month=2026-07       WF-07
15. GET  /eco/rounds/7/mission-adjust?utility=ELECTRICITY → PUT .../missions   WF-08
16. GET  /eco/rounds/7/result                    WF-10
17. GET  /eco/rounds/7/settlement                WF-11
18. POST /pocket/conversions     → POST /pocket/conversions/{id}/complete
19. GET  /greenlife/status       → POST /greenlife/link → GET /greenlife/items   BN-01~02
20. POST /greenlife/settlements                  월 지급분 → 포켓 입금
21. GET  /pocket                 → POST /pocket/withdrawals                PK-02~04
22. GET  /pocket/transactions                    PK-05
23. GET  /mypage                 → GET /reports                            MY-01·MY-04
24. POST /demo/reset                             다음 리허설
```

# 부록 B. 검증 체크리스트 (완료 조건 → 테스트)

| 검증 | 기준 | 근거 |
|---|---|---|
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
| 기준선 부재 | 지역 평균 없음 → `available:false`, **임의 값 생성 금지** | A-3-03 |
| 결산 모달 | 닫은 뒤 재진입 → 다시 뜨지 않음 | B-5-01 |
| 데모 초기화 | 초기화 후 사용자 데이터 0건, 마스터 유지 | COM-10 |

> ⚠︎ 위 세 줄은 **시안 문구(10.5% · 12% · 83%)가 아니라 계산값**입니다. 2026-09-03 결정 13에 따라 시안 쪽을 계산값으로 맞추기로 했습니다(16.4절).

**DB 층은 이미 실측 확인했습니다.** 위 항목 중 고지서 중복 · 출금 멱등 · 전환 중복 · 녹색생활 월 정산 · 기본 계좌 · 음수 금액 · 데모 초기화 7가지는 `docs/database/schema.sql` 을 MariaDB 10.11에 올려 실제로 차단되는 것을 확인했고, 에러 코드는 그 파일 하단 주석에 있습니다. **애플리케이션 테스트는 "DB가 막았을 때 사용자에게 어떤 화면이 나가는가"에 집중하세요** — 특히 멱등 재요청은 409가 아니라 **200으로 기존 거래를 돌려주는지**가 핵심입니다(1.6절).
