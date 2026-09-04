package com.greenpocket.mypage.controller;

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
import com.greenpocket.mypage.dto.MypageResponse;
import com.greenpocket.mypage.dto.ReportListResponse;
import com.greenpocket.mypage.dto.ReportType;
import com.greenpocket.mypage.service.MypageService;
import com.greenpocket.mypage.service.ReportService;

@Tag(name = "Mypage", description = "마이페이지 및 보관함 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MypageController {

	private final MypageService mypageService;
	private final ReportService reportService;

	@Operation(summary = "마이페이지 메인 조회", description = "프로필, 보관함, 제도 연동 및 그린포켓 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/mypage")
	public ApiResponse<MypageResponse> getMypage(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(mypageService.getMypage(userId));
	}

	@Operation(summary = "리포트 보관함 조회", description = "생활비 진단·전달·평가 결과 리포트를 최신순으로 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 목록 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필터 또는 페이징 값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/reports")
	public ApiResponse<ReportListResponse> getReports(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "리포트 유형", example = "MONTHLY_DIAGNOSIS")
		@RequestParam(required = false) ReportType type,
		@Parameter(description = "조회 연도", example = "2026")
		@RequestParam(required = false) @Min(1) Integer year,
		@Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@Parameter(description = "페이지 크기(1~100)", example = "20")
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(reportService.getReports(userId, type, year, page, size));
	}
}
