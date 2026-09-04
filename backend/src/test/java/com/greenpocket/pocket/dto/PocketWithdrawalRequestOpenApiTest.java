package com.greenpocket.pocket.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;

class PocketWithdrawalRequestOpenApiTest {

	@Test
	void documentsAmountAsInteger() {
		Schema<?> requestSchema = ModelConverters.getInstance()
			.read(PocketWithdrawalRequest.class)
			.get("PocketWithdrawalRequest");

		Schema<?> amountSchema = requestSchema.getProperties().get("amount");

		assertThat(amountSchema.getType()).isEqualTo("integer");
		assertThat(amountSchema.getFormat()).isEqualTo("int64");
		assertThat(amountSchema.getExample()).isInstanceOf(Number.class);
		assertThat(((Number) amountSchema.getExample()).longValue()).isEqualTo(30_000L);
	}
}
