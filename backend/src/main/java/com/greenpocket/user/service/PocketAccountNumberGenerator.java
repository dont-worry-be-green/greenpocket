package com.greenpocket.user.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class PocketAccountNumberGenerator {

	private static final int RANDOM_DIGIT_COUNT = 10;

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate() {
		StringBuilder digits = new StringBuilder(RANDOM_DIGIT_COUNT);
		for (int index = 0; index < RANDOM_DIGIT_COUNT; index++) {
			digits.append(secureRandom.nextInt(10));
		}
		return "1005-%s-%s-%s".formatted(
			digits.substring(0, 4),
			digits.substring(4, 8),
			digits.substring(8, 10)
		);
	}
}
