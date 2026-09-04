package com.greenpocket.diagnosis.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.service.DiagnosisBaselineService;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.type.UtilityType;

class DiagnosisControllerTest {

	private static final Long USER_ID = 42L;

	private DiagnosisBaselineService diagnosisBaselineService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		diagnosisBaselineService = mock(DiagnosisBaselineService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new DiagnosisController(diagnosisBaselineService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.build();
	}

	@Test
	void returnsWrappedBaselineResponse() throws Exception {
		DiagnosisBaselineResponse response = new DiagnosisBaselineResponse(
			true,
			RegionLevel.SIGUNGU,
			"11",
			"11620",
			"2026-07",
			UtilityType.ELECTRICITY,
			132_840L,
			new BigDecimal("289.400"),
			38_900L,
			"한국전력공사 전력데이터 개방포털",
			OffsetDateTime.parse("2026-08-28T00:00:00+09:00")
		);
		when(diagnosisBaselineService.findBaseline(
			USER_ID,
			"11620",
			YearMonth.of(2026, 8),
			UtilityType.ELECTRICITY
		)).thenReturn(response);

		mockMvc.perform(get("/api/v1/diagnosis/baseline")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("sigunguCode", "11620")
				.param("month", "2026-08")
				.param("utility", "ELECTRICITY"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.found").value(true))
			.andExpect(jsonPath("$.data.regionLevel").value("SIGUNGU"))
			.andExpect(jsonPath("$.data.baseMonth").value("2026-07"))
			.andExpect(jsonPath("$.data.avgAmount").value(38_900))
			.andExpect(jsonPath("$.error").doesNotExist());
	}
}
