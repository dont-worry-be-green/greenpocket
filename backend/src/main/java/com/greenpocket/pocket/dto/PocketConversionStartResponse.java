package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.pocket.entity.TransactionStatus;

public record PocketConversionStartResponse(
	@Schema(example = "120") Long conversionId,
	@Schema(example = "7") Long roundId,
	@Schema(example = "30000") Long amount,
	@Schema(example = "REQUESTED") TransactionStatus transactionStatus,
	@Schema(example = "https://ecomileage.seoul.go.kr/mileage/convert") String externalUrl,
	@Schema(example = "2026-09-03T18:58:00+09:00") OffsetDateTime requestedAt,
	@Schema(example = "현금으로 바꿔야 그린포켓 계좌로 들어와요") String notice
) {
}
