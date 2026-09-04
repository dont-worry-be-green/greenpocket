package com.greenpocket.pocket.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.pocket.dto.PocketConversionCompleteResponse;
import com.greenpocket.pocket.dto.PocketConversionRequest;
import com.greenpocket.pocket.dto.PocketConversionStartResponse;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.service.PocketConversionService;

class PocketConversionControllerTest {

	private static final Long USER_ID = 42L;
	private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

	private PocketConversionService pocketConversionService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		pocketConversionService = mock(PocketConversionService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new PocketConversionController(pocketConversionService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsCreatedForConversionStart() throws Exception {
		PocketConversionRequest request = new PocketConversionRequest(7L, true);
		when(pocketConversionService.start(USER_ID, request)).thenReturn(new PocketConversionStartResponse(
			120L,
			7L,
			30_000L,
			TransactionStatus.REQUESTED,
			"https://ecomileage.seoul.go.kr/mileage/convert",
			OffsetDateTime.parse("2026-09-03T18:58:00+09:00"),
			"현금으로 바꿔야 그린포켓 계좌로 들어와요"
		));

		mockMvc.perform(post("/api/v1/pocket/conversions")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"roundId":7,"agreed":true}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.conversionId").value(120))
			.andExpect(jsonPath("$.data.transactionStatus").value("REQUESTED"))
			.andExpect(jsonPath("$.data.amount").value(30000));
	}

	@Test
	void returnsCompletedConversion() throws Exception {
		PocketConversionCompleteResponse response = new PocketConversionCompleteResponse(
			120L,
			TransactionStatus.COMPLETED,
			30_000L,
			OffsetDateTime.parse("2026-09-03T19:01:00+09:00"),
			94_000L,
			new PocketConversionCompleteResponse.Transaction(
				120L,
				"GP-2609-0021",
				"에코마일리지 2026 상반기"
			)
		);
		when(pocketConversionService.complete(USER_ID, 120L, IDEMPOTENCY_KEY))
			.thenReturn(new PocketConversionService.CompletionExecution(response, false));

		mockMvc.perform(post("/api/v1/pocket/conversions/120/complete")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.header("Idempotency-Key", IDEMPOTENCY_KEY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.transactionStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.data.balanceAfter").value(94000))
			.andExpect(jsonPath("$.data.transaction.transactionCode").value("GP-2609-0021"));
	}

	@Test
	void rejectsMissingAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/conversions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"roundId":7,"agreed":true}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	@Test
	void rejectsMissingOrMalformedIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/conversions/120/complete")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

		mockMvc.perform(post("/api/v1/pocket/conversions/120/complete")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.header("Idempotency-Key", "not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void rejectsInvalidStartRequest() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/conversions")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"roundId":0,"agreed":true}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.error.field").value("roundId"));
	}
}
