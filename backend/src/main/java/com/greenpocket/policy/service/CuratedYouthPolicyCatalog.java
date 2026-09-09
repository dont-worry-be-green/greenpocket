package com.greenpocket.policy.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.jackson.databind.ObjectMapper;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.greenpocket.policy.external.YouthPolicySourcePolicy;

@Component
public class CuratedYouthPolicyCatalog {

	static final int CATALOG_SIZE = 60;
	private static final String SNAPSHOT_PATH = "data/curated-youth-policies.json";

	private final List<YouthPolicySourcePolicy> snapshot;
	private final Set<String> externalPolicyIds;

	public CuratedYouthPolicyCatalog(ObjectMapper objectMapper) {
		this.snapshot = loadSnapshot(objectMapper);
		this.externalPolicyIds = snapshot.stream()
			.map(YouthPolicySourcePolicy::externalPolicyId)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
		if (snapshot.size() != CATALOG_SIZE || externalPolicyIds.size() != CATALOG_SIZE) {
			throw new IllegalStateException("검증된 청년정책 카탈로그는 중복 없이 정확히 60건이어야 합니다.");
		}
		if (snapshot.stream().anyMatch(CuratedYouthPolicyCatalog::hasNoSpecificTargetCondition)) {
			throw new IllegalStateException("검증된 청년정책에는 하나 이상의 구체적인 대상 조건이 있어야 합니다.");
		}
	}

	public List<YouthPolicySourcePolicy> snapshot() {
		return snapshot;
	}

	public Set<String> externalPolicyIds() {
		return externalPolicyIds;
	}

	public List<YouthPolicySourcePolicy> selectFrom(List<YouthPolicySourcePolicy> policies) {
		Map<String, YouthPolicySourcePolicy> byId = new LinkedHashMap<>();
		for (YouthPolicySourcePolicy policy : policies) {
			if (externalPolicyIds.contains(policy.externalPolicyId())) {
				byId.put(policy.externalPolicyId(), policy);
			}
		}
		Set<String> missing = new LinkedHashSet<>(externalPolicyIds);
		missing.removeAll(byId.keySet());
		if (!missing.isEmpty()) {
			throw new IllegalStateException("검증된 청년정책 60건 중 일부를 원본 API에서 찾지 못했습니다.");
		}
		// API는 선별 ID의 존재와 최신 제공 여부만 확인한다. 화면에 쓰는 대상·신청 정보는
		// 사람이 검증한 스냅샷을 유지해 원본의 과거 사업연도·누락 링크가 다시 덮지 않게 한다.
		return snapshot;
	}

	private static List<YouthPolicySourcePolicy> loadSnapshot(ObjectMapper objectMapper) {
		try (InputStream inputStream = new ClassPathResource(SNAPSHOT_PATH).getInputStream()) {
			YouthPolicySourcePolicy[] policies = objectMapper.readValue(
				inputStream,
				YouthPolicySourcePolicy[].class
			);
			return List.of(policies);
		}
		catch (IOException exception) {
			throw new IllegalStateException("검증된 청년정책 스냅샷을 읽을 수 없습니다.", exception);
		}
	}

	private static boolean hasNoSpecificTargetCondition(YouthPolicySourcePolicy policy) {
		return policy.minAge() == null && policy.maxAge() == null
			&& "0043001".equals(policy.incomeConditionCode())
			&& "0013010".equals(policy.employmentCodes())
			&& "0049010".equals(policy.schoolCodes())
			&& "0011009".equals(policy.majorCodes())
			&& "0055003".equals(policy.marriageStatusCode())
			&& "0014010".equals(policy.specialCodes())
			&& !hasText(policy.participantTargetText())
			&& !hasText(policy.additionalConditionText());
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
