package com.greenpocket.global.exception;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요."),
	UNAUTHENTICATED_DEMO_KEY(HttpStatus.UNAUTHORIZED, "데모 사용자를 찾을 수 없어요. 처음부터 시작해 주세요."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없어요."),
	CONFLICT(HttpStatus.CONFLICT, "이미 처리된 요청이에요."),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 문제가 생겼어요. 다시 시도해 주세요."),
	EXTERNAL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "시간이 오래 걸리고 있어요. 다시 시도해 주세요.");

	private final HttpStatus status;
	private final String message;

	CommonErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String message() {
		return message;
	}
}
