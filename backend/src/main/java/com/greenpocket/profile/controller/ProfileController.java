package com.greenpocket.profile.controller;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.greenpocket.profile.dto.ProfileSaveRequest;
import com.greenpocket.profile.dto.ProfileSaveResponse;
import com.greenpocket.profile.dto.ProfileUpdateRequest;
import com.greenpocket.profile.dto.ProfileUpdateResponse;
import com.greenpocket.profile.service.ProfileService;

@Tag(name = "Profiles", description = "생활·청년정책 추천 프로필 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profile")
public class ProfileController {

	private final ProfileService profileService;

	@Operation(summary = "프로필 저장", description = "생활 기본 정보와 청년정책 추천 조건을 저장하고 온보딩을 완료합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 저장 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "생년월일 또는 관심 분야 입력 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "필수 프로필 값 누락")
	})
	@PostMapping
	public ApiResponse<ProfileSaveResponse> save(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestBody ProfileSaveRequest request
	) {
		return ApiResponse.success(profileService.save(userId, request));
	}

	@Operation(summary = "프로필 조회", description = "현재 사용자의 생활·정책 추천 프로필과 에코마일리지 연동 주소를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "프로필 미완성")
	})
	@GetMapping
	public ApiResponse<ProfileResponse> find(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(profileService.find(userId));
	}

	@Operation(summary = "프로필 수정", description = "이름과 생활·정책 추천 프로필을 수정합니다. 거주지역은 수정하지 않습니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이름·생년월일 또는 관심 분야 입력 오류"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Demo Key 인증 실패"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "필수 프로필 값 누락")
	})
	@PutMapping
	public ApiResponse<ProfileUpdateResponse> update(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestBody ProfileUpdateRequest request
	) {
		return ApiResponse.success(profileService.update(userId, request));
	}

	@Operation(summary = "정책 추천 조건 조회", description = "저장된 정책 추천 조건과 읽기 전용 에코마일리지 주소를 조회합니다.")
	@GetMapping("/policy-preferences")
	public ApiResponse<PolicyPreferencesResponse> findPolicyPreferences(
		@Parameter(hidden = true) @CurrentUserId Long userId
	) {
		return ApiResponse.success(profileService.findPolicyPreferences(userId));
	}

	@Operation(summary = "정책 추천 조건 저장", description = "사용자가 내 정보에 저장을 선택한 정책 추천 조건을 반영합니다.")
	@PutMapping("/policy-preferences")
	public ApiResponse<PolicyPreferencesUpdateResponse> updatePolicyPreferences(
		@Parameter(hidden = true) @CurrentUserId Long userId,
		@RequestBody PolicyPreferencesRequest request
	) {
		return ApiResponse.success(profileService.updatePolicyPreferences(userId, request));
	}
}
