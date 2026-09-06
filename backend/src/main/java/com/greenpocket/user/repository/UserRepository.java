package com.greenpocket.user.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.eco.entity.EcoLinkStatus;

@Repository
@RequiredArgsConstructor
public class UserRepository {

	private final JdbcClient jdbcClient;

	public Optional<UserSnapshot> findByDemoKey(String demoKey) {
		return find("demo_key = :value", demoKey);
	}

	public Optional<UserSnapshot> findById(Long userId) {
		return find("id = :value", userId);
	}

	public boolean existsByPocketAccountNo(String pocketAccountNo) {
		return jdbcClient.sql("SELECT COUNT(*) FROM app_user WHERE pocket_account_no = :accountNo")
			.param("accountNo", pocketAccountNo)
			.query(Integer.class)
			.single() > 0;
	}

	public void create(String demoKey, String name, String pocketAccountNo) {
		jdbcClient.sql("""
				INSERT INTO app_user (demo_key, name, pocket_account_no, pocket_holder)
				VALUES (:demoKey, :name, :pocketAccountNo, :pocketHolder)
				""")
			.param("demoKey", demoKey)
			.param("name", name)
			.param("pocketAccountNo", pocketAccountNo)
			.param("pocketHolder", name)
			.update();
	}

	public int deleteByDemoKey(String demoKey) {
		return jdbcClient.sql("DELETE FROM app_user WHERE demo_key = :demoKey")
			.param("demoKey", demoKey)
			.update();
	}

	private Optional<UserSnapshot> find(String condition, Object value) {
		return jdbcClient.sql("""
				SELECT id, demo_key, name, onboarding_completed,
				       eco_link_status, eco_linked_at,
				       greenlife_participating, greenlife_linked_at,
				       pocket_account_no, pocket_holder, created_at
				FROM app_user
				WHERE %s
				""".formatted(condition))
			.param("value", value)
			.query((resultSet, rowNum) -> new UserSnapshot(
				resultSet.getLong("id"),
				resultSet.getString("demo_key"),
				resultSet.getString("name"),
				resultSet.getBoolean("onboarding_completed"),
				EcoLinkStatus.valueOf(resultSet.getString("eco_link_status")),
				toLocalDateTime(resultSet.getTimestamp("eco_linked_at")),
				resultSet.getBoolean("greenlife_participating"),
				toLocalDateTime(resultSet.getTimestamp("greenlife_linked_at")),
				resultSet.getString("pocket_account_no"),
				resultSet.getString("pocket_holder"),
				resultSet.getTimestamp("created_at").toLocalDateTime()
			))
			.optional();
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	public record UserSnapshot(
		Long id,
		String demoKey,
		String name,
		boolean onboardingCompleted,
		EcoLinkStatus ecoLinkStatus,
		LocalDateTime ecoLinkedAt,
		boolean greenlifeParticipating,
		LocalDateTime greenlifeLinkedAt,
		String pocketAccountNo,
		String pocketHolder,
		LocalDateTime createdAt
	) {
	}
}
