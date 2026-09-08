package com.greenpocket.policy.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyMatchStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record PolicyDetailResponse(
	String policyId,
	String title,
	PolicyInterestCategory category,
	String subCategory,
	String description,
	String supportContent,
	Application application,
	Organizations organizations,
	Conditions conditions,
	Match match,
	List<String> referenceUrls,
	String source,
	OffsetDateTime lastSyncedAt
) {

	public record Application(
		PolicyApplicationStatus status,
		String periodType,
		LocalDate startDate,
		LocalDate endDate,
		String method,
		String url
	) {
	}

	public record Organizations(
		String supervising,
		String operating
	) {
	}

	public record Conditions(
		String age,
		String income,
		String employment,
		String education,
		String major,
		String marriage,
		String special
	) {
	}

	public record Match(
		PolicyMatchStatus status,
		int score,
		List<String> reasons
	) {
	}
}
