package com.greenpocket.eco.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class EcoResultRepository {

	private final JdbcClient jdbcClient;

	public Optional<ResultRoundSnapshot> findRound(Long userId, Long roundId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, round_status,
				       combined_target_rate, final_rate, baseline_total_amount,
				       actual_total_amount, saved_amount, confirmed_mileage, confirmed_at
				FROM eco_round
				WHERE id = :roundId AND user_id = :userId
				""")
			.param("roundId", roundId)
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ResultRoundSnapshot(
				resultSet.getLong("id"),
				resultSet.getDate("period_start").toLocalDate(),
				resultSet.getDate("period_end").toLocalDate(),
				RoundStatus.valueOf(resultSet.getString("round_status")),
				resultSet.getBigDecimal("combined_target_rate"),
				resultSet.getBigDecimal("final_rate"),
				resultSet.getObject("baseline_total_amount", Long.class),
				resultSet.getObject("actual_total_amount", Long.class),
				resultSet.getObject("saved_amount", Long.class),
				resultSet.getObject("confirmed_mileage", Long.class),
				toLocalDateTime(resultSet.getTimestamp("confirmed_at"))
			))
			.optional();
	}

	public List<UtilityResultSnapshot> findUtilityResults(Long roundId) {
		return jdbcClient.sql("""
				SELECT utility_type, baseline_usage, actual_usage,
				       target_tier, target_rate, final_rate, is_achieved
				FROM eco_round_utility
				WHERE eco_round_id = :roundId AND is_registered = 1
				ORDER BY FIELD(utility_type, 'ELECTRICITY', 'GAS', 'WATER')
				""")
			.param("roundId", roundId)
			.query((resultSet, rowNum) -> {
				UtilityType utilityType = UtilityType.valueOf(resultSet.getString("utility_type"));
				String targetTier = resultSet.getString("target_tier");
				return new UtilityResultSnapshot(
					utilityType,
					resultSet.getBigDecimal("baseline_usage"),
					resultSet.getBigDecimal("actual_usage"),
					utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3,
					targetTier == null ? null : TargetTier.valueOf(targetTier),
					resultSet.getBigDecimal("target_rate"),
					resultSet.getBigDecimal("final_rate"),
					resultSet.getObject("is_achieved", Boolean.class)
				);
			})
			.list();
	}

	public List<MonthlyRateSnapshot> findMonthlyRates(Long userId, Long roundId) {
		return jdbcClient.sql("""
				SELECT report_month, monthly_rate, is_achieved
				FROM eco_monthly_report
				WHERE user_id = :userId AND eco_round_id = :roundId
				ORDER BY report_month
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.query((resultSet, rowNum) -> new MonthlyRateSnapshot(
				resultSet.getDate("report_month").toLocalDate(),
				resultSet.getBigDecimal("monthly_rate"),
				resultSet.getBoolean("is_achieved")
			))
			.list();
	}

	public Optional<NextRoundSnapshot> findNextRound(Long userId, LocalDate periodEnd) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, goal_set_at
				FROM eco_round
				WHERE user_id = :userId AND period_start > :periodEnd
				ORDER BY period_start
				LIMIT 1
				""")
			.param("userId", userId)
			.param("periodEnd", periodEnd)
			.query((resultSet, rowNum) -> new NextRoundSnapshot(
				resultSet.getLong("id"),
				resultSet.getDate("period_start").toLocalDate(),
				resultSet.getDate("period_end").toLocalDate(),
				resultSet.getTimestamp("goal_set_at") != null
			))
			.optional();
	}

	public void markResultViewed(Long userId, Long roundId) {
		jdbcClient.sql("""
				UPDATE eco_round
				SET result_viewed_at = CURRENT_TIMESTAMP,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :roundId
				  AND user_id = :userId
				  AND result_viewed_at IS NULL
				""")
			.param("roundId", roundId)
			.param("userId", userId)
			.update();
	}

	private static LocalDateTime toLocalDateTime(Timestamp value) {
		return value == null ? null : value.toLocalDateTime();
	}

	public record ResultRoundSnapshot(
		Long id,
		LocalDate periodStart,
		LocalDate periodEnd,
		RoundStatus roundStatus,
		BigDecimal targetRate,
		BigDecimal finalRate,
		Long baselineTotalAmount,
		Long actualTotalAmount,
		Long savedAmount,
		Long confirmedMileage,
		LocalDateTime confirmedAt
	) {
	}

	public record UtilityResultSnapshot(
		UtilityType utilityType,
		BigDecimal baselineUsage,
		BigDecimal actualUsage,
		UsageUnit usageUnit,
		TargetTier targetTier,
		BigDecimal targetRate,
		BigDecimal finalRate,
		Boolean achieved
	) {
	}

	public record MonthlyRateSnapshot(
		LocalDate reportMonth,
		BigDecimal monthlyRate,
		boolean achieved
	) {
	}

	public record NextRoundSnapshot(
		Long id,
		LocalDate periodStart,
		LocalDate periodEnd,
		boolean goalSet
	) {
	}
}
