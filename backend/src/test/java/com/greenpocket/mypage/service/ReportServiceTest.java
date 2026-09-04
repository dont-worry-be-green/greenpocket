package com.greenpocket.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.bill.service.BillReportQueryService;
import com.greenpocket.bill.service.BillReportQueryService.MonthlyDiagnosisReport;
import com.greenpocket.eco.service.EcoReportQueryService;
import com.greenpocket.eco.service.EcoReportQueryService.EcoMonthlyReport;
import com.greenpocket.eco.service.EcoReportQueryService.EcoResultReport;
import com.greenpocket.mypage.dto.ReportListResponse;
import com.greenpocket.mypage.dto.ReportType;

class ReportServiceTest {

	private static final Long USER_ID = 1L;

	private BillReportQueryService billReportQueryService;
	private EcoReportQueryService ecoReportQueryService;
	private ReportService reportService;

	@BeforeEach
	void setUp() {
		billReportQueryService = mock(BillReportQueryService.class);
		ecoReportQueryService = mock(EcoReportQueryService.class);
		reportService = new ReportService(billReportQueryService, ecoReportQueryService);
	}

	@Test
	void combinesAllReportTypesInLatestMonthOrder() {
		stubReports();

		ReportListResponse response = reportService.getReports(USER_ID, null, 2026, 0, 20);

		assertThat(response.content()).extracting(ReportListResponse.Item::reportId)
			.containsExactly(
				"MONTHLY_DIAGNOSIS:2026-08",
				"ECO_MONTHLY:2026-07",
				"ECO_RESULT:2"
			);
		assertThat(response.content().get(0).targetParams()).containsEntry("month", "2026-08");
		assertThat(response.content().get(2).targetParams()).containsEntry("roundId", 2L);
		assertThat(response.content()).allMatch(item -> !item.downloadable());
		assertThat(response.totalElements()).isEqualTo(3L);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	void filtersTypeAndPaginates() {
		stubReports();

		ReportListResponse response = reportService.getReports(
			USER_ID,
			ReportType.MONTHLY_DIAGNOSIS,
			null,
			1,
			1
		);

		assertThat(response.content()).isEmpty();
		assertThat(response.totalElements()).isEqualTo(1L);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.hasNext()).isFalse();
	}

	private void stubReports() {
		when(billReportQueryService.findMonthlyDiagnosisReports(USER_ID)).thenReturn(List.of(
			new MonthlyDiagnosisReport(
				YearMonth.of(2026, 8),
				LocalDateTime.of(2026, 9, 1, 10, 22)
			)
		));
		when(ecoReportQueryService.findMonthlyReports(USER_ID)).thenReturn(List.of(
			new EcoMonthlyReport(
				YearMonth.of(2026, 7),
				LocalDateTime.of(2026, 8, 3, 0, 0)
			)
		));
		when(ecoReportQueryService.findResultReports(USER_ID)).thenReturn(List.of(
			new EcoResultReport(
				2L,
				YearMonth.of(2025, 10),
				YearMonth.of(2026, 3),
				LocalDateTime.of(2026, 6, 5, 0, 0)
			)
		));
	}
}
