package com.greenpocket.pocket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.greenpocket.eco.repository.EcoMileageQueryRepository.ConfirmedMileageRoundSnapshot;
import com.greenpocket.eco.service.EcoMileageQueryService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse.BlockReason;
import com.greenpocket.pocket.dto.PocketMainResponse;
import com.greenpocket.pocket.dto.PocketManagementResponse;
import com.greenpocket.pocket.dto.PocketTransactionListResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountListResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountResponse;
import com.greenpocket.pocket.entity.PocketTransaction;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionSourceType;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.repository.PocketTransactionRepository;
import com.greenpocket.user.repository.UserPocketQueryRepository.UserPocketSnapshot;
import com.greenpocket.user.service.UserPocketQueryService;

class PocketQueryServiceTest {

	private static final Long USER_ID = 42L;
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private PocketTransactionRepository pocketTransactionRepository;
	private WithdrawalAccountService withdrawalAccountService;
	private UserPocketQueryService userPocketQueryService;
	private EcoMileageQueryService ecoMileageQueryService;
	private PocketQueryService pocketQueryService;

	@BeforeEach
	void setUp() {
		pocketTransactionRepository = mock(PocketTransactionRepository.class);
		withdrawalAccountService = mock(WithdrawalAccountService.class);
		userPocketQueryService = mock(UserPocketQueryService.class);
		ecoMileageQueryService = mock(EcoMileageQueryService.class);
		Clock clock = Clock.fixed(Instant.parse("2026-09-04T01:00:00Z"), KOREA_ZONE_ID);
		pocketQueryService = new PocketQueryService(
			pocketTransactionRepository,
			withdrawalAccountService,
			userPocketQueryService,
			ecoMileageQueryService,
			clock
		);

		when(userPocketQueryService.findPocket(USER_ID))
			.thenReturn(Optional.of(new UserPocketSnapshot("1005-1234-5678-90", "김수현")));
		when(ecoMileageQueryService.findConfirmedMileageRounds(USER_ID)).thenReturn(List.of(round(7L, 30_000L)));
		when(pocketTransactionRepository.findBlockingSourceKeys(
			USER_ID,
			TransactionSourceType.ECO_ROUND,
			TransactionStatus.FAILED
		)).thenReturn(List.of());
		when(withdrawalAccountService.findAccounts(USER_ID))
			.thenReturn(new WithdrawalAccountListResponse(List.of(account())));
		when(pocketTransactionRepository.sumAmount(USER_ID, TransactionStatus.COMPLETED, TransactionDirection.CREDIT))
			.thenReturn(64_000L);
		when(pocketTransactionRepository.sumAmount(USER_ID, TransactionStatus.COMPLETED, TransactionDirection.DEBIT))
			.thenReturn(30_000L);
		when(pocketTransactionRepository.sumAmountByType(
			USER_ID, TransactionStatus.COMPLETED, TransactionDirection.CREDIT, TransactionType.ECO_MILEAGE
		)).thenReturn(40_000L);
		when(pocketTransactionRepository.sumAmountByType(
			USER_ID, TransactionStatus.COMPLETED, TransactionDirection.CREDIT, TransactionType.GREENLIFE
		)).thenReturn(24_000L);
	}

	@Test
	void returnsPocketMainWithLedgerBalanceAndRecentCredits() {
		PocketTransaction recentCredit = transaction(
			91L, TransactionDirection.CREDIT, TransactionType.GREENLIFE,
			TransactionStatus.COMPLETED, 3_200L, LocalDateTime.of(2026, 9, 4, 9, 0)
		);
		when(pocketTransactionRepository.findTop2ByUserIdAndDirectionAndTransactionStatusOrderByCompletedAtDescIdDesc(
			USER_ID, TransactionDirection.CREDIT, TransactionStatus.COMPLETED
		)).thenReturn(List.of(recentCredit));
		when(pocketTransactionRepository.existsByUserId(USER_ID)).thenReturn(true);

		PocketMainResponse response = pocketQueryService.getPocket(USER_ID);

		assertThat(response.pocket().accountNo()).isEqualTo("1005-1234-5678-90");
		assertThat(response.balance()).isEqualTo(34_000L);
		assertThat(response.breakdown().ecoMileage()).isEqualTo(40_000L);
		assertThat(response.breakdown().greenlife()).isEqualTo(24_000L);
		assertThat(response.convertibleMileage()).isEqualTo(30_000L);
		assertThat(response.convertibleSource().roundId()).isEqualTo(7L);
		assertThat(response.defaultAccount().accountId()).isEqualTo(3L);
		assertThat(response.recentTransactions()).singleElement()
			.satisfies(item -> assertThat(item.sourceLabel()).isEqualTo("자동 입금"));
		assertThat(response.empty().noAccount()).isFalse();
		assertThat(response.empty().noTransaction()).isFalse();
		assertThat(response.notices()).hasSize(3);
	}

