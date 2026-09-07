package com.greenpocket.auth.dto;

public record TokenRefreshResponse(
	String accessToken,
	String tokenType,
	long expiresIn
) {
}
