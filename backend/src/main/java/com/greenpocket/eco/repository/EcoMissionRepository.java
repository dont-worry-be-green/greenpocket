package com.greenpocket.eco.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.global.type.UtilityType;

@Repository
@RequiredArgsConstructor
public class EcoMissionRepository {

	private final JdbcClient jdbcClient;

	public Optional<Long> findOwnedRoundId(Long userId, Long roundId) {
		return jdbcClient.sql("SELECT id FROM eco_round WHERE id = :roundId AND user_id = :userId")
			.param("roundId", roundId)
			.param("userId", userId)
			.query(Long.class)
			.optional();
	}

	public List<TodayMissionSnapshot> findTodayMissions(
		Long userId,
		Long roundId,
		LocalDate date,
		String season
	) {
		return jdbcClient.sql("""
				SELECT selected.mission_id, mission.title, mission.utility_type, mission.difficulty,
				       COALESCE(JSON_CONTAINS(daily.completed_mission_ids,
				                              CAST(selected.mission_id AS JSON), '$'), 0) AS completed
				FROM user_mission selected
				JOIN mission_catalog mission ON mission.id = selected.mission_id
				LEFT JOIN mission_daily_log daily
				  ON daily.user_id = selected.user_id
				 AND daily.eco_round_id = selected.eco_round_id
				 AND daily.log_date = :date
				WHERE selected.user_id = :userId
				  AND selected.eco_round_id = :roundId
				  AND mission.is_active = 1
				  AND FIND_IN_SET(:season, mission.season_tags) > 0
				ORDER BY FIELD(mission.utility_type, 'ELECTRICITY', 'GAS', 'WATER'),
				         mission.display_order, mission.id
				""")
			.param("date", date)
			.param("userId", userId)
			.param("roundId", roundId)
			.param("season", season)
			.query((resultSet, rowNum) -> new TodayMissionSnapshot(
				resultSet.getLong("mission_id"),
				resultSet.getString("title"),
				UtilityType.valueOf(resultSet.getString("utility_type")),
				MissionDifficulty.valueOf(resultSet.getString("difficulty")),
				resultSet.getBoolean("completed")
			))
			.list();
	}

	public void saveDailyLog(Long userId, Long roundId, LocalDate date, List<Long> completedMissionIds) {
		String completedMissionIdsJson = completedMissionIds.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(",", "[", "]"));
		jdbcClient.sql("""
				INSERT INTO mission_daily_log (
				    user_id, eco_round_id, log_date, completed_mission_ids
				) VALUES (
				    :userId, :roundId, :date, CAST(:completedMissionIds AS JSON)
				)
				ON DUPLICATE KEY UPDATE
				    completed_mission_ids = VALUES(completed_mission_ids),
				    updated_at = CURRENT_TIMESTAMP
				""")
			.param("userId", userId)
			.param("roundId", roundId)
			.param("date", date)
			.param("completedMissionIds", completedMissionIdsJson)
			.update();
	}

	public record TodayMissionSnapshot(
		Long missionId,
		String title,
		UtilityType utilityType,
		MissionDifficulty difficulty,
		boolean completed
	) {
	}
}
