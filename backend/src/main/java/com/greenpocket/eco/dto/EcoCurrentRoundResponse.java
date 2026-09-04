package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record EcoCurrentRoundResponse(
	Long roundId,
	String periodStart,
	String periodEnd,
	int remainingMonths,
	RoundStatus roundStatus,
	ApplicationStatus applicationStatus,
	boolean goalSet,
	OffsetDateTime baselineQueriedAt,
	String baselineDescription,
	Baseline baseline,
	String nextScreen
) {

	public record Baseline(
		Long totalAmount,
		BigDecimal totalCarbonG,
		List<BaselineItem> items,
		UtilityType largestShareUtility
	) {
	}

	public record BaselineItem(
		UtilityType utilityType,
		boolean registered,
		Long amount,
		BigDecimal usage,
		UsageUnit usageUnit,
		BigDecimal carbonFactorG,
		BigDecimal shareRate
	) {
	}
}
