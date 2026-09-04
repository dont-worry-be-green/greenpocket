package com.greenpocket.pocket.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum PocketErrorCode implements ErrorCode {

	POCKET_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 출금 계좌를 찾을 수 없어요.");

	private final HttpStatus status;
	private final String message;

	PocketErrorCode(HttpStatus status, String message) {
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
