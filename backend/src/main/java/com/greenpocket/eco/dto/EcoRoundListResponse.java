package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.eco.entity.RoundStatus;

public record EcoRoundListResponse(
	List<Item> content
) {

	public record Item(
		Long roundId,
		String periodStart,
		String periodEnd,
		RoundStatus roundStatus,
		BigDecimal finalRate,
		Long confirmedMileage
	) {
	}
}
