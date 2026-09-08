package com.greenpocket.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileUpdateResponse(
	@Schema(example = "원룸 · 10평 이하") String profileSummary,
	@Schema(example = "true") boolean policyProfileCompleted,
	@Schema(example = "true") boolean recommendationsUpdated
) {
}
