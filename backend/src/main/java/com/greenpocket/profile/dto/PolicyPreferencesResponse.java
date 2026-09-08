package com.greenpocket.profile.dto;

import java.time.LocalDate;
import java.util.List;

import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record PolicyPreferencesResponse(
	LocalDate birthDate,
	HousingType housingType,
	AreaBand areaBand,
	CurrentStatus currentStatus,
	AnnualIncomeBand annualIncomeBand,
	HouseholdStatus householdStatus,
	List<PolicyInterestCategory> interestCategories,
	EcoAddress ecoAddress,
	boolean regionEditable
) {

	public record EcoAddress(
		String label,
		String sidoCode,
		String sigunguCode
	) {
	}
}
