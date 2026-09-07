package com.greenpocket.global.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.auth.service.JwtTokenService;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;
import com.greenpocket.global.exception.GlobalExceptionHandler;
import com.greenpocket.global.response.ApiResponse;

class ApiAuthenticationInterceptorTest {

	private static final String DEMO_KEY = "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41";

	@Test
	void bearerTokenResolvesCurrentUser() throws Exception {
		JwtTokenService jwtTokenService = mock(JwtTokenService.class);
		when(jwtTokenService.verifyAndGetUserId("valid-token")).thenReturn(42L);
		MockMvc mockMvc = mockMvc(jwtTokenService, false, mock(DemoUserLookup.class));

		mockMvc.perform(get("/api/v1/test-jwt-authentication")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId").value(42));
	}

	@Test
	void missingAuthenticationReturnsGeneralUnauthenticatedCode() throws Exception {
		MockMvc mockMvc = mockMvc(mock(JwtTokenService.class), false, mock(DemoUserLookup.class));

		mockMvc.perform(get("/api/v1/test-jwt-authentication"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void expiredBearerTokenKeepsDedicatedErrorCode() throws Exception {
		JwtTokenService jwtTokenService = mock(JwtTokenService.class);
		when(jwtTokenService.verifyAndGetUserId("expired-token"))
			.thenThrow(new BusinessException(CommonErrorCode.ACCESS_TOKEN_EXPIRED));
		MockMvc mockMvc = mockMvc(jwtTokenService, false, mock(DemoUserLookup.class));

		mockMvc.perform(get("/api/v1/test-jwt-authentication")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("ACCESS_TOKEN_EXPIRED"));
	}

	@Test
	void demoKeyFallbackWorksOnlyWhenEnabled() throws Exception {
		DemoUserLookup demoUserLookup = mock(DemoUserLookup.class);
		when(demoUserLookup.findUserIdByDemoKey(DEMO_KEY)).thenReturn(Optional.of(77L));
		MockMvc mockMvc = mockMvc(mock(JwtTokenService.class), true, demoUserLookup);

		mockMvc.perform(get("/api/v1/test-jwt-authentication")
				.header(DemoKeyAuthenticationInterceptor.DEMO_KEY_HEADER, DEMO_KEY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.userId").value(77));
	}

	private static MockMvc mockMvc(
		JwtTokenService jwtTokenService,
		boolean demoEnabled,
		DemoUserLookup demoUserLookup
	) {
		AuthProperties properties = new AuthProperties("unused", 1800, 1209600, false, demoEnabled);
		ApiAuthenticationInterceptor interceptor = new ApiAuthenticationInterceptor(
			jwtTokenService,
			properties,
			demoUserLookup
		);
		return MockMvcBuilders.standaloneSetup(new AuthenticationTestController())
			.addInterceptors(interceptor)
			.setCustomArgumentResolvers(new CurrentUserIdArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@RestController
	private static class AuthenticationTestController {

		@GetMapping("/api/v1/test-jwt-authentication")
		ApiResponse<CurrentUserResponse> authenticate(@CurrentUserId Long userId) {
			return ApiResponse.success(new CurrentUserResponse(userId));
		}
	}

	private record CurrentUserResponse(Long userId) {
	}
}
