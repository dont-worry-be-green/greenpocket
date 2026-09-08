package com.greenpocket.profile.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenpocket.global.auth.CurrentUserId;
import com.greenpocket.global.response.ApiResponse;
import com.greenpocket.profile.dto.PolicyPreferencesRequest;
import com.greenpocket.profile.dto.PolicyPreferencesResponse;
import com.greenpocket.profile.dto.PolicyPreferencesUpdateResponse;
import com.greenpocket.profile.dto.ProfileResponse;
import com.greenpocket.profile.service.ProfileService;

@Tag(name = "Profiles", description = "생활·청년정책 추천 프로필 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profile")
public class ProfileController {

	private final ProfileService profileService;

	@Operation(summary = "프로필 조회", description = "가입 시 확인한 사용자 정보, 선택한 정책 추천 조건과 에코마일리지 연동 주소를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping
	public ApiResponse<ProfileResponse> find(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(profileService.find(userId));
	}

	@Operation(summary = "정책 추천 조건 조회", description = "마이 탭의 선택 정보와 읽기 전용 생년월일·에코마일리지 주소를 조회합니다.")
	@GetMapping("/policy-preferences")
	public ApiResponse<PolicyPreferencesResponse> findPolicyPreferences(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(profileService.findPolicyPreferences(userId));
	}

	@Operation(summary = "정책 추천 조건 저장", description = "현재 상태·연소득 구간·가구 상태를 저장하고 추천을 갱신합니다.")
	@PutMapping("/policy-preferences")
	public ApiResponse<PolicyPreferencesUpdateResponse> updatePolicyPreferences(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestBody PolicyPreferencesRequest request
	) {
		return ApiResponse.success(profileService.updatePolicyPreferences(userId, request));
	}
}
