package com.greenpocket.greenlife.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.greenpocket.greenlife.entity.RewardStatus;

@Repository
@RequiredArgsConstructor
public class GreenlifeRepository {

	private final JdbcClient jdbcClient;

	public Optional<UserSnapshot> findUser(Long userId) {
		return jdbcClient.sql("""
				SELECT greenlife_participating, greenlife_linked_at
				FROM app_user
				WHERE id = :userId
				""")
			.param("userId", userId)
			.query((resultSet, rowNum) -> new UserSnapshot(
				resultSet.getBoolean("greenlife_participating"),
				toLocalDateTime(resultSet.getTimestamp("greenlife_linked_at"))
			))
			.optional();
	}

	public int countActiveItems(int standardYear) {
		return jdbcClient.sql("""
				SELECT COUNT(*)
				FROM greenlife_item
				WHERE standard_year = :standardYear AND is_active = 1
				""")
			.param("standardYear", standardYear)
			.query(Integer.class)
			.single();
	}

	public List<FeaturedItemSnapshot> findFeaturedItems(int standardYear) {
		return jdbcClient.sql("""
				SELECT id, name, unit_price, reward_unit, icon_key
				FROM greenlife_item
				WHERE standard_year = :standardYear
				  AND is_active = 1
				  AND item_code IN ('E_RECEIPT', 'TUMBLER', 'REUSABLE_CONTAINER', 'REFILL_STATION')
				ORDER BY FIELD(item_code, 'E_RECEIPT', 'TUMBLER', 'REUSABLE_CONTAINER', 'REFILL_STATION')
				""")
			.param("standardYear", standardYear)
			.query((resultSet, rowNum) -> new FeaturedItemSnapshot(
				resultSet.getLong("id"),
				resultSet.getString("name"),
				resultSet.getLong("unit_price"),
				resultSet.getString("reward_unit"),
				resultSet.getString("icon_key")
			))
			.list();
	}

	public MonthlyActivitySnapshot findMonthlyActivity(Long userId, LocalDate month) {
		return jdbcClient.sql("""
				SELECT COUNT(*) AS activity_count,
				       COALESCE(SUM(CASE WHEN reward_status = 'PENDING' THEN reward_amount ELSE 0 END), 0)
				           AS pending_amount
				FROM greenlife_activity
				WHERE user_id = :userId AND activity_month = :month
				""")
			.param("userId", userId)
			.param("month", month)
			.query((resultSet, rowNum) -> new MonthlyActivitySnapshot(
				resultSet.getInt("activity_count"),
				resultSet.getLong("pending_amount")
			))
			.single();
	}

	public Optional<PaidMonthSnapshot> findLatestPaidMonth(
		Long userId,
		LocalDate throughMonth,
		int standardYear
	) {
		return jdbcClient.sql("""
				SELECT activity_month, SUM(reward_amount) AS paid_amount
				FROM greenlife_activity
				WHERE user_id = :userId
				  AND reward_status = 'PAID'
				  AND YEAR(activity_month) = :standardYear
				  AND activity_month <= :throughMonth
				GROUP BY activity_month
				ORDER BY activity_month DESC
				LIMIT 1
				""")
			.param("userId", userId)
			.param("standardYear", standardYear)
			.param("throughMonth", throughMonth)
			.query((resultSet, rowNum) -> new PaidMonthSnapshot(
				resultSet.getDate("activity_month").toLocalDate(),
				resultSet.getLong("paid_amount")
			))
			.optional();
	}

	public long findAnnualPaidAmount(Long userId, int standardYear) {
		return jdbcClient.sql("""
				SELECT COALESCE(SUM(reward_amount), 0)
				FROM greenlife_activity
				WHERE user_id = :userId
				  AND reward_status = 'PAID'
				  AND YEAR(activity_month) = :standardYear
				""")
			.param("userId", userId)
			.param("standardYear", standardYear)
			.query(Long.class)
			.single();
	}

