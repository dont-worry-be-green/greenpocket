package com.greenpocket.policy.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.policy.entity.PolicyApplicationStatus;
import com.greenpocket.policy.entity.PolicyConditionType;
import com.greenpocket.policy.entity.PolicyRegionLevel;
import com.greenpocket.profile.entity.PolicyInterestCategory;

@Repository
@RequiredArgsConstructor
public class YouthPolicyRepository {

	private final JdbcClient jdbcClient;

	public void deactivateAll() {
		jdbcClient.sql("UPDATE youth_policy SET is_active = 0, updated_at = CURRENT_TIMESTAMP")
			.update();
	}

	public Long upsert(YouthPolicyCacheRecord policy) {
		jdbcClient.sql("""
				INSERT INTO youth_policy (
				    external_policy_id, title, keyword_name, description,
				    large_category_name, medium_category_name, interest_category,
				    support_content, supervising_org_name, operating_org_name,
				    application_period_code, business_start_date, business_end_date,
				    application_date_text, application_method, application_url,
				    reference_url1, reference_url2, age_limit_yn, min_age, max_age,
				    marriage_status_code, income_condition_code, income_min_amount,
				    income_max_amount, income_condition_text, additional_condition_text,
				    participant_target_text, major_codes, employment_codes, school_codes,
				    special_codes, application_status, is_active,
				    source_registered_at, source_modified_at, synced_at
				) VALUES (
				    :externalPolicyId, :title, :keywordName, :description,
				    :largeCategoryName, :mediumCategoryName, :interestCategory,
				    :supportContent, :supervisingOrgName, :operatingOrgName,
				    :applicationPeriodCode, :businessStartDate, :businessEndDate,
				    :applicationDateText, :applicationMethod, :applicationUrl,
				    :referenceUrl1, :referenceUrl2, :ageLimitYn, :minAge, :maxAge,
				    :marriageStatusCode, :incomeConditionCode, :incomeMinAmount,
				    :incomeMaxAmount, :incomeConditionText, :additionalConditionText,
				    :participantTargetText, :majorCodes, :employmentCodes, :schoolCodes,
				    :specialCodes, :applicationStatus, 1,
				    :sourceRegisteredAt, :sourceModifiedAt, :syncedAt
				)
				ON DUPLICATE KEY UPDATE
				    title = VALUES(title), keyword_name = VALUES(keyword_name),
				    description = VALUES(description), large_category_name = VALUES(large_category_name),
				    medium_category_name = VALUES(medium_category_name),
				    interest_category = VALUES(interest_category), support_content = VALUES(support_content),
				    supervising_org_name = VALUES(supervising_org_name),
				    operating_org_name = VALUES(operating_org_name),
				    application_period_code = VALUES(application_period_code),
				    business_start_date = VALUES(business_start_date), business_end_date = VALUES(business_end_date),
				    application_date_text = VALUES(application_date_text), application_method = VALUES(application_method),
				    application_url = VALUES(application_url), reference_url1 = VALUES(reference_url1),
				    reference_url2 = VALUES(reference_url2), age_limit_yn = VALUES(age_limit_yn),
				    min_age = VALUES(min_age), max_age = VALUES(max_age),
				    marriage_status_code = VALUES(marriage_status_code),
				    income_condition_code = VALUES(income_condition_code),
				    income_min_amount = VALUES(income_min_amount), income_max_amount = VALUES(income_max_amount),
				    income_condition_text = VALUES(income_condition_text),
				    additional_condition_text = VALUES(additional_condition_text),
				    participant_target_text = VALUES(participant_target_text),
				    major_codes = VALUES(major_codes), employment_codes = VALUES(employment_codes),
				    school_codes = VALUES(school_codes), special_codes = VALUES(special_codes),
				    application_status = VALUES(application_status), is_active = 1,
				    source_registered_at = VALUES(source_registered_at),
				    source_modified_at = VALUES(source_modified_at), synced_at = VALUES(synced_at),
				    updated_at = CURRENT_TIMESTAMP
				""")
			.param("externalPolicyId", policy.externalPolicyId())
			.param("title", policy.title())
			.param("keywordName", policy.keywordName())
			.param("description", policy.description())
			.param("largeCategoryName", policy.largeCategoryName())
			.param("mediumCategoryName", policy.mediumCategoryName())
			.param("interestCategory", enumName(policy.interestCategory()))
			.param("supportContent", policy.supportContent())
			.param("supervisingOrgName", policy.supervisingOrgName())
			.param("operatingOrgName", policy.operatingOrgName())
			.param("applicationPeriodCode", policy.applicationPeriodCode())
			.param("businessStartDate", policy.businessStartDate())
			.param("businessEndDate", policy.businessEndDate())
			.param("applicationDateText", policy.applicationDateText())
			.param("applicationMethod", policy.applicationMethod())
			.param("applicationUrl", policy.applicationUrl())
			.param("referenceUrl1", policy.referenceUrl1())
			.param("referenceUrl2", policy.referenceUrl2())
			.param("ageLimitYn", policy.ageLimitYn())
			.param("minAge", policy.minAge())
			.param("maxAge", policy.maxAge())
			.param("marriageStatusCode", policy.marriageStatusCode())
			.param("incomeConditionCode", policy.incomeConditionCode())
			.param("incomeMinAmount", policy.incomeMinAmount())
			.param("incomeMaxAmount", policy.incomeMaxAmount())
			.param("incomeConditionText", policy.incomeConditionText())
			.param("additionalConditionText", policy.additionalConditionText())
			.param("participantTargetText", policy.participantTargetText())
			.param("majorCodes", policy.majorCodes())
			.param("employmentCodes", policy.employmentCodes())
			.param("schoolCodes", policy.schoolCodes())
			.param("specialCodes", policy.specialCodes())
			.param("applicationStatus", policy.applicationStatus().name())
			.param("sourceRegisteredAt", policy.sourceRegisteredAt())
			.param("sourceModifiedAt", policy.sourceModifiedAt())
			.param("syncedAt", policy.syncedAt())
			.update();
		return jdbcClient.sql("SELECT id FROM youth_policy WHERE external_policy_id = :externalPolicyId")
			.param("externalPolicyId", policy.externalPolicyId())
			.query(Long.class)
			.single();
	}

