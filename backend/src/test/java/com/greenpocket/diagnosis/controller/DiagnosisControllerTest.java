package com.greenpocket.diagnosis.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.diagnosis.dto.BaselineCalculationBasis;
import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.dto.DiagnosisMonthsResponse;
import com.greenpocket.diagnosis.dto.DiagnosisResponse;
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
	void returnsDiagnosisSeriesWithExplicitNullForMissingUserUsage() throws Exception {
		DiagnosisResponse.SingleHouseholdTab tab = new DiagnosisResponse.SingleHouseholdTab(
			UtilityType.ELECTRICITY,
			true,
			null,
			new BigDecimal("210.000"),
			new BigDecimal("257.617"),
			new BigDecimal("-47.617"),
			new BigDecimal("-18.484"),
			com.greenpocket.eco.entity.UsageUnit.kWh,
			"전국 1인 가구",
			"에너지경제연구원 2023년 기준 14차 가구에너지패널조사 마이크로데이터",
			"2023",
			BaselineCalculationBasis.WEIGHTED_MONTHLY_MICRODATA_AVERAGE,
			"월별 가중평균",
			List.of(new DiagnosisResponse.SingleHouseholdSeriesPoint(
				"2026-04", null, new BigDecimal("184.783")
			))
		);
		DiagnosisResponse response = new DiagnosisResponse(
			false,
			null,
			"AN-07",
			"2026-08",
			"",
			null,
			null,
			new DiagnosisResponse.SingleHouseholdComparison("1인 가구 평균 사용량", List.of(tab)),
			null
		);
		when(diagnosisResultService.findDiagnosis(USER_ID, YearMonth.of(2026, 8)))
			.thenReturn(response);

		mockMvc.perform(get("/api/v1/diagnosis")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("month", "2026-08"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.singleHouseholdComparison.tabs[0].series[0].yearMonth")
				.value("2026-04"))
			.andExpect(jsonPath("$.data.singleHouseholdComparison.tabs[0].series[0].myUsage")
				.value(nullValue()))
			.andExpect(jsonPath("$.data.singleHouseholdComparison.tabs[0].series[0].averageUsage")
				.value(184.783));
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
			"2026-08",
			UtilityType.ELECTRICITY,
			"전국 1인 가구",
			new BigDecimal("257.617"),
			com.greenpocket.eco.entity.UsageUnit.kWh,
			"에너지경제연구원 2023년 기준 14차 가구에너지패널조사 마이크로데이터",
			"2023",
			BaselineCalculationBasis.WEIGHTED_MONTHLY_MICRODATA_AVERAGE,
			"월별 가중평균"
		);
		when(diagnosisBaselineService.findBaseline(YearMonth.of(2026, 8), UtilityType.ELECTRICITY))
			.thenReturn(response);

		mockMvc.perform(get("/api/v1/diagnosis/baseline")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("month", "2026-08")
				.param("utility", "ELECTRICITY"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.found").value(true))
			.andExpect(jsonPath("$.data.comparisonLabel").value("전국 1인 가구"))
			.andExpect(jsonPath("$.data.averageUsage").value(257.617))
			.andExpect(jsonPath("$.data.calculationBasis")
				.value("WEIGHTED_MONTHLY_MICRODATA_AVERAGE"))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	void missingRequiredParameterReturnsInvalidRequest() throws Exception {
		mockMvc.perform(get("/api/v1/diagnosis/baseline")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
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
				.param("month", "2026-08")
				.param("utility", "ELECTRIC"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}
}
