package com.greenpocket.bill.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum BillErrorCode implements ErrorCode {

	BILL_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 고지서 항목이 있어요."),
	BILL_ITEM_EMPTY(HttpStatus.BAD_REQUEST, "등록할 고지서 항목을 하나 이상 입력해 주세요."),
	BILL_USAGE_REQUIRED(HttpStatus.BAD_REQUEST, "사용량을 입력해 주세요."),
	BILL_ELECTRICITY_REQUIRED(HttpStatus.BAD_REQUEST, "직접 입력할 때는 전기 항목이 필요해요.");

	private final HttpStatus status;
	private final String message;

	BillErrorCode(HttpStatus status, String message) {
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
