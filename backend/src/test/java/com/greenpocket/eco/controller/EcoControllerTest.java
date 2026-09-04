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
import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalPreviewResponse;
import com.greenpocket.eco.dto.EcoGoalRequest;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoGoalSaveResponse;
import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.JobStatus;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.TargetTier;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.service.EcoGoalService;
import com.greenpocket.eco.service.EcoLinkService;
import com.greenpocket.eco.service.EcoRoundService;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.type.UtilityType;

class EcoControllerTest {

	private static final Long USER_ID = 42L;

	private EcoLinkService ecoLinkService;
	private EcoRoundService ecoRoundService;
	private EcoGoalService ecoGoalService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ecoLinkService = mock(EcoLinkService.class);
		ecoRoundService = mock(EcoRoundService.class);
		ecoGoalService = mock(EcoGoalService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(
			new EcoController(ecoLinkService, ecoRoundService, ecoGoalService)
		)
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
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
