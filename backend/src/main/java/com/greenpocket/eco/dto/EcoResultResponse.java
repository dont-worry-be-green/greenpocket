package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record EcoResultResponse(
	Long roundId,
	String periodStart,
	String periodEnd,
	OffsetDateTime confirmedAt,
	String confirmedSource,
	BigDecimal finalRate,
	BigDecimal targetRate,
	boolean achieved,
	TargetTier tier,
	String tierLabel,
	Long confirmedMileage,
	Amount amount,
	List<UtilityResult> utilityResults,
	List<MonthlyRate> monthlyRates,
	boolean mileageConverted,
	NextRound nextRound
) {

	public record Amount(
		Long baselineTotal,
		Long actualTotal,
		Long savedAmount,
		boolean savedIsPocketEligible
	) {
	}

	public record UtilityResult(
		UtilityType utilityType,
		BigDecimal baselineUsage,
		BigDecimal actualUsage,
		UsageUnit usageUnit,
		BigDecimal finalRate,
		BigDecimal targetRate,
		boolean achieved
	) {
	}

	public record MonthlyRate(
		String yearMonth,
		BigDecimal rate,
		boolean achieved
	) {
	}

	public record NextRound(
		Long roundId,
		String periodStart,
		String periodEnd,
		boolean goalSet
	) {
	}
}
