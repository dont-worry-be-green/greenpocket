ALTER TABLE `app_user`
	ADD COLUMN `gender` ENUM('MALE', 'FEMALE') NULL COMMENT '성별 | 가입 본인인증 입력, 일반 회원 필수' AFTER `birth_date`,
	ADD COLUMN `phone_number` VARCHAR(11) NULL COMMENT '휴대전화번호 | 숫자만 저장, 일반 회원 필수' AFTER `gender`,
	ADD UNIQUE KEY `uq_app_user_phone_number` (`phone_number`),
	MODIFY COLUMN `onboarding_completed` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '별도 온보딩 제거 | 항상 완료';

UPDATE `app_user`
SET `onboarding_completed` = 1
WHERE `onboarding_completed` = 0;
