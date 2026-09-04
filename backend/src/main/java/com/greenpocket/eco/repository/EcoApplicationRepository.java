package com.greenpocket.eco.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.ApplicationStatus;

@Repository
@RequiredArgsConstructor
public class EcoApplicationRepository {

	private final JdbcClient jdbcClient;

	public Optional<ApplicationSnapshot> findByUserIdAndRoundId(Long userId, Long roundId) {
		return jdbcClient.sql("""
				SELECT id, application_status, updated_at
				FROM eco_round
				WHERE id = :roundId AND user_id = :userId
				""")
			.param("roundId", roundId)
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ApplicationSnapshot(
				resultSet.getLong("id"),
				ApplicationStatus.valueOf(resultSet.getString("application_status")),
				resultSet.getTimestamp("updated_at").toLocalDateTime()
			))
			.optional();
	}

	public boolean markApplied(Long userId, Long roundId, LocalDateTime appliedAt) {
		return jdbcClient.sql("""
				UPDATE eco_round
				SET application_status = 'APPLIED', updated_at = :appliedAt
				WHERE id = :roundId
				  AND user_id = :userId
				  AND application_status <> 'APPLIED'
				""")
			.param("appliedAt", Timestamp.valueOf(appliedAt))
			.param("roundId", roundId)
			.param("userId", userId)
			.update() == 1;
	}

	public record ApplicationSnapshot(
		Long roundId,
		ApplicationStatus applicationStatus,
		LocalDateTime appliedAt
	) {
	}
}
