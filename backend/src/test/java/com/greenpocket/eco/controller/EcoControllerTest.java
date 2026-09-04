package com.greenpocket.eco.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.eco.dto.EcoCurrentRoundResponse;
import com.greenpocket.eco.dto.EcoApplicationResponse;
import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalPreviewResponse;
import com.greenpocket.eco.dto.EcoGoalRequest;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoGoalSaveResponse;
import com.greenpocket.eco.dto.EcoHomeResponse;
import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoMissionLogResponse;
import com.greenpocket.eco.dto.EcoMissionAdjustResponse;
import com.greenpocket.eco.dto.EcoMissionUpdateResponse;
import com.greenpocket.eco.dto.EcoMonthlyReportResponse;
import com.greenpocket.eco.dto.EcoResultResponse;
import com.greenpocket.eco.dto.EcoSettlementResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.dto.EcoTodayMissionsResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.JobStatus;
import com.greenpocket.eco.entity.MissionDifficulty;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.entity.WhatIfScreen;
import com.greenpocket.eco.service.EcoGoalService;
import com.greenpocket.eco.service.EcoApplicationService;
import com.greenpocket.eco.service.EcoLinkService;
import com.greenpocket.eco.service.EcoMissionService;
import com.greenpocket.eco.service.EcoMissionAdjustService;
import com.greenpocket.eco.service.EcoProgressService;
import com.greenpocket.eco.service.EcoResultService;
import com.greenpocket.eco.service.EcoRoundService;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.type.UtilityType;

class EcoControllerTest {

	private static final Long USER_ID = 42L;

	private EcoLinkService ecoLinkService;
	private EcoRoundService ecoRoundService;
	private EcoGoalService ecoGoalService;
	private EcoProgressService ecoProgressService;
	private EcoResultService ecoResultService;
	private EcoApplicationService ecoApplicationService;
	private EcoMissionService ecoMissionService;
	private EcoMissionAdjustService ecoMissionAdjustService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ecoLinkService = mock(EcoLinkService.class);
		ecoRoundService = mock(EcoRoundService.class);
		ecoGoalService = mock(EcoGoalService.class);
		ecoProgressService = mock(EcoProgressService.class);
		ecoResultService = mock(EcoResultService.class);
		ecoApplicationService = mock(EcoApplicationService.class);
		ecoMissionService = mock(EcoMissionService.class);
		ecoMissionAdjustService = mock(EcoMissionAdjustService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(
			new EcoController(
				ecoLinkService,
				ecoRoundService,
				ecoGoalService,
				ecoProgressService,
				ecoResultService,
				ecoApplicationService,
				ecoMissionService,
				ecoMissionAdjustService
			)
		)
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
	}

