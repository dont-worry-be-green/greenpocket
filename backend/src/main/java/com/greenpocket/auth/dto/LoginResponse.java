package com.greenpocket.auth.dto;

public record LoginResponse(
	Long userId,
	String name,
	boolean onboardingCompleted,
	String entryScreen,
	String accessToken,
	String tokenType,
	long expiresIn
) {
}
