package com.greenpocket.pocket.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenpocket.eco.repository.EcoMileageQueryRepository.ConfirmedMileageRoundSnapshot;
import com.greenpocket.eco.service.EcoMileageQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse.BlockReason;
import com.greenpocket.pocket.dto.PocketBalanceResponse;
import com.greenpocket.pocket.dto.PocketMainResponse;
import com.greenpocket.pocket.dto.PocketManagementResponse;
import com.greenpocket.pocket.dto.PocketTransactionItemResponse;
import com.greenpocket.pocket.dto.PocketTransactionListResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountResponse;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.repository.PocketTransactionRepository;
import com.greenpocket.user.repository.UserPocketQueryRepository.UserPocketSnapshot;
import com.greenpocket.user.service.UserPocketQueryService;

@Service
@Transactional(readOnly = true)
public class PocketQueryService {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final List<String> NOTICES = List.of(
		"마일리지 전환은 1일 1회만 가능해요",
		"전환 후 취소는 불가능하니 신중히 확인해 주세요",
		"실패한 거래는 잔액에 반영되지 않아요. 같은 수령 건은 한 번만 적립돼요"
	);

	private final PocketTransactionRepository pocketTransactionRepository;
	private final WithdrawalAccountService withdrawalAccountService;
	private final UserPocketQueryService userPocketQueryService;
	private final EcoMileageQueryService ecoMileageQueryService;
	private final Clock clock;

	@Autowired
	public PocketQueryService(
		PocketTransactionRepository pocketTransactionRepository,
		WithdrawalAccountService withdrawalAccountService,
		UserPocketQueryService userPocketQueryService,
		EcoMileageQueryService ecoMileageQueryService
	) {
		this(
			pocketTransactionRepository,
			withdrawalAccountService,
			userPocketQueryService,
			ecoMileageQueryService,
			Clock.system(KOREA_ZONE_ID)
		);
	}

	PocketQueryService(
		PocketTransactionRepository pocketTransactionRepository,
		WithdrawalAccountService withdrawalAccountService,
		UserPocketQueryService userPocketQueryService,
		EcoMileageQueryService ecoMileageQueryService,
		Clock clock
	) {
		this.pocketTransactionRepository = pocketTransactionRepository;
		this.withdrawalAccountService = withdrawalAccountService;
		this.userPocketQueryService = userPocketQueryService;
		this.ecoMileageQueryService = ecoMileageQueryService;
		this.clock = clock;
	}

	public PocketMainResponse getPocket(Long userId) {
		UserPocketSnapshot pocket = findPocket(userId);
		long balance = calculateBalance(userId);
		List<ConvertibleMileageResponse.Round> rounds = findConvertibleRounds(userId);
		List<WithdrawalAccountResponse> accounts = withdrawalAccountService.findAccounts(userId).accounts();
		PocketMainResponse.DefaultAccount defaultAccount = accounts.stream()
			.filter(WithdrawalAccountResponse::isDefault)
			.findFirst()
			.map(this::toDefaultAccount)
			.orElse(null);
		List<PocketTransactionItemResponse> recentTransactions = pocketTransactionRepository
			.findTop2ByUserIdAndDirectionAndTransactionStatusOrderByCompletedAtDescIdDesc(
				userId,
				TransactionDirection.CREDIT,
				TransactionStatus.COMPLETED
			)
			.stream()
			.map(this::toTransactionItem)
			.toList();

		return new PocketMainResponse(
			new PocketMainResponse.Pocket(pocket.accountNo(), pocket.holder()),
			balance,
			new PocketMainResponse.Breakdown(
				sumCompletedCredit(userId, TransactionType.ECO_MILEAGE),
				sumCompletedCredit(userId, TransactionType.GREENLIFE)
			),
			sumConfirmedMileage(rounds),
			rounds.isEmpty() ? null : toConvertibleSource(rounds.getFirst()),
			defaultAccount,
			recentTransactions,
			new PocketMainResponse.Empty(
				accounts.isEmpty(),
				!pocketTransactionRepository.existsByUserId(userId)
			),
			NOTICES
		);
	}

