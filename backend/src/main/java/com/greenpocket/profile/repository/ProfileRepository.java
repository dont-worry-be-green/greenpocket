package com.greenpocket.profile.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.EducationStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.PolicyInterestCategory;
import com.greenpocket.user.entity.Gender;

@Repository
@RequiredArgsConstructor
public class ProfileRepository {

	private final JdbcClient jdbcClient;

	public Optional<ProfileSnapshot> findByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT name, birth_date, gender, phone_number,
				       current_status, annual_income_band, education_status, household_status,
				       policy_profile_completed,
				       (SELECT GROUP_CONCAT(interest.category ORDER BY interest.id SEPARATOR ',')
				        FROM user_policy_interest interest
				        WHERE interest.user_id = app_user.id) AS interest_categories,
				       eco_link_status, eco_sido_code, eco_sigungu_code,
				       eco_address_label, eco_address_registered_at
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ProfileSnapshot(
				resultSet.getString("name"),
				toLocalDate(resultSet.getDate("birth_date")),
				toEnum(resultSet.getString("gender"), Gender.class),
				resultSet.getString("phone_number"),
				toEnum(resultSet.getString("current_status"), CurrentStatus.class),
				toEnum(resultSet.getString("annual_income_band"), AnnualIncomeBand.class),
				toEnum(resultSet.getString("education_status"), EducationStatus.class),
				toEnum(resultSet.getString("household_status"), HouseholdStatus.class),
				resultSet.getBoolean("policy_profile_completed"),
				toEnums(resultSet.getString("interest_categories"), PolicyInterestCategory.class),
				EcoLinkStatus.valueOf(resultSet.getString("eco_link_status")),
				resultSet.getString("eco_sido_code"),
				resultSet.getString("eco_sigungu_code"),
				resultSet.getString("eco_address_label"),
				toLocalDate(resultSet.getDate("eco_address_registered_at"))
			))
			.optional();
	}

	public int updatePolicyPreferences(
		Long userId,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		EducationStatus educationStatus
	) {
		return jdbcClient.sql("""
				UPDATE app_user
				SET current_status = :currentStatus,
				    annual_income_band = :annualIncomeBand,
				    education_status = :educationStatus,
				    policy_profile_completed = 1,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :userId
				""")
			.param("currentStatus", currentStatus.name())
			.param("annualIncomeBand", annualIncomeBand.name())
			.param("educationStatus", educationStatus.name())
			.param("userId", userId)
			.update();
	}

	public void replacePolicyInterests(Long userId, List<PolicyInterestCategory> interests) {
		jdbcClient.sql("DELETE FROM user_policy_interest WHERE user_id = :userId")
			.param("userId", userId)
			.update();
		for (PolicyInterestCategory interest : interests) {
			jdbcClient.sql("""
					INSERT INTO user_policy_interest (user_id, category)
					VALUES (:userId, :category)
					""")
				.param("userId", userId)
				.param("category", interest.name())
				.update();
		}
	}

	private static LocalDate toLocalDate(Date value) {
		return value == null ? null : value.toLocalDate();
	}

	private static <T extends Enum<T>> T toEnum(String value, Class<T> enumType) {
		return value == null ? null : Enum.valueOf(enumType, value);
	}

	private static <T extends Enum<T>> List<T> toEnums(String value, Class<T> enumType) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return Arrays.stream(value.split(","))
			.map(item -> Enum.valueOf(enumType, item))
			.toList();
	}

	public record ProfileSnapshot(
		String name,
		LocalDate birthDate,
		Gender gender,
		String phoneNumber,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		EducationStatus educationStatus,
		HouseholdStatus householdStatus,
		boolean policyProfileCompleted,
		List<PolicyInterestCategory> interestCategories,
		EcoLinkStatus ecoLinkStatus,
		String ecoSidoCode,
		String ecoSigunguCode,
		String ecoAddressLabel,
		LocalDate ecoAddressRegisteredAt
	) {
	}
}
