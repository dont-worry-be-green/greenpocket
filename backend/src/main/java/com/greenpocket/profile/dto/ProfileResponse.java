package com.greenpocket.profile.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record ProfileResponse(
	@Schema(example = "김수현") String name,
	@Schema(example = "1998-03-15") LocalDate birthDate,
	@Schema(example = "APARTMENT") HousingType housingType,
	@Schema(example = "OVER_20") AreaBand areaBand,
	@Schema(example = "EMPLOYED") CurrentStatus currentStatus,
	@Schema(example = "FROM_24M_TO_36M") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "ONE_PERSON") HouseholdStatus householdStatus,
	List<PolicyInterestCategory> interestCategories,
	EcoAddress ecoAddress,
	@Schema(example = "아파트 · 20평 이상") String profileSummary,
	@Schema(example = "true") boolean policyProfileCompleted,
	@Schema(example = "true") boolean onboardingCompleted
) {

	public record EcoAddress(
		@Schema(example = "서울특별시 관악구") String label,
		@Schema(example = "11") String sidoCode,
		@Schema(example = "11620") String sigunguCode,
		@Schema(example = "2026-03") String registeredAt
	) {
	}
}
