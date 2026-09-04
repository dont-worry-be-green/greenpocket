package com.greenpocket.diagnosis.controller;

import java.time.YearMonth;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.diagnosis.dto.DiagnosisBaselineResponse;
import com.greenpocket.diagnosis.service.DiagnosisBaselineService;
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

	@Operation(summary = "지역 기준선 단건 조회", description = "요청 월 이하의 최신 시군구 기준선을 조회하고 없으면 시도 기준선으로 대체합니다.")
	@GetMapping("/baseline")
	public ApiResponse<DiagnosisBaselineResponse> getBaseline(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestParam @NotBlank String sigunguCode,
		@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
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
