package com.greenpocket.greenlife.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record GreenlifeStatusResponse(
	boolean participating,
	String screen,
	OffsetDateTime linkedAt,
	ProgramInfo programInfo,
	List<FeaturedItem> featuredItems,
	String month,
	MonthSummary monthSummary,
	Annual annual,
	String delayNotice,
	Integer standardYear
) {

	public record ProgramInfo(
		String name,
		int itemCount,
		long annualLimit,
		int standardYear,
		List<String> joinSteps,
		String externalUrl
	) {
	}

	public record FeaturedItem(
		Long itemId,
		String name,
		long unitPrice,
		String rewardUnit,
		String iconKey
	) {
	}

	public record MonthSummary(
		int activityCount,
		long pendingAmount,
		long paidAmount,
		String paidMonth
	) {
	}

	public record Annual(
		int year,
		long paidAmount,
		long limitAmount,
		BigDecimal progressPercent,
		boolean limitReached
	) {
	}
}
