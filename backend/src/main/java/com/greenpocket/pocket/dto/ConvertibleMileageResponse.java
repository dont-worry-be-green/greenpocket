package com.greenpocket.pocket.dto;

import java.util.List;

public record ConvertibleMileageResponse(
	Long convertibleMileage,
	List<Round> rounds,
	boolean convertible,
	BlockReason blockReason
) {
	public ConvertibleMileageResponse {
		rounds = List.copyOf(rounds);
	}

	public record Round(
		Long roundId,
		String periodStart,
		String periodEnd,
		Long confirmedMileage
	) {
	}

	public enum BlockReason {
		NO_MILEAGE,
		DAILY_LIMIT
	}
}
