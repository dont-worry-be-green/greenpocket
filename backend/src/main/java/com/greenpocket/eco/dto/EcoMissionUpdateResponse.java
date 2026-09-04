package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.util.List;

public record EcoMissionUpdateResponse(
	Long roundId,
	BigDecimal combinedMissionRate,
	List<Item> items,
	boolean todayMissionsUpdated
) {

	public record Item(
		Long missionId,
		BigDecimal computedRate,
		boolean counted,
		String exclusionReason
	) {
	}
}
