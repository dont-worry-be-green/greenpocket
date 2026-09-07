package com.greenpocket.auth.dto;

public record SignupResponse(
	Long userId,
	String email,
	String name,
	boolean onboardingCompleted,
	String nextScreen,
	String accessToken,
	String tokenType,
	long expiresIn
) {
}
