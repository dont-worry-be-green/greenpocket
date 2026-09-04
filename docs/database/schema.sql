-- ============================================================
--  그린포켓 (GreenPocket) 배포용 DDL
--  2026 KB IT's Your Life 해커톤 · 돈워리, 비그린
--
--  기준     : ERD Cloud export + 2026-09-03 팀 결정 (C-1 ~ C-13)
--  DBMS     : MySQL 8.4 · InnoDB · utf8mb4 · utf8mb4_0900_ai_ci
--  규모     : 테이블 13 · 외래키 16 · UNIQUE 16 · CHECK 9
--
--  ERD Cloud export 는 다이어그램 원본이라 PK 외 제약이 빠져 있습니다.
--  이 파일이 스키마의 기준(단일 진실 공급원)입니다.
--
--  ⚠️ 실제로 DB에 적용되는 것은 이 파일이 아니라 Flyway 마이그레이션입니다.
--     backend/src/main/resources/db/migration/V1__init_schema.sql
--     스키마를 바꿀 때는 이 파일과 새 마이그레이션(V2, V3 …)을 함께 고칩니다.
--     이미 적용된 마이그레이션 파일은 절대 수정하지 않습니다.
--
--  2026-09-03 결정 반영
--    4  FK·UNIQUE·CHECK·AUTO_INCREMENT 복원 (A안)
--    8  app_user 에 에코마일리지 등록 주소 4컬럼 추가
--    9  DELETE FROM app_user 한 줄로 사용자 데이터 CASCADE 정리
--    1  last_visited_tab 미도입 (기능 자체 제외)
--    2  pocket_name 미도입 (포켓 이름 "그린포켓" 고정)
--    3  upload_batch_id · revision_history 미도입 (고지서 묶음·수정 이력 미관리)
--    5  source_url 미도입 (기관명만 노출)
--    6  user_mission 스냅샷 컬럼 미도입 (mission_catalog 조인)
--    7  eco_monthly_report.source_batch_id 미도입 ((user_id, report_month) 로 재계산)
--   11  지역난방 미지원 (utility_type 은 전기·가스·수도 3종 유지)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `pocket_transaction`;
DROP TABLE IF EXISTS `withdrawal_account`;
DROP TABLE IF EXISTS `greenlife_activity`;
DROP TABLE IF EXISTS `greenlife_item`;
DROP TABLE IF EXISTS `mission_daily_log`;
DROP TABLE IF EXISTS `user_mission`;
DROP TABLE IF EXISTS `mission_catalog`;
DROP TABLE IF EXISTS `eco_monthly_report`;
DROP TABLE IF EXISTS `eco_round_utility`;
DROP TABLE IF EXISTS `eco_round`;
DROP TABLE IF EXISTS `region_utility_snapshot`;
DROP TABLE IF EXISTS `utility_monthly_record`;
DROP TABLE IF EXISTS `app_user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `app_user` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '사용자 ID | 사용자 내부 식별자',
	`demo_key`	VARCHAR(50)	NOT NULL	COMMENT '데모 사용자 키 | 로그인 없는 데모에서 재진입 시 같은 사용자를 찾는 기기 고유 키',
	`name`	VARCHAR(20)	NOT NULL	COMMENT '이름 | 온보딩에서 입력한 사용자 이름, 공백 제거 후 1~20자',
	`sido_code`	VARCHAR(10)	NULL	COMMENT '시도 코드 | 거주 시도 행정구역 코드, 서울은 11',
	`sido_name`	VARCHAR(30)	NULL	COMMENT '시도명 | 화면에 표시할 거주 시도명',
	`sigungu_code`	VARCHAR(10)	NULL	COMMENT '시군구 코드 | 거주 시군구 행정구역 코드, 한전 API cityCd 겸용',
	`sigungu_name`	VARCHAR(30)	NULL	COMMENT '시군구명 | 화면에 표시할 거주 시군구명',
	`housing_type`	ENUM('ONE_ROOM', 'OFFICETEL', 'APARTMENT', 'MULTI_HOUSE')	NULL	COMMENT '주거 형태 | 원룸, 오피스텔, 아파트, 다세대',
	`area_band`	ENUM('UNDER_10', 'FROM_10_TO_20', 'OVER_20')	NULL	COMMENT '평수 구간 | 10평 이하, 10~20평, 20평 이상',
	`onboarding_completed`	TINYINT(1)	NOT NULL	DEFAULT 0	COMMENT '온보딩 완료 여부 | 0 미완료 시 ONB-01로 이동',
	`eco_link_status`	ENUM('UNLINKED', 'LINKING', 'LINKED', 'FAILED')	NOT NULL	DEFAULT 'UNLINKED'	COMMENT '에코마일리지 연동 상태 | 미연동, 연동 중, 연동 완료, 실패',
	`eco_linked_at`	DATETIME	NULL	COMMENT '에코마일리지 연동 일시 | 기준 사용량 조회일, WF-03에 표기',
	`eco_sido_code`	VARCHAR(10)	NULL	COMMENT '에코마일리지 시도 코드 | 누리집에 등록된 주소의 시도 코드, 프로필 주소와 달라지면 이사 안내를 띄운다',
	`eco_sigungu_code`	VARCHAR(10)	NULL	COMMENT '에코마일리지 시군구 코드 | 누리집에 등록된 주소의 시군구 코드',
	`eco_address_label`	VARCHAR(60)	NULL	COMMENT '에코마일리지 주소 표기 | 마이페이지에 그대로 노출할 문구, 예: 서울 관악구',
	`eco_address_registered_at`	DATE	NULL	COMMENT '에코마일리지 주소 등록 월 | 누리집에 주소가 등록된 시점의 첫째 날, 예: 2026-03-01',
	`greenlife_participating`	TINYINT(1)	NOT NULL	DEFAULT 0	COMMENT '녹색생활실천 참여 여부 | 0 미참여 시 BN-01 제도 소개 화면',
	`greenlife_linked_at`	DATETIME	NULL	COMMENT '녹색생활실천 연동 일시 | 최근 실적 연동에 성공한 시각',
	`pocket_account_no`	VARCHAR(30)	NOT NULL	COMMENT '그린포켓 계좌번호 | 두 제도의 수령 계좌로 쓰이는 서비스 부여 번호',
	`pocket_holder`	VARCHAR(30)	NOT NULL	COMMENT '그린포켓 예금주 | 그린포켓 계좌의 예금주명',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 사용자 데이터 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 사용자 데이터 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_app_user_demo_key` (`demo_key`),
	UNIQUE KEY `uq_app_user_pocket_account_no` (`pocket_account_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자';
CREATE TABLE `utility_monthly_record` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '생활요금 기록 ID | 월별 에너지원 기록 내부 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 기록을 소유한 사용자',
	`record_source`	ENUM('BILL', 'ECO_BASELINE')	NOT NULL	DEFAULT 'BILL'	COMMENT '기록 출처 | BILL 사용자 등록 고지서, ECO_BASELINE 에코마일리지 연동 직전 2년 사용량',
	`billing_month`	DATE	NOT NULL	COMMENT '청구 월 | 청구 대상 월의 첫째 날로 저장',
	`utility_type`	ENUM('ELECTRICITY', 'GAS', 'WATER')	NOT NULL	COMMENT '에너지원 유형 | 전기, 도시가스, 수도',
	`bill_type`	ENUM('MANAGEMENT', 'ELECTRICITY', 'GAS', 'WATER')	NULL	COMMENT '고지서 유형 | 관리비 통합, 전기, 도시가스, 수도. ECO_BASELINE은 NULL',
	`amount`	BIGINT	NOT NULL	DEFAULT 0	COMMENT '청구 금액 | 해당 에너지원의 청구 금액(원)',
	`usage_value`	DECIMAL(12, 3)	NOT NULL	DEFAULT 0	COMMENT '사용량 | 해당 월 사용량, 월 감축률 계산에 필수',
	`usage_unit`	ENUM('kWh', 'm3')	NOT NULL	COMMENT '사용량 단위 | 전기 kWh, 도시가스와 수도 m3',
	`input_source`	ENUM('OCR', 'MANUAL', 'ECO_LINK')	NOT NULL	COMMENT '입력 출처 | 사진 분석, 직접 입력, 에코마일리지 연동',
	`confidence`	DECIMAL(5, 4)	NULL	COMMENT 'OCR 신뢰도 | 0 이상 1 이하, 낮으면 확인 필요 배지. 직접 입력은 NULL',
	`record_status`	ENUM('CONFIRMED', 'REVIEW_REQUIRED')	NOT NULL	DEFAULT 'CONFIRMED'	COMMENT '기록 상태 | 등록 완료, 확인 대기',
	`registered_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '등록 일시 | 기록 최초 등록 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 기록 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_umr_user_source_month_utility` (`user_id`,`record_source`,`billing_month`,`utility_type`),
	KEY `ix_umr_user_month` (`user_id`,`billing_month`),
	CONSTRAINT `ck_umr_amount` CHECK (`amount` >= 0),
	CONSTRAINT `ck_umr_usage` CHECK (`usage_value` >= 0),
	CONSTRAINT `ck_umr_confidence` CHECK (`confidence` IS NULL OR (`confidence` >= 0 AND `confidence` <= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='월별 생활요금';
CREATE TABLE `region_utility_snapshot` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '지역 평균 ID | 지역별 에너지 평균 스냅샷 식별자',
	`region_level`	ENUM('SIGUNGU', 'SIDO')	NOT NULL	COMMENT '지역 수준 | 시군구 평균, 시도 평균. 표본 부족 시 시도로 대체하고 범위 배지 표시',
	`sido_code`	VARCHAR(10)	NOT NULL	COMMENT '시도 코드 | 평균 데이터의 시도 행정구역 코드',
	`sigungu_code`	VARCHAR(10)	NOT NULL	DEFAULT ''	COMMENT '시군구 코드 | 시군구 평균 코드, 시도 평균은 빈 문자열',
	`base_month`	DATE	NOT NULL	COMMENT '기준 월 | 평균 데이터 기준 월의 첫째 날',
	`utility_type`	ENUM('ELECTRICITY', 'GAS', 'WATER')	NOT NULL	COMMENT '에너지원 유형 | 전기, 도시가스, 수도. 현재 전기만 확보',
	`household_count`	BIGINT	NULL	COMMENT '표본 가구 수 | 지역 평균 계산에 포함된 가구 수',
	`avg_usage`	DECIMAL(12, 3)	NULL	COMMENT '평균 사용량 | 해당 지역 가구의 평균 에너지 사용량',
	`avg_amount`	BIGINT	NULL	COMMENT '평균 요금 | 해당 지역 가구의 평균 청구 금액(원)',
	`source_name`	VARCHAR(100)	NOT NULL	COMMENT '출처 기관명 | 지역 평균 데이터 제공 기관',
	`extracted_at`	DATETIME	NOT NULL	COMMENT '추출 일시 | 외부 데이터를 수집한 시각, 화면에 출처와 함께 표시',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 스냅샷 DB 저장 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_rus_region_month_utility` (`region_level`,`sido_code`,`sigungu_code`,`base_month`,`utility_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='지역 에너지 평균';
CREATE TABLE `eco_round` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '평가 회차 ID | 6개월 평가 회차 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 평가 회차를 소유한 사용자',
	`period_start`	DATE	NOT NULL	COMMENT '평가 시작 월 | 평가 기간 시작 월의 첫째 날, 데모는 2026-04-01',
	`period_end`	DATE	NOT NULL	COMMENT '평가 종료 월 | 평가 기간 종료 월의 첫째 날, 데모는 2026-09-01',
	`round_status`	ENUM('READY', 'GOAL_SET', 'IN_PROGRESS', 'CONFIRMED', 'CLOSED')	NOT NULL	DEFAULT 'READY'	COMMENT '평가 회차 상태 | 목표 미설정, 목표 설정, 진행 중, 확정, 종료',
	`application_status`	ENUM('NOT_APPLIED', 'APPLYING', 'APPLIED', 'FAILED')	NOT NULL	DEFAULT 'NOT_APPLIED'	COMMENT '참여신청 상태 | 미신청, 신청 중, 신청 완료, 실패. 신청해야 마일리지를 받는다',
	`baseline_total_amount`	BIGINT	NULL	COMMENT '기준 총요금 | 직전 2년 같은 기간 평균 생활요금 합계(원)',
	`baseline_total_carbon_g`	DECIMAL(18, 3)	NULL	COMMENT '기준 탄소량 | 등록 에너지 기준 사용량을 환산한 총 탄소량(gCO2e)',
	`baseline_queried_at`	DATETIME	NULL	COMMENT '기준 사용량 조회 일시 | 에코마일리지에서 기준값을 받아온 시각',
	`combined_target_rate`	DECIMAL(7, 3)	NULL	COMMENT '합산 목표 감축률 | 등록 에너지원을 탄소로 환산해 합산한 목표율(%)',
	`expected_mileage`	BIGINT	NOT NULL	DEFAULT 0	COMMENT '예상 마일리지 | 합산 목표율이 속한 구간의 지급 마일리지(M)',
	`expected_saving_amount`	BIGINT	NULL	COMMENT '예상 절감 금액 | 목표 달성 시 줄어드는 요금 합계(원)',
	`goal_set_at`	DATETIME	NULL	COMMENT '목표 저장 일시 | NULL이면 목표 미설정 상태(WF-03)',
	`cumulative_rate`	DECIMAL(7, 3)	NULL	COMMENT '누적 감축률 | 등록된 월까지의 누적 감축률(%)',
	`final_rate`	DECIMAL(7, 3)	NULL	COMMENT '최종 감축률 | 평가 종료 후 확정된 합산 감축률(%)',
	`actual_total_amount`	BIGINT	NULL	COMMENT '실제 총요금 | 평가 기간에 실제로 청구된 생활요금 합계(원)',
	`saved_amount`	BIGINT	NULL	COMMENT '절감 금액 | 기준 총요금에서 실제 총요금을 뺀 금액(원), 성과 표시 전용이며 적립 대상 아님',
	`confirmed_mileage`	BIGINT	NOT NULL	DEFAULT 0	COMMENT '확정 마일리지 | 평가 결과로 확정된 마일리지(M)',
	`confirmed_at`	DATETIME	NULL	COMMENT '평가 확정 일시 | 에코마일리지 누리집 기준 확정 시각',
	`result_viewed_at`	DATETIME	NULL	COMMENT '결과 확인 일시 | 결산 알림 모달을 최초로 확인한 시각, 재노출 방지',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 평가 회차 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 평가 회차 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_eco_round_user_period` (`user_id`,`period_start`),
	CONSTRAINT `ck_eco_round_period` CHECK (`period_end` > `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='평가 회차';
CREATE TABLE `eco_round_utility` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '회차 에너지원 ID | 평가 회차 에너지원별 행 식별자',
	`eco_round_id`	BIGINT	NOT NULL	COMMENT '평가 회차 ID | 이 행이 속한 평가 회차',
	`utility_type`	ENUM('ELECTRICITY', 'GAS', 'WATER')	NOT NULL	COMMENT '에너지원 유형 | 전기, 도시가스, 수도',
	`is_registered`	TINYINT(1)	NOT NULL	DEFAULT 1	COMMENT '에코마일리지 등록 여부 | 0이면 목표를 세울 수 없고 평가에서 제외',
	`unregistered_reason`	VARCHAR(200)	NULL	COMMENT '미등록 사유 | 세대 명의 계약이 없어 사용량을 불러올 수 없음 등',
	`carbon_factor_g`	DECIMAL(10, 3)	NOT NULL	COMMENT '탄소 환산계수 | 단위당 배출량(gCO2e). 전기 424, 수도 332, 도시가스 2240',
	`baseline_amount`	BIGINT	NULL	COMMENT '기준 요금 | 직전 2년 같은 기간 평균 요금(원)',
	`baseline_usage`	DECIMAL(12, 3)	NULL	COMMENT '기준 사용량 | 직전 2년 같은 기간 평균 사용량',
	`baseline_share_rate`	DECIMAL(7, 3)	NULL	COMMENT '기준 요금 비중 | 기준 총요금 대비 이 에너지원의 비중(%), 합이 100',
	`target_tier`	ENUM('TIER_5', 'TIER_10', 'TIER_15')	NULL	COMMENT '목표 구간 | 5~10%, 10~15%, 15% 이상. 구간 하한이 목표율',
	`target_rate`	DECIMAL(7, 3)	NULL	COMMENT '목표 절감률 | 선택 구간의 하한(%)',
	`target_usage`	DECIMAL(12, 3)	NULL	COMMENT '목표 사용량 | 기준 사용량 곱하기 (1 빼기 목표율 나누기 100)',
	`expected_saving_amount`	BIGINT	NULL	COMMENT '예상 절감액 | 기준 요금 곱하기 목표율 나누기 100(원)',
	`final_rate`	DECIMAL(7, 3)	NULL	COMMENT '최종 절감률 | 평가 확정 시 이 에너지원의 실제 절감률(%)',
	`actual_usage`	DECIMAL(12, 3)	NULL	COMMENT '실제 사용량 | 평가 기간 실제 사용량 합계',
	`is_achieved`	TINYINT(1)	NULL	COMMENT '목표 달성 여부 | 미달도 줄인 만큼 합산에는 포함된다',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_eru_round_utility` (`eco_round_id`,`utility_type`),
	CONSTRAINT `ck_eru_factor` CHECK (`carbon_factor_g` > 0),
	CONSTRAINT `ck_eru_target_rate` CHECK (`target_rate` IS NULL OR (`target_rate` >= 0 AND `target_rate` <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회차 에너지원';
CREATE TABLE `eco_monthly_report` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '월 리포트 ID | 월별 감축률 리포트 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 리포트를 소유한 사용자',
	`eco_round_id`	BIGINT	NOT NULL	COMMENT '평가 회차 ID | 이 달이 속한 평가 회차',
	`report_month`	DATE	NOT NULL	COMMENT '대상 월 | 리포트 대상 월의 첫째 날',
	`monthly_rate`	DECIMAL(7, 3)	NOT NULL	COMMENT '월 감축률 | 직전 2년 같은 달 평균 대비 감축률(%), 음수는 증가',
	`cumulative_rate`	DECIMAL(7, 3)	NOT NULL	COMMENT '누적 감축률 | 평가 기간 등록 월까지의 누적 감축률(%)',
	`target_rate`	DECIMAL(7, 3)	NULL	COMMENT '목표 감축률 | 판정 시점의 합산 목표율 스냅샷(%)',
	`required_rate`	DECIMAL(7, 3)	NULL	COMMENT '필요 월 감축률 | 남은 기간에 필요한 월 감축률(%), 남은 개월 0이면 NULL',
	`remaining_months`	TINYINT	NOT NULL	DEFAULT 0	COMMENT '남은 개월 | 0이면 역산을 생략해 0 나눗셈을 막는다',
	`is_achieved`	TINYINT(1)	NOT NULL	DEFAULT 0	COMMENT '목표 달성 여부 | 해당 월이 목표율을 넘겼는지',
	`by_utility`	JSON	NULL	COMMENT '에너지원별 결과 | 사용량, 기준 평균, 감축률, 탄소 비중의 JSON 배열, 표시 전용',
	`calculated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '계산 일시 | 리포트가 계산된 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_emr_user_month` (`user_id`,`report_month`),
	KEY `ix_emr_round_month` (`eco_round_id`,`report_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='월 감축률 리포트';
CREATE TABLE `mission_catalog` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '미션 ID | 절감 실천 미션 내부 식별자',
	`mission_code`	VARCHAR(50)	NOT NULL	COMMENT '미션 코드 | 시드 재적재와 코드 참조에 쓰는 고유 영문 코드',
	`utility_type`	ENUM('ELECTRICITY', 'GAS', 'WATER')	NOT NULL	COMMENT '에너지원 유형 | 이 미션이 줄이는 에너지원',
	`title`	VARCHAR(100)	NOT NULL	COMMENT '미션 제목 | 화면에 표시할 절감 행동 제목',
	`description`	VARCHAR(300)	NOT NULL	COMMENT '미션 설명 | 절감 행동에 대한 한 줄 안내',
	`difficulty`	ENUM('EASY', 'NORMAL', 'HARD')	NOT NULL	COMMENT '난이도 | 쉬움, 보통, 어려움',
	`evidence_amount`	DECIMAL(12, 3)	NOT NULL	COMMENT '출처 절감량 | 공식 출처가 제시한 에너지 절감량',
	`evidence_unit`	ENUM('kWh', 'm3')	NOT NULL	COMMENT '출처 절감량 단위 | kWh 또는 m3',
	`evidence_text`	VARCHAR(300)	NOT NULL	COMMENT '출처 수치 설명 | 화면에 노출할 공식 절감 수치 문구',
	`calculation_basis`	TEXT	NOT NULL	COMMENT '산출 근거 | 출처 수치를 우리 집 사용량으로 환산한 계산 설명',
	`source_org`	VARCHAR(100)	NOT NULL	COMMENT '출처 기관 | 한국에너지공단, 환경부, 서울시 아리수 등',
	`device_group`	VARCHAR(30)	NOT NULL	COMMENT '기기 그룹 | 냉방, 세탁, 샤워 등. 합산 시 그룹당 하나만 포함',
	`season_tags`	SET('SPRING', 'SUMMER', 'AUTUMN', 'WINTER')	NOT NULL	DEFAULT 'SPRING,SUMMER,AUTUMN,WINTER'	COMMENT '계절 태그 | 오늘의 실천에 노출할 계절',
	`rate_cap`	DECIMAL(7, 3)	NULL	COMMENT '절감률 상한 | 미션 하나에 적용할 최대 절감률(%), 전기 30 수도 20',
	`display_order`	INT	NOT NULL	DEFAULT 0	COMMENT '표시 순서 | 에너지원별 미션 목록 정렬 순서',
	`is_active`	TINYINT(1)	NOT NULL	DEFAULT 1	COMMENT '사용 여부 | 0 비활성, 1 활성',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 미션 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 미션 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_mission_code` (`mission_code`),
	KEY `ix_mission_utility_order` (`utility_type`,`display_order`),
	CONSTRAINT `ck_mission_evidence` CHECK (`evidence_amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='절감 미션';
CREATE TABLE `user_mission` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '선택 미션 ID | 회차별 미션 선택 행 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 미션을 선택한 사용자',
	`eco_round_id`	BIGINT	NOT NULL	COMMENT '평가 회차 ID | 미션 선택이 속한 평가 회차',
	`mission_id`	BIGINT	NOT NULL	COMMENT '미션 ID | 선택한 절감 미션',
	`computed_rate`	DECIMAL(7, 3)	NOT NULL	COMMENT '환산 절감률 | 출처 절감량 나누기 (기준 사용량 나누기 6), 선택 시점 값',
	`is_counted`	TINYINT(1)	NOT NULL	DEFAULT 1	COMMENT '합계 포함 여부 | 같은 기기 그룹은 가장 큰 하나만 1',
	`exclusion_reason`	VARCHAR(100)	NULL	COMMENT '합계 제외 사유 | 냉방 겹침 등 화면에 표시할 태그 문구',
	`selected_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '선택 일시 | 목표와 함께 저장된 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_user_mission_round_mission` (`eco_round_id`,`mission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='선택 미션';
CREATE TABLE `mission_daily_log` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '일별 실천 기록 ID | 사용자 날짜별 체크 기록 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 미션을 수행한 사용자',
	`eco_round_id`	BIGINT	NOT NULL	COMMENT '평가 회차 ID | 체크가 속한 평가 회차',
	`log_date`	DATE	NOT NULL	COMMENT '실천 날짜 | 미션 완료를 체크한 날짜',
	`completed_mission_ids`	JSON	NOT NULL	COMMENT '완료 미션 ID 목록 | 그 날 완료로 체크한 미션 ID 배열',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 일별 기록 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 일별 기록 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_mdl_user_round_date` (`user_id`,`eco_round_id`,`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='일별 실천 기록';
CREATE TABLE `greenlife_item` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '실천 항목 ID | 녹색생활실천 항목 내부 식별자',
	`item_code`	VARCHAR(50)	NOT NULL	COMMENT '실천 항목 코드 | 항목을 구분하는 고유 영문 코드',
	`name`	VARCHAR(100)	NOT NULL	COMMENT '실천 항목명 | 전자영수증 발급, 텀블러 다회용컵 이용 등 공식 항목명',
	`unit_price`	BIGINT	NOT NULL	DEFAULT 0	COMMENT '단위 적립액 | 건, 개, 회당 적립 금액(원)',
	`reward_unit`	VARCHAR(20)	NOT NULL	COMMENT '적립 단위 | 건, 개, 회, km, kg 등',
	`monthly_cap_amount`	BIGINT	NULL	COMMENT '월 적립 상한 | 항목별 월 최대 적립 금액(원), 없으면 NULL',
	`annual_cap_amount`	BIGINT	NULL	COMMENT '연 적립 상한 | 항목별 연 최대 적립 금액(원), 없으면 NULL',
	`practice_steps`	JSON	NOT NULL	COMMENT '실천 방법 | 이렇게 실천해요 3단계 문자열 배열',
	`icon_key`	VARCHAR(50)	NULL	COMMENT '아이콘 키 | 프론트엔드에서 쓸 항목별 아이콘 식별자',
	`standard_year`	SMALLINT	NOT NULL	COMMENT '적립 기준 연도 | 단가와 조건이 적용되는 공식 기준 연도',
	`external_url`	VARCHAR(500)	NULL	COMMENT '외부 페이지 URL | 참여기업 확인 또는 공식 안내 페이지',
	`display_order`	INT	NOT NULL	DEFAULT 0	COMMENT '표시 순서 | 17개 항목의 고정 정렬 순서, 실적이 없어도 목록 유지',
	`is_active`	TINYINT(1)	NOT NULL	DEFAULT 1	COMMENT '사용 여부 | 0 비활성, 1 활성',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 항목 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 항목 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_greenlife_item_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='녹색생활실천 항목';
CREATE TABLE `greenlife_activity` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '실천 실적 ID | 사용자 실천 건별 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 실적을 소유한 사용자',
	`item_id`	BIGINT	NOT NULL	COMMENT '실천 항목 ID | 실적에 해당하는 녹색생활실천 항목',
	`source_event_key`	VARCHAR(100)	NOT NULL	COMMENT '외부 실적 키 | 연동 원천의 고유 식별값, 재연동 시 중복 적재를 막는다',
	`activity_month`	DATE	NOT NULL	COMMENT '실천 월 | 월별 집계를 위한 실천 월의 첫째 날',
	`occurred_at`	DATETIME	NOT NULL	COMMENT '실천 일시 | 친환경 활동이 실제 발생한 시각',
	`quantity`	DECIMAL(12, 3)	NOT NULL	DEFAULT 1	COMMENT '실천 수량 | 건수, 횟수, 거리 또는 무게',
	`reward_amount`	BIGINT	NOT NULL	DEFAULT 0	COMMENT '적립 금액 | 상한을 적용한 해당 실적의 적립 금액(원)',
	`reward_status`	ENUM('PENDING', 'PAID')	NOT NULL	DEFAULT 'PENDING'	COMMENT '적립 상태 | 적립 예정, 지급 완료. 지급 완료만 포켓 입금 대상',
	`pending_at`	DATETIME	NOT NULL	COMMENT '적립 예정 일시 | 실적이 적립 예정으로 등록된 시각',
	`paid_at`	DATETIME	NULL	COMMENT '지급 완료 일시 | 포인트 지급이 확정된 시각',
	`item_name_snapshot`	VARCHAR(100)	NOT NULL	COMMENT '실천 항목명 스냅샷 | 실적 발생 당시의 항목명',
	`unit_price_snapshot`	BIGINT	NOT NULL	COMMENT '단위 적립액 스냅샷 | 실적 발생 당시의 단위 적립 금액(원)',
	`synced_at`	DATETIME	NOT NULL	COMMENT '연동 일시 | 이 실적이 앱에 최근 반영된 시각',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 실적 DB 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 실적 상태 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_greenlife_activity_event` (`source_event_key`),
	KEY `ix_greenlife_activity_user_month` (`user_id`,`activity_month`),
	CONSTRAINT `ck_greenlife_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='녹색생활실천 실적';
CREATE TABLE `pocket_transaction` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '거래 ID | 입금과 출금 거래의 내부 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 거래를 소유한 사용자',
	`eco_round_id`	BIGINT	NULL	COMMENT '평가 회차 ID | 에코마일리지 전환 입금일 때 대상 평가 회차',
	`withdrawal_account_id`	BIGINT	NULL	COMMENT '출금 계좌 ID | 출금 거래에 사용한 외부 은행계좌',
	`transaction_code`	VARCHAR(30)	NOT NULL	COMMENT '거래 코드 | GP-YYMM-NNNN 형식의 사용자 노출 거래번호',
	`direction`	ENUM('CREDIT', 'DEBIT')	NOT NULL	COMMENT '거래 방향 | 입금, 출금',
	`transaction_type`	ENUM('ECO_MILEAGE', 'GREENLIFE', 'WITHDRAWAL')	NOT NULL	COMMENT '거래 유형 | 에코마일리지 전환금, 녹색생활실천 수령액, 출금',
	`amount`	BIGINT	NOT NULL	COMMENT '거래 금액 | 입금 또는 출금 금액(원)',
	`transaction_status`	ENUM('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED')	NOT NULL	DEFAULT 'REQUESTED'	COMMENT '거래 상태 | 요청, 처리 중, 완료, 실패. 잔액에는 완료만 반영',
	`source_type`	ENUM('ECO_ROUND', 'GREENLIFE_MONTH', 'WITHDRAWAL')	NOT NULL	COMMENT '거래 원천 유형 | 평가 회차, 녹색생활실천 월, 출금',
	`source_key`	VARCHAR(100)	NOT NULL	COMMENT '거래 원천 키 | 같은 원천의 중복 거래를 막는 고유 식별값',
	`idempotency_key`	VARCHAR(100)	NULL	COMMENT '멱등 키 | 출금 버튼 중복 요청을 막는 요청 고유 키',
	`label`	VARCHAR(60)	NOT NULL	COMMENT '거래 표시명 | 적립 내역에 보여줄 출처명',
	`account_snapshot`	JSON	NULL	COMMENT '출금 계좌 스냅샷 | 출금 당시의 은행명, 계좌번호 표시값, 예금주',
	`requested_at`	DATETIME	NOT NULL	COMMENT '요청 일시 | 입금 또는 출금 처리를 요청한 시각',
	`expected_date`	DATE	NULL	COMMENT '입금 예정일 | 출금 신청 후 영업일 기준 1~2일 내 예정일',
	`completed_at`	DATETIME	NULL	COMMENT '완료 일시 | 거래가 최종 완료된 시각',
	`failure_reason`	VARCHAR(300)	NULL	COMMENT '실패 사유 | 실패 상태의 사용자 안내와 재시도 근거',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 거래 원장 생성 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 거래 상태 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_pocket_tx_source` (`source_type`,`source_key`),
	UNIQUE KEY `uq_pocket_tx_idempotency` (`idempotency_key`),
	UNIQUE KEY `uq_pocket_tx_code` (`transaction_code`),
	KEY `ix_pocket_tx_user_status` (`user_id`,`transaction_status`,`completed_at`),
	CONSTRAINT `ck_pocket_tx_amount` CHECK (`amount` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='그린포켓 거래 원장';
CREATE TABLE `withdrawal_account` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT	COMMENT '출금 계좌 ID | 외부 은행계좌 내부 식별자',
	`user_id`	BIGINT	NOT NULL	COMMENT '사용자 ID | 출금 계좌를 소유한 사용자',
	`bank_code`	VARCHAR(10)	NOT NULL	COMMENT '은행 코드 | 출금 계좌 금융기관 코드',
	`bank_name`	VARCHAR(30)	NOT NULL	COMMENT '은행명 | 화면에 표시할 금융기관명',
	`account_no_encrypted`	VARBINARY(512)	NOT NULL	COMMENT '암호화 계좌번호 | AES로 암호화해 저장, 평문은 로그에 남기지 않는다',
	`holder`	VARCHAR(30)	NOT NULL	COMMENT '예금주 | 외부 출금 계좌 예금주명',
	`is_default`	TINYINT(1)	NOT NULL	DEFAULT 0	COMMENT '기본 계좌 여부 | 사용자당 1건만 허용',
	`is_active`	TINYINT(1)	NOT NULL	DEFAULT 1	COMMENT '사용 여부 | 0 비활성, 1 사용 가능',
	`verified_at`	DATETIME	NULL	COMMENT '본인확인 일시 | 계좌 본인확인 완료 시각, MVP에서는 NULL',
	`default_slot`	BIGINT	GENERATED ALWAYS AS (IF(`is_default` = 1, `user_id`, NULL)) STORED	COMMENT '기본 계좌 슬롯 | 사용자당 기본 계좌 1건을 DB가 강제하기 위한 생성 컬럼',
	`created_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '생성 일시 | 출금 계좌 등록 시각',
	`updated_at`	TIMESTAMP	NOT NULL	DEFAULT CURRENT_TIMESTAMP	COMMENT '수정 일시 | 출금 계좌 최종 수정 시각',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_withdrawal_account_default` (`default_slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='출금 계좌';

-- ── 외래키 16 ────────────────────────────────────────────────
ALTER TABLE `utility_monthly_record` ADD CONSTRAINT `fk_umr_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `eco_round` ADD CONSTRAINT `fk_eco_round_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `eco_round_utility` ADD CONSTRAINT `fk_eru_round` FOREIGN KEY (`eco_round_id`) REFERENCES `eco_round` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `eco_monthly_report` ADD CONSTRAINT `fk_emr_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `eco_monthly_report` ADD CONSTRAINT `fk_emr_round` FOREIGN KEY (`eco_round_id`) REFERENCES `eco_round` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `user_mission` ADD CONSTRAINT `fk_um_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `user_mission` ADD CONSTRAINT `fk_um_round` FOREIGN KEY (`eco_round_id`) REFERENCES `eco_round` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `user_mission` ADD CONSTRAINT `fk_um_mission` FOREIGN KEY (`mission_id`) REFERENCES `mission_catalog` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `mission_daily_log` ADD CONSTRAINT `fk_mdl_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `mission_daily_log` ADD CONSTRAINT `fk_mdl_round` FOREIGN KEY (`eco_round_id`) REFERENCES `eco_round` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `greenlife_activity` ADD CONSTRAINT `fk_ga_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `greenlife_activity` ADD CONSTRAINT `fk_ga_item` FOREIGN KEY (`item_id`) REFERENCES `greenlife_item` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `withdrawal_account` ADD CONSTRAINT `fk_wa_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `pocket_transaction` ADD CONSTRAINT `fk_pt_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `pocket_transaction` ADD CONSTRAINT `fk_pt_round` FOREIGN KEY (`eco_round_id`) REFERENCES `eco_round` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE `pocket_transaction` ADD CONSTRAINT `fk_pt_account` FOREIGN KEY (`withdrawal_account_id`) REFERENCES `withdrawal_account` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT;

-- ── 참고 ─────────────────────────────────────────────────────
--  ON UPDATE 는 전부 RESTRICT 입니다. id 는 변하지 않으므로 무해하고,
--  withdrawal_account 는 STORED 생성 컬럼(default_slot)이 user_id 를 참조해
--  ON UPDATE CASCADE 를 쓰면 MySQL ERROR 1901 이 납니다.
--
--  데모 초기화(COM-10) — 아래 한 줄이면 CASCADE 로 사용자 데이터가 전부 정리되고
--  마스터(mission_catalog · greenlife_item · region_utility_snapshot)만 남습니다.
--    DELETE FROM app_user WHERE id = :uid;

-- ── DB가 강제하는 규칙 (MariaDB 10.11 에 올려 실제 차단 확인, 2026-09-03) ──
--  ① 같은 달 같은 항목 고지서 중복   ERROR 1062  uq_umr_user_source_month_utility   결정 A-3
--  ② 상태값 오타 'LINKD'             ERROR 1265  ENUM
--  ③ 음수 거래 금액                  ERROR 4025  ck_pocket_tx_amount
--  ④ 마일리지 전환 회차당 2회        ERROR 1062  uq_pocket_tx_source                D-2-02
--  ⑤ 녹색생활실천 월 정산 2회        ERROR 1062  uq_pocket_tx_source                C-2-06
--  ⑥ 출금 중복 탭                    ERROR 1062  uq_pocket_tx_idempotency           D-3-03
--  ⑦ 기본 출금계좌 2건               ERROR 1062  uq_withdrawal_account_default      D-3-01
--  ⑧ 근거 없는 미션 저장             ERROR 1364  evidence_amount NOT NULL           B-3-01
--  ⑨ 실적 중복 적재                  ERROR 1062  uq_greenlife_activity_event        C-1-02
--  ⑩ 없는 사용자 참조                ERROR 1452  fk_umr_user
--  ⑪ OCR 신뢰도 1.5                  ERROR 4025  ck_umr_confidence
--  ⑫ 회차 기간 역전                  ERROR 4025  ck_eco_round_period
--  ⑬ DELETE FROM app_user            사용자 데이터 8개 테이블 전부 0건, 마스터 3개 유지
--  ⑭ 마스터(mission_catalog) 삭제     ERROR 1451  fk_um_mission RESTRICT 로 보호
--
--  검산 — 합산 목표 감축률 (B-2-07)
--    SELECT ROUND((SUM(baseline_usage*carbon_factor_g) - SUM(target_usage*carbon_factor_g))
--                 / SUM(baseline_usage*carbon_factor_g) * 100, 3)
--    FROM eco_round_utility WHERE eco_round_id = :rid AND is_registered = 1;
--    시드(1,340kWh·108㎥·66㎥ / 목표 10·15·5%) → 11.322 %  (기준 831,992 → 목표 737,792.4 gCO2e)
