package com.greenpocket.bill.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record BillCreateRequest(
	@NotNull YearMonth billingMonth,
	@NotNull BillType billType,
	@NotNull InputSource inputSource,
	List<@NotNull @Valid Item> items
) {

	public record Item(
		@NotNull UtilityType utilityType,
		@NotNull @PositiveOrZero Long amount,
		@Digits(integer = 9, fraction = 3) BigDecimal usage,
		@NotNull UsageUnit usageUnit,
		@DecimalMin("0.0000") @DecimalMax("1.0000") @Digits(integer = 1, fraction = 4)
		BigDecimal confidence
	) {
	}
}
