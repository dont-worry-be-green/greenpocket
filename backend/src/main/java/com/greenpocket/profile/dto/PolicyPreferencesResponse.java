package com.greenpocket.profile.dto;

import java.time.LocalDate;
import java.util.List;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record PolicyPreferencesResponse(
	LocalDate birthDate,
	CurrentStatus currentStatus,
	AnnualIncomeBand annualIncomeBand,
	EducationStatus educationStatus,
	List<PolicyInterestCategory> interestCategories,
	EcoAddress ecoAddress,
	boolean birthDateEditable,
	boolean regionEditable,
	boolean completed
) {

	public record EcoAddress(
		String label,
		String sidoCode,
		String sigunguCode
	) {
	}
}
