package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;

import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;

public record PocketGreenlifeCreditResult(
	boolean created,
	Long transactionId,
	String transactionCode,
	TransactionDirection direction,
	TransactionType transactionType,
	Long amount,
	TransactionStatus transactionStatus,
	String label,
	OffsetDateTime completedAt
) {
}
