package com.greenpocket.global.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String field;
	private final Map<String, Object> details;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.message(), null, null);
	}

	public BusinessException(ErrorCode errorCode, String field, Map<String, Object> details) {
		this(errorCode, errorCode.message(), field, details);
	}

	public BusinessException(
		ErrorCode errorCode,
		String message,
		String field,
		Map<String, Object> details
	) {
		super(message);
		this.errorCode = errorCode;
		this.field = field;
		this.details = details == null ? null : Map.copyOf(details);
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public String getField() {
		return field;
	}

	public Map<String, Object> getDetails() {
		return details;
	}
}
