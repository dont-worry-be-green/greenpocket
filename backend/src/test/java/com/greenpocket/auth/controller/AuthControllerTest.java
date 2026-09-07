package com.greenpocket.auth.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.auth.dto.SignupRequest;
import com.greenpocket.auth.dto.SignupResponse;
import com.greenpocket.auth.dto.TokenRefreshResponse;
import com.greenpocket.auth.service.AuthService;
import com.greenpocket.auth.service.AuthService.AuthSession;
import com.greenpocket.global.exception.GlobalExceptionHandler;

class AuthControllerTest {

	private AuthService authService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		authService = mock(AuthService.class);
		AuthProperties properties = new AuthProperties("unused", 1800, 1209600, false, false);
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, properties))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	void signupReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
		SignupRequest request = new SignupRequest("green@example.com", "password123", "김그린");
		SignupResponse response = new SignupResponse(
			7L, "green@example.com", "김그린", false, "ONB-02", "access-token", "Bearer", 1800
		);
		when(authService.signup(request)).thenReturn(new AuthSession<>(response, "refresh-token"));

		mockMvc.perform(post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"email":"green@example.com","password":"password123","name":"김그린"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
				org.hamcrest.Matchers.containsString("refreshToken=refresh-token"),
				org.hamcrest.Matchers.containsString("HttpOnly"),
				org.hamcrest.Matchers.containsString("SameSite=Lax"),
				org.hamcrest.Matchers.containsString("Path=/api/v1/auth")
			)));
	}

	@Test
	void refreshReadsCookieAndRotatesIt() throws Exception {
		TokenRefreshResponse response = new TokenRefreshResponse("new-access", "Bearer", 1800);
		when(authService.refresh("old-refresh")).thenReturn(new AuthSession<>(response, "new-refresh"));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie(AuthController.REFRESH_TOKEN_COOKIE, "old-refresh")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").value("new-access"))
			.andExpect(header().string(HttpHeaders.SET_COOKIE,
				org.hamcrest.Matchers.containsString("refreshToken=new-refresh")));
	}

	@Test
	void logoutIsIdempotentAndDeletesCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isNoContent())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
				org.hamcrest.Matchers.containsString("refreshToken="),
				org.hamcrest.Matchers.containsString("Max-Age=0")
			)));

		verify(authService).logout(null);
	}
}
