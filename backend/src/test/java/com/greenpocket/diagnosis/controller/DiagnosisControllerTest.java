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
import com.greenpocket.diagnosis.dto.DiagnosisMonthsResponse;
import com.greenpocket.diagnosis.dto.DiagnosisResponse;
import com.greenpocket.diagnosis.entity.RegionLevel;
import com.greenpocket.diagnosis.exception.DiagnosisErrorCode;
import com.greenpocket.diagnosis.service.DiagnosisBaselineService;
import com.greenpocket.diagnosis.service.DiagnosisResultService;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.global.type.UtilityType;

class DiagnosisControllerTest {

	private static final Long USER_ID = 42L;

	private DiagnosisBaselineService diagnosisBaselineService;
	private DiagnosisResultService diagnosisResultService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		diagnosisBaselineService = mock(DiagnosisBaselineService.class);
		diagnosisResultService = mock(DiagnosisResultService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(
			new DiagnosisController(diagnosisBaselineService, diagnosisResultService)
		)
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsRegisteredMonths() throws Exception {
		when(diagnosisResultService.findMonths(USER_ID)).thenReturn(new DiagnosisMonthsResponse(
			java.util.List.of(new DiagnosisMonthsResponse.MonthItem(
				"2026-08", true, java.util.List.of(UtilityType.ELECTRICITY), 43_200L
			)),
			"2026-08"
		));

		mockMvc.perform(get("/api/v1/diagnosis/months")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.defaultMonth").value("2026-08"))
			.andExpect(jsonPath("$.data.months[0].registered").value(true))
			.andExpect(jsonPath("$.data.months[0].totalAmount").value(43_200));
	}

	@Test
	void returnsEmptyDiagnosis() throws Exception {
		when(diagnosisResultService.findDiagnosis(USER_ID, null))
			.thenReturn(DiagnosisResponse.empty("2026-08"));

		mockMvc.perform(get("/api/v1/diagnosis")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.empty").value(true))
			.andExpect(jsonPath("$.data.targetYearMonth").value("2026-08"))
			.andExpect(jsonPath("$.data.screen").value("AN-01"));
	}

	@Test
	void unregisteredDiagnosisMonthReturnsDomainError() throws Exception {
		YearMonth month = YearMonth.of(2026, 6);
		when(diagnosisResultService.findDiagnosis(USER_ID, month))
			.thenThrow(new BusinessException(DiagnosisErrorCode.DIAGNOSIS_MONTH_EMPTY));

		mockMvc.perform(get("/api/v1/diagnosis")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("month", "2026-06"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("DIAGNOSIS_MONTH_EMPTY"));
	}

	@Test
	void invalidDiagnosisMonthFormatReturnsInvalidRequest() throws Exception {
		mockMvc.perform(get("/api/v1/diagnosis")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("month", "2026/08"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
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

	@Test
	void missingRequiredParameterReturnsInvalidRequest() throws Exception {
		mockMvc.perform(get("/api/v1/diagnosis/baseline")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("month", "2026-08")
				.param("utility", "ELECTRICITY"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.error.message").value("입력값을 다시 확인해 주세요."));
	}

	@Test
	void invalidMonthFormatReturnsInvalidRequest() throws Exception {
		mockMvc.perform(get("/api/v1/diagnosis/baseline")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("sigunguCode", "11620")
				.param("month", "2026/08")
				.param("utility", "ELECTRICITY"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void invalidUtilityReturnsInvalidRequest() throws Exception {
		mockMvc.perform(get("/api/v1/diagnosis/baseline")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("sigunguCode", "11620")
				.param("month", "2026-08")
				.param("utility", "ELECTRIC"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}
}
