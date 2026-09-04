package com.greenpocket.bill.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BillUpdateRequest(
	@NotNull @PositiveOrZero Long amount,
	@NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal usage
) {
}
