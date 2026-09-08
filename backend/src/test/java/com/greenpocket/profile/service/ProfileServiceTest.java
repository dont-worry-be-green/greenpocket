package com.greenpocket.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.profile.dto.PolicyPreferencesRequest;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.repository.ProfileRepository;
import com.greenpocket.profile.repository.ProfileRepository.ProfileSnapshot;
import com.greenpocket.user.entity.Gender;

class ProfileServiceTest {

	private ProfileRepository profileRepository;
	private ProfileService profileService;

	@BeforeEach
	void setUp() {
		profileRepository = mock(ProfileRepository.class);
		profileService = new ProfileService(profileRepository);
	}

	@Test
	void findsIdentityAndReadOnlyEcoAddressBeforePolicyPreferences() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(completedLinkedProfile()));

		var response = profileService.find(1L);

		assertThat(response.birthDate()).isEqualTo(LocalDate.of(1998, 3, 15));
		assertThat(response.gender()).isEqualTo(Gender.FEMALE);
		assertThat(response.phoneNumber()).isEqualTo("01091740339");
		assertThat(response.ecoAddress().label()).isEqualTo("서울특별시 관악구");
		assertThat(response.ecoAddress().registeredAt()).isEqualTo("2026-03");
	}

	@Test
	void updatesOnlyPolicyPreferences() {
		when(profileRepository.updatePolicyPreferences(
			1L, CurrentStatus.FREELANCER, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON
		)).thenReturn(1);
		var preferencesUpdated = profileService.updatePolicyPreferences(1L, new PolicyPreferencesRequest(
			CurrentStatus.FREELANCER, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON
		));

		assertThat(preferencesUpdated.policyProfileCompleted()).isTrue();
		verify(profileRepository).updatePolicyPreferences(
			1L, CurrentStatus.FREELANCER, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON
		);
	}

	@Test
	void rejectsIncompletePolicyPreferences() {
		assertThatThrownBy(() -> profileService.updatePolicyPreferences(1L,
			new PolicyPreferencesRequest(null, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code()).isEqualTo("PROFILE_INCOMPLETE"));
	}

	private ProfileSnapshot completedLinkedProfile() {
		return new ProfileSnapshot(
			"김그린", LocalDate.of(1998, 3, 15), Gender.FEMALE, "01091740339",
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, HouseholdStatus.ONE_PERSON,
			true, EcoLinkStatus.LINKED, "11", "11620", "서울특별시 관악구",
			LocalDate.of(2026, 3, 1)
		);
	}
}
