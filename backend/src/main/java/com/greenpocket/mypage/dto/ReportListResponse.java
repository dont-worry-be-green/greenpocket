package com.greenpocket.mypage.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ReportListResponse(
	List<Item> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean hasNext
) {

	public record Item(
		String reportId,
		ReportType type,
		String yearMonth,
		String title,
		OffsetDateTime createdAt,
		String targetScreen,
		Map<String, Object> targetParams,
		boolean downloadable
	) {
	}
}
