package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record EcoGoalFormResponse(
	Long roundId,
	String periodStart,
	String periodEnd,
	List<TierOption> tiers,
	List<Segment> segments
) {

	public record TierOption(
		TargetTier tier,
		String label,
		BigDecimal targetRate,
		Long mileage
	) {
	}

	public record Segment(
		UtilityType utilityType,
		boolean registered,
		String unregisteredReason,
		Long baselineAmount,
		BigDecimal baselineUsage,
		BigDecimal monthlyBaselineUsage,
		UsageUnit usageUnit,
		BigDecimal missionRateCap,
		TargetTier selectedTier,
		String registerGuideUrl,
		boolean excludedFromCombine,
		List<Mission> missions
	) {
	}

	public record Mission(
		Long missionId,
		String missionCode,
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
		BigDecimal computedRate,
		boolean capped,
		boolean selected
	) {
	}
}
