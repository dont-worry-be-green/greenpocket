# GreenPocket Backend — Spring Boot 규칙

> 루트 `AGENTS.md`를 먼저 읽는다. 이 문서는 백엔드 전용 규칙이다.

---

## 1. 검증 명령어

작업 후 **반드시 컴파일과 테스트를 확인한다.** 확인 없이 완료를 보고하지 않는다.

```bash
cd backend
./gradlew compileJava     # 컴파일 확인 (가장 빠름)
./gradlew test            # 테스트
./gradlew build           # 전체 빌드
```

> `./gradlew bootRun`(앱 실행)에는 MySQL과 `.env`의 DB 접속 정보가 필요하다.
> DB가 없어 실행이 실패하는 것은 정상이며, 코드 검증은 `compileJava`와 `test`로 한다.

---

## 2. Spring Boot 4.1.1 — 버전 주의 ⚠️ 가장 중요

이 프로젝트는 **Spring Boot 4.1.1 / Java 21**이다. 널리 알려진 2.x·3.x 관행을 그대로 적용하면 빌드가 깨진다.

| 하지 말 것 | 올바른 것 |
| --- | --- |
| `spring-boot-starter-web`으로 되돌리기 | 이 프로젝트는 **`spring-boot-starter-webmvc`** 를 쓴다 |
| `javax.persistence.*`, `javax.validation.*` | **`jakarta.persistence.*`, `jakarta.validation.*`** |
| `@MockBean`, `@SpyBean` | **`@MockitoBean`, `@MockitoSpyBean`** |
| 기억에 의존해 의존성·API 시그니처 작성 | `build.gradle`의 **실제 의존성을 먼저 확인** |

- **`build.gradle`을 "고쳐주지" 않는다.** 현재 의존성 구성은 의도된 것이다.
- 최신 버전이라 관행이 다를 수 있다. **확실하지 않으면 추측하지 말고 사용자에게 확인한다.**

---

## 3. Flyway 마이그레이션 ⚠️ 앱 부팅과 직결

`application.yaml`에 **`ddl-auto: validate`** 가 설정되어 있다.
엔티티와 실제 테이블 구조가 다르면 **애플리케이션이 아예 뜨지 않는다.**

### 절대 규칙

1. **엔티티를 추가·변경하면 반드시 마이그레이션 SQL을 함께 작성한다.**
   엔티티만 수정하면 다음 실행에서 부팅이 실패한다.
2. **이미 커밋된 마이그레이션 파일을 수정하지 않는다.**
   Flyway 체크섬이 깨져 부팅에 실패한다. 변경이 필요하면 **새 버전 파일을 추가한다.**
3. **`ddl-auto` 값을 `update`나 `create`로 바꾸지 않는다.**
   문제를 감추고 데이터를 파괴한다. 부팅 에러의 해결책이 아니다.

### 파일 규칙

- 위치: `src/main/resources/db/migration/`
- 형식: `V<번호>__<스네이크_케이스_설명>.sql` — 예: `V2__add_pocket_transaction.sql`
- 시드 데이터: `src/main/resources/db/seed/`
- **새 마이그레이션을 만들기 전에 기존 파일의 최대 번호를 확인한다.** 번호가 겹치면 Flyway가 실패하며, 여러 명이 동시에 작업 중이라 충돌 가능성이 높다.

---

## 4. 패키지 구조와 레이어

도메인 구분은 `docs/api/api-spec.md` 15.1 「API 60개 한눈에 보기」의 영역과 1:1이다.