	@Test
	void returnsEmptyPocketState() {
		when(withdrawalAccountService.findAccounts(USER_ID))
			.thenReturn(new WithdrawalAccountListResponse(List.of()));
		when(ecoMileageQueryService.findConfirmedMileageRounds(USER_ID)).thenReturn(List.of());
		when(pocketTransactionRepository.findTop2ByUserIdAndDirectionAndTransactionStatusOrderByCompletedAtDescIdDesc(
			USER_ID, TransactionDirection.CREDIT, TransactionStatus.COMPLETED
		)).thenReturn(List.of());
		when(pocketTransactionRepository.existsByUserId(USER_ID)).thenReturn(false);

		PocketMainResponse response = pocketQueryService.getPocket(USER_ID);

		assertThat(response.defaultAccount()).isNull();
		assertThat(response.convertibleSource()).isNull();
		assertThat(response.recentTransactions()).isEmpty();
		assertThat(response.empty().noAccount()).isTrue();
		assertThat(response.empty().noTransaction()).isTrue();
	}

	@Test
	void returnsBalanceAndCalculationTime() {
		var response = pocketQueryService.getBalance(USER_ID);

		assertThat(response.balance()).isEqualTo(34_000L);
		assertThat(response.convertibleMileage()).isEqualTo(30_000L);
		assertThat(response.calculatedAt().toString()).isEqualTo("2026-09-04T10:00+09:00");
	}

	@Test
	void excludesConvertedRoundAndBlocksAnotherConversionToday() {
		when(ecoMileageQueryService.findConfirmedMileageRounds(USER_ID))
			.thenReturn(List.of(round(8L, 50_000L), round(7L, 30_000L)));
		when(pocketTransactionRepository.findBlockingSourceKeys(
			USER_ID, TransactionSourceType.ECO_ROUND, TransactionStatus.FAILED
		)).thenReturn(List.of("7"));
		when(pocketTransactionRepository
			.existsByUserIdAndTransactionTypeAndRequestedAtGreaterThanEqualAndRequestedAtLessThan(
				eq(USER_ID), eq(TransactionType.ECO_MILEAGE), any(), any()
			)).thenReturn(true);

		ConvertibleMileageResponse response = pocketQueryService.getConvertibleMileage(USER_ID);

		assertThat(response.convertibleMileage()).isEqualTo(50_000L);
		assertThat(response.rounds()).extracting(ConvertibleMileageResponse.Round::roundId).containsExactly(8L);
		assertThat(response.convertible()).isFalse();
		assertThat(response.blockReason()).isEqualTo(BlockReason.DAILY_LIMIT);
	}

	@Test
	void reportsNoMileageWhenNoEligibleRoundExists() {
		when(ecoMileageQueryService.findConfirmedMileageRounds(USER_ID)).thenReturn(List.of());

		ConvertibleMileageResponse response = pocketQueryService.getConvertibleMileage(USER_ID);

		assertThat(response.convertibleMileage()).isZero();
		assertThat(response.rounds()).isEmpty();
		assertThat(response.convertible()).isFalse();
		assertThat(response.blockReason()).isEqualTo(BlockReason.NO_MILEAGE);
	}

