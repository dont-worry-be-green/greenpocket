package com.greenpocket.eco.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

final class EcoMockData {

	static final LocalDate PERIOD_START = LocalDate.of(2026, 4, 1);
	static final LocalDate PERIOD_END = LocalDate.of(2026, 9, 1);
	static final LocalDateTime LINKED_AT = LocalDateTime.of(2026, 9, 1, 9, 0);
	static final LocalDate ADDRESS_REGISTERED_AT = LocalDate.of(2026, 3, 1);
	static final Long TOTAL_AMOUNT = 420_600L;
	static final BigDecimal TOTAL_CARBON_G = new BigDecimal("831992.000");

	static final List<UtilityBaseline> UTILITIES = List.of(
		new UtilityBaseline(
			UtilityType.ELECTRICITY,
			268_000L,
			new BigDecimal("1340.000"),
			new BigDecimal("424.000"),
			new BigDecimal("64.000"),
			UsageUnit.kWh
		),
		new UtilityBaseline(
			UtilityType.GAS,
			96_600L,
			new BigDecimal("108.000"),
			new BigDecimal("2240.000"),
			new BigDecimal("23.000"),
			UsageUnit.m3
		),
		new UtilityBaseline(
			UtilityType.WATER,
			56_000L,
			new BigDecimal("66.000"),
			new BigDecimal("332.000"),
			new BigDecimal("13.000"),
			UsageUnit.m3
		)
	);

	private EcoMockData() {
	}

	record UtilityBaseline(
		UtilityType utilityType,
		Long amount,
		BigDecimal usage,
		BigDecimal carbonFactorG,
		BigDecimal shareRate,
		UsageUnit usageUnit
	) {
		Long monthlyAmount(int monthIndex) {
			long quotient = amount / 6;
			long remainder = amount % 6;
			return monthIndex % 6 == 5 ? quotient + remainder : quotient;
		}

		BigDecimal monthlyUsage(int monthIndex) {
			BigDecimal quotient = usage.divide(new BigDecimal("6"), 3, java.math.RoundingMode.DOWN);
			BigDecimal remainder = usage.subtract(quotient.multiply(new BigDecimal("6")));
			return monthIndex % 6 == 5 ? quotient.add(remainder) : quotient;
		}
	}
}
