package com.greenpocket.user.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.eco.entity.EcoLinkStatus;

public record UserBootstrapResponse(
	@Schema(example = "1") Long userId,
	@Schema(example = "김수현") String name,
	@Schema(example = "true") boolean onboardingCompleted,
	@Schema(example = "LINKED") EcoLinkStatus ecoLinkStatus,
	@Schema(example = "2026-09-01T09:00:00+09:00") OffsetDateTime ecoLinkedAt,
	@Schema(example = "true") boolean greenlifeParticipating,
	@Schema(example = "2026-09-01T09:12:00+09:00") OffsetDateTime greenlifeLinkedAt,
	@Schema(example = "true") boolean hasBill,
	@Schema(example = "7", nullable = true) Long currentRoundId,
	@Schema(example = "WF-06") String entryScreen
) {
}
