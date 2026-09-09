package com.greenpocket.policy.service;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final String APPROVED = "0044002";
	private static final String APPLICATION_FIXED = "0057001";
	private static final String APPLICATION_ALWAYS = "0057002";
	private static final String APPLICATION_CLOSED = "0057003";
	private static final Pattern BASIC_DATE = Pattern.compile("(?<!\\d)(\\d{8})(?!\\d)");
	private static final Set<String> INDIVIDUAL_PROVISION_METHODS = Set.of(
		"0042002", // 프로그램
		"0042003", // 직접대출
		"0042006", // 보조금
		"0042007", // 대출보증
		"0042008", // 공적보험
		"0042009", // 조세지출
		"0042010"  // 바우처
	);
	private static final Set<String> NON_ACTIONABLE_APPLICATION_METHODS = Set.of(
		"-", "해당없음", "해당 없음", "별도문의", "별도 문의",
		"온라인", "온라인 신청", "방문", "방문 신청", "우편", "이메일", "전화", "홈페이지"
	);

	public Optional<NormalizedPolicy> normalizeRecommendable(YouthPolicySourcePolicy source, LocalDateTime syncedAt) {
		NormalizedPolicy normalized = normalize(source, syncedAt);
		if (!APPROVED.equals(source.approvalStatusCode())
			|| !INDIVIDUAL_PROVISION_METHODS.contains(source.provisionMethodCode())
			|| normalized.policy().applicationStatus() != PolicyApplicationStatus.OPEN
			|| normalized.policy().interestCategory() == null
			|| normalized.regions().isEmpty()
			|| !hasConcreteApplicationRoute(normalized.policy())) {
			return Optional.empty();
		}
		return Optional.of(normalized);
	}

	public NormalizedPolicy normalize(YouthPolicySourcePolicy source, LocalDateTime syncedAt) {
		ApplicationWindow applicationWindow = applicationWindow(
			source.applicationPeriodCode(), source.applicationDateText(), syncedAt.toLocalDate()
		);
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
			truncate(source.approvalStatusCode(), 20),
			truncate(source.provisionMethodCode(), 20),
			truncate(source.applicationPeriodCode(), 20),
			applicationWindow.startDate(),
			applicationWindow.endDate(),
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
			applicationWindow.status(),
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

	private static ApplicationWindow applicationWindow(String periodCode, String dateText, LocalDate 기준일) {
		if (APPLICATION_ALWAYS.equals(periodCode)) {
			return new ApplicationWindow(PolicyApplicationStatus.OPEN, null, null);
		}
		if (APPLICATION_CLOSED.equals(periodCode)) {
			return new ApplicationWindow(PolicyApplicationStatus.CLOSED, null, null);
		}
		if (!APPLICATION_FIXED.equals(periodCode)) {
			return new ApplicationWindow(PolicyApplicationStatus.UNKNOWN, null, null);
		}

		List<LocalDate> dates = basicDates(dateText);
		List<ApplicationWindow> windows = new ArrayList<>();
		for (int index = 0; index + 1 < dates.size(); index += 2) {
			LocalDate startDate = dates.get(index);
			LocalDate endDate = dates.get(index + 1);
			if (!startDate.isAfter(endDate)) {
				windows.add(new ApplicationWindow(PolicyApplicationStatus.UNKNOWN, startDate, endDate));
			}
		}
		for (ApplicationWindow window : windows) {
			if (!기준일.isBefore(window.startDate()) && !기준일.isAfter(window.endDate())) {
				return new ApplicationWindow(PolicyApplicationStatus.OPEN, window.startDate(), window.endDate());
			}
		}
		return windows.stream()
			.filter(window -> 기준일.isBefore(window.startDate()))
			.min(java.util.Comparator.comparing(ApplicationWindow::startDate))
			.map(window -> new ApplicationWindow(PolicyApplicationStatus.UPCOMING, window.startDate(), window.endDate()))
			.orElseGet(() -> windows.stream()
				.max(java.util.Comparator.comparing(ApplicationWindow::endDate))
				.map(window -> new ApplicationWindow(PolicyApplicationStatus.CLOSED, window.startDate(), window.endDate()))
				.orElse(new ApplicationWindow(PolicyApplicationStatus.UNKNOWN, null, null)));
	}

	private static List<LocalDate> basicDates(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<LocalDate> dates = new ArrayList<>();
		Matcher matcher = BASIC_DATE.matcher(value);
		while (matcher.find()) {
			try {
				dates.add(LocalDate.parse(matcher.group(1), java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
			}
			catch (java.time.DateTimeException ignored) {
				// 잘못된 원본 날짜는 추천 가능한 신청기간으로 간주하지 않는다.
			}
		}
		return List.copyOf(dates);
	}

	private static boolean hasConcreteApplicationRoute(YouthPolicyCacheRecord policy) {
		if (policy.applicationUrl() != null || policy.referenceUrl1() != null || policy.referenceUrl2() != null) {
			return true;
		}
		String method = policy.applicationMethod();
		return method != null && !method.isBlank()
			&& !NON_ACTIONABLE_APPLICATION_METHODS.contains(method.strip());
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

	private record ApplicationWindow(
		PolicyApplicationStatus status,
		LocalDate startDate,
		LocalDate endDate
	) {
	}
}
