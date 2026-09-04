package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.crypto.AccountNumberCipher;
import com.greenpocket.pocket.dto.PocketWithdrawalHistoryResponse;
import com.greenpocket.pocket.dto.PocketWithdrawalRequest;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.entity.WithdrawalAccount;
import com.greenpocket.pocket.entity.WithdrawalAccountSnapshot;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;
import com.greenpocket.pocket.repository.WithdrawalAccountRepository;
import com.greenpocket.pocket.service.PocketWithdrawalService.WithdrawalExecution;

class PocketWithdrawalServiceTest {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Long USER_ID = 42L;
	private static final Long ACCOUNT_ID = 3L;
	private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
	private static final byte[] ENCRYPTED_ACCOUNT_NUMBER = {1, 2, 3};

	private PocketTransactionRepository pocketTransactionRepository;
	private WithdrawalAccountRepository withdrawalAccountRepository;
	private AccountNumberCipher accountNumberCipher;
	private PocketWithdrawalService pocketWithdrawalService;

	@BeforeEach
	void setUp() {
		pocketTransactionRepository = mock(PocketTransactionRepository.class);
		withdrawalAccountRepository = mock(WithdrawalAccountRepository.class);
		accountNumberCipher = mock(AccountNumberCipher.class);
		Clock clock = Clock.fixed(Instant.parse("2026-09-04T01:00:00Z"), KOREA_ZONE_ID);
		pocketWithdrawalService = new PocketWithdrawalService(
			pocketTransactionRepository,
			withdrawalAccountRepository,
			accountNumberCipher,
			new PocketTransactionCodeGenerator(pocketTransactionRepository),
			clock
		);
	}

	@Test
	void createsCompletedWithdrawalAndCalculatesTwoBusinessDays() {
		WithdrawalAccount account = account();
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.of(account));
		when(accountNumberCipher.decrypt(ENCRYPTED_ACCOUNT_NUMBER)).thenReturn("110-123-456789");
		when(pocketTransactionRepository.sumAmount(USER_ID, TransactionStatus.COMPLETED, TransactionDirection.CREDIT))
			.thenReturn(64_000L);
		when(pocketTransactionRepository.sumAmount(USER_ID, TransactionStatus.COMPLETED, TransactionDirection.DEBIT))
			.thenReturn(0L);
		when(pocketTransactionRepository.existsByTransactionCode(any())).thenReturn(false);
		when(pocketTransactionRepository.save(any(PocketTransaction.class)))
			.thenAnswer(invocation -> invocation.<PocketTransaction>getArgument(0));

		WithdrawalExecution execution = pocketWithdrawalService.withdraw(
			USER_ID,
			IDEMPOTENCY_KEY,
			request(30_000L, ACCOUNT_ID)
		);

		assertThat(execution.repeated()).isFalse();
		assertThat(execution.response().transactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(execution.response().balanceAfter()).isEqualTo(34_000L);
		assertThat(execution.response().expectedDate()).isEqualTo(LocalDate.of(2026, 9, 8));
		assertThat(execution.response().accountSnapshot().accountNo()).isEqualTo("110-123-456789");
		ArgumentCaptor<PocketTransaction> captor = ArgumentCaptor.forClass(PocketTransaction.class);
		verify(pocketTransactionRepository).save(captor.capture());
		assertThat(captor.getValue().getTransactionCode()).matches("GP-2609-\\d{4}");
		assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
	}

