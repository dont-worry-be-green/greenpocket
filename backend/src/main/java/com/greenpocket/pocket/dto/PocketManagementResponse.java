package com.greenpocket.pocket.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.greenpocket.pocket.entity.TransactionStatus;

public record PocketManagementResponse(
	Pocket pocket,
	List<Account> accounts,
	List<RecentWithdrawal> recentWithdrawals
) {
	public PocketManagementResponse {
		accounts = List.copyOf(accounts);
		recentWithdrawals = List.copyOf(recentWithdrawals);
	}

	public record Pocket(
		String accountNo,
		String holder,
		Long balance
	) {
	}

	public record Account(
		Long accountId,
		String bankName,
		String accountNo,
		boolean isDefault
	) {
	}

	public record RecentWithdrawal(
		Long transactionId,
		Long amount,
		TransactionStatus transactionStatus,
		OffsetDateTime requestedAt
	) {
	}
}
