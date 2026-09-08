package com.greenpocket.auth.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.EcoLinkStatus;

@Repository
@RequiredArgsConstructor
public class AuthRepository {

	private final JdbcClient jdbcClient;

	public boolean existsByEmail(String email) {
		return jdbcClient.sql("SELECT COUNT(*) FROM auth_account WHERE email = :email")
			.param("email", email)
			.query(Integer.class)
			.single() > 0;
	}

	public void createAccount(Long userId, String email, String passwordHash) {
		jdbcClient.sql("""
				INSERT INTO auth_account (user_id, email, password_hash)
				VALUES (:userId, :email, :passwordHash)
				""")
			.param("userId", userId)
			.param("email", email)
			.param("passwordHash", passwordHash)
			.update();
	}

	public Optional<AuthAccountSnapshot> findAccountByEmail(String email) {
		return jdbcClient.sql("""
				SELECT a.user_id, a.email, a.password_hash, u.name,
				       u.onboarding_completed, u.eco_link_status
				FROM auth_account a
				JOIN app_user u ON u.id = a.user_id
				WHERE a.email = :email
				""")
			.param("email", email)
			.query((resultSet, rowNum) -> new AuthAccountSnapshot(
				resultSet.getLong("user_id"),
				resultSet.getString("email"),
				resultSet.getString("password_hash"),
				resultSet.getString("name"),
				resultSet.getBoolean("onboarding_completed"),
				EcoLinkStatus.valueOf(resultSet.getString("eco_link_status"))
			))
			.optional();
	}

	public void updateLastLoginAt(Long userId, LocalDateTime now) {
		jdbcClient.sql("UPDATE auth_account SET last_login_at = :now WHERE user_id = :userId")
			.param("now", now)
			.param("userId", userId)
			.update();
	}

	public void saveRefreshToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
		jdbcClient.sql("""
				INSERT INTO auth_refresh_token (user_id, token_hash, expires_at)
				VALUES (:userId, :tokenHash, :expiresAt)
				""")
			.param("userId", userId)
			.param("tokenHash", tokenHash)
			.param("expiresAt", expiresAt)
			.update();
	}

	public Optional<RefreshTokenSnapshot> findRefreshTokenByHash(String tokenHash) {
		return jdbcClient.sql("""
				SELECT id, user_id, expires_at, revoked_at
				FROM auth_refresh_token
				WHERE token_hash = :tokenHash
				FOR UPDATE
				""")
			.param("tokenHash", tokenHash)
			.query((resultSet, rowNum) -> new RefreshTokenSnapshot(
				resultSet.getLong("id"),
				resultSet.getLong("user_id"),
				toLocalDateTime(resultSet.getTimestamp("expires_at")),
				toLocalDateTime(resultSet.getTimestamp("revoked_at"))
			))
			.optional();
	}

	public int revokeRefreshToken(Long tokenId, LocalDateTime now) {
		return jdbcClient.sql("""
				UPDATE auth_refresh_token
				SET revoked_at = :now
				WHERE id = :tokenId AND revoked_at IS NULL
				""")
			.param("now", now)
			.param("tokenId", tokenId)
			.update();
	}

	public void revokeAllActiveRefreshTokens(Long userId, LocalDateTime now) {
		jdbcClient.sql("""
				UPDATE auth_refresh_token
				SET revoked_at = :now
				WHERE user_id = :userId AND revoked_at IS NULL
				""")
			.param("now", now)
			.param("userId", userId)
			.update();
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	public record AuthAccountSnapshot(
		Long userId,
		String email,
		String passwordHash,
		String name,
		boolean onboardingCompleted,
		EcoLinkStatus ecoLinkStatus
	) {
	}

	public record RefreshTokenSnapshot(
		Long id,
		Long userId,
		LocalDateTime expiresAt,
		LocalDateTime revokedAt
	) {
	}
}
