package com.greenpocket.pocket.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.greenpocket.pocket.dto.WithdrawalAccountCreateRequest;
import com.greenpocket.pocket.dto.WithdrawalAccountDefaultResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountListResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountUpdateRequest;
import com.greenpocket.pocket.exception.PocketErrorCode;
import com.greenpocket.pocket.service.WithdrawalAccountService;

class WithdrawalAccountControllerTest {

	private static final Long USER_ID = 42L;
	private static final Long ACCOUNT_ID = 3L;

	private WithdrawalAccountService withdrawalAccountService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		withdrawalAccountService = mock(WithdrawalAccountService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new WithdrawalAccountController(withdrawalAccountService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsAccountList() throws Exception {
		when(withdrawalAccountService.findAccounts(USER_ID))
			.thenReturn(new WithdrawalAccountListResponse(List.of(accountResponse())));

		mockMvc.perform(get("/api/v1/pocket/accounts")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accounts[0].accountId").value(ACCOUNT_ID))
			.andExpect(jsonPath("$.data.accounts[0].accountNo").value("110-123-456789"))
			.andExpect(jsonPath("$.data.accounts[0].isDefault").value(true));
	}

	@Test
	void createsAccount() throws Exception {
		WithdrawalAccountCreateRequest request = new WithdrawalAccountCreateRequest(
			"088", "신한은행", "110-123-456789", "김수현", true
		);
		when(withdrawalAccountService.createAccount(USER_ID, request)).thenReturn(accountResponse());

		mockMvc.perform(post("/api/v1/pocket/accounts")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"bankCode":"088","bankName":"신한은행","accountNo":"110-123-456789","holder":"김수현","isDefault":true}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID));
	}

	@Test
	void updatesAccount() throws Exception {
		WithdrawalAccountUpdateRequest request = new WithdrawalAccountUpdateRequest(
			"004", "KB국민은행", "123-456-789012", "김수현"
		);
		WithdrawalAccountResponse response = new WithdrawalAccountResponse(
			ACCOUNT_ID, "004", "KB국민은행", "123-456-789012", "김수현", true, true, null
		);
		when(withdrawalAccountService.updateAccount(USER_ID, ACCOUNT_ID, request)).thenReturn(response);

		mockMvc.perform(put("/api/v1/pocket/accounts/{accountId}", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"bankCode":"004","bankName":"KB국민은행","accountNo":"123-456-789012","holder":"김수현"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.bankCode").value("004"))
			.andExpect(jsonPath("$.data.accountNo").value("123-456-789012"));
	}

	@Test
	void makesAccountDefault() throws Exception {
		when(withdrawalAccountService.makeDefault(USER_ID, ACCOUNT_ID))
			.thenReturn(new WithdrawalAccountDefaultResponse(ACCOUNT_ID, true, 2L));

		mockMvc.perform(put("/api/v1/pocket/accounts/{accountId}/default", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accountId").value(ACCOUNT_ID))
			.andExpect(jsonPath("$.data.isDefault").value(true))
			.andExpect(jsonPath("$.data.previousDefaultAccountId").value(2));
	}

	@Test
	void rejectsInvalidCreateRequest() throws Exception {
		mockMvc.perform(post("/api/v1/pocket/accounts")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"bankCode":"","bankName":"","accountNo":"","holder":"","isDefault":true}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.error.field").exists());
	}

	@Test
	void rejectsInvalidUpdateRequest() throws Exception {
		mockMvc.perform(put("/api/v1/pocket/accounts/{accountId}", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"bankCode":"004","bankName":"KB국민은행","accountNo":" ","holder":"김수현"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.error.field").value("accountNo"));
	}

	@Test
	void rejectsMissingAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/pocket/accounts"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	@Test
	void returnsNotFoundForMissingOrOtherUsersAccount() throws Exception {
		WithdrawalAccountUpdateRequest request = new WithdrawalAccountUpdateRequest(
			"004", "KB국민은행", "123-456-789012", "김수현"
		);
		when(withdrawalAccountService.updateAccount(USER_ID, ACCOUNT_ID, request))
			.thenThrow(new BusinessException(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND));

		mockMvc.perform(put("/api/v1/pocket/accounts/{accountId}", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"bankCode":"004","bankName":"KB국민은행","accountNo":"123-456-789012","holder":"김수현"}
					"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("POCKET_ACCOUNT_NOT_FOUND"));
	}

	@Test
	void returnsNotFoundWhenMakingMissingOrOtherUsersAccountDefault() throws Exception {
		when(withdrawalAccountService.makeDefault(USER_ID, ACCOUNT_ID))
			.thenThrow(new BusinessException(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND));

		mockMvc.perform(put("/api/v1/pocket/accounts/{accountId}/default", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("POCKET_ACCOUNT_NOT_FOUND"));
	}

	@Test
	void rejectsNonNumericAccountId() throws Exception {
		mockMvc.perform(put("/api/v1/pocket/accounts/not-a-number/default")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void deletesAccount() throws Exception {
		mockMvc.perform(delete("/api/v1/pocket/accounts/{accountId}", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void returnsNotFoundWhenDeletingMissingOrOtherUsersAccount() throws Exception {
		org.mockito.Mockito.doThrow(new BusinessException(PocketErrorCode.POCKET_ACCOUNT_NOT_FOUND))
			.when(withdrawalAccountService).deleteAccount(USER_ID, ACCOUNT_ID);

		mockMvc.perform(delete("/api/v1/pocket/accounts/{accountId}", ACCOUNT_ID)
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("POCKET_ACCOUNT_NOT_FOUND"));
	}

	private WithdrawalAccountResponse accountResponse() {
		return new WithdrawalAccountResponse(
			ACCOUNT_ID,
			"088",
			"신한은행",
			"110-123-456789",
			"김수현",
			true,
			true,
			null
		);
	}
}
