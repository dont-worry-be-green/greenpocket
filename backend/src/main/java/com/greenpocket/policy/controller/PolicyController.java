package com.greenpocket.policy.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.policy.dto.PolicyDetailResponse;
import com.greenpocket.policy.dto.PolicyListResponse;
import com.greenpocket.policy.service.PolicyQueryService;
import com.greenpocket.profile.entity.PolicyInterestCategory;

@Tag(name = "Youth Policies", description = "온통청년 정책 목록·맞춤 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/policies")
public class PolicyController {

	private final PolicyQueryService policyQueryService;

	@Operation(summary = "맞춤 청년정책 추천 Top 5", description = "저장된 프로필과 에코마일리지 연동 주소로 정확히 판정 가능한 신청 가능 정책을 최대 5개 추천합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "맞춤 정책 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "정책 추천 프로필 미완성"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "동기화된 정책 데이터 없음")
	})
	@GetMapping("/recommendations")
	public ApiResponse<PolicyListResponse> getRecommendations(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(policyQueryService.getRecommendations(userId));
	}

	@Operation(summary = "전체 신청 가능 청년정책 목록", description = "승인·신청기간·개인 대상·신청 경로 검증을 통과한 정책을 검색·필터링합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 정책 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필터 또는 페이징 값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "동기화된 정책 데이터 없음")
	})
	@GetMapping
	public ApiResponse<PolicyListResponse> getAll(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) PolicyInterestCategory category,
		@RequestParam(required = false) String regionCode,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(policyQueryService.getAll(
			userId, keyword, category, regionCode, page, size
		));
	}

	@Operation(summary = "청년정책 상세", description = "정책 내용·신청 방법·조건과 사용자 기준 매칭 결과를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정책 상세 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정책 없음")
	})
	@GetMapping("/{policyId}")
	public ApiResponse<PolicyDetailResponse> getDetail(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@PathVariable String policyId
	) {
		return ApiResponse.success(policyQueryService.getDetail(userId, policyId));
	}
}
