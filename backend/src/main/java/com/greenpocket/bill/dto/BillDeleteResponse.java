package com.greenpocket.bill.dto;

public record BillDeleteResponse(
	Long deletedRecordId,
	BillRecalculatedResponse recalculated
) {
}
