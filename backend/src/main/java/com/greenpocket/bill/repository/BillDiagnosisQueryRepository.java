package com.greenpocket.bill.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class BillDiagnosisQueryRepository {

	private final JdbcClient jdbcClient;

	public List<MonthlyRecordSnapshot> findAllBillRecords(Long userId) {
		return queryRecords("""
				SELECT billing_month, utility_type, amount, usage_value, usage_unit
				FROM utility_monthly_record
				WHERE user_id = :userId AND record_source = 'BILL'
				ORDER BY billing_month DESC,
				         FIELD(utility_type, 'ELECTRICITY', 'GAS', 'WATER')
				""", userId);
	}

	public List<MonthlyRecordSnapshot> findBillRecords(Long userId, LocalDate from, LocalDate to) {
		return findRecords(userId, "BILL", from, to);
	}

	public List<MonthlyRecordSnapshot> findEcoBaselineRecords(Long userId, LocalDate month) {
		return findRecords(userId, "ECO_BASELINE", month, month);
	}

	private List<MonthlyRecordSnapshot> findRecords(
		Long userId,
		String recordSource,
		LocalDate from,
		LocalDate to
	) {
		return jdbcClient.sql("""
				SELECT billing_month, utility_type, amount, usage_value, usage_unit
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = :recordSource
				  AND billing_month BETWEEN :from AND :to
				ORDER BY billing_month DESC,
				         FIELD(utility_type, 'ELECTRICITY', 'GAS', 'WATER')
				""")
			.param("userId", userId)
			.param("recordSource", recordSource)
			.param("from", from)
			.param("to", to)
			.query((resultSet, rowNum) -> mapRecord(resultSet))
			.list();
	}

	private List<MonthlyRecordSnapshot> queryRecords(String sql, Long userId) {
		return jdbcClient.sql(sql)
			.param("userId", userId)
			.query((resultSet, rowNum) -> mapRecord(resultSet))
			.list();
	}

	private static MonthlyRecordSnapshot mapRecord(java.sql.ResultSet resultSet) throws java.sql.SQLException {
		return new MonthlyRecordSnapshot(
				toLocalDate(resultSet.getDate("billing_month")),
				UtilityType.valueOf(resultSet.getString("utility_type")),
				resultSet.getLong("amount"),
				resultSet.getBigDecimal("usage_value"),
				UsageUnit.valueOf(resultSet.getString("usage_unit"))
			);
	}

	private static LocalDate toLocalDate(Date date) {
		return date.toLocalDate();
	}

	public record MonthlyRecordSnapshot(
		LocalDate billingMonth,
		UtilityType utilityType,
		long amount,
		java.math.BigDecimal usage,
		UsageUnit usageUnit
	) {
	}
}
