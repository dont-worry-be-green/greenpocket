package com.greenpocket.diagnosis.controller;

import java.time.YearMonth;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.dto.DiagnosisMonthsResponse;
import com.greenpocket.diagnosis.dto.DiagnosisResponse;
import com.greenpocket.diagnosis.service.DiagnosisBaselineService;
import com.greenpocket.diagnosis.service.DiagnosisResultService;
import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.global.type.UtilityType;

@Tag(name = "Diagnosis", description = "생활요금 비교 진단 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/diagnosis")
public class DiagnosisController {

	private final DiagnosisBaselineService diagnosisBaselineService;
	private final DiagnosisResultService diagnosisResultService;

	@Operation(summary = "등록된 청구 월 목록 조회", description = "등록된 고지서가 있는 월을 최신순으로 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "청구 월 목록 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/months")
	public ApiResponse<DiagnosisMonthsResponse> getMonths(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(diagnosisResultService.findMonths(userId));
	}

	@Operation(summary = "월별 진단 결과 조회", description = "선택 월의 합계, 작년 동월, 지역 평균과 What-if 연결 정보를 조회합니다. 월을 생략하면 최신 등록 월을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "진단 결과 또는 빈 상태 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 월 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지정한 월의 등록 고지서 없음")
	})
	@GetMapping
	public ApiResponse<DiagnosisResponse> getDiagnosis(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "조회 월(YYYY-MM), 생략 시 최신 등록 월", example = "2026-08")
		@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
	) {
		return ApiResponse.success(diagnosisResultService.findDiagnosis(userId, month));
	}

	@Operation(summary = "지역 기준선 단건 조회", description = "요청 월 이하의 최신 시군구 기준선을 조회하고 없으면 시도 기준선으로 대체합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기준선 조회 성공 또는 기준선 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 쿼리 누락 또는 쿼리 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/baseline")
	public ApiResponse<DiagnosisBaselineResponse> getBaseline(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "시군구 코드", example = "11620", required = true)
		@RequestParam @NotBlank String sigunguCode,
		@Parameter(description = "조회 기준 월(YYYY-MM)", example = "2026-08", required = true)
		@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
		@Parameter(description = "에너지원", example = "ELECTRICITY", required = true)
		@RequestParam(name = "utility") UtilityType utilityType
	) {
		DiagnosisBaselineResponse response = diagnosisBaselineService.findBaseline(
			userId,
			sigunguCode,
			month,
			utilityType
		);
		return ApiResponse.success(response);
	}
}
