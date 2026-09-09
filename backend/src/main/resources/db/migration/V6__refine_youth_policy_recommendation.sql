ALTER TABLE `app_user`
	ADD COLUMN `education_status` ENUM(
		'BELOW_HIGH_SCHOOL',
		'HIGH_SCHOOL_STUDENT',
		'HIGH_SCHOOL_EXPECTED_GRADUATION',
		'HIGH_SCHOOL_GRADUATE',
		'UNIVERSITY_STUDENT',
		'UNIVERSITY_EXPECTED_GRADUATION',
		'UNIVERSITY_GRADUATE',
		'GRADUATE_SCHOOL',
		'OTHER'
	) NULL COMMENT '학력 상태 | 정책 추천용' AFTER `annual_income_band`;

ALTER TABLE `youth_policy`
	ADD COLUMN `approval_status_code` VARCHAR(20) NULL COMMENT '정책 승인 상태 plcyAprvSttsCd' AFTER `operating_org_name`,
	ADD COLUMN `provision_method_code` VARCHAR(20) NULL COMMENT '정책 제공 방법 plcyPvsnMthdCd' AFTER `approval_status_code`,
	ADD COLUMN `application_start_date` DATE NULL COMMENT '해석한 신청 시작일 aplyYmd' AFTER `application_period_code`,
	ADD COLUMN `application_end_date` DATE NULL COMMENT '해석한 신청 종료일 aplyYmd' AFTER `application_start_date`;

ALTER TABLE `user_policy_interest`
	COMMENT = '사용자 청년정책 관심 분야 | 사용자당 최대 2개';

UPDATE `app_user`
SET `policy_profile_completed` = 0
WHERE `education_status` IS NULL
	OR NOT EXISTS (
		SELECT 1
		FROM `user_policy_interest` interest
		WHERE interest.user_id = app_user.id
	);
