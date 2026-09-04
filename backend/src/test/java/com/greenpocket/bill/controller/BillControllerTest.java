package com.greenpocket.bill.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.bill.dto.BillCreateRequest;
import com.greenpocket.bill.dto.BillCreateResponse;
import com.greenpocket.bill.dto.BillDuplicateCheckResponse;
import com.greenpocket.bill.dto.BillTargetMonthResponse;
import com.greenpocket.bill.entity.BillType;
import com.greenpocket.bill.entity.InputSource;
import com.greenpocket.bill.entity.RecordStatus;
import com.greenpocket.bill.entity.UsageUnit;
import com.greenpocket.bill.exception.BillErrorCode;
import com.greenpocket.bill.service.BillRegistrationService;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.global.type.UtilityType;

class BillControllerTest {

	private static final Long USER_ID = 42L;

	private BillRegistrationService billRegistrationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		billRegistrationService = mock(BillRegistrationService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new BillController(billRegistrationService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsTargetMonth() throws Exception {
		when(billRegistrationService.getTargetMonth(USER_ID)).thenReturn(new BillTargetMonthResponse(
			"2026-08", "2026-07", false, List.of(), "AN-02"
		));

		mockMvc.perform(get("/api/v1/bills/target-month")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.targetYearMonth").value("2026-08"))
			.andExpect(jsonPath("$.data.nextScreen").value("AN-02"));
	}

	@Test
	void returnsDuplicateCheckResults() throws Exception {
		when(billRegistrationService.checkDuplicates(
			USER_ID,
			YearMonth.of(2026, 8),
			List.of(UtilityType.ELECTRICITY, UtilityType.WATER)
		)).thenReturn(new BillDuplicateCheckResponse(
			"2026-08",
			List.of(
				new BillDuplicateCheckResponse.Result(UtilityType.ELECTRICITY, true, 41L),
				new BillDuplicateCheckResponse.Result(UtilityType.WATER, false, null)
			)
		));

		mockMvc.perform(get("/api/v1/bills/duplicate-check")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("billingMonth", "2026-08")
				.param("utilityTypes", "ELECTRICITY,WATER"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.results[0].duplicated").value(true))
			.andExpect(jsonPath("$.data.results[0].existingRecordId").value(41))
			.andExpect(jsonPath("$.data.results[1].duplicated").value(false));
	}

	@Test
	void invalidDuplicateCheckMonthReturnsInvalidRequest() throws Exception {
		mockMvc.perform(get("/api/v1/bills/duplicate-check")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("billingMonth", "2026/08")
				.param("utilityTypes", "ELECTRICITY"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void createsBillsWithCreatedStatus() throws Exception {
		BillCreateRequest request = request();
		when(billRegistrationService.create(USER_ID, request)).thenReturn(new BillCreateResponse(
			"2026-08",
			List.of(new BillCreateResponse.Record(
				51L, UtilityType.ELECTRICITY, 43_200L, new BigDecimal("210.000"), RecordStatus.CONFIRMED
			)),
			43_200L,
			new BillCreateResponse.Recalculated("2026-08", true, 7L),
			"AN-07"
		));

		mockMvc.perform(post("/api/v1/bills")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "billingMonth": "2026-08",
					  "billType": "MANAGEMENT",
					  "inputSource": "OCR",
					  "items": [
					    {
					      "utilityType": "ELECTRICITY",
					      "amount": 43200,
					      "usage": 210.000,
					      "usageUnit": "kWh",
					      "confidence": 0.9412
					    }
					  ]
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.records[0].recordId").value(51))
			.andExpect(jsonPath("$.data.totalAmount").value(43_200))
			.andExpect(jsonPath("$.data.recalculated.monthlyReportUpdated").value(true));
	}

	@Test
	void duplicateSaveReturnsDomainError() throws Exception {
		BillCreateRequest request = request();
		when(billRegistrationService.create(USER_ID, request))
			.thenThrow(new BusinessException(BillErrorCode.BILL_DUPLICATED));

		mockMvc.perform(post("/api/v1/bills")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "billingMonth": "2026-08",
					  "billType": "MANAGEMENT",
					  "inputSource": "OCR",
					  "items": [
					    {
					      "utilityType": "ELECTRICITY",
					      "amount": 43200,
					      "usage": 210.000,
					      "usageUnit": "kWh",
					      "confidence": 0.9412
					    }
					  ]
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("BILL_DUPLICATED"));
	}

	@Test
	void malformedAmountReturnsInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/v1/bills")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "billingMonth": "2026-08",
					  "billType": "ELECTRICITY",
					  "inputSource": "MANUAL",
					  "items": [{
					    "utilityType": "ELECTRICITY",
					    "amount": "not-a-number",
					    "usage": 210.000,
					    "usageUnit": "kWh"
					  }]
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	private BillCreateRequest request() {
		return new BillCreateRequest(
			YearMonth.of(2026, 8),
			BillType.MANAGEMENT,
			InputSource.OCR,
			List.of(new BillCreateRequest.Item(
				UtilityType.ELECTRICITY,
				43_200L,
				new BigDecimal("210.000"),
				UsageUnit.kWh,
				new BigDecimal("0.9412")
			))
		);
	}
}
