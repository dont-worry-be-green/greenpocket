package com.greenpocket.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.service.EcoCurrentRoundQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.dto.ProfileUpdateResponse;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.repository.ProfileRepository;
import com.greenpocket.profile.repository.ProfileRepository.ProfileSnapshot;

class ProfileServiceTest {

	private ProfileRepository profileRepository;
	private EcoCurrentRoundQueryService ecoRoundQueryService;
	private ProfileService profileService;

	@BeforeEach
	void setUp() {
		profileRepository = mock(ProfileRepository.class);
		ecoRoundQueryService = mock(EcoCurrentRoundQueryService.class);
		profileService = new ProfileService(
			profileRepository,
			new com.greenpocket.user.service.SeoulRegionCatalog(new ObjectMapper()),
			ecoRoundQueryService
		);
	}

	@Test
	void savesCatalogNamesAndCompletesOnboarding() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(incompleteProfile()));
		when(profileRepository.update(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

		ProfileSaveResponse response = profileService.save(1L, new ProfileSaveRequest(
			"11", "임의 이름", "11620", "임의 구", HousingType.APARTMENT, AreaBand.OVER_20
		));

		assertThat(response.onboardingCompleted()).isTrue();
		assertThat(response.profileSummary()).isEqualTo("서울 관악구 · 아파트 20평 이상");
		assertThat(response.nextScreen()).isEqualTo("WF-06");
		assertThat(response.seoulResident()).isTrue();
		verify(profileRepository).update(
			1L, "김그린", "11", "서울특별시", "11620", "관악구", HousingType.APARTMENT, AreaBand.OVER_20
		);
	}

	@Test
	void rejectsIncompleteOrUnknownRegion() {
		assertThatThrownBy(() -> profileService.save(1L,
			new ProfileSaveRequest("11", null, null, null, HousingType.APARTMENT, AreaBand.OVER_20)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code()).isEqualTo("PROFILE_INCOMPLETE"));

		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(incompleteProfile()));
		assertThatThrownBy(() -> profileService.save(1L,
			new ProfileSaveRequest("11", null, "99999", null, HousingType.APARTMENT, AreaBand.OVER_20)))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code()).isEqualTo("REGION_NOT_FOUND"));
	}

	@Test
	void findsCompletedProfile() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(completedProfile()));

		var response = profileService.find(1L);

		assertThat(response.sigunguName()).isEqualTo("관악구");
		assertThat(response.profileSummary()).isEqualTo("서울 관악구 · 아파트 20평 이상");
		assertThat(response.seoulResident()).isTrue();
	}

	@Test
	void warnsBeforeChangingRegionDuringActiveRound() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(completedProfile()));
		when(ecoRoundQueryService.findGoalActiveRoundId(1L)).thenReturn(Optional.of(7L));

		assertThatThrownBy(() -> profileService.update(1L, updateRequest("11710", false)))
			.isInstanceOfSatisfying(BusinessException.class, exception -> {
				assertThat(exception.getErrorCode().code()).isEqualTo("CONFLICT");
				assertThat(exception.getField()).isEqualTo("confirmBaselineChange");
				assertThat(exception.getDetails()).containsEntry("affectedRoundId", 7L);
			});
		verify(profileRepository, never()).update(any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void changesRegionAfterExplicitConfirmation() {
		when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(completedProfile()));
		when(ecoRoundQueryService.findGoalActiveRoundId(1L)).thenReturn(Optional.of(7L));
		when(profileRepository.update(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

		ProfileUpdateResponse response = profileService.update(1L, updateRequest("11710", true));

		assertThat(response.baselineRecalculated()).isTrue();
		assertThat(response.affectedRoundId()).isEqualTo(7L);
		assertThat(response.profileSummary()).startsWith("서울 송파구");
	}

	private ProfileUpdateRequest updateRequest(String sigunguCode, boolean confirm) {
		return new ProfileUpdateRequest(
			"김그린", "11", null, sigunguCode, null,
			HousingType.APARTMENT, AreaBand.OVER_20, confirm
		);
	}

	private ProfileSnapshot incompleteProfile() {
		return new ProfileSnapshot("김그린", null, null, null, null, null, null, false);
	}

	private ProfileSnapshot completedProfile() {
		return new ProfileSnapshot(
			"김그린", "11", "서울특별시", "11620", "관악구",
			HousingType.APARTMENT, AreaBand.OVER_20, true
		);
	}
}
