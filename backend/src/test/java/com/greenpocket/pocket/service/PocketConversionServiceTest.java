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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.greenpocket.eco.repository.EcoMileageQueryRepository.ConfirmedMileageRoundSnapshot;
import com.greenpocket.eco.service.EcoMileageQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.dto.PocketConversionRequest;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.repository.PocketTransactionRepository;

class PocketConversionServiceTest {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final Long USER_ID = 42L;
	private static final Long ROUND_ID = 7L;
	private static final Long CONVERSION_ID = 120L;
	private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 4, 10, 0);

	private PocketTransactionRepository pocketTransactionRepository;
	private EcoMileageQueryService ecoMileageQueryService;
	private PocketConversionService pocketConversionService;

	@BeforeEach
	void setUp() {
		pocketTransactionRepository = mock(PocketTransactionRepository.class);
		ecoMileageQueryService = mock(EcoMileageQueryService.class);
		Clock clock = Clock.fixed(Instant.parse("2026-09-04T01:00:00Z"), KOREA_ZONE_ID);
		pocketConversionService = new PocketConversionService(
			pocketTransactionRepository,
			ecoMileageQueryService,
			new PocketTransactionCodeGenerator(pocketTransactionRepository),
			clock
		);
	}

	@Test
	void startsRequestedConversion() {
		stubConfirmedRound();
		when(pocketTransactionRepository.findByUserIdAndSourceTypeAndSourceKey(
			USER_ID, TransactionSourceType.ECO_ROUND, "7"
		)).thenReturn(Optional.empty());
		when(pocketTransactionRepository.existsByTransactionCode(any())).thenReturn(false);
		when(pocketTransactionRepository.saveAndFlush(any(PocketTransaction.class)))
			.thenAnswer(invocation -> withId(invocation.getArgument(0), CONVERSION_ID));

		var response = pocketConversionService.start(USER_ID, new PocketConversionRequest(ROUND_ID, true));

		assertThat(response.conversionId()).isEqualTo(CONVERSION_ID);
		assertThat(response.amount()).isEqualTo(30_000L);
		assertThat(response.transactionStatus()).isEqualTo(TransactionStatus.REQUESTED);
		assertThat(response.externalUrl()).isEqualTo("https://ecomileage.seoul.go.kr/mileage/convert");
		assertThat(response.requestedAt().toLocalDateTime()).isEqualTo(NOW);
		assertThat(response.notice()).isEqualTo("현금으로 바꿔야 그린포켓 계좌로 들어와요");
	}

	@Test
	void rejectsConversionWithoutAgreement() {
		assertError(
			() -> pocketConversionService.start(USER_ID, new PocketConversionRequest(ROUND_ID, false)),
			"INVALID_REQUEST"
		);
		verify(ecoMileageQueryService, never()).findConfirmedMileageRound(USER_ID, ROUND_ID);
	}

	@Test
	void rejectsRoundWithoutConfirmedMileage() {
		when(ecoMileageQueryService.findConfirmedMileageRound(USER_ID, ROUND_ID)).thenReturn(Optional.empty());

		assertError(
			() -> pocketConversionService.start(USER_ID, new PocketConversionRequest(ROUND_ID, true)),
			"CONVERSION_NOT_AVAILABLE"
		);
	}

	@Test
	void rejectsAlreadyRequestedRound() {
		stubConfirmedRound();
		PocketTransaction existing = requestedTransaction();
		when(pocketTransactionRepository.findByUserIdAndSourceTypeAndSourceKey(
			USER_ID, TransactionSourceType.ECO_ROUND, "7"
		)).thenReturn(Optional.of(existing));

		assertError(
			() -> pocketConversionService.start(USER_ID, new PocketConversionRequest(ROUND_ID, true)),
			"CONVERSION_ALREADY_DONE"
		);
	}

	@Test
	void rejectsSecondConversionOnSameDay() {
		stubConfirmedRound();
		when(pocketTransactionRepository.findByUserIdAndSourceTypeAndSourceKey(
			USER_ID, TransactionSourceType.ECO_ROUND, "7"
		)).thenReturn(Optional.empty());
		when(pocketTransactionRepository
			.existsByUserIdAndTransactionTypeAndRequestedAtGreaterThanEqualAndRequestedAtLessThan(
				USER_ID,
				TransactionType.ECO_MILEAGE,
				LocalDate.of(2026, 9, 4).atStartOfDay(),
				LocalDate.of(2026, 9, 5).atStartOfDay()
			)).thenReturn(true);

		assertError(
			() -> pocketConversionService.start(USER_ID, new PocketConversionRequest(ROUND_ID, true)),
			"CONVERSION_DAILY_LIMIT"
		);
	}

	@Test
	void retriesFailedConversionAfterDailyLimitExpires() {
		stubConfirmedRound();
		PocketTransaction failed = requestedTransaction();
		ReflectionTestUtils.setField(failed, "transactionStatus", TransactionStatus.FAILED);
		when(pocketTransactionRepository.findByUserIdAndSourceTypeAndSourceKey(
			USER_ID, TransactionSourceType.ECO_ROUND, "7"
		)).thenReturn(Optional.of(failed));
		when(pocketTransactionRepository.saveAndFlush(failed)).thenReturn(failed);

		var response = pocketConversionService.start(USER_ID, new PocketConversionRequest(ROUND_ID, true));

		assertThat(response.conversionId()).isEqualTo(CONVERSION_ID);
		assertThat(failed.getTransactionStatus()).isEqualTo(TransactionStatus.REQUESTED);
		assertThat(failed.getRequestedAt()).isEqualTo(NOW);
	}

	@Test
	void completesRequestedConversionAndReturnsBalance() {
		PocketTransaction requested = requestedTransaction();
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKeyAndTransactionType(
			USER_ID, IDEMPOTENCY_KEY, TransactionType.ECO_MILEAGE
		)).thenReturn(Optional.empty());
		when(pocketTransactionRepository.findByIdAndUserId(CONVERSION_ID, USER_ID))
			.thenReturn(Optional.of(requested));
		when(pocketTransactionRepository.saveAndFlush(requested)).thenReturn(requested);
		when(pocketTransactionRepository.sumAmount(
			USER_ID, TransactionStatus.COMPLETED, TransactionDirection.CREDIT
		)).thenReturn(94_000L);
		when(pocketTransactionRepository.sumAmount(
			USER_ID, TransactionStatus.COMPLETED, TransactionDirection.DEBIT
		)).thenReturn(0L);

		var execution = pocketConversionService.complete(USER_ID, CONVERSION_ID, IDEMPOTENCY_KEY);

		assertThat(execution.repeated()).isFalse();
		assertThat(execution.response().transactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(execution.response().completedAt().toLocalDateTime()).isEqualTo(NOW);
		assertThat(execution.response().balanceAfter()).isEqualTo(94_000L);
		assertThat(execution.response().transaction().label()).isEqualTo("에코마일리지 2026 상반기");
	}

	@Test
	void returnsSameCompletedConversionForRepeatedIdempotencyKey() {
		PocketTransaction completed = requestedTransaction();
		completed.completeEcoMileage(IDEMPOTENCY_KEY, NOW);
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKeyAndTransactionType(
			USER_ID, IDEMPOTENCY_KEY, TransactionType.ECO_MILEAGE
		)).thenReturn(Optional.of(completed));
		when(pocketTransactionRepository.sumAmountUntil(
			USER_ID,
			TransactionStatus.COMPLETED,
			TransactionDirection.CREDIT,
			NOW,
			CONVERSION_ID
		)).thenReturn(94_000L);
		when(pocketTransactionRepository.sumAmountUntil(
			USER_ID,
			TransactionStatus.COMPLETED,
			TransactionDirection.DEBIT,
			NOW,
			CONVERSION_ID
		)).thenReturn(0L);

		var execution = pocketConversionService.complete(USER_ID, CONVERSION_ID, IDEMPOTENCY_KEY);

		assertThat(execution.repeated()).isTrue();
		assertThat(execution.response().balanceAfter()).isEqualTo(94_000L);
		verify(pocketTransactionRepository, never()).findByIdAndUserId(CONVERSION_ID, USER_ID);
	}

	@Test
	void rejectsCompletionWithoutRequestedConversion() {
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKeyAndTransactionType(
			USER_ID, IDEMPOTENCY_KEY, TransactionType.ECO_MILEAGE
		)).thenReturn(Optional.empty());
		when(pocketTransactionRepository.findByIdAndUserId(CONVERSION_ID, USER_ID))
			.thenReturn(Optional.empty());

		assertError(
			() -> pocketConversionService.complete(USER_ID, CONVERSION_ID, IDEMPOTENCY_KEY),
			"CONVERSION_NOT_RETURNED"
		);
	}

	@Test
	void rejectsCompletedConversionWithDifferentIdempotencyKey() {
		PocketTransaction completed = requestedTransaction();
		completed.completeEcoMileage("47c52f2c-0708-4a64-8ea0-f87aceee23af", NOW);
		when(pocketTransactionRepository.findByUserIdAndIdempotencyKeyAndTransactionType(
			USER_ID, IDEMPOTENCY_KEY, TransactionType.ECO_MILEAGE
		)).thenReturn(Optional.empty());
		when(pocketTransactionRepository.findByIdAndUserId(CONVERSION_ID, USER_ID))
			.thenReturn(Optional.of(completed));

		assertError(
			() -> pocketConversionService.complete(USER_ID, CONVERSION_ID, IDEMPOTENCY_KEY),
			"CONVERSION_ALREADY_DONE"
		);
	}

	private void stubConfirmedRound() {
		when(ecoMileageQueryService.findConfirmedMileageRound(USER_ID, ROUND_ID))
			.thenReturn(Optional.of(new ConfirmedMileageRoundSnapshot(
				ROUND_ID,
				LocalDate.of(2026, 4, 1),
				LocalDate.of(2026, 9, 1),
				30_000L
			)));
	}

	private PocketTransaction requestedTransaction() {
		PocketTransaction transaction = PocketTransaction.requestedEcoMileage(
			USER_ID,
			ROUND_ID,
			"GP-2609-0021",
			30_000L,
			"에코마일리지 2026 상반기",
			NOW.minusMinutes(2)
		);
		return withId(transaction, CONVERSION_ID);
	}

	private PocketTransaction withId(PocketTransaction transaction, Long id) {
		ReflectionTestUtils.setField(transaction, "id", id);
		return transaction;
	}

	private void assertError(ThrowingCall call, String errorCode) {
		assertThatThrownBy(call::run)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode().code()).isEqualTo(errorCode));
	}

	@FunctionalInterface
	private interface ThrowingCall {
		void run();
	}
}
