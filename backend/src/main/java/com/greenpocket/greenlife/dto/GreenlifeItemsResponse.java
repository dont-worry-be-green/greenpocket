package com.greenpocket.greenlife.dto;

import java.math.BigDecimal;
import java.util.List;

public record GreenlifeItemsResponse(
	String month,
	int standardYear,
	List<Item> items,
	int totalCount,
	int collapsedAfter
) {

	public record Item(
		Long itemId,
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
		boolean capReached
	) {
	}
}
