package com.greenpocket.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.greenpocket.policy.external.YouthPolicyApiClient;
import com.greenpocket.policy.external.YouthPolicyPage;
import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.policy.repository.YouthPolicySyncRepository;

class YouthPolicySyncServiceTest {

	private YouthPolicyApiClient client;
	private YouthPolicySyncService service;

	@BeforeEach
	void setUp() {
		client = mock(YouthPolicyApiClient.class);
		service = new YouthPolicySyncService(
			client,
			mock(YouthPolicyPersistenceService.class),
			mock(YouthPolicySyncRepository.class)
		);
		ReflectionTestUtils.setField(service, "pageSize", 2);
	}

	@Test
	void fetchesEveryPageUsingSourceTotalCount() {
		YouthPolicySourcePolicy first = policy("P-1");
		YouthPolicySourcePolicy second = policy("P-2");
		YouthPolicySourcePolicy third = policy("P-3");
		when(client.fetchPage(1, 2)).thenReturn(new YouthPolicyPage(3, 1, 2, List.of(first, second)));
		when(client.fetchPage(2, 2)).thenReturn(new YouthPolicyPage(3, 2, 2, List.of(third)));

		YouthPolicySyncService.FetchedPolicies result = service.fetchAll();

		assertThat(result.totalCount()).isEqualTo(3);
		assertThat(result.lastPage()).isEqualTo(2);
		assertThat(result.policies()).extracting(YouthPolicySourcePolicy::externalPolicyId)
			.containsExactly("P-1", "P-2", "P-3");
		verify(client).fetchPage(1, 2);
		verify(client).fetchPage(2, 2);
	}

	@Test
	void rejectsIncompleteCollectionBeforeReplacingCache() {
		when(client.fetchPage(1, 2)).thenReturn(new YouthPolicyPage(2, 1, 2, List.of(policy("P-1"))));

		assertThatThrownBy(() -> service.fetchAll())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("전체 건수");
	}

	private YouthPolicySourcePolicy policy(String id) {
		return new YouthPolicySourcePolicy(
			id, "정책", null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null,
			"11620", null, null, null, null, null, null
		);
	}
}
