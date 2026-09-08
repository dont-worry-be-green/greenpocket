package com.greenpocket.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileSaveResponse(
	@Schema(example = "true") boolean onboardingCompleted,
	@Schema(example = "true") boolean policyProfileCompleted,
	@Schema(example = "아파트 · 20평 이상") String profileSummary,
	@Schema(example = "WF-06") String nextScreen,
	@Schema(example = "false") boolean policyRegionLinked
) {
}
