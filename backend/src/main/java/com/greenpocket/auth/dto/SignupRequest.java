package com.greenpocket.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupRequest(
	@Schema(description = "로그인 이메일. 공백 제거 후 소문자로 저장합니다.", example = "user@example.com")
	String email,
	@Schema(description = "8자 이상, UTF-8 기준 72바이트 이하 비밀번호", example = "green1234")
	String password,
	@Schema(description = "공백 제거 후 1~20자이며 문자 또는 숫자를 포함하는 이름", example = "김수현")
	String name
) {
}