	public List<ItemSnapshot> findItems(Long userId, LocalDate month, int standardYear) {
		return jdbcClient.sql("""
				SELECT item.id, item.item_code, item.name, item.unit_price, item.reward_unit,
				       item.icon_key, item.display_order, item.monthly_cap_amount, item.annual_cap_amount,
				       COALESCE(SUM(CASE WHEN activity.activity_month = :month THEN activity.quantity ELSE 0 END), 0)
				           AS month_count,
				       COALESCE(SUM(CASE WHEN activity.activity_month = :month THEN activity.reward_amount ELSE 0 END), 0)
				           AS month_amount,
				       COALESCE(SUM(CASE WHEN activity.reward_status = 'PAID'
				                              AND YEAR(activity.activity_month) = :standardYear
				                         THEN activity.reward_amount ELSE 0 END), 0)
				           AS annual_paid_amount
				FROM greenlife_item item
				LEFT JOIN greenlife_activity activity
				  ON activity.item_id = item.id AND activity.user_id = :userId
				WHERE item.standard_year = :standardYear AND item.is_active = 1
				GROUP BY item.id, item.item_code, item.name, item.unit_price, item.reward_unit,
				         item.icon_key, item.display_order, item.monthly_cap_amount, item.annual_cap_amount
				ORDER BY item.display_order
				""")
			.param("userId", userId)
			.param("month", month)
			.param("standardYear", standardYear)
			.query((resultSet, rowNum) -> new ItemSnapshot(
				resultSet.getLong("id"),
				resultSet.getString("item_code"),
				resultSet.getString("name"),
				resultSet.getLong("unit_price"),
				resultSet.getString("reward_unit"),
				resultSet.getString("icon_key"),
				resultSet.getInt("display_order"),
				resultSet.getBigDecimal("month_count"),
				resultSet.getLong("month_amount"),
				resultSet.getObject("monthly_cap_amount", Long.class),
				resultSet.getObject("annual_cap_amount", Long.class),
				resultSet.getLong("annual_paid_amount")
			))
			.list();
	}

	public Optional<ItemDetailSnapshot> findItem(Long itemId, int standardYear) {
		return jdbcClient.sql("""
				SELECT id, item_code, name, unit_price, reward_unit, standard_year,
				       JSON_UNQUOTE(JSON_EXTRACT(practice_steps, '$[0]')) AS practice_step_1,
				       JSON_UNQUOTE(JSON_EXTRACT(practice_steps, '$[1]')) AS practice_step_2,
				       JSON_UNQUOTE(JSON_EXTRACT(practice_steps, '$[2]')) AS practice_step_3,
				       monthly_cap_amount, annual_cap_amount, external_url
				FROM greenlife_item
				WHERE id = :itemId AND standard_year = :standardYear AND is_active = 1
				""")
			.param("itemId", itemId)
			.param("standardYear", standardYear)
			.query((resultSet, rowNum) -> new ItemDetailSnapshot(
				resultSet.getLong("id"),
				resultSet.getString("item_code"),
				resultSet.getString("name"),
				resultSet.getLong("unit_price"),
				resultSet.getString("reward_unit"),
				resultSet.getInt("standard_year"),
				List.of(
					resultSet.getString("practice_step_1"),
					resultSet.getString("practice_step_2"),
					resultSet.getString("practice_step_3")
				),
				resultSet.getObject("monthly_cap_amount", Long.class),
				resultSet.getObject("annual_cap_amount", Long.class),
				resultSet.getString("external_url")
			))
			.optional();
	}

	public ItemActivitySummarySnapshot findItemActivitySummary(Long userId, Long itemId, LocalDate month) {
		return jdbcClient.sql("""
				SELECT COALESCE(SUM(CASE WHEN activity_month = :month THEN quantity ELSE 0 END), 0)
				           AS valid_count,
				       COALESCE(SUM(CASE WHEN activity_month = :month AND reward_status = 'PENDING'
				                         THEN reward_amount ELSE 0 END), 0) AS pending_amount,
				       MAX(synced_at) AS synced_at
				FROM greenlife_activity
				WHERE user_id = :userId AND item_id = :itemId
				""")
			.param("userId", userId)
			.param("itemId", itemId)
			.param("month", month)
			.query((resultSet, rowNum) -> new ItemActivitySummarySnapshot(
				resultSet.getBigDecimal("valid_count"),
				resultSet.getLong("pending_amount"),
				toLocalDateTime(resultSet.getTimestamp("synced_at"))
			))
			.single();
	}

	public List<ActivityHistorySnapshot> findRecentItemHistory(Long userId, Long itemId) {
		return jdbcClient.sql("""
				SELECT id, occurred_at, quantity, reward_amount, reward_status, paid_at
				FROM greenlife_activity
				WHERE user_id = :userId AND item_id = :itemId
				ORDER BY occurred_at DESC, id DESC
				LIMIT 10
				""")
			.param("userId", userId)
			.param("itemId", itemId)
			.query((resultSet, rowNum) -> new ActivityHistorySnapshot(
				resultSet.getLong("id"),
				toLocalDateTime(resultSet.getTimestamp("occurred_at")),
				resultSet.getBigDecimal("quantity"),
				resultSet.getLong("reward_amount"),
				RewardStatus.valueOf(resultSet.getString("reward_status")),
				toLocalDateTime(resultSet.getTimestamp("paid_at"))
			))
			.list();
	}

