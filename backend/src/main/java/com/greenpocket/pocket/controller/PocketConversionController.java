package com.greenpocket.pocket.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.pocket.dto.PocketConversionCompleteResponse;
import com.greenpocket.pocket.dto.PocketConversionRequest;
import com.greenpocket.pocket.dto.PocketConversionStartResponse;
import com.greenpocket.pocket.service.PocketConversionService;

@Tag(name = "Pocket Conversions", description = "에코마일리지 현금 전환 시작·완료 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pocket/conversions")
public class PocketConversionController {

	private static final String UUID_V4_PATTERN =
		"(?i)^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

	private final PocketConversionService pocketConversionService;

	@Operation(summary = "마일리지 현금 전환 시작", description = "확정 마일리지와 동의를 검증하고 외부 누리집 이동 기록을 생성합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "전환 요청 생성"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 또는 동의 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "전환 마일리지 없음 또는 회차 중복"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "하루 전환 횟수 초과")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<PocketConversionStartResponse>> start(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Valid @RequestBody PocketConversionRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(pocketConversionService.start(userId, request)));
	}

	@Operation(summary = "마일리지 전환 완료", description = "외부 누리집에서 복귀한 전환 요청을 완료하고 포켓 잔액에 반영합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전환 완료 또는 같은 멱등키 재요청"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "전환 ID 또는 멱등키 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "외부 이동 기록 없음 또는 이미 완료")
	})
	@PostMapping("/{conversionId}/complete")
	public ApiResponse<PocketConversionCompleteResponse> complete(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "전환 시작 API에서 받은 전환 ID", example = "120")
		@PathVariable @Positive Long conversionId,
		@Parameter(description = "완료 중복 방지 UUID v4", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
		@RequestHeader(value = "Idempotency-Key", required = false)
		@NotBlank @Size(max = 100) @Pattern(regexp = UUID_V4_PATTERN) String idempotencyKey
	) {
		return ApiResponse.success(pocketConversionService.complete(userId, conversionId, idempotencyKey).response());
	}
}
