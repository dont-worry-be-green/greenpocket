package com.greenpocket.auth.controller;

import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.auth.dto.LoginRequest;
import com.greenpocket.auth.dto.LoginResponse;
import com.greenpocket.auth.dto.SignupRequest;
import com.greenpocket.auth.dto.SignupResponse;
import com.greenpocket.auth.dto.TokenRefreshResponse;
import com.greenpocket.auth.service.AuthService;
import com.greenpocket.auth.service.AuthService.AuthSession;
import com.greenpocket.global.response.ApiResponse;

@Tag(name = "Authentication", description = "JWT 회원가입·로그인·토큰 재발급·로그아웃 API")
@SecurityRequirements
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
	private static final String COOKIE_PATH = "/api/v1/auth";

	private final AuthService authService;
	private final AuthProperties properties;

	@Operation(summary = "회원가입", description = "이메일과 비밀번호로 가입하고 Access Token을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 이메일")
	})
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
		@Valid @RequestBody SignupRequest request,
		HttpServletResponse servletResponse
	) {
		AuthSession<SignupResponse> session = authService.signup(request);
		setRefreshCookie(servletResponse, session.refreshToken());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(session.response()));
	}

	@Operation(summary = "로그인", description = "이메일과 비밀번호를 확인하고 새 토큰 세션을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
	})
	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse servletResponse
	) {
		AuthSession<LoginResponse> session = authService.login(request);
		setRefreshCookie(servletResponse, session.refreshToken());
		return ApiResponse.success(session.response());
	}

	@Operation(summary = "Access Token 재발급", description = "Refresh Token 쿠키를 회전하고 새 Access Token을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 오류")
	})
	@PostMapping("/refresh")
	public ApiResponse<TokenRefreshResponse> refresh(
		@Parameter(hidden = true)
		@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
		HttpServletResponse servletResponse
	) {
		AuthSession<TokenRefreshResponse> session = authService.refresh(refreshToken);
		setRefreshCookie(servletResponse, session.refreshToken());
		return ApiResponse.success(session.response());
	}

	@Operation(summary = "로그아웃", description = "현재 Refresh Token을 폐기하고 쿠키를 삭제합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 완료")
	})
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		@Parameter(hidden = true)
		@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
	) {
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
			.build();
	}

	private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString());
	}

	private ResponseCookie refreshCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
			.httpOnly(true)
			.secure(properties.refreshCookieSecure())
			.sameSite("Lax")
			.path(COOKIE_PATH)
			.maxAge(Duration.ofSeconds(properties.refreshExpirationSeconds()))
			.build();
	}

	private ResponseCookie expiredRefreshCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
			.httpOnly(true)
			.secure(properties.refreshCookieSecure())
			.sameSite("Lax")
			.path(COOKIE_PATH)
			.maxAge(Duration.ZERO)
			.build();
	}
}
