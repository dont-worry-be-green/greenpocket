package com.greenpocket.user.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.user.dto.RegionListResponse;
import com.greenpocket.user.service.RegionService;

@Tag(name = "Common Meta", description = "서울 행정구역 메타 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meta")
public class RegionController {

	private final RegionService regionService;

	@Operation(
		summary = "서울 행정구역 목록 조회",
		description = "sidoCode가 없으면 서울특별시를, 11이면 서울 25개 자치구와 지역 평균 보유 여부를 반환합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역 목록 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "지원하지 않는 시도 코드")
	})
	@SecurityRequirements
	@GetMapping("/regions")
	public ApiResponse<RegionListResponse> findRegions(
		@Parameter(description = "서울특별시 코드. 없으면 시도 목록 반환", example = "11")
		@RequestParam(required = false) String sidoCode
	) {
		return ApiResponse.success(regionService.findRegions(sidoCode));
	}
}
