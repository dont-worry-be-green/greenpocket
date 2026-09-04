package com.greenpocket.pocket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public record PocketConversionRequest(
	@Schema(description = "전환할 확정 마일리지 평가 회차 ID", example = "7")
	@NotNull @Positive Long roundId,

	@Schema(description = "취소 불가 안내 확인 및 전환 동의", example = "true")
	@NotNull Boolean agreed
) {
}
