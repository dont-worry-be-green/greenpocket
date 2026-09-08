package com.greenpocket.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.policy.dto.PolicyPreviewRequest;
import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyMatchStatus;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.policy.repository.YouthPolicyRepository;
import com.greenpocket.policy.repository.YouthPolicyRepository.YouthPolicySnapshot;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.service.PolicyProfileQueryService;
import com.greenpocket.profile.service.PolicyProfileQueryService.PolicyProfile;

class PolicyQueryServiceTest {

	private static final Long USER_ID = 1L;
	private static final LocalDateTime SYNCED_AT = LocalDateTime.of(2026, 9, 8, 19, 30);

	private YouthPolicyRepository repository;
	private PolicyProfileQueryService profileQueryService;
	private PolicyQueryService service;

	@BeforeEach
	void setUp() {
		repository = mock(YouthPolicyRepository.class);
		profileQueryService = mock(PolicyProfileQueryService.class);
		service = new PolicyQueryService(repository, profileQueryService);
		when(repository.findLastSuccessfulSyncAt()).thenReturn(Optional.of(SYNCED_AT));
	}

	@Test
	void recommendsOnlyNationalPoliciesWhenEcoAddressIsNotLinked() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("NATIONAL", "전국 취업 지원", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("LOCAL", "관악 취업 지원", PolicyInterestCategory.JOB, "SIGUNGU:11620", 19, 39, false)
		));

		var response = service.getRecommendations(USER_ID, 0, 20);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().policyId()).isEqualTo("NATIONAL");
		assertThat(response.content().getFirst().matchStatus()).isEqualTo(PolicyMatchStatus.ELIGIBLE);
		assertThat(response.region().linked()).isFalse();
		assertThat(response.region().appliedLevels()).containsExactly(PolicyRegionLevel.NATIONAL);
	}

	@Test
	void includesLinkedSigunguAndMarksManualConditionsForReview() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("LOCAL", "관악 취업 지원", PolicyInterestCategory.JOB, "SIGUNGU:11620", 19, 39, true)
		));

		var response = service.getRecommendations(USER_ID, 0, 20);

		assertThat(response.content()).singleElement().satisfies(card -> {
			assertThat(card.matchStatus()).isEqualTo(PolicyMatchStatus.CHECK_REQUIRED);
			assertThat(card.matchReasons()).anyMatch(reason -> reason.contains("세부 조건"));
		});
		assertThat(response.region().linked()).isTrue();
	}

	@Test
	void previewUsesTemporaryValuesAndDoesNotRequirePersistence() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("EDU", "교육 지원", PolicyInterestCategory.EDUCATION, "SIGUNGU:11620", 19, 39, false)
		));
		PolicyPreviewRequest request = new PolicyPreviewRequest(
			LocalDate.of(1998, 3, 15), HousingType.ONE_ROOM, AreaBand.UNDER_10,
			CurrentStatus.STUDENT, AnnualIncomeBand.NO_INCOME, HouseholdStatus.WITH_PARENTS,
			List.of(PolicyInterestCategory.EDUCATION), 0, 20
		);

		var response = service.preview(USER_ID, request);

		assertThat(response.preview()).isTrue();
		assertThat(response.content()).extracting(card -> card.policyId()).containsExactly("EDU");
	}

	@Test
	void fullCatalogCanBeFilteredWithoutCompletedProfile() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.empty());
		when(repository.findAllActive()).thenReturn(List.of(
			policy("JOB", "취업 역량 지원", PolicyInterestCategory.JOB, "SIGUNGU:11620", 19, 39, false),
			policy("HOME", "주거 지원", PolicyInterestCategory.HOUSING, "SIGUNGU:11620", 19, 39, false)
		));

		var response = service.getAll(
			USER_ID, "역량", PolicyInterestCategory.JOB, "11620", PolicyApplicationStatus.OPEN, 0, 20
		);

		assertThat(response.content()).singleElement().satisfies(card -> {
			assertThat(card.policyId()).isEqualTo("JOB");
			assertThat(card.matchStatus()).isNull();
		});
	}

	@Test
	void rejectsMissingPolicyDataAndUnknownDetail() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));
		when(repository.findAllActive()).thenReturn(List.of());
		when(repository.findActiveByExternalId("UNKNOWN")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getRecommendations(USER_ID, 0, 20))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code())
					.isEqualTo("YOUTH_POLICY_DATA_UNAVAILABLE"));
		assertThatThrownBy(() -> service.getDetail(USER_ID, "UNKNOWN"))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code())
					.isEqualTo("YOUTH_POLICY_NOT_FOUND"));
	}

	private PolicyProfile profile(String sidoCode, String sigunguCode) {
		return new PolicyProfile(
			LocalDate.of(1998, 3, 15), HousingType.ONE_ROOM, AreaBand.UNDER_10,
			CurrentStatus.EMPLOYED, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON,
			List.of(PolicyInterestCategory.JOB), sidoCode, sigunguCode,
			sigunguCode == null ? null : "서울특별시 관악구"
		);
	}

	private YouthPolicySnapshot policy(
		String id,
		String title,
		PolicyInterestCategory category,
		String regionKeys,
		Integer minAge,
		Integer maxAge,
		boolean manualCondition
	) {
		return new YouthPolicySnapshot(
			1L, id, title, null, "설명", category.name(), "세부분류", category, "지원 내용",
			"주관기관", "운영기관", "0057001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
			null, "온라인", "https://example.go.kr", null, null, "N", minAge, maxAge,
			null, manualCondition ? "0043001" : null, null, null,
			manualCondition ? "세부 소득 확인" : null, null, null, null, null, null, null,
			PolicyApplicationStatus.OPEN, regionKeys, SYNCED_AT
		);
	}
}
