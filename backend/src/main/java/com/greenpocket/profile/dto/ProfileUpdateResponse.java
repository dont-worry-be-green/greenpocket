package com.greenpocket.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileUpdateResponse(
	@Schema(example = "서울 관악구 · 원룸 10평 이하") String profileSummary,
	@Schema(example = "true") boolean baselineRecalculated,
	@Schema(example = "7", nullable = true) Long affectedRoundId
) {
}
