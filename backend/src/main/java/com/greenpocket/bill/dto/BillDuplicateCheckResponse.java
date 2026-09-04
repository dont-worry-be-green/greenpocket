package com.greenpocket.bill.dto;

import java.util.List;

import com.greenpocket.global.type.UtilityType;

public record BillDuplicateCheckResponse(
	String billingMonth,
	List<Result> results
) {

	public record Result(
		UtilityType utilityType,
		boolean duplicated,
		Long existingRecordId
	) {
	}
}
