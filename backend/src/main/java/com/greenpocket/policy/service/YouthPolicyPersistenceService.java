package com.greenpocket.policy.service;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.policy.repository.YouthPolicyRepository;

@Service
@RequiredArgsConstructor
public class YouthPolicyPersistenceService {

	private final YouthPolicyRepository youthPolicyRepository;
	private final YouthPolicyNormalizer youthPolicyNormalizer;

	@Transactional
	public int replaceCache(List<YouthPolicySourcePolicy> policies, LocalDateTime syncedAt) {
		youthPolicyRepository.deactivateAll();
		int upsertedCount = 0;
		for (YouthPolicySourcePolicy source : policies) {
			YouthPolicyNormalizer.NormalizedPolicy normalized = youthPolicyNormalizer.normalize(source, syncedAt);
			Long policyId = youthPolicyRepository.upsert(normalized.policy());
			youthPolicyRepository.replaceRegions(policyId, normalized.regions());
			youthPolicyRepository.replaceConditions(policyId, normalized.conditions());
			upsertedCount++;
		}
		return upsertedCount;
	}
}
