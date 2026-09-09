package com.greenpocket.policy.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.policy.external.YouthPolicySourcePolicy;
import com.greenpocket.policy.repository.YouthPolicyRepository;

@Service
@RequiredArgsConstructor
public class YouthPolicyPersistenceService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final YouthPolicyRepository youthPolicyRepository;
	private final YouthPolicyNormalizer youthPolicyNormalizer;
	private final CuratedYouthPolicyCatalog curatedYouthPolicyCatalog;

	@Transactional
	public int replaceCache(List<YouthPolicySourcePolicy> policies, LocalDateTime syncedAt) {
		return replaceCuratedCache(curatedYouthPolicyCatalog.selectFrom(policies), syncedAt);
	}

	@Transactional
	public void ensureCuratedCache() {
		if (!youthPolicyRepository.findActiveExternalIds().equals(curatedYouthPolicyCatalog.externalPolicyIds())) {
			replaceCuratedCache(
				curatedYouthPolicyCatalog.snapshot(),
				LocalDateTime.now(KOREA_ZONE_ID)
			);
			return;
		}
		youthPolicyRepository.deleteAllExcept(curatedYouthPolicyCatalog.externalPolicyIds());
	}

	private int replaceCuratedCache(List<YouthPolicySourcePolicy> policies, LocalDateTime syncedAt) {
		List<YouthPolicyNormalizer.NormalizedPolicy> normalizedPolicies = policies.stream()
			.map(source -> youthPolicyNormalizer.normalizeRecommendable(source, syncedAt)
				.orElseThrow(() -> new IllegalStateException(
					"검증된 청년정책이 현재 활성화 조건을 충족하지 않습니다: " + source.externalPolicyId()
				)))
			.toList();
		if (normalizedPolicies.size() != CuratedYouthPolicyCatalog.CATALOG_SIZE) {
			throw new IllegalStateException("검증된 청년정책은 정확히 60건이어야 합니다.");
		}
		int upsertedCount = 0;
		for (YouthPolicyNormalizer.NormalizedPolicy normalized : normalizedPolicies) {
			Long policyId = youthPolicyRepository.upsert(normalized.policy());
			youthPolicyRepository.replaceRegions(policyId, normalized.regions());
			youthPolicyRepository.replaceConditions(policyId, normalized.conditions());
			upsertedCount++;
		}
		youthPolicyRepository.deleteAllExcept(curatedYouthPolicyCatalog.externalPolicyIds());
		return upsertedCount;
	}
}
