package com.greenpocket.greenlife.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.greenlife.dto.GreenlifeItemsResponse;
import com.greenpocket.greenlife.dto.GreenlifeItemDetailResponse;
import com.greenpocket.greenlife.dto.GreenlifeLinkResponse;
import com.greenpocket.greenlife.dto.GreenlifeStatusResponse;
import com.greenpocket.greenlife.service.GreenlifeService;

@Tag(name = "Benefits", description = "탄소중립포인트 녹색생활실천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/greenlife")
public class GreenlifeController {

	private final GreenlifeService greenlifeService;

	@Operation(summary = "녹색생활실천 참여 상태와 월 현황 조회", description = "참여 여부에 따라 BN-01 또는 BN-02 화면 데이터를 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여 상태와 월 현황 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 월 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/status")
	public ApiResponse<GreenlifeStatusResponse> getStatus(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "조회 월(YYYY-MM)", example = "2026-08")
		@RequestParam(required = false) String month
	) {
		return ApiResponse.success(greenlifeService.getStatus(userId, month));
	}

	@Operation(summary = "녹색생활실천 연동 새로고침", description = "외부 API 대신 데모 실적을 멱등하게 반영하고 참여 상태로 전환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모의 연동 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@PostMapping("/link")
	public ApiResponse<GreenlifeLinkResponse> link(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(greenlifeService.link(userId));
	}

	@Operation(summary = "녹색생활실천 항목 목록 조회", description = "실적이 없는 항목도 포함하여 2026년 공식 17개 항목을 고정 순서로 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "17개 실천 항목 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 월 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패")
	})
	@GetMapping("/items")
	public ApiResponse<GreenlifeItemsResponse> getItems(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "실적 집계 월(YYYY-MM)", example = "2026-08")
		@RequestParam(required = false) String month
	) {
		return ApiResponse.success(greenlifeService.getItems(userId, month));
	}

	@Operation(summary = "녹색생활실천 항목 상세 조회", description = "항목별 실천 방법, 월 실적, 적립 예정액과 최근 이력을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "실천항목 상세 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "조회 월 형식 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "실천항목 없음"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "녹색생활실천 미참여")
	})
	@GetMapping("/items/{itemId}")
	public ApiResponse<GreenlifeItemDetailResponse> getItemDetail(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@Parameter(description = "실천항목 ID", example = "1") @PathVariable Long itemId,
		@Parameter(description = "실적 집계 월(YYYY-MM)", example = "2026-08")
		@RequestParam(required = false) String month
	) {
		return ApiResponse.success(greenlifeService.getItemDetail(userId, itemId, month));
	}
}
