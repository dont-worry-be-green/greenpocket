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
import com.greenpocket.policy.dto.PolicyPreviewRequest;
import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyMatchStatus;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.policy.exception.PolicyErrorCode;
import com.greenpocket.policy.repository.YouthPolicyRepository;
import com.greenpocket.policy.repository.YouthPolicyRepository.YouthPolicySnapshot;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.profile.exception.ProfileErrorCode;
import com.greenpocket.profile.service.PolicyProfileQueryService;
import com.greenpocket.profile.service.PolicyProfileQueryService.PolicyProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyQueryService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final long MAX_PLAUSIBLE_INCOME_MANWON = 100_000L;
	private static final String EMPLOYMENT_NO_LIMIT = "0013010";
	private static final String INCOME_NO_LIMIT = "0043001";
	private static final String INCOME_ANNUAL = "0043002";
	private static final String MARRIAGE_MARRIED = "0055001";
	private static final String MARRIAGE_UNMARRIED = "0055002";
	private static final String MARRIAGE_NO_LIMIT = "0055003";
	private static final String SPECIAL_SINGLE_PARENT = "0014004";
	private static final String SPECIAL_NO_LIMIT = "0014010";
	private static final String MAJOR_NO_LIMIT = "0011009";
	private static final String SCHOOL_NO_LIMIT = "0049010";

	private final YouthPolicyRepository youthPolicyRepository;
	private final PolicyProfileQueryService policyProfileQueryService;

	public PolicyListResponse getRecommendations(Long userId, int page, int size) {
		PolicyProfile profile = policyProfileQueryService.findCompleted(userId)
			.orElseThrow(() -> new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE));
		return recommendations(profile, page, size, false);
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
			PolicyListResponse recommendations = recommendations(profile.get(), 0, previewSize, false);
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

	public PolicyListResponse preview(Long userId, PolicyPreviewRequest request) {
		PolicyProfile stored = policyProfileQueryService.find(userId)
			.orElseThrow(() -> new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE));
		PolicyProfile preview = validatePreview(request, stored);
		int page = request.page() == null ? 0 : request.page();
		int size = request.size() == null ? DEFAULT_PAGE_SIZE : request.size();
		return recommendations(preview, page, size, true);
	}

	public PolicyListResponse getAll(
		Long userId,
		String keyword,
		PolicyInterestCategory category,
		String regionCode,
		PolicyApplicationStatus applicationStatus,
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
			.filter(policy -> applicationStatus == null || policy.applicationStatus() == applicationStatus)
			.filter(policy -> regionCode == null || regionCode.isBlank() || hasExactRegion(policy, regionCode.strip()))
			.map(policy -> toCard(policy, profile.map(value -> match(policy, value)).orElse(null)))
			.toList();
		return page(cards, page, size, null, false);
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
				policy.businessStartDate(),
				policy.businessEndDate(),
				policy.applicationMethod(),
				policy.applicationUrl()
			),
			new PolicyDetailResponse.Organizations(
				policy.supervisingOrgName(),
				policy.operatingOrgName()
			),
			new PolicyDetailResponse.Conditions(
				ageCondition(policy),
				condition(policy.incomeConditionText(), policy.incomeConditionCode(), "제한 없음", "세부 소득 조건 확인"),
				condition(null, policy.employmentCodes(), "제한 없음", "세부 취업 상태 조건 확인"),
				condition(null, policy.schoolCodes(), "제한 없음", "세부 학력 조건 확인"),
				condition(null, policy.majorCodes(), "제한 없음", "세부 전공 조건 확인"),
				condition(null, policy.marriageStatusCode(), "제한 없음", "세부 혼인 조건 확인"),
				condition(policy.additionalConditionText(), policy.specialCodes(), "제한 없음", "세부 공고 확인")
			),
			match == null ? null : new PolicyDetailResponse.Match(match.status(), match.score(), match.reasons()),
			StreamSupport.urls(policy.referenceUrl1(), policy.referenceUrl2()),
			"온통청년",
			toOffsetDateTime(policy.syncedAt())
		);
	}

	private PolicyListResponse recommendations(PolicyProfile profile, int page, int size, boolean preview) {
		validatePage(page, size);
		List<PolicyCardResponse> cards = activePolicies().stream()
			.filter(policy -> policy.applicationStatus() != PolicyApplicationStatus.CLOSED)
			.map(policy -> new ScoredPolicy(policy, match(policy, profile)))
			.filter(scored -> scored.match().status() != PolicyMatchStatus.NOT_ELIGIBLE)
			.sorted(Comparator.comparingInt((ScoredPolicy scored) -> scored.match().score()).reversed()
				.thenComparing(scored -> scored.policy().businessEndDate(),
					Comparator.nullsLast(Comparator.naturalOrder())))
			.map(scored -> toCard(scored.policy(), scored.match()))
			.toList();
		return page(cards, page, size, region(profile), preview);
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
		PolicyListResponse.Region region,
		boolean preview
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
			toOffsetDateTime(lastSyncedAt),
			preview
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
			policy.businessEndDate(),
			match == null ? null : match.status(),
			match == null ? null : match.score(),
			match == null ? null : match.reasons(),
			regionScope(policy)
		);
	}

	private PolicyMatch match(YouthPolicySnapshot policy, PolicyProfile profile) {
		List<String> reasons = new ArrayList<>();
		int score = 0;

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

		ConditionMatch householdMatch = householdMatch(policy, profile.householdStatus());
		if (householdMatch == ConditionMatch.NOT_MATCHED) {
			return notEligible("가구 상태가 정책 조건과 일치하지 않아요");
		}
		if (householdMatch == ConditionMatch.MATCHED) {
			score += 5;
			if (hasHouseholdRestriction(policy)) {
				reasons.add("가구 상태가 정책 조건과 일치해요");
			}
		}

		boolean requiresCheck = regionMatch == RegionMatch.UNKNOWN
			|| employmentMatch == ConditionMatch.CHECK_REQUIRED
			|| incomeMatch == ConditionMatch.CHECK_REQUIRED
			|| householdMatch == ConditionMatch.CHECK_REQUIRED
			|| hasUnresolvedConditions(policy, profile.householdStatus());
		if (requiresCheck) {
			reasons.add("소득·학력 등 세부 조건은 공고에서 확인해 주세요");
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
		if (codes.isEmpty() || codes.contains(EMPLOYMENT_NO_LIMIT)) {
			return ConditionMatch.MATCHED;
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
		if (!hasText(code) || INCOME_NO_LIMIT.equals(code)) {
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

	private static ConditionMatch householdMatch(YouthPolicySnapshot policy, HouseholdStatus householdStatus) {
		String marriageCode = policy.marriageStatusCode();
		if (MARRIAGE_MARRIED.equals(marriageCode)) {
			return householdStatus == HouseholdStatus.MARRIED
				? ConditionMatch.MATCHED : ConditionMatch.CHECK_REQUIRED;
		}
		if (MARRIAGE_UNMARRIED.equals(marriageCode)) {
			return householdStatus == HouseholdStatus.MARRIED
				? ConditionMatch.NOT_MATCHED : ConditionMatch.CHECK_REQUIRED;
		}
		if (hasText(marriageCode) && !MARRIAGE_NO_LIMIT.equals(marriageCode)) {
			return ConditionMatch.CHECK_REQUIRED;
		}

		Set<String> specialCodes = conditionCodes(policy.specialCodes());
		if (specialCodes.contains(SPECIAL_SINGLE_PARENT)) {
			return householdStatus == HouseholdStatus.SINGLE_PARENT
				? ConditionMatch.MATCHED : ConditionMatch.CHECK_REQUIRED;
		}
		return ConditionMatch.MATCHED;
	}

	private static boolean hasHouseholdRestriction(YouthPolicySnapshot policy) {
		return MARRIAGE_MARRIED.equals(policy.marriageStatusCode())
			|| MARRIAGE_UNMARRIED.equals(policy.marriageStatusCode())
			|| conditionCodes(policy.specialCodes()).contains(SPECIAL_SINGLE_PARENT);
	}

	private static boolean hasUnresolvedConditions(YouthPolicySnapshot policy, HouseholdStatus householdStatus) {
		return hasText(policy.incomeConditionText())
			|| hasText(policy.additionalConditionText()) || hasText(policy.participantTargetText())
			|| hasRestrictedCodes(policy.majorCodes(), MAJOR_NO_LIMIT)
			|| hasRestrictedCodes(policy.schoolCodes(), SCHOOL_NO_LIMIT)
			|| hasUnresolvedSpecialConditions(policy.specialCodes(), householdStatus);
	}

	private static boolean hasUnresolvedSpecialConditions(String rawCodes, HouseholdStatus householdStatus) {
		Set<String> codes = conditionCodes(rawCodes);
		if (codes.isEmpty() || codes.contains(SPECIAL_NO_LIMIT)) {
			return false;
		}
		return !(codes.size() == 1 && codes.contains(SPECIAL_SINGLE_PARENT)
			&& householdStatus == HouseholdStatus.SINGLE_PARENT);
	}

	private static boolean hasRestrictedCodes(String rawCodes, String noLimitCode) {
		Set<String> codes = conditionCodes(rawCodes);
		return !codes.isEmpty() && !codes.contains(noLimitCode);
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

	private static PolicyProfile validatePreview(PolicyPreviewRequest request, PolicyProfile stored) {
		if (request == null || request.currentStatus() == null || request.annualIncomeBand() == null
			|| request.householdStatus() == null) {
			throw new BusinessException(ProfileErrorCode.PROFILE_INCOMPLETE);
		}
		return new PolicyProfile(
			stored.birthDate(), request.currentStatus(),
			request.annualIncomeBand(), request.householdStatus(),
			stored.ecoSidoCode(), stored.ecoSigunguCode(), stored.ecoAddressLabel()
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
		return policy.businessStartDate() == null && policy.businessEndDate() == null
			? "SEPARATE_NOTICE"
			: "LIMITED";
	}

	private static String ageCondition(YouthPolicySnapshot policy) {
		if (policy.minAge() == null && policy.maxAge() == null) {
			return "제한 없음";
		}
		if (policy.minAge() == null) {
			return "만 " + policy.maxAge() + "세 이하";
		}
		if (policy.maxAge() == null) {
			return "만 " + policy.minAge() + "세 이상";
		}
		return "만 " + policy.minAge() + "~" + policy.maxAge() + "세";
	}

	private static String condition(String text, String code, String empty, String fallback) {
		return hasText(text) ? text : hasText(code) ? fallback : empty;
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
