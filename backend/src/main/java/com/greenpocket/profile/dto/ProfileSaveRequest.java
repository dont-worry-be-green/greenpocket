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

public record ProfileSaveRequest(
	@Schema(example = "1998-03-15") LocalDate birthDate,
	@Schema(example = "APARTMENT") HousingType housingType,
	@Schema(example = "OVER_20") AreaBand areaBand,
	@Schema(example = "EMPLOYED") CurrentStatus currentStatus,
	@Schema(example = "FROM_24M_TO_36M") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "ONE_PERSON") HouseholdStatus householdStatus,
	@Schema(example = "[\"JOB\", \"HOUSING\"]") List<PolicyInterestCategory> interestCategories
) {
}
