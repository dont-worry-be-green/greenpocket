package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;

class PocketTransactionCodeGeneratorTest {

	@Test
	void generatesCodeWithOccurrenceYearAndMonth() {
		PocketTransactionRepository repository = mock(PocketTransactionRepository.class);
		when(repository.existsByTransactionCode(anyString())).thenReturn(false);
		PocketTransactionCodeGenerator generator = new PocketTransactionCodeGenerator(repository);

		String code = generator.generate(LocalDateTime.of(2026, 8, 10, 0, 0));

		assertThat(code).matches("GP-2608-[0-9]{4}");
	}

	@Test
	void failsAfterCodeGenerationAttemptsAreExhausted() {
		PocketTransactionRepository repository = mock(PocketTransactionRepository.class);
		when(repository.existsByTransactionCode(anyString())).thenReturn(true);
		PocketTransactionCodeGenerator generator = new PocketTransactionCodeGenerator(repository);

		assertThatThrownBy(() -> generator.generate(LocalDateTime.of(2026, 8, 10, 0, 0)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_ERROR));
	}
}
