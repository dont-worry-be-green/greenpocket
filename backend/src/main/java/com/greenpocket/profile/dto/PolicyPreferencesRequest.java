package com.greenpocket.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;

public record PolicyPreferencesRequest(
	@Schema(example = "FREELANCER") CurrentStatus currentStatus,
	@Schema(example = "UNDER_24M") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "ONE_PERSON") HouseholdStatus householdStatus
) {
}
