package com.greenpocket.pocket.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum PocketErrorCode implements ErrorCode {

	POCKET_ACCOUNT_REQUIRED(HttpStatus.CONFLICT, "출금 계좌를 먼저 등록해 주세요."),
	POCKET_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 출금 계좌를 찾을 수 없어요."),
	POCKET_INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "출금 가능한 잔액이 부족해요."),
	POCKET_AMOUNT_INVALID(HttpStatus.BAD_REQUEST, "출금 금액은 0원보다 큰 정수여야 해요."),
	CONVERSION_NOT_AVAILABLE(HttpStatus.CONFLICT, "현재 전환 가능한 마일리지가 없어요."),
	CONVERSION_ALREADY_DONE(HttpStatus.CONFLICT, "이미 전환한 평가 회차예요."),
	CONVERSION_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "마일리지 전환은 1일 1회만 가능해요."),
	CONVERSION_NOT_RETURNED(HttpStatus.CONFLICT, "외부 전환 화면을 거친 뒤 다시 시도해 주세요.");

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
