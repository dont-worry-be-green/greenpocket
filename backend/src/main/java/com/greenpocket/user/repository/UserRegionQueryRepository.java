package com.greenpocket.user.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRegionQueryRepository {

	private final JdbcClient jdbcClient;

	public Optional<String> findSidoCodeByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT sido_code
				FROM app_user
				WHERE id = :userId
				  AND sido_code IS NOT NULL
				""")
			.param("userId", userId)
			.query(String.class)
			.optional();
	}

	public Optional<UserDiagnosisProfileSnapshot> findDiagnosisProfileByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT sido_code, sido_name, sigungu_code, sigungu_name, housing_type, area_band
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new UserDiagnosisProfileSnapshot(
				resultSet.getString("sido_code"),
				resultSet.getString("sido_name"),
				resultSet.getString("sigungu_code"),
				resultSet.getString("sigungu_name"),
				resultSet.getString("housing_type"),
				resultSet.getString("area_band")
			))
			.optional();
	}

	public record UserDiagnosisProfileSnapshot(
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		String housingType,
		String areaBand
	) {
	}
}
