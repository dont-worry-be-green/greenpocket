package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;

public record WithdrawalAccountResponse(
	Long accountId,
	String bankCode,
	String bankName,
	String accountNo,
	String holder,
	boolean isDefault,
	boolean isActive,
	OffsetDateTime verifiedAt
) {
}
