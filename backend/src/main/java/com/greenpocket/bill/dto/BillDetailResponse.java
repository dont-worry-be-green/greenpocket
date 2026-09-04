package com.greenpocket.bill.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

public record BillDetailResponse(
	Long recordId,
	String billingMonth,
	UtilityType utilityType,
	BillType billType,
	Long amount,
	BigDecimal usage,
	UsageUnit usageUnit,
	InputSource inputSource,
	BigDecimal confidence,
	RecordStatus recordStatus,
	OffsetDateTime registeredAt,
	OffsetDateTime updatedAt,
	List<Sibling> siblings
) {

	public record Sibling(
		Long recordId,
		UtilityType utilityType,
		Long amount
	) {
	}
}
