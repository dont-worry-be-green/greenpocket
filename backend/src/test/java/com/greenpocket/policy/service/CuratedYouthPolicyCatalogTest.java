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
	}

	@Test
	void rejectsAnIncompleteApiCollection() {
		assertThat(org.assertj.core.api.Assertions.catchThrowable(
			() -> catalog.selectFrom(catalog.snapshot().subList(0, 59))
		)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("일부");
	}
}
