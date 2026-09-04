package com.greenpocket.pocket.dto;

public record WithdrawalAccountDefaultResponse(
	Long accountId,
	boolean isDefault,
	Long previousDefaultAccountId
) {
}
