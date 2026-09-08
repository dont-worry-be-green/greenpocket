package com.greenpocket.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;

public record PolicyPreviewRequest(
	@Schema(example = "UNEMPLOYED") CurrentStatus currentStatus,
	@Schema(example = "NO_INCOME") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "ONE_PERSON") HouseholdStatus householdStatus,
	@Schema(example = "0") Integer page,
	@Schema(example = "20") Integer size
) {
}
