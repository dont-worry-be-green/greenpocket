package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;

import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;

public record PocketTransactionItemResponse(
	Long transactionId,
	String transactionCode,
	String label,
	TransactionDirection direction,
	TransactionType transactionType,
	Long amount,
	TransactionStatus transactionStatus,
	OffsetDateTime completedAt,
	String sourceLabel
) {
}