	@Test
	void returnsMissionAdjustRecommendation() throws Exception {
		when(ecoMissionAdjustService.getMissionAdjust(USER_ID, 7L, "ELECTRICITY", "2026-08"))
			.thenReturn(new EcoMissionAdjustResponse(
				7L,
				UtilityType.ELECTRICITY,
				"2026-08",
				new BigDecimal("11.000"),
				"도시가스와 수도 감축률을 유지할 때예요",
				new BigDecimal("83.000"),
				new EcoMissionAdjustResponse.Comparison(
					new BigDecimal("3.000"),
					new BigDecimal("-1.887")
				),
				1,
				List.of(new EcoMissionAdjustResponse.Mission(
					14L,
					"조명 끄기",
					new BigDecimal("8.000"),
					MissionDifficulty.EASY,
					"조명",
					"공식 절감 수치",
					"월 기준 사용량으로 환산",
					"한국에너지공단",
					false,
					true,
					false
				)),
				new EcoMissionAdjustResponse.Preview(
					new BigDecimal("3.000"),
					new BigDecimal("11.000"),
					true
				),
				new EcoMissionAdjustResponse.TierDowngrade(false, 1, "목표 구간을 유지해도 좋아요")
			));

		mockMvc.perform(get("/api/v1/eco/rounds/7/mission-adjust")
				.param("utility", "ELECTRICITY")
				.param("month", "2026-08")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.utilityType").value("ELECTRICITY"))
			.andExpect(jsonPath("$.data.missions[0].recommended").value(true))
			.andExpect(jsonPath("$.data.preview.coversRequired").value(true));
	}

	@Test
	void updatesSelectedMissions() throws Exception {
		when(ecoGoalService.updateMissions(anyLong(), anyLong(), any()))
			.thenReturn(new EcoMissionUpdateResponse(
				7L,
				new BigDecimal("18.000"),
				List.of(new EcoMissionUpdateResponse.Item(13L, new BigDecimal("18.000"), true, null)),
				true
			));

		mockMvc.perform(put("/api/v1/eco/rounds/7/missions")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "selectedMissionIds": [13] }
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.combinedMissionRate").value(18.000))
			.andExpect(jsonPath("$.data.items[0].missionId").value(13))
			.andExpect(jsonPath("$.data.todayMissionsUpdated").value(true));
	}

	@Test
	void returnsTodayMissions() throws Exception {
		when(ecoMissionService.getTodayMissions(USER_ID, 7L, "2026-09-03"))
			.thenReturn(new EcoTodayMissionsResponse(
				"2026-09-03",
				"AUTUMN",
				1,
				2,
				List.of(
					new EcoTodayMissionsResponse.Mission(
						12L,
						"냉방 온도 26℃로 맞추기",
						UtilityType.ELECTRICITY,
						MissionDifficulty.EASY,
						true
					)
				),
				null
			));

		mockMvc.perform(get("/api/v1/eco/rounds/7/missions/today")
				.param("date", "2026-09-03")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.date").value("2026-09-03"))
			.andExpect(jsonPath("$.data.season").value("AUTUMN"))
			.andExpect(jsonPath("$.data.completedCount").value(1))
			.andExpect(jsonPath("$.data.missions[0].completed").value(true));
	}

	@Test
	void savesTodayMissionLog() throws Exception {
		when(ecoMissionService.saveMissionLog(anyLong(), anyLong(), anyString(), any()))
			.thenReturn(new EcoMissionLogResponse("2026-09-03", 2, 5));

		mockMvc.perform(put("/api/v1/eco/rounds/7/mission-logs/2026-09-03")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "completedMissionIds": [12, 31] }
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.date").value("2026-09-03"))
			.andExpect(jsonPath("$.data.completedCount").value(2))
			.andExpect(jsonPath("$.data.totalCount").value(5));
	}

	@Test
	void appliesForEcoMileage() throws Exception {
		when(ecoApplicationService.apply(USER_ID, 7L)).thenReturn(new EcoApplicationResponse(
			7L,
			ApplicationStatus.APPLIED,
			OffsetDateTime.parse("2026-09-03T18:45:00+09:00"),
			false
		));

		mockMvc.perform(post("/api/v1/eco/rounds/7/application")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.roundId").value(7))
			.andExpect(jsonPath("$.data.applicationStatus").value("APPLIED"))
			.andExpect(jsonPath("$.data.appliedAt").value("2026-09-03T18:45:00+09:00"))
			.andExpect(jsonPath("$.data.showBanner").value(false));
	}

	@Test
	void returnsWrappedEcoStatus() throws Exception {
		when(ecoLinkService.getStatus(USER_ID)).thenReturn(new EcoStatusResponse(
			EcoLinkStatus.UNLINKED,
			null,
			true,
			true,
			null,
			List.of(
				new EcoStatusResponse.RegisteredUtility(UtilityType.ELECTRICITY, false, null),
				new EcoStatusResponse.RegisteredUtility(UtilityType.GAS, false, null),
				new EcoStatusResponse.RegisteredUtility(UtilityType.WATER, false, null)
			),
			false,
			null,
			"https://ecomileage.seoul.go.kr"
		));

		mockMvc.perform(get("/api/v1/eco/status")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.linkStatus").value("UNLINKED"))
			.andExpect(jsonPath("$.data.registeredUtilities.length()").value(3));
	}

	@Test
	void returnsWhatIfHomeRoutingAndProgress() throws Exception {
		when(ecoProgressService.getHome(USER_ID)).thenReturn(new EcoHomeResponse(
			WhatIfScreen.WF_06_IN_PROGRESS,
			7L,
			new EcoHomeResponse.Header("2026-04", "2026-09", 2, List.of(8, 9)),
			new EcoHomeResponse.Progress(
				new BigDecimal("9.000"),
				List.of("2026-04", "2026-05", "2026-06", "2026-07"),
				TargetTier.TIER_5,
				TargetTier.TIER_10,
				List.of(new EcoHomeResponse.TierProgress(TargetTier.TIER_5, 10_000L, "CURRENT")),
				new BigDecimal("1.000"),
				30_000L
			),
			new EcoHomeResponse.LatestReport(
				true,
				"2026-07",
				OffsetDateTime.parse("2026-08-03T00:00:00+09:00"),
				new BigDecimal("1.039"),
				new BigDecimal("10.000"),
				false
			),
			new EcoHomeResponse.Application(
				ApplicationStatus.NOT_APPLIED,
				true,
				"https://ecomileage.seoul.go.kr"
			),
			new EcoHomeResponse.Goal(true, new BigDecimal("11.322"), TargetTier.TIER_10, 30_000L),
			new EcoHomeResponse.TodayMissions(3, 5),
			null,
			new EcoHomeResponse.Links(true, true, true)
		));

		mockMvc.perform(get("/api/v1/eco/home")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.screen").value("WF_06_IN_PROGRESS"))
			.andExpect(jsonPath("$.data.progress.cumulativeRate").value(9.000))
			.andExpect(jsonPath("$.data.latestReport.reportMonth").value("2026-07"))
			.andExpect(jsonPath("$.data.todayMissions.completedCount").value(3));
	}

	@Test
	void returnsMonthlyReportEmptyReasonWithoutError() throws Exception {
		when(ecoProgressService.getMonthlyReport(USER_ID, "2026-07"))
			.thenReturn(new EcoMonthlyReportResponse(
				"2026-07",
				7L,
				null,
				"2024·2025년 7월 평균",
				null,
				null,
				null,
				List.of(),
				"NO_BILL"
			));

		mockMvc.perform(get("/api/v1/eco/monthly-report")
				.param("month", "2026-07")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.result").doesNotExist())
			.andExpect(jsonPath("$.data.emptyReason").value("NO_BILL"));
	}

	@Test
	void startsLinkWithAcceptedStatus() throws Exception {
		when(ecoLinkService.startLink(USER_ID))
			.thenReturn(new EcoLinkStartResponse("eco_demo", JobStatus.RUNNING, 20));

		mockMvc.perform(post("/api/v1/eco/link")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.data.linkJobId").value("eco_demo"))
			.andExpect(jsonPath("$.data.status").value("RUNNING"));
	}

	@Test
	void returnsLinkProgress() throws Exception {
		when(ecoLinkService.getLinkProgress(anyLong(), anyString()))
			.thenReturn(EcoLinkProgressResponse.running("eco_demo"));

		mockMvc.perform(get("/api/v1/eco/link/eco_demo")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RUNNING"))
			.andExpect(jsonPath("$.data.utilityStatus.length()").value(3));
	}

	@Test
	void returnsWrappedCurrentRound() throws Exception {
		EcoCurrentRoundResponse response = new EcoCurrentRoundResponse(
			7L,
			"2026-04",
			"2026-09",
			1,
			RoundStatus.READY,
			ApplicationStatus.NOT_APPLIED,
			false,
			OffsetDateTime.parse("2026-09-01T09:00:00+09:00"),
			"2024·2025년 4~9월 평균",
			new EcoCurrentRoundResponse.Baseline(
				420_600L,
				new BigDecimal("831992.000"),
				List.of(new EcoCurrentRoundResponse.BaselineItem(
					UtilityType.ELECTRICITY,
					true,
					268_000L,
					new BigDecimal("1340.000"),
					UsageUnit.kWh,
					new BigDecimal("424.000"),
					new BigDecimal("64.000")
				)),
				UtilityType.ELECTRICITY
			),
			"WF-03"
		);
		when(ecoRoundService.getCurrentRound(USER_ID)).thenReturn(response);

		mockMvc.perform(get("/api/v1/eco/rounds/current")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.roundId").value(7))
			.andExpect(jsonPath("$.data.baseline.totalAmount").value(420_600))
			.andExpect(jsonPath("$.data.nextScreen").value("WF-03"));
	}

	@Test
	void returnsGoalForm() throws Exception {
		when(ecoGoalService.getGoalForm(USER_ID, 7L)).thenReturn(new EcoGoalFormResponse(
			7L,
			"2026-04",
			"2026-09",
			List.of(new EcoGoalFormResponse.TierOption(
				TargetTier.TIER_10,
				"10~15%",
				new BigDecimal("10.000"),
				30_000L
			)),
			List.of(new EcoGoalFormResponse.Segment(
				UtilityType.ELECTRICITY,
				true,
				null,
				268_000L,
				new BigDecimal("1340.000"),
				new BigDecimal("223.333"),
				UsageUnit.kWh,
				new BigDecimal("30.000"),
				TargetTier.TIER_10,
				null,
				false,
				List.of()
			))
		));

		mockMvc.perform(get("/api/v1/eco/rounds/7/goal-form")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.roundId").value(7))
			.andExpect(jsonPath("$.data.tiers[0].tier").value("TIER_10"))
			.andExpect(jsonPath("$.data.segments[0].utilityType").value("ELECTRICITY"));
	}

	@Test
	void returnsGoalPreviewWithoutSaving() throws Exception {
		when(ecoGoalService.preview(anyLong(), anyLong(), any(EcoGoalRequest.class)))
			.thenReturn(goalPreview());

		mockMvc.perform(post("/api/v1/eco/rounds/7/goal/preview")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(goalRequestJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.combined.combinedRate").value(11.322))
			.andExpect(jsonPath("$.data.combined.expectedMileage").value(30_000))
			.andExpect(jsonPath("$.data.utilities[0].targetUsage").value(1206.000));
	}

	@Test
	void createsGoal() throws Exception {
		when(ecoGoalService.save(anyLong(), anyLong(), any(EcoGoalRequest.class)))
			.thenReturn(goalSaveResponse());

		mockMvc.perform(post("/api/v1/eco/rounds/7/goal")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(goalRequestJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.roundStatus").value("GOAL_SET"))
			.andExpect(jsonPath("$.data.nextScreen").value("WF-06"));
	}

	@Test
	void updatesGoal() throws Exception {
		when(ecoGoalService.save(anyLong(), anyLong(), any(EcoGoalRequest.class)))
			.thenReturn(goalSaveResponse());

		mockMvc.perform(put("/api/v1/eco/rounds/7/goal")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(goalRequestJson()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.savedMissionCount").value(2))
			.andExpect(jsonPath("$.data.expectedSavingAmount").value(44_090));
	}

	@Test
	void returnsSavedGoal() throws Exception {
		when(ecoGoalService.getGoal(USER_ID, 7L)).thenReturn(new EcoGoalResponse(
			7L,
			true,
			OffsetDateTime.parse("2026-09-04T18:30:00+09:00"),
			new BigDecimal("11.322"),
			TargetTier.TIER_10,
			30_000L,
			44_090L,
			List.of(new EcoGoalResponse.UtilityGoal(
				UtilityType.ELECTRICITY,
				TargetTier.TIER_10,
				new BigDecimal("10.000"),
				new BigDecimal("1340.000"),
				new BigDecimal("1206.000"),
				UsageUnit.kWh,
				26_800L
			)),
			List.of()
		));

		mockMvc.perform(get("/api/v1/eco/rounds/7/goal")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.goalSet").value(true))
			.andExpect(jsonPath("$.data.tier").value("TIER_10"))
			.andExpect(jsonPath("$.data.utilities[0].expectedSaving").value(26_800));
	}

	@Test
	void returnsConfirmedRoundResult() throws Exception {
		when(ecoResultService.getResult(USER_ID, 7L)).thenReturn(new EcoResultResponse(
			7L,
			"2026-04",
			"2026-09",
			OffsetDateTime.parse("2026-12-05T00:00:00+09:00"),
			"에코마일리지 누리집 기준",
			new BigDecimal("12.499"),
			new BigDecimal("10.000"),
			true,
			TargetTier.TIER_10,
			"10~15% 구간",
			30_000L,
			new EcoResultResponse.Amount(420_600L, 370_100L, 50_500L, false),
			List.of(new EcoResultResponse.UtilityResult(
				UtilityType.ELECTRICITY,
				new BigDecimal("1340.000"),
				new BigDecimal("1166.000"),
				UsageUnit.kWh,
				new BigDecimal("13.000"),
				new BigDecimal("10.000"),
				true
			)),
			List.of(new EcoResultResponse.MonthlyRate("2026-09", new BigDecimal("17.000"), true)),
			false,
			new EcoResultResponse.NextRound(8L, "2026-10", "2027-03", false)
		));

		mockMvc.perform(get("/api/v1/eco/rounds/7/result")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.finalRate").value(12.499))
			.andExpect(jsonPath("$.data.tier").value("TIER_10"))
			.andExpect(jsonPath("$.data.amount.savedIsPocketEligible").value(false))
			.andExpect(jsonPath("$.data.utilityResults[0].utilityType").value("ELECTRICITY"))
			.andExpect(jsonPath("$.data.nextRound.roundId").value(8));
	}

	@Test
	void returnsMileageSettlement() throws Exception {
		when(ecoResultService.getSettlement(USER_ID, 7L)).thenReturn(new EcoSettlementResponse(
			7L,
			"2026-04",
			"2026-09",
			30_000L,
			"확인",
			new BigDecimal("12.499"),
			TargetTier.TIER_10,
			new EcoSettlementResponse.Calculation(
				420_600L,
				370_100L,
				50_500L,
				"전기·도시가스·수도를 직전 2년 같은 기간(4~9월) 평균과 비교했어요"
			),
			false,
			true,
			"https://ecomileage.seoul.go.kr",
			List.of("서울시 세금", "상품권", "관리비 납부")
		));

		mockMvc.perform(get("/api/v1/eco/rounds/7/settlement")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.confirmedMileage").value(30_000))
			.andExpect(jsonPath("$.data.statusLabel").value("확인"))
			.andExpect(jsonPath("$.data.isCash").value(false))
			.andExpect(jsonPath("$.data.convertible").value(true))
			.andExpect(jsonPath("$.data.otherUses.length()").value(3));
	}

	private EcoGoalPreviewResponse goalPreview() {
		return new EcoGoalPreviewResponse(
			List.of(new EcoGoalPreviewResponse.UtilityTarget(
				UtilityType.ELECTRICITY,
				new BigDecimal("10.000"),
				new BigDecimal("1340.000"),
				new BigDecimal("1206.000"),
				UsageUnit.kWh,
				268_000L,
				26_800L,
				0
			)),
			new EcoGoalPreviewResponse.Combined(
				new BigDecimal("831992.000"),
				new BigDecimal("737792.400"),
				new BigDecimal("11.322"),
				TargetTier.TIER_10,
				"10~15%",
				30_000L,
				44_090L,
				420_600L,
				List.of(),
				new EcoGoalPreviewResponse.NextTier(
					TargetTier.TIER_15,
					new BigDecimal("3.678"),
					50_000L
				)
			),
			new EcoGoalPreviewResponse.MissionSummary(
				new BigDecimal("18.000"),
				BigDecimal.ZERO.setScale(3),
				true,
				List.of()
			),
			List.of(new EcoGoalPreviewResponse.CarbonFactor(
				UtilityType.ELECTRICITY,
				new BigDecimal("424.000"),
				UsageUnit.kWh
			))
		);
	}

	private EcoGoalSaveResponse goalSaveResponse() {
		return new EcoGoalSaveResponse(
			7L,
			OffsetDateTime.parse("2026-09-04T18:30:00+09:00"),
			RoundStatus.GOAL_SET,
			new BigDecimal("11.322"),
			30_000L,
			44_090L,
			2,
			"WF-06"
		);
	}

	private String goalRequestJson() {
		return """
			{
			  "targets": [
			    {"utilityType": "ELECTRICITY", "tier": "TIER_10"}
			  ],
			  "selectedMissionIds": [12, 13]
			}
			""";
	}
}
