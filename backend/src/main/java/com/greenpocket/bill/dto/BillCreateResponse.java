package com.greenpocket.bill.dto;

import java.math.BigDecimal;
import java.util.List;

import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.global.type.UtilityType;

public record BillCreateResponse(
	String billingMonth,
	List<Record> records,
	long totalAmount,
	Recalculated recalculated,
	String nextScreen
) {

	public record Record(
		Long recordId,
		UtilityType utilityType,
		Long amount,
		BigDecimal usage,
		RecordStatus recordStatus
	) {
	}

	public record Recalculated(
		String diagnosisMonth,
		boolean monthlyReportUpdated,
		Long roundId
	) {
	}
}
