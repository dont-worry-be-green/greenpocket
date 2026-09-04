package com.greenpocket.pocket.dto;

import java.util.List;

public record PocketMainResponse(
	Pocket pocket,
	Long balance,
	Breakdown breakdown,
	Long convertibleMileage,
	ConvertibleSource convertibleSource,
	DefaultAccount defaultAccount,
	List<PocketTransactionItemResponse> recentTransactions,
	Empty empty,
	List<String> notices
) {
	public PocketMainResponse {
		recentTransactions = List.copyOf(recentTransactions);
		notices = List.copyOf(notices);
	}

	public record Pocket(
		String accountNo,
		String holder
	) {
	}

	public record Breakdown(
		Long ecoMileage,
		Long greenlife
	) {
	}

	public record ConvertibleSource(
		Long roundId,
		String periodStart,
		String periodEnd
	) {
	}

	public record DefaultAccount(
		Long accountId,
		String bankCode,
		String bankName,
		String accountNo,
		String holder,
		boolean isDefault
	) {
	}

	public record Empty(
		boolean noAccount,
		boolean noTransaction
	) {
	}
}
