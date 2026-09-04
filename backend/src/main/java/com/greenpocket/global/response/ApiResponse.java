package com.greenpocket.global.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ApiResponse<T>(
	boolean success,
	T data,
	ApiError error,
	OffsetDateTime timestamp
) {

	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, now());
	}

	public static <T> ApiResponse<T> failure(ApiError error) {
		return new ApiResponse<>(false, null, error, now());
	}

	private static OffsetDateTime now() {
		return OffsetDateTime.now(KOREA_ZONE_ID);
	}
}