	public PocketBalanceResponse getBalance(Long userId) {
		findPocket(userId);
		return new PocketBalanceResponse(
			calculateBalance(userId),
			sumConfirmedMileage(findConvertibleRounds(userId)),
			OffsetDateTime.now(clock)
		);
	}

	public ConvertibleMileageResponse getConvertibleMileage(Long userId) {
		findPocket(userId);
		List<ConvertibleMileageResponse.Round> rounds = findConvertibleRounds(userId);
		long amount = sumConfirmedMileage(rounds);
		boolean dailyLimit = amount > 0 && hasConversionToday(userId);
		BlockReason blockReason = amount == 0 ? BlockReason.NO_MILEAGE : dailyLimit ? BlockReason.DAILY_LIMIT : null;

		return new ConvertibleMileageResponse(
			amount,
			rounds,
			amount > 0 && !dailyLimit,
			blockReason
		);
	}

	public PocketTransactionListResponse getTransactions(
		Long userId,
		TransactionDirection direction,
		TransactionType type,
		int page,
		int size
	) {
		findPocket(userId);
		Page<PocketTransaction> transactions = pocketTransactionRepository.findTransactions(
			userId,
			direction,
			type,
			PageRequest.of(page, size)
		);
		Map<String, List<PocketTransaction>> transactionsByMonth = transactions.getContent().stream()
			.collect(Collectors.groupingBy(
				this::yearMonth,
				LinkedHashMap::new,
				Collectors.toList()
			));
		List<PocketTransactionListResponse.Group> groups = transactionsByMonth.entrySet().stream()
			.map(entry -> new PocketTransactionListResponse.Group(
				entry.getKey(),
				entry.getValue().stream().mapToLong(this::signedCompletedAmount).sum(),
				entry.getValue().stream().map(this::toTransactionItem).toList()
			))
			.toList();

		return new PocketTransactionListResponse(
			sumCompleted(userId, TransactionDirection.CREDIT),
			calculateBalance(userId),
			sumConfirmedMileage(findConvertibleRounds(userId)),
			groups,
			transactions.getNumber(),
			transactions.getSize(),
			transactions.getTotalElements(),
			transactions.getTotalPages(),
			transactions.hasNext()
		);
	}

	public PocketManagementResponse getManagement(Long userId) {
		UserPocketSnapshot pocket = findPocket(userId);
		long balance = calculateBalance(userId);
		List<PocketManagementResponse.Account> accounts = withdrawalAccountService.findAccounts(userId)
			.accounts()
			.stream()
			.map(account -> new PocketManagementResponse.Account(
				account.accountId(),
				account.bankName(),
				account.accountNo(),
				account.isDefault()
			))
			.toList();
		List<PocketManagementResponse.RecentWithdrawal> recentWithdrawals = pocketTransactionRepository
			.findTop3ByUserIdAndTransactionTypeOrderByRequestedAtDescIdDesc(userId, TransactionType.WITHDRAWAL)
			.stream()
			.map(transaction -> new PocketManagementResponse.RecentWithdrawal(
				transaction.getId(),
				transaction.getAmount(),
				transaction.getTransactionStatus(),
				toOffsetDateTime(transaction.getRequestedAt())
			))
			.toList();

		return new PocketManagementResponse(
			new PocketManagementResponse.Pocket(pocket.accountNo(), pocket.holder(), balance),
			accounts,
			recentWithdrawals
		);
	}

