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
import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyMatchStatus;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.policy.repository.YouthPolicyRepository;
import com.greenpocket.policy.repository.YouthPolicyRepository.YouthPolicySnapshot;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
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

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().policyId()).isEqualTo("NATIONAL");
		assertThat(response.content().getFirst().matchStatus()).isEqualTo(PolicyMatchStatus.ELIGIBLE);
		assertThat(response.region().linked()).isFalse();
		assertThat(response.region().appliedLevels()).containsExactly(PolicyRegionLevel.NATIONAL);
	}

	@Test
	void includesPotentialMatchesWithAnExplicitManualReviewStatus() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("LOCAL", "관악 취업 지원", PolicyInterestCategory.JOB, "SIGUNGU:11620", 19, 39, true)
		));

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).singleElement().satisfies(card -> {
			assertThat(card.matchStatus()).isEqualTo(PolicyMatchStatus.CHECK_REQUIRED);
			assertThat(card.matchReasons()).contains("세부 자격 조건은 공고에서 확인해 주세요");
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

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).singleElement().satisfies(card -> {
			assertThat(card.matchReasons()).contains("전국 대상 정책이에요");
			assertThat(card.matchReasons()).doesNotContain("에코마일리지 연동 지역과 일치해요");
		});
	}

	@Test
	void limitsRecommendationsToFiveEligiblePolicies() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("P-1", "정책 1", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("P-2", "정책 2", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("P-3", "정책 3", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("P-4", "정책 4", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("P-5", "정책 5", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("P-6", "정책 6", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false)
		));

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).hasSize(5);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	void filtersRecommendationsBySelectedInterestCategories() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("JOB", "취업 지원", PolicyInterestCategory.JOB, "NATIONAL:00000", 19, 39, false),
			policy("HOUSING", "주거 지원", PolicyInterestCategory.HOUSING, "NATIONAL:00000", 19, 39, false)
		));

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).extracting(card -> card.policyId()).containsExactly("JOB");
		assertThat(response.content().getFirst().matchReasons()).contains("관심 분야와 일치해요");
	}

	@Test
	void returnsFiveHousingMatchesForTheRequestedProfile() {
		PolicyProfile requestedProfile = new PolicyProfile(
			LocalDate.of(2001, 6, 15),
			CurrentStatus.UNEMPLOYED,
			AnnualIncomeBand.NO_INCOME,
			EducationStatus.UNIVERSITY_GRADUATE,
			List.of(PolicyInterestCategory.HOUSING),
			"11", "11620", "서울특별시 관악구"
		);
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(requestedProfile));
		when(repository.findAllActive()).thenReturn(List.of(
			policy("HOME-1", "청년주택드림청약통장", PolicyInterestCategory.HOUSING,
				"NATIONAL:00000", 19, 34, false),
			policy("HOME-2", "청년 매입임대주택 사업", PolicyInterestCategory.HOUSING,
				"SIDO:11", 19, 39, false),
			policy("HOME-3", "청년 부동산 중개보수 및 이사비 지원사업", PolicyInterestCategory.HOUSING,
				"SIDO:11", 19, 39, false),
			policy("HOME-4", "청년안심주택 공급", PolicyInterestCategory.HOUSING,
				"SIDO:11", 19, 39, false),
			policy("HOME-5", "청년안심주택 임차보증금 지원", PolicyInterestCategory.HOUSING,
				"SIDO:11", 19, 39, false),
			policy("JOB", "취업 지원", PolicyInterestCategory.JOB,
				"NATIONAL:00000", 19, 39, false)
		));

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).hasSize(5);
		assertThat(response.content()).allSatisfy(card ->
			assertThat(card.category()).isEqualTo(PolicyInterestCategory.HOUSING));
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

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).extracting(card -> card.policyId()).containsExactly("EMPLOYED");
		assertThat(response.content().getFirst().matchReasons())
			.contains("현재 상태가 정책의 취업 조건과 일치해요");
	}

	@Test
	void filtersOnlyWhenIncomeBandClearlyMissesStructuredRange() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.FROM_24M_TO_36M,
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

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).extracting(card -> card.policyId())
			.containsExactly("MATCH");
		assertThat(response.content().get(0).matchReasons())
			.contains("연소득 구간이 정책의 소득 조건과 일치해요");
	}

	@Test
	void keepsMarriageRestrictedPoliciesAsExplicitManualChecks() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.UNDER_24M,
			"11", "11620"
		)));
		when(repository.findAllActive()).thenReturn(List.of(
			policyWithConditions("MARRIED", "기혼 지원", "NATIONAL:00000",
				"0055001", null, null, null, null, null, null),
			policyWithConditions("UNMARRIED", "미혼 지원", "NATIONAL:00000",
				"0055002", null, null, null, null, null, null)
		));

		var response = service.getRecommendations(USER_ID);

		assertThat(response.content()).hasSize(2);
		assertThat(response.content()).allSatisfy(card ->
			assertThat(card.matchStatus()).isEqualTo(PolicyMatchStatus.CHECK_REQUIRED));
	}

	@Test
	void rendersKnownEmploymentAndMarriageConditionsWithExactLabels() {
		YouthPolicySnapshot policy = policyWithConditions(
			"UNMARRIED-JOBSEEKER", "미혼 미취업자 주거 지원", "NATIONAL:00000",
			"0055002", "0043001", null, null, null, "0013003", "0014010"
		);
		when(repository.findActiveByExternalId("UNMARRIED-JOBSEEKER")).thenReturn(Optional.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile("11", "11620")));

		var response = service.getDetail(USER_ID, "UNMARRIED-JOBSEEKER");

		assertThat(response.conditions().employment()).isEqualTo("미취업자");
		assertThat(response.conditions().marriage()).isEqualTo("미혼");
	}

	@Test
	void fullCatalogCanBeFilteredWithoutCompletedProfile() {
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.empty());
		when(repository.findAllActive()).thenReturn(List.of(
			policy("JOB", "취업 역량 지원", PolicyInterestCategory.JOB, "SIGUNGU:11620", 19, 39, false),
			policy("HOME", "주거 지원", PolicyInterestCategory.HOUSING, "SIGUNGU:11620", 19, 39, false)
		));

		var response = service.getAll(
			USER_ID, "역량", PolicyInterestCategory.JOB, "11620", 0, 20
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

		assertThatThrownBy(() -> service.getRecommendations(USER_ID))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code())
					.isEqualTo("YOUTH_POLICY_DATA_UNAVAILABLE"));
		assertThatThrownBy(() -> service.getDetail(USER_ID, "UNKNOWN"))
			.isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode().code())
					.isEqualTo("YOUTH_POLICY_NOT_FOUND"));
	}

	@Test
	void rendersAlwaysOpenAndExplicitNoLimitConditionsWithoutManualCheckLabels() {
		YouthPolicySnapshot policy = new YouthPolicySnapshot(
			1L, "ALWAYS", "상시 정책", null, "설명", "JOB", "취업", PolicyInterestCategory.JOB, "지원 내용",
			"주관기관", "운영기관", "0044002", "0042002", "0057002",
			null, null, null, null,
			null, "홈페이지 신청", "https://example.go.kr", null, null, "N", 19, 39,
			"0055003", "0043001", null, null,
			null, null, null, "0011009", "0013010", "0049010", "0014010",
			PolicyApplicationStatus.OPEN, "NATIONAL:00000", SYNCED_AT
		);
		when(repository.findActiveByExternalId("ALWAYS")).thenReturn(Optional.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));

		var response = service.getDetail(USER_ID, "ALWAYS");

		assertThat(response.application().periodType()).isEqualTo("ALWAYS");
		assertThat(response.application().startDate()).isNull();
		assertThat(response.application().endDate()).isNull();
		assertThat(response.conditions().income()).isEqualTo("제한 없음");
		assertThat(response.conditions().employment()).isEqualTo("제한 없음");
		assertThat(response.conditions().education()).isEqualTo("제한 없음");
		assertThat(response.conditions().major()).isEqualTo("제한 없음");
		assertThat(response.conditions().marriage()).isEqualTo("제한 없음");
		assertThat(response.conditions().special()).isEqualTo("제한 없음");
	}

	@Test
	void treatsMissingConditionCodesAsUnknownInsteadOfNoLimit() {
		YouthPolicySnapshot policy = new YouthPolicySnapshot(
			1L, "MISSING", "조건 누락 정책", null, "설명", "JOB", "취업", PolicyInterestCategory.JOB,
			"지원 내용", "주관기관", "운영기관", "0044002", "0042002", "0057002",
			null, null, null, null, null, "홈페이지 신청", "https://example.go.kr", null, null,
			"N", 19, 39, null, null, null, null, null, null, null, null, null, null, null,
			PolicyApplicationStatus.OPEN, "NATIONAL:00000", SYNCED_AT
		);
		when(repository.findActiveByExternalId("MISSING")).thenReturn(Optional.of(policy));
		when(repository.findAllActive()).thenReturn(List.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));

		var detail = service.getDetail(USER_ID, "MISSING");
		var recommendations = service.getRecommendations(USER_ID);

		assertThat(detail.conditions().income()).isEqualTo("세부 소득 조건 확인");
		assertThat(detail.conditions().employment()).isEqualTo("세부 취업 상태 조건 확인");
		assertThat(detail.conditions().education()).isEqualTo("세부 학력 조건 확인");
		assertThat(detail.conditions().major()).isEqualTo("세부 전공 조건 확인");
		assertThat(detail.conditions().marriage()).isEqualTo("세부 혼인 조건 확인");
		assertThat(detail.conditions().special()).isEqualTo("세부 공고 확인");
		assertThat(detail.match().status()).isEqualTo(PolicyMatchStatus.CHECK_REQUIRED);
		assertThat(recommendations.content()).isEmpty();
	}

	@Test
	void treatsMissingAgeBoundsAsUnknownWhenSourceSaysAgeIsLimited() {
		YouthPolicySnapshot policy = new YouthPolicySnapshot(
			1L, "AGE-MISSING", "연령 조건 누락 정책", null, "설명", "JOB", "취업",
			PolicyInterestCategory.JOB, "지원 내용", "주관기관", "운영기관",
			"0044002", "0042002", "0057002", null, null, null, null, null,
			"홈페이지 신청", "https://example.go.kr", null, null, "Y", null, null,
			"0055003", "0043001", null, null, null, null, null,
			"0011009", "0013010", "0049010", "0014010",
			PolicyApplicationStatus.OPEN, "NATIONAL:00000", SYNCED_AT
		);
		when(repository.findActiveByExternalId("AGE-MISSING")).thenReturn(Optional.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));

		var detail = service.getDetail(USER_ID, "AGE-MISSING");

		assertThat(detail.conditions().age()).isEqualTo("세부 연령 조건 확인");
	}

	@Test
	void showsParticipantTargetTextInsteadOfGenericNoLimitLabel() {
		YouthPolicySnapshot policy = new YouthPolicySnapshot(
			1L, "TARGET", "대상 조건 정책", null, "설명", "JOB", "취업",
			PolicyInterestCategory.JOB, "지원 내용", "주관기관", "운영기관",
			"0044002", "0042002", "0057002", null, null, null, null, null,
			"홈페이지 신청", "https://example.go.kr", null, null, "N", 19, 39,
			"0055003", "0043001", null, null, null, null, "지역 청년 재직자",
			"0011009", "0013010", "0049010", "0014010",
			PolicyApplicationStatus.OPEN, "NATIONAL:00000", SYNCED_AT
		);
		when(repository.findActiveByExternalId("TARGET")).thenReturn(Optional.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));

		var detail = service.getDetail(USER_ID, "TARGET");

		assertThat(detail.conditions().special()).isEqualTo("지역 청년 재직자");
	}

	@Test
	void rendersStructuredAnnualIncomeLimit() {
		YouthPolicySnapshot policy = policyWithConditions(
			"INCOME", "연소득 제한 정책", "NATIONAL:00000",
			"0055003", "0043002", null, 3_500L, null, "0013010", "0014010"
		);
		when(repository.findActiveByExternalId("INCOME")).thenReturn(Optional.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));

		var response = service.getDetail(USER_ID, "INCOME");

		assertThat(response.conditions().income()).isEqualTo("연소득 3,500만원 이하");
	}

	@Test
	void excludesLoanGuaranteesFromExactRecommendationsAndShowsReviewConditions() {
		YouthPolicySnapshot policy = new YouthPolicySnapshot(
			1L, "LOAN", "청년 보증부 대출", null, "설명", "복지･문화", "금융지원",
			PolicyInterestCategory.WELFARE_CULTURE, "지원 내용", "주관기관", "운영기관",
			"0044002", "0042007", "0057002", null, null, null, null, null,
			"홈페이지 신청", "https://example.go.kr", null, null, "N", 19, 34,
			"0055003", "0043002", null, 3_500L, null, null, null,
			"0011009", "0013010", "0049010", "0014010",
			PolicyApplicationStatus.OPEN, "NATIONAL:00000", SYNCED_AT
		);
		when(repository.findActiveByExternalId("LOAN")).thenReturn(Optional.of(policy));
		when(repository.findAllActive()).thenReturn(List.of(policy));
		when(profileQueryService.findCompleted(USER_ID)).thenReturn(Optional.of(profile(null, null)));

		var detail = service.getDetail(USER_ID, "LOAN");
		var recommendations = service.getRecommendations(USER_ID);

		assertThat(detail.conditions().income()).isEqualTo("연소득 3,500만원 이하");
		assertThat(detail.conditions().employment()).isEqualTo("취업·재직 조건 확인");
		assertThat(detail.conditions().education()).isEqualTo("학업·취업 상태 조건 확인");
		assertThat(detail.conditions().special()).isEqualTo("보증·대출 심사 조건 확인");
		assertThat(detail.match().status()).isEqualTo(PolicyMatchStatus.CHECK_REQUIRED);
		assertThat(detail.match().reasons()).contains("보증·대출 심사 등 세부 자격을 확인해 주세요");
		assertThat(recommendations.content()).isEmpty();
	}

	private PolicyProfile profile(String sidoCode, String sigunguCode) {
		return profile(
			CurrentStatus.EMPLOYED, AnnualIncomeBand.UNDER_24M,
			sidoCode, sigunguCode
		);
	}

	private PolicyProfile profile(
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		String sidoCode,
		String sigunguCode
	) {
		return new PolicyProfile(
			LocalDate.of(1998, 3, 15),
			currentStatus, annualIncomeBand, EducationStatus.UNIVERSITY_GRADUATE,
			List.of(PolicyInterestCategory.JOB),
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
			"주관기관", "운영기관", "0044002", "0042002", "0057001",
			LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
			LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
			null, "온라인", "https://example.go.kr", null, null, "N", minAge, maxAge,
			marriageCode == null ? "0055003" : marriageCode,
			incomeCode == null ? "0043001" : incomeCode, incomeMin, incomeMax,
			incomeText, null, null, "0011009",
			employmentCodes == null ? "0013010" : employmentCodes,
			"0049010", specialCodes == null ? "0014010" : specialCodes,
			PolicyApplicationStatus.OPEN, regionKeys, SYNCED_AT
		);
	}
}
