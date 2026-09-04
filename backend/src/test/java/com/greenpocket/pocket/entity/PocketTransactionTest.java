package com.greenpocket.pocket.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

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
}
