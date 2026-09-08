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
				SELECT eco_sido_code
				FROM app_user
				WHERE id = :userId
				  AND eco_link_status = 'LINKED'
				  AND eco_sido_code IS NOT NULL
				""")
			.param("userId", userId)
			.query(String.class)
			.optional();
	}

	public Optional<UserDiagnosisProfileSnapshot> findDiagnosisProfileByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT
				    CASE WHEN eco_link_status = 'LINKED' THEN eco_sido_code END AS sido_code,
				    CASE WHEN eco_link_status = 'LINKED' THEN SUBSTRING_INDEX(eco_address_label, ' ', 1) END AS sido_name,
				    CASE WHEN eco_link_status = 'LINKED' THEN eco_sigungu_code END AS sigungu_code,
				    CASE WHEN eco_link_status = 'LINKED' THEN SUBSTRING_INDEX(eco_address_label, ' ', -1) END AS sigungu_name,
				    housing_type,
				    area_band
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