	@Test
	void groupsFilteredTransactionsAndCalculatesSignedCompletedSubtotal() {
		PocketTransaction credit = transaction(
			91L, TransactionDirection.CREDIT, TransactionType.GREENLIFE,
			TransactionStatus.COMPLETED, 10_000L, LocalDateTime.of(2026, 9, 4, 9, 0)
		);
		PocketTransaction debit = transaction(
			92L, TransactionDirection.DEBIT, TransactionType.WITHDRAWAL,
			TransactionStatus.COMPLETED, 3_000L, LocalDateTime.of(2026, 9, 3, 9, 0)
		);
		PocketTransaction failed = transaction(
			93L, TransactionDirection.CREDIT, TransactionType.ECO_MILEAGE,
			TransactionStatus.FAILED, 30_000L, LocalDateTime.of(2026, 9, 2, 9, 0)
		);
		PageRequest pageable = PageRequest.of(0, 20);
		when(pocketTransactionRepository.findTransactions(USER_ID, null, null, pageable))
			.thenReturn(new PageImpl<>(List.of(credit, debit, failed), pageable, 3));

		PocketTransactionListResponse response = pocketQueryService.getTransactions(USER_ID, null, null, 0, 20);

		verify(pocketTransactionRepository).findTransactions(USER_ID, null, null, pageable);
		assertThat(response.totalCreditAmount()).isEqualTo(64_000L);
		assertThat(response.balance()).isEqualTo(34_000L);
		assertThat(response.groups()).singleElement().satisfies(group -> {
			assertThat(group.yearMonth()).isEqualTo("2026-09");
			assertThat(group.subtotal()).isEqualTo(7_000L);
			assertThat(group.items()).hasSize(3);
		});
		assertThat(response.totalElements()).isEqualTo(3);
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	void returnsPocketManagementWithAccountsAndRecentWithdrawals() {
		PocketTransaction withdrawal = transaction(
			130L, TransactionDirection.DEBIT, TransactionType.WITHDRAWAL,
			TransactionStatus.COMPLETED, 30_000L, LocalDateTime.of(2026, 9, 4, 10, 0)
		);
		when(pocketTransactionRepository.findTop3ByUserIdAndTransactionTypeOrderByRequestedAtDescIdDesc(
			USER_ID, TransactionType.WITHDRAWAL
		)).thenReturn(List.of(withdrawal));

		PocketManagementResponse response = pocketQueryService.getManagement(USER_ID);

		assertThat(response.pocket().balance()).isEqualTo(34_000L);
		assertThat(response.accounts()).singleElement()
			.satisfies(account -> assertThat(account.accountNo()).isEqualTo("110-123-456789"));
		assertThat(response.recentWithdrawals()).singleElement().satisfies(item -> {
			assertThat(item.transactionId()).isEqualTo(130L);
			assertThat(item.requestedAt().toString()).isEqualTo("2026-09-04T10:00+09:00");
		});
	}

	@Test
	void rejectsMissingAuthenticatedUserSnapshot() {
		when(userPocketQueryService.findPocket(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> pocketQueryService.getBalance(USER_ID))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHENTICATED_DEMO_KEY));
	}

	@Test
	void springCreatesPocketQueryServiceWithRuntimeConstructor() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.registerBean(PocketTransactionRepository.class, () -> mock(PocketTransactionRepository.class));
			context.registerBean(WithdrawalAccountService.class, () -> mock(WithdrawalAccountService.class));
			context.registerBean(UserPocketQueryService.class, () -> mock(UserPocketQueryService.class));
			context.registerBean(EcoMileageQueryService.class, () -> mock(EcoMileageQueryService.class));
			context.register(PocketQueryService.class);
			context.refresh();

			assertThat(context.getBean(PocketQueryService.class)).isNotNull();
		}
	}

	private ConfirmedMileageRoundSnapshot round(Long roundId, Long amount) {
		return new ConfirmedMileageRoundSnapshot(
			roundId,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			amount
		);
	}

	private WithdrawalAccountResponse account() {
		return new WithdrawalAccountResponse(
			3L, "088", "신한은행", "110-123-456789", "김수현", true, true, null
		);
	}

	private PocketTransaction transaction(
		Long id,
		TransactionDirection direction,
		TransactionType type,
		TransactionStatus status,
		Long amount,
		LocalDateTime dateTime
	) {
		PocketTransaction transaction = mock(PocketTransaction.class);
		when(transaction.getId()).thenReturn(id);
		when(transaction.getTransactionCode()).thenReturn("GP-2609-%04d".formatted(id));
		when(transaction.getLabel()).thenReturn("거래 " + id);
		when(transaction.getDirection()).thenReturn(direction);
		when(transaction.getTransactionType()).thenReturn(type);
		when(transaction.getTransactionStatus()).thenReturn(status);
		when(transaction.getAmount()).thenReturn(amount);
		when(transaction.getRequestedAt()).thenReturn(dateTime);
		when(transaction.getCompletedAt()).thenReturn(status == TransactionStatus.COMPLETED ? dateTime : null);
		return transaction;
	}
}
