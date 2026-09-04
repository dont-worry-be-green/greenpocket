package com.greenpocket.pocket.dto;

import tools.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

public record PocketWithdrawalRequest(
	@Schema(type = "integer", format = "int64", example = "30000", requiredMode = Schema.RequiredMode.REQUIRED)
	JsonNode amount,
	@Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
	Long accountId
) {
}
