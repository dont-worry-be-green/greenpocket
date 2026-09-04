package com.greenpocket.pocket.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.pocket.dto.ConvertibleMileageResponse;
import com.greenpocket.pocket.dto.PocketBalanceResponse;
import com.greenpocket.pocket.dto.PocketMainResponse;
import com.greenpocket.pocket.dto.PocketManagementResponse;
import com.greenpocket.pocket.dto.PocketTransactionListResponse;
import com.greenpocket.pocket.entity.TransactionDirection;
import com.greenpocket.pocket.entity.TransactionType;
import com.greenpocket.pocket.service.PocketQueryService;

@Tag(name = "Pocket", description = "그린포켓 메인·잔액·마일리지·거래 내역 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pocket")
public class PocketQueryController {

	private final PocketQueryService pocketQueryService;

	@Operation(summary = "포켓 메인 조회", description = "잔액, 적립 구분, 전환 가능 마일리지와 최근 적립 내역을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping
	public ApiResponse<PocketMainResponse> getPocket(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(pocketQueryService.getPocket(userId));
	}

	@Operation(summary = "포켓 잔액 조회", description = "완료된 거래 원장에서 현재 잔액을 계산합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/balance")
	public ApiResponse<PocketBalanceResponse> getBalance(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(pocketQueryService.getBalance(userId));
	}

	@Operation(summary = "전환 가능 마일리지 조회", description = "확정됐지만 아직 전환되지 않은 평가 회차의 마일리지를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/convertible-mileage")
	public ApiResponse<ConvertibleMileageResponse> getConvertibleMileage(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(pocketQueryService.getConvertibleMileage(userId));
	}

	@Operation(summary = "포켓 거래 내역 조회", description = "거래를 월별로 묶어 최신순으로 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필터 또는 페이징 값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/transactions")
	public ApiResponse<PocketTransactionListResponse> getTransactions(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "거래 방향 필터", example = "CREDIT")
		@RequestParam(required = false) TransactionDirection direction,
		@Parameter(description = "거래 유형 필터", example = "ECO_MILEAGE")
		@RequestParam(required = false) TransactionType type,
		@Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@Parameter(description = "페이지 크기(1~100)", example = "20")
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(pocketQueryService.getTransactions(userId, direction, type, page, size));
	}

	@Operation(summary = "포켓 관리 화면 조회", description = "그린포켓 정보, 출금 계좌와 최근 출금 3건을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/management")
	public ApiResponse<PocketManagementResponse> getManagement(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(pocketQueryService.getManagement(userId));
	}
}
