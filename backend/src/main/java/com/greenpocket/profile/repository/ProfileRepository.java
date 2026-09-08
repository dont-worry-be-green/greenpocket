package com.greenpocket.profile.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.profile.entity.AnnualIncomeBand;
import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.CurrentStatus;
import com.greenpocket.profile.entity.HouseholdStatus;
import com.greenpocket.profile.entity.HousingType;
import com.greenpocket.profile.entity.PolicyInterestCategory;

@Repository
@RequiredArgsConstructor
public class ProfileRepository {

	private final JdbcClient jdbcClient;

	public Optional<ProfileSnapshot> findByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT name, birth_date, housing_type, area_band,
				       current_status, annual_income_band, household_status,
				       onboarding_completed, policy_profile_completed,
				       eco_link_status, eco_sido_code, eco_sigungu_code,
				       eco_address_label, eco_address_registered_at
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ProfileSnapshot(
				resultSet.getString("name"),
				toLocalDate(resultSet.getDate("birth_date")),
				toEnum(resultSet.getString("housing_type"), HousingType.class),
				toEnum(resultSet.getString("area_band"), AreaBand.class),
				toEnum(resultSet.getString("current_status"), CurrentStatus.class),
				toEnum(resultSet.getString("annual_income_band"), AnnualIncomeBand.class),
				toEnum(resultSet.getString("household_status"), HouseholdStatus.class),
				resultSet.getBoolean("onboarding_completed"),
				resultSet.getBoolean("policy_profile_completed"),
				EcoLinkStatus.valueOf(resultSet.getString("eco_link_status")),
				resultSet.getString("eco_sido_code"),
				resultSet.getString("eco_sigungu_code"),
				resultSet.getString("eco_address_label"),
				toLocalDate(resultSet.getDate("eco_address_registered_at"))
			))
			.optional();
	}

	public List<PolicyInterestCategory> findInterestsByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT category
				FROM user_policy_interest
				WHERE user_id = :userId
				ORDER BY id
				""")
			.param("userId", userId)
			.query(String.class)
			.list()
			.stream()
			.map(PolicyInterestCategory::valueOf)
			.toList();
	}

	public int update(
		Long userId,
		String name,
		LocalDate birthDate,
		HousingType housingType,
		AreaBand areaBand,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		HouseholdStatus householdStatus
	) {
		return jdbcClient.sql("""
				UPDATE app_user
				SET name = :name,
				    birth_date = :birthDate,
				    housing_type = :housingType,
				    area_band = :areaBand,
				    current_status = :currentStatus,
				    annual_income_band = :annualIncomeBand,
				    household_status = :householdStatus,
				    onboarding_completed = 1,
				    policy_profile_completed = 1,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :userId
				""")
			.param("name", name)
			.param("birthDate", birthDate)
			.param("housingType", housingType.name())
			.param("areaBand", areaBand.name())
			.param("currentStatus", currentStatus.name())
			.param("annualIncomeBand", annualIncomeBand.name())
			.param("householdStatus", householdStatus.name())
			.param("userId", userId)
			.update();
	}

	public void replaceInterests(Long userId, List<PolicyInterestCategory> interests) {
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

	public record ProfileSnapshot(
		String name,
		LocalDate birthDate,
		HousingType housingType,
		AreaBand areaBand,
		CurrentStatus currentStatus,
		AnnualIncomeBand annualIncomeBand,
		HouseholdStatus householdStatus,
		boolean onboardingCompleted,
		boolean policyProfileCompleted,
		EcoLinkStatus ecoLinkStatus,
		String ecoSidoCode,
		String ecoSigunguCode,
		String ecoAddressLabel,
		LocalDate ecoAddressRegisteredAt
	) {
	}
}
