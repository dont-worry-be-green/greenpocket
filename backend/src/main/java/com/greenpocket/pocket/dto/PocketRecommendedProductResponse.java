package com.greenpocket.pocket.dto;

import java.time.LocalDate;
import java.util.List;

public record PocketRecommendedProductResponse(
	String productCode,
	String name,
	String tagline,
	Recommendation recommendation,
	String productType,
	MonthlyDeposit monthlyDeposit,
	List<Integer> contractTermsMonths,
	List<String> preferentialMissions,
	LocalDate informationBaseDate,
	String applicationUrl,
	String notice
) {
	public PocketRecommendedProductResponse {
		contractTermsMonths = List.copyOf(contractTermsMonths);
		preferentialMissions = List.copyOf(preferentialMissions);
	}

	public record Recommendation(
		String badge,
		String title,
		String description
	) {
	}

	public record MonthlyDeposit(
		Long minimumAmount,
		Long maximumAmount
	) {
	}
}
