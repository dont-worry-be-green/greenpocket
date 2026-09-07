package com.greenpocket.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.user.dto.UserStartRequest;
import com.greenpocket.user.dto.UserStartResponse;
import com.greenpocket.user.service.UserService;
import com.greenpocket.user.service.UserService.UserStartResult;

@Tag(name = "Demo Users", description = "dev·demo 프로필 전용 데모 사용자 API")
@Profile({"dev", "demo"})
@SecurityRequirements
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class DemoUserController {

	private final UserService userService;

	@Operation(summary = "데모 사용자 시작", description = "이름과 UUID v4 Demo Key로 사용자를 등록하고 그린포켓 계좌를 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신규 사용자 생성"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "같은 Demo Key의 기존 사용자 반환"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Demo Key 또는 이름 입력 오류")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<UserStartResponse>> start(
		@Valid @RequestBody UserStartRequest request
	) {
		UserStartResult result = userService.start(request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(ApiResponse.success(result.response()));
	}
}
