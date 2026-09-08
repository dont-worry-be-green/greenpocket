package com.greenpocket.profile.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

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
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.dto.ProfileUpdateResponse;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.profile.repository.ProfileRepository;
import com.greenpocket.profile.repository.ProfileRepository.ProfileSnapshot;
import com.greenpocket.user.exception.UserErrorCode;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private static final String NEXT_SCREEN = "WF-06";
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final ProfileRepository profileRepository;

	@Transactional
	public ProfileSaveResponse save(Long userId, ProfileSaveRequest request) {
		ValidatedProfile profile = validate(
			request.birthDate(), request.housingType(), request.areaBand(), request.currentStatus(),
			request.annualIncomeBand(), request.householdStatus(), request.interestCategories()
		);
		ProfileSnapshot current = findUser(userId);
		update(userId, current.name(), profile);
		return new ProfileSaveResponse(
			true,
			true,
			profileSummary(profile.housingType(), profile.areaBand()),
			NEXT_SCREEN,
			policyRegionLinked(current)
		);
	}

	@Transactional(readOnly = true)
	public ProfileResponse find(Long userId) {
		ProfileSnapshot profile = findUser(userId);
		validateComplete(profile);
		return new ProfileResponse(
			profile.name(), profile.birthDate(), profile.housingType(), profile.areaBand(),
			profile.currentStatus(), profile.annualIncomeBand(), profile.householdStatus(),
			profileRepository.findInterestsByUserId(userId),
			toProfileEcoAddress(profile),
			profileSummary(profile.housingType(), profile.areaBand()),
			profile.policyProfileCompleted(), profile.onboardingCompleted()
		);
	}

	@Transactional
	public ProfileUpdateResponse update(Long userId, ProfileUpdateRequest request) {
		ValidatedProfile profile = validate(
			request.birthDate(), request.housingType(), request.areaBand(), request.currentStatus(),
			request.annualIncomeBand(), request.householdStatus(), request.interestCategories()
		);
		ProfileSnapshot current = findUser(userId);
		String name = request.name() == null ? current.name() : normalizeAndValidateName(request.name());
		update(userId, name, profile);
		return new ProfileUpdateResponse(
			profileSummary(profile.housingType(), profile.areaBand()),
			true,
			true
		);
	}

	@Transactional(readOnly = true)
	public PolicyPreferencesResponse findPolicyPreferences(Long userId) {
		ProfileSnapshot profile = findUser(userId);
		validateComplete(profile);
		return new PolicyPreferencesResponse(
			profile.birthDate(), profile.housingType(), profile.areaBand(), profile.currentStatus(),
			profile.annualIncomeBand(), profile.householdStatus(),
			profileRepository.findInterestsByUserId(userId),
			toPreferencesEcoAddress(profile),
			false
		);
	}

	@Transactional
	public PolicyPreferencesUpdateResponse updatePolicyPreferences(Long userId, PolicyPreferencesRequest request) {
		ValidatedProfile profile = validate(
			request.birthDate(), request.housingType(), request.areaBand(), request.currentStatus(),
			request.annualIncomeBand(), request.householdStatus(), request.interestCategories()
		);
		ProfileSnapshot current = findUser(userId);
		update(userId, current.name(), profile);
		return new PolicyPreferencesUpdateResponse(true, true);
	}

	private void update(Long userId, String name, ValidatedProfile profile) {
		if (profileRepository.update(
			userId, name, profile.birthDate(), profile.housingType(), profile.areaBand(),
			profile.currentStatus(), profile.annualIncomeBand(), profile.householdStatus()
		) != 1) {
			throw unauthenticated();
		}
		profileRepository.replaceInterests(userId, profile.interestCategories());
	}

	private static ValidatedProfile validate(
		LocalDate birthDate,
		HousingType housingType,
		AreaBand areaBand,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		HouseholdStatus householdStatus,
		List<PolicyInterestCategory> interestCategories
	) {
		if (birthDate == null || housingType == null || areaBand == null || currentStatus == null
			|| annualIncomeBand == null || householdStatus == null) {
			throw new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE);
		}
		if (birthDate.isAfter(LocalDate.now())) {
			throw new BusinessException(ProfileErrorCode.BIRTH_DATE_INVALID, "birthDate", null);
		}

		List<PolicyInterestCategory> interests = interestCategories == null ? List.of() : interestCategories;
		if (interests.size() > 3) {
			throw new BusinessException(ProfileErrorCode.POLICY_INTEREST_LIMIT_EXCEEDED, "interestCategories", null);
		}
		if (interests.stream().anyMatch(java.util.Objects::isNull)
			|| new LinkedHashSet<>(interests).size() != interests.size()) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "interestCategories", null);
		}
		return new ValidatedProfile(
			birthDate, housingType, areaBand, currentStatus, annualIncomeBand, householdStatus,
			List.copyOf(interests)
		);
	}

	private static void validateComplete(ProfileSnapshot profile) {
		if (!profile.onboardingCompleted() || !profile.policyProfileCompleted() || profile.birthDate() == null
			|| profile.housingType() == null || profile.areaBand() == null || profile.currentStatus() == null
			|| profile.annualIncomeBand() == null || profile.householdStatus() == null) {
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

	private static String normalizeAndValidateName(String rawName) {
		String name = rawName.strip();
		int characterCount = name.codePointCount(0, name.length());
		boolean hasLetterOrDigit = name.codePoints().anyMatch(Character::isLetterOrDigit);
		if (characterCount < 1 || characterCount > 20 || !hasLetterOrDigit) {
			throw new BusinessException(UserErrorCode.NAME_INVALID, "name", null);
		}
		return name;
	}

	private static String profileSummary(HousingType housingType, AreaBand areaBand) {
		return housingType.label() + " · " + areaBand.label();
	}

	private static BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED);
	}

	private record ValidatedProfile(
		LocalDate birthDate,
		HousingType housingType,
		AreaBand areaBand,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		HouseholdStatus householdStatus,
		List<PolicyInterestCategory> interestCategories
	) {
	}
}
