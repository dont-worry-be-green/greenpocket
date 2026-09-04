package com.greenpocket.pocket.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.dto.PocketGreenlifeCreditResult;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PocketGreenlifeCreditService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final PocketTransactionRepository pocketTransactionRepository;
	private final PocketTransactionCodeGenerator transactionCodeGenerator;

	@Transactional
	public PocketGreenlifeCreditResult creditGreenlifeMonth(
		Long userId,
		YearMonth yearMonth,
		long paidTotal,
		LocalDateTime completedAt
	) {
		if (paidTotal <= 0) {
			throw new BusinessException(PocketErrorCode.POCKET_AMOUNT_INVALID);
		}

		String sourceKey = yearMonth.toString();
		PocketTransaction existing = pocketTransactionRepository
			.findByUserIdAndSourceTypeAndSourceKey(
				userId,
				TransactionSourceType.GREENLIFE_MONTH,
				sourceKey
			)
			.orElse(null);
		if (existing != null) {
			return toResult(existing, false);
		}

		PocketTransaction transaction = PocketTransaction.completedGreenlifeCredit(
			userId,
			transactionCodeGenerator.generate(completedAt),
			sourceKey,
			paidTotal,
			"녹색생활실천 %d월분".formatted(yearMonth.getMonthValue()),
			completedAt
		);
		return toResult(pocketTransactionRepository.saveAndFlush(transaction), true);
	}

	private PocketGreenlifeCreditResult toResult(PocketTransaction transaction, boolean created) {
		return new PocketGreenlifeCreditResult(
			created,
			transaction.getId(),
			transaction.getTransactionCode(),
			transaction.getDirection(),
			transaction.getTransactionType(),
			transaction.getAmount(),
			transaction.getTransactionStatus(),
			transaction.getLabel(),
			transaction.getCompletedAt().atZone(KOREA_ZONE_ID).toOffsetDateTime()
		);
	}
}
