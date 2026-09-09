package com.greenpocket.policy.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class CuratedYouthPolicyCatalogTest {

	private final CuratedYouthPolicyCatalog catalog = new CuratedYouthPolicyCatalog(new ObjectMapper());
	private final YouthPolicyNormalizer normalizer = new YouthPolicyNormalizer();

	@Test
	void containsExactlySixtyUniquePoliciesThatPassActivationRules() {
		assertThat(catalog.snapshot()).hasSize(CuratedYouthPolicyCatalog.CATALOG_SIZE);
		assertThat(catalog.externalPolicyIds()).hasSize(CuratedYouthPolicyCatalog.CATALOG_SIZE);
		assertThat(catalog.snapshot())
			.allSatisfy(policy -> assertThat(normalizer.normalizeRecommendable(
				policy,
				LocalDateTime.of(2026, 9, 9, 0, 0)
			)).as(policy.externalPolicyId()).isPresent());
		assertThat(catalog.snapshot())
			.extracting(policy -> policy.title())
			.doesNotContain(
				"온국민평생배움터 운영",
				"한국형 온라인 공개강좌(K-MOOC)",
				"전세보증금 반환보증 보증료 지원",
				"부산 자립준비청년 자립수당 지원"
			);
	}

	@Test
	void rejectsAnIncompleteApiCollection() {
		assertThat(org.assertj.core.api.Assertions.catchThrowable(
			() -> catalog.selectFrom(catalog.snapshot().subList(0, 59))
		)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("일부");
	}

	@Test
	void containsFiveDetailedSeoulOrNationalHousingPolicies() {
		var housing = catalog.snapshot().stream()
			.filter(policy -> "주거".equals(policy.largeCategoryName()))
			.filter(policy -> policy.regionCodes().contains("11620"))
			.toList();

		assertThat(housing).hasSizeGreaterThanOrEqualTo(5);
		assertThat(housing.stream().limit(5).toList()).allSatisfy(policy -> {
			assertThat(policy.description()).isNotBlank();
			assertThat(policy.supportContent()).isNotBlank();
			assertThat(policy.applicationMethod()).isNotBlank();
			assertThat(policy.applicationUrl()).startsWith("https://");
		});
	}
}
