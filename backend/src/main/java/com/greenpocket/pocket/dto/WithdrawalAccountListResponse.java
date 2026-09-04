package com.greenpocket.pocket.dto;

import java.util.List;

public record WithdrawalAccountListResponse(
	List<WithdrawalAccountResponse> accounts
) {
	public WithdrawalAccountListResponse {
		accounts = List.copyOf(accounts);
	}
}