	public void replaceRegions(Long policyId, List<PolicyRegionRecord> regions) {
		jdbcClient.sql("DELETE FROM youth_policy_region WHERE youth_policy_id = :policyId")
			.param("policyId", policyId)
			.update();
		for (PolicyRegionRecord region : regions) {
			jdbcClient.sql("""
					INSERT INTO youth_policy_region (youth_policy_id, region_level, region_code, region_name)
					VALUES (:policyId, :regionLevel, :regionCode, :regionName)
					""")
				.param("policyId", policyId)
				.param("regionLevel", region.regionLevel().name())
				.param("regionCode", region.regionCode())
				.param("regionName", region.regionName())
				.update();
		}
	}

	public void replaceConditions(Long policyId, List<PolicyConditionRecord> conditions) {
		jdbcClient.sql("DELETE FROM youth_policy_condition WHERE youth_policy_id = :policyId")
			.param("policyId", policyId)
			.update();
		for (PolicyConditionRecord condition : conditions) {
			jdbcClient.sql("""
					INSERT INTO youth_policy_condition (
					    youth_policy_id, condition_type, condition_code, condition_value, machine_readable
					) VALUES (:policyId, :conditionType, :conditionCode, :conditionValue, :machineReadable)
					""")
				.param("policyId", policyId)
				.param("conditionType", condition.conditionType().name())
				.param("conditionCode", condition.conditionCode())
				.param("conditionValue", condition.conditionValue())
				.param("machineReadable", condition.machineReadable())
				.update();
		}
	}

	private static String enumName(Enum<?> value) {
		return value == null ? null : value.name();
	}

	public record YouthPolicyCacheRecord(
		String externalPolicyId,
		String title,
		String keywordName,
		String description,
		String largeCategoryName,
		String mediumCategoryName,
		PolicyInterestCategory interestCategory,
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
		String majorCodes,
		String employmentCodes,
		String schoolCodes,
		String specialCodes,
		PolicyApplicationStatus applicationStatus,
		LocalDateTime sourceRegisteredAt,
		LocalDateTime sourceModifiedAt,
		LocalDateTime syncedAt
	) {
	}

	public record PolicyRegionRecord(
		PolicyRegionLevel regionLevel,
		String regionCode,
		String regionName
	) {
	}

	public record PolicyConditionRecord(
		PolicyConditionType conditionType,
		String conditionCode,
		String conditionValue,
		boolean machineReadable
	) {
	}
}
