package com.greenpocket.user.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserStartResponse(
	@Schema(example = "1") Long userId,
	@Schema(example = "김수현") String name,
	@Schema(example = "false") boolean onboardingCompleted,
	@Schema(example = "ONB-02") String nextScreen,
	@Schema(example = "1005-1234-5678-90") String pocketAccountNo,
	@Schema(example = "김수현") String pocketHolder,
	@Schema(example = "2026-09-03T18:30:00+09:00") OffsetDateTime createdAt
) {
}
