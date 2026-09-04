package com.greenpocket.bill.repository;

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
public class BillReportQueryRepository {

	private final JdbcClient jdbcClient;

	public long countBills(Long userId) {
		return jdbcClient.sql("""
				SELECT COUNT(*)
				FROM utility_monthly_record
				WHERE user_id = :userId AND record_source = 'BILL'
				""")
			.param("userId", userId)
			.query(Long.class)
			.single();
	}

	public List<MonthlyDiagnosisReportSnapshot> findMonthlyDiagnosisReports(Long userId) {
		return jdbcClient.sql("""
				SELECT billing_month, MAX(registered_at) AS created_at
				FROM utility_monthly_record
				WHERE user_id = :userId AND record_source = 'BILL'
				GROUP BY billing_month
				ORDER BY billing_month DESC
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new MonthlyDiagnosisReportSnapshot(
				toLocalDate(resultSet.getDate("billing_month")),
				toLocalDateTime(resultSet.getTimestamp("created_at"))
			))
			.list();
	}

	private static LocalDate toLocalDate(Date value) {
		return value.toLocalDate();
	}

	private static LocalDateTime toLocalDateTime(Timestamp value) {
		return value.toLocalDateTime();
	}

	public record MonthlyDiagnosisReportSnapshot(
		LocalDate billingMonth,
		LocalDateTime createdAt
	) {
	}
}
