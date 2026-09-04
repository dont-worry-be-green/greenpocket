package com.greenpocket.bill.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record BillListResponse(
	List<Item> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext,
	Map<String, Long> counts
) {

	public record Item(
		Long recordId,
		String billingMonth,
		UtilityType utilityType,
		BillType billType,
		Long amount,
		BigDecimal usage,
		UsageUnit usageUnit,
		InputSource inputSource,
		RecordStatus recordStatus,
		OffsetDateTime registeredAt
	) {
	}
}
