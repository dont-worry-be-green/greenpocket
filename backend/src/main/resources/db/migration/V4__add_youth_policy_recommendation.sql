-- 온통청년 정책 동기화 및 맞춤 추천 기반 (C-22~C-25)

ALTER TABLE `app_user`
	ADD COLUMN `birth_date` DATE NULL COMMENT '생년월일 | 정책 지원 연령 계산 기준, 미래 날짜 불가' AFTER `area_band`,
	ADD COLUMN `current_status` ENUM('EMPLOYED', 'SELF_EMPLOYED', 'UNEMPLOYED', 'FREELANCER', 'STUDENT', 'PREPARING_STARTUP', 'OTHER') NULL COMMENT '현재 상태 | 정책 추천용' AFTER `birth_date`,
	ADD COLUMN `annual_income_band` ENUM('NO_INCOME', 'UNDER_24M', 'FROM_24M_TO_36M', 'FROM_36M_TO_50M', 'OVER_50M', 'UNKNOWN') NULL COMMENT '연소득 구간 | 상세 소득은 수집하지 않음' AFTER `current_status`,
	ADD COLUMN `household_status` ENUM('ONE_PERSON', 'WITH_PARENTS', 'MARRIED', 'SINGLE_PARENT', 'OTHER') NULL COMMENT '가구 상태 | 정책 추천용' AFTER `annual_income_band`,
	ADD COLUMN `policy_profile_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '정책 추천 프로필 완료 여부' AFTER `household_status`;

CREATE TABLE `user_policy_interest` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 관심 분야 ID',
	`user_id` BIGINT NOT NULL COMMENT '사용자 ID',
	`category` ENUM('JOB', 'HOUSING', 'EDUCATION', 'WELFARE_CULTURE', 'PARTICIPATION_RIGHTS') NOT NULL COMMENT '정책 관심 분야 | 사용자당 최대 3개는 애플리케이션 검증',
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_user_policy_interest` (`user_id`,`category`),
	CONSTRAINT `fk_user_policy_interest_user`
		FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
		ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자 청년정책 관심 분야';

