package com.greenpocket.pocket.dto;

import java.util.List;

public record PocketTransactionListResponse(
	Long totalCreditAmount,
	Long balance,
	Long convertibleMileage,
	List<Group> groups,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {
	public PocketTransactionListResponse {
		groups = List.copyOf(groups);
	}

	public record Group(
		String yearMonth,
		Long subtotal,
		List<PocketTransactionItemResponse> items
	) {
		public Group {
			items = List.copyOf(items);
		}
	}
}
