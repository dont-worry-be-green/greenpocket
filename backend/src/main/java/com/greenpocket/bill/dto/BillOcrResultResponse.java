package com.greenpocket.bill.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.global.type.UtilityType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BillOcrResultResponse(
	String jobId,
	BillOcrJobStatus status,
	Integer progress,
	BillType billType,
	String billingMonth,
	Boolean partialRecognition,
	List<Item> items,
	String errorCode,
	String message,
	String fallbackScreen
) {

	public record Item(
		UtilityType utilityType,
		boolean hasData,
		String billingMonth,
		Long amount,
		BigDecimal usage,
		UsageUnit usageUnit,
		BigDecimal confidence,
		RecordStatus recordStatus
	) {
	}
}
