package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.WhatIfScreen;

public record EcoHomeResponse(
	WhatIfScreen screen,
	Long roundId,
	Header header,
	Progress progress,
	LatestReport latestReport,
	Application application,
	Goal goal,
	TodayMissions todayMissions,
	ResultModal resultModal,
	Links links
) {

	public record Header(
		String periodStart,
		String periodEnd,
		int remainingMonths,
		List<Integer> remainingLabelMonths
	) {
	}

	public record Progress(
		BigDecimal cumulativeRate,
		List<String> coveredMonths,
		TargetTier currentTier,
		TargetTier targetTier,
		List<TierProgress> tiers,
		BigDecimal gapToNextTierPoint,
		Long nextTierMileage
	) {
	}

	public record TierProgress(
		TargetTier tier,
		Long mileage,
		String state
	) {
	}

	public record LatestReport(
		boolean available,
		String reportMonth,
		OffsetDateTime billRegisteredAt,
		BigDecimal monthlyRate,
		BigDecimal targetRate,
		Boolean achieved
	) {
	}

	public record Application(
		ApplicationStatus status,
		boolean showBanner,
		String externalUrl
	) {
	}

	public record Goal(
		boolean goalSet,
		BigDecimal combinedTargetRate,
		TargetTier tier,
		Long expectedMileage
	) {
	}

	public record TodayMissions(
		int completedCount,
		int totalCount
	) {
	}

	public record ResultModal(
		Long roundId,
		String periodStart,
		String periodEnd,
		BigDecimal finalRate,
		TargetTier tier,
		Long mileage,
		OffsetDateTime confirmedAt
	) {
	}

	public record Links(
		boolean benefitTab,
		boolean pocketTab,
		boolean movingNotice
	) {
	}
}
