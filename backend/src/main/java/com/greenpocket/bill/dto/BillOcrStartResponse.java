package com.greenpocket.bill.dto;

public record BillOcrStartResponse(
	String jobId,
	BillOcrJobStatus status,
	int progress,
	int pollAfterMs
) {
}
