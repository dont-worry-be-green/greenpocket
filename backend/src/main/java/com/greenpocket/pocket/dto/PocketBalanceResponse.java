package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;

public record PocketBalanceResponse(
	Long balance,
	Long convertibleMileage,
	OffsetDateTime calculatedAt
) {
}
