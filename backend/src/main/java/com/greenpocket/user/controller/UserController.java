package com.greenpocket.user.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.user.dto.UserBootstrapResponse;
import com.greenpocket.user.service.UserService;

@Tag(name = "Common Users", description = "현재 회원의 앱 부트스트랩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@Operation(summary = "앱 부트스트랩", description = "현재 사용자의 상태를 한 번에 조회해 앱 진입 화면을 결정합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "현재 사용자 상태 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Access Token 인증 실패")
	})
	@GetMapping("/me")
	public ApiResponse<UserBootstrapResponse> getBootstrap(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(userService.getBootstrap(userId));
	}
}
