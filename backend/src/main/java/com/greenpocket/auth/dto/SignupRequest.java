package com.greenpocket.auth.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.greenpocket.user.entity.Gender;

public record SignupRequest(
	@Schema(description = "로그인 이메일. 공백 제거 후 소문자로 저장합니다.", example = "user@example.com")
	String email,
	@Schema(description = "8자 이상, UTF-8 기준 72바이트 이하 비밀번호", example = "green1234")
	String password,
	@Schema(description = "공백 제거 후 1~20자이며 문자 또는 숫자를 포함하는 이름", example = "김수현")
	String name,
	@Schema(description = "본인인증으로 확인한 생년월일", example = "1998-03-15")
	LocalDate birthDate,
	@Schema(description = "본인인증으로 확인한 성별", example = "FEMALE")
	Gender gender,
	@Schema(description = "본인인증으로 확인한 휴대전화번호. 숫자만 저장합니다.", example = "01091740339")
	String phoneNumber
) {
}
