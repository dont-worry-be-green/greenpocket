package com.greenpocket.eco.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.repository.EcoProgressRepository;
import com.greenpocket.eco.repository.EcoProgressRepository.ProgressRoundSnapshot;

class EcoMonthlyReportRefreshServiceTest {

	private static final Long USER_ID = 42L;
	private static final Long ROUND_ID = 7L;
	private static final YearMonth REPORT_MONTH = YearMonth.of(2026, 8);

	private EcoProgressRepository ecoProgressRepository;
	private EcoProgressService ecoProgressService;
	private EcoMonthlyReportRefreshService service;

	@BeforeEach
	void setUp() {
		ecoProgressRepository = mock(EcoProgressRepository.class);
		ecoProgressService = mock(EcoProgressService.class);
		service = new EcoMonthlyReportRefreshService(
			ecoProgressRepository,
			ecoProgressService,
			new ObjectMapper()
		);
	}

	@Test
	void returnsNotUpdatedWhenMonthIsOutsideEvaluationRound() {
		when(ecoProgressRepository.findRoundForMonth(USER_ID, REPORT_MONTH.atDay(1)))
			.thenReturn(java.util.Optional.empty());

		var result = service.refresh(USER_ID, REPORT_MONTH);

		assertThat(result.updated()).isFalse();
		assertThat(result.roundId()).isNull();
		verify(ecoProgressRepository).deleteMonthlyReport(USER_ID, REPORT_MONTH.atDay(1));
		verify(ecoProgressService, never()).getMonthlyReport(USER_ID, REPORT_MONTH.toString());
	}

	@Test
	void returnsNotUpdatedWhenBaselineCalculationIsUnavailable() {
		when(ecoProgressRepository.findRoundForMonth(USER_ID, REPORT_MONTH.atDay(1)))
			.thenReturn(java.util.Optional.of(round()));
		when(ecoProgressService.getMonthlyReport(USER_ID, REPORT_MONTH.toString()))
			.thenReturn(new EcoMonthlyReportResponse(
				"2026-08", ROUND_ID, null, null, null, null, null, List.of(), "NO_BILL"
			));

		var result = service.refresh(USER_ID, REPORT_MONTH);

		assertThat(result.updated()).isFalse();
		assertThat(result.roundId()).isEqualTo(ROUND_ID);
		verify(ecoProgressRepository).deleteMonthlyReport(USER_ID, REPORT_MONTH.atDay(1));
	}

	@Test
	void upsertsCalculatedMonthlyReport() {
		when(ecoProgressRepository.findRoundForMonth(USER_ID, REPORT_MONTH.atDay(1)))
			.thenReturn(java.util.Optional.of(round()));
		when(ecoProgressService.getMonthlyReport(USER_ID, REPORT_MONTH.toString()))
			.thenReturn(report());

		var result = service.refresh(USER_ID, REPORT_MONTH);

		assertThat(result.updated()).isTrue();
		assertThat(result.roundId()).isEqualTo(ROUND_ID);
		verify(ecoProgressRepository).upsertMonthlyReport(
			USER_ID,
			ROUND_ID,
			REPORT_MONTH.atDay(1),
			new BigDecimal("1.039"),
			new BigDecimal("9.000"),
			new BigDecimal("10.000"),
			new BigDecimal("12.000"),
			1,
			false,
			"[]"
		);
	}

	private ProgressRoundSnapshot round() {
		return new ProgressRoundSnapshot(
			ROUND_ID,
			LocalDate.of(2026, 4, 1),
			LocalDate.of(2026, 9, 1),
			ApplicationStatus.APPLIED,
			LocalDate.of(2026, 4, 2).atStartOfDay(),
			new BigDecimal("10.000"),
			30_000L,
			null,
			null,
			null
		);
	}

	private EcoMonthlyReportResponse report() {
		return new EcoMonthlyReportResponse(
			"2026-08",
			ROUND_ID,
			null,
			"2024·2025년 8월 평균",
			new EcoMonthlyReportResponse.Result(
				new BigDecimal("1.039"),
				new BigDecimal("10.000"),
				false,
				new BigDecimal("9.000"),
				List.of("2026-04", "2026-05", "2026-06", "2026-07", "2026-08")
			),
			new EcoMonthlyReportResponse.Cause(List.of(), null, List.of()),
			new EcoMonthlyReportResponse.Prescription(
				1, List.of(9), new BigDecimal("12.000"), false, List.of(), BigDecimal.ZERO, null
			),
			List.of(),
			null
		);
	}
}
