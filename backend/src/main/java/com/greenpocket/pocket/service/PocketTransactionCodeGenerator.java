package com.greenpocket.pocket.service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;

@Component
@RequiredArgsConstructor
public class PocketTransactionCodeGenerator {

	private static final int GENERATION_ATTEMPTS = 100;

	private final PocketTransactionRepository pocketTransactionRepository;

	public String generate(LocalDateTime occurredAt) {
		String prefix = "GP-%02d%02d-".formatted(
			occurredAt.getYear() % 100,
			occurredAt.getMonthValue()
		);
		for (int attempt = 0; attempt < GENERATION_ATTEMPTS; attempt++) {
			String code = prefix + "%04d".formatted(ThreadLocalRandom.current().nextInt(10_000));
			if (!pocketTransactionRepository.existsByTransactionCode(code)) {
				return code;
			}
		}
		throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
	}
}
