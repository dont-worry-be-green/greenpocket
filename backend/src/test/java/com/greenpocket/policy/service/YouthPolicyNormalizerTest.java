package com.greenpocket.policy.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyConditionType;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.profile.entity.PolicyInterestCategory;

class YouthPolicyNormalizerTest {

	private final YouthPolicyNormalizer normalizer = new YouthPolicyNormalizer();

	@Test
	void normalizesCategoryStatusRegionConditionsAndSafeUrls() {
		YouthPolicySourcePolicy source = new YouthPolicySourcePolicy(
			"P-1", "청년 취업 지원", null, "설명", "일자리", "취업", "지원 내용",
			"주관기관", "운영기관", "0057001",
			LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, "온라인 신청",
			"javascript:alert(1)", "https://example.go.kr", null,
			"N", 19, 39, "0055003", "0043001", null, null, "중위소득 확인",
			"세부 자격 확인", "미취업 청년", "11620", "0011009", "0013001", "0049010",
			"0014001", LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 9, 1, 0, 0)
		);

		var normalized = normalizer.normalize(source, LocalDateTime.of(2026, 9, 8, 12, 0));

		assertThat(normalized.policy().interestCategory()).isEqualTo(PolicyInterestCategory.JOB);
		assertThat(normalized.policy().applicationStatus()).isEqualTo(PolicyApplicationStatus.OPEN);
		assertThat(normalized.policy().applicationUrl()).isNull();
		assertThat(normalized.policy().referenceUrl1()).isEqualTo("https://example.go.kr");
		assertThat(normalized.regions()).singleElement().satisfies(region -> {
			assertThat(region.regionLevel()).isEqualTo(PolicyRegionLevel.SIGUNGU);
			assertThat(region.regionCode()).isEqualTo("11620");
		});
		assertThat(normalized.conditions())
			.extracting(condition -> condition.conditionType())
			.contains(PolicyConditionType.AGE, PolicyConditionType.INCOME, PolicyConditionType.EMPLOYMENT);
	}

	@Test
	void marksFutureAndPastPoliciesByBusinessPeriod() {
		YouthPolicySourcePolicy future = policyWithPeriod(
			LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)
		);
		YouthPolicySourcePolicy past = policyWithPeriod(
			LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)
		);
		LocalDateTime 기준일 = LocalDateTime.of(2026, 9, 8, 12, 0);

		assertThat(normalizer.normalize(future, 기준일).policy().applicationStatus())
			.isEqualTo(PolicyApplicationStatus.UPCOMING);
		assertThat(normalizer.normalize(past, 기준일).policy().applicationStatus())
			.isEqualTo(PolicyApplicationStatus.CLOSED);
	}

	private YouthPolicySourcePolicy policyWithPeriod(LocalDate startDate, LocalDate endDate) {
		return new YouthPolicySourcePolicy(
			"P-1", "정책", null, null, null, null, null, null, null, null,
			startDate, endDate, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null,
			"11620", null, null, null, null, null, null
		);
	}
}
