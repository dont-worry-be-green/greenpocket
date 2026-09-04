package com.greenpocket.pocket.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;

public record PocketWithdrawalResponse(
	Long transactionId,
	String transactionCode,
	TransactionDirection direction,
	TransactionType transactionType,
	Long amount,
	TransactionStatus transactionStatus,
	OffsetDateTime requestedAt,
	LocalDate expectedDate,
	Long balanceAfter,
	WithdrawalAccountSnapshotResponse accountSnapshot,
	String notice
) {
}
