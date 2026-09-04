package com.greenpocket.diagnosis.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.greenpocket.diagnosis.entity.RegionLevel;
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
	RegionComparison regionComparison,
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

	public record RegionComparison(
		RegionLevel regionLevel,
		String regionLabel,
		boolean fallbackApplied,
		String sourceName,
		String baseMonth,
		OffsetDateTime extractedAt,
		List<RegionTab> tabs
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RegionTab(
		UtilityType utilityType,
		boolean available,
		String unavailableReason,
		Long myAmount,
		Long regionAvgAmount,
		Long diffRegion,
		List<SeriesPoint> series
	) {
	}

	public record SeriesPoint(
		String yearMonth,
		Long mine,
		Long regionAvg
	) {
	}

	public record WhatIfLink(Long roundId, boolean goalSet) {
	}
}
