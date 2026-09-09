package com.greenpocket.profile.service;

import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.profile.dto.PolicyPreferencesRequest;
import com.greenpocket.profile.dto.PolicyPreferencesResponse;
import com.greenpocket.profile.dto.PolicyPreferencesUpdateResponse;
import com.greenpocket.profile.dto.ProfileResponse;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.profile.repository.ProfileRepository;
import com.greenpocket.profile.repository.ProfileRepository.ProfileSnapshot;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final ProfileRepository profileRepository;

	@Transactional(readOnly = true)
	public ProfileResponse find(Long userId) {
		ProfileSnapshot profile = findUser(userId);
		return new ProfileResponse(
			profile.name(), profile.birthDate(), profile.gender(), profile.phoneNumber(),
			profile.currentStatus(), profile.annualIncomeBand(), profile.educationStatus(),
			profile.interestCategories(),
			toProfileEcoAddress(profile),
			profile.policyProfileCompleted()
		);
	}

	@Transactional(readOnly = true)
	public PolicyPreferencesResponse findPolicyPreferences(Long userId) {
		ProfileSnapshot profile = findUser(userId);
		return new PolicyPreferencesResponse(
			profile.birthDate(), profile.currentStatus(), profile.annualIncomeBand(), profile.educationStatus(),
			profile.interestCategories(),
			toPreferencesEcoAddress(profile),
			false,
			false,
			profile.policyProfileCompleted()
		);
	}

	@Transactional
	public PolicyPreferencesUpdateResponse updatePolicyPreferences(Long userId, PolicyPreferencesRequest request) {
		validatePreferences(request);
		if (profileRepository.updatePolicyPreferences(
			userId, request.currentStatus(), request.annualIncomeBand(), request.educationStatus()
		) != 1) {
			throw unauthenticated();
		}
		profileRepository.replacePolicyInterests(userId, request.interestCategories());
		return new PolicyPreferencesUpdateResponse(true, true);
	}

	private static void validatePreferences(PolicyPreferencesRequest request) {
		if (request == null || request.currentStatus() == null || request.annualIncomeBand() == null
			|| request.educationStatus() == null || request.interestCategories() == null
			|| request.interestCategories().isEmpty() || request.interestCategories().size() > 2
			|| request.interestCategories().stream().anyMatch(java.util.Objects::isNull)
			|| request.interestCategories().stream().distinct().count() != request.interestCategories().size()) {
			throw new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE);
		}
	}

	private ProfileSnapshot findUser(Long userId) {
		return profileRepository.findByUserId(userId).orElseThrow(ProfileService::unauthenticated);
	}

	private static ProfileResponse.EcoAddress toProfileEcoAddress(ProfileSnapshot profile) {
		if (!policyRegionLinked(profile)) {
			return null;
		}
		return new ProfileResponse.EcoAddress(
			profile.ecoAddressLabel(), profile.ecoSidoCode(), profile.ecoSigunguCode(),
			profile.ecoAddressRegisteredAt() == null
				? null
				: YEAR_MONTH_FORMATTER.format(profile.ecoAddressRegisteredAt())
		);
	}

	private static PolicyPreferencesResponse.EcoAddress toPreferencesEcoAddress(ProfileSnapshot profile) {
		if (!policyRegionLinked(profile)) {
			return null;
		}
		return new PolicyPreferencesResponse.EcoAddress(
			profile.ecoAddressLabel(), profile.ecoSidoCode(), profile.ecoSigunguCode()
		);
	}

	private static boolean policyRegionLinked(ProfileSnapshot profile) {
		return profile.ecoLinkStatus() == EcoLinkStatus.LINKED
			&& profile.ecoSidoCode() != null
			&& profile.ecoSigunguCode() != null;
	}

	private static BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED);
	}
}
