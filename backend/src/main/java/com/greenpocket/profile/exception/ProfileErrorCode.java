package com.greenpocket.profile.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum ProfileErrorCode implements ErrorCode {

	PROFILE_INCOMPLETE(HttpStatus.CONFLICT, "필수 프로필 정보를 모두 입력해 주세요."),
	REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 서울시 자치구를 찾을 수 없어요."),
	BIRTH_DATE_INVALID(HttpStatus.BAD_REQUEST, "생년월일을 다시 확인해 주세요."),
	POLICY_INTEREST_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "관심 분야는 최대 3개까지 선택할 수 있어요.");

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
