package com.greenpocket.bill.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BillUpdateResponse(
	Long recordId,
	Long amount,
	BigDecimal usage,
	OffsetDateTime updatedAt,
	BillRecalculatedResponse recalculated
) {
}
