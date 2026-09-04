package com.greenpocket.pocket.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountCreateRequest;
import com.greenpocket.pocket.dto.WithdrawalAccountDefaultResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountListResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountResponse;
import com.greenpocket.pocket.dto.WithdrawalAccountUpdateRequest;
import com.greenpocket.pocket.service.WithdrawalAccountService;

@Tag(name = "Pocket Accounts", description = "포켓 출금 계좌 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pocket/accounts")
public class WithdrawalAccountController {

	private final WithdrawalAccountService withdrawalAccountService;

	@Operation(summary = "출금 계좌 목록 조회")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping
	public ApiResponse<WithdrawalAccountListResponse> findAccounts(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(withdrawalAccountService.findAccounts(userId));
	}

	@Operation(summary = "출금 계좌 등록")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<WithdrawalAccountResponse>> createAccount(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Valid @RequestBody WithdrawalAccountCreateRequest request
	) {
		WithdrawalAccountResponse response = withdrawalAccountService.createAccount(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@Operation(summary = "출금 계좌 수정")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "출금 계좌 없음")
	})
	@PutMapping("/{accountId}")
	public ApiResponse<WithdrawalAccountResponse> updateAccount(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "출금 계좌 ID", example = "3", required = true)
		@PathVariable Long accountId,
		@Valid @RequestBody WithdrawalAccountUpdateRequest request
	) {
		return ApiResponse.success(withdrawalAccountService.updateAccount(userId, accountId, request));
	}

	@Operation(summary = "기본 출금 계좌 지정")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기본 계좌 지정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "출금 계좌 없음")
	})
	@PutMapping("/{accountId}/default")
	public ApiResponse<WithdrawalAccountDefaultResponse> makeDefault(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "출금 계좌 ID", example = "3", required = true)
		@PathVariable Long accountId
	) {
		return ApiResponse.success(withdrawalAccountService.makeDefault(userId, accountId));
	}
}
