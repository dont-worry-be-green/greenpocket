package com.greenpocket.eco.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum EcoErrorCode implements ErrorCode {

	ECO_NOT_SEOUL(HttpStatus.FORBIDDEN, "서울 거주자만 에코마일리지를 연동할 수 있어요."),
	ECO_NOT_LINKED(HttpStatus.CONFLICT, "에코마일리지를 먼저 연동해 주세요."),
	ECO_LINK_FAILED(HttpStatus.BAD_GATEWAY, "에코마일리지 사용량을 불러오지 못했어요."),
	ECO_ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 평가 회차를 찾을 수 없어요.");

	private final HttpStatus status;
	private final String message;

	EcoErrorCode(HttpStatus status, String message) {
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
