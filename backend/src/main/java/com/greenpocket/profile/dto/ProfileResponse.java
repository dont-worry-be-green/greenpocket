package com.greenpocket.profile.dto;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.user.entity.Gender;

public record ProfileResponse(
	@Schema(example = "김수현") String name,
	@Schema(example = "1998-03-15") LocalDate birthDate,
	@Schema(example = "FEMALE") Gender gender,
	@Schema(example = "01091740339") String phoneNumber,
	@Schema(example = "EMPLOYED") CurrentStatus currentStatus,
	@Schema(example = "FROM_24M_TO_36M") AnnualIncomeBand annualIncomeBand,
	@Schema(example = "UNIVERSITY_GRADUATE") EducationStatus educationStatus,
	List<PolicyInterestCategory> interestCategories,
	EcoAddress ecoAddress,
	@Schema(example = "true") boolean policyProfileCompleted
) {

	public record EcoAddress(
		@Schema(example = "서울특별시 관악구") String label,
		@Schema(example = "11") String sidoCode,
		@Schema(example = "11620") String sigunguCode,
		@Schema(example = "2026-03") String registeredAt
	) {
	}
}
