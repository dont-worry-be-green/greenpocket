package com.greenpocket.profile.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record PolicyPreferencesRequest(
	@Schema(example = "FREELANCER") CurrentStatus currentStatus,
	@Schema(example = "UNDER_24M") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "UNIVERSITY_GRADUATE") EducationStatus educationStatus,
	@Schema(description = "관심 분야 1~2개", example = "[\"JOB\", \"HOUSING\"]")
	List<PolicyInterestCategory> interestCategories
) {
}
