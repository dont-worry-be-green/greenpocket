package com.greenpocket.pocket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.pocket.dto.PocketWithdrawalHistoryResponse;
import com.greenpocket.pocket.dto.PocketWithdrawalRequest;
import com.greenpocket.pocket.dto.PocketWithdrawalResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountSnapshotResponse;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionStatus;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.service.PocketWithdrawalService;
import com.greenpocket.pocket.service.PocketWithdrawalService.WithdrawalExecution;

class PocketWithdrawalControllerTest {

	private static final Long USER_ID = 42L;
	private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

	private PocketWithdrawalService pocketWithdrawalService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		pocketWithdrawalService = mock(PocketWithdrawalService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new PocketWithdrawalController(pocketWithdrawalService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void createsWithdrawal() throws Exception {
		when(pocketWithdrawalService.withdraw(eq(USER_ID), eq(IDEMPOTENCY_KEY), any(PocketWithdrawalRequest.class)))
			.thenReturn(new WithdrawalExecution(response(), false));

		performWithdrawal("{\"amount\":30000,\"accountId\":3}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.transactionStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.data.balanceAfter").value(34000))
			.andExpect(jsonPath("$.data.expectedDate").value("2026-09-08"));
	}

	@Test
	void returnsOkForRepeatedIdempotencyKey() throws Exception {
		when(pocketWithdrawalService.withdraw(eq(USER_ID), eq(IDEMPOTENCY_KEY), any(PocketWithdrawalRequest.class)))
			.thenReturn(new WithdrawalExecution(response(), true));

		performWithdrawal("{\"amount\":1,\"accountId\":999999}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.transactionId").value(130));
	}

	@Test
	void returnsWithdrawalHistory() throws Exception {
		when(pocketWithdrawalService.findWithdrawals(USER_ID, 0, 20))
			.thenReturn(new PocketWithdrawalHistoryResponse(List.of(), 0, 20, 0, 0, false));

		mockMvc.perform(get("/api/v1/pocket/withdrawals")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isEmpty())
			.andExpect(jsonPath("$.data.hasNext").value(false));
	}

	@Test
	void rejectsMissingAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/pocket/withdrawals"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	@Test
	void rejectsMissingIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/withdrawals")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":30000,\"accountId\":3}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void rejectsTooLongIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/withdrawals")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.header("Idempotency-Key", "a".repeat(101))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":30000,\"accountId\":3}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void rejectsBlankIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/withdrawals")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.header("Idempotency-Key", " ")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":30000,\"accountId\":3}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void rejectsInvalidPaging() throws Exception {
		mockMvc.perform(get("/api/v1/pocket/withdrawals")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.param("page", "-1")
				.param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void mapsInvalidAmountError() throws Exception {
		stubBusinessError(PocketErrorCode.POCKET_AMOUNT_INVALID);

		performWithdrawal("{\"amount\":0,\"accountId\":3}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("POCKET_AMOUNT_INVALID"));
	}

	@Test
	void mapsAccountRequiredError() throws Exception {
		stubBusinessError(PocketErrorCode.POCKET_ACCOUNT_REQUIRED);

		performWithdrawal("{\"amount\":30000,\"accountId\":null}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("POCKET_ACCOUNT_REQUIRED"));
	}

	@Test
	void mapsAccountNotFoundError() throws Exception {
		stubBusinessError(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND);

		performWithdrawal("{\"amount\":30000,\"accountId\":999999999}")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("POCKET_ACCOUNT_NOT_FOUND"));
	}

	@Test
	void mapsInsufficientBalanceError() throws Exception {
		stubBusinessError(PocketErrorCode.POCKET_INSUFFICIENT_BALANCE);

		performWithdrawal("{\"amount\":999999999,\"accountId\":3}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("POCKET_INSUFFICIENT_BALANCE"));
	}

	private org.springframework.test.web.servlet.ResultActions performWithdrawal(String body) throws Exception {
		return mockMvc.perform(post("/api/v1/pocket/withdrawals")
			.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
			.header("Idempotency-Key", IDEMPOTENCY_KEY)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private void stubBusinessError(PocketErrorCode errorCode) {
		when(pocketWithdrawalService.withdraw(eq(USER_ID), eq(IDEMPOTENCY_KEY), any(PocketWithdrawalRequest.class)))
			.thenThrow(new BusinessException(errorCode));
	}

	private PocketWithdrawalResponse response() {
		return new PocketWithdrawalResponse(
			130L,
			"GP-2609-0025",
			TransactionDirection.DEBIT,
			TransactionType.WITHDRAWAL,
			30_000L,
			TransactionStatus.COMPLETED,
			OffsetDateTime.parse("2026-09-04T10:00:00+09:00"),
			LocalDate.of(2026, 9, 8),
			34_000L,
			new WithdrawalAccountSnapshotResponse("신한은행", "110-123-456789", "김수현"),
			"영업일 기준 1~2일 내에 입금될 예정이에요"
		);
	}
}
