package com.greenpocket.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserStartRequest(
	@Schema(description = "프론트엔드에서 생성한 UUID v4 Demo Key", example = "9f2c1a7e-4b30-4c88-9a11-6d0e5b7c2f41")
	@NotBlank
	@Pattern(regexp = "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
	String demoKey,

	@Schema(description = "공백 제거 후 1~20자이며 문자 또는 숫자를 포함하는 이름", example = "김수현")
	String name
) {
}
