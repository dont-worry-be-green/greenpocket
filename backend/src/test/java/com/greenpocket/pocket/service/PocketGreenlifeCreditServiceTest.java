package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.dto.PocketGreenlifeCreditResult;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;

class PocketGreenlifeCreditServiceTest {

	private static final Long USER_ID = 42L;
	private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 7);
	private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 8, 10, 0, 0);

	private PocketTransactionRepository repository;
	private PocketTransactionCodeGenerator codeGenerator;
	private PocketGreenlifeCreditService service;

	@BeforeEach
	void setUp() {
		repository = mock(PocketTransactionRepository.class);
		codeGenerator = mock(PocketTransactionCodeGenerator.class);
		service = new PocketGreenlifeCreditService(repository, codeGenerator);
	}

	@Test
	void createsCompletedCreditForPaidGreenlifeMonth() {
		when(repository.findByUserIdAndSourceTypeAndSourceKey(
			USER_ID, TransactionSourceType.GREENLIFE_MONTH, "2026-07"
		)).thenReturn(Optional.empty());
		when(codeGenerator.generate(COMPLETED_AT)).thenReturn("GP-2608-0001");
		when(repository.saveAndFlush(any(PocketTransaction.class))).thenAnswer(invocation -> {
			PocketTransaction transaction = invocation.getArgument(0);
			ReflectionTestUtils.setField(transaction, "id", 88L);
			return transaction;
		});

		PocketGreenlifeCreditResult result = service.creditGreenlifeMonth(
			USER_ID, YEAR_MONTH, 3_140L, COMPLETED_AT
		);

		assertThat(result.created()).isTrue();
		assertThat(result.transactionId()).isEqualTo(88L);
		assertThat(result.transactionCode()).isEqualTo("GP-2608-0001");
		assertThat(result.direction()).isEqualTo(TransactionDirection.CREDIT);
		assertThat(result.transactionType()).isEqualTo(TransactionType.GREENLIFE);
		assertThat(result.amount()).isEqualTo(3_140L);
		assertThat(result.transactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
		assertThat(result.label()).isEqualTo("녹색생활실천 7월분");
		assertThat(result.completedAt().toString()).isEqualTo("2026-08-10T00:00+09:00");
	}

	@Test
	void returnsExistingCreditWithoutCreatingDuplicate() {
		PocketTransaction existing = PocketTransaction.completedGreenlifeCredit(
			USER_ID, "GP-2608-0001", "2026-07", 3_140L, "녹색생활실천 7월분", COMPLETED_AT
		);
		ReflectionTestUtils.setField(existing, "id", 88L);
		when(repository.findByUserIdAndSourceTypeAndSourceKey(
			USER_ID, TransactionSourceType.GREENLIFE_MONTH, "2026-07"
		)).thenReturn(Optional.of(existing));

		PocketGreenlifeCreditResult result = service.creditGreenlifeMonth(
			USER_ID, YEAR_MONTH, 3_140L, COMPLETED_AT
		);

		assertThat(result.created()).isFalse();
		assertThat(result.transactionId()).isEqualTo(88L);
		verify(repository, never()).saveAndFlush(any());
		verify(codeGenerator, never()).generate(any());
	}

	@Test
	void rejectsNonPositiveCreditAmount() {
		assertThatThrownBy(() -> service.creditGreenlifeMonth(USER_ID, YEAR_MONTH, 0L, COMPLETED_AT))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(PocketErrorCode.POCKET_AMOUNT_INVALID));
		verify(repository, never()).saveAndFlush(any());
	}
}
