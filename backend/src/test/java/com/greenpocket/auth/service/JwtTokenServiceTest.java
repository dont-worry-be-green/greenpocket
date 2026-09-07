package com.greenpocket.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

class JwtTokenServiceTest {

	private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
	private static final String SECRET = Base64.getEncoder().encodeToString(
		"0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
	);

	@Test
	void issuesAndVerifiesAccessToken() {
		JwtTokenService service = service(SECRET, "2026-09-07T00:00:00Z", 1800);

		String token = service.issueAccessToken(42L);

		assertEquals(42L, service.verifyAndGetUserId(token));
	}

	@Test
	void rejectsExpiredAccessTokenWithDedicatedCode() {
		String token = service(SECRET, "2026-09-07T00:00:00Z", 1).issueAccessToken(42L);
		JwtTokenService verifier = service(SECRET, "2026-09-07T00:00:02Z", 1800);

		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> verifier.verifyAndGetUserId(token)
		);

		assertEquals(CommonErrorCode.ACCESS_TOKEN_EXPIRED, exception.getErrorCode());
	}

	@Test
	void rejectsTokenSignedWithDifferentSecret() {
		String token = service(SECRET, "2026-09-07T00:00:00Z", 1800).issueAccessToken(42L);
		String otherSecret = Base64.getEncoder().encodeToString(
			"abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8)
		);

		BusinessException exception = assertThrows(
			BusinessException.class,
			() -> service(otherSecret, "2026-09-07T00:00:00Z", 1800).verifyAndGetUserId(token)
		);

		assertEquals(CommonErrorCode.UNAUTHENTICATED, exception.getErrorCode());
	}

	@Test
	void rejectsSecretShorterThanHs256Minimum() {
		String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));

		assertThrows(IllegalStateException.class, () -> service(shortSecret, "2026-09-07T00:00:00Z", 1800));
	}

	private static JwtTokenService service(String secret, String instant, long expirationSeconds) {
		AuthProperties properties = new AuthProperties(secret, expirationSeconds, 1209600, false, false);
		Clock clock = Clock.fixed(Instant.parse(instant), KOREA_ZONE);
		return new JwtTokenService(properties, clock);
	}
}
