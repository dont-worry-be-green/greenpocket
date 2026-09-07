package com.greenpocket.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
	@Schema(description = "가입한 이메일", example = "user@example.com")
	String email,
	@Schema(description = "가입 시 설정한 비밀번호", example = "green1234")
	String password
) {
}
