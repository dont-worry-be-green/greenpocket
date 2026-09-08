package com.greenpocket.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.profile.dto.PolicyPreferencesRequest;
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.repository.ProfileRepository;
import com.greenpocket.profile.repository.ProfileRepository.ProfileSnapshot;

class ProfileServiceTest {

	private ProfileRepository profileRepository;
	private ProfileService profileService;

	@BeforeEach
	void setUp() {
		profileRepository = mock(ProfileRepository.class);
		profileService = new ProfileService(profileRepository);
	}

	@Test
	void savesRequiredProfileAndInterestsWithoutResidenceInput() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(incompleteProfile()));
		when(profileRepository.update(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

		var response = profileService.save(1L, saveRequest());

		assertThat(response.onboardingCompleted()).isTrue();
		assertThat(response.policyProfileCompleted()).isTrue();
		assertThat(response.profileSummary()).isEqualTo("아파트 · 20평 이상");
		assertThat(response.nextScreen()).isEqualTo("WF-06");
		assertThat(response.policyRegionLinked()).isFalse();
		verify(profileRepository).replaceInterests(
			1L, List.of(PolicyInterestCategory.JOB, PolicyInterestCategory.HOUSING)
		);
	}

	@Test
	void rejectsMissingFutureBirthDateAndTooManyInterests() {
		assertThatThrownBy(() -> profileService.save(1L, new ProfileSaveRequest(
			null, HousingType.APARTMENT, AreaBand.OVER_20, CurrentStatus.EMPLOYED,
			AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON, List.of()
		)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code()).isEqualTo("PROFILE_INCOMPLETE"));

		assertThatThrownBy(() -> profileService.save(1L, new ProfileSaveRequest(
			LocalDate.now().plusDays(1), HousingType.APARTMENT, AreaBand.OVER_20, CurrentStatus.EMPLOYED,
			AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON, List.of()
		)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code()).isEqualTo("BIRTH_DATE_INVALID"));

		assertThatThrownBy(() -> profileService.save(1L, new ProfileSaveRequest(
			LocalDate.of(1998, 3, 15), HousingType.APARTMENT, AreaBand.OVER_20, CurrentStatus.EMPLOYED,
			AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON,
			List.of(
				PolicyInterestCategory.JOB, PolicyInterestCategory.HOUSING,
				PolicyInterestCategory.EDUCATION, PolicyInterestCategory.WELFARE_CULTURE
			)
		)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code())
					.isEqualTo("POLICY_INTEREST_LIMIT_EXCEEDED"));
	}

	@Test
	void findsCompletedProfileWithReadOnlyEcoAddress() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(completedLinkedProfile()));
		when(profileRepository.findInterestsByUserId(1L))
			.thenReturn(List.of(PolicyInterestCategory.JOB, PolicyInterestCategory.HOUSING));

		var response = profileService.find(1L);

		assertThat(response.birthDate()).isEqualTo(LocalDate.of(1998, 3, 15));
		assertThat(response.profileSummary()).isEqualTo("아파트 · 20평 이상");
		assertThat(response.ecoAddress().label()).isEqualTo("서울특별시 관악구");
		assertThat(response.ecoAddress().registeredAt()).isEqualTo("2026-03");
		assertThat(response.interestCategories())
			.containsExactly(PolicyInterestCategory.JOB, PolicyInterestCategory.HOUSING);
	}

	@Test
	void updatesProfileAndPolicyPreferences() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(completedLinkedProfile()));
		when(profileRepository.update(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
		ProfileUpdateRequest updateRequest = new ProfileUpdateRequest(
			"김그린", LocalDate.of(1998, 3, 15), HousingType.ONE_ROOM, AreaBand.UNDER_10,
			CurrentStatus.FREELANCER, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON,
			List.of(PolicyInterestCategory.HOUSING)
		);

		var updated = profileService.update(1L, updateRequest);
		var preferencesUpdated = profileService.updatePolicyPreferences(1L, new PolicyPreferencesRequest(
			LocalDate.of(1998, 3, 15), HousingType.ONE_ROOM, AreaBand.UNDER_10,
			CurrentStatus.FREELANCER, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON,
			List.of(PolicyInterestCategory.HOUSING)
		));

		assertThat(updated.profileSummary()).isEqualTo("원룸 · 10평 이하");
		assertThat(updated.recommendationsUpdated()).isTrue();
		assertThat(preferencesUpdated.policyProfileCompleted()).isTrue();
	}

	private ProfileSaveRequest saveRequest() {
		return new ProfileSaveRequest(
			LocalDate.of(1998, 3, 15), HousingType.APARTMENT, AreaBand.OVER_20,
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, HouseholdStatus.ONE_PERSON,
			List.of(PolicyInterestCategory.JOB, PolicyInterestCategory.HOUSING)
		);
	}

	private ProfileSnapshot incompleteProfile() {
		return new ProfileSnapshot(
			"김그린", null, null, null, null, null, null, false, false,
			EcoLinkStatus.UNLINKED, null, null, null, null
		);
	}

	private ProfileSnapshot completedLinkedProfile() {
		return new ProfileSnapshot(
			"김그린", LocalDate.of(1998, 3, 15), HousingType.APARTMENT, AreaBand.OVER_20,
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, HouseholdStatus.ONE_PERSON,
			true, true, EcoLinkStatus.LINKED, "11", "11620", "서울특별시 관악구",
			LocalDate.of(2026, 3, 1)
		);
	}
}
