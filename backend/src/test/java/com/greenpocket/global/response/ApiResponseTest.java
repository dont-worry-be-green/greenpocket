package com.greenpocket.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

	@Test
	void successResponseContainsDataAndKoreaTimestamp() {
		ApiResponse<String> response = ApiResponse.success("ok");

		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo("ok");
		assertThat(response.error()).isNull();
		assertThat(response.timestamp().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
	}
}
