package com.greenpocket.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PocketAccountNumberGeneratorTest {

	private final PocketAccountNumberGenerator generator = new PocketAccountNumberGenerator();

	@Test
	void generatesAccountNumberInGreenPocketFormat() {
		assertThat(generator.generate()).matches("1005-\\d{4}-\\d{4}-\\d{2}");
	}
}
