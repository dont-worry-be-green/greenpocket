package com.greenpocket.eco.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class EcoProgressRepository {

	private final JdbcClient jdbcClient;

	public Optional<EcoLinkStatus> findLinkStatus(Long userId) {
		return jdbcClient.sql("SELECT eco_link_status FROM app_user WHERE id = :userId")
			.param("userId", userId)
			.query(String.class)
			.optional()
			.map(EcoLinkStatus::valueOf);
	}

	public Optional<ProgressRoundSnapshot> findCurrentRound(Long userId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, application_status, goal_set_at,
				       combined_target_rate, expected_mileage, final_rate,
				       confirmed_mileage, confirmed_at
				FROM eco_round
				WHERE user_id = :userId
				ORDER BY period_end DESC
				LIMIT 1
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> roundSnapshot(resultSet))
			.optional();
	}

	public Optional<ProgressRoundSnapshot> findRoundForMonth(Long userId, LocalDate month) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, application_status, goal_set_at,
				       combined_target_rate, expected_mileage, final_rate,
				       confirmed_mileage, confirmed_at
				FROM eco_round
				WHERE user_id = :userId
				  AND period_start <= :month
				  AND period_end >= :month
				ORDER BY period_end DESC
				LIMIT 1
				""")
			.param("userId", userId)
			.param("month", month)
			.query((resultSet, rowNum) -> roundSnapshot(resultSet))
			.optional();
	}

	public Optional<ResultRoundSnapshot> findUnviewedResult(Long userId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, final_rate, confirmed_mileage, confirmed_at
				FROM eco_round
				WHERE user_id = :userId
				  AND round_status = 'CONFIRMED'
				  AND result_viewed_at IS NULL
				ORDER BY period_end DESC
				LIMIT 1
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ResultRoundSnapshot(
				resultSet.getLong("id"),
				resultSet.getDate("period_start").toLocalDate(),
				resultSet.getDate("period_end").toLocalDate(),
				resultSet.getBigDecimal("final_rate"),
				resultSet.getLong("confirmed_mileage"),
				toLocalDateTime(resultSet.getTimestamp("confirmed_at"))
			))
			.optional();
	}

	public Optional<LocalDate> findLatestBillMonth(Long userId, LocalDate periodStart, LocalDate periodEnd) {
		return jdbcClient.sql("""
				SELECT MAX(billing_month)
				FROM utility_monthly_record
				WHERE user_id = :userId
				  AND record_source = 'BILL'
				  AND record_status = 'CONFIRMED'
				  AND billing_month BETWEEN :periodStart AND :periodEnd
				""")
			.param("userId", userId)
			.param("periodStart", periodStart)
			.param("periodEnd", periodEnd)
			.query(Date.class)
			.optional()
			.map(Date::toLocalDate);
	}

	public List<MonthlyUtilitySnapshot> findMonthlyUtilities(
		Long userId,
		Long roundId,
		LocalDate periodStart,
		LocalDate reportMonth
	) {
		return jdbcClient.sql("""
				SELECT bill.billing_month, bill.utility_type,
				       AVG(baseline.usage_value) AS baseline_usage,
				       bill.usage_value AS actual_usage,
				       bill.registered_at AS bill_registered_at,
				       utilities.carbon_factor_g, utilities.target_rate,
				       bill.usage_unit
				FROM utility_monthly_record bill
				JOIN utility_monthly_record baseline
				  ON baseline.user_id = bill.user_id
				 AND baseline.record_source = 'ECO_BASELINE'
				 AND baseline.record_status = 'CONFIRMED'
				 AND baseline.utility_type = bill.utility_type
				 AND MONTH(baseline.billing_month) = MONTH(bill.billing_month)
				 AND baseline.billing_month >= DATE_SUB(bill.billing_month, INTERVAL 2 YEAR)
				 AND baseline.billing_month < bill.billing_month
				JOIN eco_round_utility utilities
				  ON utilities.eco_round_id = :roundId
				 AND utilities.utility_type = bill.utility_type
				 AND utilities.is_registered = 1
				WHERE bill.user_id = :userId
				  AND bill.record_source = 'BILL'
				  AND bill.record_status = 'CONFIRMED'
				  AND bill.billing_month BETWEEN :periodStart AND :reportMonth
				GROUP BY bill.billing_month, bill.utility_type, bill.usage_value,
				         bill.registered_at, bill.usage_unit,
				         utilities.carbon_factor_g, utilities.target_rate
				ORDER BY bill.billing_month,
				         FIELD(bill.utility_type, 'ELECTRICITY', 'GAS', 'WATER')
				""")
			.param("roundId", roundId)
			.param("userId", userId)
			.param("periodStart", periodStart)
			.param("reportMonth", reportMonth)
			.query((resultSet, rowNum) -> new MonthlyUtilitySnapshot(
				resultSet.getDate("billing_month").toLocalDate(),
				UtilityType.valueOf(resultSet.getString("utility_type")),
				resultSet.getBigDecimal("baseline_usage"),
				resultSet.getBigDecimal("actual_usage"),
				UsageUnit.valueOf(resultSet.getString("usage_unit")),
				resultSet.getBigDecimal("carbon_factor_g"),
				resultSet.getBigDecimal("target_rate"),
				toLocalDateTime(resultSet.getTimestamp("bill_registered_at"))
			)).list();
	}

	public MissionProgressSnapshot findMissionProgress(Long userId, Long roundId, LocalDate today, String season) {
		int totalCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM user_mission selected
				JOIN mission_catalog mission ON mission.id = selected.mission_id
				WHERE selected.user_id = :userId
				  AND selected.eco_round_id = :roundId
				  AND mission.is_active = 1
				  AND FIND_IN_SET(:season, mission.season_tags) > 0
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.param("season", season)
			.query(Integer.class)
			.single();
		int completedCount = jdbcClient.sql("""
				SELECT COUNT(*)
				FROM user_mission selected
				JOIN mission_catalog mission ON mission.id = selected.mission_id
				JOIN mission_daily_log daily
				  ON daily.user_id = selected.user_id
				 AND daily.eco_round_id = selected.eco_round_id
				 AND daily.log_date = :today
				WHERE selected.user_id = :userId
				  AND selected.eco_round_id = :roundId
				  AND mission.is_active = 1
				  AND FIND_IN_SET(:season, mission.season_tags) > 0
				  AND JSON_CONTAINS(daily.completed_mission_ids, CAST(selected.mission_id AS JSON), '$')
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.param("today", today)
			.param("season", season)
			.query(Integer.class)
			.single();
		return new MissionProgressSnapshot(Math.min(completedCount, totalCount), totalCount);
	}

	public BigDecimal findSelectedMissionRate(Long userId, Long roundId) {
		return jdbcClient.sql("""
				SELECT COALESCE(SUM(computed_rate), 0)
				FROM user_mission
				WHERE user_id = :userId AND eco_round_id = :roundId AND is_counted = 1
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.query(BigDecimal.class)
			.single();
	}

	public void upsertMonthlyReport(
		Long userId,
		Long roundId,
		LocalDate reportMonth,
		BigDecimal monthlyRate,
		BigDecimal cumulativeRate,
		BigDecimal targetRate,
		BigDecimal requiredRate,
		int remainingMonths,
		boolean achieved,
		String byUtilityJson
	) {
		jdbcClient.sql("""
				INSERT INTO eco_monthly_report (
				    user_id, eco_round_id, report_month,
				    monthly_rate, cumulative_rate, target_rate,
				    required_rate, remaining_months, is_achieved,
				    by_utility, calculated_at
				)
				VALUES (
				    :userId, :roundId, :reportMonth,
				    :monthlyRate, :cumulativeRate, :targetRate,
				    :requiredRate, :remainingMonths, :achieved,
				    CAST(:byUtility AS JSON), CURRENT_TIMESTAMP
				)
				ON DUPLICATE KEY UPDATE
				    eco_round_id = VALUES(eco_round_id),
				    monthly_rate = VALUES(monthly_rate),
				    cumulative_rate = VALUES(cumulative_rate),
				    target_rate = VALUES(target_rate),
				    required_rate = VALUES(required_rate),
				    remaining_months = VALUES(remaining_months),
				    is_achieved = VALUES(is_achieved),
				    by_utility = VALUES(by_utility),
				    calculated_at = CURRENT_TIMESTAMP
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.param("reportMonth", reportMonth)
			.param("monthlyRate", monthlyRate)
			.param("cumulativeRate", cumulativeRate)
			.param("targetRate", targetRate)
			.param("requiredRate", requiredRate)
			.param("remainingMonths", remainingMonths)
			.param("achieved", achieved)
			.param("byUtility", byUtilityJson)
			.update();
	}

	public void deleteMonthlyReport(Long userId, LocalDate reportMonth) {
		jdbcClient.sql("""
				DELETE FROM eco_monthly_report
				WHERE user_id = :userId AND report_month = :reportMonth
				""")
			.param("userId", userId)
			.param("reportMonth", reportMonth)
			.update();
	}

	private static ProgressRoundSnapshot roundSnapshot(java.sql.ResultSet resultSet) throws java.sql.SQLException {
		return new ProgressRoundSnapshot(
			resultSet.getLong("id"),
			resultSet.getDate("period_start").toLocalDate(),
			resultSet.getDate("period_end").toLocalDate(),
			ApplicationStatus.valueOf(resultSet.getString("application_status")),
			toLocalDateTime(resultSet.getTimestamp("goal_set_at")),
			resultSet.getBigDecimal("combined_target_rate"),
			resultSet.getObject("expected_mileage", Long.class),
			resultSet.getBigDecimal("final_rate"),
			resultSet.getObject("confirmed_mileage", Long.class),
			toLocalDateTime(resultSet.getTimestamp("confirmed_at"))
		);
	}

	private static LocalDateTime toLocalDateTime(Timestamp value) {
		return value == null ? null : value.toLocalDateTime();
	}

	public record ProgressRoundSnapshot(
		Long id,
		LocalDate periodStart,
		LocalDate periodEnd,
		ApplicationStatus applicationStatus,
		LocalDateTime goalSetAt,
		BigDecimal combinedTargetRate,
		Long expectedMileage,
		BigDecimal finalRate,
		Long confirmedMileage,
		LocalDateTime confirmedAt
	) {
	}

	public record ResultRoundSnapshot(
		Long id,
		LocalDate periodStart,
		LocalDate periodEnd,
		BigDecimal finalRate,
		Long confirmedMileage,
		LocalDateTime confirmedAt
	) {
	}

	public record MonthlyUtilitySnapshot(
		LocalDate billingMonth,
		UtilityType utilityType,
		BigDecimal baselineUsage,
		BigDecimal actualUsage,
		UsageUnit usageUnit,
		BigDecimal carbonFactorG,
		BigDecimal targetRate,
		LocalDateTime billRegisteredAt
	) {
	}

	public record MissionProgressSnapshot(int completedCount, int totalCount) {
	}
}
