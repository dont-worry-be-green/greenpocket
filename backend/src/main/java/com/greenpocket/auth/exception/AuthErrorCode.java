package com.greenpocket.auth.exception;

import org.springframework.http.HttpStatus;

import com.greenpocket.global.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

	EMAIL_INVALID(HttpStatus.BAD_REQUEST, "이메일 형식을 확인해 주세요."),
	PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상 72자 이하로 입력해 주세요."),
	EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "이미 가입된 이메일이에요."),
	AUTH_CREDENTIALS_INVALID(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않아요."),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인 유지 시간이 만료됐어요. 다시 로그인해 주세요.");

	private final HttpStatus status;
	private final String message;

	AuthErrorCode(HttpStatus status, String message) {
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
