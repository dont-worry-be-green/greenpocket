package com.greenpocket.bill.dto;

import java.util.List;

import com.greenpocket.global.type.UtilityType;

public record BillTargetMonthResponse(
	String targetYearMonth,
	String lastRegisteredMonth,
	boolean alreadyRegistered,
	List<UtilityType> registeredUtilitiesInTarget,
	String nextScreen
) {
}
