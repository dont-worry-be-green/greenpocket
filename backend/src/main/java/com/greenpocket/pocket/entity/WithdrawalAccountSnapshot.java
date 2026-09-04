package com.greenpocket.pocket.entity;

public record WithdrawalAccountSnapshot(
	String bankName,
	String accountNo,
	String holder
) {
}
