package com.greenpocket.eco.entity;

import java.math.BigDecimal;

public enum TargetTier {
	TIER_5("5~10%", new BigDecimal("5.000"), 10_000L),
	TIER_10("10~15%", new BigDecimal("10.000"), 30_000L),
	TIER_15("15% 이상", new BigDecimal("15.000"), 50_000L);

	private final String label;
	private final BigDecimal targetRate;
	private final Long mileage;

	TargetTier(String label, BigDecimal targetRate, Long mileage) {
		this.label = label;
		this.targetRate = targetRate;
		this.mileage = mileage;
	}

	public String label() {
		return label;
	}

	public BigDecimal targetRate() {
		return targetRate;
	}

	public Long mileage() {
		return mileage;
	}
}
