package com.greenpocket.bill.dto;

public record BillRecalculatedResponse(
	String diagnosisMonth,
	boolean monthlyReportUpdated
) {
}
