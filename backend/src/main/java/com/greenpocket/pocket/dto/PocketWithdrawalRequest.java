package com.greenpocket.pocket.dto;

import tools.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

public record PocketWithdrawalRequest(
	@Schema(implementation = Long.class, example = "30000", requiredMode = Schema.RequiredMode.REQUIRED)
	JsonNode amount,
	@Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
	Long accountId
) {
}
