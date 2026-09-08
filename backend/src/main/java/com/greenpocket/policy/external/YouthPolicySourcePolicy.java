package com.greenpocket.policy.external;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record YouthPolicySourcePolicy(
	String externalPolicyId,
	String title,
	String keywordName,
	String description,
	String largeCategoryName,
	String mediumCategoryName,
	String supportContent,
	String supervisingOrgName,
	String operatingOrgName,
	String applicationPeriodCode,
	LocalDate businessStartDate,
	LocalDate businessEndDate,
	String applicationDateText,
	String applicationMethod,
	String applicationUrl,
	String referenceUrl1,
	String referenceUrl2,
	String ageLimitYn,
	Integer minAge,
	Integer maxAge,
	String marriageStatusCode,
	String incomeConditionCode,
	Long incomeMinAmount,
	Long incomeMaxAmount,
	String incomeConditionText,
	String additionalConditionText,
	String participantTargetText,
	String regionCodes,
	String majorCodes,
	String employmentCodes,
	String schoolCodes,
	String specialCodes,
	LocalDateTime sourceRegisteredAt,
	LocalDateTime sourceModifiedAt
) {
}
