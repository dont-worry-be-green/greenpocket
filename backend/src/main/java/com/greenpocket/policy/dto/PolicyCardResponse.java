package com.greenpocket.policy.dto;

import java.time.LocalDate;
import java.util.List;

import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyMatchStatus;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.profile.entity.PolicyInterestCategory;

public record PolicyCardResponse(
	String policyId,
	String title,
	PolicyInterestCategory category,
	String subCategory,
	String supportSummary,
	PolicyApplicationStatus applicationStatus,
	LocalDate applicationEndDate,
	PolicyMatchStatus matchStatus,
	Integer matchScore,
	List<String> matchReasons,
	PolicyRegionLevel regionScope
) {
}
