package com.greenpocket.policy.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.policy.repository.YouthPolicyRepository;

class YouthPolicyPersistenceServiceTest {

	private YouthPolicyRepository repository;
	private YouthPolicyNormalizer normalizer;
	private CuratedYouthPolicyCatalog catalog;
	private YouthPolicyPersistenceService service;

	@BeforeEach
	void setUp() {
		repository = Mockito.mock(YouthPolicyRepository.class);
		normalizer = Mockito.mock(YouthPolicyNormalizer.class);
		catalog = Mockito.mock(CuratedYouthPolicyCatalog.class);
		service = new YouthPolicyPersistenceService(repository, normalizer, catalog);
	}

	@Test
	void validatesEveryCuratedPolicyBeforeDeletingExistingCache() {
		YouthPolicySourcePolicy invalid = Mockito.mock(YouthPolicySourcePolicy.class);
		List<YouthPolicySourcePolicy> selected = java.util.Collections.nCopies(
			CuratedYouthPolicyCatalog.CATALOG_SIZE,
			invalid
		);
		when(invalid.externalPolicyId()).thenReturn("invalid-policy");
		when(catalog.selectFrom(selected)).thenReturn(selected);
		when(normalizer.normalizeRecommendable(invalid, LocalDateTime.MIN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.replaceCache(selected, LocalDateTime.MIN))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("invalid-policy");

		verify(repository, never()).deleteAllExcept(Mockito.anySet());
		verify(repository, never()).upsert(Mockito.any());
	}
}
