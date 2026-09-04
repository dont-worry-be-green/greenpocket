package com.greenpocket.pocket.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "pocket_transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PocketTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "eco_round_id")
	private Long ecoRoundId;

	@Column(name = "withdrawal_account_id")
	private Long withdrawalAccountId;

	@Column(name = "transaction_code", nullable = false, length = 30)
	private String transactionCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "direction", nullable = false, length = 10)
	private TransactionDirection direction;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 20)
	private TransactionType transactionType;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_status", nullable = false, length = 20)
	private TransactionStatus transactionStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 20)
	private TransactionSourceType sourceType;

	@Column(name = "source_key", nullable = false, length = 100)
	private String sourceKey;

	@Column(name = "idempotency_key", length = 100)
	private String idempotencyKey;

	@Column(name = "label", nullable = false, length = 60)
	private String label;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "account_snapshot", columnDefinition = "json")
	private WithdrawalAccountSnapshot accountSnapshot;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "expected_date")
	private LocalDate expectedDate;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "failure_reason", length = 300)
	private String failureReason;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	public static PocketTransaction completedWithdrawal(
		Long userId,
		WithdrawalAccount account,
		String transactionCode,
		String idempotencyKey,
		Long amount,
		WithdrawalAccountSnapshot accountSnapshot,
		LocalDateTime requestedAt,
		LocalDate expectedDate
	) {
		PocketTransaction transaction = new PocketTransaction();
		transaction.userId = userId;
		transaction.withdrawalAccountId = account.getId();
		transaction.transactionCode = transactionCode;
		transaction.direction = TransactionDirection.DEBIT;
		transaction.transactionType = TransactionType.WITHDRAWAL;
		transaction.amount = amount;
		transaction.transactionStatus = TransactionStatus.COMPLETED;
		transaction.sourceType = TransactionSourceType.WITHDRAWAL;
		transaction.sourceKey = idempotencyKey;
		transaction.idempotencyKey = idempotencyKey;
		transaction.label = "그린포켓 출금";
		transaction.accountSnapshot = accountSnapshot;
		transaction.requestedAt = requestedAt;
		transaction.expectedDate = expectedDate;
		transaction.completedAt = requestedAt;
		return transaction;
	}
}
