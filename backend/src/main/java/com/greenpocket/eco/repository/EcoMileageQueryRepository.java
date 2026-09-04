package com.greenpocket.eco.repository;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EcoMileageQueryRepository {

	private final JdbcClient jdbcClient;

	public List<ConfirmedMileageRoundSnapshot> findConfirmedMileageRounds(Long userId) {
		return jdbcClient.sql("""
				SELECT id, period_start, period_end, confirmed_mileage
				FROM eco_round
				WHERE user_id = :userId
				  AND round_status = 'CONFIRMED'
				  AND confirmed_mileage > 0
				ORDER BY period_end DESC, id DESC
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new ConfirmedMileageRoundSnapshot(
				resultSet.getLong("id"),
				resultSet.getObject("period_start", LocalDate.class),
				resultSet.getObject("period_end", LocalDate.class),
				resultSet.getLong("confirmed_mileage")
			))
			.list();
	}

	public record ConfirmedMileageRoundSnapshot(
		Long roundId,
		LocalDate periodStart,
		LocalDate periodEnd,
		Long confirmedMileage
	) {
	}
}
