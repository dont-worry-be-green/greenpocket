package com.greenpocket.greenlife.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.greenlife.entity.RewardStatus;

public record GreenlifeItemDetailResponse(
	Long itemId,
	String itemCode,
	String name,
	Long unitPrice,
	String rewardUnit,
	int standardYear,
	List<String> practiceSteps,
	String month,
	BigDecimal validCount,
	Long pendingAmount,
	Long monthlyCapAmount,
	boolean capReached,
	List<History> history,
	String externalUrl,
	OffsetDateTime syncedAt,
	String delayNotice
) {

	public record History(
		Long activityId,
		OffsetDateTime occurredAt,
		BigDecimal quantity,
		Long rewardAmount,
		RewardStatus rewardStatus,
		OffsetDateTime paidAt
	) {
	}
}