	private UserPocketSnapshot findPocket(Long userId) {
		return userPocketQueryService.findPocket(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
	}

	private List<ConvertibleMileageResponse.Round> findConvertibleRounds(Long userId) {
		Set<String> blockingSourceKeys = Set.copyOf(pocketTransactionRepository.findBlockingSourceKeys(
			userId,
			TransactionSourceType.ECO_ROUND,
			TransactionStatus.FAILED
		));
		return ecoMileageQueryService.findConfirmedMileageRounds(userId).stream()
			.filter(round -> !blockingSourceKeys.contains(round.roundId().toString()))
			.map(this::toConvertibleRound)
			.toList();
	}

	private ConvertibleMileageResponse.Round toConvertibleRound(ConfirmedMileageRoundSnapshot round) {
		return new ConvertibleMileageResponse.Round(
			round.roundId(),
			YearMonth.from(round.periodStart()).toString(),
			YearMonth.from(round.periodEnd()).toString(),
			round.confirmedMileage()
		);
	}

	private PocketMainResponse.ConvertibleSource toConvertibleSource(ConvertibleMileageResponse.Round round) {
		return new PocketMainResponse.ConvertibleSource(
			round.roundId(),
			round.periodStart(),
			round.periodEnd()
		);
	}

	private PocketMainResponse.DefaultAccount toDefaultAccount(WithdrawalAccountResponse account) {
		return new PocketMainResponse.DefaultAccount(
			account.accountId(),
			account.bankCode(),
			account.bankName(),
			account.accountNo(),
			account.holder(),
			account.isDefault()
		);
	}

	private PocketTransactionItemResponse toTransactionItem(PocketTransaction transaction) {
		return new PocketTransactionItemResponse(
			transaction.getId(),
			transaction.getTransactionCode(),
			transaction.getLabel(),
			transaction.getDirection(),
			transaction.getTransactionType(),
			transaction.getAmount(),
			transaction.getTransactionStatus(),
			toOffsetDateTime(transaction.getCompletedAt()),
			sourceLabel(transaction.getTransactionType())
		);
	}

	private String sourceLabel(TransactionType type) {
		return switch (type) {
			case ECO_MILEAGE -> "전환 신청 후 입금";
			case GREENLIFE -> "자동 입금";
			case WITHDRAWAL -> "출금 신청";
		};
	}

	private String yearMonth(PocketTransaction transaction) {
		LocalDateTime dateTime = transaction.getCompletedAt() == null
			? transaction.getRequestedAt()
			: transaction.getCompletedAt();
		return YearMonth.from(dateTime).toString();
	}

	private long signedCompletedAmount(PocketTransaction transaction) {
		if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED) {
			return 0L;
		}
		return transaction.getDirection() == TransactionDirection.CREDIT
			? transaction.getAmount()
			: -transaction.getAmount();
	}

	private long calculateBalance(Long userId) {
		return sumCompleted(userId, TransactionDirection.CREDIT)
			- sumCompleted(userId, TransactionDirection.DEBIT);
	}

	private long sumCompleted(Long userId, TransactionDirection direction) {
		return pocketTransactionRepository.sumAmount(userId, TransactionStatus.COMPLETED, direction);
	}

	private long sumCompletedCredit(Long userId, TransactionType type) {
		return pocketTransactionRepository.sumAmountByType(
			userId,
			TransactionStatus.COMPLETED,
			TransactionDirection.CREDIT,
			type
		);
	}

	private long sumConfirmedMileage(List<ConvertibleMileageResponse.Round> rounds) {
		return rounds.stream().mapToLong(ConvertibleMileageResponse.Round::confirmedMileage).sum();
	}

	private boolean hasConversionToday(Long userId) {
		LocalDate today = LocalDate.now(clock);
		return pocketTransactionRepository
			.existsByUserIdAndTransactionTypeAndRequestedAtGreaterThanEqualAndRequestedAtLessThan(
				userId,
				TransactionType.ECO_MILEAGE,
				today.atStartOfDay(),
				today.plusDays(1).atStartOfDay()
			);
	}

	private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(KOREA_ZONE_ID).toOffsetDateTime();
	}
}
