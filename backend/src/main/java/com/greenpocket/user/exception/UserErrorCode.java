package com.greenpocket.user.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

	NAME_INVALID(HttpStatus.BAD_REQUEST, "이름은 공백을 제외하고 1~20자의 문자 또는 숫자를 포함해 입력해 주세요."),
	BIRTH_DATE_INVALID(HttpStatus.BAD_REQUEST, "생년월일을 확인해 주세요."),
	GENDER_REQUIRED(HttpStatus.BAD_REQUEST, "성별을 입력해 주세요."),
	PHONE_NUMBER_INVALID(HttpStatus.BAD_REQUEST, "휴대전화번호를 확인해 주세요."),
	PHONE_NUMBER_ALREADY_USED(HttpStatus.CONFLICT, "이미 가입된 휴대전화번호예요.");

	private final HttpStatus status;
	private final String message;

	UserErrorCode(HttpStatus status, String message) {
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
