-- JWT 회원 인증 기반 (COM-13~16, 결정 C-17)

ALTER TABLE `app_user`
	MODIFY COLUMN `demo_key` VARCHAR(50) NULL
	COMMENT '데모 사용자 키 | dev·demo 프로필에서만 사용하는 UUID v4, 일반 회원은 NULL';

CREATE TABLE `auth_account` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '인증 계정 ID',
	`user_id` BIGINT NOT NULL COMMENT '사용자 ID | app_user 1:1',
	`email` VARCHAR(255) NOT NULL COMMENT '로그인 이메일 | trim 후 소문자로 정규화',
	`password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt 비밀번호 해시 | 원문 저장 금지',
	`last_login_at` DATETIME NULL COMMENT '마지막 로그인 성공 일시',
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_auth_account_user` (`user_id`),
	UNIQUE KEY `uq_auth_account_email` (`email`),
	CONSTRAINT `fk_auth_account_user`
		FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
		ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='이메일·비밀번호 인증 계정';

CREATE TABLE `auth_refresh_token` (
	`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Refresh Token ID',
	`user_id` BIGINT NOT NULL COMMENT '사용자 ID | 토큰 소유자',
	`token_hash` CHAR(64) NOT NULL COMMENT 'Refresh Token SHA-256 hex | 원문 저장 금지',
	`expires_at` DATETIME NOT NULL COMMENT '만료 일시',
	`revoked_at` DATETIME NULL COMMENT '폐기 일시 | NULL이면 활성',
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
	`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uq_auth_refresh_token_hash` (`token_hash`),
	KEY `ix_auth_refresh_token_user_active` (`user_id`, `revoked_at`, `expires_at`),
	CONSTRAINT `fk_auth_refresh_token_user`
		FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
		ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Refresh Token 세션';