	public void markParticipating(Long userId, LocalDateTime linkedAt) {
		jdbcClient.sql("""
				UPDATE app_user
				SET greenlife_participating = 1,
				    greenlife_linked_at = :linkedAt,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = :userId
				""")
			.param("userId", userId)
			.param("linkedAt", linkedAt)
			.update();
	}

	public void upsertMockActivity(
		Long userId,
		String itemCode,
		String sourceEventKey,
		LocalDate activityMonth,
		LocalDateTime occurredAt,
		BigDecimal quantity,
		long rewardAmount,
		RewardStatus rewardStatus,
		LocalDateTime pendingAt,
		LocalDateTime paidAt,
		LocalDateTime syncedAt
	) {
		jdbcClient.sql("""
				INSERT INTO greenlife_activity (
				    user_id, item_id, source_event_key, activity_month, occurred_at,
				    quantity, reward_amount, reward_status, pending_at, paid_at,
				    item_name_snapshot, unit_price_snapshot, synced_at
				)
				SELECT :userId, item.id, :sourceEventKey, :activityMonth, :occurredAt,
				       :quantity, :rewardAmount, :rewardStatus, :pendingAt, :paidAt,
				       item.name, item.unit_price, :syncedAt
				FROM greenlife_item item
				WHERE item.item_code = :itemCode AND item.is_active = 1
				ON DUPLICATE KEY UPDATE
				    activity_month = VALUES(activity_month),
				    occurred_at = VALUES(occurred_at),
				    quantity = VALUES(quantity),
				    reward_amount = VALUES(reward_amount),
				    reward_status = IF(greenlife_activity.reward_status = 'PAID'
				                           OR VALUES(reward_status) = 'PAID', 'PAID', 'PENDING'),
				    pending_at = VALUES(pending_at),
				    paid_at = COALESCE(greenlife_activity.paid_at, VALUES(paid_at)),
				    item_name_snapshot = VALUES(item_name_snapshot),
				    unit_price_snapshot = VALUES(unit_price_snapshot),
				    synced_at = VALUES(synced_at),
				    updated_at = CURRENT_TIMESTAMP
				""")
			.param("userId", userId)
			.param("itemCode", itemCode)
			.param("sourceEventKey", sourceEventKey)
			.param("activityMonth", activityMonth)
			.param("occurredAt", occurredAt)
			.param("quantity", quantity)
			.param("rewardAmount", rewardAmount)
			.param("rewardStatus", rewardStatus.name())
			.param("pendingAt", pendingAt)
			.param("paidAt", paidAt)
			.param("syncedAt", syncedAt)
			.update();
	}

	private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	public record UserSnapshot(boolean participating, LocalDateTime linkedAt) {
	}

	public record FeaturedItemSnapshot(
		Long id,
		String name,
		long unitPrice,
		String rewardUnit,
		String iconKey
	) {
	}

	public record MonthlyActivitySnapshot(int activityCount, long pendingAmount) {
	}

	public record PaidMonthSnapshot(LocalDate month, long paidAmount) {
	}

	public record ItemSnapshot(
		Long id,
		String itemCode,
		String name,
		long unitPrice,
		String rewardUnit,
		String iconKey,
		int displayOrder,
		BigDecimal monthCount,
		long monthAmount,
		Long monthlyCapAmount,
		Long annualCapAmount,
		long annualPaidAmount
	) {
	}

	public record ItemDetailSnapshot(
		Long id,
		String itemCode,
		String name,
		long unitPrice,
		String rewardUnit,
		int standardYear,
		List<String> practiceSteps,
		Long monthlyCapAmount,
		Long annualCapAmount,
		String externalUrl
	) {
	}

	public record ItemActivitySummarySnapshot(
		BigDecimal validCount,
		long pendingAmount,
		LocalDateTime syncedAt
	) {
	}

	public record ActivityHistorySnapshot(
		Long activityId,
		LocalDateTime occurredAt,
		BigDecimal quantity,
		long rewardAmount,
		RewardStatus rewardStatus,
		LocalDateTime paidAt
	) {
	}
}
