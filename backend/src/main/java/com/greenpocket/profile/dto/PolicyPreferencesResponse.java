package com.greenpocket.profile.dto;

import java.time.LocalDate;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;

public record PolicyPreferencesResponse(
	LocalDate birthDate,
	CurrentStatus currentStatus,
	AnnualIncomeBand annualIncomeBand,
	HouseholdStatus householdStatus,
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
