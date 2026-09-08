package com.greenpocket.profile.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.repository.ProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyProfileQueryService {

	private final ProfileRepository profileRepository;

	public Optional<PolicyProfile> findCompleted(Long userId) {
		return profileRepository.findByUserId(userId)
			.filter(profile -> profile.onboardingCompleted() && profile.policyProfileCompleted())
			.filter(profile -> profile.birthDate() != null && profile.housingType() != null
				&& profile.areaBand() != null && profile.currentStatus() != null
				&& profile.annualIncomeBand() != null && profile.householdStatus() != null)
			.map(profile -> new PolicyProfile(
				profile.birthDate(),
				profile.housingType(),
				profile.areaBand(),
				profile.currentStatus(),
				profile.annualIncomeBand(),
				profile.householdStatus(),
				profileRepository.findInterestsByUserId(userId),
				profile.ecoLinkStatus() == EcoLinkStatus.LINKED ? profile.ecoSidoCode() : null,
				profile.ecoLinkStatus() == EcoLinkStatus.LINKED ? profile.ecoSigunguCode() : null,
				profile.ecoLinkStatus() == EcoLinkStatus.LINKED ? profile.ecoAddressLabel() : null
			));
	}

	public record PolicyProfile(
		LocalDate birthDate,
		HousingType housingType,
		AreaBand areaBand,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		HouseholdStatus householdStatus,
		List<PolicyInterestCategory> interestCategories,
		String ecoSidoCode,
		String ecoSigunguCode,
		String ecoAddressLabel
	) {
		public boolean regionLinked() {
			return ecoSidoCode != null && ecoSigunguCode != null;
		}
	}
}
