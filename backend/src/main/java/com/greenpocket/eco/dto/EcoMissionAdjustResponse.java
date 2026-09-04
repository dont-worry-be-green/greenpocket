package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.global.type.UtilityType;

public record EcoMissionAdjustResponse(
	Long roundId,
	UtilityType utilityType,
	String reportMonth,
	BigDecimal requiredRate,
	String requiredAssumption,
	BigDecimal carbonSharePercent,
	Comparison comparison,
	int currentSelectedCount,
	List<Mission> missions,
	Preview preview,
	TierDowngrade tierDowngrade
) {

	public record Comparison(
		BigDecimal selectedExpectedRate,
		BigDecimal actualRate
	) {
	}

	public record Mission(
		Long missionId,
		String title,
		BigDecimal computedRate,
		MissionDifficulty difficulty,
		String deviceGroup,
		String evidenceText,
		String calculationBasis,
		String sourceOrg,
		boolean selected,
		boolean recommended,
		boolean capped
	) {
	}

	public record Preview(
		BigDecimal currentRate,
		BigDecimal withRecommendedRate,
		boolean coversRequired
	) {
	}

	public record TierDowngrade(
		boolean suggest,
		int consecutiveMisses,
		String message
	) {
	}
}
