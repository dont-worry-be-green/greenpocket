package com.greenpocket.pocket.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.repository.EcoMileageQueryRepository.ConfirmedMileageRoundSnapshot;
import com.greenpocket.eco.service.EcoMileageQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.pocket.dto.PocketConversionCompleteResponse;
import com.greenpocket.pocket.dto.PocketConversionRequest;
import com.greenpocket.pocket.dto.PocketConversionStartResponse;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;

@Service
@Transactional(readOnly = true)
public class PocketConversionService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int TRANSACTION_CODE_ATTEMPTS = 100;
	private static final String EXTERNAL_URL = "https://ecomileage.seoul.go.kr/mileage/convert";
	private static final String CONVERSION_NOTICE = "현금으로 바꿔야 그린포켓 계좌로 들어와요";

	private final PocketTransactionRepository pocketTransactionRepository;
	private final EcoMileageQueryService ecoMileageQueryService;
	private final Clock clock;

	@Autowired
	public PocketConversionService(
		PocketTransactionRepository pocketTransactionRepository,
		EcoMileageQueryService ecoMileageQueryService
	) {
		this(
			pocketTransactionRepository,
			ecoMileageQueryService,
			Clock.system(KOREA_ZONE_ID)
		);
	}

	PocketConversionService(
		PocketTransactionRepository pocketTransactionRepository,
		EcoMileageQueryService ecoMileageQueryService,
		Clock clock
	) {
		this.pocketTransactionRepository = pocketTransactionRepository;
		this.ecoMileageQueryService = ecoMileageQueryService;
		this.clock = clock;
	}

	@Transactional
	public PocketConversionStartResponse start(Long userId, PocketConversionRequest request) {
		if (!Boolean.TRUE.equals(request.agreed())) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "agreed", null);
		}

		ConfirmedMileageRoundSnapshot round = ecoMileageQueryService
			.findConfirmedMileageRound(userId, request.roundId())
			.orElseThrow(() -> new BusinessException(PocketErrorCode.CONVERSION_NOT_AVAILABLE));
		LocalDateTime requestedAt = LocalDateTime.now(clock);
		PocketTransaction existing = pocketTransactionRepository
			.findByUserIdAndSourceTypeAndSourceKey(
				userId,
				TransactionSourceType.ECO_ROUND,
				round.roundId().toString()
			)
			.orElse(null);

		if (existing != null && existing.getTransactionStatus() != TransactionStatus.FAILED) {
			throw new BusinessException(PocketErrorCode.CONVERSION_ALREADY_DONE);
		}
		if (hasConversionToday(userId, requestedAt.toLocalDate())) {
			throw new BusinessException(PocketErrorCode.CONVERSION_DAILY_LIMIT);
		}

		if (existing != null) {
			existing.retryEcoMileage(round.confirmedMileage(), requestedAt);
			return toStartResponse(pocketTransactionRepository.saveAndFlush(existing));
		}

		PocketTransaction transaction = PocketTransaction.requestedEcoMileage(
			userId,
			round.roundId(),
			generateTransactionCode(requestedAt),
			round.confirmedMileage(),
			conversionLabel(round),
			requestedAt
		);
		return toStartResponse(pocketTransactionRepository.saveAndFlush(transaction));
	}

	@Transactional
	public CompletionExecution complete(Long userId, Long conversionId, String idempotencyKey) {
		PocketTransaction repeated = pocketTransactionRepository
			.findByUserIdAndIdempotencyKeyAndTransactionType(
				userId,
				idempotencyKey,
				TransactionType.ECO_MILEAGE
			)
			.orElse(null);
		if (repeated != null) {
			return new CompletionExecution(toCompleteResponse(repeated, calculateBalanceAt(repeated)), true);
		}

		PocketTransaction transaction = pocketTransactionRepository.findByIdAndUserId(conversionId, userId)
			.orElseThrow(() -> new BusinessException(PocketErrorCode.CONVERSION_NOT_RETURNED));
		if (transaction.getTransactionType() != TransactionType.ECO_MILEAGE
			|| transaction.getTransactionStatus() != TransactionStatus.REQUESTED) {
			if (transaction.getTransactionType() == TransactionType.ECO_MILEAGE
				&& transaction.getTransactionStatus() == TransactionStatus.COMPLETED) {
				throw new BusinessException(PocketErrorCode.CONVERSION_ALREADY_DONE);
			}
			throw new BusinessException(PocketErrorCode.CONVERSION_NOT_RETURNED);
		}

		transaction.completeEcoMileage(idempotencyKey, LocalDateTime.now(clock));
		PocketTransaction completed = pocketTransactionRepository.saveAndFlush(transaction);
		return new CompletionExecution(toCompleteResponse(completed, calculateBalance(userId)), false);
	}

	private boolean hasConversionToday(Long userId, LocalDate date) {
		return pocketTransactionRepository
			.existsByUserIdAndTransactionTypeAndRequestedAtGreaterThanEqualAndRequestedAtLessThan(
				userId,
				TransactionType.ECO_MILEAGE,
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay()
			);
	}

	private long calculateBalance(Long userId) {
		long credits = pocketTransactionRepository.sumAmount(
			userId,
			TransactionStatus.COMPLETED,
			TransactionDirection.CREDIT
		);
		long debits = pocketTransactionRepository.sumAmount(
			userId,
			TransactionStatus.COMPLETED,
			TransactionDirection.DEBIT
		);
		return credits - debits;
	}

	private long calculateBalanceAt(PocketTransaction transaction) {
		long credits = pocketTransactionRepository.sumAmountUntil(
			transaction.getUserId(),
			TransactionStatus.COMPLETED,
			TransactionDirection.CREDIT,
			transaction.getCompletedAt(),
			transaction.getId()
		);
		long debits = pocketTransactionRepository.sumAmountUntil(
			transaction.getUserId(),
			TransactionStatus.COMPLETED,
			TransactionDirection.DEBIT,
			transaction.getCompletedAt(),
			transaction.getId()
		);
		return credits - debits;
	}

	private String generateTransactionCode(LocalDateTime requestedAt) {
		String prefix = "GP-%02d%02d-".formatted(
			requestedAt.getYear() % 100,
			requestedAt.getMonthValue()
		);
		for (int attempt = 0; attempt < TRANSACTION_CODE_ATTEMPTS; attempt++) {
			String code = prefix + "%04d".formatted(ThreadLocalRandom.current().nextInt(10_000));
			if (!pocketTransactionRepository.existsByTransactionCode(code)) {
				return code;
			}
		}
		throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
	}

	private String conversionLabel(ConfirmedMileageRoundSnapshot round) {
		String half = round.periodStart().getMonthValue() <= 6 ? "상반기" : "하반기";
		return "에코마일리지 %d %s".formatted(round.periodStart().getYear(), half);
	}

	private PocketConversionStartResponse toStartResponse(PocketTransaction transaction) {
		return new PocketConversionStartResponse(
			transaction.getId(),
			transaction.getEcoRoundId(),
			transaction.getAmount(),
			transaction.getTransactionStatus(),
			EXTERNAL_URL,
			toOffsetDateTime(transaction.getRequestedAt()),
			CONVERSION_NOTICE
		);
	}

	private PocketConversionCompleteResponse toCompleteResponse(PocketTransaction transaction, long balanceAfter) {
		return new PocketConversionCompleteResponse(
			transaction.getId(),
			transaction.getTransactionStatus(),
			transaction.getAmount(),
			toOffsetDateTime(transaction.getCompletedAt()),
			balanceAfter,
			new PocketConversionCompleteResponse.Transaction(
				transaction.getId(),
				transaction.getTransactionCode(),
				transaction.getLabel()
			)
		);
	}

	private java.time.OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	public record CompletionExecution(PocketConversionCompleteResponse response, boolean repeated) {
	}
}
