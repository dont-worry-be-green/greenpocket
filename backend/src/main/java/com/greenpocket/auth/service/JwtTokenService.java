package com.greenpocket.auth.service;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.greenpocket.auth.config.AuthProperties;
import com.greenpocket.global.exception.BusinessException;
import com.greenpocket.global.exception.CommonErrorCode;

@Service
public class JwtTokenService {

	private static final int MINIMUM_HS256_KEY_BYTES = 32;

	private final AuthProperties properties;
	private final Clock clock;
	private final byte[] secret;

	public JwtTokenService(AuthProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.secret = decodeSecret(properties.jwtSecretBase64());
		if (properties.accessExpirationSeconds() <= 0) {
			throw new IllegalStateException("JWT_ACCESS_EXPIRATION_SECONDS must be positive");
		}
	}

	public String issueAccessToken(Long userId) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plusSeconds(properties.accessExpirationSeconds());
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.subject(userId.toString())
			.issueTime(Date.from(issuedAt))
			.expirationTime(Date.from(expiresAt))
			.jwtID(UUID.randomUUID().toString())
			.build();
		SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

		try {
			signedJwt.sign(new MACSigner(secret));
			return signedJwt.serialize();
		}
		catch (JOSEException exception) {
			throw new IllegalStateException("Access Token 서명에 실패했습니다.", exception);
		}
	}

	public Long verifyAndGetUserId(String token) {
		if (!StringUtils.hasText(token)) {
			throw unauthenticated();
		}

		try {
			SignedJWT signedJwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(signedJwt.getHeader().getAlgorithm())
				|| !signedJwt.verify(new MACVerifier(secret))) {
				throw unauthenticated();
			}

			JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
			Date expirationTime = claims.getExpirationTime();
			if (expirationTime == null || !StringUtils.hasText(claims.getSubject())) {
				throw unauthenticated();
			}
			if (!expirationTime.toInstant().isAfter(clock.instant())) {
				throw new BusinessException(CommonErrorCode.ACCESS_TOKEN_EXPIRED);
			}

			Long userId = Long.valueOf(claims.getSubject());
			if (userId <= 0) {
				throw unauthenticated();
			}
			return userId;
		}
		catch (BusinessException exception) {
			throw exception;
		}
		catch (ParseException | JOSEException | NumberFormatException exception) {
			throw unauthenticated();
		}
	}

	public long accessExpirationSeconds() {
		return properties.accessExpirationSeconds();
	}

	private static byte[] decodeSecret(String encodedSecret) {
		try {
			byte[] decoded = Base64.getDecoder().decode(encodedSecret == null ? "" : encodedSecret);
			if (decoded.length < MINIMUM_HS256_KEY_BYTES) {
				throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes");
			}
			return decoded;
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64", exception);
		}
	}

	private static BusinessException unauthenticated() {
		return new BusinessException(CommonErrorCode.UNAUTHENTICATED);
	}
}
