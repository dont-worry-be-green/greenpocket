package com.greenpocket.policy.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.policy.dto.PolicyCardResponse;
import com.greenpocket.policy.dto.PolicyDetailResponse;
import com.greenpocket.policy.dto.PolicyListResponse;
import com.greenpocket.policy.dto.PolicyMypageSummary;
import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyMatchStatus;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.policy.exception.PolicyErrorCode;
import com.greenpocket.policy.repository.YouthPolicyRepository;
import com.greenpocket.policy.repository.YouthPolicyRepository.YouthPolicySnapshot;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.profile.service.PolicyProfileQueryService;
import com.greenpocket.profile.service.PolicyProfileQueryService.PolicyProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyQueryService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int RECOMMENDATION_LIMIT = 5;
	private static final long MAX_PLAUSIBLE_INCOME_MANWON = 100_000L;
	private static final String EMPLOYMENT_NO_LIMIT = "0013010";
	private static final String INCOME_NO_LIMIT = "0043001";
	private static final String INCOME_ANNUAL = "0043002";
	private static final String MARRIAGE_NO_LIMIT = "0055003";
	private static final String SPECIAL_NO_LIMIT = "0014010";
	private static final String MAJOR_NO_LIMIT = "0011009";
	private static final String SCHOOL_NO_LIMIT = "0049010";
	private static final Set<String> FINANCIAL_REVIEW_PROVISION_METHODS = Set.of(
		"0042003", // 직접대출
		"0042007", // 대출보증
		"0042008"  // 공적보험
	);

	private final YouthPolicyRepository youthPolicyRepository;
	private final PolicyProfileQueryService policyProfileQueryService;

	public PolicyListResponse getRecommendations(Long userId) {
		PolicyProfile profile = policyProfileQueryService.findCompleted(userId)
			.orElseThrow(() -> new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE));
		return recommendations(profile);
	}

	public PolicyMypageSummary getMypageSummary(Long userId, int previewSize) {
		Optional<PolicyProfile> profile = policyProfileQueryService.findCompleted(userId);
		if (profile.isEmpty()) {
			return new PolicyMypageSummary(
				false, false, 0, List.of(),
				toOffsetDateTime(youthPolicyRepository.findLastSuccessfulSyncAt().orElse(null))
			);
		}
		try {
			PolicyListResponse recommendations = recommendations(profile.get());
			return new PolicyMypageSummary(
				true,
				profile.get().regionLinked(),
				recommendations.totalElements(),
				recommendations.content(),
				recommendations.lastSyncedAt()
			);
		}
		catch (BusinessException exception) {
			if (exception.getErrorCode() != PolicyErrorCode.YOUTH_POLICY_DATA_UNAVAILABLE) {
				throw exception;
			}
			return new PolicyMypageSummary(
				true,
				profile.get().regionLinked(),
				0,
				List.of(),
				toOffsetDateTime(youthPolicyRepository.findLastSuccessfulSyncAt().orElse(null))
			);
		}
	}

	public PolicyListResponse getAll(
		Long userId,
		String keyword,
		PolicyInterestCategory category,
		String regionCode,
		int page,
		int size
	) {
		validatePage(page, size);
		List<YouthPolicySnapshot> all = activePolicies();
		Optional<PolicyProfile> profile = policyProfileQueryService.findCompleted(userId);
		String normalizedKeyword = keyword == null ? null : keyword.strip().toLowerCase(Locale.ROOT);
		List<PolicyCardResponse> cards = all.stream()
			.filter(policy -> normalizedKeyword == null || normalizedKeyword.isEmpty()
				|| containsIgnoreCase(policy.title(), normalizedKeyword)
				|| containsIgnoreCase(policy.supportContent(), normalizedKeyword))
			.filter(policy -> category == null || policy.interestCategory() == category)
			.filter(policy -> regionCode == null || regionCode.isBlank() || hasExactRegion(policy, regionCode.strip()))
			.map(policy -> toCard(policy, profile.map(value -> match(policy, value)).orElse(null)))
			.toList();
		return page(cards, page, size, null);
	}

	public PolicyDetailResponse getDetail(Long userId, String policyId) {
		YouthPolicySnapshot policy = youthPolicyRepository.findActiveByExternalId(policyId)
			.orElseThrow(() -> new BusinessException(PolicyErrorCode.YOUTH_POLICY_NOT_FOUND));
		PolicyMatch match = policyProfileQueryService.findCompleted(userId)
			.map(profile -> match(policy, profile))
			.orElse(null);
		return new PolicyDetailResponse(
			policy.externalPolicyId(),
			policy.title(),
			policy.interestCategory(),
			policy.mediumCategoryName(),
			policy.description(),
			policy.supportContent(),
			new PolicyDetailResponse.Application(
				policy.applicationStatus(),
				periodType(policy),
				policy.applicationStartDate(),
				policy.applicationEndDate(),
				policy.applicationMethod(),
				policy.applicationUrl()
			),
			new PolicyDetailResponse.Organizations(
				policy.supervisingOrgName(),
				policy.operatingOrgName()
			),
			new PolicyDetailResponse.Conditions(
				ageCondition(policy),
				incomeCondition(policy),
				employmentCondition(policy),
				educationCondition(policy),
				codedCondition(policy.majorCodes(), MAJOR_NO_LIMIT, "세부 전공 조건 확인"),
				codedCondition(policy.marriageStatusCode(), MARRIAGE_NO_LIMIT, "세부 혼인 조건 확인"),
				specialCondition(policy)
			),
			match == null ? null : new PolicyDetailResponse.Match(match.status(), match.score(), match.reasons()),
			StreamSupport.urls(policy.referenceUrl1(), policy.referenceUrl2()),
			"온통청년",
			toOffsetDateTime(policy.syncedAt())
		);
	}

	private PolicyListResponse recommendations(PolicyProfile profile) {
		List<PolicyCardResponse> cards = activePolicies().stream()
			.filter(policy -> policy.applicationStatus() == PolicyApplicationStatus.OPEN)
			.map(policy -> new ScoredPolicy(policy, match(policy, profile)))
			.filter(scored -> scored.match().status() == PolicyMatchStatus.ELIGIBLE)
			.sorted(Comparator.comparingInt((ScoredPolicy scored) -> scored.match().score()).reversed()
				.thenComparing(scored -> scored.policy().applicationEndDate(),
					Comparator.nullsLast(Comparator.naturalOrder())))
			.limit(RECOMMENDATION_LIMIT)
			.map(scored -> toCard(scored.policy(), scored.match()))
			.toList();
		return page(cards, 0, RECOMMENDATION_LIMIT, region(profile));
	}

	private List<YouthPolicySnapshot> activePolicies() {
		List<YouthPolicySnapshot> policies = youthPolicyRepository.findAllActive();
		if (policies.isEmpty()) {
			throw new BusinessException(PolicyErrorCode.YOUTH_POLICY_DATA_UNAVAILABLE);
		}
		return policies;
	}

	private PolicyListResponse page(
		List<PolicyCardResponse> cards,
		int page,
		int size,
		PolicyListResponse.Region region
	) {
		int from = (int)Math.min((long)page * size, cards.size());
		int to = Math.min(from + size, cards.size());
		int totalPages = cards.isEmpty() ? 0 : (cards.size() + size - 1) / size;
		LocalDateTime lastSyncedAt = youthPolicyRepository.findLastSuccessfulSyncAt().orElse(null);
		return new PolicyListResponse(
			List.copyOf(cards.subList(from, to)),
			page,
			size,
			cards.size(),
			totalPages,
			page + 1 < totalPages,
			region,
			toOffsetDateTime(lastSyncedAt)
		);
	}

	private PolicyCardResponse toCard(YouthPolicySnapshot policy, PolicyMatch match) {
		return new PolicyCardResponse(
			policy.externalPolicyId(),
			policy.title(),
			policy.interestCategory(),
			policy.mediumCategoryName(),
			summary(policy.supportContent()),
			policy.applicationStatus(),
			policy.applicationEndDate(),
			match == null ? null : match.status(),
			match == null ? null : match.score(),
			match == null ? null : match.reasons(),
			regionScope(policy)
		);
	}

	private PolicyMatch match(YouthPolicySnapshot policy, PolicyProfile profile) {
		List<String> reasons = new ArrayList<>();
		int score = 0;
		if (profile.interestCategories().contains(policy.interestCategory())) {
			score += 5;
			reasons.add("관심 분야와 일치해요");
		}

		RegionMatch regionMatch = regionMatch(policy, profile);
		if (regionMatch == RegionMatch.NO) {
			return new PolicyMatch(PolicyMatchStatus.NOT_ELIGIBLE, 0, List.of("지원 지역이 일치하지 않아요"));
		}
		if (regionMatch == RegionMatch.YES) {
			score += 30;
			reasons.add(isNational(policy)
				? "전국 대상 정책이에요"
				: "에코마일리지 연동 지역과 일치해요");
		}

		int age = age(profile.birthDate(), LocalDate.now(KOREA_ZONE_ID));
		if ((policy.minAge() != null && age < policy.minAge())
			|| (policy.maxAge() != null && age > policy.maxAge())) {
			return new PolicyMatch(PolicyMatchStatus.NOT_ELIGIBLE, 0, List.of("지원 연령에 해당하지 않아요"));
		}
		if (policy.minAge() != null || policy.maxAge() != null) {
			score += 25;
			reasons.add("지원 연령에 해당해요");
		}
		else {
			score += 25;
		}

		if (policy.applicationStatus() == PolicyApplicationStatus.OPEN) {
			score += 10;
			reasons.add("현재 신청 가능한 기간이에요");
		}

		ConditionMatch employmentMatch = employmentMatch(policy, profile.currentStatus());
		if (employmentMatch == ConditionMatch.NOT_MATCHED) {
			return notEligible("현재 상태가 정책의 취업 조건과 일치하지 않아요");
		}
		if (employmentMatch == ConditionMatch.MATCHED) {
			score += 15;
			if (hasRestrictedCodes(policy.employmentCodes(), EMPLOYMENT_NO_LIMIT)) {
				reasons.add("현재 상태가 정책의 취업 조건과 일치해요");
			}
		}

		ConditionMatch incomeMatch = incomeMatch(policy, profile.annualIncomeBand());
		if (incomeMatch == ConditionMatch.NOT_MATCHED) {
			return notEligible("연소득 구간이 정책의 소득 조건과 일치하지 않아요");
		}
		if (incomeMatch == ConditionMatch.MATCHED) {
			score += 15;
			if (INCOME_ANNUAL.equals(policy.incomeConditionCode())) {
				reasons.add("연소득 구간이 정책의 소득 조건과 일치해요");
			}
		}

		ConditionMatch educationMatch = educationMatch(policy, profile.educationStatus());
		if (educationMatch == ConditionMatch.NOT_MATCHED) {
			return notEligible("학력 상태가 정책 조건과 일치하지 않아요");
		}
		if (educationMatch == ConditionMatch.MATCHED) {
			score += 10;
			if (hasRestrictedCodes(policy.schoolCodes(), SCHOOL_NO_LIMIT)) {
				reasons.add("학력 상태가 정책 조건과 일치해요");
			}
		}

		boolean financialReviewRequired = requiresFinancialReview(policy);
		boolean requiresCheck = regionMatch == RegionMatch.UNKNOWN
			|| employmentMatch == ConditionMatch.CHECK_REQUIRED
			|| incomeMatch == ConditionMatch.CHECK_REQUIRED
			|| educationMatch == ConditionMatch.CHECK_REQUIRED
			|| hasUnresolvedConditions(policy)
			|| financialReviewRequired;
		if (requiresCheck) {
			reasons.add(financialReviewRequired
				? "보증·대출 심사 등 세부 자격을 확인해 주세요"
				: "세부 자격 조건은 공고에서 확인해 주세요");
		}
		return new PolicyMatch(
			requiresCheck ? PolicyMatchStatus.CHECK_REQUIRED : PolicyMatchStatus.ELIGIBLE,
			requiresCheck ? Math.min(score, 90) : Math.min(score, 100),
			List.copyOf(reasons)
		);
	}

	private static PolicyMatch notEligible(String reason) {
		return new PolicyMatch(PolicyMatchStatus.NOT_ELIGIBLE, 0, List.of(reason));
	}

	private static ConditionMatch employmentMatch(YouthPolicySnapshot policy, CurrentStatus currentStatus) {
		Set<String> codes = conditionCodes(policy.employmentCodes());
		if (codes.isEmpty()) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		if (isExplicitNoLimit(codes, EMPLOYMENT_NO_LIMIT)) {
			return ConditionMatch.MATCHED;
		}
		if (codes.contains(EMPLOYMENT_NO_LIMIT)) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		String currentCode = employmentCode(currentStatus);
		if (currentCode == null) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		return codes.contains(currentCode) ? ConditionMatch.MATCHED : ConditionMatch.NOT_MATCHED;
	}

	private static String employmentCode(CurrentStatus currentStatus) {
		return switch (currentStatus) {
			case EMPLOYED -> "0013001";
			case SELF_EMPLOYED -> "0013002";
			case UNEMPLOYED -> "0013003";
			case FREELANCER -> "0013004";
			case PREPARING_STARTUP -> "0013006";
			case STUDENT, OTHER -> null;
		};
	}

	private static ConditionMatch incomeMatch(YouthPolicySnapshot policy, AnnualIncomeBand incomeBand) {
		String code = policy.incomeConditionCode();
		if (!hasText(code)) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		if (INCOME_NO_LIMIT.equals(code)) {
			return ConditionMatch.MATCHED;
		}
		if (!INCOME_ANNUAL.equals(code) || incomeBand == AnnualIncomeBand.UNKNOWN) {
			return ConditionMatch.CHECK_REQUIRED;
		}

		Long policyMin = policy.incomeMinAmount();
		Long policyMax = policy.incomeMaxAmount();
		if (!validIncomeAmount(policyMin) || !validIncomeAmount(policyMax)
			|| (policyMin != null && policyMax != null && policyMin > policyMax)) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		if (policyMin == null && policyMax == null) {
			return ConditionMatch.CHECK_REQUIRED;
		}

		IncomeRange userRange = incomeRange(incomeBand);
		long minimum = policyMin == null ? 0L : policyMin;
		long maximum = policyMax == null ? Long.MAX_VALUE : policyMax;
		if (userRange.maximum() < minimum || userRange.minimum() > maximum) {
			return ConditionMatch.NOT_MATCHED;
		}
		if (userRange.minimum() >= minimum && userRange.maximum() <= maximum) {
			return ConditionMatch.MATCHED;
		}
		return ConditionMatch.CHECK_REQUIRED;
	}

	private static boolean validIncomeAmount(Long amount) {
		return amount == null || (amount >= 0L && amount <= MAX_PLAUSIBLE_INCOME_MANWON);
	}

	private static IncomeRange incomeRange(AnnualIncomeBand incomeBand) {
		return switch (incomeBand) {
			case NO_INCOME -> new IncomeRange(0L, 0L);
			case UNDER_24M -> new IncomeRange(1L, 2_399L);
			case FROM_24M_TO_36M -> new IncomeRange(2_400L, 3_599L);
			case FROM_36M_TO_50M -> new IncomeRange(3_600L, 4_999L);
			case OVER_50M -> new IncomeRange(5_000L, Long.MAX_VALUE);
			case UNKNOWN -> throw new IllegalArgumentException("UNKNOWN 소득 구간은 범위로 변환할 수 없습니다.");
		};
	}

	private static ConditionMatch educationMatch(YouthPolicySnapshot policy, EducationStatus educationStatus) {
		Set<String> codes = conditionCodes(policy.schoolCodes());
		if (codes.isEmpty()) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		if (isExplicitNoLimit(codes, SCHOOL_NO_LIMIT)) {
			return ConditionMatch.MATCHED;
		}
		if (codes.contains(SCHOOL_NO_LIMIT)) {
			return ConditionMatch.CHECK_REQUIRED;
		}
		String educationCode = switch (educationStatus) {
			case BELOW_HIGH_SCHOOL -> "0049001";
			case HIGH_SCHOOL_STUDENT -> "0049002";
			case HIGH_SCHOOL_EXPECTED_GRADUATION -> "0049003";
			case HIGH_SCHOOL_GRADUATE -> "0049004";
			case UNIVERSITY_STUDENT -> "0049005";
			case UNIVERSITY_EXPECTED_GRADUATION -> "0049006";
			case UNIVERSITY_GRADUATE -> "0049007";
			case GRADUATE_SCHOOL -> "0049008";
			case OTHER -> "0049009";
		};
		return codes.contains(educationCode) ? ConditionMatch.MATCHED : ConditionMatch.NOT_MATCHED;
	}

	private static boolean hasUnresolvedConditions(YouthPolicySnapshot policy) {
		return hasText(policy.incomeConditionText())
			|| hasText(policy.additionalConditionText()) || hasText(policy.participantTargetText())
			|| hasUnknownOrRestrictedCodes(policy.majorCodes(), MAJOR_NO_LIMIT)
			|| !hasText(policy.marriageStatusCode())
			|| !MARRIAGE_NO_LIMIT.equals(policy.marriageStatusCode())
			|| hasUnknownOrRestrictedCodes(policy.specialCodes(), SPECIAL_NO_LIMIT);
	}

	private static boolean hasUnknownOrRestrictedCodes(String rawCodes, String noLimitCode) {
		Set<String> codes = conditionCodes(rawCodes);
		return !isExplicitNoLimit(codes, noLimitCode);
	}

	private static boolean hasRestrictedCodes(String rawCodes, String noLimitCode) {
		Set<String> codes = conditionCodes(rawCodes);
		return !codes.isEmpty() && !isExplicitNoLimit(codes, noLimitCode);
	}

	private static boolean isExplicitNoLimit(Set<String> codes, String noLimitCode) {
		return codes.size() == 1 && codes.contains(noLimitCode);
	}

	private static Set<String> conditionCodes(String value) {
		if (value == null || value.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(value.split(","))
			.map(String::strip)
			.filter(code -> !code.isEmpty())
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private static boolean isNational(YouthPolicySnapshot policy) {
		return regionKeys(policy).contains("NATIONAL:00000");
	}

	private static RegionMatch regionMatch(YouthPolicySnapshot policy, PolicyProfile profile) {
		Set<String> regions = regionKeys(policy);
		if (regions.contains("NATIONAL:00000")) {
			return RegionMatch.YES;
		}
		if (!profile.regionLinked()) {
			return RegionMatch.NO;
		}
		if (regions.contains("SIGUNGU:" + profile.ecoSigunguCode())
			|| regions.contains("SIDO:" + profile.ecoSidoCode())) {
			return RegionMatch.YES;
		}
		return regions.isEmpty() ? RegionMatch.UNKNOWN : RegionMatch.NO;
	}

	private static boolean hasExactRegion(YouthPolicySnapshot policy, String regionCode) {
		return regionKeys(policy).stream().anyMatch(value -> value.endsWith(":" + regionCode));
	}

	private static Set<String> regionKeys(YouthPolicySnapshot policy) {
		if (policy.regionKeys() == null || policy.regionKeys().isBlank()) {
			return Set.of();
		}
		return Set.copyOf(Arrays.asList(policy.regionKeys().split(",")));
	}

	private static PolicyRegionLevel regionScope(YouthPolicySnapshot policy) {
		Set<String> keys = regionKeys(policy);
		if (keys.stream().anyMatch(value -> value.startsWith("NATIONAL:"))) {
			return PolicyRegionLevel.NATIONAL;
		}
		if (keys.stream().anyMatch(value -> value.startsWith("SIDO:"))) {
			return PolicyRegionLevel.SIDO;
		}
		return PolicyRegionLevel.SIGUNGU;
	}

	private static PolicyListResponse.Region region(PolicyProfile profile) {
		return new PolicyListResponse.Region(
			profile.regionLinked(),
			profile.ecoAddressLabel(),
			profile.regionLinked()
				? List.of(PolicyRegionLevel.NATIONAL, PolicyRegionLevel.SIDO, PolicyRegionLevel.SIGUNGU)
				: List.of(PolicyRegionLevel.NATIONAL)
		);
	}

	private static void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
	}

	private static int age(LocalDate birthDate, LocalDate 기준일) {
		return (int)ChronoUnit.YEARS.between(birthDate, 기준일);
	}

	private static String summary(String value) {
		if (value == null || value.isBlank() || value.length() <= 160) {
			return value;
		}
		return value.substring(0, 157) + "...";
	}

	private static String periodType(YouthPolicySnapshot policy) {
		return "0057002".equals(policy.applicationPeriodCode()) ? "ALWAYS" : "LIMITED";
	}

	private static String ageCondition(YouthPolicySnapshot policy) {
		if (policy.minAge() == null && policy.maxAge() == null) {
			return "N".equals(policy.ageLimitYn()) ? "제한 없음" : "세부 연령 조건 확인";
		}
		if (policy.minAge() == null) {
			return "만 " + policy.maxAge() + "세 이하";
		}
		if (policy.maxAge() == null) {
			return "만 " + policy.minAge() + "세 이상";
		}
		return "만 " + policy.minAge() + "~" + policy.maxAge() + "세";
	}

	private static String incomeCondition(YouthPolicySnapshot policy) {
		if (hasText(policy.incomeConditionText())) {
			return policy.incomeConditionText();
		}
		if (!hasText(policy.incomeConditionCode())) {
			return "세부 소득 조건 확인";
		}
		if (INCOME_NO_LIMIT.equals(policy.incomeConditionCode())) {
			return "제한 없음";
		}
		if (INCOME_ANNUAL.equals(policy.incomeConditionCode())
			&& validIncomeAmount(policy.incomeMinAmount()) && validIncomeAmount(policy.incomeMaxAmount())) {
			return annualIncomeCondition(policy.incomeMinAmount(), policy.incomeMaxAmount());
		}
		return "세부 소득 조건 확인";
	}

	private static String annualIncomeCondition(Long minimum, Long maximum) {
		if (minimum == null && maximum == null) {
			return "세부 소득 조건 확인";
		}
		if (minimum == null) {
			return "연소득 " + formatManwon(maximum) + "만원 이하";
		}
		if (maximum == null) {
			return "연소득 " + formatManwon(minimum) + "만원 이상";
		}
		if (minimum > maximum) {
			return "세부 소득 조건 확인";
		}
		return "연소득 " + formatManwon(minimum) + "만~" + formatManwon(maximum) + "만원";
	}

	private static String formatManwon(Long amount) {
		return String.format(Locale.KOREA, "%,d", amount);
	}

	private static String employmentCondition(YouthPolicySnapshot policy) {
		if (requiresFinancialReview(policy)) {
			return "취업·재직 조건 확인";
		}
		return codedCondition(policy.employmentCodes(), EMPLOYMENT_NO_LIMIT, "세부 취업 상태 조건 확인");
	}

	private static String educationCondition(YouthPolicySnapshot policy) {
		if (requiresFinancialReview(policy)) {
			return "학업·취업 상태 조건 확인";
		}
		return codedCondition(policy.schoolCodes(), SCHOOL_NO_LIMIT, "세부 학력 조건 확인");
	}

	private static String specialCondition(YouthPolicySnapshot policy) {
		if (hasText(policy.additionalConditionText())) {
			return policy.additionalConditionText();
		}
		if (hasText(policy.participantTargetText())) {
			return policy.participantTargetText();
		}
		if (requiresFinancialReview(policy)) {
			return "보증·대출 심사 조건 확인";
		}
		return codedCondition(policy.specialCodes(), SPECIAL_NO_LIMIT, "세부 공고 확인");
	}

	private static boolean requiresFinancialReview(YouthPolicySnapshot policy) {
		return FINANCIAL_REVIEW_PROVISION_METHODS.contains(policy.provisionMethodCode());
	}

	private static String codedCondition(String rawCodes, String noLimitCode, String fallback) {
		Set<String> codes = conditionCodes(rawCodes);
		return isExplicitNoLimit(codes, noLimitCode) ? "제한 없음" : fallback;
	}

	private static boolean containsIgnoreCase(String value, String normalizedKeyword) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	private enum RegionMatch {
		YES,
		NO,
		UNKNOWN
	}

	private enum ConditionMatch {
		MATCHED,
		NOT_MATCHED,
		CHECK_REQUIRED
	}

	private record IncomeRange(long minimum, long maximum) {
	}

	private record PolicyMatch(
		PolicyMatchStatus status,
		int score,
		List<String> reasons
	) {
	}

	private record ScoredPolicy(
		YouthPolicySnapshot policy,
		PolicyMatch match
	) {
	}

	private static final class StreamSupport {

		private StreamSupport() {
		}

		private static List<String> urls(String... values) {
			return Arrays.stream(values).filter(PolicyQueryService::hasText).toList();
		}
	}
}
