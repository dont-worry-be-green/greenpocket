package com.greenpocket.eco.repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EcoReportQueryRepository {

	private final JdbcClient jdbcClient;

	public List<EcoMonthlyReportSnapshot> findMonthlyReports(Long userId) {
		return jdbcClient.sql("""
				SELECT report_month, calculated_at
				FROM eco_monthly_report
				WHERE user_id = :userId
				ORDER BY report_month DESC
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new EcoMonthlyReportSnapshot(
				toLocalDate(resultSet.getDate("report_month")),
				toLocalDateTime(resultSet.getTimestamp("calculated_at"))
			))
			.list();
	}

	public List<EcoResultReportSnapshot> findResultReports(Long userId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, confirmed_at
				FROM eco_round
				WHERE user_id = :userId
				  AND round_status IN ('CONFIRMED', 'CLOSED')
				  AND confirmed_at IS NOT NULL
				ORDER BY period_end DESC, id DESC
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new EcoResultReportSnapshot(
				resultSet.getLong("id"),
				toLocalDate(resultSet.getDate("period_start")),
				toLocalDate(resultSet.getDate("period_end")),
				toLocalDateTime(resultSet.getTimestamp("confirmed_at"))
			))
			.list();
	}

	private static LocalDate toLocalDate(Date value) {
		return value.toLocalDate();
	}

	private static LocalDateTime toLocalDateTime(Timestamp value) {
		return value.toLocalDateTime();
	}

	public record EcoMonthlyReportSnapshot(
		LocalDate reportMonth,
		LocalDateTime calculatedAt
	) {
	}

	public record EcoResultReportSnapshot(
		Long roundId,
		LocalDate periodStart,
		LocalDate periodEnd,
		LocalDateTime confirmedAt
	) {
	}
}
