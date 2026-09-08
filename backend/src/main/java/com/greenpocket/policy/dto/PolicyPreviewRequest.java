package com.greenpocket.policy.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record PolicyPreviewRequest(
	@Schema(example = "1998-03-15") LocalDate birthDate,
	@Schema(example = "ONE_ROOM") HousingType housingType,
	@Schema(example = "UNDER_10") AreaBand areaBand,
	@Schema(example = "UNEMPLOYED") CurrentStatus currentStatus,
	@Schema(example = "NO_INCOME") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "ONE_PERSON") HouseholdStatus householdStatus,
	@Schema(example = "[\"JOB\", \"HOUSING\"]") List<PolicyInterestCategory> interestCategories,
	@Schema(example = "0") Integer page,
	@Schema(example = "20") Integer size
) {
}
