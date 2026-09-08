package com.greenpocket.policy.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum PolicyErrorCode implements ErrorCode {

	YOUTH_POLICY_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "청년정책 데이터를 준비하고 있어요. 잠시 후 다시 시도해 주세요."),
	YOUTH_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "청년정책을 찾을 수 없어요.");

	private final HttpStatus status;
	private final String message;

	PolicyErrorCode(HttpStatus status, String message) {
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
