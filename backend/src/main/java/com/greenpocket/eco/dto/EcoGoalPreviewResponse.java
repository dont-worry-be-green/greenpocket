package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record EcoGoalPreviewResponse(
	List<UtilityTarget> utilities,
	Combined combined,
	MissionSummary missions,
	List<CarbonFactor> carbonFactors
) {

	public record UtilityTarget(
		UtilityType utilityType,
		BigDecimal targetRate,
		BigDecimal baselineUsage,
		BigDecimal targetUsage,
		UsageUnit usageUnit,
		Long baselineAmount,
		Long expectedSaving,
		int displayPrecision
	) {
	}

	public record Combined(
		BigDecimal baselineCarbonG,
		BigDecimal targetCarbonG,
		BigDecimal combinedRate,
		TargetTier tier,
		String tierLabel,
		Long expectedMileage,
		Long totalExpectedSaving,
		Long baselineTotalAmount,
		List<UtilityType> excludedUtilities,
		NextTier nextTier
	) {
	}

	public record NextTier(
		TargetTier tier,
		BigDecimal gapPoint,
		Long mileage
	) {
	}

	public record MissionSummary(
		BigDecimal combinedMissionRate,
		BigDecimal shortfallPoint,
		boolean meetsTarget,
		List<MissionItem> items
	) {
	}

	public record MissionItem(
		Long missionId,
		BigDecimal computedRate,
		boolean counted,
		String exclusionReason
	) {
	}

	public record CarbonFactor(
		UtilityType utilityType,
		BigDecimal factorG,
		UsageUnit unit
	) {
	}
}
