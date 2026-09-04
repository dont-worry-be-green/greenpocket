package com.greenpocket.eco.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.greenpocket.eco.entity.RoundStatus;

public record EcoGoalSaveResponse(
	Long roundId,
	OffsetDateTime goalSetAt,
	RoundStatus roundStatus,
	BigDecimal combinedTargetRate,
	Long expectedMileage,
	Long expectedSavingAmount,
	int savedMissionCount,
	String nextScreen
) {
}
