package com.greenpocket.policy.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PolicyMypageSummary(
	boolean profileCompleted,
	boolean regionLinked,
	long recommendedCount,
	List<PolicyCardResponse> preview,
	OffsetDateTime lastSyncedAt
) {
}
