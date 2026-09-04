package com.greenpocket.greenlife.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.greenpocket.greenlife.entity.RewardStatus;

final class GreenlifeMockData {

	static final LocalDateTime LINKED_AT = LocalDateTime.of(2026, 9, 2, 18, 30);
	static final LocalDate SUMMARY_MONTH = LocalDate.of(2026, 8, 1);

	private GreenlifeMockData() {
	}

	static List<MockActivity> activities() {
		List<MockActivity> activities = new ArrayList<>();

		addActivities(activities, "2026-08-receipt", "E_RECEIPT", 24, 10,
			LocalDateTime.of(2026, 8, 1, 12, 0), RewardStatus.PENDING, null);
		addActivities(activities, "2026-08-tumbler", "TUMBLER", 8, 300,
			LocalDateTime.of(2026, 8, 1, 8, 30), RewardStatus.PENDING, null);
		addActivities(activities, "2026-08-container", "REUSABLE_CONTAINER", 3, 500,
			LocalDateTime.of(2026, 8, 10, 19, 0), RewardStatus.PENDING, null);
		addActivities(activities, "2026-08-refill", "REFILL_STATION", 1, 500,
			LocalDateTime.of(2026, 8, 15, 14, 0), RewardStatus.PENDING, null);
		addActivities(activities, "2026-08-bag", "SHOPPING_BAG", 6, 50,
			LocalDateTime.of(2026, 8, 18, 17, 0), RewardStatus.PENDING, null);
		addActivities(activities, "2026-08-recycle", "HIGH_QUALITY_RECYCLING", 2, 300,
			LocalDateTime.of(2026, 8, 26, 10, 0), RewardStatus.PENDING, null);

		LocalDateTime julyPaidAt = LocalDateTime.of(2026, 8, 10, 0, 0);
		addActivities(activities, "2026-07-receipt", "E_RECEIPT", 24, 10,
			LocalDateTime.of(2026, 7, 1, 12, 0), RewardStatus.PAID, julyPaidAt);
		addActivities(activities, "2026-07-tumbler", "TUMBLER", 8, 300,
			LocalDateTime.of(2026, 7, 1, 8, 30), RewardStatus.PAID, julyPaidAt);
		addActivities(activities, "2026-07-container", "REUSABLE_CONTAINER", 1, 500,
			LocalDateTime.of(2026, 7, 20, 19, 0), RewardStatus.PAID, julyPaidAt);

		LocalDateTime previousPaidAt = LocalDateTime.of(2026, 7, 10, 0, 0);
		addActivities(activities, "2026-04-tree", "TREE_PLANTING", 5, 3_000,
			LocalDateTime.of(2026, 4, 5, 10, 0), RewardStatus.PAID, previousPaidAt);
		addActivities(activities, "2026-05-recycle", "HIGH_QUALITY_RECYCLING", 1, 300,
			LocalDateTime.of(2026, 5, 12, 10, 0), RewardStatus.PAID, previousPaidAt);
		addActivities(activities, "2026-06-bag", "SHOPPING_BAG", 3, 50,
			LocalDateTime.of(2026, 6, 10, 17, 0), RewardStatus.PAID, previousPaidAt);
		addActivities(activities, "2026-06-receipt", "E_RECEIPT", 1, 10,
			LocalDateTime.of(2026, 6, 15, 12, 0), RewardStatus.PAID, previousPaidAt);

		return List.copyOf(activities);
	}

	private static void addActivities(
		List<MockActivity> activities,
		String keyPrefix,
		String itemCode,
		int count,
		long unitPrice,
		LocalDateTime firstOccurredAt,
		RewardStatus rewardStatus,
		LocalDateTime paidAt
	) {
		for (int index = 0; index < count; index++) {
			LocalDateTime occurredAt = firstOccurredAt.plusDays(index);
			activities.add(new MockActivity(
				keyPrefix + "-" + (index + 1),
				itemCode,
				occurredAt.toLocalDate().withDayOfMonth(1),
				occurredAt,
				BigDecimal.ONE.setScale(3),
				unitPrice,
				rewardStatus,
				occurredAt,
				paidAt
			));
		}
	}

	record MockActivity(
		String sourceKeySuffix,
		String itemCode,
		LocalDate activityMonth,
		LocalDateTime occurredAt,
		BigDecimal quantity,
		long rewardAmount,
		RewardStatus rewardStatus,
		LocalDateTime pendingAt,
		LocalDateTime paidAt
	) {
	}
}
