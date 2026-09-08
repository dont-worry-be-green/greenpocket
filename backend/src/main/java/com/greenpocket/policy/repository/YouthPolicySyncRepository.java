package com.greenpocket.policy.repository;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class YouthPolicySyncRepository {

	private final JdbcClient jdbcClient;

	@Transactional
	public Long start(LocalDateTime startedAt) {
		jdbcClient.sql("""
				INSERT INTO youth_policy_sync (status, started_at)
				VALUES ('RUNNING', :startedAt)
				""")
			.param("startedAt", startedAt)
			.update();
		return jdbcClient.sql("SELECT LAST_INSERT_ID()")
			.query(Long.class)
			.single();
	}

	public void succeed(
		Long syncId,
		LocalDateTime finishedAt,
		int sourceTotalCount,
		int fetchedCount,
		int upsertedCount,
		int lastPage
	) {
		jdbcClient.sql("""
				UPDATE youth_policy_sync
				SET status = 'SUCCEEDED',
				    finished_at = :finishedAt,
				    source_total_count = :sourceTotalCount,
				    fetched_count = :fetchedCount,
				    upserted_count = :upsertedCount,
				    last_page = :lastPage,
				    error_code = NULL,
				    error_message = NULL
				WHERE id = :syncId
				""")
			.param("finishedAt", finishedAt)
			.param("sourceTotalCount", sourceTotalCount)
			.param("fetchedCount", fetchedCount)
			.param("upsertedCount", upsertedCount)
			.param("lastPage", lastPage)
			.param("syncId", syncId)
			.update();
	}

	public void fail(Long syncId, LocalDateTime finishedAt, String errorCode, String errorMessage) {
		jdbcClient.sql("""
				UPDATE youth_policy_sync
				SET status = 'FAILED',
				    finished_at = :finishedAt,
				    error_code = :errorCode,
				    error_message = :errorMessage
				WHERE id = :syncId
				""")
			.param("finishedAt", finishedAt)
			.param("errorCode", errorCode)
			.param("errorMessage", errorMessage)
			.param("syncId", syncId)
			.update();
	}
}
