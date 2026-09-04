package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoResultResponse;
import com.greenpocket.eco.dto.EcoSettlementResponse;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.exception.EcoErrorCode;
import com.greenpocket.eco.repository.EcoResultRepository;
import com.greenpocket.eco.repository.EcoResultRepository.MonthlyRateSnapshot;
import com.greenpocket.eco.repository.EcoResultRepository.NextRoundSnapshot;
import com.greenpocket.eco.repository.EcoResultRepository.ResultRoundSnapshot;
import com.greenpocket.eco.repository.EcoResultRepository.UtilityResultSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse;
import com.greenpocket.pocket.service.PocketQueryService;

class EcoResultServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long ROUND_ID = 7L;

	private EcoResultRepository ecoResultRepository;
	private PocketQueryService pocketQueryService;
	private EcoResultService ecoResultService;

	@BeforeEach
	void setUp() {
		ecoResultRepository = mock(EcoResultRepository.class);
		pocketQueryService = mock(PocketQueryService.class);
		ecoResultService = new EcoResultService(ecoResultRepository, pocketQueryService);
	}

	@Test
	void returnsConfirmedResultWithUtilityMonthlyAndNextRoundData() {
		when(ecoResultRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.of(confirmedRound()));
		when(ecoResultRepository.findUtilityResults(ROUND_ID)).thenReturn(List.of(new UtilityResultSnapshot(
			UtilityType.ELECTRICITY,
			new BigDecimal("1340.000"),
			new BigDecimal("1166.000"),
			UsageUnit.kWh,
			TargetTier.TIER_10,
			new BigDecimal("10.000"),
			new BigDecimal("13.000"),
			true
		)));
		when(ecoResultRepository.findMonthlyRates(USER_ID, ROUND_ID)).thenReturn(List.of(
			new MonthlyRateSnapshot(LocalDate.of(2026, 9, 1), new BigDecimal("17.000"), true)
		));
		when(ecoResultRepository.findNextRound(USER_ID, LocalDate.of(2026, 9, 1))).thenReturn(Optional.of(
			new NextRoundSnapshot(8L, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 1), false)
		));
		when(pocketQueryService.getConvertibleMileage(USER_ID)).thenReturn(convertibleMileage(List.of(
			new ConvertibleMileageResponse.Round(ROUND_ID, "2026-04", "2026-09", 30_000L)
		)));

		EcoResultResponse response = ecoResultService.getResult(USER_ID, ROUND_ID);

		assertThat(response.roundId()).isEqualTo(ROUND_ID);
		assertThat(response.confirmedAt()).isEqualTo("2026-12-05T00:00+09:00");
		assertThat(response.finalRate()).isEqualByComparingTo("12.499");
		assertThat(response.achieved()).isTrue();
		assertThat(response.tier()).isEqualTo(TargetTier.TIER_10);
		assertThat(response.tierLabel()).isEqualTo("10~15% 구간");
		assertThat(response.amount().savedIsPocketEligible()).isFalse();
		assertThat(response.utilityResults()).hasSize(1);
		assertThat(response.monthlyRates().getFirst().yearMonth()).isEqualTo("2026-09");
		assertThat(response.mileageConverted()).isFalse();
		assertThat(response.nextRound().roundId()).isEqualTo(8L);
	}

	@Test
	void marksMileageConvertedWhenConfirmedRoundIsNotConvertible() {
		when(ecoResultRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.of(confirmedRound()));
		when(ecoResultRepository.findUtilityResults(ROUND_ID)).thenReturn(List.of());
		when(ecoResultRepository.findMonthlyRates(USER_ID, ROUND_ID)).thenReturn(List.of());
		when(ecoResultRepository.findNextRound(USER_ID, LocalDate.of(2026, 9, 1))).thenReturn(Optional.empty());
		when(pocketQueryService.getConvertibleMileage(USER_ID)).thenReturn(convertibleMileage(List.of()));

		EcoResultResponse response = ecoResultService.getResult(USER_ID, ROUND_ID);

		assertThat(response.mileageConverted()).isTrue();
		assertThat(response.nextRound()).isNull();
	}

	@Test
	void returnsSettlementWithConvertibleConfirmedMileage() {
		when(ecoResultRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.of(confirmedRound()));
		when(pocketQueryService.getConvertibleMileage(USER_ID)).thenReturn(convertibleMileage(List.of(
			new ConvertibleMileageResponse.Round(ROUND_ID, "2026-04", "2026-09", 30_000L)
		)));

		EcoSettlementResponse response = ecoResultService.getSettlement(USER_ID, ROUND_ID);

		assertThat(response.confirmedMileage()).isEqualTo(30_000L);
		assertThat(response.statusLabel()).isEqualTo("확인");
		assertThat(response.cumulativeRate()).isEqualByComparingTo("12.499");
		assertThat(response.tier()).isEqualTo(TargetTier.TIER_10);
		assertThat(response.calculation().baselineAmount()).isEqualTo(420_600L);
		assertThat(response.calculation().note())
			.isEqualTo("전기·도시가스·수도를 직전 2년 같은 기간(4~9월) 평균과 비교했어요");
		assertThat(response.isCash()).isFalse();
		assertThat(response.convertible()).isTrue();
		assertThat(response.otherUses()).containsExactly("서울시 세금", "상품권", "관리비 납부");
	}

	@Test
	void blocksSettlementConversionWhenDailyLimitApplies() {
		when(ecoResultRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.of(confirmedRound()));
		when(pocketQueryService.getConvertibleMileage(USER_ID)).thenReturn(new ConvertibleMileageResponse(
			30_000L,
			List.of(new ConvertibleMileageResponse.Round(ROUND_ID, "2026-04", "2026-09", 30_000L)),
			false,
			ConvertibleMileageResponse.BlockReason.DAILY_LIMIT
		));

		EcoSettlementResponse response = ecoResultService.getSettlement(USER_ID, ROUND_ID);

		assertThat(response.convertible()).isFalse();
	}

	@Test
	void rejectsRoundBeforeResultConfirmation() {
		ResultRoundSnapshot unconfirmed = new ResultRoundSnapshot(
			ROUND_ID,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			RoundStatus.IN_PROGRESS,
			new BigDecimal("10.000"),
			null,
			420_600L,
			null,
			null,
			null,
			null
		);
		when(ecoResultRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.of(unconfirmed));

		assertThatThrownBy(() -> ecoResultService.getResult(USER_ID, ROUND_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_RESULT_NOT_CONFIRMED));
	}

	@Test
	void hidesOtherUsersRoundAsNotFound() {
		when(ecoResultRepository.findRound(USER_ID, ROUND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> ecoResultService.getResult(USER_ID, ROUND_ID))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> assertThat(((BusinessException)error).getErrorCode())
				.isEqualTo(EcoErrorCode.ECO_ROUND_NOT_FOUND));
	}

	private ResultRoundSnapshot confirmedRound() {
		return new ResultRoundSnapshot(
			ROUND_ID,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			RoundStatus.CONFIRMED,
			new BigDecimal("10.000"),
			new BigDecimal("12.499"),
			420_600L,
			370_100L,
			50_500L,
			30_000L,
			LocalDateTime.of(2026, 12, 5, 0, 0)
		);
	}

	private ConvertibleMileageResponse convertibleMileage(List<ConvertibleMileageResponse.Round> rounds) {
		return new ConvertibleMileageResponse(
			rounds.stream().mapToLong(ConvertibleMileageResponse.Round::confirmedMileage).sum(),
			rounds,
			!rounds.isEmpty(),
			rounds.isEmpty() ? ConvertibleMileageResponse.BlockReason.NO_MILEAGE : null
		);
	}
}
