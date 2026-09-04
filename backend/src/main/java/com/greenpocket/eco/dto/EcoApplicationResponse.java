package com.greenpocket.eco.dto;

import java.time.OffsetDateTime;

import com.greenpocket.eco.entity.ApplicationStatus;

public record EcoApplicationResponse(
	Long roundId,
	ApplicationStatus applicationStatus,
	OffsetDateTime appliedAt,
	boolean showBanner
) {
}
