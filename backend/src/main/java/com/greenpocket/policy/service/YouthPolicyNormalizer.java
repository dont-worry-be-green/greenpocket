package com.greenpocket.policy.service;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyConditionType;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.policy.repository.YouthPolicyRepository.PolicyConditionRecord;
import com.greenpocket.policy.repository.YouthPolicyRepository.PolicyRegionRecord;
import com.greenpocket.policy.repository.YouthPolicyRepository.YouthPolicyCacheRecord;
import com.greenpocket.profile.entity.PolicyInterestCategory;

@Component
public class YouthPolicyNormalizer {

	private static final int NATIONAL_REGION_CODE_COUNT = 200;

	public NormalizedPolicy normalize(YouthPolicySourcePolicy source, LocalDateTime syncedAt) {
		YouthPolicyCacheRecord policy = new YouthPolicyCacheRecord(
			truncate(source.externalPolicyId(), 30),
			truncate(source.title(), 300),
			truncate(source.keywordName(), 200),
			source.description(),
			truncate(source.largeCategoryName(), 100),
			truncate(source.mediumCategoryName(), 100),
			category(source.largeCategoryName()),
			source.supportContent(),
			truncate(source.supervisingOrgName(), 200),
			truncate(source.operatingOrgName(), 200),
			truncate(source.applicationPeriodCode(), 20),
			source.businessStartDate(),
			source.businessEndDate(),
			truncate(source.applicationDateText(), 500),
			source.applicationMethod(),
			httpUrl(source.applicationUrl()),
			httpUrl(source.referenceUrl1()),
			httpUrl(source.referenceUrl2()),
			truncate(source.ageLimitYn(), 1),
			source.minAge(),
			source.maxAge(),
			truncate(source.marriageStatusCode(), 20),
			truncate(source.incomeConditionCode(), 20),
			source.incomeMinAmount(),
			source.incomeMaxAmount(),
			source.incomeConditionText(),
			source.additionalConditionText(),
			source.participantTargetText(),
			truncate(source.majorCodes(), 500),
			truncate(source.employmentCodes(), 500),
			truncate(source.schoolCodes(), 500),
			truncate(source.specialCodes(), 500),
			applicationStatus(source.businessStartDate(), source.businessEndDate(), syncedAt.toLocalDate()),
			source.sourceRegisteredAt(),
			source.sourceModifiedAt(),
			syncedAt
		);
		return new NormalizedPolicy(policy, regions(source.regionCodes()), conditions(source));
	}

	private List<PolicyRegionRecord> regions(String rawRegionCodes) {
		Set<String> codes = splitCodes(rawRegionCodes);
		if (codes.size() >= NATIONAL_REGION_CODE_COUNT) {
			return List.of(new PolicyRegionRecord(PolicyRegionLevel.NATIONAL, "00000", "전국"));
		}
		return codes.stream()
			.filter(code -> code.length() == 5)
			.map(code -> new PolicyRegionRecord(PolicyRegionLevel.SIGUNGU, code, null))
			.toList();
	}

	private List<PolicyConditionRecord> conditions(YouthPolicySourcePolicy source) {
		List<PolicyConditionRecord> conditions = new ArrayList<>();
		if (source.minAge() != null || source.maxAge() != null) {
			conditions.add(new PolicyConditionRecord(
				PolicyConditionType.AGE,
				"",
				value(source.minAge()) + ":" + value(source.maxAge()),
				true
			));
		}
		addCondition(conditions, PolicyConditionType.INCOME, source.incomeConditionCode(),
			source.incomeConditionText(), source.incomeMinAmount() != null || source.incomeMaxAmount() != null);
		addCondition(conditions, PolicyConditionType.EMPLOYMENT, source.employmentCodes(), null, false);
		addCondition(conditions, PolicyConditionType.EDUCATION, source.schoolCodes(), null, false);
		addCondition(conditions, PolicyConditionType.MAJOR, source.majorCodes(), null, false);
		addCondition(conditions, PolicyConditionType.MARRIAGE, source.marriageStatusCode(), null, false);
		addCondition(conditions, PolicyConditionType.SPECIAL, source.specialCodes(), null, false);
		addCondition(conditions, PolicyConditionType.OTHER, "", source.additionalConditionText(), false);
		return List.copyOf(conditions);
	}

	private void addCondition(
		List<PolicyConditionRecord> conditions,
		PolicyConditionType type,
		String code,
		String value,
		boolean machineReadable
	) {
		String normalizedCode = truncate(code, 50);
		String normalizedValue = truncate(value, 500);
		if (normalizedCode == null && normalizedValue == null) {
			return;
		}
		conditions.add(new PolicyConditionRecord(
			type,
			normalizedCode == null ? "" : normalizedCode,
			normalizedValue == null ? "" : normalizedValue,
			machineReadable
		));
	}

	private static PolicyInterestCategory category(String largeCategoryName) {
		if (largeCategoryName == null) {
			return null;
		}
		if (largeCategoryName.contains("일자리")) {
			return PolicyInterestCategory.JOB;
		}
		if (largeCategoryName.contains("주거")) {
			return PolicyInterestCategory.HOUSING;
		}
		if (largeCategoryName.contains("교육")) {
			return PolicyInterestCategory.EDUCATION;
		}
		if (largeCategoryName.contains("금융") || largeCategoryName.contains("복지")
			|| largeCategoryName.contains("문화")) {
			return PolicyInterestCategory.WELFARE_CULTURE;
		}
		if (largeCategoryName.contains("참여") || largeCategoryName.contains("권리")) {
			return PolicyInterestCategory.PARTICIPATION_RIGHTS;
		}
		return null;
	}

	private static PolicyApplicationStatus applicationStatus(
		LocalDate startDate,
		LocalDate endDate,
		LocalDate 기준일
	) {
		if (startDate != null && 기준일.isBefore(startDate)) {
			return PolicyApplicationStatus.UPCOMING;
		}
		if (endDate != null && 기준일.isAfter(endDate)) {
			return PolicyApplicationStatus.CLOSED;
		}
		if (startDate != null || endDate != null) {
			return PolicyApplicationStatus.OPEN;
		}
		return PolicyApplicationStatus.UNKNOWN;
	}

	private static String httpUrl(String value) {
		if (value == null) {
			return null;
		}
		try {
			String scheme = URI.create(value).getScheme();
			if (scheme == null) {
				return null;
			}
			String normalized = scheme.toLowerCase(Locale.ROOT);
			return "http".equals(normalized) || "https".equals(normalized) ? truncate(value, 1000) : null;
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static Set<String> splitCodes(String value) {
		if (value == null || value.isBlank()) {
			return Set.of();
		}
		Set<String> result = new LinkedHashSet<>();
		for (String code : value.split(",")) {
			String normalized = code.strip();
			if (!normalized.isEmpty()) {
				result.add(normalized);
			}
		}
		return result;
	}

	private static String value(Integer value) {
		return value == null ? "" : value.toString();
	}

	private static String truncate(String value, int maximumLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.strip();
		return normalized.length() <= maximumLength ? normalized : normalized.substring(0, maximumLength);
	}

	public record NormalizedPolicy(
		YouthPolicyCacheRecord policy,
		List<PolicyRegionRecord> regions,
		List<PolicyConditionRecord> conditions
	) {
	}
}
