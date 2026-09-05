package com.greenpocket.profile.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.profile.entity.AreaBand;
import com.greenpocket.profile.entity.HousingType;

@Repository
@RequiredArgsConstructor
public class ProfileRepository {

	private final JdbcClient jdbcClient;

	public Optional<ProfileSnapshot> findByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT name, sido_code, sido_name, sigungu_code, sigungu_name,
				       housing_type, area_band, onboarding_completed
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ProfileSnapshot(
				resultSet.getString("name"),
				resultSet.getString("sido_code"),
				resultSet.getString("sido_name"),
				resultSet.getString("sigungu_code"),
				resultSet.getString("sigungu_name"),
				toHousingType(resultSet.getString("housing_type")),
				toAreaBand(resultSet.getString("area_band")),
				resultSet.getBoolean("onboarding_completed")
			))
			.optional();
	}

	public int update(
		Long userId,
		String name,
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		HousingType housingType,
		AreaBand areaBand
	) {
		return jdbcClient.sql("""
				UPDATE app_user
				SET name = :name,
				    sido_code = :sidoCode,
				    sido_name = :sidoName,
				    sigungu_code = :sigunguCode,
				    sigungu_name = :sigunguName,
				    housing_type = :housingType,
				    area_band = :areaBand,
				    onboarding_completed = 1,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :userId
				""")
			.param("name", name)
			.param("sidoCode", sidoCode)
			.param("sidoName", sidoName)
			.param("sigunguCode", sigunguCode)
			.param("sigunguName", sigunguName)
			.param("housingType", housingType.name())
			.param("areaBand", areaBand.name())
			.param("userId", userId)
			.update();
	}

	private static HousingType toHousingType(String value) {
		return value == null ? null : HousingType.valueOf(value);
	}

	private static AreaBand toAreaBand(String value) {
		return value == null ? null : AreaBand.valueOf(value);
	}

	public record ProfileSnapshot(
		String name,
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		HousingType housingType,
		AreaBand areaBand,
		boolean onboardingCompleted
	) {
	}
}
