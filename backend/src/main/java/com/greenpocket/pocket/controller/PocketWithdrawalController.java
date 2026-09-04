package com.greenpocket.pocket.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.pocket.dto.PocketWithdrawalHistoryResponse;
import com.greenpocket.pocket.dto.PocketWithdrawalRequest;
import com.greenpocket.pocket.dto.PocketWithdrawalResponse;
import com.greenpocket.pocket.service.PocketWithdrawalService;
import com.greenpocket.pocket.service.PocketWithdrawalService.WithdrawalExecution;

@Tag(name = "Pocket Withdrawals", description = "포켓 출금 신청·내역 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pocket/withdrawals")
public class PocketWithdrawalController {

	private final PocketWithdrawalService pocketWithdrawalService;

	@Operation(summary = "출금 신청", description = "멱등키로 중복 출금을 방지하고 모의 출금을 완료 처리합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "출금 신청 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "같은 멱등키 재요청"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "출금 금액 또는 요청 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "출금 계좌 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "출금 계좌 미등록 또는 잔액 부족")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<PocketWithdrawalResponse>> withdraw(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "출금 중복 방지 키", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
		@RequestHeader(value = "Idempotency-Key", required = false)
		@NotBlank @Size(max = 100) String idempotencyKey,
		@RequestBody PocketWithdrawalRequest request
	) {
		WithdrawalExecution execution = pocketWithdrawalService.withdraw(userId, idempotencyKey, request);
		return ResponseEntity.status(execution.repeated() ? HttpStatus.OK : HttpStatus.CREATED)
			.body(ApiResponse.success(execution.response()));
	}

	@Operation(summary = "출금 내역 조회")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "페이징 값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping
	public ApiResponse<PocketWithdrawalHistoryResponse> findWithdrawals(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(pocketWithdrawalService.findWithdrawals(userId, page, size));
	}
}
