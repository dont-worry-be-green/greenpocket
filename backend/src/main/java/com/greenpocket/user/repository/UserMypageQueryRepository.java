package com.greenpocket.user.repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.EcoLinkStatus;

@Repository
@RequiredArgsConstructor
public class UserMypageQueryRepository {

	private final JdbcClient jdbcClient;

	public Optional<UserMypageSnapshot> findByUserId(Long userId) {
		return jdbcClient.sql("""
				SELECT name, sido_code, sido_name, sigungu_code, sigungu_name,
				       housing_type, area_band,
				       eco_link_status, eco_linked_at,
				       eco_sido_code, eco_sigungu_code,
				       eco_address_label, eco_address_registered_at,
				       greenlife_participating, greenlife_linked_at,
				       pocket_account_no
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new UserMypageSnapshot(
				resultSet.getString("name"),
				resultSet.getString("sido_code"),
				resultSet.getString("sido_name"),
				resultSet.getString("sigungu_code"),
				resultSet.getString("sigungu_name"),
				resultSet.getString("housing_type"),
				resultSet.getString("area_band"),
				EcoLinkStatus.valueOf(resultSet.getString("eco_link_status")),
				toLocalDateTime(resultSet.getTimestamp("eco_linked_at")),
				resultSet.getString("eco_sido_code"),
				resultSet.getString("eco_sigungu_code"),
				resultSet.getString("eco_address_label"),
				toLocalDate(resultSet.getDate("eco_address_registered_at")),
				resultSet.getBoolean("greenlife_participating"),
				toLocalDateTime(resultSet.getTimestamp("greenlife_linked_at")),
				resultSet.getString("pocket_account_no")
			))
			.optional();
	}

	private static LocalDateTime toLocalDateTime(Timestamp value) {
		return value == null ? null : value.toLocalDateTime();
	}

	private static LocalDate toLocalDate(Date value) {
		return value == null ? null : value.toLocalDate();
	}

	public record UserMypageSnapshot(
		String name,
		String sidoCode,
		String sidoName,
		String sigunguCode,
		String sigunguName,
		String housingType,
		String areaBand,
		EcoLinkStatus ecoLinkStatus,
		LocalDateTime ecoLinkedAt,
		String ecoSidoCode,
		String ecoSigunguCode,
		String ecoAddressLabel,
		LocalDate ecoAddressRegisteredAt,
		boolean greenlifeParticipating,
		LocalDateTime greenlifeLinkedAt,
		String pocketAccountNo
	) {
	}
}
