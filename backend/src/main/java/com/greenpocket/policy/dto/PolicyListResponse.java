package com.greenpocket.policy.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.policy.entity.PolicyRegionLevel;

public record PolicyListResponse(
	List<PolicyCardResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext,
	Region region,
	OffsetDateTime lastSyncedAt
) {

	public record Region(
		boolean linked,
		String label,
		List<PolicyRegionLevel> appliedLevels
	) {
	}
}
