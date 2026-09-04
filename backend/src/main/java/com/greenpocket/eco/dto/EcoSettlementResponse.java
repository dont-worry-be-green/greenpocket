package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.eco.entity.TargetTier;

public record EcoSettlementResponse(
	Long roundId,
	String periodStart,
	String periodEnd,
	Long confirmedMileage,
	String statusLabel,
	BigDecimal cumulativeRate,
	TargetTier tier,
	Calculation calculation,
	boolean isCash,
	boolean convertible,
	String externalUrl,
	List<String> otherUses
) {

	public record Calculation(
		Long baselineAmount,
		Long actualAmount,
		Long savedAmount,
		String note
	) {
	}
}