CREATE TABLE `youth_policy` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '청년정책 내부 ID',
	`external_policy_id` VARCHAR(30) NOT NULL COMMENT '온통청년 정책번호 plcyNo',
	`title` VARCHAR(300) NOT NULL COMMENT '정책명 plcyNm',
	`keyword_name` VARCHAR(200) NULL COMMENT '정책 키워드 plcyKywdNm',
	`description` MEDIUMTEXT NULL COMMENT '정책 설명 plcyExplnCn',
	`large_category_name` VARCHAR(100) NULL COMMENT '대분류 lclsfNm',
	`medium_category_name` VARCHAR(100) NULL COMMENT '중분류 mclsfNm',
	`interest_category` ENUM('JOB', 'HOUSING', 'EDUCATION', 'WELFARE_CULTURE', 'PARTICIPATION_RIGHTS') NULL COMMENT '그린포켓 추천 분야로 정규화한 값',
	`support_content` MEDIUMTEXT NULL COMMENT '지원 내용 plcySprtCn',
	`supervising_org_name` VARCHAR(200) NULL COMMENT '주관 기관 sprvsnInstCdNm',
	`operating_org_name` VARCHAR(200) NULL COMMENT '운영 기관 operInstCdNm',
	`application_period_code` VARCHAR(20) NULL COMMENT '신청기간 구분 aplyPrdSeCd',
	`business_start_date` DATE NULL COMMENT '사업 시작일 bizPrdBgngYmd',
	`business_end_date` DATE NULL COMMENT '사업 종료일 bizPrdEndYmd',
	`application_date_text` VARCHAR(500) NULL COMMENT '별도 신청일 안내 aplyYmd',
	`application_method` MEDIUMTEXT NULL COMMENT '신청 방법 plcyAplyMthdCn',
	`application_url` VARCHAR(1000) NULL COMMENT '신청 URL aplyUrlAddr | http·https만 응답',
	`reference_url1` VARCHAR(1000) NULL COMMENT '참고 URL 1',
	`reference_url2` VARCHAR(1000) NULL COMMENT '참고 URL 2',
	`age_limit_yn` CHAR(1) NULL COMMENT '연령 제한 여부 sprtTrgtAgeLmtYn',
	`min_age` SMALLINT NULL COMMENT '최소 지원 연령 | 유효 숫자만 저장',
	`max_age` SMALLINT NULL COMMENT '최대 지원 연령 | 유효 숫자만 저장',
	`marriage_status_code` VARCHAR(20) NULL COMMENT '혼인 상태 코드 mrgSttsCd',
	`income_condition_code` VARCHAR(20) NULL COMMENT '소득 조건 코드 earnCndSeCd',
	`income_min_amount` BIGINT NULL COMMENT '최소 소득 | API의 유효한 구조화 값만 저장',
	`income_max_amount` BIGINT NULL COMMENT '최대 소득 | API의 유효한 구조화 값만 저장',
	`income_condition_text` TEXT NULL COMMENT '자유 텍스트 소득 조건 earnEtcCn',
	`additional_condition_text` MEDIUMTEXT NULL COMMENT '추가 신청 자격 addAplyQlfcCndCn',
	`participant_target_text` MEDIUMTEXT NULL COMMENT '참여 대상 ptcpPrpTrgtCn',
	`major_codes` VARCHAR(500) NULL COMMENT '전공 코드 plcyMajorCd 원문',
	`employment_codes` VARCHAR(500) NULL COMMENT '취업 상태 코드 jobCd 원문',
	`school_codes` VARCHAR(500) NULL COMMENT '학력 코드 schoolCd 원문',
	`special_codes` VARCHAR(500) NULL COMMENT '특화 대상 코드 sbizCd 원문',
	`application_status` ENUM('OPEN', 'UPCOMING', 'CLOSED', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN' COMMENT '신청 상태 | 기간과 기준일로 정규화',
	`is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '최신 동기화에서 존재하는 정책 여부',
	`source_registered_at` DATETIME NULL COMMENT '온통청년 최초 등록일 frstRegDt',
	`source_modified_at` DATETIME NULL COMMENT '온통청년 최종 수정일 lastMdfcnDt',
	`synced_at` DATETIME NOT NULL COMMENT '마지막 정상 동기화 시각',
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_youth_policy_external` (`external_policy_id`),
	KEY `ix_youth_policy_search` (`interest_category`,`application_status`,`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='온통청년 청년정책 캐시';

CREATE TABLE `youth_policy_region` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '정책 지역 ID',
	`youth_policy_id` BIGINT NOT NULL COMMENT '청년정책 내부 ID',
	`region_level` ENUM('NATIONAL', 'SIDO', 'SIGUNGU') NOT NULL COMMENT '적용 지역 수준',
	`region_code` VARCHAR(10) NOT NULL COMMENT '행정구역 코드 | 전국은 00000',
	`region_name` VARCHAR(100) NULL COMMENT '표시용 지역명',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_youth_policy_region` (`youth_policy_id`,`region_level`,`region_code`),
	KEY `ix_youth_policy_region_code` (`region_code`,`region_level`),
	CONSTRAINT `fk_youth_policy_region_policy`
		FOREIGN KEY (`youth_policy_id`) REFERENCES `youth_policy` (`id`)
		ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='청년정책 적용 지역';

CREATE TABLE `youth_policy_condition` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '정책 조건 ID',
	`youth_policy_id` BIGINT NOT NULL COMMENT '청년정책 내부 ID',
	`condition_type` ENUM('AGE', 'INCOME', 'EMPLOYMENT', 'EDUCATION', 'MAJOR', 'MARRIAGE', 'SPECIAL', 'OTHER') NOT NULL COMMENT '정책 조건 유형',
	`condition_code` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '온통청년 코드 | 자유 텍스트 조건은 빈 문자열',
	`condition_value` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '정규화 값 또는 원문 일부',
	`machine_readable` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '자동 판정 가능 여부',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_youth_policy_condition` (`youth_policy_id`,`condition_type`,`condition_code`,`condition_value`),
	CONSTRAINT `fk_youth_policy_condition_policy`
		FOREIGN KEY (`youth_policy_id`) REFERENCES `youth_policy` (`id`)
		ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='청년정책 정규화 조건';

CREATE TABLE `youth_policy_sync` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '정책 동기화 실행 ID',
	`status` ENUM('RUNNING', 'SUCCEEDED', 'FAILED') NOT NULL COMMENT '동기화 상태',
	`started_at` DATETIME NOT NULL COMMENT '동기화 시작 시각',
	`finished_at` DATETIME NULL COMMENT '동기화 종료 시각',
	`source_total_count` INT NOT NULL DEFAULT 0 COMMENT 'API가 알린 전체 정책 수',
	`fetched_count` INT NOT NULL DEFAULT 0 COMMENT '가져온 정책 수',
	`upserted_count` INT NOT NULL DEFAULT 0 COMMENT '저장·갱신한 정책 수',
	`last_page` INT NOT NULL DEFAULT 0 COMMENT '마지막 처리 페이지',
	`error_code` VARCHAR(50) NULL COMMENT '실패 코드 | 인증키 원문 금지',
	`error_message` VARCHAR(500) NULL COMMENT '실패 요약 | 응답 본문·인증키 원문 금지',
	PRIMARY KEY (`id`),
	KEY `ix_youth_policy_sync_status` (`status`,`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='온통청년 정책 동기화 이력';
