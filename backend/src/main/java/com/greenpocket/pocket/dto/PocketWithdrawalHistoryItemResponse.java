package com.greenpocket.pocket.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.greenpocket.pocket.entity.TransactionStatus;

public record PocketWithdrawalHistoryItemResponse(
	Long transactionId,
	String transactionCode,
	Long amount,
	TransactionStatus transactionStatus,
	OffsetDateTime requestedAt,
	LocalDate expectedDate,
	OffsetDateTime completedAt,
	WithdrawalAccountSnapshotResponse accountSnapshot,
	String failureReason,
	boolean retryable
) {
}
