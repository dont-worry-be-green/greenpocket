package com.greenpocket.diagnosis.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum DiagnosisErrorCode implements ErrorCode {

	DIAGNOSIS_MONTH_EMPTY(HttpStatus.NOT_FOUND, "해당 월에 등록된 고지서가 없어요.");

	private final HttpStatus status;
	private final String message;

	DiagnosisErrorCode(HttpStatus status, String message) {
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
