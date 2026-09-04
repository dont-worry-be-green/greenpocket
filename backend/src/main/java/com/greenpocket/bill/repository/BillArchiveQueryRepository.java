package com.greenpocket.bill.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class BillArchiveQueryRepository {

	private final JdbcClient jdbcClient;

	public List<BillListSnapshot> findBills(
		Long userId,
		UtilityType utilityType,
		Integer year,
		int page,
		int size
	) {
		String conditions = conditions(utilityType, year);
		JdbcClient.StatementSpec query = jdbcClient.sql("""
				SELECT id, billing_month, utility_type, bill_type,
				       amount, usage_value, usage_unit, input_source,
				       record_status, registered_at
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = 'BILL'
				%s
				ORDER BY billing_month DESC,
				         FIELD(utility_type, 'ELECTRICITY', 'WATER', 'GAS'),
				         id DESC
				LIMIT :size OFFSET :offset
				""".formatted(conditions))
			.param("userId", userId)
			.param("size", size)
			.param("offset", (long)page * size);
		query = bindFilters(query, utilityType, year);
		return query.query((resultSet, rowNum) -> new BillListSnapshot(
			resultSet.getLong("id"),
			toLocalDate(resultSet.getDate("billing_month")),
			UtilityType.valueOf(resultSet.getString("utility_type")),
			BillType.valueOf(resultSet.getString("bill_type")),
			resultSet.getLong("amount"),
			resultSet.getBigDecimal("usage_value"),
			UsageUnit.valueOf(resultSet.getString("usage_unit")),
			InputSource.valueOf(resultSet.getString("input_source")),
			RecordStatus.valueOf(resultSet.getString("record_status")),
			toLocalDateTime(resultSet.getTimestamp("registered_at"))
		)).list();
	}

	public long countBills(Long userId, UtilityType utilityType, Integer year) {
		JdbcClient.StatementSpec query = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = 'BILL'
				%s
				""".formatted(conditions(utilityType, year)))
			.param("userId", userId);
		query = bindFilters(query, utilityType, year);
		return query.query(Long.class).single();
	}

	public Map<UtilityType, Long> countByUtility(Long userId, Integer year) {
		String yearCondition = year == null ? "" : "AND billing_month >= :yearStart AND billing_month < :yearEnd";
		JdbcClient.StatementSpec query = jdbcClient.sql("""
				SELECT utility_type, COUNT(*) AS item_count
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = 'BILL'
				  %s
				GROUP BY utility_type
				""".formatted(yearCondition))
			.param("userId", userId);
		query = bindYear(query, year);
		Map<UtilityType, Long> counts = new EnumMap<>(UtilityType.class);
		query.query((resultSet, rowNum) -> Map.entry(
			UtilityType.valueOf(resultSet.getString("utility_type")),
			resultSet.getLong("item_count")
		)).list().forEach(entry -> counts.put(entry.getKey(), entry.getValue()));
		return counts;
	}

	private String conditions(UtilityType utilityType, Integer year) {
		StringBuilder conditions = new StringBuilder();
		if (utilityType != null) {
			conditions.append("  AND utility_type = :utilityType\n");
		}
		if (year != null) {
			conditions.append("  AND billing_month >= :yearStart AND billing_month < :yearEnd\n");
		}
		return conditions.toString();
	}

	private JdbcClient.StatementSpec bindFilters(
		JdbcClient.StatementSpec query,
		UtilityType utilityType,
		Integer year
	) {
		if (utilityType != null) {
			query = query.param("utilityType", utilityType.name());
		}
		return bindYear(query, year);
	}

	private JdbcClient.StatementSpec bindYear(JdbcClient.StatementSpec query, Integer year) {
		if (year == null) {
			return query;
		}
		return query
			.param("yearStart", LocalDate.of(year, 1, 1))
			.param("yearEnd", LocalDate.of(year + 1, 1, 1));
	}

	private static LocalDate toLocalDate(Date value) {
		return value.toLocalDate();
	}

	private static LocalDateTime toLocalDateTime(Timestamp value) {
		return value.toLocalDateTime();
	}

	public record BillListSnapshot(
		Long recordId,
		LocalDate billingMonth,
		UtilityType utilityType,
		BillType billType,
		Long amount,
		BigDecimal usage,
		UsageUnit usageUnit,
		InputSource inputSource,
		RecordStatus recordStatus,
		LocalDateTime registeredAt
	) {
	}
}
