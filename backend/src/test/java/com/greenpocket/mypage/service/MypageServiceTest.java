package com.greenpocket.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.greenpocket.bill.service.BillReportQueryService;
import com.greenpocket.bill.service.BillReportQueryService.MonthlyDiagnosisReport;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.service.EcoLinkService;
import com.greenpocket.eco.service.EcoReportQueryService;
import com.greenpocket.eco.service.EcoReportQueryService.EcoMonthlyReport;
import com.greenpocket.eco.service.EcoReportQueryService.EcoResultReport;
import com.greenpocket.global.type.UtilityType;
import com.greenpocket.mypage.dto.MypageResponse;
import com.greenpocket.user.repository.UserMypageQueryRepository.UserMypageSnapshot;
import com.greenpocket.user.service.UserMypageQueryService;

class MypageServiceTest {

	private static final Long USER_ID = 1L;

	private UserMypageQueryService userMypageQueryService;
	private BillReportQueryService billReportQueryService;
	private EcoReportQueryService ecoReportQueryService;
	private EcoLinkService ecoLinkService;
	private MypageService mypageService;

	@BeforeEach
	void setUp() {
		userMypageQueryService = mock(UserMypageQueryService.class);
		billReportQueryService = mock(BillReportQueryService.class);
		ecoReportQueryService = mock(EcoReportQueryService.class);
		ecoLinkService = mock(EcoLinkService.class);
		mypageService = new MypageService(
			userMypageQueryService,
			billReportQueryService,
			ecoReportQueryService,
			ecoLinkService
		);
	}

	@Test
	void returnsProfileIntegrationAndArchiveSummary() {
		when(userMypageQueryService.findMypageUser(USER_ID)).thenReturn(Optional.of(linkedUser()));
		when(billReportQueryService.countBills(USER_ID)).thenReturn(14L);
		when(billReportQueryService.findMonthlyDiagnosisReports(USER_ID)).thenReturn(List.of(
			new MonthlyDiagnosisReport(YearMonth.of(2026, 8), LocalDateTime.of(2026, 9, 1, 10, 22))
		));
		when(ecoReportQueryService.findMonthlyReports(USER_ID)).thenReturn(List.of(
			new EcoMonthlyReport(YearMonth.of(2026, 7), LocalDateTime.of(2026, 8, 3, 0, 0))
		));
		when(ecoReportQueryService.findResultReports(USER_ID)).thenReturn(List.of(
			new EcoResultReport(
				2L,
				YearMonth.of(2025, 10),
				YearMonth.of(2026, 3),
				LocalDateTime.of(2026, 6, 5, 0, 0)
			)
		));
		when(ecoLinkService.getStatus(USER_ID)).thenReturn(linkedEcoStatus());

		MypageResponse response = mypageService.getMypage(USER_ID);

		assertThat(response.profile().profileSummary()).isEqualTo("서울 관악구 · 원룸 · 10평 이하");
		assertThat(response.links().billArchive().count()).isEqualTo(14L);
		assertThat(response.links().reportArchive().count()).isEqualTo(3L);
		assertThat(response.ecoAddress().registeredAt()).isEqualTo("2026-03");
		assertThat(response.ecoAddress().matchesProfile()).isTrue();
		assertThat(response.ecoAddress().notice()).contains("이사했다면");
		assertThat(response.integration().registeredUtilities())
			.containsExactly(UtilityType.ELECTRICITY, UtilityType.GAS, UtilityType.WATER);
		assertThat(response.pocketAccountNo()).isEqualTo("1005-1234-5678-90");
	}

	@Test
	void returnsNullEcoAddressWhenNotLinked() {
		UserMypageSnapshot user = new UserMypageSnapshot(
			"김수현", "11", "서울특별시", "11620", "관악구", "ONE_ROOM", "UNDER_10",
			EcoLinkStatus.UNLINKED, null, null, null, null, null, false, null, "1005-1234-5678-90"
		);
		when(userMypageQueryService.findMypageUser(USER_ID)).thenReturn(Optional.of(user));
		when(billReportQueryService.findMonthlyDiagnosisReports(USER_ID)).thenReturn(List.of());
		when(ecoReportQueryService.findMonthlyReports(USER_ID)).thenReturn(List.of());
		when(ecoReportQueryService.findResultReports(USER_ID)).thenReturn(List.of());
		when(ecoLinkService.getStatus(USER_ID)).thenReturn(new EcoStatusResponse(
			EcoLinkStatus.UNLINKED, null, true, true, null, List.of(), false, null,
			"https://ecomileage.seoul.go.kr"
		));

		MypageResponse response = mypageService.getMypage(USER_ID);

		assertThat(response.ecoAddress()).isNull();
		assertThat(response.integration().registeredUtilities()).isEmpty();
	}

	private UserMypageSnapshot linkedUser() {
		return new UserMypageSnapshot(
			"김수현", "11", "서울특별시", "11620", "관악구", "ONE_ROOM", "UNDER_10",
			EcoLinkStatus.LINKED, LocalDateTime.of(2026, 9, 1, 9, 0), "11", "11620",
			"서울 관악구", LocalDate.of(2026, 3, 1), true,
			LocalDateTime.of(2026, 9, 1, 9, 12), "1005-1234-5678-90"
		);
	}

	private EcoStatusResponse linkedEcoStatus() {
		return new EcoStatusResponse(
			EcoLinkStatus.LINKED,
			null,
			true,
			false,
			null,
			List.of(
				new EcoStatusResponse.RegisteredUtility(UtilityType.ELECTRICITY, true, null),
				new EcoStatusResponse.RegisteredUtility(UtilityType.GAS, true, null),
				new EcoStatusResponse.RegisteredUtility(UtilityType.WATER, true, null)
			),
			true,
			null,
			"https://ecomileage.seoul.go.kr"
		);
	}
}
