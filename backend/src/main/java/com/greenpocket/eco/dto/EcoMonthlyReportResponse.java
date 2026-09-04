package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record EcoMonthlyReportResponse(
	String reportMonth,
	Long roundId,
	OffsetDateTime billRegisteredAt,
	String baselineDescription,
	Result result,
	Cause cause,
	Prescription prescription,
	List<MonthlyRate> monthlyRates,
	String emptyReason
) {

	public record Result(
		BigDecimal monthlyRate,
		BigDecimal targetRate,
		boolean achieved,
		BigDecimal cumulativeRate,
		List<String> cumulativeMonths
	) {
	}

	public record Cause(
		List<UtilityResult> byUtility,
		UtilityType largestCarbonUtility,
		List<CarbonFactor> carbonFactors
	) {
	}

	public record UtilityResult(
		UtilityType utilityType,
		BigDecimal baselineUsage,
		BigDecimal actualUsage,
		UsageUnit usageUnit,
		BigDecimal rate,
		boolean achieved,
		BigDecimal carbonSharePercent,
		boolean expanded
	) {
	}

	public record CarbonFactor(
		UtilityType utilityType,
		BigDecimal factorG,
		UsageUnit unit
	) {
	}

	public record Prescription(
		int remainingMonths,
		List<Integer> remainingMonthLabels,
		BigDecimal requiredRate,
		boolean achievable,
		List<RequiredUtility> requiredByUtility,
		BigDecimal selectedMissionRate,
		UtilityType adjustTargetUtility
	) {
	}

	public record RequiredUtility(
		UtilityType utilityType,
		BigDecimal requiredRate,
		String assumption
	) {
	}

	public record MonthlyRate(
		String yearMonth,
		BigDecimal rate,
		boolean achieved
	) {
	}
}
