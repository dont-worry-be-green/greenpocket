package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record EcoGoalResponse(
	Long roundId,
	boolean goalSet,
	OffsetDateTime goalSetAt,
	BigDecimal combinedTargetRate,
	TargetTier tier,
	Long expectedMileage,
	Long expectedSavingAmount,
	List<UtilityGoal> utilities,
	List<SavedMission> missions
) {

	public record UtilityGoal(
		UtilityType utilityType,
		TargetTier targetTier,
		BigDecimal targetRate,
		BigDecimal baselineUsage,
		BigDecimal targetUsage,
		UsageUnit usageUnit,
		Long expectedSaving
	) {
	}

	public record SavedMission(
		Long missionId,
		String title,
		UtilityType utilityType,
		BigDecimal computedRate,
		boolean counted,
		String exclusionReason
	) {
	}
}
