package com.greenpocket.eco.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.eco.dto.EcoCurrentRoundResponse;
import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.service.EcoLinkService;
import com.greenpocket.eco.service.EcoRoundService;
import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;

@Tag(name = "Green What-if", description = "에코마일리지 목 연동 및 평가 회차 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/eco")
public class EcoController {

	private final EcoLinkService ecoLinkService;
	private final EcoRoundService ecoRoundService;

	@Operation(summary = "에코마일리지 연동 상태 조회")
	@GetMapping("/status")
	public ApiResponse<EcoStatusResponse> getStatus(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(ecoLinkService.getStatus(userId));
	}

	@Operation(summary = "에코마일리지 목 연동 시작", description = "외부 API 대신 데모 기준 사용량을 불러오는 작업을 시작합니다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(
		responseCode = "202",
		description = "에코마일리지 목 연동 작업 생성"
	)
	@PostMapping("/link")
	public ResponseEntity<ApiResponse<EcoLinkStartResponse>> startLink(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ResponseEntity.status(HttpStatus.ACCEPTED)
			.body(ApiResponse.success(ecoLinkService.startLink(userId)));
	}

	@Operation(summary = "에코마일리지 목 연동 진행 조회")
	@GetMapping("/link/{linkJobId}")
	public ApiResponse<EcoLinkProgressResponse> getLinkProgress(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@PathVariable String linkJobId
	) {
		return ApiResponse.success(ecoLinkService.getLinkProgress(userId, linkJobId));
	}

	@Operation(summary = "현재 에코마일리지 평가 회차 조회")
	@GetMapping("/rounds/current")
	public ApiResponse<EcoCurrentRoundResponse> getCurrentRound(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(ecoRoundService.getCurrentRound(userId));
	}
}
