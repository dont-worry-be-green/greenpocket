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

	/**
	 * 오늘의 실천 — 목표에서 고른 미션 **전부**다(결정 C-35). 계절로 거르지 않는다.
	 * 계절 태그는 화면이 「여름 전용」 칩으로 알리는 용도로만 함께 내려준다.
	 */
	public List<TodayMissionSnapshot> findTodayMissions(Long userId, Long roundId, LocalDate date) {
		return jdbcClient.sql("""
				SELECT selected.mission_id, mission.title, mission.utility_type, mission.difficulty,
				       mission.season_tags,
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
				ORDER BY FIELD(mission.utility_type, 'ELECTRICITY', 'GAS', 'WATER'),
				         mission.display_order, mission.id
				""")
			.param("date", date)
			.param("userId", userId)
			.param("roundId", roundId)
			.query((resultSet, rowNum) -> new TodayMissionSnapshot(
				resultSet.getLong("mission_id"),
				resultSet.getString("title"),
				UtilityType.valueOf(resultSet.getString("utility_type")),
				MissionDifficulty.valueOf(resultSet.getString("difficulty")),
				seasonTags(resultSet.getString("season_tags")),
				resultSet.getBoolean("completed")
			))
			.list();
	}

	/** MySQL SET('SPRING,SUMMER') → 목록. 비어 있으면 빈 목록 */
	private static List<String> seasonTags(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return List.of(value.split(","));
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
		List<String> seasonTags,
		boolean completed
	) {
	}
}
