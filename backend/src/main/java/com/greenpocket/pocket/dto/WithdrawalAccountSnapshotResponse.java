package com.greenpocket.pocket.dto;

public record WithdrawalAccountSnapshotResponse(
	String bankName,
	String accountNo,
	String holder
) {
}
