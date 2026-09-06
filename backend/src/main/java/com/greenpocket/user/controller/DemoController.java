package com.greenpocket.user.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.user.dto.DemoResetRequest;
import com.greenpocket.user.dto.DemoResetResponse;
import com.greenpocket.user.service.DemoResetService;

@Tag(name = "Common Demo", description = "해커톤 시연 상태 초기화 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/demo")
public class DemoController {

	private final DemoResetService demoResetService;

	@Operation(summary = "데모 초기화", description = "Demo Key 사용자의 데이터만 FK CASCADE로 삭제하고 온보딩 시작 상태로 복원합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "초기화 성공 또는 이미 초기화된 상태"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Demo Key 형식 오류")
	})
	@SecurityRequirements
	@PostMapping("/reset")
	public ApiResponse<DemoResetResponse> reset(
		@RequestBody(
			required = true,
			content = @Content(
				schema = @Schema(implementation = DemoResetRequest.class),
				examples = @ExampleObject(value = "{\"demoKey\":\"84cc0ab0-4fba-477d-8434-fcee3be057ab\"}")
			)
		) @org.springframework.web.bind.annotation.RequestBody DemoResetRequest request
	) {
		return ApiResponse.success(demoResetService.reset(request));
	}
}
