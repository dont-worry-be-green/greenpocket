package com.greenpocket.greenlife.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum GreenlifeErrorCode implements ErrorCode {

	GREENLIFE_NOT_PARTICIPATING(HttpStatus.CONFLICT, "녹색생활실천 참여 연동을 먼저 완료해 주세요."),
	GREENLIFE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "실천항목을 찾을 수 없어요.");

	private final HttpStatus status;
	private final String message;

	GreenlifeErrorCode(HttpStatus status, String message) {
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
