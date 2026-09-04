package com.greenpocket.eco.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class EcoRepository {

	private final JdbcClient jdbcClient;

	public Optional<EcoUserSnapshot> findUser(Long userId) {
		return jdbcClient.sql("""
				SELECT sido_code, sido_name, sigungu_code, sigungu_name,
				       eco_link_status, eco_linked_at,
				       eco_sido_code, eco_sigungu_code, eco_address_label, eco_address_registered_at
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new EcoUserSnapshot(
				resultSet.getString("sido_code"),
				resultSet.getString("sido_name"),
				resultSet.getString("sigungu_code"),
				resultSet.getString("sigungu_name"),
				EcoLinkStatus.valueOf(resultSet.getString("eco_link_status")),
				toLocalDateTime(resultSet.getTimestamp("eco_linked_at")),
				resultSet.getString("eco_sido_code"),
				resultSet.getString("eco_sigungu_code"),
				resultSet.getString("eco_address_label"),
				toLocalDate(resultSet.getDate("eco_address_registered_at"))
			))
			.optional();
	}

	public void markLinking(Long userId) {
		jdbcClient.sql("""
				UPDATE app_user
				SET eco_link_status = 'LINKING', updated_at = CURRENT_TIMESTAMP
				WHERE id = :userId
				""")
			.param("userId", userId)
			.update();
	}

	public void markLinked(
		Long userId,
		LocalDateTime linkedAt,
		String sidoCode,
		String sigunguCode,
		String addressLabel,
		LocalDate addressRegisteredAt
	) {
		jdbcClient.sql("""
				UPDATE app_user
				SET eco_link_status = 'LINKED',
				    eco_linked_at = :linkedAt,
				    eco_sido_code = :sidoCode,
				    eco_sigungu_code = :sigunguCode,
				    eco_address_label = :addressLabel,
				    eco_address_registered_at = :addressRegisteredAt,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :userId
				""")
			.param("linkedAt", linkedAt)
			.param("sidoCode", sidoCode)
			.param("sigunguCode", sigunguCode)
			.param("addressLabel", addressLabel)
			.param("addressRegisteredAt", addressRegisteredAt)
			.param("userId", userId)
			.update();
	}

	public Optional<EcoRoundSnapshot> findCurrentRound(Long userId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, round_status, application_status,
				       baseline_total_amount, baseline_total_carbon_g, baseline_queried_at, goal_set_at
				FROM eco_round
				WHERE user_id = :userId
				ORDER BY period_end DESC
				LIMIT 1
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new EcoRoundSnapshot(
				resultSet.getLong("id"),
				resultSet.getDate("period_start").toLocalDate(),
				resultSet.getDate("period_end").toLocalDate(),
				RoundStatus.valueOf(resultSet.getString("round_status")),
				ApplicationStatus.valueOf(resultSet.getString("application_status")),
				resultSet.getObject("baseline_total_amount", Long.class),
				resultSet.getBigDecimal("baseline_total_carbon_g"),
				toLocalDateTime(resultSet.getTimestamp("baseline_queried_at")),
				toLocalDateTime(resultSet.getTimestamp("goal_set_at"))
			))
			.optional();
	}

	public List<EcoUtilitySnapshot> findUtilities(Long roundId) {
		return jdbcClient.sql("""
				SELECT utility_type, is_registered, unregistered_reason, carbon_factor_g,
				       baseline_amount, baseline_usage, baseline_share_rate
				FROM eco_round_utility
				WHERE eco_round_id = :roundId
				ORDER BY FIELD(utility_type, 'ELECTRICITY', 'GAS', 'WATER')
				""")
			.param("roundId", roundId)
			.query((resultSet, rowNum) -> new EcoUtilitySnapshot(
				UtilityType.valueOf(resultSet.getString("utility_type")),
				resultSet.getBoolean("is_registered"),
				resultSet.getString("unregistered_reason"),
				resultSet.getBigDecimal("carbon_factor_g"),
				resultSet.getObject("baseline_amount", Long.class),
				resultSet.getBigDecimal("baseline_usage"),
				resultSet.getBigDecimal("baseline_share_rate")
			)).list();
	}

	public Long upsertMockRound(
		Long userId,
		LocalDate periodStart,
		LocalDate periodEnd,
		Long totalAmount,
		BigDecimal totalCarbonG,
		LocalDateTime queriedAt
	) {
		Optional<Long> existingId = jdbcClient.sql("""
				SELECT id FROM eco_round
				WHERE user_id = :userId AND period_start = :periodStart
				""")
			.param("userId", userId)
			.param("periodStart", periodStart)
			.query(Long.class)
			.optional();

		if (existingId.isPresent()) {
			jdbcClient.sql("""
					UPDATE eco_round
					SET period_end = :periodEnd,
					    baseline_total_amount = :totalAmount,
					    baseline_total_carbon_g = :totalCarbonG,
					    baseline_queried_at = :queriedAt,
					    updated_at = CURRENT_TIMESTAMP
					WHERE id = :roundId
					""")
				.param("periodEnd", periodEnd)
				.param("totalAmount", totalAmount)
				.param("totalCarbonG", totalCarbonG)
				.param("queriedAt", queriedAt)
				.param("roundId", existingId.get())
				.update();
			return existingId.get();
		}

		jdbcClient.sql("""
				INSERT INTO eco_round (
				    user_id, period_start, period_end, round_status, application_status,
				    baseline_total_amount, baseline_total_carbon_g, baseline_queried_at
				) VALUES (
				    :userId, :periodStart, :periodEnd, 'READY', 'NOT_APPLIED',
				    :totalAmount, :totalCarbonG, :queriedAt
				)
				""")
			.param("userId", userId)
			.param("periodStart", periodStart)
			.param("periodEnd", periodEnd)
			.param("totalAmount", totalAmount)
			.param("totalCarbonG", totalCarbonG)
			.param("queriedAt", queriedAt)
			.update();

		return jdbcClient.sql("""
				SELECT id FROM eco_round
				WHERE user_id = :userId AND period_start = :periodStart
				""")
			.param("userId", userId)
			.param("periodStart", periodStart)
			.query(Long.class)
			.single();
	}

	public void upsertMockUtility(
		Long roundId,
		UtilityType utilityType,
		BigDecimal carbonFactorG,
		Long baselineAmount,
		BigDecimal baselineUsage,
		BigDecimal shareRate
	) {
		jdbcClient.sql("""
				INSERT INTO eco_round_utility (
				    eco_round_id, utility_type, is_registered, unregistered_reason,
				    carbon_factor_g, baseline_amount, baseline_usage, baseline_share_rate
				) VALUES (
				    :roundId, :utilityType, 1, NULL,
				    :carbonFactorG, :baselineAmount, :baselineUsage, :shareRate
				)
				ON DUPLICATE KEY UPDATE
				    is_registered = 1,
				    unregistered_reason = NULL,
				    carbon_factor_g = :carbonFactorG,
				    baseline_amount = :baselineAmount,
				    baseline_usage = :baselineUsage,
				    baseline_share_rate = :shareRate
				""")
			.param("roundId", roundId)
			.param("utilityType", utilityType.name())
			.param("carbonFactorG", carbonFactorG)
			.param("baselineAmount", baselineAmount)
			.param("baselineUsage", baselineUsage)
			.param("shareRate", shareRate)
			.update();
	}

	public void upsertMockBaselineRecord(
		Long userId,
		LocalDate billingMonth,
		UtilityType utilityType,
		Long amount,
		BigDecimal usageValue,
		UsageUnit usageUnit
	) {
		jdbcClient.sql("""
				INSERT INTO utility_monthly_record (
				    user_id, record_source, billing_month, utility_type, bill_type,
				    amount, usage_value, usage_unit, input_source, confidence, record_status
				) VALUES (
				    :userId, 'ECO_BASELINE', :billingMonth, :utilityType, NULL,
				    :amount, :usageValue, :usageUnit, 'ECO_LINK', NULL, 'CONFIRMED'
				)
				ON DUPLICATE KEY UPDATE
				    amount = :amount,
				    usage_value = :usageValue,
				    usage_unit = :usageUnit,
				    input_source = 'ECO_LINK',
				    record_status = 'CONFIRMED',
				    updated_at = CURRENT_TIMESTAMP
				""")
			.param("userId", userId)
			.param("billingMonth", billingMonth)
			.param("utilityType", utilityType.name())
			.param("amount", amount)
			.param("usageValue", usageValue)
			.param("usageUnit", usageUnit.name())
			.update();
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private static LocalDate toLocalDate(Date date) {
		return date == null ? null : date.toLocalDate();
	}

	public record EcoUserSnapshot(
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		EcoLinkStatus linkStatus,
		LocalDateTime linkedAt,
		String ecoSidoCode,
		String ecoSigunguCode,
		String ecoAddressLabel,
		LocalDate ecoAddressRegisteredAt
	) {
		public boolean isSeoulResident() {
			return "11".equals(sidoCode);
		}

		public String profileAddressLabel() {
			return Stream.of(sidoName, sigunguName)
				.filter(value -> value != null && !value.isBlank())
				.reduce((left, right) -> left + " " + right)
				.orElse("서울");
		}
	}

	public record EcoRoundSnapshot(
		Long id,
		LocalDate periodStart,
		LocalDate periodEnd,
		RoundStatus roundStatus,
		ApplicationStatus applicationStatus,
		Long baselineTotalAmount,
		BigDecimal baselineTotalCarbonG,
		LocalDateTime baselineQueriedAt,
		LocalDateTime goalSetAt
	) {
	}

	public record EcoUtilitySnapshot(
		UtilityType utilityType,
		boolean registered,
		String unregisteredReason,
		BigDecimal carbonFactorG,
		Long baselineAmount,
		BigDecimal baselineUsage,
		BigDecimal baselineShareRate
	) {
	}
}
