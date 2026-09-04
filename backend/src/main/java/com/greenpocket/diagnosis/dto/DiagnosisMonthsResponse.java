package com.greenpocket.diagnosis.dto;

import java.util.List;

import com.greenpocket.global.type.UtilityType;

public record DiagnosisMonthsResponse(
	List<MonthItem> months,
	String defaultMonth
) {

	public record MonthItem(
		String yearMonth,
		boolean registered,
		List<UtilityType> utilities,
		long totalAmount
	) {
	}
}
