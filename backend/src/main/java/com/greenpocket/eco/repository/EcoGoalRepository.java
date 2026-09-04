package com.greenpocket.eco.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class EcoGoalRepository {

	private final JdbcClient jdbcClient;

	public Optional<GoalRoundSnapshot> findRound(Long userId, Long roundId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, round_status,
				       baseline_total_amount, baseline_total_carbon_g, goal_set_at,
				       combined_target_rate, expected_mileage, expected_saving_amount
				FROM eco_round
				WHERE id = :roundId AND user_id = :userId
				""")
			.param("roundId", roundId)
			.param("userId", userId)
			.query((resultSet, rowNum) -> new GoalRoundSnapshot(
				resultSet.getLong("id"),
				resultSet.getDate("period_start").toLocalDate(),
				resultSet.getDate("period_end").toLocalDate(),
				RoundStatus.valueOf(resultSet.getString("round_status")),
				resultSet.getObject("baseline_total_amount", Long.class),
				resultSet.getBigDecimal("baseline_total_carbon_g"),
				toLocalDateTime(resultSet.getTimestamp("goal_set_at")),
				resultSet.getBigDecimal("combined_target_rate"),
				resultSet.getObject("expected_mileage", Long.class),
				resultSet.getObject("expected_saving_amount", Long.class)
			))
			.optional();
	}

	public List<GoalUtilitySnapshot> findUtilities(Long roundId) {
		return jdbcClient.sql("""
				SELECT utility_type, is_registered, unregistered_reason, carbon_factor_g,
				       baseline_amount, baseline_usage, baseline_share_rate,
				       target_tier, target_rate, target_usage, expected_saving_amount
				FROM eco_round_utility
				WHERE eco_round_id = :roundId
				ORDER BY FIELD(utility_type, 'ELECTRICITY', 'GAS', 'WATER')
				""")
			.param("roundId", roundId)
			.query((resultSet, rowNum) -> new GoalUtilitySnapshot(
				UtilityType.valueOf(resultSet.getString("utility_type")),
				resultSet.getBoolean("is_registered"),
				resultSet.getString("unregistered_reason"),
				resultSet.getBigDecimal("carbon_factor_g"),
				resultSet.getObject("baseline_amount", Long.class),
				resultSet.getBigDecimal("baseline_usage"),
				resultSet.getBigDecimal("baseline_share_rate"),
				toTargetTier(resultSet.getString("target_tier")),
				resultSet.getBigDecimal("target_rate"),
				resultSet.getBigDecimal("target_usage"),
				resultSet.getObject("expected_saving_amount", Long.class)
			)).list();
	}

	public List<MissionSnapshot> findActiveMissions() {
		return jdbcClient.sql("""
				SELECT id, mission_code, utility_type, title, description, difficulty,
				       evidence_amount, evidence_unit, evidence_text, calculation_basis,
				       source_org, device_group, season_tags, rate_cap, display_order
				FROM mission_catalog
				WHERE is_active = 1
				ORDER BY FIELD(utility_type, 'ELECTRICITY', 'GAS', 'WATER'), display_order, id
				""")
			.query((resultSet, rowNum) -> new MissionSnapshot(
				resultSet.getLong("id"),
				resultSet.getString("mission_code"),
				UtilityType.valueOf(resultSet.getString("utility_type")),
				resultSet.getString("title"),
				resultSet.getString("description"),
				MissionDifficulty.valueOf(resultSet.getString("difficulty")),
				resultSet.getBigDecimal("evidence_amount"),
				UsageUnit.valueOf(resultSet.getString("evidence_unit")),
				resultSet.getString("evidence_text"),
				resultSet.getString("calculation_basis"),
				resultSet.getString("source_org"),
				resultSet.getString("device_group"),
				seasonTags(resultSet.getString("season_tags")),
				resultSet.getBigDecimal("rate_cap"),
				resultSet.getInt("display_order")
			)).list();
	}

	public List<SavedMissionSnapshot> findSavedMissions(Long roundId) {
		return jdbcClient.sql("""
				SELECT um.mission_id, mc.title, mc.utility_type, um.computed_rate,
				       um.is_counted, um.exclusion_reason
				FROM user_mission um
				JOIN mission_catalog mc ON mc.id = um.mission_id
				WHERE um.eco_round_id = :roundId
				ORDER BY mc.display_order, um.mission_id
				""")
			.param("roundId", roundId)
			.query((resultSet, rowNum) -> new SavedMissionSnapshot(
				resultSet.getLong("mission_id"),
				resultSet.getString("title"),
				UtilityType.valueOf(resultSet.getString("utility_type")),
				resultSet.getBigDecimal("computed_rate"),
				resultSet.getBoolean("is_counted"),
				resultSet.getString("exclusion_reason")
			)).list();
	}

	public void updateRoundGoal(
		Long roundId,
		BigDecimal combinedTargetRate,
		Long expectedMileage,
		Long expectedSavingAmount
	) {
		jdbcClient.sql("""
				UPDATE eco_round
				SET round_status = 'GOAL_SET',
				    combined_target_rate = :combinedTargetRate,
				    expected_mileage = :expectedMileage,
				    expected_saving_amount = :expectedSavingAmount,
				    goal_set_at = CURRENT_TIMESTAMP,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :roundId
				""")
			.param("combinedTargetRate", combinedTargetRate)
			.param("expectedMileage", expectedMileage)
			.param("expectedSavingAmount", expectedSavingAmount)
			.param("roundId", roundId)
			.update();
	}

	public void clearUtilityTargets(Long roundId) {
		jdbcClient.sql("""
				UPDATE eco_round_utility
				SET target_tier = NULL,
				    target_rate = NULL,
				    target_usage = NULL,
				    expected_saving_amount = NULL
				WHERE eco_round_id = :roundId
				""")
			.param("roundId", roundId)
			.update();
	}

	public void updateUtilityTarget(
		Long roundId,
		UtilityType utilityType,
		TargetTier targetTier,
		BigDecimal targetRate,
		BigDecimal targetUsage,
		Long expectedSaving
	) {
		jdbcClient.sql("""
				UPDATE eco_round_utility
				SET target_tier = :targetTier,
				    target_rate = :targetRate,
				    target_usage = :targetUsage,
				    expected_saving_amount = :expectedSaving
				WHERE eco_round_id = :roundId AND utility_type = :utilityType
				""")
			.param("targetTier", targetTier.name())
			.param("targetRate", targetRate)
			.param("targetUsage", targetUsage)
			.param("expectedSaving", expectedSaving)
			.param("roundId", roundId)
			.param("utilityType", utilityType.name())
			.update();
	}

	public void deleteSavedMissions(Long roundId) {
		jdbcClient.sql("DELETE FROM user_mission WHERE eco_round_id = :roundId")
			.param("roundId", roundId)
			.update();
	}

	public void saveMission(
		Long userId,
		Long roundId,
		Long missionId,
		BigDecimal computedRate,
		boolean counted,
		String exclusionReason
	) {
		JdbcClient.StatementSpec statement = jdbcClient.sql("""
				INSERT INTO user_mission (
				    user_id, eco_round_id, mission_id, computed_rate, is_counted, exclusion_reason
				) VALUES (
				    :userId, :roundId, :missionId, :computedRate, :counted, :exclusionReason
				)
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.param("missionId", missionId)
			.param("computedRate", computedRate)
			.param("counted", counted);
		if (exclusionReason == null) {
			statement.param("exclusionReason", null, java.sql.Types.VARCHAR);
		}
		else {
			statement.param("exclusionReason", exclusionReason);
		}
		statement.update();
	}

	private static TargetTier toTargetTier(String value) {
		return value == null ? null : TargetTier.valueOf(value);
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private static List<String> seasonTags(String value) {
		return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split(","));
	}

	public record GoalRoundSnapshot(
		Long id,
		LocalDate periodStart,
		LocalDate periodEnd,
		RoundStatus roundStatus,
		Long baselineTotalAmount,
		BigDecimal baselineTotalCarbonG,
		LocalDateTime goalSetAt,
		BigDecimal combinedTargetRate,
		Long expectedMileage,
		Long expectedSavingAmount
	) {
	}

	public record GoalUtilitySnapshot(
		UtilityType utilityType,
		boolean registered,
		String unregisteredReason,
		BigDecimal carbonFactorG,
		Long baselineAmount,
		BigDecimal baselineUsage,
		BigDecimal baselineShareRate,
		TargetTier targetTier,
		BigDecimal targetRate,
		BigDecimal targetUsage,
		Long expectedSavingAmount
	) {
	}

	public record MissionSnapshot(
		Long id,
		String missionCode,
		UtilityType utilityType,
		String title,
		String description,
		MissionDifficulty difficulty,
		BigDecimal evidenceAmount,
		UsageUnit evidenceUnit,
		String evidenceText,
		String calculationBasis,
		String sourceOrg,
		String deviceGroup,
		List<String> seasonTags,
		BigDecimal rateCap,
		int displayOrder
	) {
	}

	public record SavedMissionSnapshot(
		Long missionId,
		String title,
		UtilityType utilityType,
		BigDecimal computedRate,
		boolean counted,
		String exclusionReason
	) {
	}
}
