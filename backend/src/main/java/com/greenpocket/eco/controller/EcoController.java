package com.greenpocket.eco.controller;

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

import com.greenpocket.eco.dto.EcoCurrentRoundResponse;
import com.greenpocket.eco.dto.EcoGoalFormResponse;
import com.greenpocket.eco.dto.EcoGoalPreviewResponse;
import com.greenpocket.eco.dto.EcoGoalRequest;
import com.greenpocket.eco.dto.EcoGoalResponse;
import com.greenpocket.eco.dto.EcoGoalSaveResponse;
import com.greenpocket.eco.dto.EcoLinkProgressResponse;
import com.greenpocket.eco.dto.EcoLinkStartResponse;
import com.greenpocket.eco.dto.EcoStatusResponse;
import com.greenpocket.eco.service.EcoGoalService;
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
	private final EcoGoalService ecoGoalService;

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

	@Operation(summary = "평가 회차 목표 설정 화면 조회")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목표 설정 화면 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "평가 회차 없음")
	})
	@GetMapping("/rounds/{roundId}/goal-form")
	public ApiResponse<EcoGoalFormResponse> getGoalForm(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "평가 회차 ID", example = "1") @PathVariable Long roundId
	) {
		return ApiResponse.success(ecoGoalService.getGoalForm(userId, roundId));
	}

	@Operation(summary = "평가 회차 목표 미리보기", description = "계산 결과를 반환하며 DB에는 저장하지 않습니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목표 계산 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "목표 구간 또는 요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "평가 회차 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "미등록 요금에 목표 설정")
	})
	@PostMapping("/rounds/{roundId}/goal/preview")
	public ApiResponse<EcoGoalPreviewResponse> previewGoal(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "평가 회차 ID", example = "1") @PathVariable Long roundId,
		@RequestBody EcoGoalRequest request
	) {
		return ApiResponse.success(ecoGoalService.preview(userId, roundId, request));
	}

	@Operation(summary = "평가 회차 목표 최초 저장")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목표 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "목표 구간 또는 요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "평가 회차 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "미등록 요금에 목표 설정")
	})
	@PostMapping("/rounds/{roundId}/goal")
	public ApiResponse<EcoGoalSaveResponse> createGoal(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "평가 회차 ID", example = "1") @PathVariable Long roundId,
		@RequestBody EcoGoalRequest request
	) {
		return ApiResponse.success(ecoGoalService.save(userId, roundId, request));
	}

	@Operation(summary = "평가 회차 목표 수정")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목표 수정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "목표 구간 또는 요청값 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "평가 회차 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "미등록 요금에 목표 설정")
	})
	@PutMapping("/rounds/{roundId}/goal")
	public ApiResponse<EcoGoalSaveResponse> updateGoal(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "평가 회차 ID", example = "1") @PathVariable Long roundId,
		@RequestBody EcoGoalRequest request
	) {
		return ApiResponse.success(ecoGoalService.save(userId, roundId, request));
	}

	@Operation(summary = "평가 회차에 저장된 목표 조회")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장된 목표 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "평가 회차 없음")
	})
	@GetMapping("/rounds/{roundId}/goal")
	public ApiResponse<EcoGoalResponse> getGoal(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "평가 회차 ID", example = "1") @PathVariable Long roundId
	) {
		return ApiResponse.success(ecoGoalService.getGoal(userId, roundId));
	}
}
