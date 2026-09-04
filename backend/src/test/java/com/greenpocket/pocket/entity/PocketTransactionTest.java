package com.greenpocket.pocket.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PocketTransactionTest {

	@Test
	void createsCompletedWithdrawalLedgerEntry() {
		WithdrawalAccount account = mock(WithdrawalAccount.class);
		when(account.getId()).thenReturn(3L);
		WithdrawalAccountSnapshot snapshot = new WithdrawalAccountSnapshot(
			"신한은행",
			"110-123-456789",
			"김수현"
		);
		LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 4, 10, 0);

		PocketTransaction transaction = PocketTransaction.completedWithdrawal(
			42L,
			account,
			"GP-2609-0025",
			"550e8400-e29b-41d4-a716-446655440000",
			30_000L,
			snapshot,
			requestedAt,
			LocalDate.of(2026, 9, 8)
		);

		assertThat(transaction.getDirection()).isEqualTo(TransactionDirection.DEBIT);
		assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
		assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(transaction.getSourceType()).isEqualTo(TransactionSourceType.WITHDRAWAL);
		assertThat(transaction.getWithdrawalAccountId()).isEqualTo(3L);
		assertThat(transaction.getAmount()).isEqualTo(30_000L);
		assertThat(transaction.getCompletedAt()).isEqualTo(requestedAt);
		assertThat(transaction.getAccountSnapshot()).isEqualTo(snapshot);
	}

	@Test
	void createsRequestsRetriesAndCompletesEcoMileageConversion() {
		LocalDateTime firstRequestedAt = LocalDateTime.of(2026, 9, 3, 18, 58);
		PocketTransaction transaction = PocketTransaction.requestedEcoMileage(
			42L,
			7L,
			"GP-2609-0021",
			30_000L,
			"에코마일리지 2026 상반기",
			firstRequestedAt
		);

		assertThat(transaction.getDirection()).isEqualTo(TransactionDirection.CREDIT);
		assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.ECO_MILEAGE);
		assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.REQUESTED);
		assertThat(transaction.getSourceType()).isEqualTo(TransactionSourceType.ECO_ROUND);
		assertThat(transaction.getSourceKey()).isEqualTo("7");

		ReflectionTestUtils.setField(transaction, "transactionStatus", TransactionStatus.FAILED);
		ReflectionTestUtils.setField(transaction, "failureReason", "전환 실패");
		LocalDateTime retriedAt = firstRequestedAt.plusDays(1);
		transaction.retryEcoMileage(30_000L, retriedAt);

		assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.REQUESTED);
		assertThat(transaction.getRequestedAt()).isEqualTo(retriedAt);
		assertThat(transaction.getFailureReason()).isNull();

		LocalDateTime completedAt = retriedAt.plusMinutes(3);
		transaction.completeEcoMileage("550e8400-e29b-41d4-a716-446655440000", completedAt);

		assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(transaction.getIdempotencyKey()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
		assertThat(transaction.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void createsCompletedGreenlifeMonthlyCredit() {
		LocalDateTime completedAt = LocalDateTime.of(2026, 8, 10, 0, 0);

		PocketTransaction transaction = PocketTransaction.completedGreenlifeCredit(
			42L,
			"GP-2608-0001",
			"2026-07",
			3_140L,
			"녹색생활실천 7월분",
			completedAt
		);

		assertThat(transaction.getUserId()).isEqualTo(42L);
		assertThat(transaction.getDirection()).isEqualTo(TransactionDirection.CREDIT);
		assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.GREENLIFE);
		assertThat(transaction.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(transaction.getSourceType()).isEqualTo(TransactionSourceType.GREENLIFE_MONTH);
		assertThat(transaction.getSourceKey()).isEqualTo("2026-07");
		assertThat(transaction.getAmount()).isEqualTo(3_140L);
		assertThat(transaction.getRequestedAt()).isEqualTo(completedAt);
		assertThat(transaction.getCompletedAt()).isEqualTo(completedAt);
	}
}
