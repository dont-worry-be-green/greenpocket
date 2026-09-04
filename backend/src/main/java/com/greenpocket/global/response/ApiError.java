package com.greenpocket.global.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.greenpocket.global.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
	String code,
	String message,
	String field,
	Map<String, Object> details
) {

	public ApiError {
		details = details == null ? null : Map.copyOf(details);
	}

	public static ApiError of(ErrorCode errorCode) {
		return new ApiError(errorCode.code(), errorCode.message(), null, null);
	}

	public static ApiError of(ErrorCode errorCode, String field, Map<String, Object> details) {
		return new ApiError(errorCode.code(), errorCode.message(), field, details);
	}
}
