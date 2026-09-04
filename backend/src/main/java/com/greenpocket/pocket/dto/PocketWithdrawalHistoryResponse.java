package com.greenpocket.pocket.dto;

import java.util.List;

public record PocketWithdrawalHistoryResponse(
	List<PocketWithdrawalHistoryItemResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
	public PocketWithdrawalHistoryResponse {
		content = List.copyOf(content);
	}
}
