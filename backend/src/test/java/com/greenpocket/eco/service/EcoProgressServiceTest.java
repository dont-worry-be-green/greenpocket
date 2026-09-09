package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.eco.dto.EcoHomeResponse;
import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.entity.WhatIfScreen;
import com.greenpocket.eco.repository.EcoProgressRepository;
import com.greenpocket.eco.repository.EcoProgressRepository.MissionProgressSnapshot;
import com.greenpocket.eco.repository.EcoProgressRepository.MonthlyUtilitySnapshot;
import com.greenpocket.eco.repository.EcoProgressRepository.ProgressRoundSnapshot;
import com.greenpocket.eco.repository.EcoProgressRepository.ResultRoundSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.type.UtilityType;

class EcoProgressServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long ROUND_ID = 7L;
	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

	private EcoProgressRepository repository;
	private EcoProgressService service;

	@BeforeEach
	void setUp() {
		repository = mock(EcoProgressRepository.class);
		Clock clock = Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), KOREA_ZONE);
		service = new EcoProgressService(repository, clock);
	}

	@Test
	void routesUnlinkedAndLinkingUsersWithoutRoundLookup() {
		when(repository.findLinkStatus(USER_ID))
			.thenReturn(Optional.of(EcoLinkStatus.UNLINKED))
			.thenReturn(Optional.of(EcoLinkStatus.LINKING));

		EcoHomeResponse unlinked = service.getHome(USER_ID);
		EcoHomeResponse linking = service.getHome(USER_ID);

		assertThat(unlinked.screen()).isEqualTo(WhatIfScreen.WF_01_UNLINKED);
		assertThat(linking.screen()).isEqualTo(WhatIfScreen.WF_02_LINKING);
		assertThat(unlinked.roundId()).isNull();
		assertThat(unlinked.links().benefitTab()).isTrue();
	}

	@Test
	void returnsHomeProgressFromTheSameMonthlyCalculationAsTheReport() {
		stubLinkedRound();
		when(repository.findLatestBillMonth(USER_ID, date(4), date(9))).thenReturn(Optional.of(date(7)));
		when(repository.findMonthlyUtilities(USER_ID, ROUND_ID, date(4), date(7)))
			.thenReturn(monthlyRowsThroughJuly());
		when(repository.findMissionProgress(USER_ID, ROUND_ID, LocalDate.of(2026, 8, 15), "SUMMER"))
			.thenReturn(new MissionProgressSnapshot(2, 3));

		EcoHomeResponse response = service.getHome(USER_ID);

		assertThat(response.screen()).isEqualTo(WhatIfScreen.WF_06_IN_PROGRESS);
		assertThat(response.header().remainingMonths()).isEqualTo(2);
		assertThat(response.header().remainingLabelMonths()).containsExactly(8, 9);
		assertThat(response.progress().cumulativeRate()).isEqualByComparingTo("6.000");
		assertThat(response.progress().coveredMonths()).containsExactly("2026-04", "2026-07");
		assertThat(response.progress().currentTier()).isEqualTo(TargetTier.TIER_5);
		assertThat(response.progress().targetTier()).isEqualTo(TargetTier.TIER_10);
		assertThat(response.progress().gapToNextTierPoint()).isEqualByComparingTo("4.000");
		assertThat(response.latestReport().monthlyRate()).isEqualByComparingTo("2.000");
		assertThat(response.latestReport().achieved()).isFalse();
		assertThat(response.todayMissions().completedCount()).isEqualTo(2);
		assertThat(response.application().showBanner()).isTrue();
	}

	@Test
	void routesLinkedUserWithoutGoalToGoalSetupScreen() {
		ProgressRoundSnapshot roundWithoutGoal = new ProgressRoundSnapshot(
			ROUND_ID,
			date(4),
			date(9),
			ApplicationStatus.NOT_APPLIED,
			null,
			null,
			0L,
			null,
			0L,
			null
		);
		when(repository.findLinkStatus(USER_ID)).thenReturn(Optional.of(EcoLinkStatus.LINKED));
		when(repository.findCurrentRound(USER_ID)).thenReturn(Optional.of(roundWithoutGoal));
		when(repository.findUnviewedResult(USER_ID)).thenReturn(Optional.empty());
		when(repository.findLatestBillMonth(USER_ID, date(4), date(9))).thenReturn(Optional.empty());

		EcoHomeResponse response = service.getHome(USER_ID);

		assertThat(response.screen()).isEqualTo(WhatIfScreen.WF_03_NO_GOAL);
		assertThat(response.progress()).isNull();
		assertThat(response.goal().goalSet()).isFalse();
		assertThat(response.latestReport().available()).isFalse();
	}

	@Test
	void prioritizesAnUnviewedConfirmedResultScreen() {
		stubLinkedRound();
		when(repository.findLatestBillMonth(USER_ID, date(4), date(9))).thenReturn(Optional.empty());
		when(repository.findMissionProgress(USER_ID, ROUND_ID, LocalDate.of(2026, 8, 15), "SUMMER"))
			.thenReturn(new MissionProgressSnapshot(0, 0));
		when(repository.findUnviewedResult(USER_ID)).thenReturn(Optional.of(new ResultRoundSnapshot(
			6L,
			LocalDate.of(2025, 10, 1),
			LocalDate.of(2026, 3, 1),
			new BigDecimal("12.000"),
			30_000L,
			LocalDateTime.of(2026, 6, 5, 0, 0)
		)));

		EcoHomeResponse response = service.getHome(USER_ID);

		assertThat(response.screen()).isEqualTo(WhatIfScreen.WF_09_RESULT_READY);
		assertThat(response.resultModal().roundId()).isEqualTo(6L);
		assertThat(response.resultModal().tier()).isEqualTo(TargetTier.TIER_10);
		assertThat(response.resultModal().mileage()).isEqualTo(30_000L);
	}

	@Test
	void returnsNoBillAsSuccessfulEmptyReport() {
		when(repository.findLinkStatus(USER_ID)).thenReturn(Optional.of(EcoLinkStatus.LINKED));
		when(repository.findCurrentRound(USER_ID)).thenReturn(Optional.of(round()));
		when(repository.findLatestBillMonth(USER_ID, date(4), date(9))).thenReturn(Optional.empty());

		EcoMonthlyReportResponse response = service.getMonthlyReport(USER_ID, null);

		assertThat(response.result()).isNull();
		assertThat(response.emptyReason()).isEqualTo("NO_BILL");
		assertThat(response.monthlyRates()).isEmpty();
	}

	@Test
	void calculatesMonthlyCumulativeCauseAndPrescription() {
		when(repository.findLinkStatus(USER_ID)).thenReturn(Optional.of(EcoLinkStatus.LINKED));
		when(repository.findCurrentRound(USER_ID)).thenReturn(Optional.of(round()));
		when(repository.findRoundForMonth(USER_ID, date(7))).thenReturn(Optional.of(round()));
		when(repository.findMonthlyUtilities(USER_ID, ROUND_ID, date(4), date(7)))
			.thenReturn(monthlyRowsThroughJuly());
		when(repository.findSelectedMissionRate(USER_ID, ROUND_ID)).thenReturn(new BigDecimal("25.000"));

		EcoMonthlyReportResponse response = service.getMonthlyReport(USER_ID, "2026-07");

		assertThat(response.result().monthlyRate()).isEqualByComparingTo("2.000");
		assertThat(response.result().cumulativeRate()).isEqualByComparingTo("6.000");
		assertThat(response.result().achieved()).isFalse();
		assertThat(response.cause().largestCarbonUtility()).isEqualTo(UtilityType.ELECTRICITY);
		assertThat(response.cause().byUtility()).hasSize(2);
		assertThat(response.cause().byUtility().getFirst().rate()).isEqualByComparingTo("-10.000");
		assertThat(response.cause().byUtility().getFirst().expanded()).isTrue();
		assertThat(response.prescription().remainingMonths()).isEqualTo(2);
		assertThat(response.prescription().requiredRate()).isEqualByComparingTo("24.000");
		assertThat(response.prescription().achievable()).isTrue();
		assertThat(response.prescription().requiredByUtility().getFirst().requiredRate())
			.isEqualByComparingTo("17.500");
		assertThat(response.monthlyRates())
			.extracting(EcoMonthlyReportResponse.MonthlyRate::yearMonth)
			.containsExactly("2026-04", "2026-07");
		assertThat(response.emptyReason()).isNull();
	}

	@Test
	void omitsRequiredRateWhenReportIsForFinalMonth() {
		when(repository.findLinkStatus(USER_ID)).thenReturn(Optional.of(EcoLinkStatus.LINKED));
		when(repository.findCurrentRound(USER_ID)).thenReturn(Optional.of(round()));
		when(repository.findRoundForMonth(USER_ID, date(9))).thenReturn(Optional.of(round()));
		when(repository.findMonthlyUtilities(USER_ID, ROUND_ID, date(4), date(9)))
			.thenReturn(List.of(row(9, UtilityType.ELECTRICITY, "100", "110", "10")));
		when(repository.findSelectedMissionRate(USER_ID, ROUND_ID)).thenReturn(BigDecimal.ZERO);

		EcoMonthlyReportResponse response = service.getMonthlyReport(USER_ID, "2026-09");

		assertThat(response.result().monthlyRate()).isEqualByComparingTo("-10.000");
		assertThat(response.prescription().remainingMonths()).isZero();
		assertThat(response.prescription().requiredRate()).isNull();
		assertThat(response.prescription().requiredByUtility()).isEmpty();
	}

	@Test
	void rejectsInvalidMonthFormatBeforeQueryingARound() {
		when(repository.findLinkStatus(USER_ID)).thenReturn(Optional.of(EcoLinkStatus.LINKED));
		when(repository.findCurrentRound(USER_ID)).thenReturn(Optional.of(round()));

		assertThatThrownBy(() -> service.getMonthlyReport(USER_ID, "2026-7"))
			.isInstanceOf(BusinessException.class)
			.satisfies(error -> {
				BusinessException businessException = (BusinessException)error;
				assertThat(businessException.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
				assertThat(businessException.getField()).isEqualTo("month");
			});
		verify(repository).findCurrentRound(USER_ID);
	}

	private void stubLinkedRound() {
		when(repository.findLinkStatus(USER_ID)).thenReturn(Optional.of(EcoLinkStatus.LINKED));
		when(repository.findCurrentRound(USER_ID)).thenReturn(Optional.of(round()));
		when(repository.findUnviewedResult(USER_ID)).thenReturn(Optional.empty());
	}

	private ProgressRoundSnapshot round() {
		return new ProgressRoundSnapshot(
			ROUND_ID,
			date(4),
			date(9),
			ApplicationStatus.NOT_APPLIED,
			LocalDateTime.of(2026, 4, 1, 9, 0),
			new BigDecimal("10.000"),
			30_000L,
			null,
			0L,
			null
		);
	}

	private List<MonthlyUtilitySnapshot> monthlyRowsThroughJuly() {
		return List.of(
			row(4, UtilityType.ELECTRICITY, "100", "90", "10"),
			row(7, UtilityType.ELECTRICITY, "80", "88", "10"),
			row(7, UtilityType.GAS, "20", "10", "15")
		);
	}

	private MonthlyUtilitySnapshot row(
		int month,
		UtilityType utilityType,
		String baselineUsage,
		String actualUsage,
		String targetRate
	) {
		return new MonthlyUtilitySnapshot(
			date(month),
			utilityType,
			new BigDecimal(baselineUsage),
			new BigDecimal(actualUsage),
			utilityType == UtilityType.ELECTRICITY ? UsageUnit.kWh : UsageUnit.m3,
			BigDecimal.ONE,
			new BigDecimal(targetRate),
			LocalDateTime.of(2026, Math.min(month + 1, 12), 3, 0, 0)
		);
	}

	private LocalDate date(int month) {
		return LocalDate.of(2026, month, 1);
	}
}
