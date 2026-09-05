package com.greenpocket.user.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegionListResponse(
	@Schema(example = "SIGUNGU") Level level,
	List<Item> items
) {
	public enum Level {
		SIDO,
		SIGUNGU
	}

	public record Item(
		@Schema(example = "11620") String code,
		@Schema(example = "관악구") String name,
		@Schema(example = "11") String sidoCode,
		@Schema(example = "true") boolean hasRegionAverage
	) {
	}
}
