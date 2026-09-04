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

	public static PocketTransaction requestedEcoMileage(
		Long userId,
		Long ecoRoundId,
		String transactionCode,
		Long amount,
		String label,
		LocalDateTime requestedAt
	) {
		PocketTransaction transaction = new PocketTransaction();
		transaction.userId = userId;
		transaction.ecoRoundId = ecoRoundId;
		transaction.transactionCode = transactionCode;
		transaction.direction = TransactionDirection.CREDIT;
		transaction.transactionType = TransactionType.ECO_MILEAGE;
		transaction.amount = amount;
		transaction.transactionStatus = TransactionStatus.REQUESTED;
		transaction.sourceType = TransactionSourceType.ECO_ROUND;
		transaction.sourceKey = ecoRoundId.toString();
		transaction.label = label;
		transaction.requestedAt = requestedAt;
		return transaction;
	}

	public static PocketTransaction completedGreenlifeCredit(
		Long userId,
		String transactionCode,
		String sourceKey,
		Long amount,
		String label,
		LocalDateTime completedAt
	) {
		PocketTransaction transaction = new PocketTransaction();
		transaction.userId = userId;
		transaction.transactionCode = transactionCode;
		transaction.direction = TransactionDirection.CREDIT;
		transaction.transactionType = TransactionType.GREENLIFE;
		transaction.amount = amount;
		transaction.transactionStatus = TransactionStatus.COMPLETED;
		transaction.sourceType = TransactionSourceType.GREENLIFE_MONTH;
		transaction.sourceKey = sourceKey;
		transaction.label = label;
		transaction.requestedAt = completedAt;
		transaction.completedAt = completedAt;
		return transaction;
	}

	public void retryEcoMileage(Long amount, LocalDateTime requestedAt) {
		if (transactionType != TransactionType.ECO_MILEAGE || transactionStatus != TransactionStatus.FAILED) {
			throw new IllegalStateException("실패한 에코마일리지 전환만 재시도할 수 있습니다.");
		}
		this.amount = amount;
		this.transactionStatus = TransactionStatus.REQUESTED;
		this.idempotencyKey = null;
		this.requestedAt = requestedAt;
		this.completedAt = null;
		this.failureReason = null;
	}

	public void completeEcoMileage(String idempotencyKey, LocalDateTime completedAt) {
		if (transactionType != TransactionType.ECO_MILEAGE || transactionStatus != TransactionStatus.REQUESTED) {
			throw new IllegalStateException("요청 상태의 에코마일리지 전환만 완료할 수 있습니다.");
		}
		this.transactionStatus = TransactionStatus.COMPLETED;
		this.idempotencyKey = idempotencyKey;
		this.completedAt = completedAt;
		this.failureReason = null;
	}
}
