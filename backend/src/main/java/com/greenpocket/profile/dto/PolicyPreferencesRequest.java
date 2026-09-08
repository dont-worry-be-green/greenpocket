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

public record PolicyPreferencesRequest(
	@Schema(example = "1998-03-15") LocalDate birthDate,
	@Schema(example = "ONE_ROOM") HousingType housingType,
	@Schema(example = "UNDER_10") AreaBand areaBand,
	@Schema(example = "FREELANCER") CurrentStatus currentStatus,
	@Schema(example = "UNDER_24M") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "ONE_PERSON") HouseholdStatus householdStatus,
	@Schema(example = "[\"HOUSING\", \"WELFARE_CULTURE\"]") List<PolicyInterestCategory> interestCategories
) {
}
