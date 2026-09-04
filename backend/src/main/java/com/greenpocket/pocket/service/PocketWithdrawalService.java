package com.greenpocket.pocket.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.pocket.crypto.AccountNumberCipher;
import com.greenpocket.pocket.dto.PocketWithdrawalHistoryItemResponse;
import com.greenpocket.pocket.dto.PocketWithdrawalHistoryResponse;
import com.greenpocket.pocket.dto.PocketWithdrawalRequest;
import com.greenpocket.pocket.dto.PocketWithdrawalResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountSnapshotResponse;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.entity.WithdrawalAccount;
import com.greenpocket.pocket.entity.WithdrawalAccountSnapshot;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.repository.PocketTransactionRepository;
import com.greenpocket.pocket.repository.WithdrawalAccountRepository;

@Service
@Transactional(readOnly = true)
public class PocketWithdrawalService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final String WITHDRAWAL_NOTICE = "영업일 기준 1~2일 내에 입금될 예정이에요";

	private final PocketTransactionRepository pocketTransactionRepository;
	private final WithdrawalAccountRepository withdrawalAccountRepository;
	private final AccountNumberCipher accountNumberCipher;
	private final PocketTransactionCodeGenerator transactionCodeGenerator;
	private final Clock clock;

	@Autowired
	public PocketWithdrawalService(
		PocketTransactionRepository pocketTransactionRepository,
		WithdrawalAccountRepository withdrawalAccountRepository,
		AccountNumberCipher accountNumberCipher,
		PocketTransactionCodeGenerator transactionCodeGenerator
	) {
		this(
			pocketTransactionRepository,
			withdrawalAccountRepository,
			accountNumberCipher,
			transactionCodeGenerator,
			Clock.system(KOREA_ZONE_ID)
		);
	}

	PocketWithdrawalService(
		PocketTransactionRepository pocketTransactionRepository,
		WithdrawalAccountRepository withdrawalAccountRepository,
		AccountNumberCipher accountNumberCipher,
		PocketTransactionCodeGenerator transactionCodeGenerator,
		Clock clock
	) {
		this.pocketTransactionRepository = pocketTransactionRepository;
		this.withdrawalAccountRepository = withdrawalAccountRepository;
		this.accountNumberCipher = accountNumberCipher;
		this.transactionCodeGenerator = transactionCodeGenerator;
		this.clock = clock;
	}

	@Transactional
	public WithdrawalExecution withdraw(
		Long userId,
		String idempotencyKey,
		PocketWithdrawalRequest request
	) {
		return pocketTransactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
			.map(transaction -> new WithdrawalExecution(toIdempotentResponse(transaction), true))
			.orElseGet(() -> new WithdrawalExecution(
				createWithdrawal(userId, idempotencyKey, request),
				false
			));
	}

	public PocketWithdrawalHistoryResponse findWithdrawals(Long userId, int page, int size) {
		PageRequest pageable = PageRequest.of(
			page,
			size,
			Sort.by(Sort.Direction.DESC, "requestedAt", "id")
		);
		Page<PocketTransaction> result = pocketTransactionRepository.findByUserIdAndTransactionType(
			userId,
			TransactionType.WITHDRAWAL,
			pageable
		);

		return new PocketWithdrawalHistoryResponse(
			result.getContent().stream().map(this::toHistoryItem).toList(),
			result.getNumber(),
			result.getSize(),
			result.getTotalElements(),
			result.getTotalPages(),
			result.hasNext()
		);
	}

	private PocketWithdrawalResponse createWithdrawal(
		Long userId,
		String idempotencyKey,
		PocketWithdrawalRequest request
	) {
		long amount = validateAmount(request.amount());
		WithdrawalAccount account = findWithdrawalAccount(userId, request.accountId());
		long balance = calculateBalance(userId);
		if (amount > balance) {
			throw new BusinessException(PocketErrorCode.POCKET_INSUFFICIENT_BALANCE);
		}

		LocalDateTime requestedAt = LocalDateTime.now(clock);
		WithdrawalAccountSnapshot snapshot = new WithdrawalAccountSnapshot(
			account.getBankName(),
			accountNumberCipher.decrypt(account.getEncryptedAccountNumber()),
			account.getHolder()
		);
		PocketTransaction transaction = PocketTransaction.completedWithdrawal(
			userId,
			account,
			transactionCodeGenerator.generate(requestedAt),
			idempotencyKey,
			amount,
			snapshot,
			requestedAt,
			calculateExpectedDate(requestedAt.toLocalDate())
		);
		PocketTransaction saved = pocketTransactionRepository.save(transaction);
		return toResponse(saved, balance - amount);
	}

	private long validateAmount(JsonNode amountNode) {
		if (amountNode == null || !amountNode.isIntegralNumber() || !amountNode.canConvertToLong()) {
			throw new BusinessException(PocketErrorCode.POCKET_AMOUNT_INVALID);
		}
		long amount = amountNode.longValue();
		if (amount <= 0) {
			throw new BusinessException(PocketErrorCode.POCKET_AMOUNT_INVALID);
		}
		return amount;
	}

	private WithdrawalAccount findWithdrawalAccount(Long userId, Long accountId) {
		if (accountId == null) {
			throw new BusinessException(PocketErrorCode.POCKET_ACCOUNT_REQUIRED);
		}
		return withdrawalAccountRepository.findByIdAndUserIdAndIsActiveTrue(accountId, userId)
			.orElseThrow(() -> {
				if (!withdrawalAccountRepository.existsByUserIdAndIsActiveTrue(userId)) {
					return new BusinessException(PocketErrorCode.POCKET_ACCOUNT_REQUIRED);
				}
				return new BusinessException(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND);
			});
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

	static LocalDate calculateExpectedDate(LocalDate requestedDate) {
		LocalDate expectedDate = requestedDate;
		int businessDays = 0;
		while (businessDays < 2) {
			expectedDate = expectedDate.plusDays(1);
			DayOfWeek dayOfWeek = expectedDate.getDayOfWeek();
			if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
				businessDays++;
			}
		}
		return expectedDate;
	}

	private PocketWithdrawalResponse toIdempotentResponse(PocketTransaction transaction) {
		return toResponse(transaction, calculateBalanceAt(transaction));
	}

	private PocketWithdrawalResponse toResponse(PocketTransaction transaction, long balanceAfter) {
		return new PocketWithdrawalResponse(
			transaction.getId(),
			transaction.getTransactionCode(),
			transaction.getDirection(),
			transaction.getTransactionType(),
			transaction.getAmount(),
			transaction.getTransactionStatus(),
			toOffsetDateTime(transaction.getRequestedAt()),
			transaction.getExpectedDate(),
			balanceAfter,
			toSnapshotResponse(transaction.getAccountSnapshot()),
			WITHDRAWAL_NOTICE
		);
	}

	private PocketWithdrawalHistoryItemResponse toHistoryItem(PocketTransaction transaction) {
		return new PocketWithdrawalHistoryItemResponse(
			transaction.getId(),
			transaction.getTransactionCode(),
			transaction.getAmount(),
			transaction.getTransactionStatus(),
			toOffsetDateTime(transaction.getRequestedAt()),
			transaction.getExpectedDate(),
			toOffsetDateTime(transaction.getCompletedAt()),
			toSnapshotResponse(transaction.getAccountSnapshot()),
			transaction.getFailureReason(),
			transaction.getTransactionStatus() == TransactionStatus.FAILED
		);
	}

	private WithdrawalAccountSnapshotResponse toSnapshotResponse(WithdrawalAccountSnapshot snapshot) {
		if (snapshot == null) {
			return null;
		}
		return new WithdrawalAccountSnapshotResponse(
			snapshot.bankName(),
			snapshot.accountNo(),
			snapshot.holder()
		);
	}

	private java.time.OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}

	public record WithdrawalExecution(
		PocketWithdrawalResponse response,
		boolean repeated
	) {
	}
}
