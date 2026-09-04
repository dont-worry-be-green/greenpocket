package com.greenpocket.user.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.auth.CurrentUserIdArgumentResolver;
import com.greenpocket.global.auth.DemoKeyAuthenticationInterceptor;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.user.dto.UserBootstrapResponse;
import com.greenpocket.user.dto.UserStartRequest;
import com.greenpocket.user.dto.UserStartResponse;
import com.greenpocket.user.exception.UserErrorCode;
import com.greenpocket.user.service.UserService;

class UserControllerTest {

	private static final String DEMO_KEY = "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41";
	private static final Long USER_ID = 1L;

	private UserService userService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		userService = mock(UserService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void returnsCreatedForNewUser() throws Exception {
		UserStartRequest request = new UserStartRequest(DEMO_KEY, "김수현");
		when(userService.start(request)).thenReturn(new UserService.UserStartResult(startResponse(), true));

		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"demoKey":"%s","name":"김수현"}
					""".formatted(DEMO_KEY)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.nextScreen").value("ONB-02"))
			.andExpect(jsonPath("$.data.pocketAccountNo").value("1005-1234-5678-90"));
	}

	@Test
	void returnsOkForExistingDemoKey() throws Exception {
		UserStartRequest request = new UserStartRequest(DEMO_KEY, "김수현");
		when(userService.start(request)).thenReturn(new UserService.UserStartResult(startResponse(), false));

		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"demoKey":"%s","name":"김수현"}
					""".formatted(DEMO_KEY)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("김수현"));
	}

	@Test
	void rejectsMalformedDemoKey() throws Exception {
		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"demoKey":"not-a-uuid","name":"김수현"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.error.field").value("demoKey"));
	}

	@Test
	void returnsNameInvalid() throws Exception {
		UserStartRequest request = new UserStartRequest(DEMO_KEY, "!@#$");
		when(userService.start(request)).thenThrow(new BusinessException(UserErrorCode.NAME_INVALID, "name", null));

		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"demoKey":"%s","name":"!@#$"}
					""".formatted(DEMO_KEY)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.code").value("NAME_INVALID"))
			.andExpect(jsonPath("$.error.field").value("name"));
	}

	@Test
	void returnsBootstrapForAuthenticatedUser() throws Exception {
		when(userService.getBootstrap(USER_ID)).thenReturn(new UserBootstrapResponse(
			USER_ID,
			"김수현",
			true,
			EcoLinkStatus.LINKED,
			OffsetDateTime.parse("2026-09-01T09:00:00+09:00"),
			true,
			OffsetDateTime.parse("2026-09-01T09:12:00+09:00"),
			true,
			7L,
			"WF-06"
		));

		mockMvc.perform(get("/api/v1/users/me")
				.requestAttr(DemoKeyAuthenticationInterceptor.CURRENT_USER_ID_ATTRIBUTE, USER_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ecoLinkStatus").value("LINKED"))
			.andExpect(jsonPath("$.data.hasBill").value(true))
			.andExpect(jsonPath("$.data.currentRoundId").value(7))
			.andExpect(jsonPath("$.data.entryScreen").value("WF-06"));
	}

	@Test
	void rejectsBootstrapWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED_DEMO_KEY"));
	}

	private UserStartResponse startResponse() {
		return new UserStartResponse(
			USER_ID,
			"김수현",
			false,
			"ONB-02",
			"1005-1234-5678-90",
			"김수현",
			OffsetDateTime.parse("2026-09-03T18:30:00+09:00")
		);
	}
}
