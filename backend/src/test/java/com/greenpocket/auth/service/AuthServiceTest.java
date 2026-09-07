package com.greenpocket.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.auth.dto.LoginRequest;
import com.greenpocket.auth.dto.SignupRequest;
import com.greenpocket.auth.exception.AuthErrorCode;
import com.greenpocket.auth.repository.AuthRepository;
import com.greenpocket.auth.repository.AuthRepository.AuthAccountSnapshot;
import com.greenpocket.auth.repository.AuthRepository.RefreshTokenSnapshot;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.user.service.UserService;
import com.greenpocket.user.service.UserService.RegisteredUser;

class AuthServiceTest {

	private static final Long USER_ID = 7L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 7, 9, 0);
	private static final AuthProperties PROPERTIES = new AuthProperties(
		"unused-in-mocked-jwt", 1800, 1209600, false, false
	);

	private AuthRepository authRepository;
	private UserService userService;
	private PasswordEncoder passwordEncoder;
	private JwtTokenService jwtTokenService;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		authRepository = mock(AuthRepository.class);
		userService = mock(UserService.class);
		passwordEncoder = mock(PasswordEncoder.class);
		jwtTokenService = mock(JwtTokenService.class);
		Clock clock = Clock.fixed(Instant.parse("2026-09-07T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		authService = new AuthService(
			authRepository,
			userService,
			passwordEncoder,
			jwtTokenService,
			PROPERTIES,
			new SecureRandom(new byte[]{1, 2, 3, 4}),
			clock
		);
		when(jwtTokenService.issueAccessToken(USER_ID)).thenReturn("access-token");
		when(jwtTokenService.accessExpirationSeconds()).thenReturn(1800L);
	}

	@Test
	void signupNormalizesEmailHashesPasswordAndIssuesSession() {
		when(authRepository.existsByEmail("green@example.com")).thenReturn(false);
		when(userService.createRegisteredUser("김그린"))
			.thenReturn(new RegisteredUser(USER_ID, "김그린", false));
		when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");

		AuthService.AuthSession<?> session = authService.signup(
			new SignupRequest("  GREEN@Example.com ", "password123", "김그린")
		);

		assertEquals("green@example.com", ((com.greenpocket.auth.dto.SignupResponse) session.response()).email());
		assertEquals("ONB-02", ((com.greenpocket.auth.dto.SignupResponse) session.response()).nextScreen());
		verify(authRepository).createAccount(USER_ID, "green@example.com", "bcrypt-hash");
		verify(authRepository).updateLastLoginAt(USER_ID, NOW);
		verifyRefreshTokenStored(session.refreshToken());
	}

	@Test
	void rejectsDuplicateEmailBeforeCreatingUser() {
		when(authRepository.existsByEmail("green@example.com")).thenReturn(true);

		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> authService.signup(new SignupRequest("green@example.com", "password123", "김그린"))
		);

		assertEquals(AuthErrorCode.EMAIL_ALREADY_USED, exception.getErrorCode());
		verify(userService, never()).createRegisteredUser(anyString());
	}

	@Test
	void rejectsMalformedSignupEmailWithDomainCode() {
		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> authService.signup(new SignupRequest("not-an-email", "password123", "김그린"))
		);

		assertEquals(AuthErrorCode.EMAIL_INVALID, exception.getErrorCode());
	}

	@Test
	void rejectsShortSignupPasswordWithDomainCode() {
		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> authService.signup(new SignupRequest("green@example.com", "short", "김그린"))
		);

		assertEquals(AuthErrorCode.PASSWORD_INVALID, exception.getErrorCode());
	}

	@Test
	void loginDoesNotRevealWhetherEmailOrPasswordWasWrong() {
		when(authRepository.findAccountByEmail("green@example.com")).thenReturn(Optional.of(
			new AuthAccountSnapshot(USER_ID, "green@example.com", "bcrypt-hash", "김그린", true)
		));
		when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> authService.login(new LoginRequest("green@example.com", "wrong-password"))
		);

		assertEquals(AuthErrorCode.AUTH_CREDENTIALS_INVALID, exception.getErrorCode());
	}

	@Test
	void refreshRevokesOldTokenAndStoresRotatedToken() {
		when(authRepository.findRefreshTokenByHash(anyString())).thenReturn(Optional.of(
			new RefreshTokenSnapshot(10L, USER_ID, NOW.plusDays(1), null)
		));
		when(authRepository.revokeRefreshToken(10L, NOW)).thenReturn(1);

		AuthService.AuthSession<?> session = authService.refresh("old-refresh-token");

		verify(authRepository).revokeRefreshToken(10L, NOW);
		assertNotEquals("old-refresh-token", session.refreshToken());
		verifyRefreshTokenStored(session.refreshToken());
	}

	@Test
	void refreshTokenReuseRevokesAllActiveSessions() {
		when(authRepository.findRefreshTokenByHash(anyString())).thenReturn(Optional.of(
			new RefreshTokenSnapshot(10L, USER_ID, NOW.plusDays(1), NOW.minusMinutes(1))
		));

		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> authService.refresh("reused-refresh-token")
		);

		assertEquals(AuthErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
		verify(authRepository).revokeAllActiveRefreshTokens(USER_ID, NOW);
	}

	@Test
	void logoutWithoutCookieIsIdempotent() {
		authService.logout(null);

		verify(authRepository, never()).findRefreshTokenByHash(anyString());
	}

	private void verifyRefreshTokenStored(String rawRefreshToken) {
		ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
		verify(authRepository).saveRefreshToken(
			eq(USER_ID),
			hashCaptor.capture(),
			eq(NOW.plusSeconds(PROPERTIES.refreshExpirationSeconds()))
		);
		assertEquals(64, hashCaptor.getValue().length());
		assertNotEquals(rawRefreshToken, hashCaptor.getValue());
	}
}
