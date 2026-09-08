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
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
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
			assertThat(card.matchScore()).isLessThan(100);
			assertThat(card.matchReasons()).anyMatch(reason -> reason.contains("세부 조건"));
		});
		assertThat(response.region().linked()).isTrue();
	}

	@Test
	void keepsNationalReasonAfterEcoAddressIsLinked() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("NATIONAL", "전국 취업 지원", PolicyInterestCategory.JOB,
				"NATIONAL:00000", 19, 39, false)
		));

		var response = service.getRecommendations(USER_ID, 0, 20);

		assertThat(response.content()).singleElement().satisfies(card -> {
			assertThat(card.matchReasons()).contains("전국 대상 정책이에요");
			assertThat(card.matchReasons()).doesNotContain("에코마일리지 연동 지역과 일치해요");
		});
	}

	@Test
	void filtersPoliciesByStructuredEmploymentCode() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policyWithConditions("EMPLOYED", "재직자 지원", "NATIONAL:00000",
				null, null, null, null, null, "0013001", null),
			policyWithConditions("UNEMPLOYED", "미취업자 지원", "NATIONAL:00000",
				null, null, null, null, null, "0013003", null)
		));

		var response = service.getRecommendations(USER_ID, 0, 20);

		assertThat(response.content()).extracting(card -> card.policyId()).containsExactly("EMPLOYED");
		assertThat(response.content().getFirst().matchReasons())
			.contains("현재 상태가 정책의 취업 조건과 일치해요");
	}

	@Test
	void previewRecalculatesWithTemporaryEmploymentStatus() {
		when(profileQueryService.find(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policyWithConditions("EMPLOYED", "재직자 지원", "NATIONAL:00000",
				null, null, null, null, null, "0013001", null),
			policyWithConditions("UNEMPLOYED", "미취업자 지원", "NATIONAL:00000",
				null, null, null, null, null, "0013003", null)
		));

		var response = service.preview(USER_ID, new PolicyPreviewRequest(
			CurrentStatus.UNEMPLOYED, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON, 0, 20
		));

		assertThat(response.content()).extracting(card -> card.policyId()).containsExactly("UNEMPLOYED");
	}

	@Test
	void filtersOnlyWhenIncomeBandClearlyMissesStructuredRange() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M, HouseholdStatus.ONE_PERSON,
			"11", "11620"
		)));
		when(repository.findAllActive()).thenReturn(List.of(
			policyWithConditions("LOW", "저소득 지원", "NATIONAL:00000",
				null, "0043002", null, 2_399L, null, null, null),
			policyWithConditions("MATCH", "중간소득 지원", "NATIONAL:00000",
				null, "0043002", 2_400L, 5_000L, null, null, null),
			policyWithConditions("PARTIAL", "경계소득 지원", "NATIONAL:00000",
				null, "0043002", 3_000L, 5_000L, null, null, null)
		));

		var response = service.getRecommendations(USER_ID, 0, 20);

		assertThat(response.content()).extracting(card -> card.policyId())
			.containsExactly("MATCH", "PARTIAL");
		assertThat(response.content().get(0).matchReasons())
			.contains("연소득 구간이 정책의 소득 조건과 일치해요");
		assertThat(response.content().get(1).matchStatus()).isEqualTo(PolicyMatchStatus.CHECK_REQUIRED);
	}

	@Test
	void filtersMarriedUserFromUnmarriedOnlyPolicy() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.UNDER_24M, HouseholdStatus.MARRIED,
			"11", "11620"
		)));
		when(repository.findAllActive()).thenReturn(List.of(
			policyWithConditions("MARRIED", "기혼 지원", "NATIONAL:00000",
				"0055001", null, null, null, null, null, null),
			policyWithConditions("UNMARRIED", "미혼 지원", "NATIONAL:00000",
				"0055002", null, null, null, null, null, null)
		));

		var response = service.getRecommendations(USER_ID, 0, 20);

		assertThat(response.content()).extracting(card -> card.policyId()).containsExactly("MARRIED");
		assertThat(response.content().getFirst().matchReasons())
			.contains("가구 상태가 정책 조건과 일치해요");
	}

	@Test
	void previewUsesTemporaryValuesAndDoesNotRequirePersistence() {
		when(profileQueryService.find(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("EDU", "교육 지원", PolicyInterestCategory.EDUCATION, "SIGUNGU:11620", 19, 39, false)
		));
		PolicyPreviewRequest request = new PolicyPreviewRequest(
			CurrentStatus.STUDENT, AnnualIncomeBand.NO_INCOME, HouseholdStatus.WITH_PARENTS, 0, 20
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
		return profile(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.UNDER_24M, HouseholdStatus.ONE_PERSON,
			sidoCode, sigunguCode
		);
	}

	private PolicyProfile profile(
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		HouseholdStatus householdStatus,
		String sidoCode,
		String sigunguCode
	) {
		return new PolicyProfile(
			LocalDate.of(1998, 3, 15),
			currentStatus, annualIncomeBand, householdStatus,
			sidoCode, sigunguCode,
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
		return policyWithConditions(
			id, title, regionKeys, minAge, maxAge,
			null, manualCondition ? "0043001" : null, null, null,
			manualCondition ? "세부 소득 확인" : null, null, null,
			category
		);
	}

	private YouthPolicySnapshot policyWithConditions(
		String id,
		String title,
		String regionKeys,
		String marriageCode,
		String incomeCode,
		Long incomeMin,
		Long incomeMax,
		String incomeText,
		String employmentCodes,
		String specialCodes
	) {
		return policyWithConditions(
			id, title, regionKeys, 19, 39, marriageCode, incomeCode, incomeMin, incomeMax,
			incomeText, employmentCodes, specialCodes, PolicyInterestCategory.JOB
		);
	}

	private YouthPolicySnapshot policyWithConditions(
		String id,
		String title,
		String regionKeys,
		Integer minAge,
		Integer maxAge,
		String marriageCode,
		String incomeCode,
		Long incomeMin,
		Long incomeMax,
		String incomeText,
		String employmentCodes,
		String specialCodes,
		PolicyInterestCategory category
	) {
		return new YouthPolicySnapshot(
			1L, id, title, null, "설명", category.name(), "세부분류", category, "지원 내용",
			"주관기관", "운영기관", "0057001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
			null, "온라인", "https://example.go.kr", null, null, "N", minAge, maxAge,
			marriageCode, incomeCode, incomeMin, incomeMax,
			incomeText, null, null, null, employmentCodes, null, specialCodes,
			PolicyApplicationStatus.OPEN, regionKeys, SYNCED_AT
		);
	}
}
