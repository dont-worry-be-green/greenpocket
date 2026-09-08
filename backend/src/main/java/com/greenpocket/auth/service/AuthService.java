package com.greenpocket.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.auth.dto.LoginRequest;
import com.greenpocket.auth.dto.LoginResponse;
import com.greenpocket.auth.dto.SignupRequest;
import com.greenpocket.auth.dto.SignupResponse;
import com.greenpocket.auth.dto.TokenRefreshResponse;
import com.greenpocket.auth.exception.AuthErrorCode;
import com.greenpocket.auth.repository.AuthRepository;
import com.greenpocket.auth.repository.AuthRepository.AuthAccountSnapshot;
import com.greenpocket.auth.repository.AuthRepository.RefreshTokenSnapshot;
import com.greenpocket.eco.entity.EcoLinkStatus;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.user.service.UserService;
import com.greenpocket.user.service.UserService.RegisteredUser;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";
	private static final String ECO_LINK_SCREEN = "WF-01";
	private static final String ECO_LINKING_SCREEN = "WF-02";
	private static final String HOME_SCREEN = "WF-06";
	private static final int REFRESH_TOKEN_BYTES = 32;
	private static final int BCRYPT_MAX_BYTES = 72;
	private static final Pattern EMAIL_PATTERN = Pattern.compile(
		"^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9-]+(?:\\.[A-Z0-9-]+)+$",
		Pattern.CASE_INSENSITIVE
	);

	private final AuthRepository authRepository;
	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final AuthProperties properties;
	private final SecureRandom secureRandom;
	private final Clock clock;

	@Transactional
	public AuthSession<SignupResponse> signup(SignupRequest request) {
		String email = normalizeAndValidateEmail(request.email());
		validateSignupPassword(request.password());
		if (authRepository.existsByEmail(email)) {
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_USED, "email", null);
		}

		RegisteredUser user = userService.createRegisteredUser(
			request.name(), request.birthDate(), request.gender(), request.phoneNumber()
		);
		try {
			authRepository.createAccount(user.userId(), email, passwordEncoder.encode(request.password()));
		}
		catch (DuplicateKeyException exception) {
			throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_USED, "email", null);
		}
		authRepository.updateLastLoginAt(user.userId(), now());

		SessionTokens tokens = issueSession(user.userId());
		SignupResponse response = new SignupResponse(
			user.userId(),
			email,
			user.name(),
			user.onboardingCompleted(),
			ECO_LINK_SCREEN,
			tokens.accessToken(),
			TOKEN_TYPE,
			jwtTokenService.accessExpirationSeconds()
		);
		return new AuthSession<>(response, tokens.refreshToken());
	}

	@Transactional
	public AuthSession<LoginResponse> login(LoginRequest request) {
		String email = normalizeAndValidateEmail(request.email());
		AuthAccountSnapshot account = authRepository.findAccountByEmail(email)
			.orElseThrow(AuthService::invalidCredentials);
		if (!StringUtils.hasText(request.password())
			|| request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES
			|| !passwordEncoder.matches(request.password(), account.passwordHash())) {
			throw invalidCredentials();
		}

		authRepository.updateLastLoginAt(account.userId(), now());
		SessionTokens tokens = issueSession(account.userId());
		LoginResponse response = new LoginResponse(
			account.userId(),
			account.name(),
			account.onboardingCompleted(),
			entryScreen(account.ecoLinkStatus()),
			tokens.accessToken(),
			TOKEN_TYPE,
			jwtTokenService.accessExpirationSeconds()
		);
		return new AuthSession<>(response, tokens.refreshToken());
	}

	@Transactional(noRollbackFor = BusinessException.class)
	public AuthSession<TokenRefreshResponse> refresh(String rawRefreshToken) {
		RefreshTokenSnapshot refreshToken = findRefreshToken(rawRefreshToken);
		LocalDateTime now = now();
		if (refreshToken.revokedAt() != null) {
			authRepository.revokeAllActiveRefreshTokens(refreshToken.userId(), now);
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}
		if (!refreshToken.expiresAt().isAfter(now)) {
			authRepository.revokeRefreshToken(refreshToken.id(), now);
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		if (authRepository.revokeRefreshToken(refreshToken.id(), now) != 1) {
			authRepository.revokeAllActiveRefreshTokens(refreshToken.userId(), now);
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}
		SessionTokens tokens = issueSession(refreshToken.userId());
		return new AuthSession<>(
			new TokenRefreshResponse(
				tokens.accessToken(),
				TOKEN_TYPE,
				jwtTokenService.accessExpirationSeconds()
			),
			tokens.refreshToken()
		);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (!StringUtils.hasText(rawRefreshToken)) {
			return;
		}
		authRepository.findRefreshTokenByHash(hash(rawRefreshToken))
			.ifPresent(token -> authRepository.revokeRefreshToken(token.id(), now()));
	}

	private SessionTokens issueSession(Long userId) {
		if (properties.refreshExpirationSeconds() <= 0) {
			throw new IllegalStateException("JWT_REFRESH_EXPIRATION_SECONDS must be positive");
		}
		String accessToken = jwtTokenService.issueAccessToken(userId);
		String refreshToken = generateRefreshToken();
		authRepository.saveRefreshToken(
			userId,
			hash(refreshToken),
			now().plusSeconds(properties.refreshExpirationSeconds())
		);
		return new SessionTokens(accessToken, refreshToken);
	}

	private RefreshTokenSnapshot findRefreshToken(String rawRefreshToken) {
		if (!StringUtils.hasText(rawRefreshToken)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}
		return authRepository.findRefreshTokenByHash(hash(rawRefreshToken))
			.orElseThrow(() -> new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID));
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
		}
	}

	private static String normalizeAndValidateEmail(String rawEmail) {
		String email = normalizeEmail(rawEmail);
		if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
			throw new BusinessException(AuthErrorCode.EMAIL_INVALID, "email", null);
		}
		return email;
	}

	private static String normalizeEmail(String rawEmail) {
		return rawEmail == null ? "" : rawEmail.strip().toLowerCase(Locale.ROOT);
	}

	private static void validateSignupPassword(String password) {
		int characterCount = password == null ? 0 : password.codePointCount(0, password.length());
		int byteCount = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
		if (characterCount < 8 || byteCount > BCRYPT_MAX_BYTES) {
			throw new BusinessException(AuthErrorCode.PASSWORD_INVALID, "password", null);
		}
	}

	private LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	private static BusinessException invalidCredentials() {
		return new BusinessException(AuthErrorCode.AUTH_CREDENTIALS_INVALID);
	}

	private static String entryScreen(EcoLinkStatus status) {
		return switch (status) {
			case LINKING -> ECO_LINKING_SCREEN;
			case LINKED -> HOME_SCREEN;
			case UNLINKED, FAILED -> ECO_LINK_SCREEN;
		};
	}

	private record SessionTokens(String accessToken, String refreshToken) {
	}

	public record AuthSession<T>(T response, String refreshToken) {
	}
}