```
com.greenpocket
├── user/         데모 사용자, 지역 메타, 데모 초기화        API 4  · 4절
├── profile/      주거 프로필                              API 3  · 5절
├── bill/         관리비·전기·수도·가스 고지서, OCR (ocr/)   API 9  · 6절
├── diagnosis/    지역 평균·작년 동월 비교 진단              API 3  · 7절
├── eco/          에코마일리지 연동·목표·진행·평가 결과       API 20 · 8~11절
│   └── mission/  실천 미션 선택·일일 기록·재조정
├── greenlife/    탄소중립포인트 녹색생활실천                API 5  · 12절
├── pocket/       잔액·적립 내역·마일리지 전환·출금 계좌      API 14 · 13절
├── mypage/       마이페이지, 리포트 보관함                  API 2  · 14절
└── global/       config, exception, entity, response  ← 공통
```

각 도메인은 `controller / service / repository / entity / dto` 하위 구조를 가진다.
`mypage/`는 다른 도메인의 서비스를 조합해 보여주기만 하므로 `repository / entity`를 두지 않는다.

### 레이어 규칙

- 호출 방향은 **`controller → service → repository` 한 방향**이다. 역방향이나 건너뛰기를 하지 않는다.
- **컨트롤러는 엔티티를 반환하지 않는다.** 반드시 DTO로 변환한다.
- **다른 도메인의 `repository`를 직접 호출하지 않는다.** 상대 도메인의 `service`를 거친다.
- 비즈니스 로직은 서비스에 둔다. 컨트롤러는 요청/응답 변환만 담당한다.

---

## 5. JPA 주의사항

`application.yaml`에 **`open-in-view: false`** 가 설정되어 있다. **트랜잭션 밖에서는 지연 로딩이 동작하지 않는다.**

- 엔티티를 그대로 컨트롤러까지 넘기면 `LazyInitializationException`이 발생한다.
- **DTO 변환은 반드시 `@Transactional` 서비스 메서드 안에서 끝낸다.**
- 연관관계는 기본 `LAZY`로 둔다. `EAGER`를 쓰지 않는다.
- 조회 전용 메서드에는 `@Transactional(readOnly = true)`를 붙인다.
- 공통 필드(생성일시·수정일시)는 `global/entity`의 베이스 엔티티를 상속해 사용한다.

---

## 6. 공통 응답·예외

- 모든 API 응답은 `global/response`의 공통 포맷으로 감싼다. **도메인마다 다른 응답 구조를 만들지 않는다.**
- 예외는 `global/exception`의 전역 핸들러에서 처리한다. 컨트롤러에서 `try-catch`로 삼키지 않는다.
- 요청 검증은 `jakarta.validation` 어노테이션 + `@Valid`로 처리한다.

> `global/` 아래 공통 클래스가 아직 없다면 **임의로 만들지 말고 사용자에게 확인한다.**
> 여러 명이 각자 만들면 응답 포맷이 갈라져 프론트 연동이 깨진다.

---

## 7. Lombok

- 사용: `@Getter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`
- **엔티티에 `@Setter`와 `@Data`를 쓰지 않는다.** 상태 변경은 의미 있는 메서드로 표현한다.
  (예: `bill.updateAmount(...)` ✅ / `bill.setAmount(...)` ❌)
- **엔티티에 `@ToString`을 쓰지 않는다.** 양방향 연관관계에서 순환 참조로 무한 루프가 발생한다.
- 엔티티 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 제한한다.

---

## 8. 네이밍

| 대상 | 규칙 | 예 |
| --- | --- | --- |
| 테이블·컬럼 | `snake_case` — **`schema.sql`에 정의된 이름을 그대로 쓴다** | `utility_monthly_record`, `charged_amount` |
| 엔티티·필드 | `PascalCase` / `camelCase` | `UtilityMonthlyRecord`, `chargedAmount` |
| 엔드포인트 | `api-spec.md`에 적힌 경로를 그대로 쓴다 | `/api/v1/bills`, `/api/v1/eco/rounds/{id}/goal` |
| DTO | `<기능>Request` / `<기능>Response` | `BillCreateRequest` |

**테이블·컬럼명을 새로 짓지 않는다.** `docs/database/schema.sql`이 데이터 기준이므로 엔티티는 그 이름에 맞춘다. ENUM 값도 DDL 정의와 1:1이다.

