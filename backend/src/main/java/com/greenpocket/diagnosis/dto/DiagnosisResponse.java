package com.greenpocket.diagnosis.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosisResponse(
	boolean empty,
	String targetYearMonth,
	String screen,
	String yearMonth,
	String profileSummary,
	Summary summary,
	LastYearComparison lastYearComparison,
	SingleHouseholdComparison singleHouseholdComparison,
	WhatIfLink whatIfLink
) {

	public static DiagnosisResponse empty(String targetYearMonth) {
		return new DiagnosisResponse(
			true,
			targetYearMonth,
			"AN-01",
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

	public record Summary(
		long currentTotal,
		Long previousYearTotal,
		Long diffLastYearTotal,
		boolean hasPreviousYear,
		List<SummaryItem> items
	) {
	}

	public record SummaryItem(
		UtilityType utilityType,
		long amount,
		BigDecimal usage,
		UsageUnit usageUnit
	) {
	}

	public record LastYearComparison(
		boolean available,
		String unavailableReason,
		Long totalDiff,
		List<LastYearItem> items
	) {
	}

	public record LastYearItem(
		UtilityType utilityType,
		Long lastYearAmount,
		Long thisYearAmount,
		Long diff
	) {
	}

	public record SingleHouseholdComparison(
		String comparisonLabel,
		List<SingleHouseholdTab> tabs
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SingleHouseholdTab(
		UtilityType utilityType,
		boolean available,
		String unavailableReason,
		BigDecimal myUsage,
		BigDecimal averageUsage,
		BigDecimal differenceUsage,
		BigDecimal differenceRate,
		UsageUnit usageUnit,
		String comparisonLabel,
		String sourceName,
		String referencePeriod,
		BaselineCalculationBasis calculationBasis,
		String note
	) {
	}

	public record WhatIfLink(Long roundId, boolean goalSet) {
	}
}