	@Test
	void returnsExistingWithdrawalForRepeatedIdempotencyKey() {
		PocketTransaction existing = existingTransaction(TransactionStatus.COMPLETED);
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
			.thenReturn(Optional.of(existing));
		when(pocketTransactionRepository.sumAmountUntil(
			USER_ID,
			TransactionStatus.COMPLETED,
			TransactionDirection.CREDIT,
			existing.getCompletedAt(),
			existing.getId()
		)).thenReturn(64_000L);
		when(pocketTransactionRepository.sumAmountUntil(
			USER_ID,
			TransactionStatus.COMPLETED,
			TransactionDirection.DEBIT,
			existing.getCompletedAt(),
			existing.getId()
		)).thenReturn(30_000L);

		WithdrawalExecution execution = pocketWithdrawalService.withdraw(
			USER_ID,
			IDEMPOTENCY_KEY,
			new PocketWithdrawalRequest(StringNode.valueOf("invalid"), null)
		);

		assertThat(execution.repeated()).isTrue();
		assertThat(execution.response().transactionId()).isEqualTo(130L);
		assertThat(execution.response().balanceAfter()).isEqualTo(34_000L);
		verify(pocketTransactionRepository, never()).save(any());
	}

	@ParameterizedTest
	@ValueSource(longs = {0, -1})
	void rejectsNonPositiveAmount(long amount) {
		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request(amount, ACCOUNT_ID)),
			PocketErrorCode.POCKET_AMOUNT_INVALID
		);
	}

	@Test
	void rejectsDecimalAmount() {
		PocketWithdrawalRequest request = new PocketWithdrawalRequest(
			DecimalNode.valueOf(new java.math.BigDecimal("1000.5")),
			ACCOUNT_ID
		);
		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request),
			PocketErrorCode.POCKET_AMOUNT_INVALID
		);
	}

	@Test
	void rejectsStringAmount() {
		PocketWithdrawalRequest request = new PocketWithdrawalRequest(StringNode.valueOf("30000"), ACCOUNT_ID);
		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request),
			PocketErrorCode.POCKET_AMOUNT_INVALID
		);
	}

	@Test
	void rejectsMissingAmount() {
		PocketWithdrawalRequest request = new PocketWithdrawalRequest(null, ACCOUNT_ID);
		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request),
			PocketErrorCode.POCKET_AMOUNT_INVALID
		);
	}

	@Test
	void rejectsMissingAccount() {
		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request(30_000L, null)),
			PocketErrorCode.POCKET_ACCOUNT_REQUIRED
		);
	}

	@Test
	void rejectsAccountIdWhenUserHasNoActiveAccount() {
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.empty());
		when(withdrawalAccountRepository.existsByUserIdAndIsActiveTrue(USER_ID)).thenReturn(false);

		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request(30_000L, ACCOUNT_ID)),
			PocketErrorCode.POCKET_ACCOUNT_REQUIRED
		);
	}

	@Test
	void rejectsMissingOrOtherUsersAccount() {
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.empty());
		when(withdrawalAccountRepository.existsByUserIdAndIsActiveTrue(USER_ID)).thenReturn(true);

		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request(30_000L, ACCOUNT_ID)),
			PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND
		);
	}

	@Test
	void rejectsAmountGreaterThanCompletedBalance() {
		WithdrawalAccount account = account();
		when(withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
			.thenReturn(Optional.of(account));
		when(pocketTransactionRepository.sumAmount(USER_ID, TransactionStatus.COMPLETED, TransactionDirection.CREDIT))
			.thenReturn(20_000L);
		when(pocketTransactionRepository.sumAmount(USER_ID, TransactionStatus.COMPLETED, TransactionDirection.DEBIT))
			.thenReturn(0L);

		assertPocketError(
			() -> pocketWithdrawalService.withdraw(USER_ID, IDEMPOTENCY_KEY, request(30_000L, ACCOUNT_ID)),
			PocketErrorCode.POCKET_INSUFFICIENT_BALANCE
		);
	}

	@Test
	void returnsPagedWithdrawalHistoryAndMarksFailedItemRetryable() {
		PocketTransaction failed = existingTransaction(TransactionStatus.FAILED);
		PageRequest pageable = PageRequest.of(0, 20);
		when(pocketTransactionRepository.findByUserIdAndTransactionType(
			org.mockito.ArgumentMatchers.eq(USER_ID),
			org.mockito.ArgumentMatchers.eq(TransactionType.WITHDRAWAL),
			any()
		)).thenReturn(new PageImpl<>(List.of(failed), pageable, 1));

		PocketWithdrawalHistoryResponse response = pocketWithdrawalService.findWithdrawals(USER_ID, 0, 20);

		assertThat(response.content()).singleElement().satisfies(item -> {
			assertThat(item.transactionStatus()).isEqualTo(TransactionStatus.FAILED);
			assertThat(item.retryable()).isTrue();
			assertThat(item.failureReason()).isEqualTo("모의 출금 실패");
		});
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	void calculatesTwoWeekdaysWithoutSkippingHolidays() {
		assertThat(PocketWithdrawalService.calculateExpectedDate(LocalDate.of(2026, 9, 2)))
			.isEqualTo(LocalDate.of(2026, 9, 4));
		assertThat(PocketWithdrawalService.calculateExpectedDate(LocalDate.of(2026, 9, 4)))
			.isEqualTo(LocalDate.of(2026, 9, 8));
	}

	@Test
	void springCreatesWithdrawalServiceWithRuntimeConstructor() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(
				PocketTransactionRepository.class,
				() -> mock(PocketTransactionRepository.class)
			);
			context.registerBean(
				WithdrawalAccountRepository.class,
				() -> mock(WithdrawalAccountRepository.class)
			);
			context.registerBean(AccountNumberCipher.class, () -> mock(AccountNumberCipher.class));
			context.registerBean(
				PocketTransactionCodeGenerator.class,
				() -> mock(PocketTransactionCodeGenerator.class)
			);
			context.register(PocketWithdrawalService.class);
			context.refresh();

			assertThat(context.getBean(PocketWithdrawalService.class)).isNotNull();
		}
	}

	private PocketWithdrawalRequest request(long amount, Long accountId) {
		return new PocketWithdrawalRequest(LongNode.valueOf(amount), accountId);
	}

	private WithdrawalAccount account() {
		WithdrawalAccount account = mock(WithdrawalAccount.class);
		when(account.getId()).thenReturn(ACCOUNT_ID);
		when(account.getBankName()).thenReturn("신한은행");
		when(account.getEncryptedAccountNumber()).thenReturn(ENCRYPTED_ACCOUNT_NUMBER);
		when(account.getHolder()).thenReturn("김수현");
		return account;
	}

	private PocketTransaction existingTransaction(TransactionStatus status) {
		PocketTransaction transaction = mock(PocketTransaction.class);
		when(transaction.getId()).thenReturn(130L);
		when(transaction.getUserId()).thenReturn(USER_ID);
		when(transaction.getTransactionCode()).thenReturn("GP-2609-0025");
		when(transaction.getDirection()).thenReturn(TransactionDirection.DEBIT);
		when(transaction.getTransactionType()).thenReturn(TransactionType.WITHDRAWAL);
		when(transaction.getAmount()).thenReturn(30_000L);
		when(transaction.getTransactionStatus()).thenReturn(status);
		when(transaction.getRequestedAt()).thenReturn(LocalDateTime.of(2026, 9, 4, 10, 0));
		when(transaction.getExpectedDate()).thenReturn(LocalDate.of(2026, 9, 8));
		when(transaction.getCompletedAt()).thenReturn(LocalDateTime.of(2026, 9, 4, 10, 0));
		when(transaction.getAccountSnapshot()).thenReturn(
			new WithdrawalAccountSnapshot("신한은행", "110-123-456789", "김수현")
		);
		when(transaction.getFailureReason()).thenReturn(status == TransactionStatus.FAILED ? "모의 출금 실패" : null);
		return transaction;
	}

	private void assertPocketError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, PocketErrorCode code) {
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
			.thenReturn(Optional.empty());
		assertThatThrownBy(action)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(code));
	}
}
