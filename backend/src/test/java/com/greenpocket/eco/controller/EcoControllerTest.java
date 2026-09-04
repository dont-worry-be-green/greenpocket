package com.greenpocket.eco.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.eco.dto.EcoCurrentRoundResponse;
import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.entity.ApplicationStatus;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.eco.entity.JobStatus;
import com.greenpocket.eco.entity.RoundStatus;
import com.greenpocket.eco.entity.UsageUnit;
import com.greenpocket.eco.service.EcoLinkService;
import com.greenpocket.eco.service.EcoRoundService;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.type.UtilityType;

class EcoControllerTest {

	private static final Long USER_ID = 42L;

	private EcoLinkService ecoLinkService;
	private EcoRoundService ecoRoundService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		ecoLinkService = mock(EcoLinkService.class);
		ecoRoundService = mock(EcoRoundService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new EcoController(ecoLinkService, ecoRoundService))
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
}
