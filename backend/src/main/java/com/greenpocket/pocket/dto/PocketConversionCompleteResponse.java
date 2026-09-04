package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.pocket.entity.TransactionStatus;

public record PocketConversionCompleteResponse(
	@Schema(example = "120") Long conversionId,
	@Schema(example = "COMPLETED") TransactionStatus transactionStatus,
	@Schema(example = "30000") Long amount,
	@Schema(example = "2026-09-03T19:01:00+09:00") OffsetDateTime completedAt,
	@Schema(example = "94000") Long balanceAfter,
	Transaction transaction
) {

	public record Transaction(
		@Schema(example = "120") Long transactionId,
		@Schema(example = "GP-2609-0021") String transactionCode,
		@Schema(example = "에코마일리지 2026 상반기") String label
	) {
	}
}