---

## 9. 금액·계산 규칙

- **금액 계산에 `double`이나 `float`를 쓰지 않는다.** 부동소수점 오차로 금액이 어긋난다. 금액은 **정수형(원 단위)**, 사용량·비율은 `BigDecimal`을 쓴다.
- 타입은 `api-spec.md` 1.4절과 DDL을 따른다 — 금액 정수 원 · 사용량 `DECIMAL(12,3)` · 비율 `DECIMAL(7,3)` · 신뢰도 `DECIMAL(5,4)`.
- **공식 요금표 기반 요금 계산 엔진은 MVP 제외 범위다.** 항목별 예상 절감액은 `S_i = B_base,i × r_i ÷ 100` 비례 계산이다. 요금표를 끌어와 계산하지 않는다.
- **배출계수·단가를 코드에 하드코딩하지 않는다.** 계수는 `eco_round_utility.carbon_factor_g`, 단가는 `greenlife_item.unit_price`처럼 **DB에 저장된 값을 읽어 쓴다.** 계산 조건(기준 기간·등록 항목·계수)을 결과와 함께 저장한다.
- **구간 경계는 상위 구간으로 간다.** `R = 5.000 / 10.000 / 15.000` → 각각 10,000M / 30,000M / 50,000M. `4.999`는 0M.
- **포켓 잔액은 캐시 컬럼을 두지 않는다.** 조회할 때마다 `SUM(CREDIT) − SUM(DEBIT)`를 **`COMPLETED` 거래만으로** 계산한다.
- **검증 기준값은 `api-spec.md` 부록 B**에 있다. 계산 로직을 구현하면 그 표의 값으로 테스트한다.

---

## 10. 멱등·중복 방지

DB가 1차 방어선이고 애플리케이션은 **DB가 막았을 때 어떤 응답을 주는가**를 책임진다.

| 대상 | 보장 수단 | 재요청 시 응답 |
| --- | --- | --- |
| 고지서 | `UNIQUE(user_id, record_source, billing_month, utility_type)` | `409 BILL_DUPLICATED` |
| 마일리지 전환 | `UNIQUE(source_type, source_key)` — 회차당 1회 | `409 CONVERSION_ALREADY_DONE` |
| 출금 | `UNIQUE(idempotency_key)` | **`200` + 기존 거래** (409 아님) |
| 녹색생활 월 정산 | `yearMonth`가 키 | **`200` + `created: false`** |
| 기본 출금 계좌 | `default_slot` 생성 컬럼 + UNIQUE | 이전 기본 계좌 해제 후 1건 유지 |

- **`Idempotency-Key` 재요청에 409를 주지 않는다.** 기존 거래를 `200`으로 돌려주는 것이 명세다(`api-spec.md` 1.6절).
- **실패한 거래는 잔액을 바꾸지 않는다.** `FAILED`로 남기고 재시도해도 거래가 중복 생성되지 않아야 한다.
- 계좌번호는 `VARBINARY`로 암호화 저장하고 **원문을 로그에 남기지 않는다.**

---

## 11. 데이터 없음 처리

- **비교 기준선이 없어도 에러가 아니다.** `200` + `available: false` + `unavailableReason`으로 응답한다 (예: `REGION_DATA_NOT_PUBLISHED`, `SAMPLE_TOO_SMALL`, `NO_BASELINE`).
- **값을 추정하거나 그럴듯한 숫자를 채워 넣지 않는다.** 수도·가스 지역 평균은 현재 미확보 상태이며, 데이터가 들어오면 API 변경 없이 `true`로 바뀐다(결정 C-12).
- OCR·연동 같은 비동기 작업은 `202` + `jobId`로 응답하고, 폴링 결과가 `SUCCEEDED`가 아니면 완료로 취급하지 않는다.
