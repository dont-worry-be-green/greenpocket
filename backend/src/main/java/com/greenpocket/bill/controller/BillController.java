package com.greenpocket.bill.controller;

import java.time.YearMonth;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.bill.dto.BillCreateRequest;
import com.greenpocket.bill.dto.BillCreateResponse;
import com.greenpocket.bill.dto.BillDuplicateCheckResponse;
import com.greenpocket.bill.dto.BillTargetMonthResponse;
import com.greenpocket.bill.service.BillRegistrationService;
import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.global.type.UtilityType;

@Tag(name = "Bills", description = "생활요금 고지서 등록 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bills")
public class BillController {

	private final BillRegistrationService billRegistrationService;

	@Operation(summary = "등록 대상 고지월 조회", description = "현재 KST 기준 직전 월의 등록 상태와 가장 최근 등록 월을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 대상 월 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/target-month")
	public ApiResponse<BillTargetMonthResponse> getTargetMonth(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(billRegistrationService.getTargetMonth(userId));
	}

	@Operation(summary = "고지서 항목 중복 사전 확인", description = "청구 월과 에너지원별 기존 등록 레코드 ID를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 확인 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "청구 월 또는 에너지원 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/duplicate-check")
	public ApiResponse<BillDuplicateCheckResponse> checkDuplicates(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "청구 월(YYYY-MM)", example = "2026-08", required = true)
		@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth billingMonth,
		@Parameter(description = "쉼표로 구분한 에너지원", example = "ELECTRICITY,WATER", required = true)
		@RequestParam @NotEmpty List<UtilityType> utilityTypes
	) {
		return ApiResponse.success(
			billRegistrationService.checkDuplicates(userId, billingMonth, utilityTypes)
		);
	}

	@Operation(summary = "고지서 확정 저장", description = "검증된 고지서 항목을 저장하고 진단 및 What-if 월 리포트 갱신 결과를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "고지서 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 필수 항목 누락"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "같은 월과 에너지원 고지서 중복")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<BillCreateResponse>> create(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Valid @RequestBody BillCreateRequest request
	) {
		BillCreateResponse response = billRegistrationService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
