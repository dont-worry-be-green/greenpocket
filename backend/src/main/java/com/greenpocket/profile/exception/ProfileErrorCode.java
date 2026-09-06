package com.greenpocket.profile.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum ProfileErrorCode implements ErrorCode {

	PROFILE_INCOMPLETE(HttpStatus.CONFLICT, "지역·주거 형태·평수를 모두 입력해 주세요."),
	REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 서울시 자치구를 찾을 수 없어요.");

	private final HttpStatus status;
	private final String message;

	ProfileErrorCode(HttpStatus status, String message) {
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
